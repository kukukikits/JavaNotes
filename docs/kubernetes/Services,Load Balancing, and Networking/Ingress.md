# Ingress
*FEATURE STATE: Kubernetes v1.19 [stable]*

一个API对象，用于管理从外部访问集群服务的请求，通常是HTTP。

Ingress可以提供负载平衡、SSL终止和基于名称的虚拟主机

## Terminology 

为清楚起见，本指南定义了以下术语：
- 节点/Node：Kubernetes中的工作机，集群的一部分。
- 集群/Cluster：一组节点，运行了由Kubernetes管理的容器化应用程序。在本例中，以及在大多数常见的Kubernetes deployments中，集群中的节点不是公共互联网的一部分。
- 边缘路由器/Edge router：为您的集群实施防火墙策略的路由器。可以是由云提供商管理的网关，也可以是一个物理硬件。
- 集群网络/Cluster network：根据Kubernetes网络模型，一组逻辑或物理的链路，用于促进集群内的通信。
- 服务/Service：Kubernetes服务，使用标签选择器标识一组pod。除非另有说明，否则假设服务只有在集群网络中可路由的虚拟ip

## What is Ingress?

Ingress公开从集群外部到集群内服务的HTTP和HTTPS路由。流量路由由在Ingress(入口)资源上定义的规则控制。

Ingress可以配置外部可访问的url、负载平衡通信、终止SSL/TLS以及提供基于名称的虚拟主机。Ingress控制器负责实现Ingress，通常使用负载平衡器实现，但它也可以配置边缘路由器或其他前端来帮助处理流量。

Ingress不会暴露任意端口或协议。将HTTP和HTTPS以外的服务公开到internet通常使用类型为的Service.Type=NodePort或者Service.Type=LoadBalancer的Service

## Prerequisites

必须有Ingress控制器才能满足Ingress要求。只创建Ingress资源没有效果。

