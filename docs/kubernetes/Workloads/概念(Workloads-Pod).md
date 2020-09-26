# Workloads
## Pods
pod是您可以在Kubernetes中创建和管理的最小的可部署计算单元。

Pod（如一群鲸鱼或豌豆荚）是一组一个或多个容器，具有共享的存储/网络资源，以及如何运行容器的规范。Pod的内容总是在一个共享的上下文中被定位、调度和运行。Pod构建了一个特定于应用程序的“逻辑主机”：它包含一个或多个相对紧密耦合的应用程序容器。在非云上下文中，在同一物理或虚拟机上运行的应用程序类似于在同一逻辑主机上运行的云应用程序。

除了应用程序容器外，Pod还可以包含在Pod启动期间运行的[init containers](https://kubernetes.io/docs/concepts/workloads/pods/init-containers/)。如果集群提供了[ephemeral containers临时容器](https://kubernetes.io/docs/concepts/workloads/pods/ephemeral-containers/)，那么也可以注入临时容器进行调试。

### What is a Pod？
Pod的共享上下文是一组Linux namespaces、cgroups和潜在的其他facets of isolation —— 就是隔离Docker容器的东西。在Pod的上下文中，各个应用程序可能应用了进一步的子隔离。

从Docker的概念角度来看，Pod类似于一组具有共享命名空间和共享文件系统卷的Docker容器。

### Using Pods
通常您不需要直接创建pod，即使是单例Pods。相反，可以使用工作负载资源（如Deployment或Job）来创建它们。如果您的Pods需要跟踪状态，请考虑StatefulSet资源。

Kubernetes集群中的Pods主要有两种使用方式：
- 只运行一个容器的Pod。“每个Pod一个容器”模型是最常见的Kubernetes用例；在本例中，您可以将Pod看作单个容器的包装器；Kubernetes管理Pod而不是直接管理容器。
- 运行需要一起工作的多个容器的pod。Pod可以封装由多个紧密耦合并需要共享资源的同一位置的容器组成的应用程序。这些位于同一位置的容器形成了一个统一的服务单元，例如，一个容器将存储在共享卷中的数据提供给公众，而另一个sidecar容器则刷新或更新这些文件。Pod将这些容器、存储资源和一个短暂的网络标识作为一个单元包装在一起。

每个Pod都意味着运行给定应用程序的单个实例。如果您想横向分布应用程序（通过运行更多实例来提供更多的总体资源），您应该使用多个pod，每个pod对应一个实例。在Kubernetes中，这通常被称为replication复制。Replicated pods通常由工作负载资源及其控制器以组为单位来创建和管理。

有关Kubernetes如何使用工作负载资源及其控制器来实现应用程序分布和自动修复的更多信息，请参阅[Pods和controllers](https://kubernetes.io/docs/concepts/workloads/pods/#pods-and-controllers)。

pod本机为其组成容器提供两种共享资源：网络和存储。

### Working with Pods
您很少会直接在Kubernetes中创建单独的Pods，甚至是singleton Pods。这是因为Pods被设计成相对短暂的一次性实体。当一个Pod被创建（直接由您创建，或者由控制器间接创建），新的Pod将被安排在集群中的节点上运行。Pod将保留在该节点上，直到Pod完成执行、Pod对象被删除、Pod因缺少资源而被逐出或节点失败。

:notes:不要把Restarting a container与restarting a Pod混淆。Pod不是一个进程，而是一个运行容器的环境。Pod将一直存在，直到被删除。为Pod对象创建清单时，请确保指定的名称是有效的DNS子域名.

#### Pods and controllers
您可以使用workload resources为您创建和管理多个pod。资源的控制器处理复制和部署，以及在Pod失败时自动修复。例如，如果某个节点出现故障，控制器会注意到该节点上的Pod已停止工作，并创建一个replacement Pod替换版本。调度程序将替代pod放置到正常节点上。

下面是一些管理一个或多个pod的工作负载资源示例：
* [部署](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/)
* [状态集](https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/)
* [守护者](https://kubernetes.io/docs/concepts/workloads/controllers/daemonset)


#### Pod templates
工作负载资源的控制器从pod模板创建pod并管理这些pod。

PodTemplates是创建Pods的规范，包含在工作负载资源中，例如Deployments、Jobs和DaemonSets。

工作负载资源的每个控制器都使用workload对象中的PodTemplate来生成实际的Pods。PodTemplate是一种期望的状态，是用来运行应用程序所需的工作负载资源所需状态的一部分。

下面的示例是一个包含启动一个容器的template的简单作业的清单。该容器中的容器打印一条消息，然后暂停。
```yml
apiVersion: batch/v1
kind: Job
metadata:
  name: hello
spec:
  template:
    # This is the pod template
    spec:
      containers:
      - name: hello
        image: busybox
        command: ['sh', '-c', 'echo "Hello, Kubernetes!" && sleep 3600']
      restartPolicy: OnFailure
    # The pod template ends here
```

修改pod模板或切换到新的pod模板对已经存在的pod没有影响。Pods不直接接收模板更新。相反，将创建一个新的Pod来匹配修改后的Pod模板。

例如，部署控制器确保正在运行的pod与每个Deployment对象的当前pod模板匹配。如果更新了模板，Deployment必须删除现有的pod，并基于更新的模板创建新的pod。每个工作负载资源实现自己的规则来处理对Pod模板的更改。

在节点上，kubelet不直接观察或管理pod模板和更新的任何细节；这些细节被抽象化了。这种关注点的抽象和分离简化了系统语义，并使得在不改变现有代码的情况下扩展集群的行为成为可能。

### Resource sharing and communication
Pods允许在他的组成容器之间共享数据以及通信。
#### Storage in Pods
Pod可以指定一组共享存储卷。允许Pod内的容器访问共享卷，和共享数据。卷还允许Pod中的持久数据在需要重新启动的情况下继续存在。有关Kubernetes如何实现共享存储并使其对pod可用的更多信息，请参见[存储](https://kubernetes.io/docs/concepts/storage/)。
#### Pod networking
每个Pod为每个地址族分配一个唯一的IP地址。Pod中的每个容器共享网络namespace，包括IP地址和网络端口。在一个Pod中（并且只有在那时），属于Pod的容器可以使用localhost彼此通信。当Pod中的容器与Pod外部的实体通信时，它们必须协调如何使用共享的网络资源（例如端口）。在一个Pod中，容器共享一个IP地址和端口空间，并且可以通过localhost找到彼此。Pod中的容器还可以使用标准进程间通信（如SystemV信号量或POSIX共享内存）彼此通信。不同Pod中的容器具有不同的IP地址，如果没有[特殊配置](https://kubernetes.io/docs/concepts/policy/pod-security-policy/)，则无法通过IPC进行通信。容器想要与运行在不同Pod中的容器交互可以使用IP网络进行通信。

Pod中的容器将系统主机名视为与Pod的配置名称相同。在[网络](https://kubernetes.io/docs/concepts/cluster-administration/networking/)部分有更多关于这方面的内容。

### Privileged mode for containers
Pod中的任何容器都可以使用privileged模式，通过在容器spec的security context上设置privileged标志启用。这对于希望使用操作系统管理功能（如操作网络堆栈或访问硬件设备）的容器非常有用。Privileged容器中的进程获得的特权与容器外的进程几乎相同。

:notebook: 注意：容器运行时必须支持privileged容器的概念，此设置才能相关

### static pods 
静态pod由kubelet守护进程在特定节点上直接管理，且不需要API server监控它们。虽然大多数Pod是由control plane（例如，Deployment）管理的，但是对于静态Pods，kubelet直接监控每个静态Pod（如果失败则重新启动）。

静态Pods总是绑定到特定节点上的一个Kubelet。静态Pods的主要用途是运行一个自托管self-host的control plane：换句话说，使用kubelet来监视各个[control plane components](https://kubernetes.io/docs/concepts/overview/components/#control-plane-components)。

kubelet会自动尝试在kubernetes api server上为每个静态Pod创建一个镜像Pod。这意味着节点上运行的pods在API server可见，但是不能从API server这里进行控制。

## Pod Lifecycle
本页描述了Pod的生命周期。Pod遵循一个定义的生命周期，从Pending阶段开始，在至少一个主容器正常启动时，进入Running状态，然后根据Pod中的任何容器是否以失败终止分别进入Succeed或Failed阶段。

当一个Pod正在运行时，kubelet能够重新启动容器来处理某些类型的故障。在Pod中，kubernetes 跟踪和处理不同容器的状态。

在kubernetes API中，pod既有规范又有实际状态。Pod对象的状态由一组Pod conditions组成。如果对您的应用程序有用，您还可以将定制的readiness information准备就绪信息注入Pod的condition data中。

Pod在其生命周期中只调度一次。一旦Pod被调度（分配）到一个节点，这个Pod就会在该节点上运行，直到它停止或终止。

### Pod lifetime
与单个应用程序容器一样，pod被认为是相对短暂的（而不是持久的）实体。pod被创建，分配一个惟一的ID（UID），并被调度到节点上，直到终止（根据重启策略）或删除。

如果一个节点死了，那么调度到该节点的pod将在超时时间后被删除。

Pod本身不会自我恢复。如果一个Pod被调度到一个节点后节点失败了，或者调度操作本身失败，那么这个Pod就会被删除；同样，由于缺乏资源或节点维护，Pod将无法生存。Kubernetes使用一个更高级别的抽象，称为控制器，管理相对可丢弃的Pod实例。

一个成型的Pod（已经给定UID）永远不会“重新调度”到不同的节点；相反，可以用一个新的、几乎完全相同的Pod替换该Pod，如果需要，甚至可以使用相同的名称，但是使用不同的UID。

当某个对象与Pod具有相同的生命周期时，例如卷，这意味着只要特定的Pod（具有确定的UID）存在，该对象就存在。如果这个Pod由于任何原因被删除，即使创建了相同的替换，相关的东西（在本例中是卷）也会被销毁并重新创建。

### Pod phase
Pod的status字段是一个PodStatus对象，它有一个phase字段。

Pod的phase是对Pod在其生命周期中所处的位置的简单、高度的总结。Phase不是容器或Pod状态观察值的综合汇总comprehensive rollup，也不是一个复杂的state machine。

Pod phase的数量和含义受到严格保护。除了这里记录的内容外，其他关于Pods的phase的假设都是不能有的。

以下是phase的合法值：

Value | Description 
---------|----------
 Pending | Pod已经被kubernetes接收，但是其中的一个或多个容器还没有设置好准备运行。这包括Pod等待调度，以及通过网络下载容器镜像所花费的时间 
 Running | Pod已经绑定到一个节点上，所有容器已经创建。至少有一个容器仍在运行，或正在启动或重新启动中。
 Succeeded | Pod中的所有容器已成功正常终止，且不会重新启动
 Failed | Pod中的所有容器已经终止，至少有一个容器异常终止。也就是容器要么退出时返回非0状态，要么是被操作系统终止
 Unknown | 由于某种原因不能获取Pod的状态。此阶段通常是由于与Pod应该运行的节点通信时出错而发生的。

 如果一个节点死亡或者与集群中其余部分断开连接，kubernetes将采用一个策略，即把丢失节点上所有的Pod的phase标记为Failed.

### Container states 容器状态
和Pod一样，kubernetes跟踪Pod中每个容器的状态。你可以使用容器生命周期钩子来触发事件，在容器生命周期中的特定点运行。

当scheduler分配一个pod给节点，kubelet就开始使用容器运行时为pod创建容器。这里有三种容器状态：**Waiting、Running、Terminated**

要检查Pod中容器的状态，可以使用命令 *kubectl describe pod \<name-of-pod\>*. 

1. Waiting 等待状态
    如果容器不是Running和Terminated状态，那么它就在Waiting状态。Waiting状态的容器仍然在运行一些完成启动所需的操作：比如，从image registry下载容器镜像，或应用Secret data。当你使用kubectl查到Pod中的一个容器处于Waiting状态，你可以看到一个Reason字段，描述了容器为什么处于这个状态的原因
2. Running 运行中
   Running状态指容器正常运行且无问题。如果配置有postStart钩子，那么它已经执行并完成了。
3. Terminated
   Terminated状态就是容器开始执行，然后运行到完成或由于某些原因失败。如果容器配置了preStop钩子，那么钩子在容器进入Terminated状态之前运行。

### Container restart policy 容器重启策略
Pod的spec字段下有一个restartPolicy字段，其值为Always，OnFailure，Never。默认是Always

RestartPolicy应用于Pod中的所有容器。RestartPolicy仅仅指kubelet在同一节点上重新启动容器。在Pod中的容器退出后，kubelet采用指数后退延迟（10s, 20s, 40s, ...）的方式重新启动容器，该延迟的上限为5分钟。。一旦一个容器没有任何问题地执行了10分钟，kubelet将重置容器的重启倒计时器。

### Pod conditions
Pod有一个PodStatus对象, 该对象有一个[PodConditions](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#podcondition-v1-core)对象数组，代表了Pod已有或还没完成的状态。PodStatus的值如下：
- PodScheduled: Pod已经调度到一个节点上
- ContainerReady: Pod的所有容器已经就绪
- Initialized: 所有init container已经成功启动
- Ready：Pod已经可以为请求提供服务，可以被添加到所有匹配服务的负载均衡池中

PodConditions对象的属性如下：

Field name | Description
---------|----------
 type | Name of this Pod condition 
 status | 指示该condition是否可用：True、False、Unknown
 lastProbeTime | 上次探测Pod condition的时间戳
 lastTransitionTime | Pod上次从一种状态转换到另一种状态的时间戳。
 reason | 机器可读的大写驼峰文本，指示状态最后一次迁移的原因。
 message | 可读消息，指示上次状态迁移的详情

#### Pod readiness Pod就绪
*FEATURE STATE: Kubernetes v1.14 [stable]*
你的应用程序可以向PodStatus注入额外的反馈或信号：Pod readiness。要使用这个特性，需要在Pod的spec中设置readinessGates, 指定kubelet评估Pod readiness的附加条件。

Readiness gates由Pod的status.condition字段表示的当前状态决定。如果Pod内找不到status.conditions字段，那么condition的status默认为False。

下面是一个例子：
```yml
kind: Pod
...
spec:
  readinessGates:
    - conditionType: "www.example.com/feature-1"
status:
  conditions:
    - type: Ready                              # a built in PodCondition
      status: "False"
      lastProbeTime: null
      lastTransitionTime: 2018-01-01T00:00:00Z
    - type: "www.example.com/feature-1"        # an extra PodCondition
      status: "False"
      lastProbeTime: null
      lastTransitionTime: 2018-01-01T00:00:00Z
  containerStatuses:
    - containerID: docker://abcd...
      ready: true
...
```

#### Status for Pod readiness
kubectl patch命令不支持修补对象status。要设置Pod的status.conditions属性，应用程序和operators(用来管理自定义资源的特殊的controller)应该使用PATCH action。你可以使用[kubernetes client library](https://kubernetes.io/docs/reference/using-api/client-libraries/)，通过代码给Pod readiness设置自定义的Pod conditions。

对于一个使用自定义conditions的Pod来说，只有当下面两个条件都满足时，Pod才是ready状态：
- Pod中的所有容器都ready
- 所有在readinessGates指定的conditions都是true

当Pod的容器都处于Ready，但是至少有一个自定义的condition丢失或False，那么kubelet会把Pod的condition设置为ContainersReady.

### Container probes 容器探针
探针是一个kubelet定期对容器执行的诊断。为了执行一次诊断，kubelet调用容器实现的处理程序。有三类处理程序：
- ExecAction：在容器内部执行特殊的命令。如果命令以状态码0退出，则诊断为正常
- TCPSocketAction：对指定端口上的Pod的IP地址执行TCP检查。如果端口打开，则认为诊断成功
- HTTPGetAction：对Pod的IP地址、端口和路径发送HTTP GET请求。如果HTTP响应状态码大于或等于200，且小于400，则认为诊断成功

每一次探测都有三种结果：
- Success：容器诊断结果为通过
- Failure：容器诊断结果为失败
- Unknown：诊断过程本身失败，因此不采取任何措施

kubelet可以在运行中的容器上选择性地执行并响应三种类型的探针：
- livenessProbe: 指示容器是否running。如果liveness probe失败，kubelet就杀死容器，且容器遵循restart policy。如果容器没有提供liveness probe, 那么默认的状态就是Success.
- readinessProbe: 指示容器是否已经可以响应请求。如果readiness probe失败，那么endpoints controller就把Pod的IP地址从所有匹配这个Pod的services里移除。初始延迟前的默认readiness为Failure。如果容器未提供readiness probe, 默认状态是Success.
- startupProbe: 指示容器内部的应用程序是否已经启动。如果提供了startupProbe，那么其他的probes就被禁用了，直到startupProbe成功为止。如果startupProbe失败，kubelet会杀死容器，容器将遵循其restart policy。如果容器不提供startupProbe，则默认状态为Success。
  
For more information about how to set up a liveness, readiness, or startup probe, see [Configure Liveness, Readiness and Startup Probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/)

#### 什么时候使用liveness probe(活动探测器)?
*FEATURE STATE: Kubernetes v1.0 [stable]*
如果容器中的进程在遇到问题或变不正常时，进程会自行奔溃，那么你就不需要liveness probe。kubelet会根据Pod的restart policy自动执行正确的操作。

如果你希望在探测失败时终止并重新启动容器，那么你就需要指定一个liveness probe，并且把restartPolicy设置为Always或者OnFailure。

#### 什么时候使用readiness probe(就绪探测器)?
*FEATURE STATE: Kubernetes v1.0 [stable]*
如果只希望在探测成功时才开始向Pod发送流量，请指定readiness probe。在这种情况下，readiness probe可能与liveness probe相同，但规范中的readiness probe的存在意味着Pod将在不接收任何通信量的情况下启动，并且只有在探测器开始成功之后才开始接收通信量。如果容器需要在启动期间加载大数据、配置文件或迁移，请指定就绪探测器。

如果希望容器能够自行关闭以进行维护，则可以指定一个readiness probe，并使用这个探测器检查特定绪状态的端点（该端点与活动探测不同）。

:notes: 注意：如果您只想在Pod被删除时排出请求，则不一定需要准备就绪探测器；删除时，无论准备就绪探测器是否存在，Pod都会自动进入unready状态。在等待Pod中的容器停止时，Pod仍处于unready状态

#### 什么时候使用startup probe?
*FEATURE STATE: Kubernetes v1.16 [alpha]*
启动探测器对于那些容器需要很长时间才能投入使用的pod很有用。您可以配置一个单独的配置，以便在容器启动时探测容器，而不是设置一个较长的活动间隔liveness internal，这样就可以获得比liveness interval更长的时间。

如果容器的启动时间通常超过 $initialDelaySeconds + failureThreshold × periodSeconds$，则应指定一个启动探测器，该探测器检查与liveness探测器检查相同的端点。periodSeconds的默认值为30秒。然后应将其FailureThreshold设置为足够高，以允许容器启动，而不是更改liveness探测器的默认值。这有助于防止死锁。

### Termination of Pods Pod的终止
因为Pod就是集群节点上运行的进程，所以在这些进程不需要的时候优雅地终止是非常重要的（而不是使用KILL信号突然停止，这样就没有机会进行一些清理操作）

设计目标是让您能够请求删除并知道进程何时终止，但也能够确保删除最终完成。当您请求删除一个Pod时，集群会在允许强制终止Pod之前记录并跟踪预期的宽限期。有了能够跟踪强制关闭的功能，kubelet会尝试优雅地关闭。

通常，容器运行时向每个容器中的主进程发送一个TERM信号。一旦宽限期到期，KILL信号将被发送到任何剩余的进程，然后Pod将从API server中删除。如果kubelet或容器运行时的管理服务在等待进程终止时重新启动，集群将从头重试，包括使用完整的原始宽限期。

下面是一个例子：
1. 使用kubectl工具手动删除特定的Pod，默认宽限期为30秒。
2. API server中Pod的时间会被更新，超过这个时间（算上宽限期 ）Pod就会被认为是“已死亡”。如果使用kubectl describe检查要删除的Pod，则Pod显示为“Terminating”。在Pod运行的节点上：一旦kubelet看到一个Pod被标记为terminating（已经设置了一个优雅的关闭持续时间），kubelet就开始关闭本地Pod的进程。
    1. 如果Pod中的某个容器定义了preStop钩子，那么kubelet在容器内部执行这个钩子。如果preStop钩子在宽限期到期后还在运行，kubelet就会发送一个小的一次性宽限期延长2秒的请求。
        > :notebook: 注意如果preStop钩子需要比默认宽限期允许的时间更长的时间来完成，则必须修改terminationGracePeriodSeconds以适应此情况。
    2. kubelet触发容器运行时向每个容器内的1号进程发送一个TERM信号。
        > :notebook: 注意Pod中的容器在不同的时间以任意顺序接收到TERM信号。如果关闭顺序很重要，可以考虑使用preStop钩子进行同步。
3. 在kubelet开始正常关闭的同时，control plane从端点（如果启用的话，还有EndpointSlice）对象中删除关闭中的Pod，这些端点对象就是配置了selector的Service。ReplicaSets和其他工作负载资源不再将关闭中Pod视为有效的服务中副本。在终止宽限期termination grace period开始时，负载均衡器（如service proxy）就已经把Pod从端点列表中删除，因此缓慢关闭的Pod也就无法继续接收流量。
4. 当宽限期到期时，kubelet会触发强制关闭。容器运行时将SIGKILL发送给仍然在Pod的任何容器中运行的进程。如果容器运行时使用了一个隐藏的pause容器，kubelet还会清理该容器。
5. kubelet通过将宽限期设置为0（立即删除），触发从API server强制删除Pod对象。
6. API server删除Pod的API对象，然后该对象在任何客户机上就都不再可见了。

#### Forced Pod termination 强制终止Pod
:warning: 强制删除可能会对某些工作负载及其pod造成破坏。

默认情况下，所有优雅删除操作都在30秒内完成。kubectl delete命令支持--grace-period=\<seconds\>选项，该选项允许您覆盖默认值并指定自己的值。

强制将宽限期设置为0，即立即从API服务器中删除Pod。如果pod仍在某个节点上运行，则强制删除会触发，kubelet立即开始清理。

:candy: 要执行强制删除，必须指定附加标志--force和--grace-period=0

当执行强制删除时，API服务器不会等待kubelet确认Pod已在其运行的节点上终止。它会立即删除API中的Pod，以便可以用相同的名称创建新的Pod。在节点上，被设置为立即终止的pod在被强制杀死之前仍然会有一个小的宽限期。

如果需要强制删除属于StatefulSet的pod，请参阅[从StatefulSet删除Pods的任务文档](https://kubernetes.io/docs/tasks/run-application/force-delete-stateful-set-pod/)

#### Garbage collection of failed Pods 对失败的Pod进行垃圾回收
对于失败的pod，API对象将保留在集群的API中，直到人工或控制器进程显式删除它们。

当Pod数量超过配置的阈值（由kube-controller-manager的terminated-pod-gc-threshold决定）时，Control plane将清理已终止的Pod（将阶段标记为Succeed或Failed）。避免Pod随时间创建和终止时出现资源泄漏。

## Init containers 初始化容器
本页提供init容器的概述：在Pod中应用程序容器之前运行的特殊的容器。初始化容器可以包含应用程序镜像中不存在的程序或安装脚本。

您可以在Pod的spec的containers数组（描述应用程序容器）旁边指定init容器

### Understanding init containers 理解init containers
一个Pod可以有多个容器在其中运行应用程序，但是它也可以有一个或多个init容器，这些容器在应用程序容器启动之前运行。

Init容器与常规容器完全相同，除了：
- 初始化容器总是运行至完成并退出。
- 在下一个初始化容器启动之前，每个初始化容器都必须成功完成。

如果一个Pod的init容器失败，Kubernetes会反复重新启动Pod，直到init容器成功为止。但是，如果restartPolicy是Never，那么就不会重新启动。

通过在Pod spec中添加initContainers字段来为Pod指定init container，该字段的值为[Container](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#container-v1-core)对象数组。init容器的状态在.status.initContainersStatuses字段中返回，该字段值是一个container statuses数组（类似于.status.containers状态）。

#### Differences from regular containers
Init容器支持应用程序容器的所有字段和功能，包括资源限制、卷和安全设置。但是，init容器的资源请求和限制的处理方式不同，如[参考资料](https://kubernetes.io/docs/concepts/workloads/pods/init-containers/#resources)中所述。

另外，init容器不支持lifecycle、livenessProbe、readinessProbe或startupProbe，因为它们必须在Pod准备就绪之前运行到完成。

如果为一个Pod指定多个init容器，Kubelet将按顺序运行每个init容器。每个init容器必须成功，下一个才能运行。当所有init容器都运行到完成时，Kubelet初始化Pod的应用程序容器并像往常一样运行它们。

### Using init containers 使用init containers
由于init容器与app容器具有不同的镜像，因此它们对于启动相关代码具有一些优势：
- Init容器可以包含应用程序镜像中不存在的实用程序或自定义代码。例如，如果仅仅是在启动时使用一些如sed、awk、python或者dig这样的工具，没有必要使用FROM从其他镜像构建新镜像。
- 应用程序镜像生成器和部署者角色可以独立工作，而无需联合构建单个应用程序镜像。
- Init容器可以用与同一个Pod中的app容器不同的文件系统视图运行。因此，他们可以获得应用程序容器无法访问的机密。
- 由于init容器在任何应用程序容器启动之前运行到完成，init容器提供了一种机制来阻止或延迟应用程序容器的启动，直到满足一组先决条件。一旦满足了前提条件，Pod中的所有应用程序容器都可以并行启动。
- Init容器可以安全地运行实用程序或自定义代码，这些操作在app容器镜像中会降低安全性。通过将不必要的工具分开，可以限制应用程序容器镜像可能遭受攻击的面。

#### 例子
下面是一些init containers的例子：
- 使用shell一行命令等待服务创建，如：
  ```shell
  for i in {1..100}; do sleep 1; if dig myservice; then exit 0; fi; done; exit 1
  ```
- 使用如下命令从downward API向远程server注册Pod：
  ```shell
  curl -X POST http://$MANAGEMENT_SERVICE_HOST:$MANAGEMENT_SERVICE_PORT/register -d 'instance=$(<POD_NAME>)&ip=$(<POD_IP>)'
  ```
- 请等待一段时间，然后使用以下命令启动应用程序容器
  ```shell
  sleep 60
  ```
- Clone一个Git仓库到卷
- 将值放入配置文件并运行模板工具，以动态生成主应用程序容器的配置文件。例如，将POD_IP值放入配置中，并使用Jinja生成主应用程序配置文件。

##### Init containers in use
这个例子定义了一个有两个init容器的简单Pod。第一个等待myservice，第二个等待mydb。一旦两个init容器都完成，Pod将依照其spec部分运行app容器。
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: myapp-pod
  labels:
    app: myapp
spec:
  containers:
  - name: myapp-container
    image: busybox:1.28
    command: ['sh', '-c', 'echo The app is running! && sleep 3600']
  initContainers:
  - name: init-myservice
    image: busybox:1.28
    command: ['sh', '-c', "until nslookup myservice.$(cat /var/run/secrets/kubernetes.io/serviceaccount/namespace).svc.cluster.local; do echo waiting for myservice; sleep 2; done"]
  - name: init-mydb
    image: busybox:1.28
    command: ['sh', '-c', "until nslookup mydb.$(cat /var/run/secrets/kubernetes.io/serviceaccount/namespace).svc.cluster.local; do echo waiting for mydb; sleep 2; done"]
```

使用如下命令运行Pod：
```shell
kubectl apply -f myapp.yaml
```

使用如下命令获取状态：
```shell
kubectl get -f myapp.yaml

NAME       READY    STATUS     RESTARTS    AGE
myapp-pod  0/1      Init:0/2   0           6m
```

或者使用describe获取更多信息：
```shell
kubectl describe -f myapp.yaml

Name:          myapp-pod
Namespace:     default
[...]
Labels:        app=myapp
Status:        Pending
[...]
Init Containers:
  init-myservice:
[...]
    State:         Running
[...]
  init-mydb:
[...]
    State:         Waiting
      Reason:      PodInitializing
    Ready:         False
[...]
Containers:
  myapp-container:
[...]
    State:         Waiting
      Reason:      PodInitializing
    Ready:         False
[...]
Events:
  FirstSeen    LastSeen    Count    From                      SubObjectPath                           Type          Reason        Message
  ---------    --------    -----    ----                      -------------                           --------      ------        -------
  16s          16s         1        {default-scheduler }                                              Normal        Scheduled     Successfully assigned myapp-pod to 172.17.4.201
  16s          16s         1        {kubelet 172.17.4.201}    spec.initContainers{init-myservice}     Normal        Pulling       pulling image "busybox"
  13s          13s         1        {kubelet 172.17.4.201}    spec.initContainers{init-myservice}     Normal        Pulled        Successfully pulled image "busybox"
  13s          13s         1        {kubelet 172.17.4.201}    spec.initContainers{init-myservice}     Normal        Created       Created container with docker id 5ced34a04634; Security:[seccomp=unconfined]
  13s          13s         1        {kubelet 172.17.4.201}    spec.initContainers{init-myservice}     Normal        Started       Started container with docker id 5ced34a04634
```

要查看此Pod中init容器的日志，请运行：
```shell
kubectl logs myapp-pod -c init-myservice # Inspect the first init container
kubectl logs myapp-pod -c init-mydb      # Inspect the second init container
```

此时，那些init容器将等待发现名为mydb和myservice的服务。使用以下配置来创建服务：
```yaml
---
apiVersion: v1
kind: Service
metadata:
  name: myservice
spec:
  ports:
  - protocol: TCP
    port: 80
    targetPort: 9376
---
apiVersion: v1
kind: Service
metadata:
  name: mydb
spec:
  ports:
  - protocol: TCP
    port: 80
    targetPort: 9377
```

使用如下命令创建服务：
```shell
kubectl apply -f services.yaml

service/myservice created
service/mydb created
```

然后您将看到这些init容器完成，myapp-pod Pod进入运行状态：
```shell
kubectl get -f myapp.yaml

NAME        READY     STATUS    RESTARTS   AGE
myapp-pod   1/1       Running   0          9m
```
这个简单的例子应该会给您提供一些灵感，让您创建自己的init容器。“[下一步](https://kubernetes.io/docs/concepts/workloads/pods/init-containers/#whats-next)”包含指向更详细示例的链接


### Detailed behavior 详细的行为
在Pod启动期间，kubelet会延迟init容器的运行，直到网络和存储准备就绪。然后kubelet按照Pod规范中出现的顺序运行Pod的init容器。

在下一个容器启动之前，每个init容器都必须成功退出。如果容器由于运行时而无法启动或因失败退出，将根据Pod restartPolicy重试。但是，如果Pod restartPolicy设置为Always，那么init容器将使用restartPolicy OnFailure。

在所有初始化容器成功之前，Pod无法就绪Ready。init容器上的端口不在服务下聚合(The ports on an init container are not aggregated under a Service.)。正在初始化的Pod处于Pending状态，但应改有个Initialized condition为true。

如果Pod重新启动或已经重新启动，则必须再次执行所有init容器。

对init container spec的更改仅限于container image字段。更改init container image字段相当于重新启动Pod。

因为可以重新启动、重试或重新执行init容器，所以init容器代码应该是幂等的。特别是，写到EmptyDirs上的文件的代码应该为输出文件已经存在这种情况做好准备。

Init容器具有应用程序容器的所有字段。但是，Kubernetes禁止使用readinessProbe，因为init容器不能定义一个readiness并且和completion区分。这是在验证期间强制执行的。

在Pod上使用activeDeadlineSeconds，以及在容器上使用livenessProbe，以防止init容器永远失败。active deadline包括init容器(The active deadline includes init containers.)

Pod中每个app和init容器的名称必须是唯一的；对于与另一个容器共享名称的任何容器，都会引发验证错误。

#### Resources
鉴于init容器的排序和执行，resource使用规则如下：
- 在所有init容器上定义的所能请求或限制的任何特定资源的最大值是有效的init request/limit值
- Pod对某个资源的有效的request/limit是以下两个中的较高的一个：
  - 所有app容器所请求或限制的资源的总和
  - 对于一个资源的有效的init request或limit
- 调度基于有效的requests/limits完成，意味着init container可以保留资源来初始化，而这一部分资源在Pod的生命周期并没有使用到
- Pod的effective QoS层的QoS(Quality of service)服务质量层，是init containers和app containers的QoS

Quota配额和limits限制是根据有效的Pod请求和限制应用的。

Pod级的控制组（cgroups）基于有效的Pod请求和限制，与调度器相同。

#### Pod restart reasons
由于以下原因，Pod可能会重新启动，导致重新执行init容器：
- 用户更新Pod规范，导致init容器镜像发生更改。对init容器镜像的任何更改都会重新启动Pod。应用程序容器镜像更改仅重新启动应用程序容器。
- Pod基础设施容器重新启动。这是不常见的，可能与拥有对应节点root访问权限的人造成的。
- 当restartPolicy设置为Always时，Pod中的所有容器都将终止，强制重新启动，并且由于垃圾回收，init容器完成记录会丢失。

## Pod Topology Spread Constraints Pod拓扑分布约束
您可以使用拓扑分布约束来控制pod在故障域（如regions、zones、节点和其他用户定义的拓扑域）之间在集群中的分布方式。这有助于实现高可用性和高效的资源利用。

### Prerequisites 先决条件
#### Node Labels 节点标签
拓扑分布约束依赖于节点标签来标识每个节点所在的拓扑域。例如，一个节点可能有标签：Node=node1，zone=us-east-1a，region=us-east-1

假设您有一个具有以下标签的4节点集群：
```shell
NAME    STATUS   ROLES    AGE     VERSION   LABELS
node1   Ready    <none>   4m26s   v1.16.0   node=node1,zone=zoneA
node2   Ready    <none>   3m58s   v1.16.0   node=node2,zone=zoneA
node3   Ready    <none>   3m17s   v1.16.0   node=node3,zone=zoneB
node4   Ready    <none>   2m43s   v1.16.0   node=node4,zone=zoneB
```

集群从逻辑上的视图如下：
```
+---------------+---------------+
|     zoneA     |     zoneB     |
+-------+-------+-------+-------+
| node1 | node2 | node3 | node4 |
+-------+-------+-------+-------+
```
您还可以重用在大多数集群上自动创建和填充的[已知标签](https://kubernetes.io/docs/reference/kubernetes-api/labels-annotations-taints/)，而不是手动创建标签

### Spread Constraints for Pods Pod的分布约束
#### API
接口中的字段pod.spec.topologySpreadConstraints定义如下：
```yml
apiVersion: v1
kind: Pod
metadata:
  name: mypod
spec:
  topologySpreadConstraints:
    - maxSkew: <integer>
      topologyKey: <string>
      whenUnsatisfiable: <string>
      labelSelector: <object>
```

你可以定义一个或多个topologySpreadConstraint来指示kube-scheduler如何将incoming Pod相对于集群中已有Pod进行安置。下面是字段的解释：
- **maxSkew**: maxSkew描述了Pod分布不均匀的程度。这是给定拓扑类型的任意两个拓扑域中匹配的pod数量之间允许的最大差异。它必须大于零。其语义根据whenUnsatisfiable的值而不同：
  - 当whenUnsatisfiable = DoNotSchedule, maxSkew等于目标拓扑域中匹配的Pod的数量和全局最小值之间允许的最大差
  - 当whenUnsatisfiable = ScheduleAnyway, scheduler为减少skew的拓扑提供更高的优先级。
- **topologyKey**: 节点标签的key. 如果两个节点使用这个key标记，并且对应标签的值相同，则调度程序会将这两个节点视为处于同一拓扑中。调度器尝试在每个拓扑域中放置均衡数量的pod。
- **whenUnsatisfiable**: 指示在不满足分布约束的情况下如何处理Pod：
  - *DoNotSchedule*(默认)告诉调度程序不要调度Pod
  - *ScheduleAnyway*告诉调度程序在为了减少skew而对节点进行优先级排序时，任然对pod进行调度
- **labelSelector**: 用来匹配pods。使用这个label选择器匹配的pod会被用来计算对应拓扑域中pod的数量。有关详细信息，请参阅[标签选择器](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#label-selectors)。

你可以通过运行kubectl explain Pod.spec.topologySpreadConstraints来获取更多关于此字段的信息。

#### Example: One TopologySpreadConstraint 单个拓扑分布限制
假如你有4个节点的集群，其中3个Pod有foo:bar的label，分别位于node1, node2, node3节点上（P代表Pod）:
```
+---------------+---------------+
|     zoneA     |     zoneB     |
+-------+-------+-------+-------+
| node1 | node2 | node3 | node4 |
+-------+-------+-------+-------+
|   P   |   P   |   P   |       |
+-------+-------+-------+-------+
```

如果我们想要一个incoming pod与现有pod均匀地分布在各个区域，spec可以如下所示：
```yaml
kind: Pod
apiVersion: v1
metadata:
  name: mypod
  labels:
    foo: bar
spec:
  topologySpreadConstraints:
  - maxSkew: 1
    topologyKey: zone
    whenUnsatisfiable: DoNotSchedule
    labelSelector:
      matchLabels:
        foo: bar
  containers:
  - name: pause
    image: k8s.gcr.io/pause:3.1
```
topologyKey: zone 指均匀分布只作用在拥有zone:\<any value\> label的节点上。whenUnsatisfiable: DoNotSchedule告诉调度程序如果incoming Pod不能满足限制条件就让它保持pending状态。

如果调度程序将incoming pod放到zoneA区域，pod的分布将变成[3, 1] (zoneA中3个，zoneB中1个pod), 因此实际的$skew = 3 - 1 = 2$, 而这个值不满足maxSkew: 1的限制。在这个例子中incoming Pod只能调度到zoneB:
```
+---------------+---------------+      +---------------+---------------+
|     zoneA     |     zoneB     |      |     zoneA     |     zoneB     |
+-------+-------+-------+-------+      +-------+-------+-------+-------+
| node1 | node2 | node3 | node4 |  OR  | node1 | node2 | node3 | node4 |
+-------+-------+-------+-------+      +-------+-------+-------+-------+
|   P   |   P   |   P   |   P   |      |   P   |   P   |  P P  |       |
+-------+-------+-------+-------+      +-------+-------+-------+-------+
```

您可以调整Pod规范以满足各种需求：
- 调整maxSkew，比如大于2，这样incoming pod就可以调度到zoneA区域
- 修改topologyKey为node，这样pod就会均匀地分布到节点上，而不是zone上。在上面的例子中，如果maxSkew任然是1，那么incoming pod则只能调度到node4
- 把whenUnsatisfiable修改为ScheduleAnyway来保证总是能调度incoming pod(在假设其他scheduling APIs都满足的情况下)。但是会优先把它放在匹配的pod较少的拓扑域中（请注意，这种优先性与其他内部调度优先级（如资源使用率等）共同归一化了。）


#### Example: Multiple TopologySpreadConstraint 多个拓扑分布限制
这是基于前面的例子。假设您有一个4节点集群，其中3个pod被标记为foo:bar, 分别位于node1、node2、node3（P代表pod）：
```
+---------------+---------------+
|     zoneA     |     zoneB     |
+-------+-------+-------+-------+
| node1 | node2 | node3 | node4 |
+-------+-------+-------+-------+
|   P   |   P   |   P   |       |
+-------+-------+-------+-------+
```

您可以使用2个TopologySpreadConstraint来控制在区域和节点上pod的分布：
pods/topology-spread-constraints/two-constraints.yaml :
```yaml
kind: Pod
apiVersion: v1
metadata:
  name: mypod
  labels:
    foo: bar
spec:
  topologySpreadConstraints:
  - maxSkew: 1
    topologyKey: zone
    whenUnsatisfiable: DoNotSchedule
    labelSelector:
      matchLabels:
        foo: bar
  - maxSkew: 1
    topologyKey: node
    whenUnsatisfiable: DoNotSchedule
    labelSelector:
      matchLabels:
        foo: bar
  containers:
  - name: pause
    image: k8s.gcr.io/pause:3.1
```

在这种情况下，为了匹配第一个约束，incoming Pod只能放在“zoneB”上；而对于第二个约束，incoming Pod只能放在“node4”上。然后对两个约束条件的结果进行“and”运算，因此唯一可行的选择是将“node4”放在“node4”上。

多个约束可能导致冲突。假设您有一个跨2个区域的3节点群集：
```
+---------------+-------+
|     zoneA     | zoneB |
+-------+-------+-------+
| node1 | node2 | node3 |
+-------+-------+-------+
|  P P  |   P   |  P P  |
+-------+-------+-------+
```

如果在集群上应用two-constraints.yaml，你会发现到“mypod”处于Pending状态。这是因为：为了满足第一个约束，“mypod”只能放在“zoneB”上；而对于第二个约束，“mypod”只能放在“node2”上。那么“zoneB”和“node2”的联合结果就是没有结果。

为了克服这种情况，您可以增加maxSkew，或者修改其中一个约束为whenUnsatisfiable: scheduleAnyway。

#### Conventions 惯例
这里有一些隐含的需要注意的惯例：
- 只有与incoming Pod具有相同namespace的Pod才能被匹配候选。
- 没有topologySpreadConstraints[*].topologyKey的节点会被忽略。也就意味着：
  - 位于这些节点上的Pod不会影响maxSkew计算。在上面的例子中，假设node1没有标签zone，那么node1上的2个Pod将被忽略，因此incoming Pod将被调度到“zoneA”。
  - incoming Pod没有机会被调度到这类节点上。在上面的示例中，假设一个带有标签{zone-typo:zoneC}的“node5”加入集群，由于没有标签键“zone”，它会被忽略。
- 请注意，如果incoming Pod的topologySpreadConstraints[*].labelSelector与自己的标签不匹配，将会发生什么。在上面的例子中，如果我们删除了incoming Pod的标签，它仍然可以被放在“zoneB”上，因为约束仍然得到满足。然而，在放置之后，集群的不平衡程度保持不变——仍然是zoneA有2个pod，其中包含标签{foo:bar}, 以及zoneB有一个pod，上面有标签{foo:bar}. 因此，如果这不是您所期望的，我们建议工作负载的topologySpreadConstraints[*].labelSelector匹配它自己的标签。
- 如果incoming pod的spec.nodeSelector或者spec.affinity.nodeAffinity规范定义后，节点不匹配这两个规范，那么节点会被忽略。

假设您有一个从zoneA到zoneC的5节点集群：
```yaml
+---------------+---------------+-------+
|     zoneA     |     zoneB     | zoneC |
+-------+-------+-------+-------+-------+
| node1 | node2 | node3 | node4 | node5 |
+-------+-------+-------+-------+-------+
|   P   |   P   |   P   |       |       |
+-------+-------+-------+-------+-------+
```
你知道“zoneC”必须排除在外。在这种情况下，您可以按如下方式编写yaml，这样“mypod”将被放在“zoneB”而不是“zoneC”上。同样spec.nodeSelector也要满足。
pods/topology-spread-constraints/one-constraint-with-nodeaffinity.yaml 
```yaml
kind: Pod
apiVersion: v1
metadata:
  name: mypod
  labels:
    foo: bar
spec:
  topologySpreadConstraints:
  - maxSkew: 1
    topologyKey: zone
    whenUnsatisfiable: DoNotSchedule
    labelSelector:
      matchLabels:
        foo: bar
  affinity:
    nodeAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        nodeSelectorTerms:
        - matchExpressions:
          - key: zone
            operator: NotIn
            values:
            - zoneC
  containers:
  - name: pause
    image: k8s.gcr.io/pause:3.1
```


#### Cluster-level default constraints 集群级别的默认约束
*FEATURE STATE: Kubernetes v1.19 [beta]*

可以为群集设置默认拓扑扩展约束。只有在以下情况下，才会将默认拓扑分布约束应用于Pod：
- Pod没有在其.spec.topologySpreadConstraints定义任何约束.
- Pod属于服务、replication controller, replica set或stateful set。
  
可以在[调度profile](https://kubernetes.io/docs/reference/scheduling/config/#profiles)配置中设置默认约束，作为PodTopologySpread插件参数的一部分值。除了labelSelector必须为空之外，这些约束是用上面相同的API。选择器是根据Pod所属的services, replication controllers, replica sets or stateful sets 来计算的。

配置示例如下：
```yaml
apiVersion: kubescheduler.config.k8s.io/v1beta1
kind: KubeSchedulerConfiguration

profiles:
  pluginConfig:
    - name: PodTopologySpread
      args:
        defaultConstraints:
          - maxSkew: 1
            topologyKey: topology.kubernetes.io/zone
            whenUnsatisfiable: ScheduleAnyway
```
:notebook: 默认调度约束生成的分数可能与[SelectorSpread插件](https://kubernetes.io/docs/reference/scheduling/config/#scheduling-plugins)生成的分数冲突。当对PodTopologySpread使用默认约束时，建议您在scheduling profile中禁用此插件。

#### Internal default constraints 内部默认约束
*FEATURE STATE: Kubernetes v1.19 [alpha]*
当您启用DefaultPodTopologySpread特性门时，旧的SelectorSpread插件将被禁用。kube-scheduler对PodTopologySpread插件配置使用以下默认拓扑约束：

```yaml
defaultConstraints:
  - maxSkew: 3
    topologyKey: "kubernetes.io/hostname"
    whenUnsatisfiable: ScheduleAnyway
  - maxSkew: 5
    topologyKey: "topology.kubernetes.io/zone"
    whenUnsatisfiable: ScheduleAnyway
```

此外，提供等效行为的遗留SelectorSpread插件也被禁用。

:notebook: 注：如果您的节点不希望都有 kubernetes.io/hostname以及topology.kubernetes.io/zone 标签集，定义您自己的约束，而不是使用Kubernetes默认值。PodTopologySpread插件不会对没有在spreading constraints中指定topology key的节点进行评分。

### Comparison with PodAffinity/PodAntiAffinity 和节点亲和/反亲和性的对比

在Kubernetes中，与“亲和力”相关的指令控制着如何安排Pod —— 更多的是打包的，或更多的是分散的。
- 对于podAffinity，可以尝试将任意数量的pod打包到合格的拓扑域中
- 对于PodAntiAffinity，只有一个Pod可以调度到一个拓扑域中。

为了更好地控制，您可以指定拓扑分布约束，以便在不同的拓扑域中分布pod，从而实现高可用性或节省成本。这也有助于滚动更新工作负载和平滑地扩展副本。详见[动机](https://github.com/kubernetes/enhancements/tree/master/keps/sig-scheduling/895-pod-topology-spread#motivation)


### Known limitations 已知限制
- 缩小部署规模可能会导致pod分布不平衡。
- 受污染节点上匹配的pod也会被考虑。见[第80921期](https://github.com/kubernetes/kubernetes/issues/80921)


## Pod Presets 预设
*FEATURE STATE: Kubernetes v1.6 [alpha]*
本页提供了PodPresets的概述，PodPresets是在创建时将某些信息注入pods的对象。这些信息可以包括机密、卷、卷装载和环境变量

### Understanding Pod presets
PodPreset是一种API资源，用于在创建时将额外的运行时需求注入Pod。您可以使用label selector来指定Pods并应用PodPreset。

使用PodPreset允许pod模板作者不必显式地为每个pod提供所有信息。作者不需要知道pod模板所需服务的所有细节。

### 在群集中启用PodPreset
为了在集群中使用Pod presets，您必须确保以下事项：
- 您已启用API类型settings.k8s.io/v1alpha1/podpreset。例如，可以通过在API server的--runtime config选项中包含settings.k8s.io/v1alpha1=true来实现。在minikube中添加此标志--extra config=apiserver.runtime-config=settings.k8s.io/v1alpha1=true，同时启动集群。
- s您已启用名为PodPreset的许可控制器admission controller。一种方法是在为API服务器的--enable-admission-plugins选项值中包含PodPreset。例如，如果使用Minikube，在启动时请添加以下标志：
  ```shell
    --extra-config=apiserver.enable-admission-plugins=NamespaceLifecycle,LimitRanger,ServiceAccount,DefaultStorageClass,DefaultTolerationSeconds,NodeRestriction,MutatingAdmissionWebhook,ValidatingAdmissionWebhook,ResourceQuota,PodPreset
  ```

### How it works
Kubernetes提供了一个admission controller（PodPreset），当启用时，它将Pod Presets应用于incoming Pod创建请求。当pod创建请求发生时，系统执行以下操作：
- 检索所有可用的PodPresets。
- 检查任何PodPreset的label selectors是否与正在创建的pod上的标签匹配。
- 尝试将PodPreset定义的各种资源合并到正在创建的Pod中。
- 出错时，在pod上抛出一个记录合并错误的事件，并在不注入任何PodPreset的资源的情况下创建pod。
- 对结果修改后的Pod规范进行注释，以表明它已被PodPreset修改。注释的形式是 podpreset.admission.kubernetes.io/podpreset-\<pod-preset name\>: "\<resource version\>"。

每个Pod可以由零个或多个PodPreset匹配；每个PodPreset可以应用于零个或多个Pods。当PodPreset应用于一个或多个Pods时，Kubernetes会修改Pod Spec。对于env、envFrom和volumeMounts的更改，Kubernetes将修改Pod中所有容器的container spec；对于卷的更改，Kubernetes修改Pod spec。

> :notebook: 注：
>    Pod Preset可在适当时修改Pod Spec的以下字段：
> - .spec.containers字段
> - .spec.initContainers字段

#### Disable Pod Preset for a specific pod
可能有这样的情况，你希望一个Pod不被任何Pod preset所修改。在这些情况下，您可以在Pod的.spec中添加一个注释：podpreset.admission.kubernetes.io/exclude: "true"

### What's next
See [Injecting data into a Pod using PodPreset](https://kubernetes.io/docs/tasks/inject-data-application/podpreset/)
For more information about the background, see the [design proposal for PodPreset.](https://git.k8s.io/community/contributors/design-proposals/service-catalog/pod-preset.md)



## Disruptions 中断
本指南适用于希望构建高可用性应用程序的应用程序所有者，因此需要了解Pod可能发生的中断类型。
它也适用于希望执行自动化集群操作（如升级和自动调整集群）的集群管理员。

### Voluntary and involuntary disruptions 自愿和非自愿中断

Pod不会消失，直到有人（一个人或一个控制器）摧毁他们，或有一个不可避免的硬件或系统软件错误。

我们将这些不可避免的情况称为应用程序的**非自愿中断**。例如：
- 支持节点的物理计算机的硬件故障
- 群集管理员错误地删除VM（实例）
- 云提供商或虚拟机监控程序故障使虚拟机消失
- kernel panic
- 由于群集网络分区，节点从群集中消失
- 由于节点资源不足而逐出pod。

除了资源不足的情况外，大多数用户都应该熟悉所有这些情况；它们不是Kubernetes特有的。

**我们称其他情况为自愿中断**。这些操作包括由应用程序所有者启动的操作和由群集管理员启动的操作。典型的应用程序所有者操作包括：
- 删除管理pod的deployment或其他controller
- 更新deployment的pod模板导致重新启动
- 直接删除pod（如意外）

群集管理员操作包括：
- 排出节点以进行修复或升级。
- 从集群中排出节点以缩小集群（了解集群自动调整）。
- 从一个节点上移除一个pod以允许在该节点上安装其他内容。

这些操作可以由群集管理员直接执行，也可以由群集管理器自动运行，或者由您的群集宿主提供程序执行。

询问您的集群管理员或咨询您的云提供商或分发文档，以确定是否为您的集群启用了任何自愿中断源。如果没有启用，您可以跳过创建Pod Disruption Budgets。

:warning: 并非所有自愿中断都受到Pod Disruption Budgets限制。例如，删除deployment或Pod会绕过Pod Disruption Budgets.

### Dealing with disruptions
以下是一些减轻非自愿中断的方法：
- 确保你的pod请求能获取到它需要的[资源](https://kubernetes.io/docs/tasks/configure-pod-container/assign-memory-resource)。
- 如果需要更高的可用性，请复制应用程序。（了解如何运行复制的[无状态](https://kubernetes.io/docs/tasks/run-application/run-stateless-application-deployment/)和[有状态](https://kubernetes.io/docs/tasks/run-application/run-replicated-stateful-application/)的应用程序。）
- 为了在运行复制的应用程序时获得更高的可用性，请跨机架（使用[反亲和](https://kubernetes.io/docs/user-guide/node-selection/#inter-pod-affinity-and-anti-affinity-beta-feature)）或跨区域（如果[使用多区域群集](https://kubernetes.io/docs/setup/multiple-zones)）分布应用程序

自愿中断的频率各不相同。在一个基本的Kubernetes集群中，根本没有自愿中断。但是，您的群集管理员或主机提供商可能会运行一些附加服务，这些服务会导致自愿中断。例如，推出节点软件更新可能会导致自愿中断。此外，集群（节点）自动调整的某些实现可能会导致自愿中断，然后进行碎片整理和压缩节点。或者说，如果您的托管服务提供商有任何级别的中断，应该有文档记录

### Pod disruption budgets 
*FEATURE STATE: Kubernetes v1.5 [beta]*
Kubernetes提供了一些特性，可以帮助您运行高可用性的应用程序，即使您引入了频繁的自愿中断。

作为应用程序所有者，您可以为每个应用程序创建一个PodDisruptionBudget（PDB, Pod中断预算）。PDB限制了复制应用程序同时自愿中断的数量。例如，基于仲裁quorum-based的应用程序希望确保运行的副本数量永远不会低于仲裁quorum所需的数量。web前端可能希望确保为负载提供服务的副本数量永远不会低于总数的某个百分比。

集群管理器和托管提供商应该使用一些工具，通过调用[Eviction API](https://kubernetes.io/docs/tasks/administer-cluster/safely-drain-node/#the-eviction-api)来维护PodDisruptionBudgets, 而不是直接删除pods或配deployments.

例如，kubectl drain子命令允许您将节点标记为停止服务。当您运行kubectl drain时，该工具会尝试收回您要停止服务的节点上的所有pod。kubectl代表您提交的eviction请求可能会被临时拒绝，因此该工具会定期重试所有失败的请求，直到目标节点上的所有pod终止，或者直到达到可配置的超时( until a configurable timeout is reached.)。

PDB指定应用程序可以容忍的复制副本数量，而不是预期的副本数量。例如，一个具有.spec.replicas：5的Deployment在任何给定时间都应该有5个pods。如果PDB允许一次有4个，那么Eviction API将允许一次自愿中断一个（而不是两个）pod。

组成应用程序的pod组是使用标签选择器指定的，与应用程序的控制器（部署、状态集等）使用的选择器相同。

“预期”的pod数量由工作负载资源(workload resource)的.spec.replicas计算得出，这些工作负载资源管理了这些pod。Control plane通过检查Pod的.metadata.ownerReferences来发现pod所属的工作负载资源。

PDB无法防止非自愿中断的发生，但它们确实会影响Pod中断的预算。

由于对应用程序的滚动升级而被删除或不可用的pod会被计入中断预算，但是在进行滚动升级时，工作负载资源（如deployment和StatefulSet）不受PDB的限制。相反，应用程序更新期间的故障处理方式在特定的工作负载资源的spec里进行配置。

当使用Eviction API逐出一个pod时，它将被优雅地终止，遵循其[PodSpec](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#podspec-v1-core)中的terminationGracePeriodSeconds设置。

### PodDisruptionBudget example

考虑一个有3个节点的集群，节点1到节点3。群集正在运行多个应用程序。其中有3个最初称为pod-a、pod-b和pod-c的副本，另一个没有PDB的无关pod，称为pod-x。最初，pod的布局如下：

```
node-1	         node-2	             node-3
pod-a available  pod-b available	 pod-c available
pod-x available
```

所有3个pod都是deployment的一部分，它们共同拥有一个PDB，这要求3个pod中至少有2个随时可用。

例如，假设集群管理员希望重新启动到新的内核版本，以修复内核中的错误。集群管理员首先尝试使用kubectl drain命令排出node-1。该工具试图逐出pod-a和pod-x，这会立即成功。两个pod同时进入终止Terminating状态。这将使群集处于以下状态：

```
node-1 draining	    node-2	        node-3
pod-a terminating	pod-b available	pod-c available
pod-x terminating
```

Deployment注意到其中一个pod正在终止，因此它创建了一个名为pod-d的替换项。由于node-1被封锁，它将调度到另一个节点上。同时也会创建pod-y作为pod-x的替代品。

（注意：对于StatefulSet，pod-a（也可能叫做pod-0之类的东西）需要完全终止，然后才能创建其替换项（这个替换项的名字同样是pod-0，但具有不同的UID）。否则，该示例也适用于StatefulSet。）

现在集群的状态如下：
```
node-1 draining    	node-2	        node-3
pod-a terminating	pod-b available	pod-c available
pod-x terminating	pod-d starting	pod-y
```

在某个时刻，pod度终止了，然后集群的状态就会像下面：
```
node-1 draining    	node-2	        node-3
                    pod-b available	pod-c available
                    pod-d starting	pod-y
```

此时，如果一个不耐烦的集群管理员试图排出node-2或node-3，drain命令将阻塞，因为只有2个可用的pod可用于部署，而其PDB至少需要2个。经过一段时间后，pod-d变为可用。

然后集群的状态如下：
```
node-1 draining    	node-2	        node-3
                    pod-b available	pod-c available
                    pod-d available	pod-y
```

现在，集群管理员尝试排出node-2。排水命令将尝试按某种顺序驱逐两个pod，比如先驱逐pod-b，然后驱逐pod-d。如果开始时驱逐pod-b则操作成功。但是，当它试图驱逐pod-d时，它将被拒绝，因为这将只留下一个pod可供Deployment使用。

Deployment为pod-b创建了一个称为pod-e的替换项。因为集群中没有足够的资源来调度pod-e，drain操作将再次阻塞。群集可能最终处于这种状态：
```
node-1 drained	node-2	            node-3	        no node
                pod-b terminating	pod-c available	pod-e pending
                pod-d available	    pod-y
```
此时，集群管理员需要将一个节点添加回集群以继续升级。

您可以看到Kubernetes如何改变中断发生的速率，根据：
- 一个应用程序需要多少副本
- 正常关闭实例需要多长时间
- 新实例启动需要多长时间
- 控制器类型
- 群集的资源容量

### 分离集群所有者和应用程序所有者角色

通常，将集群管理员和应用程序所有者视为互不影响的独立角色是很有用的。这种职责分离在以下情况下可能有意义：
- 当有许多应用程序团队共享一个Kubernetes集群时，角色自然会专门化
- 当第三方工具或服务用于自动化群集管理时

Pod中断预算通过提供角色之间的接口来支持这种角色分离。

如果您的组织没有这样的职责划分，您可能不需要使用Pod中断预算。

### How to perform Disruptive Actions on your Cluster 如何在集群上执行破坏性操作
如果您是群集管理员，并且需要对群集中的所有节点执行中断性操作，例如节点或系统软件升级，以下是一些选项：
- 接受升级期间的停机时间。
- 故障转移到另一个完整的副本群集。
  - 没有停机时间，但是对于复制的节点和人工协调切换来说，代价可能很高。
- 编写可中断的应用程序并使用PDB。
  - 没有停机时间。
  - 最少的资源重复。
  - 允许群集管理更加自动化。
  - 编写可容忍中断的应用程序是很棘手的，但是容忍自愿中断的工作与支持自动调整和容忍非自愿中断的工作基本上是重叠的。
下一步是什么

### What's next
Follow steps to protect your application by [configuring a Pod Disruption Budget](https://kubernetes.io/docs/tasks/run-application/configure-pdb/).

Learn more about [draining nodes](https://kubernetes.io/docs/tasks/administer-cluster/safely-drain-node/)

Learn about [updating a deployment](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#updating-a-deployment) including steps to maintain its availability during the rollout.


---
##  Ephemeral Containers 临时容器
*FEATURE STATE: Kubernetes v1.16 [alpha]*
这个页面提供了临时容器的概述：一种特殊类型的容器，它临时运行在现有的Pod中，以完成用户启动的操作，如故障排除。可以使用临时容器检查服务，而不是构建应用程序。

:warning: 警告：临时容器处于早期alpha状态，不适合生产集群。根据Kubernetes的弃用策略，这个alpha特性在将来可能会发生重大变化，或者被完全删除。

### Understanding ephemeral containers
pod是Kubernetes应用程序的基本构建块。由于Pods是一次性的和可替换的，所以一旦创建了一个容器，就不能将其添加到Pod中。相反，您通常使用Deployment来控制Pod的删除和替换。

然而，有时有必要检查一个现有的Pod的状态，例如排除一个难以重现的bug。在这些情况下，您可以在现有的Pod中运行一个临时容器来检查其状态并运行任意命令

#### What is an ephemeral container?
临时容器与其他容器的不同之处在于，它们缺乏对资源或执行的保证，而且它们永远不会自动重新启动，因此它们不适合于构建应用程序。使用与常规容器相同的ContainerSpec来描述临时容器，但是许多字段对于临时容器是不兼容和不允许的。
- 临时容器没有端口，因此不允许使用ports、livessprobe、readinessProbe等字段。
- Pod资源分配是不可变的，因此不允许设置resources。
- 有关允许字段的完整列表，请参阅[EphemeralContainer参考文档](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#ephemeralcontainer-v1-core)。

临时容器是使用API中的一个特殊的ephemeralcontainers处理程序创建的，而不是直接将它们添加到pod.spec，因此不可能使用kubectl edit添加临时容器。

像普通的容器一样，在你把一个短暂的容器添加到一个pod之后，你不能改变或移除它。

### Uses for ephemeral containers

当kubectl exec因容器崩溃或容器映像不包含调试实用程序而不能进行调试时，临时容器对于交互式故障排除非常有用。

特别是，[distroless images](https://github.com/GoogleContainerTools/distroless)（最小发行镜像？）使您能够部署最小的容器映像，从而减少攻击面和漏洞暴露。由于distroless images不包含shell或任何调试实用程序，因此仅使用kubectl exec很难排除distroless images的故障。

使用临时容器时，[启用进程命名空间共享](https://kubernetes.io/docs/tasks/configure-pod-container/share-process-namespace/)很有帮助，这样您就可以在其他容器中查看进程。

有关[使用临时容器进行故障排除](https://kubernetes.io/docs/tasks/debug-application-cluster/debug-running-pod/#debugging-with-ephemeral-debug-container)的示例，请参阅使用临时调试容器进行调试

### Ephemeral containers API
:warning: 注意：本节中的示例要求启用EphemeralContainers特性门，并启用Kubernetes客户机和服务器版本v1.16或更高版本。

本节中的示例演示如何在API中使用临时容器。您通常会使用kubectl alpha debug或另一个kubectl插件来自动化这些步骤，而不是直接调用API。

临时容器是使用Pod的ephemeralcontainers子资源创建的，可以使用kubectl-raw。首先将要添加的临时容器描述为临时容器列表：
```json
{
    "apiVersion": "v1",
    "kind": "EphemeralContainers",
    "metadata": {
            "name": "example-pod"
    },
    "ephemeralContainers": [{
        "command": [
            "sh"
        ],
        "image": "busybox",
        "imagePullPolicy": "IfNotPresent",
        "name": "debugger",
        "stdin": true,
        "tty": true,
        "terminationMessagePolicy": "File"
    }]
}
```

要更新正在运行的example-pod的临时容器, 使用如下命令：
```shell
kubectl replace --raw /api/v1/namespaces/default/pods/example-pod/ephemeralcontainers  -f ec.json
```

这将返回临时容器的新列表：
```json
{
   "kind":"EphemeralContainers",
   "apiVersion":"v1",
   "metadata":{
      "name":"example-pod",
      "namespace":"default",
      "selfLink":"/api/v1/namespaces/default/pods/example-pod/ephemeralcontainers",
      "uid":"a14a6d9b-62f2-4119-9d8e-e2ed6bc3a47c",
      "resourceVersion":"15886",
      "creationTimestamp":"2019-08-29T06:41:42Z"
   },
   "ephemeralContainers":[
      {
         "name":"debugger",
         "image":"busybox",
         "command":[
            "sh"
         ],
         "resources":{

         },
         "terminationMessagePolicy":"File",
         "imagePullPolicy":"IfNotPresent",
         "stdin":true,
         "tty":true
      }
   ]
}

```

您可以使用kubectl describe查看新创建的临时容器的状态：
```shell
kubectl describe pod example-pod
```

```
...
Ephemeral Containers:
  debugger:
    Container ID:  docker://cf81908f149e7e9213d3c3644eda55c72efaff67652a2685c1146f0ce151e80f
    Image:         busybox
    Image ID:      docker-pullable://busybox@sha256:9f1003c480699be56815db0f8146ad2e22efea85129b5b5983d0e0fb52d9ab70
    Port:          <none>
    Host Port:     <none>
    Command:
      sh
    State:          Running
      Started:      Thu, 29 Aug 2019 06:42:21 +0000
    Ready:          False
    Restart Count:  0
    Environment:    <none>
    Mounts:         <none>
...
```

您可以与使用kubectl attach、kubectl exec和kubectl logs的与其他容器相同的方式，和新的临时容器交互，例如：
```shell
kubectl attach -it example-pod -c debugger
```