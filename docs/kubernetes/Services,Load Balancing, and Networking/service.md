# Services, Load Balancing, and Networking

Kubernetes联网解决了四个问题：
- pod内的容器使用网络通过loopback回路进行通信。
- 集群网络提供不同POD之间的通信。
- 服务资源允许您公开在Pods中运行的应用程序，以便可以从集群外部访问它。
- 您还可以使用服务来发布服务，仅在集群内使用。

---

# Service
将运行在一组pod上的应用程序公开为网络服务的抽象方法。

使用Kubernetes，您不需要修改应用程序来使用不熟悉的服务发现机制。Kubernetes为Pods提供了自己的IP地址和一组pod的单一DNS名称，并且可以在它们之间实现负载平衡

## Motivation

Kubernetes的pod是可以死亡的。他们出生，然后死亡，并不会复活。如果您使用Deployment来运行应用程序，它可以动态地创建和销毁pod。

每个Pod都有自己的IP地址，但是在Deployment中，一个时刻运行的Pod集合可能与稍后运行该应用程序的Pod集不同。

这就导致了一个问题：如果一些pod集（称之为“后端”）为集群内的其他pod（称之为“前端”）提供功能，那么前端如何发现并跟踪要连接到哪个IP地址，然后让前端可以使用工作负载的后端部分？

答案就是使用Services

## Service resources

在Kubernetes中，服务是一种抽象，它定义了一组逻辑pod和访问它们的策略（有时这种模式称为微服务）。服务选中的pod集通常由选择器决定（请参见下面的内容，了解为什么您可能希望服务没有选择器）。

例如，考虑使用3个副本运行的无状态图像处理后端。这些复制品是可替换的，前端并不关心它们使用哪个后端。虽然组成后端集的实际pod可能会发生变化，但是前端客户机不需要意识到这一点，也不需要自己跟踪后端集。

服务抽象支持这种解耦。

### Cloud-native service discovery

如果您能够在应用程序中使用kubernetes api进行服务发现，那么您可以查询API Server中的端点，当服务中的pod集发生变化时，端点就会更新。

对于非原生应用程序，Kubernetes提供了在应用程序和后端pod之间放置网络端口或负载平衡器的方法。

## Defining a Service

Kubernetes中的服务是REST对象，类似于Pod。与所有REST对象一样，您可以将service definition发布`POST`到API服务器以创建新实例。服务对象的名称必须是有效的DNS标签名称。

例如，假设您有一组pod，每个pod都在TCP端口9376上侦听，并带有一个标签`app=MyApp`：

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
spec:
  selector:
    app: MyApp
  ports:
    - protocol: TCP
      port: 80
      targetPort: 9376
```

这个规范创建了一个名为“my-service”的新服务对象，它对应任何带有app=MyApp标签的Pod上的TCP端口9376。

Kubernetes为该服务分配一个IP地址（有时称为“集群IP”），由服务代理使用（参见下面的[虚拟IP和服务代理](https://kubernetes.io/docs/concepts/services-networking/service/#virtual-ips-and-service-proxies)）。

服务选择器的控制器不断扫描匹配其选择器的pod，然后将任何更新POST到相同名称的“myservice”的端点对象。

> 注意：服务可以将任何传入port映射到targetPort。默认情况下，为了方便起见，targetPort设置为与port字段相同的值。

Pods中的端口定义有名称，您可以在服务的targetPort属性中引用这些名称。即使Service中有使用相同配置名称的多个混杂的pod，且这些pod有相同的网络协议，但使用的端口号不同，我们同样可以使用服务的targetPort属性中的端口名称。这为部署和改进服务提供了很大的灵活性。例如，您可以更改Pods在下一个版本的后端软件中公开的端口号，而不会破坏客户端。

服务的默认协议是TCP；您也可以使用任何其他[受支持的协议](https://kubernetes.io/docs/concepts/services-networking/service/#protocol-support)。

由于许多服务需要公开多个端口，Kubernetes支持服务对象上定义多个端口。每个端口定义可以有相同的协议，也可以有不同的协议。

### Services without selectors
服务通常抽象了对kubernetes pods的访问，但它们也可以抽象其他类型的后端。例如：
- 您希望在生产环境中使用一个外部数据库集群，但是在您的测试环境中，使用自己的数据库。
- 您希望将服务指向另一个命名空间或另一个集群上的服务。
- 您正在将一个工作负载迁移到Kubernetes。在评估该这种迁移时，您只在Kubernetes运行一部分后端。

在这些场景中，您可以定义一个没有Pod选择器的服务。例如：

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
spec:
  ports:
    - protocol: TCP
      port: 80
      targetPort: 9376
```

