# Scheduling Framework
**FEATURE STATE: Kubernetes v1.15 [alpha]**

调度框架是Kubernetes调度器的一种可插拔式的架构，它使得调度器的定制变得容易。它向现有的调度程序添加了一组新的“插件”api。插件可以被编译到调度程序中。这些api允许将大多数调度特性作为插件实现，同时保持调度“核心”的简单性和可维护性。有关框架设计的更多技术信息，请参阅[调度框架的设计方案](https://github.com/kubernetes/enhancements/blob/master/keps/sig-scheduling/624-scheduling-framework/README.md)。

## Framework workflow

调度框架定义了几个扩展点。调度程序插件通过注册在一个或多个扩展点进行调用。其中一些插件可以更改调度决策，也有些插件只能查看信息。

每次尝试调度一个Pod可以分为两个阶段，调度周期scheduling cycle和绑定周期binding cycle。

## Scheduling Cycle & Binding Cycle

调度周期为Pod选择一个节点，绑定周期则将该决策应用于集群。调度周期和绑定周期一起被称为“调度上下文”。

调度周期是串行运行的，而绑定周期可以并发运行。

如果Pod被确定为不可调度或存在内部错误，则可以中止调度或绑定循环。Pod将返回队列并重试。

## Extension points 

下图显示了Pod的调度上下文和调度框架公开的扩展点。在这幅图中，Filter相当于Predicate，Scoring相当于Priority function。

一个插件可以在多个扩展点注册以执行更复杂或有状态的任务。

![scheduling framework extension points](https://d33wubrfki0l68.cloudfront.net/4e9fa4651df31b7810c851b142c793776509e046/61a36/images/docs/scheduling-framework-extensions.png)


### QueueSort

这些插件用于对调度队列中的pod进行排序。队列排序插件本质上提供了一个`Less(Pod1, Pod2)`函数。一次只能启用一个队列排序插件。

### PreFilter

这些插件用于预处理关于Pod的信息，或者检查集群或Pod必须满足的某些条件。如果预过滤器插件返回错误，调度周期将中止。

### Filter

这些插件用于过滤掉不能运行Pod的节点。对于每个节点，调度程序将按配置的顺序调用过滤器插件。如果任何过滤器插件将节点标记为不可行，则不会为该节点调用其余插件。可以并发过滤节点。

### PostFilter

这些插件是在过滤器阶段之后调用的，但只有在没有为pod找到可行的节点时才调用。插件按配置的顺序调用。如果任何postFilter插件将节点标记为可调度的，则不会调用其余的插件。典型的后过滤器实现是抢占（preemption），它试图通过抢占其他pod来使pod可调度。

### PreScore

这些插件用于执行“预评分”工作，这会生成一个可共享的状态供Score插件使用。如果预存储插件返回错误，则调度周期将中止。

### Score

这些插件用于对通过筛选阶段的节点进行排序。调度程序将为每个节点调用每个评分插件。将有一个明确的整数范围，代表最低和最高分数。在NormalizeScore阶段之后，调度器将根据配置的插件权重计算来自所有插件的节点总得分

### NormalizeScore

这些插件用于在调度器计算节点的最终排名之前修改分数。注册此扩展点的插件将使用来自同一种插件的分数结果来进行调用。每个调度周期每个插件调用一次。

例如，假设插件`BlinkingLightScorer`根据节点的闪烁灯光数量对节点进行排序。

```c++
func ScoreNode(_ *v1.pod, n *v1.Node) (int, error) {
    return getBlinkingLightCount(n)
}
```

但是，与NodeScoreMax相比，闪烁灯的最大数量可能很小。为了解决这个问题，BlinkingLightScorer还应该注册这个扩展点。
```c++
func NormalizeScores(scores map[string]int) {
    highest := 0
    for _, score := range scores {
        highest = max(highest, score)
    }
    for node, score := range scores {
        scores[node] = score*NodeScoreMax/highest
    }
}
```

如果任何NormalizeScore插件返回错误，调度周期将中止。

> 注意：希望执行“预保留pre-reserve”工作的插件应该使用NormalizeScore扩展点。

### Reserve 

实现了Reserve扩展的插件有两个方法，即`Reserve`和`Unreserve`，分别支持Reserve和Unreserve两个信息调度阶段。维护运行时状态的插件（也称为“有状态插件”）应该使用这些阶段，从而当节点上的资源为给定Pod保留或不保留时得到调度器的通知。

Reserve阶段发生在调度器实际将Pod绑定到其指定节点之前。它的存在是为了防止调度程序在等待绑定成功的时候可能出现的竞争情况。每个Reserve插件的Reserve方法可能成功也可能失败；如果一个Reserve方法调用失败，则不会执行后续插件，并且认为Reserve阶段失败。如果所有插件的Reserve方法成功，Reserve阶段被认为是成功的，并执行剩余的调度周期和绑定周期。

如果Reserve阶段或后续阶段失败，将触发Unreserve阶段。当这种情况发生时，所有Reserve插件的Unreserve方法将以Reserve方法调用的相反顺序执行。此阶段是为了清除与reserved Pod相关的状态。

> 注意：Reserve插件中Unreserve方法的实现必须是幂等的，并且不能失败。


### Permit

在每个Pod的调度周期结束时调用Permit插件，以阻止或延迟与候选节点的绑定。permit插件可以执行以下三种操作之一：
1. approve
    一旦所有的Permit插件都批准了Pod，Pod就会被发送去绑定。
2. deny
   如果任何Permit插件拒绝Pod，Pod将返回到调度队列。这将触发Reserve插件中的Unreserve阶段。
3. wait (with a timeout)
   如果一个Permit插件返回“wait”，那么这个Pod将被保存在一个内部的“waiting” Pods列表中，并且这个Pod的绑定循环开始，但是会直接阻塞，直到它被批准为止。如果超时，wait会变为deny，Pod返回到调度队列，触发Reserve插件中的Unreserve阶段。

> 注意：虽然任何插件都可以访问“waiting” Pods列表并批准它们（请参见[FrameworkHandle](https://github.com/kubernetes/enhancements/blob/master/keps/sig-scheduling/20180409-scheduling-framework.md#frameworkhandle)），但我们只希望使用permit插件来批准处于“waiting” 状态的reserved Pods的绑定。一旦Pod被批准，它将被发送到[预绑定](https://kubernetes.io/docs/concepts/scheduling-eviction/scheduling-framework/#pre-bind)阶段。


### PreBind

这些插件用于在绑定Pod之前执行所需的任何工作。例如，预绑定插件可以提供一个网络卷，并在允许Pod在目标节点上运行之前将其装载到目标节点上。

如果任何PreBind插件返回错误，Pod将被[拒绝](https://kubernetes.io/docs/concepts/scheduling-eviction/scheduling-framework/#reserve)并返回到调度队列。


### Bind

这些插件用于将Pod绑定到节点。在所有PreBind插件完成之前，不会调用Bind插件。按照配置的顺序调用每个Bind插件。Bind插件可以选择是否处理Pod。如果Bind插件选择处理一个Pod，其余的Bind插件将跳过。

### PostBind

这是一个信息扩展点。PostBind插件在Pod成功绑定后调用。这是绑定循环的结束，可用于清理关联的资源。



## Plugin API

使用插件API有两个步骤。首先，插件必须注册并配置，然后使用扩展点接口。扩展点接口具有以下形式。

```c++
type Plugin interface {
    Name() string
}

type QueueSortPlugin interface {
    Plugin
    Less(*v1.pod, *v1.pod) bool
}

type PreFilterPlugin interface {
    Plugin
    PreFilter(context.Context, *framework.CycleState, *v1.pod) error
}

// ...
```


## Plugin configuration

您可以在调度程序配置中启用或禁用插件。如果您使用Kubernetes v1.18或更高版本，那么大多数调度[插件](https://kubernetes.io/docs/reference/scheduling/config/#scheduling-plugins)都在使用中，并且默认情况下启用。

除了默认插件之外，您还可以实现自己的调度插件，并将它们与默认插件一起配置。您可以访问[调度程序插件](https://github.com/kubernetes-sigs/scheduler-plugins)了解更多详细信息。

如果您使用的是kubernetes v1.18或更高版本，您可以将一组插件配置为调度器概要文件，然后定义多个概要文件以适应各种工作负载。请参阅[多个配置文件](https://kubernetes.io/docs/reference/scheduling/config/#multiple-profiles)以了解更多信息。

