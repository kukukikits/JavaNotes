# Network Policies

如果您想在IP地址或端口级别（OSI第3层或第4层）控制流量，那么可以考虑对集群中的特定应用程序使用Kubernetes NetworkPolicys。NetworkPolicies是一个以应用程序为中心的结构，它允许您指定如何允许pod在网络上与各种网络“实体”通信（我们在这里使用“实体”一词，以避免在网络上overloading更常见的术语，如“端点endpoints”和“服务services”，它们具有特定的Kubernetes含义）。

可以与Pod通信的实体通过以下3个标识符的组合进行标识：
- 允许的其他pod（例外：pod不能阻止对自身的访问）
- 允许的命名空间
- IP块（例外：进出有pod运行的节点的任何流量始终被允许，无论Pod或节点的IP地址是什么）

当定义一个基于pod或基于namespace的NetworkPolicy，你要使用一个选择器来指定哪些来自pod的流量，哪些请求pod的流量是被允许的。

同时，当创建基于IP的网络策略时，我们基于IP块（CIDR范围）定义策略

## Prerequisites
网络策略由网络插件实现。要使用网络策略，必须使用支持NetworkPolicy的网络解决方案。在没有实现NetworkPolicy的控制器的情况下创建NetworkPolicy资源是无效的。

## Isolated and Non-isolated Pods

默认情况下，pod是非隔离的；它们接受来自任何来源的流量。

通过网络策略来选择Pods，Pods会被隔离。一旦命名空间中有任何NetworkPolicy选择了特定的pod，该pod将拒绝任何NetworkPolicy不允许的任意连接。（命名空间中未被任何NetworkPolicy选择的其他Pod将继续接受所有流量。）

网络策略不冲突；它们是累加的。如果有一个或多个策略选择了一个pod，则使用这些策略的ingress/egress规则的合集来限制pod的网络。因此，规则的顺序并不影响策略结果。

## The NetworkPolicy resource

