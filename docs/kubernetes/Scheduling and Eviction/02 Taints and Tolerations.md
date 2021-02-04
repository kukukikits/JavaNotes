# Taints and Tolerations

节点亲和力是pod的一个属性，它将pod吸引到一组节点上（作为偏好或硬需求）。污点恰恰相反——它们允许一个节点排斥一组Pod。

容忍应用于pod，允许（但不要求）pods调度到具有与污点匹配的节点上。

污染和容忍一起工作，以确保pod不会被调度到不合适的节点上。一个或多个污点被应用到一个节点上，这标志着该节点不应该接受任何不能容忍这些污点的pod

## Concepts

使用kubectl taint向节点添加污点。例如，
```sh
kubectl taint nodes node1 key=value:NoSchedule
```

在节点1上放置污点。污点具有键key、值value和污点effect NoSchedule。这意味着除非Pod具有匹配的容忍度，否则pod就不能调度到node1上。

要删除上述命令添加的污点，可以运行：
```sh
kubectl taint nodes node1 key:NoSchedule-
```

您可以在Pod的PodSpec中设置容忍度。以下两种容忍“匹配”上述kubectl taint定义的污染，因此具有下面任一一个容忍的pod都可以调度到node1上：

```yaml
tolerations:
- key: "key"
  operator: "Equal"
  value: "value"
  effect: "NoSchedule"
```
```yaml
tolerations:
- key: "key"
  operator: "Exists"
  effect: "NoSchedule"
```

下面是一个使用容忍度的pod示例：

pods/pod-with-toleration.yaml 
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
  tolerations:
  - key: "example-key"
    operator: "Exists"
    effect: "NoSchedule"
```

运算符的默认值为“Equal”。

如果key相同且effect相同，则容忍“匹配”一个污点，并且：
- 运算符存在（在这种情况下不应指定value），或
- 运算符是Equal且value相等。

> 注：
> 有两种特殊情况：
> - 操作符为Exists且key为空，匹配所有键、值和效果，这意味着它将容忍一切。
> - effect为空，用键匹配所有的effect。

上面的例子使用了`NoSchedule`的effect。或者，您可以使用`PreferNoSchedule`的effect。这是NoSchedule的一个“首选项”或“软”版本——系统将尽量避免在节点上放置一个不能容忍污染的pod，但这不是必需的。第三种effect是`NoExecute`，稍后描述。

您可以在同一个节点上放置多个污点，也可以在同一个pod上放置多个容忍。Kubernetes处理多个污点和容忍的方式就像一个过滤器：从一个节点的所有污点开始，然后忽略那些pod具有匹配容忍度的污点；其余未被忽略的污点对pod有不同的effect。特别地，
- 如果至少有一个未被忽略的污点有`NoSchedule` effect，那么Kubernetes不会将pod调度到该节点上
- 如果没有effect为`NoSchedule`的未被忽略的污点，但是至少有一个具有`PreferNoSchedule` effect的未被忽略的污点，那么Kubernetes将尝试不将pod调度到该节点上
- 如果至少有一个未被忽略且effect为`NoExecute`的污点，那么pod将被逐出节点（如果Pod已经在节点上运行），并且不会被调度到节点上（如果Pod还没有在节点上运行）。

例如，假设您污染了这样一个节点
```sh
kubectl taint nodes node1 key1=value1:NoSchedule
kubectl taint nodes node1 key1=value1:NoExecute
kubectl taint nodes node1 key2=value2:NoSchedule
```

Pod有两种容忍：
```yaml
tolerations:
- key: "key1"
  operator: "Equal"
  value: "value1"
  effect: "NoSchedule"
- key: "key1"
  operator: "Equal"
  value: "value1"
  effect: "NoExecute"
```

在这种情况下，pod无法调度到节点上，因为没有与第三个污染匹配的容忍度。但是如果它已经在节点上运行了，那么它将能够继续运行，因为第三个污点是pod不能容忍的唯一一个。

通常，如果节点中添加了一个效果为NoExecute的污点，那么任何不容忍该污点的pod都将被立即逐出，而能够容忍该污点的pod将永远不会被逐出。但是，具有NoExecute效果的容忍可以指定一个可选的`tolerationSeconds`字段，该字段指示添加污点后pod将在节点上停留多长时间。例如，
```yaml
tolerations:
- key: "key1"
  operator: "Equal"
  value: "value1"
  effect: "NoExecute"
  tolerationSeconds: 3600