您可能需要部署一个Ingress控制器，比如ingres-nginx。您可以从许多[入口控制器](https://kubernetes.io/docs/concepts/services-networking/ingress-controllers)中选择。

理想情况下，所有入口控制器应符合参考规范。实际上，各种入口控制器的操作方式略有不同。

> :bookmark: 注意：请务必查看入口控制器的文档，以了解选择它的注意事项

## The Ingress resource
A minimal Ingress resource example:

service/networking/minimal-ingress.yaml

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: minimal-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
  - http:
      paths:
      - path: /testpath
        pathType: Prefix
        backend:
          service:
            name: test
            port:
              number: 80
```

与所有其他Kubernetes资源一样，Ingress需要apiVersion、kind和metadata字段。Ingress对象的名称必须是有效的DNS子域名。有关使用配置文件的信息，请参阅[部署应用程序](https://kubernetes.io/docs/tasks/run-application/run-stateless-application-deployment/)、[配置容器](https://kubernetes.io/docs/tasks/configure-pod-container/configure-pod-configmap/)、[管理资源](https://kubernetes.io/docs/concepts/cluster-administration/manage-deployment/)。Ingress经常根据Ingress控制器使用注释来配置一些选项，例如[重写目标注释](https://github.com/kubernetes/ingress-nginx/blob/master/docs/examples/rewrite/README.md)。不同的Ingress控制器支持不同的注释。请查看文档以了解您选择的入口控制器，了解支持哪些注释。

Ingress规范包含配置负载平衡器或代理服务器所需的所有信息。最重要的是，它包含和所有传入请求匹配的规则列表。Ingress资源仅支持用于定向HTTP（S）流量的规则

### Ingress rules
每个HTTP规则都包含以下信息：

- 可选主机。在本例中，没有指定主机，因此该规则适用于通过指定IP地址的所有入站HTTP流量。如果提供了主机（例如，foo.bar.com)，则规则适用于该主机。
- 路径列表（例如，/testpath），每个路径都有一个关联后端，该后端用`service.name`还有一个`service.port.name`或者`service.port.number`指定. 在负载平衡器将流量定向到引用的服务之前，主机和路径都必须与传入请求的内容匹配。
- 后端是服务和端口名称的组合，如服务文档中所述，或通过CRD自定义资源后端。对与规则的主机和路径匹配的HTTP（和HTTPS）请求被发送到列出的后端。

### DefaultBackend 

没有配置规则的Ingress将所有流量发送到一个默认后端。defaultBackend通常是Ingress控制器的一个配置选项，并且没有在您的Ingress资源中指定。

如果没有主机或路径与ingres对象中的HTTP请求匹配，则流量将路由到默认后端

### Resource backends 

`Resource backend`是一个与ingres对象有相同命名空间中的另一个Kubernetes资源的ObjectRef。Resource与Service是互斥的设置，如果两者都指定，会验证失败。`Resource backend`的常见用法是使用静态资产将数据输入到对象存储后端。

service/networking/ingress-resource-backend.yaml
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ingress-resource-backend
spec:
  defaultBackend:
    resource:
      apiGroup: k8s.example.com
      kind: StorageBucket
      name: static-assets
  rules:
    - http:
        paths:
          - path: /icons
            pathType: ImplementationSpecific
            backend:
              resource:
                apiGroup: k8s.example.com
                kind: StorageBucket
                name: icon-assets
```

After creating the Ingress above, you can view it with the following command:

```shell
kubectl describe ingress ingress-resource-backend

Name:             ingress-resource-backend
Namespace:        default
Address:
Default backend:  APIGroup: k8s.example.com, Kind: StorageBucket, Name: static-assets
Rules:
  Host        Path  Backends
  ----        ----  --------
  *
              /icons   APIGroup: k8s.example.com, Kind: StorageBucket, Name: icon-assets
Annotations:  <none>
Events:       <none>
```

### Path types

Ingress中的每个路径都必须具有相应的path type(路径类型)。不包含显式`pathType`的路径将无法验证。支持三种路径类型：

- 特定于实现/`ImplementationSpecific`：对于此路径类型，是否匹配取决于IngressClass。具体实现可以将其视为单独的pathType，也可以将其视为Prefix或Exact路径类型。
- 精确/Exact：精确匹配URL路径，并且区分大小写。
- 前缀/Prefix：按/分隔的URL路径前缀匹配。匹配区分大小写，并按照路径元素一个一个地进行匹配。路径元素是指路径中由/分隔后的字符串列表的元素。如果请求路径的前缀元素都是p，则请求与路径p匹配。
    > :warning: 注意：如果路径规则的最后一个元素是请求路径中最后一个元素的子字符串，则它不是匹配项（例如：/foo/bar 匹配/foo/bar/baz，但不匹配/foo/barbaz）

### Examples
```
Kind	Path(s) 	                Request path(s)	Matches?
Prefix	/	                        (all paths) 	Yes
Exact	/foo	                    /foo	        Yes
Exact	/foo	                    /bar	        No
Exact	/foo    	                /foo/	        No
Exact	/foo/   	                /foo	        No
Prefix	/foo	                    /foo, /foo/	    Yes
Prefix	/foo/	                    /foo, /foo/	    Yes
Prefix	/aaa/bb	                    /aaa/bbb	    No
Prefix	/aaa/bbb	                /aaa/bbb	    Yes
Prefix	/aaa/bbb/	                /aaa/bbb	    Yes, ignores trailing slash
Prefix	/aaa/bbb	                /aaa/bbb/	    Yes, matches trailing slash
Prefix	/aaa/bbb	                /aaa/bbb/ccc	Yes, matches subpath
Prefix	/aaa/bbb	                /aaa/bbbxyz	    No, does not match string prefix
Prefix	/, /aaa 	                /aaa/ccc	    Yes, matches /aaa prefix
Prefix	/, /aaa, /aaa/bbb	        /aaa/bbb	    Yes, matches /aaa/bbb prefix
Prefix	/, /aaa, /aaa/bbb	        /ccc	        Yes, matches / prefix
Prefix	/aaa	                    /ccc	        No, uses default backend
Mixed	/foo (Prefix), /foo (Exact)	/foo	        Yes, prefers Exact

```

#### Multiple matches 

在某些情况下，Ingress中的多个路径将匹配一个请求。在这种情况下，优先权将首先给予最长的匹配路径。如果两条路径仍然相等匹配，则具有Exact路径类型的路径将优先于Prefix路径类型

## Hostname wildcards 

主机可以是精确匹配（例如`foo.bar.com`）或通配符（例如`*.foo.com`)匹配. 精确匹配要求HTTP `host` header与`host`字段匹配。通配符匹配要求HTTP `host` header等于通配符规则的后缀。