完整定义，请参阅[NetworkPolicy参考](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#networkpolicy-v1-networking-k8s-io)。

An example NetworkPolicy might look like this:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: test-network-policy
  namespace: default
spec:
  podSelector:
    matchLabels:
      role: db
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - ipBlock:
        cidr: 172.17.0.0/16
        except:
        - 172.17.1.0/24
    - namespaceSelector:
        matchLabels:
          project: myproject
    - podSelector:
        matchLabels:
          role: frontend
    ports:
    - protocol: TCP
      port: 6379
  egress:
  - to:
    - ipBlock:
        cidr: 10.0.0.0/24
    ports:
    - protocol: TCP
      port: 5978
```

> :warning: 注意：除非您选择的网络解决方案支持网络策略，否则将此配置发布到群集的API服务器是无效的。

**必需字段Mandatory Fields**：与所有其他Kubernetes配置一样，NetworkPolicy需要apiVersion、kind和metadata字段。有关使用配置文件的一般信息，请参阅[使用ConfigMap配置容器](https://kubernetes.io/docs/tasks/configure-pod-container/configure-pod-configmap/)和[对象管理](https://kubernetes.io/docs/concepts/overview/working-with-objects/object-management)。

**规范spec**：NetworkPolicy spec包含在给定命名空间中定义特定网络策略所需的所有信息。

**podSelector**：每个NetworkPolicy都包含一个podSelector，它选择一组使用该策略的pods分组。示例策略选择标签为“role=db”的pod。空的podSelector选择命名空间中的所有pod。

**policyTypes**：每个NetworkPolicy都包含一个policyTypes列表，该列表可以包含Ingress、Egress或两个都包括。policyTypes字段指示给定策略是否适用于选中Pod的入口流量、选中pod的出口流量，或两者都适用。如果NetworkPolicy上没有指定policyTypes，那么默认情况下总是设置Ingress，如果NetworkPolicy有任何Egress出口规则，则设置Egress。

**ingress入口**：每个NetworkPolicy可能包含一个允许的Ingress规则列表。每个规则都允许与from和ports部分相匹配的流量。示例策略包含一个规则，它匹配来自三个源之一的单个端口上的流量，第一个源通过ipBlock指定，第二个通过namespaceSelector指定，第三个源通过podSelector。

**egress出口**：每个NetworkPolicy可以包括一个允许的egress规则的列表。每个规则都允许与to和ports部分相匹配的流量。示例策略包含一个规则，它匹配单个端口上流出的到10.0.0.0/24的流量。

So, the example NetworkPolicy:

1. 隔离“default”命名空间中“role=db”的pod的入流量和出流量（如果它们还没有被隔离）
2. （入口Ingress规则）允许从以下地址连接到pod(该pod的TCP端口为6379，标签为“role=db”，并且在“default”命名空间中)：
   - “default”命名空间中标签为“role=frontend”的任何pod
   - 命名空间的标签为“project=myproject”的任何pod
   - IP地址范围为172.17.0.0 – 172.17.0.255 和 172.17.2.0 – 172.17.255.255（即，除172.17.1.0/24之外的所有172.17.0.0/16）
3. （出口Egress规则）允许从“default”命名空间中标签为“role=db”的任何pod流出到TCP端口为5978且CIDR为10.0.0.0/24的流量

See the [Declare Network Policy](https://kubernetes.io/docs/tasks/administer-cluster/declare-network-policy/) walkthrough for further examples

## Behavior of `to` and `from` selectors

有四种选择器可以在ingress from或者egress to里面定义：

- podSelector：这将选择与NetworkPolicy相同命名空间中的特定pod
- namespaceSelector：它选择特定的命名空间，所有pod都可以作为ingress source或egress destinations。
- namespaceSelector和podSelector：一个指定namespaceSelector和podSelector的to/from条目选择特定命名空间中的特定pod。请注意使用正确的YAML语法；此策略：
  ```yaml
  ...
    ingress:
    - from:
      - namespaceSelector:
          matchLabels:
            user: alice
        podSelector:
          matchLabels:
            role: client
    ...
  ```
  值包含一个from元素，允许来自标签为user=alice的命名空间中标签为role=client的pod的连接。但是这个政策：
  ```yaml
      ...
      ingress:
      - from:
        - namespaceSelector:
            matchLabels:
              user: alice
        - podSelector:
            matchLabels:
              role: client
      ...
  ```
  from数组中的有两个元素，则允许来自标签为role=client的本地命名空间中的Pod的连接，或来自标签user=alice的任何名称空间中的任何Pod的连接。

  如果有疑问，请使用kubectl describe查看Kubernetes如何解释该策略。

- ipBlock： 这将选择特定的IP CIDR范围作为ingress sources或egress destinations。这些应该是集群外部IP，因为Pod IP是短暂的和不可预测的。
  
  集群ingress和egress机制通常需要重写数据包的源或目标IP。对于这种情况，没有定义是在NetworkPolicy处理之前还是之后发生，并且对于网络插件、云提供商、服务实现等的不同组合，相应的行为可能会有所不同。

  以上重写如果发生在ingress的情况下，这意味着在某些情况下，您可以根据实际的原始源IP过滤传入的数据包，然而在其他情况下，NetworkPolicy作用的“源IP”可能是负载平衡器或Pod节点的IP等。

  如果重写发生在egress过程，这意味着从Pod到Service IP的连接（这些IP被重写为群集外部IP）可能会或可能不受ipBlock的策略的约束

## Default policies

默认情况下，如果命名空间中不存在策略，则允许该命名空间中所有进出Pod的流量。下面的示例允许您更改该命名空间中的默认行为

### Default deny all ingress traffic
您可以通过创建一个NetworkPolicy来为命名空间创建一个“默认”隔离策略，该策略选择所有pod，但不允许任何流量进入这些pod

service/networking/network-policy-default-deny-ingress.yaml 

```yaml
---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-ingress
spec:
  podSelector: {}
  policyTypes:
  - Ingress
```

这可以确保即使未被任何其他NetworkPolicy选择的pod也会被隔离。此策略不会更改默认egress隔离行为

### Default allow all ingress traffic

如果希望允许对命名空间中所有pod的所有流量（即使添加的策略导致某些pod被视为“隔离”），则可以创建一个策略，显式允许该命名空间中的所有流量。

service/networking/network-policy-allow-all-ingress.yaml
```yaml
---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-all-ingress
spec:
  podSelector: {}
  ingress:
  - {}
  policyTypes:
  - Ingress
```

### Default deny all egress traffic 

您可以通过创建一个NetworkPolicy来为命名空间创建一个“默认”出口隔离策略，该策略选择所有pod，但不允许来自这些pod的任何出去的流量。

service/networking/network-policy-default-deny-egress.yaml 

```yaml
---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-egress
spec:
  podSelector: {}
  policyTypes:
  - Egress
```
这可以确保即使未被任何其他网络策略选择的pod也不会被允许有流量出站。此策略不会更改默认的ingress隔离行为

### Default allow all egress traffic

如果希望允许来自命名空间中所有pod的所有流量（即使添加的策略导致某些pod被视为“隔离”），则可以创建一个策略，显式允许该命名空间中的所有出口流量。

service/networking/network-policy-allow-all-egress.yaml
```yaml
---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-all-egress
spec:
  podSelector: {}
  egress:
  - {}
  policyTypes:
  - Egress
```

### Default deny all ingress and all egress traffic

您可以为一个命名空间创建一个“默认”策略，通过在该命名空间中创建以下NetworkPolicy来阻止所有进出流量。
service/networking/network-policy-default-deny-all.yaml
```yaml
---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-all
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress
```

这可以确保即使未被任何其他网络策略选择的pod也不会被允许有流量进入或离开

## SCTP support
*FEATURE STATE: Kubernetes v1.19 [beta]*

作为beta特性，默认情况下是启用的。要在集群级别禁用SCTP，您（或您的集群管理员）需要使用`--feature-gates=SCTPSupport=false,….`为API服务器禁用SCTPSupport特性门。启用功能后，可以将NetworkPolicy的protocol字段设置为SCTP。

> :book: 注意：您必须使用支持SCTP协议网络策略的CNI插件。

## What you CAN'T do with network policy's (at least, not yet) 

从Kubernetes 1.20开始，NetworkPolicy API中不存在以下功能，但您可以使用操作系统组件（如SELinux、OpenVSwitch、IPTables等）或第7层技术（入口控制器、服务网格实现）或admission controllers来实现这些功能。如果您不熟悉Kubernetes中的网络安全性，那么值得注意的是，以下用户情景（目前）还不能使用NetworkPolicy API来实现。这些用户案例中的一些（但不是全部）正在积极讨论，以备将来的NetworkPolicy API版本使用。
- 强制内部集群流量通过一个公共网关（这可能最好使用服务网格或其他代理解决）。
- 任何与TLS相关的东西（使用服务网格或入口控制器解决）。
- 特定于节点的策略（您可以对这些策略使用CIDR表示法，但是不能通过节点的Kubernetes标识来确定目标节点）。
- 按名称确定命名空间或service（但是，您可以通过标签确定pod或命名空间，这通常是一种可行的解决方法）。
- 创建或管理由第三方完成的“策略请求”。
- 应用于所有名称空间或pod的默认策略（有一些第三方Kubernetes发行版和项目可以做到这一点）。
- 高级策略查询和可达性工具。
- 在一个策略声明中确定端口范围的能力。
- 记录网络安全事件（例如记录被阻止或接受的连接）的能力。
- 显式拒绝策略的能力（当前NetworkPolicies的模型默认情况下是deny，只有添加allow规则的能力）。
- 防止环回(loopback)或传入主机流量(incoming host traffic)的能力（Pods当前不能阻止localhost访问，也不能阻止来自其所在节点的访问）。

## What's next
See the [Declare Network Policy](https://kubernetes.io/docs/tasks/administer-cluster/declare-network-policy/) walkthrough for further examples.
See more [recipes](https://github.com/ahmetb/kubernetes-network-policy-recipes) for common scenarios enabled by the NetworkPolicy resource.
