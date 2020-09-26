

# 本文描述所有Controllers

---

# ReplicationController

> 注意：配置ReplicaSet的Deployment现在是设置复制的推荐方法。

ReplicationController 确保在任何时候运行指定数量的pod副本。换句话说，复制控制器确保一个pod或一组同类的pod始终处于可用状态

## How a ReplicationController Works

如果有太多的pod，复制控制器会终止额外的pods。如果太少，复制控制器会启动更多的pod。与手动创建的pod不同，由复制控制器维护的pod在发生故障、被删除或被终止时会自动被替换。例如，在中断性维护（如内核升级）之后，将在节点上重新创建pod。因此，即使应用程序只需要一个pod，也应该使用ReplicationController。复制控制器类似于进程管理器，但它不是监视单个节点上的单个进程，而是跨多个节点监视多个pod。

在讨论中，ReplicationController通常缩写为“rc”，并且在kubectl命令中作为快捷方式。

一个简单的例子是创建一个ReplicationController对象来无限期可靠地运行一个Pod实例。一个更复杂的用例是运行一个复制服务的多个相同副本，例如web服务器


## Running an example ReplicationController

This example ReplicationController config runs three copies of the nginx web server.

controllers/replication.yaml 

```yaml
apiVersion: v1
kind: ReplicationController
metadata:
  name: nginx
spec:
  replicas: 3
  selector:
    app: nginx
  template:
    metadata:
      name: nginx
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx
        ports:
        - containerPort: 80
```

Run the example job by downloading the example file and then running this command:
```shell
kubectl apply -f https://k8s.io/examples/controllers/replication.yaml
#output
replicationcontroller/nginx created

kubectl describe replicationcontrollers/nginx
#output
Name:        nginx
Namespace:   default
Selector:    app=nginx
Labels:      app=nginx
Annotations:    <none>
Replicas:    3 current / 3 desired
Pods Status: 0 Running / 3 Waiting / 0 Succeeded / 0 Failed
Pod Template:
  Labels:       app=nginx
  Containers:
   nginx:
    Image:              nginx
    Port:               80/TCP
    Environment:        <none>
    Mounts:             <none>
  Volumes:              <none>
Events:
  FirstSeen       LastSeen     Count    From                        SubobjectPath    Type      Reason              Message
  ---------       --------     -----    ----                        -------------    ----      ------              -------
  20s             20s          1        {replication-controller }                    Normal    SuccessfulCreate    Created pod: nginx-qrm3m
  20s             20s          1        {replication-controller }                    Normal    SuccessfulCreate    Created pod: nginx-3ntk0
  20s             20s          1        {replication-controller }                    Normal    SuccessfulCreate    Created pod: nginx-4ok8v
```

Here, three pods are created, but none is running yet, perhaps because the image is being pulled. A little later, the same command may show:

```shell
Pods Status:    3 Running / 0 Waiting / 0 Succeeded / 0 Failed
```

要以机器可读的形式列出属于复制控制器的所有pod，可以使用如下命令：

```shell
pods=$(kubectl get pods --selector=app=nginx --output=jsonpath={.items..metadata.name})
echo $pods
#output
nginx-3ntk0 nginx-4ok8v nginx-qrm3m
```

这里，选择器与ReplicationController的选择器相同（见kubectl descripe output），但是使用和replication.yaml中不同的形式. `--output=jsonpath`选项指定一个表达式，该表达式只从返回列表中的每个pod获取名称。

## Writing a ReplicationController Spec

