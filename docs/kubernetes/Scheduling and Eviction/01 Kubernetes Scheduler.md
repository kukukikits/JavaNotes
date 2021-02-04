# Kubernetes Scheduler

在Kubernetes中，调度是指确保pod与节点匹配，以便Kubelet可以运行Pod。

## Scheduling overview 

调度程序监视没有分配节点的新创建的pod。对于调度程序发现的每个Pod，调度程序都负责为该Pod找到运行的最佳节点。调度器在考虑到下面描述的调度原则的情况下做出调度决策。

如果您想了解pod为什么会被放置到某个特定的节点上，或者您计划自己实现一个定制的调度器，本页将帮助您了解调度。

## kube-scheduler 

kube调度器是Kubernetes的默认调度器，作为控制平面的一部分运行。kube调度器的设计是这样的，如果您愿意和需要，您可以编写自己的调度组件并使用它。

对于每个新创建的pod或其他未调度的pod，kube调度器会选择一个最佳节点来运行它们。然而，每个Pod中的容器对资源有不同的要求，每个Pod也有不同的要求。因此，需要根据具体的调度要求对现有节点进行过滤。

在集群中，满足Pod调度要求的节点称为可行节点。如果没有合适的节点，pod将保持未调度状态，直到调度器能够找到合适的节点并调度它。

调度器为一个Pod找到可行节点，然后运行一组函数对可行节点进行评分，并从可行节点中选择得分最高的节点来运行Pod。然后，调度程序在一个名为binding的进程中将此决定通知给API服务器。

调度决策需要考虑的因素包括个体和集体资源需求、硬件/软件/策略约束、亲和性和反亲和性规范、数据局部性、工作负载间干扰等。

### Node selection in kube-scheduler

kube scheduler通过两步操作为pod选择一个节点：

1. Filtering
2. Scoring

过滤步骤会找到一组可以调度Pod的节点。例如，PodFitsResources过滤器检查候选节点是否有足够的可用资源来满足Pod的特定资源请求。在这一步之后，节点列表中是任何合适的节点；通常会有多个。如果这个列表是空的，那么这个Pod是不可调度的。

在打分的步骤中，调度器对剩余节点进行排序，以选择最合适的Pod位置。调度程序根据激活的评分规则，为过滤后的每个节点打分。

最后，kube调度器将Pod分配给分数最高的节点。如果有多个节点得分相等，kube调度器会随机选择其中一个。

有两种支持的方法来配置调度程序的筛选和评分行为：

1. [调度策略](https://kubernetes.io/docs/reference/scheduling/policies)允许您配置用于筛选的断言Predicates和评分的优先级Priorities。
2. [调度概要文件](https://kubernetes.io/docs/reference/scheduling/config/#profiles)允许您配置实现不同调度阶段的插件，包括：QueueSort、Filter、Score、Bind、Reserve、Permit等。您还可以配置kube调度程序来运行不同的概要文件。

## What's next
* Read about [scheduler performance tuning](https://kubernetes.io/docs/concepts/scheduling-eviction/scheduler-perf-tuning/)
* Read about [Pod topology spread constraints](https://kubernetes.io/docs/concepts/workloads/pods/pod-topology-spread-constraints/)
* Read the [reference documentation](https://kubernetes.io/docs/reference/command-line-tools-reference/kube-scheduler/) for kube-scheduler
* Learn about [configuring multiple schedulers](https://kubernetes.io/docs/tasks/extend-kubernetes/configure-multiple-schedulers/)
* Learn about [topology management policies](https://kubernetes.io/docs/tasks/administer-cluster/topology-manager/)
* Learn about [Pod Overhead](https://kubernetes.io/docs/concepts/scheduling-eviction/pod-overhead/)
* Learn about scheduling of Pods that use volumes in:
  * [Volume Topology Support](https://kubernetes.io/docs/concepts/storage/storage-classes/#volume-binding-mode)
  * [Storage Capacity Tracking](https://kubernetes.io/docs/concepts/storage/storage-capacity/)
  * [Node-specific Volume Limits](https://kubernetes.io/docs/concepts/storage/storage-limits/)

