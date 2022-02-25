# Ablauf

1. Spring-DevTools, schnell mal bauen
   1. live-edit
2. Image bauen ohne Docker (Jib)
3. Spring-Boot Plugin build-image
4. Testcontainer vs. Embedded
   1. DB
   2. MockService
   3. Kafka
5. Docker vs. MicroVMs
6. *Serverless (KNative)


# moseslux install

1. rockylinux 8 minimal (über USB ohne LAN)
2. Mit LAN `ifup enp1s0 && dnf install NetworkManager-wifi`
3. restart
4. Kabel ab
5. `dnf upgrade`
6. mkdir .ssh
7. chmod 700 .ssh
8. vi authorized_keys
9. chmod 400 .ssh/authorized_keys
10. vi /etc/ssh/sshd_config # PermitRootLogin yes -> no && PasswordAuthentication yes -> no
11. setenforce 0 # selinux permissive
12. sed -i 's/^SELINUX=enforcing$/SELINUX=permissive/' /etc/selinux/config

## 0. prerequirements
```shell
dnf install -y golang git telnet
mkdir ~/go
echo "export GOPATH=${HOME}/go" >> ~/.bashrc
```


## 1. install docker
```shell
dnf config-manager --add-repo=https://download.docker.com/linux/centos/docker-ce.repo
dnf update
dnf install -y docker-ce docker-ce-cli containerd.io
systemctl enable docker
systemctl start docker
systemctl status docker
usermod -aG docker slawomir
```

## 2. install kata-containers

```shell
curl -L -O https://github.com/kata-containers/kata-containers/releases/download/2.3.3/kata-static-2.3.3-x86_64.tar.xz
tar -xvf kata-static-2.3.3-x86_64.tar.xz -C /
ln -s /opt/kata/bin/containerd-shim-kata-v2 /usr/bin
ln -s /opt/kata/bin/kata-collect-data.sh /usr/bin
ln -s /opt/kata/bin/kata-runtime /usr/bin
# cat <<EOF | sudo tee /etc/docker/daemon.json
#{
#  "default-runtime": "kata-runtime",
#  "runtimes": {
#    "kata-runtime": {
#      "path": "/usr/bin/kata-runtime"
#    }
#  }
#}
#EOF
#systemctl daemon-reload
#systemctl restart docker

```

## install runc

```shell
curl -OL https://github.com/opencontainers/runc/releases/download/v1.1.0/runc.amd64 && mv runc.amd64 /usr/bin/runc && chmod +x /usr/bin/runc
```

## 3. install containerd
```shell
export CONTAINERD_VERSION=1.6.1
curl -L -O https://github.com/containerd/containerd/releases/download/v${CONTAINERD_VERSION}/containerd-${CONTAINERD_VERSION}-linux-amd64.tar.gz
tar -xvf containerd-${CONTAINERD_VERSION}-linux-amd64.tar.gz -C /usr
curl -O https://raw.githubusercontent.com/containerd/containerd/master/containerd.service && mv containerd.service /usr/lib/systemd/system/containerd.service
ln -s /usr/lib/systemd/system/containerd.service /etc/systemd/system/multi-user.target.wants/
/sbin/restorecon -v /usr/lib/systemd/system/containerd.service
mkdir -p /etc/containerd
containerd config default | sudo tee /etc/containerd/config.toml
systemctl daemon-reload
systemctl start containerd
systemctl enable containerd

```