与所有其他Kubernetes配置一样，复制控制器需要apiVersion、kind和metadata字段。复制控制器对象的名称必须是有效的DNS子域名。有关使用配置文件的常规信息，请参阅[对象管理](https://kubernetes.io/docs/concepts/overview/working-with-objects/object-management/)。

复制控制器还需要.spec部分

### Pod Template

`.spec.template`是.spec的唯一必需字段。
`.spec.template`是[pod template](https://kubernetes.io/docs/concepts/workloads/pods/#pod-templates)。它的模式与Pod完全相同，只是它是嵌套的，没有apiVersion或kind。

除了Pod的必需字段外，复制控制器中的Pod模板必须指定适当的标签和适当的重新启动策略。对于标签，请确保不要与其他控制器重叠。参见[pod选择器](https://kubernetes.io/docs/concepts/workloads/controllers/replicationcontroller/#pod-selector)。

只有`.spec.template.spec.restartPolicy`等于Always是允许的，如果未指定，则为默认值。

对于本地容器重新启动，复制控制器委托给节点上的代理，例如Kubelet或Docker。

### Labels on the ReplicationController 

复制控制器本身可以有标签（`.metadata.labels`). 通常，将这些设置和`.spec.template.metadata.labels`相同；如果`.metadata.labels`未指定，则默认为`.spec.template.metadata.labels`。但是，它们可以是不同的, `.metadata.labels`不影响复制控制器的行为

###  Pod Selector

这个`.spec.selector`字段是一个[标签选择器](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#label-selectors)。复制控制器使用与选择器匹配的标签管理所有的pod。它不区分它创建或删除的pod和其他人或进程创建或删除的pod。这样就可以在不影响运行pod的情况下更换复制控制器。

如果指定，则`.spec.template.metadata.labels`必须等于`.spec.selector`，否则将被API拒绝。如果`.spec.selector`未指定，则默认为`.spec.template.metadata.labels`。

另外，您通常不应该创建任何标签与此选择器匹配的pod，或者直接与另一个复制控制器（例如Job）匹配。如果你这样做，复制控制器会认为它创建了其他的pod。Kubernetes并没有阻止你这么做。

如果您最终使用多个具有重叠选择器的控制器，则必须自己管理删除操作（见下文）。

### Multiple Replicas 
您可以通过设置`.spec.replicas`来指定应该同时运行多少个pod。在任何时候运行的pod数量可能会更高或更低，例如复制副本刚刚增加或减少，或者如果一个pod正常关闭，并且提前开始替换。

如果不指定`.spec.replicas`，则默认为1。

## Working with ReplicationControllers
### Deleting a ReplicationController and its Pods 

要删除复制控制器及其所有pod，请使用kubectl delete。Kubectl将复制控制器缩放到零，并等待它删除每个pod，然后再删除ReplicationController本身。如果这个kubectl命令被中断，可以重新启动它。

使用REST API或go client library时，需要显式地执行这些步骤（将副本缩放到0，等待pod删除，然后删除ReplicationController）。

### Deleting just a ReplicationController

您可以删除复制控制器而不影响其任何pod。

使用kubectl，将`--cascade=false`选项指定给kubectl delete。

使用REST API或go客户端库时，只需删除ReplicationController对象。

删除原始副本后，可以创建新的复制控制器来替换它。只要旧的和新的`.spec.selector`都是一样的，那么新的将收养旧的pod。然而，它不会做出任何努力，使现有的pod匹配一个新的，不同的pod模板。要以受控的方式将pods更新到新的规范，请使用[滚动更新](https://kubernetes.io/docs/concepts/workloads/controllers/replicationcontroller/#rolling-updates)。


## Common usage patterns 
### Rescheduling
如上所述，无论您有1个要继续运行的pod，还是1000个，ReplicationController都将确保存在指定数量的pod，即使在节点故障或pod终止的情况下（例如，由于另一个control agent的操作）。

### Scaling
ReplicationController只需更新replicas字段，就可以轻松地通过手动或自动缩放控制代理来缩放副本数量。

### Rolling updates

ReplicationController旨在通过逐个替换pod来实现对服务的滚动更新。

如[#1353](https://issue.k8s.io/1353)所述，建议的方法是创建一个带有1个副本的新复制控制器，逐个缩放新的（+1）和旧的（-1）控制器，然后在旧控制器达到0个副本后删除它。这可以可预测地更新pod集，而不管意外的失败。

理想情况下，滚动更新控制器将考虑应用程序的就绪性，并确保在任何给定时间都有足够数量的pod有效地服务。

这两个复制控制器需要创建至少有一个区分标签的pod，例如pod主容器的image标签，因为rolling update通常是镜像更新。

### Multiple release tracks 

除了在滚动更新过程中运行一个应用程序的多个版本外，通常会使用多个版本跟踪来长时间运行多个版本，甚至连续运行多个版本。这些跟踪将通过标签加以区分。

例如，一个服务可能使用`tier in (frontend), environment in (prod)`来定位pod。现在假设您有10个复制的pod组成这个tier。但是你希望能够'金丝雀canary'这个组件的新版本。可以设置一个ReplicationController，ReplicationController的`replicas`设置为9，labels设置为`tier=frontend，environment=prod，track=stable`；对于canary，可以设置一个ReplicationController，`replicas`设置为1，labels设置为`tier=frontend，environment=prod，track=canary`。现在，这项服务涵盖了金丝雀和非金丝雀pod。但是你可以单独处理复制控制器来测试，监控等等。

### Using ReplicationControllers with Services 
多个复制控制器可以位于一个服务Service后面，因此，例如，一些流量流向旧版本，而另一些流量流向新版本。

复制控制器永远不会自行终止，但它的寿命不会像服务那样长。服务可以由多个复制控制器控制的pod组成，并且在服务的生命周期内，可能会创建和销毁许多复制控制器（例如，更新运行服务的pod）。服务本身和它们的客户机不需要知晓维护服务的pod的复制控制器。


## Writing programs for Replication 

复制控制器创建的pod是可替换的，并且在语义上是相同的，尽管随着时间的推移，它们的配置可能变得异构。这显然适合于replicated无状态服务器，但复制控制器也可以用于维护主选择master-elected、分片sharded和工作池worker-pool应用程序的可用性。这样的应用程序应该使用动态的工作分配机制，比如RabbitMQ工作队列，而不是静态/一次性定制每个pod的配置(这被认为是反模式)。执行任何pod自定义过程，例如资源（例如cpu或内存）的垂直自动调整大小，都应该由另一个在线的控制器进程执行，就像复制控制器本身一样。

## Responsibilities of the ReplicationController 

ReplicationController只需确保所需数量的pod与它的标签选择器相匹配，并且pod是可操作的。目前，只有终止的pod不在其计数范围内。将来，系统中 [readiness](https://issue.k8s.io/620) 和其他信息可能会被考虑在内，我们可能会增加对替换策略的更多控制，并且我们计划发出事件，这些事件可以被外部客户机用来执行任意复杂的替换和/或缩小策略。

复制控制器永远受限于这一狭隘的责任。它本身不会执行readiness或liveness探测。它不会自动执行缩放，而是由一个外部自动缩放器控制（如[#492](https://issue.k8s.io/492)中所述）（通过更改其replicas字段）。我们不会将调度策略（例如，[传播spreading](https://issue.k8s.io/367#issuecomment-48428019)）添加到复制控制器。它也不应该验证所控制的pod是否与当前指定的模板匹配，因为那样会妨碍自动调整大小和其他自动化过程。类似地， completion deadlines、ordering dependencies顺序依赖、配置扩展和其他特性也属于其他地方。我们甚至计划去掉创建大量pod的机制。

ReplicationController旨在成为一个可组合的构建块原语。为了方便用户，我们希望将来在它和其他补充原语的基础上构建更高级别的api和/或工具。kubectl（run，scale）目前支持的“宏”操作就是这方面的概念证明示例。就好比，我们可以想象是Asgard在管理ReplicationControllers, auto-scalers, services, scheduling policies, canaries等。

## API Object 
复制控制器是kubernetes REST API中的顶层资源。有关API对象的更多详细信息，请访问：[ReplicationController API object](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#replicationcontroller-v1-core)


## Alternatives to ReplicationController
### ReplicaSet

ReplicaSet是新一代的复制控制器，它支持新的基于[集合的标签选择器](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#set-based-requirement)。它主要用于Deployment，作为协调pod创建、删除和更新的机制。请注意，我们建议使用Deployment而不是直接使用副本集，除非您需要自定义更新编排或根本不需要更新

### Deployment (Recommended)

Deployment是一个更高级别的API对象，它更新其底层副本集及其pod。如果需要这种滚动更新功能，建议Deployment，因为它们是声明性的、服务器端的，并且具有附加功能。

### Bare Pods
与用户直接创建pod的情况不同，ReplicationController替换由于任何原因而被删除或终止的pod，例如在节点故障或中断性节点维护（如内核升级）的情况下。因此，我们建议您使用复制控制器，即使您的应用程序只需要一个pod。可以把它想象成一个进程管理器，只是它管理多个节点上的多个pod，而不是单个节点上的单个进程。复制控制器将本地容器重新启动委托给节点上的某个代理（例如Kubelet或Docker）。

### Job
对预期将自行终止的pod（即批处理作业）使用作业而不是复制控制器

### DaemonSet
对于提供机器级功能（如机器监视或机器日志记录）的pod，使用DaemonSet而不是复制控制器。这些pod的生命周期与机器的生命周期相关联：pod需要在其他pod启动之前在机器上运行，并且在机器准备好重新启动/关闭时可以安全地终止。

## For more information
Read [Run Stateless Application Deployment](https://kubernetes.io/docs/tasks/run-application/run-stateless-application-deployment/).


