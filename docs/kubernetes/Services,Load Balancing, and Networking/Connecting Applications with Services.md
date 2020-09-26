# Connecting Applications with Services

## The Kubernetes model for connecting containers 

现在您有了一个连续运行的复制应用程序，您可以在网络上公开它。在讨论Kubernetes的联网方法之前，有必要将其与Docker的“正常”网络工作方式进行对比。

默认情况下，Docker使用主机专用网络，因此容器只能在同一台机器上与其他容器通信。为了让Docker容器跨节点通信，必须在机器自己的IP地址上分配端口，然后将这些端口转发或代理到容器。这显然意味着容器必须非常小心地协调它们使用的端口，或者必须动态地分配端口。

在多个开发人员或团队之间协调容器的端口分配非常困难，并且使用户暴露在集群级别的问题中并脱离了用户的控制。Kubernetes假设pod可以与其他pod进行通信，而不管它们部署在哪个主机上。Kubernetes为每个pod提供了自己的集群专用IP地址，因此您不需要显式地在pod之间创建链接或将容器端口映射到主机端口。这意味着在同一个Pod内部的容器可以使用localhost访问其他容器的端口，集群中的所有Pod都可以在不使用NAT的情况下相互查看。本文的其余部分将详细介绍如何在这种网络模型上运行可靠的服务。

本指南使用一个简单的nginx服务器来演示概念证明。

## Exposing pods to the cluster 将pods暴露到集群

我们在前面的示例中已经演示过了，但是让我们再次演示一下，并将重点放在网络视角上。创建一个nginx Pod，注意它有一个容器端口的规范：
service/networking/run-my-nginx.yaml 
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-nginx
spec:
  selector:
    matchLabels:
      run: my-nginx
  replicas: 2
  template:
    metadata:
      labels:
        run: my-nginx
    spec:
      containers:
      - name: my-nginx
        image: nginx
        ports:
        - containerPort: 80
```

这样就可以从集群中的任何节点访问它。检查Pod正在运行的节点：

```shell
kubectl apply -f ./run-my-nginx.yaml
kubectl get pods -l run=my-nginx -o wide

#output
NAME                        READY     STATUS    RESTARTS   AGE       IP            NODE
my-nginx-3800858182-jr4a2   1/1       Running   0          13s       10.244.3.4    kubernetes-minion-905m
my-nginx-3800858182-kna2y   1/1       Running   0          13s       10.244.2.5    kubernetes-minion-ljyd
```

Check your pods' IPs:

```yaml
kubectl get pods -l run=my-nginx -o yaml | grep podIP
    podIP: 10.244.3.4
    podIP: 10.244.2.5
```

您应该能够通过ssh连接到集群中的任何节点并curl这两个ip。请注意，容器没有使用节点的80端口，也没有任何特殊的NAT规则来将流量路由到pod。这意味着您可以在同一节点上运行多个nginx pods，所有这些都使用相同的containerPort，并使用IP从集群中的任何其他pod或节点访问它们。与Docker一样，端口仍然可以发布到主机节点的接口上，但是由于kubernetes的网络模型的存在，这根本就不需要这样做了。

You can read more about [how we achieve this](https://kubernetes.io/docs/concepts/cluster-administration/networking/#how-to-achieve-this) if you're curious

## Creating a Service

所以我们在一个flat、集群范围的地址空间中运行nginx。理论上，您可以直接与这些pod通信，但是当一个节点死亡时会发生什么呢？这些pod也随之消亡，Deployment会随之创建一个有不同IP的新的pod。这就是Service解决的问题。

Kubernetes服务是一种抽象，它定义了运行在集群中某处的逻辑pod集，它们都提供相同的功能。创建时，每个服务都被分配一个唯一的IP地址（也称为clusterIP）。此地址与服务的生命周期相关联，并且在服务处于活动状态时不会更改。Pods可以配置为与服务对话，并且知道与服务的通信将自动负载平衡到属于服务成员的某个pod。

您可以使用kubectl expose为2个nginx副本创建服务：
```shell
kubectl expose deployment/my-nginx
```

这相当于kubectl apply-f以下yaml：
service/networking/nginx-svc.yaml 

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-nginx
  labels:
    run: my-nginx
spec:
  ports:
  - port: 80
    protocol: TCP
  selector:
    run: my-nginx
```

