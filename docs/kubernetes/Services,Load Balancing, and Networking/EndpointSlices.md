# EndpointSlices
*FEATURE STATE: Kubernetes v1.17 [beta]*

EndpointSlices 提供了一种简单的方法来跟踪Kubernetes集群中的网络端点。它们为端点提供了一种更具伸缩性和可扩展性的替代方案。

## Motivation 动机

Kubernetes的Endpoints API提供了一种简单的网络端点跟踪方法。不幸的是，随着Kubernetes集群和服务的增长，需要处理和发送更多的流量到更多的后端pod，原始API的局限性就会变得更加明显。最明显的是，在扩展到更大数量的网络端点时存在困难。

由于服务的所有网络端点都存储在单个端点资源中，因此这些资源可能会变得非常大。这影响了Kubernetes组件（尤其是master control plane）的性能，并在端点发生变化时导致大量的网络流量和处理。EndpointSlices帮助您减轻这些问题，同时提供具有额外特性的（如拓扑路由）的可扩展平台。

## EndpointSlice resources

在Kubernetes中，EndpointSlice包含对一组网络端点的引用。Control plane会自动为任何指定了选择器的Kubernetes服务创建EndpointSlices。这些EndpointSlices包括所有与服务选择器匹配的pod的引用。EndpointSlices通过协议、端口号和服务名称的唯一组合将网络端点分组在一起。

EndpointSlice对象的名称必须是有效的DNS子域名。

下面是一个`example`Service的EndpointSlice资源。

```yaml
apiVersion: discovery.k8s.io/v1beta1
kind: EndpointSlice
metadata:
  name: example-abc
  labels:
    kubernetes.io/service-name: example
addressType: IPv4
ports:
  - name: http
    protocol: TCP
    port: 80
endpoints:
  - addresses:
      - "10.1.2.3"
    conditions:
      ready: true
    hostname: pod-1
    topology:
      kubernetes.io/hostname: node-1
      topology.kubernetes.io/zone: us-west2-a
```

默认情况下，control plane将创建和管理端点不超过100的EndpointSlices。您可以使用` --max-endpoints-per-slice kube-controller-manager`标志对此进行配置，最多不超过1000个。

当涉及到如何路由内部流量时，EndpointSlices可以充当kube-proxy的真实来源。当启用时，它们应该为具有大量端点的服务提供性能改进。

### Address types
EndpointSlice支持三种地址类型：
- IPv4
- IPv6
- FQDN (Fully Qualified Domain Name)

### Topology information

EndpointSlice中的每个端点都可以包含相关的拓扑信息。它用于指示端点的位置，包含有关相应节点、分区和区域的信息。当这些值可用时，control plane将为EndpointSlices设置以下拓扑标签：

- `kubernetes.io/hostname` - 此endpoint所在节点的名称。
- `topology.kubernetes.io/zone` - 此终结点所在的zone。
- `topology.kubernetes.io/region` - 此终结点所在的region。

这些标签的值来自于切片中和每个端点相关联的资源。hostname标签表示相应Pod上NodeName字段的值。zone分区和region区域标签表示相应节点上具有相同名称的标签的值。

### Management 

通常，control plane（具体是endpoint slice控制器）创建和管理EndpointSlice对象。对于EndpointSlices，还有许多其他用例，例如service mesh实现，这些用例可能导致其他实体或控制器管理额外的EndpointSlices集。

为了确保多个实体可以在不相互干扰的情况下管理EndpointSlices，Kubernetes定义了标签`endpointslice.kubernetes.io/managed-by`，用来表示管理EndpointSlice的实体。endpoint slice控制器会把所有它管理的EndpointSlices的`endpointslice.kubernetes.io/managed-by`标签值设置为`endpointslice-controller.k8s.io`。管理EndpointSlices的其他实体也应该为此标签设置一个唯一的值。

## Ownership 

在大多数用例中，EndpointSlices属于那些被endpoint slice对象跟踪endpoints的服务。此所有权由每个EndpointSlice上的所有者引用表示，同时`kubernetes.io/service-name`标签也用来表示这种所有权关系，该标签允许对属于Service的所有EndpointSlice进行简单查找。

## EndpointSlice mirroring

在某些情况下，应用程序会创建自定义端点资源。为了确保这些应用程序不同时写入Endpoints和EndpointSlice资源，集群的control plane将大多数Endpoints资源镜像到相应的EndpointSlices。

control plane会镜像Endpoints资源，除了：
- Endpoints资源的`endpointslice.kubernetes.io/skip-mirror`设置为true。
- Endpoints资源有`control-plane.alpha.kubernetes.io/leader`注释。
- 相应的Service资源不存在。
- 相应的服务资源具有non-nil selector

个别Endpoints资源可以转换为多个EndpointSlices。如果Endpoints资源具有多个子集或包含具有多个IP族（IPv4和IPv6）的Endpoints，则会发生这种情况。每个子集最多将有1000个地址镜像到EndpointSlices

## Distribution of EndpointSlices 

每个EndpointSlice都有一组应用于资源内所有endpoints的端口。当Service使用命名的端口时，
Service对应的Pod可以是相同的命名端口，不同的端口号。这与使用Endpoints分组子集的逻辑类似。

control plane会尝试尽可能完整地填充EndpointsSlices，但不会主动重新平衡它们。逻辑相当简单：
- 迭代现有的EndpointSlices，删除不再需要的endpoints，并更新已更改的匹配endpoints。
- 迭代在第一步中修改过的EndpointSlices，并用需要的新端点填充它们
- 如果还有新的端点要添加，尝试将它们放入先前未更改的切片中，并/或创建新的EndpointSlice。

重要的是，第三步中会优先限制EndpointSlice的更新，不行的话再执行EndpointSlices的完全完整分发。例如，如果要添加10个新的endpoints，并且有2个EndpointSlice，每个EndpointSlice都有5个空间可以容纳5个以上的endpoints，则此方法将创建一个新的EndpointSlice，而不是填充现有的2个EndpointSlice。换句话说，仅创建一个新的EndpointSlice的优先级高于更新多个EndpointSlice。

由于kube-proxy在每个节点上运行并监视EndpointSlices，对一个EndpointSlice的每次更改都会变得相对昂贵，因为修改后的EndpointSlice要传输到集群中的每个节点。这种方法旨在限制需要发送到每个节点的更改的数量，虽然这可能会导致多个EndpointSlices没有被填满。

实际上，这种不太理想的分发过程应该很少发生。由EndpointSlice控制器处理的大多数更改都足够小，现有的一个EndpointSlice就能满足了，如果不是这样，则很可能很快就需要一个新的EndpointSlice。Deployment的滚动更新还提供了EndpointSlice的自然重新打包，所有pod及其对应的endpoints都会被替换然后打包到pod里面。

## Duplicate endpoints
由于EndpointSlice更改的性质，endpoints可以同时在多个EndpointSlice中存在。当对不同EndpointSlice对象的更改在不同的时间到达Kubernetes客户机监视/缓存时，这现象就会发生。使用EndpointSlice的实现必须有能力处理endpoint出现在多个切片中的情况。如何消除重复的endpoint，可以参考kube-proxy中实现的EndpointSliceCache。

## What's next
- Learn about [Enabling EndpointSlices](https://kubernetes.io/docs/tasks/administer-cluster/enabling-endpointslices)
- Read [Connecting Applications with Services](https://kubernetes.io/docs/concepts/services-networking/connect-applications-service/)