## 4. install cni network plugin
```shell
export GO111MODULE=off
go get github.com/containernetworking/plugins
pushd $GOPATH/src/github.com/containernetworking/plugins
./build_linux.sh
mkdir -p /opt/cni
sudo cp -r bin /opt/cni/
popd

openssl req -newkey rsa:4096 \
           -keyout cni.key \
           -nodes \
           -out cni.csr \
           -subj "/CN=calico-cni"

openssl x509 -req -in cni.csr \
                  -CA /etc/kubernetes/pki/ca.crt \
                  -CAkey /etc/kubernetes/pki/ca.key \
                  -CAcreateserial \
                  -out cni.crt \
                  -days 365
chown $(id -u):$(id -g) cni.crt
APISERVER=$(kubectl config view -o jsonpath='{.clusters[0].cluster.server}')

kubectl config set-cluster kubernetes \
    --certificate-authority=/etc/kubernetes/pki/ca.crt \
    --embed-certs=true \
    --server=$APISERVER \
    --kubeconfig=cni.kubeconfig
    
kubectl config set-credentials calico-cni \
    --client-certificate=cni.crt \
    --client-key=cni.key \
    --embed-certs=true \
    --kubeconfig=cni.kubeconfig
    
kubectl config set-context default \
    --cluster=kubernetes \
    --user=calico-cni \
    --kubeconfig=cni.kubeconfig

kubectl config use-context default --kubeconfig=cni.kubeconfig

kubectl apply -f - <<EOF
kind: ClusterRole
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: calico-cni
rules:
  # The CNI plugin needs to get pods, nodes, and namespaces.
  - apiGroups: [""]
    resources:
      - pods
      - nodes
      - namespaces
    verbs:
      - get
  # The CNI plugin patches pods/status.
  - apiGroups: [""]
    resources:
      - pods/status
    verbs:
      - patch
 # These permissions are required for Calico CNI to perform IPAM allocations.
  - apiGroups: ["crd.projectcalico.org"]
    resources:
      - blockaffinities
      - ipamblocks
      - ipamhandles
    verbs:
      - get
      - list
      - create
      - update
      - delete
  - apiGroups: ["crd.projectcalico.org"]
    resources:
      - ipamconfigs
      - clusterinformations
      - ippools
    verbs:
      - get
      - list
EOF

kubectl create clusterrolebinding calico-cni --clusterrole=calico-cni --user=calico-cni



curl -L -o /opt/cni/bin/calico https://github.com/projectcalico/cni-plugin/releases/download/v3.14.0/calico-amd64
chmod 755 /opt/cni/bin/calico
curl -L -o /opt/cni/bin/calico-ipam https://github.com/projectcalico/cni-plugin/releases/download/v3.14.0/calico-ipam-amd64
chmod 755 /opt/cni/bin/calico-ipam

openssl req -newkey rsa:4096 \
           -keyout calico-node.key \
           -nodes \
           -out calico-node.csr \
           -subj "/CN=calico-node"

openssl x509 -req -in calico-node.csr \
                  -CA typhaca.crt \
                  -CAkey typhaca.key \
                  -CAcreateserial \
                  -out calico-node.crt \
                  -days 365


echo '{"cniVersion":"0.4.0","name":"myptp","type":"ptp","ipMasq":true,"ipam":{"type":"host-local","subnet":"172.16.29.0/24","routes":[{"dst":"0.0.0.0/0"}]},"dns": {"nameservers": [ "10.1.1.1", "8.8.8.8" ]}}' | sudo tee /etc/cni/net.d/10-myptp.conf

```

## 4. configure containerd for kata-containers as runtime 
Ensure /etc/containerd/config.toml
```toml
...
[plugins]
  [plugins.cri]
    [plugins."io.containerd.grpc.v1.cri".containerd]
      [plugins."io.containerd.grpc.v1.cri".containerd.default_runtime]
        #runtime_type = "io.containerd.runtime.v1.linux"

    [plugins."io.containerd.grpc.v1.cri".cni]
      # conf_dir is the directory in which the admin places a CNI conf.
      conf_dir = "/etc/cni/net.d"
...
[plugins."io.containerd.grpc.v1.cri".containerd]
      no_pivot = false
    [plugins."io.containerd.grpc.v1.cri".containerd.untrusted_workload_runtime]
      runtime_type = "io.containerd.kata.v2"
    [plugins."io.containerd.grpc.v1.cri".containerd.runtimes]
      [plugins."io.containerd.grpc.v1.cri".containerd.runtimes.runc]
         runtime_type = "io.containerd.runc.v1"
         [plugins."io.containerd.grpc.v1.cri".containerd.runtimes.runc.options]
           NoPivotRoot = false
           NoNewKeyring = false
           ShimCgroup = ""
           IoUid = 0
           IoGid = 0
           BinaryName = "runc"
           Root = ""
           CriuPath = ""
           SystemdCgroup = true   #<----important
      [plugins."io.containerd.grpc.v1.cri".containerd.runtimes.kata]
         runtime_type = "io.containerd.kata.v2"
      [plugins."io.containerd.grpc.v1.cri".containerd.runtimes.katacli]
         runtime_type = "io.containerd.runc.v1"
         [plugins."io.containerd.grpc.v1.cri".containerd.runtimes.katacli.options]
           NoPivotRoot = false
           NoNewKeyring = false
           ShimCgroup = ""
           IoUid = 0
           IoGid = 0
           BinaryName = "/usr/bin/kata-runtime"
           Root = ""
           CriuPath = ""
           SystemdCgroup = false
```

Try:
```shell
ctr image pull docker.io/library/busybox:latest
ctr run --runtime io.containerd.run.kata.v2 -t --rm docker.io/library/busybox:latest hello sh
```

## install kubernates