```
Host	    Host header	        Match?
*.foo.com	bar.foo.com	        Matches based on shared suffix
*.foo.com	baz.bar.foo.com	    No match, wildcard only covers a single DNS label
*.foo.com	foo.com	            No match, wildcard only covers a single DNS label
```

service/networking/ingress-wildcard-host.yaml 
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ingress-wildcard-host
spec:
  rules:
  - host: "foo.bar.com"
    http:
      paths:
      - pathType: Prefix
        path: "/bar"
        backend:
          service:
            name: service1
            port:
              number: 80
  - host: "*.foo.com"
    http:
      paths:
      - pathType: Prefix
        path: "/foo"
        backend:
          service:
            name: service2
            port:
              number: 80
```

## Ingress class

Ingress可以由不同的控制器实现，通常有不同的配置。每个Ingress都应该指定一个类，也就是一个IngressClass resource的引用，这个类包含一些附加配置 —— 实现该类的控制器的名称。

service/networking/external-lb.yaml 
```yaml
apiVersion: networking.k8s.io/v1
kind: IngressClass
metadata:
  name: external-lb
spec:
  controller: example.com/ingress-controller
  parameters:
    apiGroup: k8s.example.com
    kind: IngressParameters
    name: external-lb
```

IngressClass资源包含一个可选的参数字段，为此类提供其他配置的引用。

### Deprecated annotation

在Kubernetes 1.18增加IngressClass资源和ingressClassName字段之前，使用`kubernetes.io/ingress.class`在Ingress上的类注释。这个注释从未被正式定义过，但得到了Ingress控制器的广泛支持。

ingress上较新的`ingressClassName`字段是对该注释的替代，但不是直接等效的。虽然注释通常用于引用应该实现Ingress的Ingress控制器的名称，但是该字段一个IngressClass resource的引用，这个IngressClass resource包含了附加的Ingress配置，以及Ingress控制器的名称。

### Default IngressClass

您可以将特定的IngressClass标记为集群默认。设置IngressClass的`ingressclass.kubernetes.io/is-default-class`注释为true，那么新的没有指定ingressClassName字段的Ingress会被分配这个默认IngressClass。 

> 警告：如果有多个IngressClass标记为群集的默认值，则admission controller会阻止创建未指定ingressClassName的新的IngressClass对象。通过确保集群中最多只有1个默认IngressClass来解决这个问题。

## Types of Ingress

### Ingress backed by a single Service 

现有的Kubernetes概念允许您公开单个服务（参见[备选方案](https://kubernetes.io/docs/concepts/services-networking/ingress/#alternatives)）。您也可以通过指定一个没有规则的默认后端来实现这一点。

service/networking/test-ingress.yaml
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: test-ingress
spec:
  defaultBackend:
    service:
      name: test
      port:
        number: 80
```

如果使用kubectl apply-f创建它，您应该能够查看刚刚添加的Ingress的状态：
```shell
kubectl get ingress test-ingress

NAME           CLASS         HOSTS   ADDRESS         PORTS   AGE
test-ingress   external-lb   *       203.0.113.123   80      59s
```

其中203.0.113.123是Ingress控制器为满足该Ingress而分配的IP。

> :warning: 注意：Ingress控制器和负载平衡器可能需要一两分钟来分配一个IP地址。在此之前，您经常会看到地址列为\<pending\>

### Simple fanout
fanout配置根据请求的http uri将流量从单个IP地址路由到多个服务。Ingress允许您将负载平衡器的数量保持在最小值。例如，设置如下：

```
                                                                                     ----→ Pod
                                                                                    /
                                               --------→ /foo --→ Service service1:4200
         Ingress-managed                      /                                     \
client ------------------→ Ingress, 178.91.123.132                                   ----→ Pod
          load balancer                       \
                                               --------→ /bar --→ Service service2:8080
                                                                                     \
                                                                                      ----→ Pod
```

