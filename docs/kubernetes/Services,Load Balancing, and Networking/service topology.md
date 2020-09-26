# Services, Load Balancing, and Networking

Kubernetes联网解决了四个问题：
- pod内的容器使用网络通过loopback回路进行通信。
- 集群网络提供不同POD之间的通信。
- 服务资源允许您公开在Pods中运行的应用程序，以便可以从集群外部访问它。
- 您还可以使用服务来发布服务，仅在集群内使用。

---

# Service Topology
*FEATURE STATE: Kubernetes v1.17 [alpha]*

服务拓扑使服务能够根据集群的节点拓扑来路由流量。例如，服务可以指定流量优先路由到与客户机位于同一节点上或同一可用性区域中的端点。

## Introduction

默认情况下，发送到ClusterIP或NodePort服务的流量可以路由到Service的任何后端地址。自从Kubernetes 1.7以来，就可以将“外部”流量路由到接收流量的节点上运行的pod上，但是ClusterIP Service不支持这一点，更复杂的拓扑结构（如分区路由）也不可能实现。服务拓扑特性通过允许服务创建者基于源节点和目标节点的节点标签为路由通信量定义一个策略来解决这个问题。

通过使用源和目的地之间的节点标签匹配，操作员可以指定彼此之间“更近”和“更远”的节点组（近、远的含义使用任何能够满足需求的度量）。例如，对于公共云中的许多运营商来说，他们倾向于将服务流量保持在同一区域内，因为区域间的流量会带来相关的成本，而区域内的流量则不会。其他常见的需求包括能够将流量路由到由DaemonSet管理的本地Pod，或者将通信量保持到连接到同一机架顶部交换机的节点以实现最低的延迟。

## Using Service Topology
如果您的群集启用了服务拓扑，则可以通过在服务规范中指定`topologyKeys`字段来控制服务流量路由。此字段是节点标签的首选顺序列表，在访问此服务时将使用该列表对端点进行排序。流量将被定向到第一个标签的值与原始节点标签值相匹配的节点。如果匹配节点上没有服务的后端，那么将考虑第二个标签，依此类推，直到没有标签用来匹配为止。

如果找不到匹配项，则流量将被拒绝，就像根本没有该服务的后端一样。也就是说，端点是根据具有可用后端的第一个topology key来选择的。如果指定了此字段，并且所有条目都没有与客户机拓扑匹配的后端，则该服务没有该客户机对应的后端，连接应失败。“any topology”可以用“*”表示。这个匹配所有的值，放在匹配列表的最后面匹配。

如果topologyKeys未指定或为空，则不会应用拓扑约束。

考虑一个集群，其中的节点标有主机名hostname、区域名zone name和区域名region name。然后您可以将服务的topologyKeys值设置为下面的值来路由流量：
- 仅路由到同一个节点的endpoints，如果节点上不存在endpoint，则失败：["kubernetes.io/主机名"].
- 优先路由到同一节点上的端点，到同一区域中的端点次之，到同一区域优先级最低，如果没有匹配则失败：["kubernetes.io/hostname", "topology.kubernetes.io/zone", "topology.kubernetes.io/region"]. 例如，在数据局部性很重要的情况下，这可能很有用。
- 优先路由到同一区域，但如果此区域内没有可用的endpoint，则路由到任何可用的endpoints：[”topology.kubernetes.io/zone", "*"]

## Constraints

- 服务拓扑与externalTrafficPolicy=Local不兼容，因此服务不能同时使用这两个功能。可以在同一个集群中的不同服务上使用这两个特性，只是不能在同一个服务上使用。
- 有效的拓扑键当前只能是：kubernetes.io/hostname, topology.kubernetes.io/zone，和topology.kubernetes.io/region，但将来将推广到其他节点标签。
- 拓扑键必须是有效的标签键，最多可以指定16个键。
- catch all值“*”必须是拓扑键中的最后一个值（如果使用）

## Example
以下是使用服务拓扑特性的常见示例

### Only Node Local Endpoints

只路由到节点本地端点的服务。如果节点上不存在终结点，则流量将被丢弃：
```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
spec:
  selector:
    app: my-app
  ports:
    - protocol: TCP
      port: 80
      targetPort: 9376
  topologyKeys:
    - "kubernetes.io/hostname"
```

### Prefer Node Local Endpoints 

首选节点本地endpoints，但如果节点本地endpoints不存在，则返回群集范围的endpoints的服务：

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
spec:
  selector:
    app: my-app
  ports:
    - protocol: TCP
      port: 80
      targetPort: 9376
  topologyKeys:
    - "kubernetes.io/hostname"
    - "*"
```

### Only Zonal or Regional Endpoints

一种更倾向于zone而非region的端点的服务。如果两者中都不存在端点，则流量将被丢弃。

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
spec:
  selector:
    app: my-app
  ports:
    - protocol: TCP
      port: 80
      targetPort: 9376
  topologyKeys:
    - "topology.kubernetes.io/zone"
    - "topology.kubernetes.io/region"
```

### Prefer Node Local, Zonal, then Regional Endpoints

一种服务，优先选择节点本地、zone，然后是region端点，最终降级回退到集群范围的端点。

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
spec:
  selector:
    app: my-app
  ports:
    - protocol: TCP
      port: 80
      targetPort: 9376
  topologyKeys:
    - "kubernetes.io/hostname"
    - "topology.kubernetes.io/zone"
    - "topology.kubernetes.io/region"
    - "*"
```

## What's next
- Read about [enabling Service Topology](https://kubernetes.io/docs/tasks/administer-cluster/enabling-service-topology)
- Read [Connecting Applications with Services](https://kubernetes.io/docs/concepts/services-networking/connect-applications-service/)
