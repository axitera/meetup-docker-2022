# Kata-Containers

[Configuration Dokumentation](https://github.com/containerd/containerd/tree/main/docs)
```shell
vi /etc/containerd/config.toml

ctr plugins ls
crictl pull ghcr.io/axitera/meetup-docker-2022:latest
ctr run --runtime "io.containerd.kata.v2" --rm -d -t "$image" test-kata sleep 300
```
[crictl Dokumentation](https://github.com/kubernetes-sigs/cri-tools/blob/master/docs/crictl.md)
pod-config.json
```json
{
    "metadata": {
        "name": "meetup-docker-2022",
        "namespace": "default",
        "attempt": 1,
        "uid": "hdishd83djaidwnduwk28bcsb"
    },
    "log_directory": "/tmp",
    "linux": {
    }
}
```
container-config.json
```json
{
  "metadata": {
      "name": "meetup-docker-2022"
  },
  "image":{
      "image": "ghcr.io/axitera/meetup-docker-2022:latest"
  },
  "log_path":"busybox.0.log",
  "linux": {
  }
}
```

```shell
crictl run --runtime "io.containerd.kata.v2"  container-config.json pod-config.json
```

```shell
kubectl get all
export SERVICE_POD_NAME=$(kubectl get pods -o=jsonpath='{.items[*].metadata.name}' --selector=app=untrusted)
kubectl logs -f ${SERVICE_POD_NAME}
kubectl exec -it ${SERVICE_POD_NAME} /bin/bash
```

## Install kubernetes
```shell

#echo "192.168.178.75 moseeslux moseslux.localdomain" >> /etc/hosts
#echo "fe80::e7ff:d26b:2f1f:ab9d moseeslux moseslux.localdomain" >> /etc/hosts

#https://stackoverflow.com/questions/52119985/kubeadm-init-shows-kubelet-isnt-running-or-healthy
echo '{"exec-opts": ["native.cgroupdriver=systemd"]}' > /etc/docker/daemon.json
kubeadm init --cri-socket unix:///run/containerd/containerd.sock

# run master node taint
kubectl taint nodes moseslux.localdomain node-role.kubernetes.io/master:NoSchedule-
kubectl apply -f https://raw.githubusercontent.com/flannel-io/flannel/master/Documentation/kube-flannel.yml

#https://kubernetes.io/docs/tasks/configure-pod-container/pull-image-private-registry/
kubectl create secret generic regcred \
    --from-file=.dockerconfigjson=/home/slawomir/.docker/config.json> \
    --type=kubernetes.io/dockerconfigjson
    
    
kubectl create deployment meetup-docker-2022 --image=ghcr.io/axitera/meetup-docker-2022:latest
```