那么上面的配置就需要定义如下的Ingress:

service/networking/simple-fanout-example.yaml 

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: simple-fanout-example
spec:
  rules:
  - host: foo.bar.com
    http:
      paths:
      - path: /foo
        pathType: Prefix
        backend:
          service:
            name: service1
            port:
              number: 4200
      - path: /bar
        pathType: Prefix
        backend:
          service:
            name: service2
            port:
              number: 8080
```

When you create the Ingress with `kubectl apply -f`:
```shell
kubectl describe ingress simple-fanout-example

Name:             simple-fanout-example
Namespace:        default
Address:          178.91.123.132
Default backend:  default-http-backend:80 (10.8.2.3:8080)
Rules:
  Host         Path  Backends
  ----         ----  --------
  foo.bar.com
               /foo   service1:4200 (10.8.0.90:4200)
               /bar   service2:8080 (10.8.0.91:8080)
Events:
  Type     Reason  Age                From                     Message
  ----     ------  ----               ----                     -------
  Normal   ADD     22s                loadbalancer-controller  default/test
```

只要服务（service1，service2）存在，Ingress控制器就提供满足Ingress的特定于实现的负载平衡器。完成后，您可以在address字段中看到负载平衡器的地址。

> :warning: 注意：根据你使用的Ingress controller，你可能需要创建默认的http后端service

### Name based virtual hosting
基于名称的虚拟主机支持将HTTP流量路由到同一IP地址的多个主机名。

```
                                                                                                ----→ Pod
                                                                                                /
                                               --------→ Host: foo.bar.com  --→ Service service1:80
         Ingress-managed                      /                                                 \
client ------------------→ Ingress, 178.91.123.132                                               ----→ Pod
          load balancer                       \
                                               --------→ Host: bar.foo.com --→ Service service2:80
                                                                                                \
                                                                                                 ----→ Pod
```

下面的Ingress告诉后台负载平衡器根据[主机头(Host header)](https://tools.ietf.org/html/rfc7230#section-5.4)路由请求。

service/networking/name-virtual-host-ingress.yaml 

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: name-virtual-host-ingress
spec:
  rules:
  - host: foo.bar.com
    http:
      paths:
      - pathType: Prefix
        path: "/"
        backend:
          service:
            name: service1
            port:
              number: 80
  - host: bar.foo.com
    http:
      paths:
      - pathType: Prefix
        path: "/"
        backend:
          service:
            name: service2
            port:
              number: 80
```

如果创建的Ingress资源没有在规则中定义任何主机，则可以匹配到Ingress控制器IP地址的任何web流量，而无需基于名称的虚拟主机。

例如，以下Ingress路由从`first.bar.com`到service1, `second.foo.com`到service2的流量，以及其他任何没有定义hostname的到service3的请求。

service/networking/name-virtual-host-ingress-no-third-host.yaml 

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: name-virtual-host-ingress-no-third-host
spec:
  rules:
  - host: first.bar.com
    http:
      paths:
      - pathType: Prefix
        path: "/"
        backend:
          service:
            name: service1
            port:
              number: 80
  - host: second.bar.com
    http:
      paths:
      - pathType: Prefix
        path: "/"
        backend:
          service:
            name: service2
            port:
              number: 80
  - http:
      paths:
      - pathType: Prefix
        path: "/"
        backend:
          service:
            name: service3
            port:
              number: 80
```

### TLS

您可以通过指定包含TLS私钥和证书的[Secret](https://kubernetes.io/docs/concepts/configuration/secret/)来保护Ingress。Ingress资源仅支持单个TLS端口443，并假定TLS终端在Ingress上（到服务及其pod的流量是明文的）。如果Ingress中的TLS配置部分指定了不同的主机，那么它们将根据SNI TLS扩展指定的主机名在同一端口上多路复用（前提是Ingress控制器支持SNI）。TLS secret必须包含名为tls.crt的证书以及tls.key的私钥，用于TLS使用。例如：
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: testsecret-tls
  namespace: default
data:
  tls.crt: base64 encoded cert
  tls.key: base64 encoded key
type: kubernetes.io/tls
```