此规范将创建一个服务，该服务的目标是有`run: my-nginx`标签的任何Pod上的TCP 80端口，并将其公开在一个抽象的服务端口上（targetPort:是容器接受流量的端口，port:是抽象的服务端口，可以是其他pods用来访问服务的任何端口）。你可以查看[服务API](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#service-v1-core)对象以查看服务定义中支持的字段列表。检查您的服务：
```shell
kubectl get svc my-nginx

#output
NAME       TYPE        CLUSTER-IP     EXTERNAL-IP   PORT(S)   AGE
my-nginx   ClusterIP   10.0.162.149   <none>        80/TCP    21s
```

如前所述，服务由一组pod组成。这些pod通过endpoints公开。服务的Selector将被持续计算，其选择结果将被POST到一个名字同样是`my-nginx`的Endpoints对象。当一个Pod死亡时，它会自动从端点移除，并且与服务选择器匹配的新Pod将自动添加到端点。检查端点，注意IP与第一步中创建的POD相同：

```shell
kubectl describe svc my-nginx

Name:                my-nginx
Namespace:           default
Labels:              run=my-nginx
Annotations:         <none>
Selector:            run=my-nginx
Type:                ClusterIP
IP:                  10.0.162.149
Port:                <unset> 80/TCP
Endpoints:           10.244.2.5:80,10.244.3.4:80
Session Affinity:    None
Events:              <none>
```

```shell
kubectl get ep my-nginx

NAME       ENDPOINTS                     AGE
my-nginx   10.244.2.5:80,10.244.3.4:80   1m
```

现在您应该能够从集群中的任何节点将nginx服务curl到`<CLUSTER-IP>：<PORT>`。请注意，服务IP是完全虚拟的，它从不触网。如果您想知道这是如何工作的，可以阅读有关[服务代理](https://kubernetes.io/docs/concepts/services-networking/service/#virtual-ips-and-service-proxies)的更多信息。

## Accessing the Service 

Kubernetes支持两种主要的查找服务模式: 环境变量和DNS。前者是开箱即用的，而后者需要[CoreDNS集群插件](https://releases.k8s.io/master/cluster/addons/dns/coredns)。

> 注意：如果不需要服务环境变量（因为可能与预期的程序变量发生冲突，要处理的变量太多，仅使用DNS等），可以通过在pod规范中将enableServiceLinks标志设置为false来禁用此模式

### Environment Variables

当一个Pod在一个节点上运行时，kubelet会在Pod里为每个活动Service添加一组环境变量。这会导致顺序问题。要了解原因，请检查正在运行的nginx Pods的环境（您的Pod名称将不同）：

```shell
kubectl exec my-nginx-3800858182-jr4a2 -- printenv | grep SERVICE

KUBERNETES_SERVICE_HOST=10.0.0.1
KUBERNETES_SERVICE_PORT=443
KUBERNETES_SERVICE_PORT_HTTPS=443
```

可以注意到这里没有上面创建的Service。这是因为您在服务之前创建了复制副本。这样做的另一个缺点是调度器可能会将两个pod放在同一台机器上，如果机器死机，整个服务就会被关闭。我们可以通过杀死2个pod并等待Deployment重新创建它们来正确地完成这项工作。这一次，Service在两个复制副本之前就已经存在。这样Service就会在scheduler-level在Pod上调度（前提是所有节点的容量相等），而且Pod会设置适当的环境变量：

```shell
kubectl scale deployment my-nginx --replicas=0; kubectl scale deployment my-nginx --replicas=2;

kubectl get pods -l run=my-nginx -o wide

#output
NAME                        READY     STATUS    RESTARTS   AGE     IP            NODE
my-nginx-3800858182-e9ihh   1/1       Running   0          5s      10.244.2.7    kubernetes-minion-ljyd
my-nginx-3800858182-j4rm4   1/1       Running   0          5s      10.244.3.8    kubernetes-minion-905m
```

你可能会注意到这些Pod有不同的名字，因为它们被杀死并被重新创造了。
```shell
kubectl exec my-nginx-3800858182-e9ihh -- printenv | grep SERVICE

#output
KUBERNETES_SERVICE_PORT=443
MY_NGINX_SERVICE_HOST=10.0.162.149
KUBERNETES_SERVICE_HOST=10.0.0.1
MY_NGINX_SERVICE_PORT=80
KUBERNETES_SERVICE_PORT_HTTPS=443
```

### DNS
Kubernetes提供了一个DNS集群插件服务，可以自动为其他服务分配DNS名称。您可以检查它是否正在群集上运行：

```shell
kubectl get services kube-dns --namespace=kube-system

#output
NAME       TYPE        CLUSTER-IP   EXTERNAL-IP   PORT(S)         AGE
kube-dns   ClusterIP   10.0.0.10    <none>        53/UDP,53/TCP   8m
```

本节的其余部分将假设您有一个稳定的IP（my-nginx）的服务，以及一个为该IP分配了名称的DNS服务器。这里我们使用CoreDNS cluster addon（应用程序名kube-dns），因此您可以使用标准方法（例如`gethostbyname()`）从集群中的任何pod里与服务对话。如果CoreDNS没有运行，您可以参考[CoreDNS自述文件](https://github.com/coredns/deployment/tree/master/kubernetes)或[安装CoreDNS](https://kubernetes.io/docs/tasks/administer-cluster/coredns/#installing-coredns)来启用它。让我们运行另一个curl应用程序来测试：

```shell
kubectl run curl --image=radial/busyboxplus:curl -i --tty

#output
Waiting for pod default/curl-131556218-9fnch to be running, status is Pending, pod ready: false
Hit enter for command prompt
```

Then, hit enter and run nslookup my-nginx:

```shell
[ root@curl-131556218-9fnch:/ ]$ nslookup my-nginx
Server:    10.0.0.10
Address 1: 10.0.0.10

Name:      my-nginx
Address 1: 10.0.162.149
```


## Securing the Service

到目前为止，我们只从集群内部访问nginx服务器。在向internet公开服务之前，您需要确保通信通道是安全的。为此，您需要：
- https的自签名证书（除非您已经有身份证书）
- 配置了使用证书的nginx服务器
- 一个可以让pod访问证书的[秘钥](https://kubernetes.io/docs/concepts/configuration/secret/)

您可以从[nginx https示例](https://github.com/kubernetes/examples/tree/master/staging/https-nginx/)中获取所有这些信息。这需要安装go和make工具。如果不想安装这些，请稍后按照下面手动步骤操作。简而言之：

```shell
make keys KEY=/tmp/nginx.key CERT=/tmp/nginx.crt
kubectl create secret tls nginxsecret --key /tmp/nginx.key --cert /tmp/nginx.crt

#output
secret/nginxsecret created
```

```shell
kubectl get secrets
#output
NAME                  TYPE                                  DATA      AGE
default-token-il9rc   kubernetes.io/service-account-token   1         1d
nginxsecret           kubernetes.io/tls                     2         1m
```

And also the configmap:

```shell
kubectl create configmap nginxconfigmap --from-file=default.conf
#output
configmap/nginxconfigmap created

kubectl get configmaps
#output
NAME             DATA   AGE
nginxconfigmap   1      114s
```

#### 手动操作步骤
以下是手动安装步骤，以防你在运行make（例如在windows上）时遇到问题：

```shell
# Create a public private key pair
openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout /d/tmp/nginx.key -out /d/tmp/nginx.crt -subj "/CN=my-nginx/O=my-nginx"
# Convert the keys to base64 encoding
cat /d/tmp/nginx.crt | base64
cat /d/tmp/nginx.key | base64
```

使用前面命令的输出创建一个yaml文件，如下所示。base64编码的值应该全部在一行上。

```yaml
apiVersion: "v1"
kind: "Secret"
metadata:
  name: "nginxsecret"
  namespace: "default"
type: kubernetes.io/tls
data:
  tls.crt: "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURIekNDQWdlZ0F3SUJBZ0lKQUp5M3lQK0pzMlpJTUEwR0NTcUdTSWIzRFFFQkJRVUFNQ1l4RVRBUEJnTlYKQkFNVENHNW5hVzU0YzNaak1SRXdEd1lEVlFRS0V3aHVaMmx1ZUhOMll6QWVGdzB4TnpFd01qWXdOekEzTVRKYQpGdzB4T0RFd01qWXdOekEzTVRKYU1DWXhFVEFQQmdOVkJBTVRDRzVuYVc1NGMzWmpNUkV3RHdZRFZRUUtFd2h1CloybHVlSE4yWXpDQ0FTSXdEUVlKS29aSWh2Y05BUUVCQlFBRGdnRVBBRENDQVFvQ2dnRUJBSjFxSU1SOVdWM0IKMlZIQlRMRmtobDRONXljMEJxYUhIQktMSnJMcy8vdzZhU3hRS29GbHlJSU94NGUrMlN5ajBFcndCLzlYTnBwbQppeW1CL3JkRldkOXg5UWhBQUxCZkVaTmNiV3NsTVFVcnhBZW50VWt1dk1vLzgvMHRpbGhjc3paenJEYVJ4NEo5Ci82UVRtVVI3a0ZTWUpOWTVQZkR3cGc3dlVvaDZmZ1Voam92VG42eHNVR0M2QURVODBpNXFlZWhNeVI1N2lmU2YKNHZpaXdIY3hnL3lZR1JBRS9mRTRqakxCdmdONjc2SU90S01rZXV3R0ljNDFhd05tNnNTSzRqYUNGeGpYSnZaZQp2by9kTlEybHhHWCtKT2l3SEhXbXNhdGp4WTRaNVk3R1ZoK0QrWnYvcW1mMFgvbVY0Rmo1NzV3ajFMWVBocWtsCmdhSXZYRyt4U1FVQ0F3RUFBYU5RTUU0d0hRWURWUjBPQkJZRUZPNG9OWkI3YXc1OUlsYkROMzhIYkduYnhFVjcKTUI4R0ExVWRJd1FZTUJhQUZPNG9OWkI3YXc1OUlsYkROMzhIYkduYnhFVjdNQXdHQTFVZEV3UUZNQU1CQWY4dwpEUVlKS29aSWh2Y05BUUVGQlFBRGdnRUJBRVhTMW9FU0lFaXdyMDhWcVA0K2NwTHI3TW5FMTducDBvMm14alFvCjRGb0RvRjdRZnZqeE04Tzd2TjB0clcxb2pGSW0vWDE4ZnZaL3k4ZzVaWG40Vm8zc3hKVmRBcStNZC9jTStzUGEKNmJjTkNUekZqeFpUV0UrKzE5NS9zb2dmOUZ3VDVDK3U2Q3B5N0M3MTZvUXRUakViV05VdEt4cXI0Nk1OZWNCMApwRFhWZmdWQTRadkR4NFo3S2RiZDY5eXM3OVFHYmg5ZW1PZ05NZFlsSUswSGt0ejF5WU4vbVpmK3FqTkJqbWZjCkNnMnlwbGQ0Wi8rUUNQZjl3SkoybFIrY2FnT0R4elBWcGxNSEcybzgvTHFDdnh6elZPUDUxeXdLZEtxaUMwSVEKQ0I5T2wwWW5scE9UNEh1b2hSUzBPOStlMm9KdFZsNUIyczRpbDlhZ3RTVXFxUlU9Ci0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0K"
  tls.key: "LS0tLS1CRUdJTiBQUklWQVRFIEtFWS0tLS0tCk1JSUV2UUlCQURBTkJna3Foa2lHOXcwQkFRRUZBQVNDQktjd2dnU2pBZ0VBQW9JQkFRQ2RhaURFZlZsZHdkbFIKd1V5eFpJWmVEZWNuTkFhbWh4d1NpeWF5N1AvOE9ta3NVQ3FCWmNpQ0RzZUh2dGtzbzlCSzhBZi9WemFhWm9zcApnZjYzUlZuZmNmVUlRQUN3WHhHVFhHMXJKVEVGSzhRSHA3VkpMcnpLUC9QOUxZcFlYTE0yYzZ3MmtjZUNmZitrCkU1bEVlNUJVbUNUV09UM3c4S1lPNzFLSWVuNEZJWTZMMDUrc2JGQmd1Z0ExUE5JdWFubm9UTWtlZTRuMG4rTDQKb3NCM01ZUDhtQmtRQlAzeE9JNHl3YjREZXUraURyU2pKSHJzQmlIT05Xc0RadXJFaXVJMmdoY1kxeWIyWHI2UAozVFVOcGNSbC9pVG9zQngxcHJHclk4V09HZVdPeGxZZmcvbWIvNnBuOUYvNWxlQlkrZStjSTlTMkQ0YXBKWUdpCkwxeHZzVWtGQWdNQkFBRUNnZ0VBZFhCK0xkbk8ySElOTGo5bWRsb25IUGlHWWVzZ294RGQwci9hQ1Zkank4dlEKTjIwL3FQWkUxek1yall6Ry9kVGhTMmMwc0QxaTBXSjdwR1lGb0xtdXlWTjltY0FXUTM5SjM0VHZaU2FFSWZWNgo5TE1jUHhNTmFsNjRLMFRVbUFQZytGam9QSFlhUUxLOERLOUtnNXNrSE5pOWNzMlY5ckd6VWlVZWtBL0RBUlBTClI3L2ZjUFBacDRuRWVBZmI3WTk1R1llb1p5V21SU3VKdlNyblBESGtUdW1vVlVWdkxMRHRzaG9reUxiTWVtN3oKMmJzVmpwSW1GTHJqbGtmQXlpNHg0WjJrV3YyMFRrdWtsZU1jaVlMbjk4QWxiRi9DSmRLM3QraTRoMTVlR2ZQegpoTnh3bk9QdlVTaDR2Q0o3c2Q5TmtEUGJvS2JneVVHOXBYamZhRGR2UVFLQmdRRFFLM01nUkhkQ1pKNVFqZWFKClFGdXF4cHdnNzhZTjQyL1NwenlUYmtGcVFoQWtyczJxWGx1MDZBRzhrZzIzQkswaHkzaE9zSGgxcXRVK3NHZVAKOWRERHBsUWV0ODZsY2FlR3hoc0V0L1R6cEdtNGFKSm5oNzVVaTVGZk9QTDhPTm1FZ3MxMVRhUldhNzZxelRyMgphRlpjQ2pWV1g0YnRSTHVwSkgrMjZnY0FhUUtCZ1FEQmxVSUUzTnNVOFBBZEYvL25sQVB5VWs1T3lDdWc3dmVyClUycXlrdXFzYnBkSi9hODViT1JhM05IVmpVM25uRGpHVHBWaE9JeXg5TEFrc2RwZEFjVmxvcG9HODhXYk9lMTAKMUdqbnkySmdDK3JVWUZiRGtpUGx1K09IYnRnOXFYcGJMSHBzUVpsMGhucDBYSFNYVm9CMUliQndnMGEyOFVadApCbFBtWmc2d1BRS0JnRHVIUVV2SDZHYTNDVUsxNFdmOFhIcFFnMU16M2VvWTBPQm5iSDRvZUZKZmcraEppSXlnCm9RN3hqWldVR3BIc3AyblRtcHErQWlSNzdyRVhsdlhtOElVU2FsbkNiRGlKY01Pc29RdFBZNS9NczJMRm5LQTQKaENmL0pWb2FtZm1nZEN0ZGtFMXNINE9MR2lJVHdEbTRpb0dWZGIwMllnbzFyb2htNUpLMUI3MkpBb0dBUW01UQpHNDhXOTVhL0w1eSt5dCsyZ3YvUHM2VnBvMjZlTzRNQ3lJazJVem9ZWE9IYnNkODJkaC8xT2sybGdHZlI2K3VuCnc1YytZUXRSTHlhQmd3MUtpbGhFZDBKTWU3cGpUSVpnQWJ0LzVPbnlDak9OVXN2aDJjS2lrQ1Z2dTZsZlBjNkQKckliT2ZIaHhxV0RZK2Q1TGN1YSt2NzJ0RkxhenJsSlBsRzlOZHhrQ2dZRUF5elIzT3UyMDNRVVV6bUlCRkwzZAp4Wm5XZ0JLSEo3TnNxcGFWb2RjL0d5aGVycjFDZzE2MmJaSjJDV2RsZkI0VEdtUjZZdmxTZEFOOFRwUWhFbUtKCnFBLzVzdHdxNWd0WGVLOVJmMWxXK29xNThRNTBxMmk1NVdUTThoSDZhTjlaMTltZ0FGdE5VdGNqQUx2dFYxdEYKWSs4WFJkSHJaRnBIWll2NWkwVW1VbGc9Ci0tLS0tRU5EIFBSSVZBVEUgS0VZLS0tLS0K"
```

现在使用该文件创建secret：
```shell
kubectl apply -f nginxsecrets.yaml
kubectl get secrets

#output
NAME                  TYPE                                  DATA      AGE
default-token-il9rc   kubernetes.io/service-account-token   1         1d
nginxsecret           kubernetes.io/tls                     2         1m
```

现在修改nginx副本以使用secret中的证书启动https服务器，并修改服务以公开两个端口（80和443）：
service/networking/nginx-secure-app.yaml

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-nginx
  labels:
    run: my-nginx
spec:
  type: NodePort
  ports:
  - port: 8080
    targetPort: 80
    protocol: TCP
    name: http
  - port: 443
    protocol: TCP
    name: https
  selector:
    run: my-nginx
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-nginx
spec:
  selector:
    matchLabels:
      run: my-nginx
  replicas: 1
  template:
    metadata:
      labels:
        run: my-nginx
    spec:
      volumes:
      - name: secret-volume
        secret:
          secretName: nginxsecret
      - name: configmap-volume
        configMap:
          name: nginxconfigmap
      containers:
      - name: nginxhttps
        image: bprashanth/nginxhttps:1.0
        ports:
        - containerPort: 443
        - containerPort: 80
        volumeMounts:
        - mountPath: /etc/nginx/ssl
          name: secret-volume
        - mountPath: /etc/nginx/conf.d
          name: configmap-volume
```

关于nginx安全应用程序清单的注意事项：
- 它在同一个文件中包含了Deployment和Service规范。
- nginx服务器在端口80上提供HTTP流量，在443上提供HTTPS流量，nginx服务公开这两个端口。
- 每个容器都可以通过安装在/etc/nginx/ssl上的卷获取到密钥。这是在nginx服务器启动之前设置的。

```shell
kubectl delete deployments,svc my-nginx; kubectl create -f ./nginx-secure-app.yaml
```

此时，您可以从任何节点访问nginx服务器。

```shell
kubectl get pods -o yaml | grep -i podip
    podIP: 10.244.3.5
node $ curl -k https://10.244.3.5
...
<h1>Welcome to nginx!</h1>
```

注意我们在最后一步中是如何向curl提供-k参数的，这是因为在证书生成时我们对运行nginx的pod一无所知，所以我们必须告诉curl忽略CName不匹配。通过创建服务，我们将证书中使用的CName与pods在服务查找(Service lookup)期间使用的实际DNS名称相链接。让我们从一个pod中测试一下（简单起见重用同样的secret，pod只需要一个nginx.crt来访问服务）：
service/networking/curlpod.yaml 

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: curl-deployment
spec:
  selector:
    matchLabels:
      app: curlpod
  replicas: 1
  template:
    metadata:
      labels:
        app: curlpod
    spec:
      volumes:
      - name: secret-volume
        secret:
          secretName: nginxsecret
      containers:
      - name: curlpod
        command:
        - sh
        - -c
        - while true; do sleep 1; done
        image: radial/busyboxplus:curl
        volumeMounts:
        - mountPath: /etc/nginx/ssl
          name: secret-volume
```

```shell
kubectl apply -f ./curlpod.yaml
kubectl get pods -l app=curlpod

#output
NAME                               READY     STATUS    RESTARTS   AGE
curl-deployment-1515033274-1410r   1/1       Running   0          1m

kubectl exec curl-deployment-1515033274-1410r -- curl https://my-nginx --cacert /etc/nginx/ssl/tls.crt
...
<title>Welcome to nginx!</title>
...
```


## Exposing the Service

对于应用程序的某些部分，您可能希望将服务公开到外部IP地址。Kubernetes支持两种方法来实现这一点：NodePorts和LoadBalancers。在上一节中创建的服务已经使用了NodePort，所以如果您的节点有一个公共IP，那么nginx https副本就可以在internet上提供流量了。

```shell
kubectl get svc my-nginx -o yaml | grep nodePort -C 5
  uid: 07191fb3-f61a-11e5-8ae5-42010af00002
spec:
  clusterIP: 10.0.162.149
  ports:
  - name: http
    nodePort: 31704
    port: 8080
    protocol: TCP
    targetPort: 80
  - name: https
    nodePort: 32453
    port: 443
    protocol: TCP
    targetPort: 443
  selector:
    run: my-nginx
```

```shell
kubectl get nodes -o yaml | grep ExternalIP -C 1
    - address: 104.197.41.11
      type: ExternalIP
    allocatable:
--
    - address: 23.251.152.56
      type: ExternalIP
    allocatable:
...

$ curl https://<EXTERNAL-IP>:<NODE-PORT> -k
...
<h1>Welcome to nginx!</h1>
```


现在我们使用云负载平衡器来重新创建服务，只需将我的nginx服务类型从NodePort更改为LoadBalancer：
```shell
kubectl edit svc my-nginx
kubectl get svc my-nginx

#output
NAME       TYPE           CLUSTER-IP     EXTERNAL-IP        PORT(S)               AGE
my-nginx   LoadBalancer   10.0.162.149     xx.xxx.xxx.xxx     8080:30163/TCP        21s
```

```shell
curl https://<EXTERNAL-IP> -k
...
<title>Welcome to nginx!</title>
```

EXTERNAL-IP列中的IP地址是公共internet上可用的地址。CLUSTER-IP仅在集群/私有云网络内可用。

注意，在AWS上，LoadBalancer类型创建一个ELB，它使用（长）主机名，而不是IP。事实上，它太长了，不适合标准的`kubectl get svc`输出，所以您需要执行kubectl describe service my nginx才能看到它。你会看到这样的画面：

```shell
kubectl describe service my-nginx
...
LoadBalancer Ingress:   a320587ffd19711e5a37606cf4a74574-1142138393.us-east-1.elb.amazonaws.com
...
```

## What's next
Learn more about [Using a Service to Access an Application in a Cluster](https://kubernetes.io/docs/tasks/access-application-cluster/service-access-application-cluster/)
Learn more about [Connecting a Front End to a Back End Using a Service](https://kubernetes.io/docs/tasks/access-application-cluster/connecting-frontend-backend/)
Learn more about [Creating an External Load Balancer](https://kubernetes.io/docs/tasks/access-application-cluster/create-external-load-balancer/)