```

这意味着如果这个pod正在运行并且一个匹配的污点被添加到节点，那么pod在3600秒内仍然与节点保持绑定，时间过后被逐出。如果污点在3600秒以内被清除，则Pod不会被驱逐。

## Example Use Cases

污点和容忍是一种灵活的方法，可以引导Pod远离节点或驱逐不应该运行的Pod。一些用例是
- **专用节点**：如果您想将一组节点专用于一组特定的用户，您可以向这些节点添加一个污点（例如，`kubectl taint nodes nodename dedicated=groupName:NoSchedule`)然后给这些用户的pod添加相应的容忍度（这可以通过编写一个自定义的[许可控制器](https://kubernetes.io/docs/reference/access-authn-authz/admission-controllers/)来实现）。具有容忍的pod将被允许使用受污染（专用）节点以及集群中的任何其他节点。如果要将节点专用于特定用户并确保用户只能使用专用节点，则应在同一组节点上添加一个类似于污点的标签（例如，dedicated=groupName），并且许可控制器应该另外添加一个节点亲和性，以要求pods只能调度到标有dedicated=groupName的节点上。
- **具有特殊硬件的节点**：在一个集群中，一小部分节点有专门的硬件（例如gpu），最好将不需要专用硬件的pod放在这些节点之外，为以后到达的需要专用硬件的pod留出空间。这可以通过污染具有专用硬件的节点来完成（例如`kubectl taint nodes nodename special=true:NoSchedule`或`kubectl taint nodes nodename special=true:PreferNoSchedule`)，并为使用特殊硬件的pod添加相应的容忍度。和在专用节点用例中一样，使用自定义的[许可控制器](https://kubernetes.io/docs/reference/access-authn-authz/admission-controllers/)来应用容忍可能是最容易的。例如，建议使用[扩展资源](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/#extended-resources)来表示特殊硬件，用扩展资源名称污染您的特殊硬件节点，并运行[ExtendedResourceTolerance](https://kubernetes.io/docs/reference/access-authn-authz/admission-controllers/#extendedresourcetoleration)许可控制器。现在，因为节点被污染了，没有容忍度的pod不会在它们上面调度。但是，当您提交一个请求扩展资源的pod时，`ExtendedResourceToleration`许可控制器将自动向pod添加正确的容忍，并且该pod调度到特定的硬件节点上。这将确保这些特殊的硬件节点专用于请求此类硬件的pod，并且您不必手动向pod添加容忍度。
- **基于污点的逐出**：当存在节点问题时，每个pod可配置的逐出行为，这将在下一节中描述

## Taint based Evictions 
**FEATURE STATE: Kubernetes v1.18 [stable]**

上面提到的NoExecute污染效果会影响已经在节点上运行的pod，如下所示
* 不能忍受污染的Pod立即被驱逐
* 容忍污染但没有指定`tolerationSeconds`的Pod会永远保持绑定
* 容忍污染且指定了`tolerationSeconds`的Pod，会在指定的容忍时间内保持绑定

当某些条件为真时，节点控制器会自动污染节点。内置以下污点：
- `node.kubernetes.io/not-ready`: 节点未就绪。这对应于NodeCondition `Ready`为“False”。
- `node.kubernetes.io/unreachable`：无法从节点控制器访问节点。这对应于NodeCondition Ready为“Unknown”。
- `node.kubernetes.io/out-of-disk`：节点磁盘空间不足
- `node.kubernetes.io/memory-pressure`：节点有内存压力。
- `node.kubernetes.io/disk-pressure`：节点有磁盘压力。
- `node.kubernetes.io/network-unavailable`：节点的网络不可用。
- `node.kubernetes.io/unschedulable`：节点不可调度。
- `node.cloudprovider.kubernetes.io/uninitialized`：当kubelet使用“外部”云提供程序启动时，会在节点上设置此污点，以将其标记为不可用。在云控制器管理器中的一个控制器初始化这个节点之后，kubelet会删除这个污点。

如果一个节点要被逐出，节点控制器或kubelet会添加相关的`NoExecute`污点。如果故障恢复，kubelet或节点控制器会移除相关污点。

> 注意：控制平面限制向节点添加节点新污点的速率。此速率限制管理当许多节点在同一时刻无法访问（例如：如果网络中断）时触发的逐出次数。

您可以为Pod指定`tolerationSeconds`，来定义Pod和故障或无响应节点保持绑定的时长。

例如，您可能希望在发生网络分区时，将具有大量本地状态的应用程序长时间绑定到节点，希望分区能够恢复，从而避免pod逐出。你对那个Pod的容忍度可能看起来像：
```yaml
tolerations:
- key: "node.kubernetes.io/unreachable"
  operator: "Exists"
  effect: "NoExecute"
  tolerationSeconds: 6000
```

> 注：
> Kubernetes会自动添加`node.kubernetes.io/not-ready`、`node.kubernetes.io/unreachable`以及`tolerationSeconds=300`，除非您或控制器显式设置这些容忍。
> 这些自动添加的容忍度意味着在检测到这两个中的其中一个问题后，pod将与节点保持5分钟的绑定。

对于以下没有设置`tolerationSeconds`的污染，[DaemonSet](https://kubernetes.io/docs/concepts/workloads/controllers/daemonset/) pods创建时会指定`NoExecute`容忍：
* node.kubernetes.io/unreachable
* node.kubernetes.io/not-ready
  
这可以确保守护进程不会因为这些问题而被逐出。


## Taint Nodes by Condition

节点lifecycle controller会自动创建与节点状态相对应的`NoSchedule`效果的污点。同样地，调度程序不会检查节点状态，而是会检查污染。这样可以确保节点状态不会影响已经调度到节点上的内容。用户可以通过添加适当的Pod容忍度来选择忽略掉节点的一些问题（这些问题以节点状态形式表示）。

守护进程控制器会自动向所有守护进程添加以下NoSchedule容忍，以防止守护进程中断。
- `node.kubernetes.io/memory-pressure`
- `node.kubernetes.io/disk-pressure`
- `node.kubernetes.io/out-of-disk`(仅适用于关键Pod)
- `node.kubernetes.io/unschedulable`(1.10或之后的版本)
- `node.kubernetes.io/network-unavailable`(仅主机网络)

添加这些容忍可以确保向后兼容性。您还可以向守护进程添加任意容忍

## What's next
* Read about [out of resource handling](https://kubernetes.io/docs/tasks/administer-cluster/out-of-resource/) and how you can configure it
* Read about [pod priority](https://kubernetes.io/docs/concepts/configuration/pod-priority-preemption/)