在Ingress中引用这个secret告诉Ingress控制器使用TLS保护从客户端到负载平衡器的通道。您需要确保您创建的TLS secret中有包含Common Name（CN）（也称为如sslexample.foo.com的完全限定域名（FQDN））的证书.

service/networking/tls-example-ingress.yaml 

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: tls-example-ingress
spec:
  tls:
  - hosts:
      - https-example.foo.com
    secretName: testsecret-tls
  rules:
  - host: https-example.foo.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: service1
            port:
              number: 80
```

> :bookmark: 注：不同Ingress控制器支持的TLS功能之间存在不同。请参考nginx、GCE或任何其他特定于平台的Ingress控制器的文档，以了解TLS在您的环境中是如何工作的。

### Load balancing

Ingress控制器使用一些应用于所有Ingress的负载平衡策略设置（如负载平衡算法、后端权重方案等）引导启动。更高级的负载平衡概念（例如持久会话、动态权重）还没有使用Ingress公开。相反，您可以使用用于Service的负载均衡器使用这些特性。

同样值得注意的是，尽管健康检查没有通过Ingress直接暴露出来，但是Kubernetes中也存在类似的概念，比如[readiness probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/)允许您获得相同的结果。请查看控制器相关的文档，了解它们如何处理运行状况检查（例如：[nginx](https://git.k8s.io/ingress-nginx/README.md)或[GCE](https://git.k8s.io/ingress-gce/README.md#health-checks)）。

## Updating an Ingress

要更新现有Ingress以添加新主机，可以通过编辑资源对其进行更新：
```shell
kubectl describe ingress test

Name:             test
Namespace:        default
Address:          178.91.123.132
Default backend:  default-http-backend:80 (10.8.2.3:8080)
Rules:
  Host         Path  Backends
  ----         ----  --------
  foo.bar.com
               /foo   service1:80 (10.8.0.90:80)
Annotations:
  nginx.ingress.kubernetes.io/rewrite-target:  /
Events:
  Type     Reason  Age                From                     Message
  ----     ------  ----               ----                     -------
  Normal   ADD     35s                loadbalancer-controller  default/test
```

```shell
kubectl edit ingress test
```
这个命令将弹出一个编辑器，其中包含YAML格式的现有配置。修改并添加新主机：
```yaml
spec:
  rules:
  - host: foo.bar.com
    http:
      paths:
      - backend:
          service:
            name: service1
            port:
              number: 80
        path: /foo
        pathType: Prefix
  - host: bar.baz.com
    http:
      paths:
      - backend:
          service:
            name: service2
            port:
              number: 80
        path: /foo
        pathType: Prefix
..
```

保存更改后，kubectl更新API服务器中的资源，这将通知Ingress控制器重新配置负载平衡器。
```shell
kubectl describe ingress test

Name:             test
Namespace:        default
Address:          178.91.123.132
Default backend:  default-http-backend:80 (10.8.2.3:8080)
Rules:
  Host         Path  Backends
  ----         ----  --------
  foo.bar.com
               /foo   service1:80 (10.8.0.90:80)
  bar.baz.com
               /foo   service2:80 (10.8.0.91:80)
Annotations:
  nginx.ingress.kubernetes.io/rewrite-target:  /
Events:
  Type     Reason  Age                From                     Message
  ----     ------  ----               ----                     -------
  Normal   ADD     45s                loadbalancer-controller  default/test
```

您可以使用修改后的ingress YAML文件调用`kubectl replace -f`命令来获得相同的结果

## Failing across availability zones 

不同云提供商在故障域间传播流量的技术各不相同。有关详细信息，请查阅相关[Ingress控制器](https://kubernetes.io/docs/concepts/services-networking/ingress-controllers)的文档

## Alternatives
您可以通过多种不直接涉及Ingress资源的方式公开服务：

- Use Service.Type=LoadBalancer
- Use Service.Type=NodePort

## What's next
- Learn about the [Ingress API](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#ingress-v1beta1-networking-k8s-io)
- Learn about [Ingress controllers](https://kubernetes.io/docs/concepts/services-networking/ingress-controllers/)
- [Set up Ingress on Minikube with the NGINX Controller](https://kubernetes.io/docs/tasks/access-application-cluster/ingress-minikube/)