由于此服务没有选择器，因此不会自动创建相应的端点对象。通过手动添加端点对象，可以手动将服务映射到其运行的网络地址和端口：

```yaml
apiVersion: v1
kind: Endpoints
metadata:
  name: my-service
subsets:
  - addresses:
      - ip: 192.0.2.42
    ports:
      - port: 9376
```

The name of the Endpoints object must be a valid DNS subdomain name.

> 注：
> endpoint IP不能是：loopback回路（IPv4为127.0.0.0/8，IPv6为::1/128）或本地链路（IPv4为169.254.0.0/16和224.0.0.0/24，IPv6为fe80::/64）。
> 
> endpoint IP地址不能是其他Kubernetes服务的群集IP，因为kube代理不支持虚拟IP作为目的地址。

在没有选择器的情况下访问服务与使用选择器时访问服务相同。在上面的示例中，流量被路由到YAML定义的单个端点: 192.0.2.42:9376（TCP）。

ExternalName服务是一种特殊情况的服务，它没有选择器，而是使用DNS名称。有关详细信息，请参阅本文档后面的[ExternalName](https://kubernetes.io/docs/concepts/services-networking/service/#externalname)部分

### EndpointSlices 端点分片
*FEATURE STATE: Kubernetes v1.17 [beta]*

endpointSlice是一种API资源，它可以为端点提供更具伸缩性的替代方案。虽然在概念上与端点非常相似，但EndpointSlice允许跨多个资源分布网络端点。默认情况下，当一个EndpointSlice到达100个端点时，它被认为是“full”，此时将创建额外的EndpointSlice来存储任何额外的端点。

EndpointSlice提供了附加的属性和功能，在[EndpointSlices](https://kubernetes.io/docs/concepts/services-networking/endpoint-slices/)中有详细描述。

### Application protocol
*FEATURE STATE: Kubernetes v1.19 [beta]*

AppProtocol字段提供了一种方法来指定要用于每个服务端口的应用程序协议。此字段的值由相应的端点和EndpointSlice资源镜像。（The value of this field is mirrored by corresponding Endpoints and EndpointSlice resources.）

## Virtual IPs and service proxies

Kubernetes集群中的每个节点都运行一个kube-proxy。kube-proxy负责为ExternalName以外的类型的服务实现一种形式的虚拟IP

### Why not use round-robin DNS? 为什么不使用循环DNS？

一个时不时会出现的问题是为什么Kubernetes依赖代理将入站流量转发到后端。其他方法呢？例如，是否可以配置具有多个A值（或AAAA For IPv6）并依赖循环round-robin名称解析的DNS记录？

对服务使用代理有几个原因：
- 长期以来，DNS实现不遵守TTLs记录规则，并在会在name lookups的结果过期后任然缓存这些结果。
- 有些应用程序只执行一次DNS查找并无限期缓存结果。
- 即使应用程序和库进行了适当的重新解析，使用一个很小的或者0值的TTLs会给DNS施加很高的负载，然后变得难以管理

### User space proxy mode 用户空间代理模式
在这种模式下，kube-proxy监视Kubernetes master服务器添加和删除Service和Endpoint对象的情况。对于每个服务，它在本地节点上打开一个端口（随机选择）。任何到这个“代理端口”的连接都被代理到服务的后端的某一个pod上（这个pod是端点报告上来的）。在决定要使用哪个后端Pod时，kube-proxy会参考服务的SessionAffinity设置。

最后，用户空间代理安装iptables规则，这些规则捕捉到服务的clusterIP（虚拟）和端口的流量。规则将流量重定向到代理后端pod的代理端口上。

默认情况下，用户空间模式下的kube-proxy通过循环 round-robin 算法选择后端。

![User space proxy mode image](https://d33wubrfki0l68.cloudfront.net/e351b830334b8622a700a8da6568cb081c464a9b/13020/images/docs/services-userspace-overview.svg)


### iptables proxy mode

在这种模式下，kube-proxy监视Kubernetes control plane添加和删除服务和端点对象的情况。对于每个服务，它会安装iptables规则，这些规则捕获到服务的clusterIP和端口的流量，并将该流量重定向到服务后端集的某一个上。对于每个端点对象，它安装iptables规则来选择后端Pod。

默认情况下，iptables模式下的kube-proxy随机选择一个后端。

使用iptables处理流量具有较低的系统开销，因为流量由Linux netfilter处理，而不需要在用户空间和内核空间之间切换。这种方法也可能更加可靠。

如果kube-proxy在iptables模式下运行，并且所选的第一个Pod没有响应，则连接失败。这与用户空间模式不同：在用户空间模式下，kube-proxy将检测到第一个Pod（已经连接失败）连接，并将使用不同的后端Pod自动重试。

您可以使用Pod readiness探测器来验证后端Pods是否正常工作，这样iptables模式下的kube-proxy只看到测试结果正常的后端。这样做意味着您可以避免通过kube-proxy将流量发送到已失败的Pod上。

![iptables proxy mode image](https://d33wubrfki0l68.cloudfront.net/27b2978647a8d7bdc2a96b213f0c0d3242ef9ce0/e8c9b/images/docs/services-iptables-overview.svg)
 
### IPVS proxy mode 
*FEATURE STATE: Kubernetes v1.11 [stable]*

在ipvs模式下，kube-proxy监视Kubernetes服务和端点，调用`netlink`接口创建相应的ipvs规则，并定期将ipvs规则与Kubernetes服务和端点同步。此控制回路确保IPV状态与所需状态匹配。当访问一个服务时，ipv将流量定向到一个后端pod。

IPVS代理模式基于netfilter hook函数，类似于iptables模式，但使用哈希表作为底层数据结构，并在内核空间工作。这意味着IPVS模式下的kube-proxy重定向流量的延迟比iptables模式下的kube代理要低，同步代理规则时的性能要好得多。与其他代理模式相比，IPVS模式还支持更高的网络流量吞吐量。

IPV提供了更多选项来平衡到后端POD的流量；这些选项包括：
- `rr`：round-robin
- `lc`：least connection最小连接数（最小开放连接数）
- `dh`: destination hashing目的地哈希
- `sh`：source hashing源哈希
- `sed`: shortest expected delay最短预期延迟
- `nq`: never queue从不排队

> 注：
> 要在IPVS模式下运行kube代理，必须在启动kube代理之前使ipv在节点上可用。
> 
> 当kube-proxy以IPVS proxy模式启动时，它验证IPVS内核模块是否可用。如果没有检测到IPVS内核模块，那么kube代理将降级到iptables代理模式下运行。

![IPVS image](https://d33wubrfki0l68.cloudfront.net/2d3d2b521cf7f9ff83238218dac1c019c270b1ed/9ac5c/images/docs/services-ipvs-overview.svg)

在这些代理模型中，绑定到Service的IP:Port的流量被代理到合适的后端，而客户机不需要知道Kubernetes、services或Pods。

如果要确保每次都将来自特定客户端的连接传递到同一个Pod，则可以根据客户端的IP地址通过设置`service.spec.sessionAffinity`属性为“ClientIP”（默认值为“None”）来实现。您也可以通过设置`service.spec.sessionAffinityConfig.clientIP.timeoutSeconds`属性来设置合适的最大会话限制时间。（默认值为10800，即3小时）

## Multi-Port Services 
对于某些服务，您需要公开多个端口。Kubernetes允许您在服务对象上配置多个端口定义。当为一个服务使用多个端口时，您必须给出所有端口的名称，保证这些端口不会出现歧义。例如：

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
spec:
  selector:
    app: MyApp
  ports:
    - name: http
      protocol: TCP
      port: 80
      targetPort: 9376
    - name: https
      protocol: TCP
      port: 443
      targetPort: 9377
```

> 注：
> 与一般的Kubernetes名称一样，端口名称只能包含小写字母数字字符和`-`。端口名还必须以字母数字字符开头和结尾。
> 
> 例如，名称123-abc和web是有效的，但是123_abc和-web无效。

## Choosing your own IP address 

您可以指定自己的群集IP地址作为创建服务请求的一部分。通过设置`.spec.clusterIP `字段实现。例如，如果您已经有一个想要重用的现有DNS条目，或者有一个旧的系统，它配置有特定的IP地址且很难重新配置。

您选择的IP地址必须有效的IPv4或IPv6地址，IP地址范围必须在API server配置的`service-cluster-ip-range` CIDR范围内。如果尝试使用无效的clusterIP地址值创建服务，API服务器将返回422 http状态码，指示存在问题。

## Discovering services

Kubernetes支持两种主要的查找Service的模式：即环境变量和DNS。

### Environment variables 
当Pod在节点上运行时，kubelet会为每个活动服务添加一组环境变量。它支持[Docker links兼容](https://docs.docker.com/userguide/dockerlinks/)变量（请参阅makeLinkVariables）和更简单的`{SVCNAME}_SERVICE_HOST`和`{SVCNAME}_SERVICE_PORT`变量，其中服务名称为大写，破折号转换为下划线。

例如，服务“redis master”公开TCP端口6379并分配了群集IP地址10.0.0.11，它会产生以下环境变量：
```properties
REDIS_MASTER_SERVICE_HOST=10.0.0.11
REDIS_MASTER_SERVICE_PORT=6379
REDIS_MASTER_PORT=tcp://10.0.0.11:6379
REDIS_MASTER_PORT_6379_TCP=tcp://10.0.0.11:6379
REDIS_MASTER_PORT_6379_TCP_PROTO=tcp
REDIS_MASTER_PORT_6379_TCP_PORT=6379
REDIS_MASTER_PORT_6379_TCP_ADDR=10.0.0.11
```

> 注：
> 如果有一个Pod需要访问服务，同时你在使用环境变量方法公开端口和集群IP给客户机Pods，那么你必须在客户机Pods出现之前创建服务。否则，这些客户机pod将不会填充这些环境变量。
> 
> 如果只使用DNS来发现服务的群集IP，则不必担心此顺序问题

### DNS

您可以（几乎总是应该）使用[附加组件](https://kubernetes.io/docs/concepts/cluster-administration/addons/)为Kubernetes集群设置DNS服务。

支持集群的DNS服务器（如CoreDNS）监视kubernetes api以获得新的服务，并为每个服务创建一组DNS记录。如果在整个集群中启用了DNS，那么所有pod都应该能够根据DNS名称自动解析服务。

例如，如果在Kubernetes名称空间“my-ns”中有一个名为“my-service”的服务，则control plane和DNS服务一起为“my-service.my-ns”创建一个DNS记录。“my-ns”命名空间中的pod能够简单地使用"my-service"名称查找到对应的服务（使用“my-service.my-ns”也能找到）。

其他命名空间中的pod必须将名称限定为`my-service.my-ns`才能找到。这些名称会解析为给服务分配的群集IP。

Kubernetes还支持命名端口的DNS SRV（服务）记录。如果`my-service.my-ns`服务有一个名为“http”的端口，协议设置为TCP，你可以通过DNS SRV查询`_http._tcp.my-service.my-ns`来发现“http”的端口号以及IP地址

Kubernetes DNS服务器是访问ExternalName服务的唯一方法。您可以在[DNS Pods和服务](https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/)中找到有关ExternalName解析的更多信息。

## Headless Services

有时您不需要负载均衡和单一服务IP。在这种情况下，您可以通过显式地为集群IP（`.spec.clusterIP`）设置为None, 来创建所谓的“headless”服务

您可以使用 headless Service 与其他服务发现机制交互，而不必与Kubernetes的实现绑定。

对于headless服务，没有分配集群IP，kube-proxy不会处理这些服务，并且平台没有为它们进行负载平衡或代理。如何自动配置DNS取决于服务是否定义了选择器：

1. 如果定义了Selectors
    对于定义选择器的 headless Services，端点控制器在API中创建`Endpoints`记录，并修改DNS配置以返回直接指向Service背后的pod的记录（地址）

2. 没有定义Selectors
    对于不定义选择器的headless Services，端点控制器不创建`Endpoints`记录。但是，DNS系统会查找并配置：
    - ExternalName类型服务的CNAME记录。
    - 对于所有其他类型，给任意和Service共享name的`Endpoints`创建记录

## Publishing Services (ServiceTypes) 

对于应用程序的某些部分（例如，前端），您可能希望将服务公开到集群之外的外部IP地址。

Kubernetes `ServiceTypes`允许您指定所需的服务类型。默认值为`ClusterIP`。

`Type`的值及其行为是：
- `ClusterIP`：在集群内部IP上公开服务。选择此值将使服务只能从群集中访问。这是默认的服务类型。
- [NodePort](https://kubernetes.io/docs/concepts/services-networking/service/#nodeport)：在每一个节点的IP和静态端口上暴露服务。将自动创建一个`ClusterIP`服务, `NodePort`服务会路由到这个服务上。您可以从集群外部，通过请求`<NodeIP>：<NodePort>`访问NodePort服务。
- [LoadBalancer](https://kubernetes.io/docs/concepts/services-networking/service/#loadbalancer)：使用云提供商的负载平衡器对外公开服务。系统自动创建NodePort和ClusterIP服务，并让外部负载平衡器路由到这两个服务。
- [ExternalName](https://kubernetes.io/docs/concepts/services-networking/service/#externalname)：通过返回一个CNAME记录, 将服务映射到包含ExternalName字段的内容（例如`foo.bar.example.com`）。不需要设置任何类型的代理
    
    > 注意：您需要kube-dns版本1.7或CoreDNS版本0.0.8或更高版本才能使用ExternalName类型。

您还可以使用[Ingress](https://kubernetes.io/docs/concepts/services-networking/ingress/)来公开您的服务。Ingress不是服务类型，但它可以充当集群的入口点。它允许您将路由规则整合到单个资源中，因为它可以在同一个IP地址下公开多个服务。

### Type NodePort 
如果将type字段设置为NodePort，Kubernetes control plane将从`--service-node-port-range`指定的范围（默认：30000-32767）分配一个端口。每个节点将该端口（每个节点上都使用相同的端口号）代理到服务。Service会在`.spec.ports[*].nodePort`字段中上报分配的端口。

如果要指定特定的IP来代理端口，可以将kube-proxy中的`--nodeport-addresses`标志设置为特定的IP块；自从kubernetes v1.10以来，就支持这一点。此标志采用逗号分隔的IP块列表（例如10.0.0.0/8, 192.0.2.0/25）来指定IP地址范围，并且kube代理将其视为该节点的本地IP地址范围。

例如，如果使用`--nodeport-addresses=127.0.0.0/8`标志启动kube-proxy，kube-proxy只为 NodePort 服务选择 loopback 接口。默认的`--nodeport-addresses`是一个空列表, 也就意味着对于NodePort, kube-proxy可以使用所有可用的网络接口。（这也与早期的Kubernetes版本兼容）。

如果需要特定的端口号，可以在`nodePort`字段中指定一个值。Control plane将为您分配该端口, 如果失败则报告API事务失败。这意味着您需要自己处理可能的端口冲突。您还必须使用有效的端口号，该端口号必须在NodePort配置的端口范围内。

使用NodePort可以让您自由地设置自己的负载平衡解决方案，用来配置Kubernetes不能完全支持的环境，甚至可以直接公开一个或多个节点的ip。

注意，NodePort Service在`<NodeIP>:spec.ports[*].nodePort`，以及`.spec.clusterIP:spec.ports[*].port`这些地址都可以访问到。（如果设置了kube-proxy的`--nodeport-addresses` flag，这些访问地址就是过滤出来的NodeIP）

For example:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
spec:
  type: NodePort
  selector:
    app: MyApp
  ports:
      # By default and for convenience, the `targetPort` is set to the same value as the `port` field.
    - port: 80
      targetPort: 80
      # Optional field
      # By default and for convenience, the Kubernetes control plane will allocate a port from a range (default: 30000-32767)
      nodePort: 30007
```

### Type LoadBalancer 

在支持外部负载平衡器的云提供商上，将type字段设置为LoadBalancer将为您的服务提供一个负载平衡器。负载平衡器的实际创建是异步的，有关已配置的平衡器的信息将在服务的`.status.loadBalancer`字段上出现。例如：
```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
spec:
  selector:
    app: MyApp
  ports:
    - protocol: TCP
      port: 80
      targetPort: 9376
  clusterIP: 10.0.171.239
  type: LoadBalancer
status:
  loadBalancer:
    ingress:
    - ip: 192.0.2.127
```

来自负载平衡器外部的流量直接指向后端pod。云提供商决定如何进行负载平衡。

对于LoadBalancer类型的服务，当定义了多个端口时，所有端口必须具有相同的协议，并且协议必须是TCP、UDP和SCTP之一。

某些云提供商允许您指定loadBalancerIP。在这些情况下，将使用用户指定的loadBalancerIP创建负载平衡器。如果未指定loadBalancerIP字段，则使用临时IP地址设置loadBalancer。如果指定loadBalancerIP，但云提供程序不支持该功能，则会忽略您设置的loadBalancerIP字段。

> 注意：如果您使用的是SCTP，请参阅下面关于LoadBalancer服务类型的[警告](https://kubernetes.io/docs/concepts/services-networking/service/#caveat-sctp-loadbalancer-service-type)。

> 注：
> 在Azure上，如果要使用用户指定的公共类型loadBalancerIP，首先需要创建静态类型的公共IP地址资源。此公用IP地址资源应位于群集的其他自动创建资源的同一资源组中。例如，MC_myResourceGroup_myAKSCluster_eastus。
> 
> 将分配的IP地址指定为loadBalancerIP。确保已更新云提供程序配置文件中的securityGroupName。有关对CreatingLoadBalancerFailed权限问题进行故障排除的信息，请参阅，[Use a static IP address with the Azure Kubernetes Service (AKS) load balancer](https://docs.microsoft.com/en-us/azure/aks/static-ip) or [CreatingLoadBalancerFailed on AKS cluster with advanced networking](https://github.com/Azure/AKS/issues/357)

#### Internal load balancer（没太看懂这有什么用，怎么用）

在混合环境中，有时需要路由这样的流量：来自同一（虚拟）网络地址块内的Service的流量。

在split-horizon DNS环境中，您需要两个服务才能将外部和内部流量路由到您的端点。

您可以通过向服务添加以下注释之一来实现这一点。要添加的注释取决于您使用的云服务提供商。

```
# Alibaba Cloud
[...]
metadata:
  annotations:
    service.beta.kubernetes.io/alibaba-cloud-loadbalancer-address-type: "intranet"
[...]

# Tencent Cloud
[...]
metadata:
  annotations:
    service.kubernetes.io/qcloud-loadbalancer-internal-subnetid: subnet-xxxxx
[...]
```
等等

#### TLS support on AWS 
#### PROXY protocol support on AWS
#### Network Load Balancer support on AWS


### Type ExternalName
ExternalName类型的服务将服务映射到DNS名称，而不是映射到典型的选择器。使用`spec.externalName`指定。

例如，这个服务定义将prod命名空间中的`my-service`服务映射到`my.database.example.com`：

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
  namespace: prod
spec:
  type: ExternalName
  externalName: my.database.example.com
```

> 注意：ExternalName接受IPv4地址字符串，但必须是由数字组成的DNS名称，而不是IP地址。CoreDNS或ingres-nginx无法解析与类似于IPv4地址的ExternalNames，因为ExternalName旨在指定规范的DNS名称。要硬编码IP地址，请考虑使用headless Services。

当查找`my-service.prod.svc.cluster.local`主机时，群集DNS服务返回一个值为`my.database.example.com`的CNAME记录。和其他Service一样可以使用`my-service`访问，但关键的区别是使用ExternalName的方式其重定向是发生在DNS级别的，而不是通过代理或转发。如果以后决定将数据库移到集群中，可以启动数据库的pod，然后添加适当的选择器或端点，最后更改服务的type。

> :warning: 警告：
> 对于一些常见的协议，包括HTTP和HTTPS，使用ExternalName可能会遇到问题。如果使用ExternalName，那么集群内客户机使用的hostname与ExternalName引用的名称不同。
> 
> 对于使用hostname的协议，此差异可能会导致错误或异常响应。HTTP请求将包含一个`Host`头部，源服务器则无法识别；TLS服务器将无法提供与客户端连接的hostname匹配的证书。

> Note: This section is indebted to the [Kubernetes Tips - Part 1](https://akomljen.com/kubernetes-tips-part-1/) blog post from [Alen Komljen](https://akomljen.com/)
> 

### External IPs
如果有外部IP(`externalIPs`)路由到一个或多个集群节点，Kubernetes服务可以在这些外部IP上公开。通过外部IP（作为目标IP）和Service port进入集群的流量将被路由到其中一个服务端点。externalIPs不由Kubernetes管理，由集群管理员负责。

在服务规范中，externalIPs可以与任何ServiceTypes一起指定。在下面的示例中，`my-service`可以由`80.11.12.10:80`（`externalIP:port`）上的客户机访问

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
spec:
  selector:
    app: MyApp
  ports:
    - name: http
      protocol: TCP
      port: 80
      targetPort: 9376
  externalIPs:
    - 80.11.12.10
```

## Shortcomings 缺点

为VIP使用用户空间代理可以在中小型范围内工作，但不会扩展到包含数千个服务的非常大的集群。[门户最初的设计方案](https://github.com/kubernetes/kubernetes/issues/1107)对此有更多的细节。

使用用户空间代理会模糊访问服务的数据包的源IP地址。这使得某些类型的网络过滤（防火墙）变得不可能。iptables代理模式不会抹去进入集群的source IPs，但它仍然会影响通过负载平衡器或节点端口的客户端。

Type字段设计有嵌套功能：每个级别都添加到上一个级别。这并不是所有云提供商都严格要求的（例如，Google Compute Engine不需要分配NodePort来让LoadBalancer工作，但AWS需要这样做），但当前的API需要这样做

## Virtual IP implementation 

对于许多只想使用服务的人来说，前面的信息应该足够了。然而，在幕后有很多事情值得我们去理解。

### Avoiding collisions 

Kubernetes的一个主要哲学是，你不会因为非自身错误而出现操作失败。对于服务资源的设计，这意味着如果你自己的选择和他人的选择可能存在冲突，那么系统就不会让你自己去选择端口号。这是一种隔离错误。

为了允许你自己为服务选择端口号，我们必须确保没有两个服务发生冲突。Kubernetes通过为每个服务分配自己的IP地址来做到这一点。

为了确保给每个服务分配唯一的IP，内部分配器在创建每个服务之前会自动更新etcd中的全局分配映射对象。映射对象必须存在于注册表中，服务才能分配获得IP地址，否则服务的创建就会失败，并显示一条消息，指示无法分配IP地址。

在control plane中，一个后台控制器负责创建这个映射（需要支持使用内存锁定的旧版本的Kubernetes的迁移）。Kubernetes还使用控制器来检查无效的IP分配（例如由于管理员的干预）以及清理已分配的不再被任何服务使用的IP地址。

### Service IP addresses

与Pod IP地址不同，Pod IP地址实际上路由到一个固定的地址，服务IP实际上不是由单个主机应答的。相反，kube-proxy使用iptables（Linux中的包处理逻辑）来定义虚拟IP地址，这些地址可以根据需要透明地重定向。当客户端连接到VIP时，它们的流量会自动传输到适当的端点。服务的环境变量和DNS实际上是使用服务的虚拟IP地址（和端口）填充的。

kube-proxy支持三种代理模式：userspace、iptables和IPVS，它们的操作方式略有不同

#### Userspace 

作为示例，考虑上述图像处理应用程序。在后端服务创建后，Kubernetes master将分配一个虚拟IP地址，例如10.0.0.1。假设服务端口是1234，集群中的所有kube代理实例都会观察到该服务。当代理看到一个新的服务时，它会打开一个新的随机端口，在iptables上建立一个从虚拟IP地址重定向到这个随机端口的规则，然后开始接收该端口上的连接。

当客户机连接到服务的虚拟IP地址时，iptables规则生效，并将数据包重定向到kube-proxy自己的端口上。然后“Service proxy”选择一个后端，并开始代理从客户端到后端的流量。

这意味着Service所有者可以选择他们想要的任何端口，而不会有碰撞的风险。客户机可以简单地连接到一个IP和端口，而不用知道它们实际在访问哪些pod

#### iptables

再次，考虑上述图像处理应用程序。创建后端服务时，Kubernetes control plane将分配一个虚拟IP地址，例如10.0.0.1。假设服务端口是1234，集群中的所有kube-proxy实例都会观察到该服务。当代理看到一个新服务时，它会安装一系列iptables规则，这些规则从虚拟IP地址重定向到per-Service。per-Service规则链接到per-Endpoint规则，这些规则将流量（使用目标NAT）重定向到后端。

当客户端连接到服务的虚拟IP地址时，iptables规则生效。后端的A被选中（基于会话亲和力session affinity或随机选择），并将数据包重定向到后端A。与用户空间代理不同，数据包永远不会复制到用户空间，kube-proxy不必运行虚拟IP地址才能工作，节点可以看到来自未更改的客户端IP地址的流量。

当流量通过节点端口或负载平衡器传入时，会执行相同的流程，不过在这些情况下，客户端IP确实会被更改。

#### IPVS 

iptables在大规模集群（如10000个服务）中的运行速度急剧下降。IPVS是为负载平衡而设计的，它基于内核内的哈希表。因此，您可以通过基于IPVS的kube-proxy实现大规模服务的性能一致性。同时，基于IPVS的kube-proxy具有更复杂的负载平衡算法（least conns, locality, weighted, persistence）

## API Object

Service是kubernetes rest api中的顶级资源。您可以在以下位置找到有关API对象的更多详细信息：[service api object](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#service-v1-core)。

## Supported protocols
1. TCP
   您可以将TCP用于任何类型的服务，这是默认的网络协议。

2. UDP
    大多数服务都可以使用UDP。对于type=LoadBalancer服务，UDP取决于云提供商

3. HTTP
    如果您的云提供商支持它，则可以使用LoadBalancer模式下的服务来设置外部HTTP/HTTPS反向代理，并将其转发到服务的端点。
    > Note：您也可以使用Ingress代替服务来公开HTTP/HTTPS服务

4. PROXY protocol
   如果您的云提供商支持它，您可以使用LoadBalancer模式下的服务来配置Kubernetes之外的负载平衡器，它将转发以[PROXY protocol](https://www.haproxy.org/download/1.8/doc/proxy-protocol.txt)为前缀的连接。

   负载平衡器将发送一个八位字节的初始序列，这个序列描述描述了incoming connection，与此示例类似
   ```
    PROXY TCP4 192.0.2.202 10.0.42.7 12345 7\r\n
   ```
   然后就是来自客户端的数据

5. SCTP
   *FEATURE STATE: Kubernetes v1.19 [beta]*
   Kubernetes支持[SCTP](https://www.ibm.com/developerworks/cn/linux/l-sctp/index.html)作为Service、Endpoints、EndpointSlice、NetworkPolicy和Pod定义中的协议值。作为beta特性，默认情况下是启用的。要在集群级别禁用SCTP，您（或您的集群管理员）需要使用`--feature-gates=SCTPSupport=false,…`关闭

   如果功能启用了，你可以将服务、端点、EndpointSlice、NetworkPolicy或Pod的协议字段设置为SCTP。Kubernetes为SCTP关联相应地设置网络，就像它为TCP连接所做的那样

   > :warning: 
   > Support for multihomed SCTP associations
   > Warning:
   >> The support of multihomed SCTP associations requires that the CNI plugin can support the assignment of multiple interfaces and IP addresses to a Pod.
   >> NAT for multihomed SCTP associations requires special logic in the corresponding kernel modules.

   > Service with type=LoadBalancer
   >> Warning: You can only create a Service with type LoadBalancer plus protocol SCTP if the cloud provider's load balancer implementation supports SCTP as a protocol. Otherwise, the Service creation request is rejected. The current set of cloud load balancer providers (Azure, AWS, CloudStack, GCE, OpenStack) all lack support for SCTP.

   > Windows
   >> Warning: SCTP is not supported on Windows based nodes.

   > Userspace kube-proxy
   >> Warning: The kube-proxy does not support the management of SCTP associations when it is in userspace mode
