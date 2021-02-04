# Scheduler Performance Tunning

**FEATURE STATE: Kubernetes v1.14 [beta]**

[kube-scheduler ](https://kubernetes.io/docs/concepts/scheduling-eviction/kube-scheduler/#kube-scheduler)是Kubernetes的默认调度器。它负责在集群节点上放置pod。

群集中满足Pod调度要求的节点称为Pod的可行节点。调度器为Pod找到可行节点，然后运行一组函数来对可行节点进行评分，从可行节点中选择得分最高的节点来运行Pod。然后，调度程序在一个名为Binding的进程中将此决定通知给API服务器。

本页介绍与大型Kubernetes集群相关的性能优化。

在大型集群中，您可以调整调度器的行为，在延迟（快速放置新的pod）和准确性（调度器很少做出错误的放置决策）之间平衡调度结果。

您可以通过kube-scheduler设置`percentageOfNodesToScore`配置来进行优化。此`KubeSchedulerConfiguration`设置确定了在集群中调度节点的阈值。

### Setting the threshold

PercentageOfNodesCore选项接受0到100之间的整数值。值0是一个特殊的数字，它指示kube调度程序应该使用其编译的默认值。如果您将percentageOfNodesToScore设置为100以上，kube调度器的行为和设置为100一样。

要更改该值，请编辑kube调度程序配置文件（可能是`/etc/kubernetes/config/kube-scheduler.yaml`)，然后重新启动调度器。

在你做了这个改变之后，你可以运行：
```sh
kubectl get pods -n kube-system | grep kube-scheduler
```

并验证kube调度程序组件是否正常。

## Node scoring threshold 

为了提高调度性能，kube调度器可以在找到足够多的可行节点后停止寻找。在大型集群中，与考虑每个节点的方法相比，这节省了时间。

指定阈值来表示多少个节点就足够了，以集群中所有节点的整数百分比表示该阈值。kube调度器将其转换为节点的整数个数。在调度过程中，如果kube调度器已经识别出足够多的可行节且超过了配置的百分比，kube调度器将停止搜索更多的可行节点，进入[评分阶段](https://kubernetes.io/docs/concepts/scheduling-eviction/kube-scheduler/#kube-scheduler-implementation)。

[调度器在节点上迭代的方式](https://kubernetes.io/docs/concepts/scheduling-eviction/scheduler-perf-tuning/#how-the-scheduler-iterates-over-nodes)详细描述了该过程。


### Default threshold

如果不指定阈值，Kubernetes会使用一个线性公式计算一个数字，对于100个节点的集群，该公式得出50%的结果，对于5000个节点的集群，则得出10%的结果。自动计算值的下限为5%。

这意味着，无论集群有多大，kube调度程序总是至少获得集群的5%，除非您明确地将percentageOfNodesToScore设置为小于5。

如果希望调度程序对集群中的所有节点评分，请将PercentageOfNodesCore设置为100


## Example 

下面是一个示例配置，它将`percentageOfNodesToScore`设置为50%。
```yaml
apiVersion: kubescheduler.config.k8s.io/v1alpha1
kind: KubeSchedulerConfiguration
algorithmSource:
  provider: DefaultProvider

...

percentageOfNodesToScore: 50
```

## Tuning percentageOfNodesToScore 

percentageOfNodesToScore必须是介于1和100之间的值，默认值是根据群集大小计算的。还有一个硬编码的最小值, 50个节点。

> 注：
> 在少于50个可行节点的集群中，调度程序仍然检查所有节点，这仅仅是因为没有足够的可行节点来提前停止调度程序的搜索。
>
> 在一个小集群中，如果设置了一个较低的percentageOfNodesToScore值，出于类似的原因，您的更改将不会产生或几乎没有影响。
>  
> 如果您的集群有几百个或更少的节点，请将此配置选项保留为默认值。进行更改不太可能显著提高调度程序的性能。

设置此值时需要考虑的一个重要细节是，当检查集群中较少数量的节点是否可行时，某些节点不会被发送并针对Pod进行评分。结果就是，一个节点得分很高，但可能不会到评分阶段。这将导致Pod调度到不太理想的Node上。

您应该避免将`percentageOfNodesToScore`设置得非常低，这样kube调度器就不会频繁地做出糟糕的Pod布局决策。避免将`percentageOfNodesToScore`设置为低于10%的值，除非调度程序的吞吐量对应用程序至关重要，并且节点的分数并不重要。换句话说，只要您愿意在任何可行的节点上运行Pod，就可以忽略10%的要求。

## How the scheduler iterates over Nodes

本节是为那些想了解此功能的内部细节的人准备的。

为了给集群中的所有节点一个公平的机会来运行Pods，调度器以循环的方式(round robin fashion)迭代节点。可以想象节点在一个数组中。调度程序从数组的起始位置开始，检查节点的可行性，直到找到足够的节点（由percentageOfNodesToScore指定）。对于下一个Pod，调度程序从上一次循环迭代停止的位置开始继续。

如果节点在多个zones中，调度器会迭代不同zones中的节点，以确保在可行性检查中考虑来自不同zones的节点。例如，考虑两个zones中的六个节点：

```
Zone 1: Node 1, Node 2, Node 3, Node 4
Zone 2: Node 5, Node 6
```

调度程序按以下顺序评估节点的可行性：
```
Node 1, Node 5, Node 2, Node 6, Node 3, Node 4
```

在检查了所有节点之后，它返回到 Node 1。



