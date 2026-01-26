# Kafka Training Environment
[⬅️ Back to Kafka overview](README.md)


This training comes with a predefined environment provided in multiple docker containers. 

## Technical prerequisite for the training

The following service components should be installed on your training device before attending the training:

* Development: 
  * VS Code (or your IDE/text editor of choice) 
  * Current Java JDK
  * Git
  * Maven (if not already included in IDE)
* Clone `git clone https://github.com/Zuehlke/kafka-streaming-technology.git` to your training device
* Optional: HTTP client to send requests like Postman


## Access to your environment

From your instructor to will get information how to access to a ubuntu-vm in the cloud:

- Login <a href="https://zuhlke.awsapps.com/start/#/?tab=accounts" target="_blank">url</a>
- Click on the Account named `I10ZCH_DEVOP_2024 / Zuhlke_K8s_Trainings` and then on the Role named `Contributor`<br>![AWS Login](/img/aws-login.png)
- After login, you find your VM <a href="https://lightsail.aws.amazon.com/ls/webapp/home/instances" target="_blank">here</a>

There are different VM's available, pick the one with your name and start it.

## Configure your environment

### Modify your local hosts file (DNS entry)

Add the following entry in your hosts file:  `[ip of your VM] myVMsIP`
* Windows in `C:\Windows\System32\drivers\etc\hosts`
* MAC in `/private/etc/hosts`

### SSH

For some exercises you need to login with ssh to your instance (the webconsole does not work as there are a limited number of consoles available):
* you got your ssh key by mail
* Login: `ssh -i [yourKey].pem -l ubuntu myVMsIP`
* in the case password protection is enforced, create a password-protected file: `openssl rsa -aes256 -in [yourKey].pem -out [yourProtectedKey].pem`


### Tool access

To enable tool access for the exercises, whitelist [the IP of your workstation](https://whatismyipaddress.com/) with the following ports [on your vm](https://lightsail.aws.amazon.com/ls/webapp/home/instances): `Networking -> IPv4 Firewall -> Add rule`:

* AKHQ:            Custom TCP 8080 [your IP]
* Schema Registry: Custom TCP 8081 [your IP]
* Rest Proxy:      Custom TCP 8082 [your IP]
* Kafka Connect:   Custom TCP 8083 [your IP]
* phpMyAdmin:      Custom TCP 8085 [your IP]
* KSQL:            Custom TCP 8088 [your IP]
* Kafka Broker:    Custom TCP 9094 [your IP]
* MariaDB:         Custom TCP 3306 [your IP]

If your IP changes, you have to redo this.


## Test your setup

### Test AKHQ
 * in your [browser](http://myVMsIP:8080): `myVMsIP:8080`

You should see the AKHQ Topics screen


### Test the setup of your local development environment

* In your IDE-Terminal or shell, change to the following directory: `$ cd uc-iot/kafka-stream/`
* Compile the sources: `~/kafka-streaming-technology/uc-iot/kafka-stream$ mvn compile`

```
~/kafka-streaming-technology/uc-iot/kafka-stream$ mvn compile
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by com.google.inject.internal.cglib.core.$ReflectUtils$1 (file:/usr/share/maven/lib/guice.jar) to method java.lang.ClassLoader.defineClass(java.lang.String,byte[],int,int,java.security.ProtectionDomain)
WARNING: Please consider reporting this to the maintainers of com.google.inject.internal.cglib.core.$ReflectUtils$1
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------< com.zuehlke.training.kafka.iot:kafka-stream >-------------
[INFO] Building kafka-stream 0.0.1-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- avro-maven-plugin:1.11.1:schema (default) @ kafka-stream ---
[INFO] 
[INFO] --- maven-resources-plugin:3.2.0:resources (default-resources) @ kafka-stream ---
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] Using 'UTF-8' encoding to copy filtered properties files.
[INFO] Copying 1 resource
[INFO] Copying 2 resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.10.1:compile (default-compile) @ kafka-stream ---
[INFO] Nothing to compile - all classes are up to date
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  1.392 s
[INFO] Finished at: 2023-04-06T17:15:29+02:00
[INFO] ------------------------------------------------------------------------
~/kafka-streaming-technology/uc-iot/kafka-stream$ 
```



* start the container: `mvn spring-boot:run`
* you should see the following output (after a while):

```
...
2023-04-07 11:00:17.951  INFO 39972 --- [-StreamThread-1] c.z.t.kafka.iot.stream.Exercise0Stream   : Processing message with key=mySensor and value=161829
2023-04-07 11:00:17.952  INFO 39972 --- [-StreamThread-1] c.z.t.kafka.iot.stream.Exercise0Stream   : Processing message with key=mySensor and value=871768
2023-04-07 11:00:17.952  INFO 39972 --- [-StreamThread-1] c.z.t.kafka.iot.stream.Exercise0Stream   : Processing message with key=myMotor and value=running
2023-04-07 11:00:17.953  INFO 39972 --- [-StreamThread-1] c.z.t.kafka.iot.stream.Exercise0Stream   : Processing message with key=mySensor and value=638412
2023-04-07 11:00:17.954  INFO 39972 --- [-StreamThread-1] c.z.t.kafka.iot.stream.Exercise0Stream   : Processing message with key=mySensor and value=584775
2023-04-07 11:00:17.955  INFO 39972 --- [-StreamThread-1] c.z.t.kafka.iot.stream.Exercise0Stream   : Processing message with key=myMotor and value=running
2023-04-07 11:00:17.955  INFO 39972 --- [-StreamThread-1] c.z.t.kafka.iot.stream.Exercise0Stream   : Processing message with key=mySensor and value=852575
2023-04-07 11:00:27.491  INFO 39972 --- [-StreamThread-1] c.z.t.kafka.iot.stream.Exercise0Stream   : Processing message with key=mySensor and value=756824
...
```

turn off the vm when your finished the configuration
