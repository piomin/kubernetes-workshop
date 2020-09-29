# Kubernetes Workshop
Checkout branch `master`. It contains the starting version of source code. For the final version switch to branch `final`.
With the source code version in `master` you can follow the instructions.

## Table of Contents
1. [Prerequisites](#1-prerequisites)\
  1.a) [JDK](#1a-jdk)\
  1.b) [Git](#1b-git)\
  1.c) [Maven](#1c-maven)\
  1.d) [Skaffold](#1d-skaffold)\
  1.e) [Istio](#1e-istio)\
  1.f) [Docker registry](#1f-docker-registry)\
  1.g) [Existing cluster on GKE](#1g-existing-cluster-on-gke)\
  1.h) [Disable auto-save (optional)](#1h-optionally-disable-auto-save-on-your-ide)
2. [Skaffold/Jib](#2-skaffoldjib)\
  2.a) [Initialize Skaffold for projects](#2a-initialize-skaffold-for-projects)\
  2.b) [Deploy applications in dev mode](#2b-deploy-applications-in-dev-mode)\
  2.c) [Deploy applications in debug mode](#2c-deploy-applications-in-debug-mode)\
  2.d) [Deploy applications in multi-module mode](#2d-initialize-skaffold-in-multi-module-mode)
3. [Development](#3-development)\
  3.a) [Inject metadata into container](#3a-inject-metadata-into-container)\
  3.b) [Add code](#3b-add-code)\
  3.c) [Inject labels into container](#3c-inject-labels-with-downwardapi)\
  3.d) [Read and print labels inside application](#3d-read-and-print-labels-inside-application)
4. [Install Istio](#4-install-istio)
5. [Configure traffic with Istio](#5-configure-traffic-with-istio)\
  5.a) [Split across versions](#5a-split-across-versions)\
  5.b) [Create Istio gateway](#5b-create-istio-gateway)\
  5.c) [Inter-service communication](#5c-inter-service-communication)\
  5.d) [Faults and timeouts](#5d-faults-and-timeouts)

### 1. Prerequisites

#### 1.a) JDK
Check Java (JDK) version by calling `java --version`. Version used in this workshop is 11.\
You can download it here: https://www.oracle.com/java/technologies/javase-jdk11-downloads.html.

#### 1.b) Git
Check Git Client version by calling `git version`.\
You can download it here: https://git-scm.com/downloads.
 
#### 1.c) Maven
Check Maven version by calling `mvn --version`.\
You can download it here: https://maven.apache.org/download.cgi.

Example response:
```
Apache Maven 3.6.0 (97c98ec64a1fdfee7767ce5ffb20918da4f719f3; 2018-10-24T20:41:47+02:00)\
Maven home: C:\Users\minkowp\apache-maven-3.6.0\bin\..\
Java version: 11.0.1, vendor: Oracle Corporation, runtime: C:\Program Files\jdk-11.0.1\
```

#### 1.d) Skaffold
Check Skaffold version by calling `skaffold version`.\
You can download it here: https://skaffold.dev/docs/install/.

#### 1.e) Istio
Check Istio CLI version by calling `istioctl version`.\
You can download it here: https://github.com/istio/istio/releases/tag/1.7.2.

#### 1.f) Docker Registry
You need to have account on the remote Docker Registry like docker.io.\
Your docker.io username is then referred as `YOUR_DOCKER_USERNAME`.

#### 1.g) Existing cluster on GKE
Use `gcloud` command to initialize a new Kubernetes cluster using GKE services:

```shell script
gcloud container clusters create cnw3 \
   --zone europe-west3 \
   --node-locations europe-west3-a,europe-west3-b,europe-west3-c \
   --cluster-version=1.17 \
   --disk-size=50GB \
   --enable-autoscaling \
   --num-nodes=1 \
   --min-nodes=1 \
   --max-nodes=3
```

After it has successfully started configure kubectl context:

```shell script
gcloud container clusters get-credentials cnw3 --region=europe-west3
```

#### 1.h) Optionally disable auto-save on your IDE
With Intellij go to File > Settings > Appearance & Behavior > System Settings.\
Uncheck the following:
- Save files on frame deactivation
- Save files automatically if application is idle for x sec.

### 2. Skaffold/Jib

#### 2.a) Initialize Skaffold for projects
My docker.io login is `piomin`, so I will just replace all occurrences of `<YOUR_DOCKER_USERNAME>` into `piomin`. It is used then in case of pushing image to remote registry.
Go to callme-service `cd callme-service`.\
Execute command `skaffold init --XXenableJibInit`. Then you should see the message `One or more valid Kubernetes manifests are required to run skaffold`.\
Then you should create a directory `k8s` and place there a YAML manifest with `Deployment`.\
For example:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: callme-deployment
spec:
  selector:
    matchLabels:
      app: callme
  template:
    metadata:
      labels:
        app: callme
    spec:
      containers:
      - name: callme
        image: <YOUR_DOCKER_USERNAME>/callme-service
        ports:
        - containerPort: 8080
```
Then, add the following fragment inside `build.plugins` tag in `callme-service/pom.xml`.
```xml
<plugin>
    <groupId>com.google.cloud.tools</groupId>
    <artifactId>jib-maven-plugin</artifactId>
    <version>2.4.0</version>
</plugin>
```

Now, execute command `skaffold init --XXenableJibInit` once again and accept communicate:\
"Do you want to write this configuration to skaffold.yaml? [y/n]". -> y\
The skaffold.yaml has been generated.
```yaml
apiVersion: skaffold/v2beta5
kind: Config
metadata:
  name: callme-service
build:
  artifacts:
  - image: <YOUR_DOCKER_USERNAME>/callme-service
    jib:
      project: pl.piomin.samples.kubernetes:callme-service
deploy:
  kubectl:
    manifests:
    - k8s/deployment.yaml
```
You can remove all lines after line 8. 
```yaml
apiVersion: skaffold/v2beta5
kind: Config
metadata:
  name: callme-service
build:
  artifacts:
  - image: <YOUR_DOCKER_USERNAME>/callme-service
    jib: {}
```

Go to caller-service directory `cd caller-service`. Then check if you have to add there anything else the same as for callme-service. 
You need to change <YOUR_DOCKER_USERNAME> into your login in `deployment.yaml`.

#### 2.b) Deploy applications in dev mode
First, let's create a dedicated namespace for our workshop: `kubectl create ns workshop`. (optional)\
Then let's set it as a default namespace for kubectl: `kubectl config set-context --current --namespace=workshop` (optional)\
Go to callme-service directory and run Skaffold: `skaffold dev -n workshop --port-forward`.\
Go to caller-service directory and run Skaffold: `skaffold dev -n workshop --port-forward`. 

#### 2.c) Deploy applications in debug mode
Go to callme-service directory and run Skaffold: `skaffold debug -n workshop --port-forward`.\
Then you can find the following log (the number of forwarded port is important here):
```
Port forwarding pod/callme-deployment-6f595bb5f4-sgpvr in namespace workshop, remote port 5005 -> address 127.0.0.1 port 5005
...
Picked up JAVA_TOOL_OPTIONS: -agentlib:jdwp=transport=dt_socket,server=y,address=5005,suspend=n,quiet=y
```
Go to caller-service directory and run Skaffold: `skaffold debug -n workshop --port-forward`.\
(Optional) Now we create a debugger. With IntelliJ go to Run -> Edit configurations... -> Templates -> Remote -> New...\
Then set the port number and module name (demo).
 
#### 2.d) Initialize Skaffold in multi-module mode
Go to root directory of project `cd ..`. \
Now, execute command `skaffold init --XXenableJibInit` in the root of Maven project:\
First choose "None" [ENTER], then \
"Do you want to write this configuration to skaffold.yaml? [y/n]". -> y\
The skaffold.yaml has been generated.
```yaml
apiVersion: skaffold/v2beta5
kind: Config
metadata:
  name: kubernetes-workshop
deploy:
  kubectl:
    manifests:
    - caller-service/k8s/deployment.yaml
    - callme-service/k8s/deployment.yaml
```
Let's modify the `skaffold.yaml` file into that:
```yaml
apiVersion: skaffold/v2beta5
kind: Config
metadata:
  name: kubernetes-workshop
build:
  artifacts:
    - image: <YOUR_DOCKER_USERNAME>/callme-service
      jib:
        project: callme-service
    - image: <YOUR_DOCKER_USERNAME>/caller-service
      jib:
        project: caller-service
deploy:
  kubectl:
    manifests:
    - callme-service/k8s/*.yaml
    - caller-service/k8s/*.yaml
    - k8s/*.yaml
```
Then run Skaffold in dev mode: `skaffold dev -n workshop --port-forward`.

### 3. Development

#### 3.a) Inject metadata into container
Add the following environment variables to `Deployment` in section `spec.template.spec.containers`
```yaml
env:
  - name: POD_NAME
    valueFrom:
      fieldRef:
        fieldPath: metadata.name
  - name: POD_NAMESPACE
    valueFrom:
      fieldRef:
        fieldPath: metadata.namespace
```

#### 3.b) Add code
Go to callme-service -> `src/main/java` -> `pl.piomin.samples.kubernetes.CallmeController`.\
The name of package is just a proposition.\ 
```java
public class CallmeController {

	@Value("${spring.application.name}")
	private String appName;
	@Value("${POD_NAME}")
	private String podName;
	@Value("${POD_NAMESPACE}")
	private String podNamespace;

	@GetMapping("/ping")
	public String ping() {
		return appName + ": " + podName + " in " + podNamespace;
	}

}
```
Here's my current structure of the project:

callme-service\
--k8s/\
----deployment.yaml\
--src/main/\
----java/pl/piomin/samples/kubernetes/\
------controller/\
--------CallmeController.java\
------utils/\
--------AppVersion.java\
------CallmeApplication.java\
----resources/\
------application.yml

#### 3.c) Inject labels with `downwardAPI`
Add volume to section `spec.template.spec`
```yaml
volumes:
- name: podinfo
  downwardAPI:
    items:
      - path: "labels"
        fieldRef:
          fieldPath: metadata.labels
```
Then mount volume to the container:
```yaml
volumeMounts:
  - mountPath: /etc/podinfo
    name: podinfo
```
 
#### 3.d) Read and print labels inside application
The implementation of bean responsible for reading `version` label from file inside a mounted volume is ready and visible below.
```java
@Component
public class AppVersion {

	public String getVersionLabel() {
		try (Stream<String> stream = Files.lines(Paths.get("/etc/podinfo/labels"))) {
			stream.forEach(System.out::println);
			Optional<String> optVersion = stream.filter(it -> it.startsWith("version=")).findFirst();
			return optVersion.map(s -> s.split("=")[1].replace("\"", ""))
					.orElse("null");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "null";
	}
}
```

Go to callme-service -> `src/main/java` -> `pl.piomin.samples.kubernetes.CallmeController`.\
Then inject `AppVersion` bean and invoke method `getVersionLabel` in `ping` method.
```
@Autowired
private AppVersion appVersion;

@GetMapping("/ping")
public String ping() {
    return appName + "(" + appVersion.getVersionLabel() + "): " + " in " + podNamespace;
}
```

Reload is finished automatically. The last line in logs is similar to the following one:
```
[callme-deployment-75f9d5ff44-4jwst callme] 2020-08-17 12:33:12.089  INFO 1 --- [           main] p.p.s.kubernetes.CallmeApplication       : Started Cal
 lmeApplication in 6.589 seconds (JVM running for 7.403)
```

Call HTTP endpoint `GET /callme/ping`: `curl http://localhost:8080/callme/ping`.\
The response: `curl: (52) Empty reply from server`.

Add `Service` definition in `callme-service/k8s/deployment.yaml`:
```yaml
---
apiVersion: v1
kind: Service
metadata:
  name: callme-service
spec:
  type: ClusterIP
  selector:
    app: callme
  ports:
  - port: 8080
```

After reload you should see the following line in the logs:
```
Port forwarding service/callme-service in namespace test, remote port 8080 -> address 127.0.0.1 port 8080
```

Call the endpoint once again:
```shell script
curl http://localhost:8080/callme/ping
{"timestamp":"2020-08-17T12:41:24.492+00:00","status":500,"error":"Internal Server Error","message":"","path":"/callme/ping"}
```

Go to the logs. Try to fix error. In case you had problems with it or you just want to skip it the proper implementation of `AppVersion` is inside `caller-service`. \
Go to caller-service -> `src/main/java` -> `pl.piomin.samples.kubernetes.utils.AppVersion`, and compare it with method in `callme-service`. \
After fix call the endpoint once again:
```shell script
curl http://localhost:8080/callme/ping
callme-service(null): callme-deployment-6d94874588-gs9vw in test
```

#### 3.e) Deploy two versions of application
Before any change stop command `skaffold dev` with CTRL+C to clean resources. \
Go to `callme-service\k8s\deployment.yaml`. \
Add to `spec.template.metadata.labels` and to `spec.selector.matchLabels` the new label `version: v1`. \
Change the name of `Deployment` from `callme-deployment` to `callme-deployment`. \
Then create a second deployment `callme-deployment-v2`.
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: callme-deployment-v2
spec:
  selector:
    matchLabels:
      app: callme
      version: v2
  template:
    metadata:
      labels:
        app: callme
        version: v2
    spec:
      containers:
        - name: callme
          image: <YOUR_DOCKER_USERNAME>/callme-service

          ports:
            - containerPort: 8080
          env:
            - name: POD_NAME
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
            - name: POD_NAMESPACE
              valueFrom:
                fieldRef:
                  fieldPath: metadata.namespace
          volumeMounts:
            - mountPath: /etc/podinfo
              name: podinfo
      volumes:
        - name: podinfo
          downwardAPI:
            items:
              - path: "labels"
                fieldRef:
                  fieldPath: metadata.labels
```

After reload verify list of `Deployment` in your namespace.
```shell script
kubectl get deploy -n test
NAME                   READY   UP-TO-DATE   AVAILABLE   AGE
callme-deployment-v1   1/1     1            1           88s
callme-deployment-v2   1/1     1            1           88s
```

Call the endpoint once again: `curl http://localhost:8080/callme/ping`.

#### 3.f) Deploy `caller-service`
Go to `caller-service` directory.\
Run `skaffold dev --port-forward`. You will probably have port 8081.
Let's verify the state:
```shell script
$ kubectl get deploy -n test
NAME                   READY   UP-TO-DATE   AVAILABLE   AGE
callme-deployment-v1   1/1     1            1           88s
callme-deployment-v2   1/1     1            1           88s
caller-deployment-v1   1/1     1            1           44s
caller-deployment-v2   1/1     1            1           44s
```

Call the endpoint: `curl http://localhost:8080/caller/ping`.

### 4. Install Istio
If running on the local cluster: `istioctl install`. \
Enable Istio for your namespace: `kubectl label namespace <YOUR_NAMESPACE> istio-injection=enabled`. \
After installation you can the following commands:
```shell script
$ istioctl version
client version: 1.6.5
control plane version: 1.6.5
data plane version: 1.6.5 (4 proxies)
```
And then:
```shell script
$ kubectl get pod -n istio-system
NAME                                    READY   STATUS    RESTARTS   AGE
istio-ingressgateway-54c4985b54-fhg7g   1/1     Running   0          1d
istiod-585c5d45f5-qlwnd                 1/1     Running   0          1d
prometheus-547b4d6f8c-lrcbj             2/2     Running   0          1d
```

### 5. Configure traffic with Istio

#### 5.a) Split across versions
Go to callme-service/k8s directory. Create the file `istio.yml`. You can pick any name.
Create `DestinationRule` with 2 subsets: `v1` and `v2`.
```yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: callme-service-destination
spec:
  host: callme-service.test.svc.cluster.local
  subsets:
    - name: v1
      labels:
        version: v1
    - name: v2
      labels:
        version: v2
```
Create `VirtualService` with 3 routes splitted by HTTP header: `v1`, `v2`, no header.
```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: callme-service-route
spec:
  hosts:
    - callme-service.test.svc.cluster.local
  http:
    - match:
        - headers:
            X-Version:
              exact: v1
      route:
        - destination:
            host: callme-service.test.svc.cluster.local
            subset: v1
    - match:
        - headers:
            X-Version:
              exact: v2
      route:
        - destination:
            host: callme-service.test.svc.cluster.local
            subset: v2
    - route:
        - destination:
            host: callme-service.test.svc.cluster.local
            subset: v1
          weight: 20
        - destination:
            host: callme-service.test.svc.cluster.local
            subset: v2
          weight: 80
```
Run the following command to verify list of `VirtualService`.
```shell script
$ kubectl get vs -n test
NAME                   GATEWAYS   HOSTS                                     AGE
callme-service-route              [callme-service.test.svc.cluster.local]   24s
```

#### 5.b) Create Istio gateway
Create k8s in the root of project. Then create the file `istio.yml`.\
Create Istio `Gateway` available on "any" host and port 80.\
Omit creating `Gateway` if you are **not using your own Kubernetes instance**.
```yaml
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: microservices-gateway
spec:
  selector:
    istio: ingressgateway
  servers:
    - port:
        number: 80
        name: http
        protocol: HTTP
      hosts:
        - "*"
```
Then apply it using the following command.
```shell script
$ kubectl apply -f k8s/istio.yaml
```

Create another file `istio-with-gateway.yaml` in directory callme-service/k8s.\
Add Istio `VirtualService` that references to `microservices-gateway` `Gateway`.
```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: callme-service-gateway-route
spec:
  hosts:
    - "*"
  gateways:
    - microservices-gateway
  http:
    - match:
        - headers:
            X-Version:
              exact: v1
          uri:
            prefix: "/callme"
      rewrite:
        uri: " "
      route:
        - destination:
            host: callme-service.test.svc.cluster.local
            subset: v1
    - match:
        - uri:
            prefix: "/callme"
          headers:
            X-Version:
              exact: v2
      rewrite:
        uri: " "
      route:
        - destination:
            host: callme-service.test.svc.cluster.local
            subset: v2
    - match:
        - uri:
            prefix: "/callme"
      rewrite:
        uri: " "
      route:
        - destination:
            host: callme-service.test.svc.cluster.local
            subset: v1
          weight: 20
        - destination:
            host: callme-service.test.svc.cluster.local
            subset: v2
          weight: 80
```

Let's verify list of `VirtualService`.
```shell script
$ kubectl get vs -n test
NAME                           GATEWAYS                  HOSTS                                     AGE
callme-service-gateway-route   [microservices-gateway]   [*]                                       45s
callme-service-route                                     [callme-service.test.svc.cluster.local]   15m
```

Verify address of `istio-ingressgateway` in `istio-system` namespace.
```shell script
kubectl get svc -n istio-system
NAME                   TYPE           CLUSTER-IP       EXTERNAL-IP   PORT(S)                                                      AGE
istio-ingressgateway   LoadBalancer   10.110.87.104    localhost     15021:31846/TCP,80:31466/TCP,443:30947/TCP,15443:30736/TCP   41d
istiod                 ClusterIP      10.106.253.175   <none>        15010/TCP,15012/TCP,443/TCP,15014/TCP,853/TCP                41d
prometheus             ClusterIP      10.108.45.212    <none>        9090/TCP                                                     41d
```

Let's `GET /callme/ping` endpoint with header `X-Version=v1`, `X-Version=v2` or `X-Version=null` 
```shell script
$ curl http://localhost/callme/callme/ping -H "X-Version:v1"
callme-service(v1): callme-deployment-v1-57d8c69586-m67f5 in test
$ curl http://localhost/callme/callme/ping -H "X-Version:v2"
callme-service(v2): callme-deployment-v2-774f46f699-tcpw8 in test
$ curl http://localhost/callme/callme/ping
callme-service(v2): callme-deployment-v2-774f46f699-tcpw8 in test
$ curl http://localhost/callme/callme/ping
callme-service(v1): callme-deployment-v1-57d8c69586-m67f5 in test
```

#### 5.c) Inter-service communication
Go to caller-service/src/main/java/pl/piomin/samples/kubernetes/controller directory.\
Edit file `CallerController.java`. Then change existing endpoint implementation of `GET \caller\ping` endpoint.\
You need to add `@RequestHeader`, invoke method `callme(version)`, and change return statement by adding response from callme-service.\
The final implementation is visible below.
```java
@RestController
@RequestMapping("/caller")
public class CallerController {
    
    // ...

    @GetMapping("/ping")
    public String ping(@RequestHeader(name = "X-Version", required = false) String version) {
        String callme = callme(version);
        return appName + "(" + appVersion.getVersionLabel() + "): " + podName + " in " + podNamespace
                + " is calling " + callme;
    }
    
    // ...

}
```

Deploy caller-service. Go to caller-service and run Skaffold: `skaffold dev -n test --port-forward`.\
The starting version of Istio manifest `istio.yaml` is available in directory caller-service/k8s. Use it in the beginning. We will change it in the next steps.
Then verify list of Istio virtual services.
```shell script
$ kubectl get vs -n test
NAME                           GATEWAYS                  HOSTS                                     AGE
caller-service-gateway-route   [microservices-gateway]   [*]                                       24s
callme-service-gateway-route   [microservices-gateway]   [*]                                       64m
callme-service-route                                     [callme-service.test.svc.cluster.local]   79m
```

Then send some test requests with header `X-Version` set to `v1`, `v2` or not set.
```shell script
$ curl http://localhost/caller/caller/ping -H "X-Version:v1"
caller-service(v1): caller-deployment-v1-54f6486b5-4ltpw in test is calling callme-service(v1): callme-deployment-v1-64cb6c744d-9z7w2 in test
$ curl http://localhost/caller/caller/ping -H "X-Version:v2"
caller-service(v2): caller-deployment-v2-67969c6cc6-rvsk4 in test is calling callme-service(v2): callme-deployment-v2-864494769c-lm7sg in test
$ curl http://localhost/caller/caller/ping
caller-service(v2): caller-deployment-v2-879ffc844-nqsd6 in test is calling callme-service(v2): callme-deployment-v2-864494769c-lm7sg in test
$ curl http://localhost/caller/caller/ping
caller-service(v2): caller-deployment-v2-879ffc844-nqsd6 in test is calling callme-service(v1): callme-deployment-v1-64cb6c744d-9z7w2 in test
```

#### 5.d) Faults and timeouts
Go to callme-service/k8s. Edit `istio.yml` file.
Add the following code to section `spec.http[0].route` defined for the `v1` version.
```yaml
fault:
  abort:
    percentage:
      value: 50
    httpStatus: 400
```
Similarly, add he following code to section `spec.http[1].route` defined for the `v2` version.
```yaml
fault:
  delay:
    percentage:
      value: 50
    fixedDelay: 3s
```
Leave third route without any changes. Save files, the changes are applied automatically by skaffold.
Let's call endpoint with `v1` version. Since 50% of requests are finished with HTTP 400 you may repeat the request several times. \
```shell script
$ curl http://localhost/caller/caller/ping -H "X-Version:v1"
{"timestamp":"2020-09-02T13:10:43.136+00:00","status":500,"error":"Internal Server Error","message":"","path":"/caller/ping"}
```
You should see the following log in caller-service.
```
2020-09-02 13:09:54.619 ERROR 1 --- [nio-8080-exec-6] o.a.c.c.C.[.[.[/].[dispatcherServlet]    :
 Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed; nested exception is org.springfra
mework.web.client.HttpClientErrorException$BadRequest: 400 Bad Request: [fault filter abort]] with root cause
```

Let's call endpoint with `v1` version. Since 50% of requests are delayed 3s you may repeat the request several times.
```shell script
$ curl http://localhost/caller/caller/ping -H "X-Version:v2" -w "\nTime: %{time_total}\n" -v
*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 80 (#0)
> GET /caller/caller/ping HTTP/1.1
> Host: localhost
> User-Agent: curl/7.55.1
> Accept: */*
> X-Version:v2
>
< HTTP/1.1 200 OK
< content-type: text/plain;charset=UTF-8
< content-length: 142
< date: Wed, 02 Sep 2020 13:24:35 GMT
< x-envoy-upstream-service-time: 3022
< server: istio-envoy
<
caller-service(v2): caller-deployment-v2-7d57f6799c-4h46t in test is calling callme-service(v2): callme-deployment-v2-84f947d86d-lgjqq in test* Connecti
on #0 to host localhost left intact

Time: 3,093000
```
There is fault injection in the following request, that does not contain version.
```shell script
$ curl http://localhost/caller/caller/ping
```

Go to caller-service/k8s. Edit file `istio.yml`.
Add the following line to the section `spec.http[0]`. Then save changes.
```yaml
timeout: 1s
```
Let's call caller-service with `v2`. The HTTP 504 Gateway Timeout is after 1s.
```shell script
$ curl http://localhost/caller/caller/ping -H "X-Version:v2" -w "\nTime: %{time_total}\n" -v
*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 80 (#0)
> GET /caller/caller/ping HTTP/1.1
> Host: localhost
> User-Agent: curl/7.55.1
> Accept: */*
> X-Version:v2
>
< HTTP/1.1 504 Gateway Timeout
< content-length: 24
< content-type: text/plain
< date: Wed, 02 Sep 2020 13:34:14 GMT
< server: istio-envoy
<
upstream request timeout* Connection #0 to host localhost left intact

Time: 1,094000
```
Before the next step let's enable Envoy's access logs.
```shell script
$ istioctl install --set meshConfig.accessLogFile="/dev/stdout"
```

Go to caller-service/k8s. Edit file `istio.yaml`.
Add the following line to the section `spec.http[0]`. Then save changes.
```yaml
retries:
  attempts: 3
  retryOn: 5xx
```

Let's call endpoint caller-service with `v1`.
```shell script
$ curl http://localhost/caller/caller/ping -H "X-Version:v1"
caller-service(v1): caller-deployment-v1-78988d4cfb-tqhqh in test is calling callme-service(v1): callme-deployment-v1-6b5f5fdfb9-qtq52 in test
```

Go to the logs available on the terminal with `skaffold dev` for caller-service.\
Although we receive a proper value with HTTP 200, the request has been retried by Istio.
```
[caller-deployment-v1-78988d4cfb-tqhqh istio-proxy] [2020-09-02T13:56:58.788Z] "GET /callme/ping HTTP/1.1" 400 FI "-" "-" 0 18 0 - "-" "Java/11
.0.6" "1bf27689-98bc-4058-819e-5750c21d1f8c" "callme-service:8080" "-" - - 10.106.101.0:8080 10.1.2.21:54776 - -
[caller-deployment-v1-78988d4cfb-tqhqh istio-proxy] [2020-09-02T13:55:59.609Z] "- - -" 0 - "-" "-" 4425 783 59196 - "-" "-" "-" "-" "127.0.0.1:
8080" inbound|8080||caller-service.test.svc.cluster.local 127.0.0.1:52466 10.1.2.21:8080 10.1.2.14:57982 outbound_.8080_.v1_.caller-service.test.svc.clu
ster.local -
[caller-deployment-v1-78988d4cfb-tqhqh istio-proxy] [2020-09-02T13:56:58.831Z] "GET /callme/ping HTTP/1.1" 200 - "-" "-" 0 65 5 5 "-" "Java/11.
0.6" "b133ee29-a1d9-415b-bd3a-4364d50119f9" "callme-service:8080" "10.1.2.22:8080" outbound|8080|v1|callme-service.test.svc.cluster.local 10.1.2.21:5069
2 10.106.101.0:8080 10.1.2.21:54776 - -
[caller-deployment-v1-78988d4cfb-tqhqh istio-proxy] [2020-09-02T13:55:58.327Z] "- - -" 0 - "-" "-" 1475 257 61516 - "-" "-" "-" "-" "127.0.0.1:
8080" inbound|8080||caller-service.test.svc.cluster.local 127.0.0.1:52438 10.1.2.21:8080 10.1.2.14:57954 outbound_.8080_.v1_.caller-service.test.svc.clu
ster.local -
```

Go to caller-service/k8s. Edit file `istio.yaml`.
Add the following line to the section `spec.http[1]`. Then save changes.
```yaml
retries:
  attempts: 3
  retryOn: 5xx
  perTryTimeout: 0.33s
```
Then call caller-service `v2`.
```shell script
$ curl http://localhost/caller/caller/ping -H "X-Version:v2" -w "\nTime: %{time_total}\n" -v
```

Here's the final `VirtualService` used in the part 5 of our exercise.
```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: caller-service-gateway-route
spec:
  hosts:
    - "*"
  gateways:
    - microservices-gateway
  http:
    - match:
        - headers:
            X-Version:
              exact: v1
          uri:
            prefix: "/caller"
      rewrite:
        uri: " "
      route:
        - destination:
            host: caller-service.test.svc.cluster.local
            subset: v1
      retries:
        attempts: 3
        retryOn: 5xx
    - match:
        - uri:
            prefix: "/caller"
          headers:
            X-Version:
              exact: v2
      rewrite:
        uri: " "
      route:
        - destination:
            host: caller-service.test.svc.cluster.local
            subset: v2
      timeout: 1s
      retries:
        attempts: 3
        retryOn: 5xx
        perTryTimeout: 0.33s
    - match:
        - uri:
            prefix: "/caller"
      rewrite:
        uri: " "
      route:
        - destination:
            host: caller-service.test.svc.cluster.local
            subset: v1
          weight: 20
        - destination:
            host: caller-service.test.svc.cluster.local
            subset: v2
          weight: 80
```