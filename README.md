# Setup Intellij

```
File -> Settings -> Build, Execution, Deployment -> Compiler -> Build project automatically
```

```
File -> Settings -> Advenced Settings -> Allow auto-make to start even if developed application is running 
```
build.gradle output-Verzeichnis setzen
```groovy
idea {
	module {
		inheritOutputDirs = false
		outputDir = file("$buildDir/classes/java/main/")
	}
}
```

# Docker


build.gradle DevTools müssen im das jar eingebaut werden, dass in das Iamge kommt.
```groovy

bootJar {
	classpath configurations.developmentOnly
}

dependencies {
	implementation('org.springframework.boot:spring-boot-starter-web')
	developmentOnly("org.springframework.boot:spring-boot-devtools")
}
```

1. Image bauen
2. ausführen
3. RemoteSpringApplication starten
4. Code editeren

# Test

## Embedded 

Zonky braucht unter Windows

[visual c 2013](https://support.microsoft.com/en-us/topic/update-for-visual-c-2013-and-visual-c-redistributable-package-5b2ac5ab-4139-8acc-08e2-9578ec9b2cf1)


# Kafka consumer

```shell
kafka-console-consumer.sh --topic ax-meetup-topic --from-beginning --bootstrap-server localhost:9092
```