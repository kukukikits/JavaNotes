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