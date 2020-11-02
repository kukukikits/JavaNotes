# IPv4/IPv6 dual-stack
**FEATURE STATE: Kubernetes v1.16 [alpha]**

IPv4/IPv6双栈支持同时将IPv4和IPv6地址分配给pod和服务。

如果为Kubernetes群集启用IPv4/IPv6双堆栈网络，则群集将支持同时分配IPv4和IPv6地址

## Supported Features

在Kubernetes群集上启用IPv4/IPv6双堆栈提供了以下功能：
- 双栈pod网络（每个pod分配有一个IPv4和IPv6地址）
- 支持IPv4和IPv6的Service（每个服务必须针对单个address family）
- 通过IPv4和IPv6接口支持Pod off-cluster egress路由（如Internet）

## Prerequisites

要利用IPv4/IPv6双堆栈Kubernetes群集，需要以下先决条件：
- Kubernetes 1.16或更高版本
- 供应商对双栈网络的支持（云提供商或其他供应商必须能够为Kubernetes节点提供可路由的IPv4/IPv6网络接口）
- 支持双栈的网络插件（如Kubenet或Calico）

## Enable IPv4/IPv6 dual-stack

要启用IPv4/IPv6双堆栈，请为群集的相关组件启用IPv6DualStack feature gate，并配置双堆栈群集网络分配：
- kube-apiserver:
  - `--feature-gates="IPv6DualStack=true"`
  - `--service-cluster-ip-range=<IPv4 CIDR>,<IPv6 CIDR>`
- kube-controller-manager:
  - `--feature-gates="IPv6DualStack=true"`
  - `--cluster-cidr=<IPv4 CIDR>,<IPv6 CIDR>`
  - `--service-cluster-ip-range=<IPv4 CIDR>,<IPv6 CIDR>`
  - `--node-cidr-mask-size-ipv4|--node-cidr-mask-size-ipv6`, IPv4默认为/24，IPv6默认为/64
- kubelet:
  - `--feature-gates="IPv6DualStack=true"`
- kube-proxy:
  - `--cluster-cidr=<IPv4 CIDR>,<IPv6 CIDR>`
  - `--feature-gates="IPv6DualStack=true"`

> 注：
> ipv4 CIDR的一个示例：10.244.0.0/16（您可以提供自己的地址范围）
> IPv6 CIDR示例：fdXY:IJKL:MNOP:15::/64（这只是用来展示格式，不是有效地址-请参阅[RFC 4193](https://tools.ietf.org/html/rfc4193)）

## Services

如果您的群集启用了IPv4/IPv6双堆栈网络，则可以使用IPv4或IPv6地址创建服务。您可以通过设置service上的`.spec.ipFamily`字段来选择服务群集IP的地址族。只能在创建新服务时设置此字段。设置`.spec.ipFamily`字段是可选的，仅当您计划在集群上启用IPv4和IPv6服务和Ingress时才应使用该字段。此字段的配置不是egress流量所必须的。

> 注意：集群的默认地址族是通过kube控制器管理器的`--service-cluster-ip-range`标志配置的第一个service-cluster-ip-range的地址族。

你可以使用下面任意方式配置`.spec.ipFamily`：
- IPv4: API服务器将从`service-cluster-ip-range`（即ipv4）分配IP
- IPv6: API服务器将从`service-cluster-ip-range`（即IPv6）分配IP

以下服务规范不包括ipFamily字段。Kubernetes将从第一个配置的`service-cluster-ip-range`向该服务分配一个IP地址（也称为“集群IP”）。

service/networking/dual-stack-default-svc.yaml
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

以下服务规范包括ipFamily字段。Kubernetes将从配置的`service-cluster-ip-range`给该服务分配IPv6地址（也称为“群集IP”）。

service/networking/dual-stack-ipv6-svc.yaml
```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
spec:
  ipFamily: IPv6
  selector:
    app: MyApp
  ports:
    - protocol: TCP
      port: 80
      targetPort: 9376
```

为了进行比较，以下服务规范将从配置的`service-cluster-ip-range`分配IPv4地址（也称为“群集IP”）给服务。

service/networking/dual-stack-ipv4-svc.yaml
```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
spec:
  ipFamily: IPv4
  selector:
    app: MyApp
  ports:
    - protocol: TCP
      port: 80
      targetPort: 9376
```

### Type LoadBalancer

在支持IPv6的外部负载平衡器的云提供商上，除了将ipFamily字段设置为IPv6之外，将type字段设置为LoadBalancer来为您的服务提供云负载平衡器

## Egress Traffic

如果底层CNI提供商能够实现传输，则可以使用公共路由和非公共路由的IPv6地址块。如果您有一个使用非公开路由IPv6的Pod，并希望该Pod能够到达群集外部地址（例如，公共Internet），则必须为出口流量和任何回复设置IP伪装（IP masquerading）。[ip-masq-agent](https://github.com/kubernetes-incubator/ip-masq-agent)可以使用dual-stack，因此你可以在dual-stack集群上使用ip-masq-agent(ip双重伪装代理)来伪装IP

## Known Issues
Kubenet forces IPv4,IPv6 positional reporting of IPs (--cluster-cidr）

## What's next
[Validate IPv4/IPv6 dual-stack](https://kubernetes.io/docs/tasks/network/validate-dual-stack) networking




