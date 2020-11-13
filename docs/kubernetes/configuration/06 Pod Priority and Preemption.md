# Pod Priority and Preemption    | Pod优先级和抢占

*FEATURE STATE: Kubernetes v1.14 [stable]*

Pod可以有优先级。优先级表示一个Pod相对于其他Pod的重要性。如果一个Pod无法被调度，调度程序会尝试抢占（逐出）低优先级的Pod，使pending Pod的调度成为可能。

> :warning: 警告：
> 在并非所有用户都受信任的集群中，恶意用户可能会以尽可能高的优先级创建pod，从而导致其他pod被逐出/无法得到调度。管理员可以使用ResourceQuota阻止用户以高优先级创建pod。
> 有关详细信息，请参见[限制默认优先级类的使用](https://kubernetes.io/docs/concepts/policy/resource-quotas/#limit-priority-class-consumption-by-default)。

## How to use priority and preemption 

要使用优先级和抢占：
1. 添加一个或多个[PriorityClass](https://kubernetes.io/docs/concepts/configuration/pod-priority-preemption/#priorityclass)。
2. 创建[priorityClassName](https://kubernetes.io/docs/concepts/configuration/pod-priority-preemption/#pod-priority)设置为某PriorityClasses的Pods。当然，您不需要直接创建Pod；通常是把priorityClassName添加到集合对象（如Deployment）的Pod模板中。

请继续阅读以了解有关这些步骤的更多信息。

> :warning: 注意：Kubernetes已经提供了两个PriorityClasses：`system-cluster-critical`和`system-node-critical`。这些是常见的类，用于[确保关键组件总是首先被调度](https://kubernetes.io/docs/tasks/administer-cluster/guaranteed-scheduling-critical-addon-pods/)


## PriorityClass

PriorityClass是一个非命名空间的对象，它定义了优先级类名与优先级整数值的映射关系。名称在PriorityClass对象元数据的`name`字段中指定。优先级整数值在`value`字段中指定(必须)。值越高，优先级越高。PriorityClass对象的名称必须是有效的DNS子域名，并且不能以`system-`作为前缀。

PriorityClass对象可以有小于或等于10亿的32位整数值。对于通常不应被抢占或驱逐的关键系统级Pod，优先级数保留为较大的值。集群管理员应该为他们想要的每个映射创建一个PriorityClass对象。

PriorityClass还有两个可选字段：globalDefault和description。globalDefault字段指示此PriorityClass的值应用于没有priorityClassName的pod。系统中只能存在一个globalDefault设置为true的PriorityClass。如果没有设置了globalDefault的PriorityClass，则没有priorityClassName的Pods的优先级为零。

description字段是任意字符串。它的目的是告诉集群的用户何时应该使用这个PriorityClass。

### Notes about PodPriority and existing clusters

- 如果升级现有集群时没有此功能，那么现有Pods的优先级实际上为零。
- 在globalDefault设置为true的情况下添加PriorityClass不会更改现有pod的优先级。PriorityClass的这个值仅适用于添加PriorityClass后创建的Pod。
- 如果删除PriorityClass，使用已删除PriorityClass名称的现有Pod将保持不变，但无法再创建使用已删除PriorityClass名称的Pod。

### Example PriorityClass
```yaml
apiVersion: scheduling.k8s.io/v1
kind: PriorityClass
metadata:
  name: high-priority
value: 1000000
globalDefault: false
description: "This priority class should be used for XYZ service pods only."
```

## Non-preempting PriorityClass 
*FEATURE STATE: Kubernetes v1.19 [beta]*

具有`PreemptionPolicy: Never`抢占策略的Pods，在调度队列中会排在优先级较低的Pods前面，但是它们不能抢占其他pod。等待调度的非抢占式Pod将停留在调度队列中，直到有足够的资源空闲，并且可以对其进行调度。非抢占式Pod和其他Pod一样，都受到调度器退避 back-off的影响。也就是说，如果调度程序尝试调度这些pod，但结果却无法调度它们时，调度器将以较低的频率重试这些Pod，从而允许优先级较低的其他pod可以在它们之前被调度。

非抢占式Pod仍可能被其他高优先级Pod抢占。

PreemptionPolicy默认为`PreemptLowerPriority`，这将允许pod抢占较低优先级的pod（现在默认的行为就是这样）。如果PreemptionPolicy设置为`Never`，则使用该PriorityClass的pod将是非抢占的。

一个示例用例是针对数据科学工作负载的。用户可以提交一个他们希望优先于其他工作负载的作业，但不希望通过抢占运行的pods而放弃现有的工作。一旦有足够的集群资源可用，具有`PreemptionPolicy: Never`抢占策略的高优先级的作业会被安排到队列中其他pods的前面。

### Example Non-preempting PriorityClass 
```yaml
apiVersion: scheduling.k8s.io/v1
kind: PriorityClass
metadata:
  name: high-priority-nonpreempting
value: 1000000
preemptionPolicy: Never
globalDefault: false
description: "This priority class will not cause other pods to be preempted."
```

## Pod priority 

在拥有一个或多个PriorityClass之后，可以创建pod，并在它们的规范中指定其中一个PriorityClass名称。优先级许可控制器使用priorityClassName字段并填充优先级的整数值。如果没有找到优先级类，Pod会被拒绝。

下面的YAML是一个Pod配置的示例，它使用在前面的示例中创建的PriorityClass。优先级许可控制器将检查规范并将Pod的优先级解析为1000000。

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: nginx
  labels:
    env: test
spec:
  containers:
  - name: nginx
    image: nginx
    imagePullPolicy: IfNotPresent
  priorityClassName: high-priority
```

### Effect of Pod priority on scheduling order
当Pod优先级启用时，调度器按优先级排序pending Pod，并且在调度队列中，一个挂起的Pod被放在其他优先级较低的挂起的Pod之前。因此，在满足调度要求的情况下，优先级较高的Pod可能比优先级较低的Pod提前调度。如果这样的Pod无法被调度，调度程序将继续并尝试调度其他优先级较低的Pod

## Preemption

当pod被创建时，它们会进入队列等待调度。调度程序从队列中选择一个Pod并尝试在节点上调度它。如果没有找到满足Pod所有指定要求的节点，则会为挂起的Pod触发抢占逻辑。如果挂起的Pod叫P。抢占逻辑试图找到一个节点，在该节点上删除一个或多个优先级低于P的Pod，使P在该节点上被调度。如果找到这样的节点，一个或多个优先级较低的pod将从节点中移出。当Pods移除后，可以在节点上调度P。

### User exposed information 用户公开的信息

当Pod P抢占节点N上的一个或多个Pod时，Pod P状态的nominatedNodeName字段被设置为Node N的name。该字段帮助调度器跟踪为Pod P保留的资源，并向用户提供有关集群中抢占的信息。

请注意，Pod P不一定被安排到“指定节点”。当其他Pod被抢占后，被抢占的Pod会被优雅终止。如果另一个节点在调度器等待Pods优雅终止时变为可用，那么调度程序将使用另一个节点来调度Pod P。因此，Pod spec的`nominatedNodeName`和`nodeName`并不总是相同的。另外，如果调度程序抢占节点N上的Pods，但是当Pod P优先级更高的Pod到达时，调度器可能会将节点N分配给新的高优先级Pod。在这种情况下，调度器清除Pod P的`nominatedNodeName`，通过这样做，调度器使Pod P有资格抢占另一个节点上的Pods。

### Limitations of preemption 

#### Graceful termination of preemption victims 

当Pod被抢占时，受害者(被抢占的Pod)有一个[优雅终止期](https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#pod-termination)。他们有一段时间可以来完成工作然后退出。如果不这样做，这些Pod就会直接被killed掉。这个优雅终止期会在调度器抢占Pod和节点（N）上可以调度挂起的Pod（P）之间创造了一个时间间隙。同时，调度程序继续调度其他挂起的pod。当受害者退出或被终止时，调度程序会尝试调度挂起队列中的pod。因此，在调度程序抢占受害者和Pod P被调度之间通常有一个时间间隔。为了减小这种时间间隙，可以将低优先级Pod的优雅终止周期设置为零或一个较小的数值。

#### PodDisruptionBudget is supported, but not guaranteed 

[PodDisruptionBudget](https://kubernetes.io/docs/concepts/workloads/pods/disruptions/)（PDB）允许应用程序所有者限制复制的应用程序中同时由于自愿中断而减少的Pod数量。Kubernetes在抢占Pods时支持PDB，但是只尽最大的努力去遵循PDB的规则。调度器试图找到不违反PDB的可以被抢占的Pod，但是如果没有发现这样的Pod，抢占仍然会发生，并且低优先级的pod将被移除，尽管它们会违反PDB的规则。

#### Inter-Pod affinity on lower-priority Pods

只有当下面问题的答案是“是”时，抢占发生时节点才会被考虑进去。这个问题是：“如果所有优先级低于挂起Pod的Pod都从节点上移除，那么该挂起的Pod是否可以在该节点上进行调度？”

> :warning: 注意：抢占并不一定要删除所有低优先级的pod。如果可以通过只移除部分（非全部）低优先级的Pod来调度挂起的Pod，那么只移除这一小部分低优先级Pod。即便如此，对上述问题的回答也必须是yes才行。如果答案是no，则不考虑在这个节点进行抢占。

如果一个挂起的Pod与节点上的一个或多个低优先级Pod有inter-pod affinity亲和力，并且在没有这些低优先级Pod的情况下，inter-pod affinity的规则不能满足。在这种情况下，调度程序不会抢占节点上的任何pod。而是寻找另一个节点。调度程序可能会找到合适的节点，也可能找不到。无法保证挂起的Pod的调度。

对于这个问题，我们建议的解决方案是只对同等或更高优先级的Pod创建inter-Pod affinity(Pod间的亲和力)。

#### Cross node preemption 

假设一个节点N正在考虑抢占，以便可以在N上调度一个挂起的pod P。只有当另一个节点上的Pod被抢占时，P在N上才可行。下面是一个例子：
- Pod P正在被考虑调度到Node N上
- Pod Q正在和Node N同一Zone的另一个Node上运行
- Pod P和Pod Q之间存在Zone-wide anti-affinity全区反亲和力(`topologyKey: topology.kubernetes.io/zone`)
- Pod P与该区其它Pod间无其他 anti-affinity 抗亲和力的情况。
- 为了在节点N上调度Pod P，Pod Q此时可以被抢占，但调度器不执行跨节点抢占。因此，Pod P在节点N上被认为是不可调度的。

如果Pod Q从它的节点上被移除，就没有Pod反亲和力的冲突了，这样Pod P就有可能被安排在节点N上。

如果有足够的需求，并且我们能找到一个性能较好的算法时，我们可以考虑在将来的版本中增加跨节点抢占的功能。

## Troubleshooting 

Pod优先级和抢占可能会产生一些副作用。下面是一些潜在问题的例子和处理它们的方法

### Pods are preempted unnecessarily 

抢占在存在资源压力的情况下从集群中移除现有的pod，为更高优先级的挂起pod腾出空间。如果您错误地将高优先级分配给某些pod，这些无意中的高优先级Pods可能会导致集群发生抢占。Pod优先级通过在Pod规范中设置priorityClassName字段来指定。然后解析优先级数值会填充到podSpec的priority字段中。

为了解决这个问题，您可以将这些pod的priorityClassName更改为较低优先级的类，或者将该字段留空。默认情况下，空priorityClassName优先级为零。

当一个Pod被抢占时，会记录一些关于被抢占Pod的事件。只有当集群没有足够的资源来容纳Pod时，才会发生抢占。在这种情况下，只有当挂起的Pod（抢占者）的优先级高于被抢占的Pod时，才会发生抢占。当没有挂起的Pod，或者挂起的Pod的优先级等于或低于被抢占Pod时，不会发生抢占。如果在这种情况下发生抢占，请提交问题给我们。

### Pods are preempted, but the preemptor is not scheduled 

当pod被抢占时，它们接收到请求的优雅终止时间，默认为30秒。如果被抢占的Pod在这段时间内没有终止，它们将被强制终止。一旦所有的被抢占者都终止了，抢占者Pod就可以安排了。

当抢占者Pod等待被抢占者终止时，可能会创建一个适合于同一节点的更高优先级的Pod。在这种情况下，调度程序将调度优先级较高的Pod而不是抢占者Pod。

这是预期的行为：优先级较高的Pod应该取代优先级较低的Pod。其他控制器操作，例如集群[自动调整](https://kubernetes.io/docs/tasks/administer-cluster/cluster-management/#cluster-autoscaling)，最终可能提供一些容量让挂起的pod可以调度。

### Higher priority Pods are preempted before lower priority pods

调度器尝试查找到可以运行挂起Pod的节点。如果找不到节点，调度器将尝试从任意节点删除优先级较低的pod，以便为挂起的pod留出空间。如果拥有低优先级Pod的节点不能运行挂起的Pod，调度程序可以选择另一个拥有较高优先级Pod的节点（与另一个节点上的Pod相比）进行抢占。被抢占者的优先级必须低于抢占者的优先级。

当有多个节点可供抢占时，调度器尝试选择一组具有最低优先级的pod的节点。但是，如果这样的Pods具有`PodDisruptionBudget`，如果它们被抢占会违反PDB规则时，那么调度程序会选择另一个具有较高优先级Pods的节点。

当存在多个节点进行抢占，并且上面的场景都不适用时，调度器将选择拥有优先级最低Pods的节点。


## Interactions between Pod priority and quality of service

Pod优先级和[QoS类(Quality of Service Class)](https://kubernetes.io/docs/reference/glossary/?all=true#term-qos-class)是两个正交的特性，交互很少，并且根据Pod的QoS类设置Pod的优先级没有默认限制。调度程序的抢占逻辑在选择抢占目标时不考虑QoS。抢占考虑Pod优先级，并尝试选择一组优先级最低的目标。只有当移除最低优先级的Pods不足以让调度器调度抢占器Pod时，或者如果最低优先级的Pods受`PodDisruptionBudget`保护，则考虑优先权较高的Pods。

唯一一起考虑QoS和Pod优先级的组件是[kubelet out-of-resource eviction](https://kubernetes.io/docs/tasks/administer-cluster/out-of-resource/)。kubelet会对Pod进行排序，首先按照它们对资源的使用是否超过请求排序，然后按优先级排序，然后根据相对于Pods调度请求消耗的计算资源进行排序。有关详细信息，请参见[evicting end-user pods](https://kubernetes.io/docs/tasks/administer-cluster/out-of-resource/#evicting-end-user-pods)。

当pod的使用量不超过其请求时，kubelet out-of-resource eviction不会驱逐pod。如果一个低优先级的Pod没有超过它的请求，它不会被逐出。另一个优先级高的使用量超过其请求的Pod可能会被逐出。

## What's next

Read about using ResourceQuotas in connection with PriorityClasses: [limit Priority Class consumption by default](https://kubernetes.io/docs/concepts/policy/resource-quotas/#limit-priority-class-consumption-by-default)