```shell
#https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/install-kubeadm/
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
mv kubectl /usr/bin
swapoff -a 
vi /etc/fstab
mount -a
reboot
cat <<EOF | sudo tee /etc/modules-load.d/k8s.conf
br_netfilter
EOF
cat <<EOF | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-ip6tables = 1
net.bridge.bridge-nf-call-iptables = 1
net.ipv4.ip_forward = 1
EOF

cat <<EOF | sudo tee /etc/yum.repos.d/kubernetes.repo
[kubernetes]
name=Kubernetes
baseurl=https://packages.cloud.google.com/yum/repos/kubernetes-el7-\$basearch
enabled=1
gpgcheck=1
repo_gpgcheck=1
gpgkey=https://packages.cloud.google.com/yum/doc/yum-key.gpg https://packages.cloud.google.com/yum/doc/rpm-package-key.gpg
exclude=kubelet kubeadm kubectl
EOF

sed -i 's/^SELINUX=enforcing$/SELINUX=permissive/' /etc/selinux/config
dnf install -y kubelet kubeadm kubectl --disableexcludes=kubernetes
systemctl enable --now kubelet
reboot
kubeadm init
mkdir -p $HOME/.kube
cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
chown $(id -u):$(id -g) $HOME/.kube/config
kubectl taint nodes --all node-role.kubernetes.io/master-


mkdir -p  /etc/systemd/system/kubelet.service.d/
cat << EOF | sudo tee  /etc/systemd/system/kubelet.service.d/0-containerd.conf
[Service]                                                 
Environment="KUBELET_EXTRA_ARGS=--container-runtime=remote --runtime-request-timeout=15m --container-runtime-endpoint=unix:///run/containerd/containerd.sock --network-plugin=kubenet"
EOF
systemctl daemon-reload

 cat > runtime.yaml <<EOF
apiVersion: node.k8s.io/v1
kind: RuntimeClass
metadata:
  name: kata
handler: kata
EOF

```

```shell
kubectl create secret docker-registry regcred --docker-server=ghcr.io --docker-username=mosesonline --docker-password=${GH_TOKEN}

```

# allow operations to user

```shell
export MY_USER=slawomir
mkdir -p /home/$MY_USER/.kube
cp -i /etc/kubernetes/admin.conf /home/$MY_USER/.kube/config
chown $(id -u slawomir):$(id -g slawomir) /home/$MY_USER/.kube/config
```

# reset Kubernetes cluster when IP changed

[Quelle](https://stackoverflow.com/questions/52647072/my-kubernetes-cluster-ip-address-changed-and-now-kubectl-will-no-longer-connect)

```shell
systemctl stop kubelet
systemctl stop containerd
pkill kube-scheduler
pkill kube*
pkill containerd-shim*
pkill core*
pkill pause*
rm -rf /etc/kubernetes-backup
rm -rf /var/lib/kubelet-backup
cd /etc/
mv kubernetes kubernetes-backup
mv /var/lib/kubelet /var/lib/kubelet-backup
mkdir -p kubernetes
cp -r kubernetes-backup/pki kubernetes
rm kubernetes/pki/{apiserver.*,etcd/peer.*}
systemctl start containerd.service

kubeadm init --ignore-preflight-errors=DirAvailable--var-lib-etcd

rm -rf ~/.kube
rm -rf /home/slawomir/.kube
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config

kubectl taint nodes moseslux.localdomain node-role.kubernetes.io/master:NoSchedule-
```


# The rest
12. dnf module install -y container-tools xz
13. https://github.com/kata-containers/kata-containers/blob/main/docs/install/container-manager/containerd/containerd-install.md
    1. curl -L -O https://github.com/kata-containers/kata-containers/releases/download/2.3.3/kata-static-2.3.3-x86_64.tar.xz
    2. tar -xvf kata-static-2.3.3-x86_64.tar.xz -C /
    3. links erstellen
    4. curl -L -O https://github.com/containerd/containerd/releases/download/v1.6.1/containerd-1.6.1-linux-amd64.tar.gz
    5. tar -xvf containerd-1.6.1-linux-amd64.tar.gz -C /usr
    6. curl -O https://raw.githubusercontent.com/containerd/containerd/master/containerd.service && mv containerd.service /etc/systemd/system/multi-user.target.wants/containerd.service
    7. mkdir -p /etc/containerd
    8. containerd config default | sudo tee /etc/containerd/config.toml
    9. /sbin/restorecon -v '/var/lib/containerd/io.containerd.metadata.v1.bolt/meta.db'
    10. /sbin/restorecon -v /usr/lib/systemd/system/containerd.service
    11. /sbin/restorecon -v /usr/local/bin/containerd
14. install docker
    1.  dnf config-manager --add-repo=https://download.docker.com/linux/centos/docker-ce.repo 
```shell
#links erstellen

```
    