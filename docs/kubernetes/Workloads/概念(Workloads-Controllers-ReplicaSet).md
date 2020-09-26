

# 本文描述所有Controllers

---
# ReplicaSet 复制集

复制集的目的是维护能在任意给定时间稳定运行的一组副本pod。因此，它通常用于保证特定数量的相同pod的可用性

## How a ReplicaSet works

复制集由一些字段定义，包括一个selector字段，用于指定如何识别它可以获取的pod，一个number字段，用于指示它应该维护多少个pod的副本，以及一个pod template，该模板指定了它应该创建的新pod的数据，以满足副本数量的要求。然后，复制集通过根据需要创建和删除pod来调整以达到所需的数量。当复制集需要创建新的Pod时，使用Pod template进行创建。

复制集通过Pods的[metadata.ownerReferences](https://kubernetes.io/docs/concepts/workloads/controllers/garbage-collection/#owners-and-dependents)字段与Pods关联，该字段指定当前对象的什么资源被ownerReferences拥有。ReplicaSet选中的所有Pod在他们的ownerReferences字段里都有拥有者ReplicaSet的标识信息。ReplicaSet就是通过这个链接关系知道它正在维护的pod的状态并相应地进行计划。

复制集使用它的选择器标识要获取的新pod。如果存在一个没有OwnerReference的Pod，或者OwnerReference指的不是Controller且与复制集的选择器匹配，则该Pod将立即被所述复制集选中获取。

## When to use a ReplicaSet

复制集确保在任何给定的时间运行指定数量的pod副本。然而，Deployment是一个更高层次的概念，它管理复制集并为pod提供声明式的更新以及许多其他有用的特性。因此，我们建议使用Deployment而不是直接使用复制集，除非您需要自定义更新编排或根本不需要更新。

这实际上意味着您可能永远不需要操作复制集对象：而是使用Deployment在spec部分定义应用程序。

## Example
controllers/frontend.yaml 

```yaml
apiVersion: apps/v1
kind: ReplicaSet
metadata:
  name: frontend
  labels:
    app: guestbook
    tier: frontend
spec:
  # modify replicas according to your case
  replicas: 3
  selector:
    matchLabels:
      tier: frontend
  template:
    metadata:
      labels:
        tier: frontend
    spec:
      containers:
      - name: php-redis
        image: gcr.io/google_samples/gb-frontend:v3
```
Saving this manifest into frontend.yaml and submitting it to a Kubernetes cluster will create the defined ReplicaSet and the Pods that it manages.
```shell
kubectl apply -f https://kubernetes.io/examples/controllers/frontend.yaml
kubectl get rs

# output
NAME       DESIRED   CURRENT   READY   AGE
frontend   3         3         3       6s

kubectl describe rs/frontend

# output
Name:         frontend
Namespace:    default
Selector:     tier=frontend
Labels:       app=guestbook
              tier=frontend
Annotations:  kubectl.kubernetes.io/last-applied-configuration:
                {"apiVersion":"apps/v1","kind":"ReplicaSet","metadata":{"annotations":{},"labels":{"app":"guestbook","tier":"frontend"},"name":"frontend",...
Replicas:     3 current / 3 desired
Pods Status:  3 Running / 0 Waiting / 0 Succeeded / 0 Failed
Pod Template:
  Labels:  tier=frontend
  Containers:
   php-redis:
    Image:        gcr.io/google_samples/gb-frontend:v3
    Port:         <none>
    Host Port:    <none>
    Environment:  <none>
    Mounts:       <none>
  Volumes:        <none>
Events:
  Type    Reason            Age   From                   Message
  ----    ------            ----  ----                   -------
  Normal  SuccessfulCreate  117s  replicaset-controller  Created pod: frontend-wtsmm
  Normal  SuccessfulCreate  116s  replicaset-controller  Created pod: frontend-b2zdv
  Normal  SuccessfulCreate  116s  replicaset-controller  Created pod: frontend-vcmts
```

You can also verify that the owner reference of these pods is set to the frontend ReplicaSet. To do this, get the yaml of one of the Pods running:

```shell
kubectl get pods frontend-b2zdv -o yaml

apiVersion: v1
kind: Pod
metadata:
  creationTimestamp: "2020-02-12T07:06:16Z"
  generateName: frontend-
  labels:
    tier: frontend
  name: frontend-b2zdv
  namespace: default
  ownerReferences:
  - apiVersion: apps/v1
    blockOwnerDeletion: true
    controller: true
    kind: ReplicaSet
    name: frontend
    uid: f391f6db-bb9b-4c09-ae74-6a1f77f3d5cf
...
```

## Non-Template Pod acquisitions
虽然您可以毫无问题地创建裸Pods，但强烈建议确保裸Pods没有与您的其中一个复制集的选择器匹配的标签。这样做的原因是，复制集并不局限于拥有由其模板指定的pod —— 它可以按照前面几节中指定的方式获取其他pod

以前面的前端复制集为例，以及在以下清单中指定的pod：

pods/pod-rs.yaml 

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: pod1
  labels:
    tier: frontend
spec:
  containers:
  - name: hello1
    image: gcr.io/google-samples/hello-app:2.0

---

apiVersion: v1
kind: Pod
metadata:
  name: pod2
  labels:
    tier: frontend
spec:
  containers:
  - name: hello2
    image: gcr.io/google-samples/hello-app:1.0
```

由于这些pod没有控制器（或任何对象）作为其所有者引用owner reference，并且由于其标签和上面的fronted ReplicaSet的选择器相匹配，因此这些pod会立即被fronted ReplicaSet获取。

假设fronted ReplicaSet已经部署并启动了满足副本数量要求的pod，再创建如下的pod：
```shell
kubectl apply -f https://kubernetes.io/examples/pods/pod-rs.yaml
```

新的pod将被复制集获取，然后复制集认为pod的个数超过了其期望值，因此会立即把这两个pod终止掉。

查看Pods信息：
```shell
kubectl get pods

NAME             READY   STATUS        RESTARTS   AGE
frontend-b2zdv   1/1     Running       0          10m
frontend-vcmts   1/1     Running       0          10m
frontend-wtsmm   1/1     Running       0          10m
pod1             0/1     Terminating   0          1s
pod2             0/1     Terminating   0          1s
```

如果先创建pod，再创建ReplicaSet:
```shell
kubectl apply -f https://kubernetes.io/examples/pods/pod-rs.yaml

kubectl apply -f https://kubernetes.io/examples/controllers/frontend.yaml
```

您将看到replicaSet已经获得了Pods，并且只根据它的规范创建新的Pods，并且新创建的Pods和原始的Pods的数量与期望的数量一致。获取Pods信息：
```shell
kubectl get pods

# 你会发现
NAME             READY   STATUS    RESTARTS   AGE
frontend-hmmj2   1/1     Running   0          9s
pod1             1/1     Running   0          36s
pod2             1/1     Running   0          36s
```

在这种创建方式下，复制集拥有的是一组非同质的pod。

## Writing a ReplicaSet manifest 

与所有其他kubernetes API对象一样，复制集需要apiVersion、kind和metadata字段。对于replicaSet，kind总是replicaSet。在kubernetes 1.9中，replicaSet kind的API版本apps/v1是当前版本，默认情况下是启用的。API版本apps/v1beta2已弃用。请参阅fronted.yaml指导示例。

复制集对象的名称必须是有效的[DNS子域名](https://kubernetes.io/docs/concepts/overview/working-with-objects/names#dns-subdomain-names)。

复制集还需要.spec部分。

### Pod Template
`.spec.template`是一个pod模板，同时需要拥有labels。在上面的fronted.yaml例子中我们有一个label：`tier:fronted`。小心不要与其他控制器的选择器重叠，以免他们试图控制此pod。

对于模板的`restart policy`字段, `.spec.template.spec.restartPolicy`，唯一允许的值是Always，这是默认值

### Pod Selector
`.spec.selector`字段是一个[标签选择器](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/)。如前所述，这些标签用于识别潜在的pod。在我们的frontend.yaml例子中，选择器是：
```yaml
matchLabels:
  tier: frontend
```

在复制集中, `.spec.template.metadata.labels`必须匹配`spec.selector`，否则将被API拒绝。

> 注意：对于2个拥有相同`.spec.selector`，但是不同的`.spec.template.metadata.labels`，不同的`.spec.template.spec`的ReplicaSet，复制集会相互忽略另一个复制集创建的pod。

### Replicas

您可以通过设置`.spec.replicas`来指定应该同时运行多少个pod。复制集将创建/删除pod以匹配来维护pod数量。

如果不指定.spec.replicas，则默认为1

## Working with ReplicaSets
### Deleting a ReplicaSet and its Pods
要删除复制集及其所有pod，请使用`kubectl delete`。默认情况下，[垃圾回收器](https://kubernetes.io/docs/concepts/workloads/controllers/garbage-collection/)会自动删除所有依赖的pod。

使用 REST API或client-go库时，必须在 -d 选项中将propagationPolicy设置为Background或Foreground。例如：
```shell
kubectl proxy --port=8080
curl -X DELETE  'localhost:8080/apis/apps/v1/namespaces/default/replicasets/frontend' \
> -d '{"kind":"DeleteOptions","apiVersion":"v1","propagationPolicy":"Foreground"}' \
> -H "Content-Type: application/json"
```

### Deleting just a ReplicaSet 
您可以使用带有`--cascade=false`选项的`kubectl delete`删除一个复制集，而不会影响它的任何pod。使用REST API或client-go库时，必须将propagationPolicy设置为Orphan。例如：
```shell
kubectl proxy --port=8080
curl -X DELETE  'localhost:8080/apis/apps/v1/namespaces/default/replicasets/frontend' \
> -d '{"kind":"DeleteOptions","apiVersion":"v1","propagationPolicy":"Orphan"}' \
> -H "Content-Type: application/json"
```

删除原始副本后，可以创建一个新的复制集来替换它。只要旧的和新的`.spec.selector`都是一样的，那么新的ReplicaSet就会接管旧的pod。然而，它不会使现有的pod去匹配一个新的，不同的pod template。要以受控的方式将pod更新到新spec，请使用Deployment，因为复制集不直接支持滚动更新。

### Isolating Pods from a ReplicaSet 从复制集中分离Pods
您可以通过更改pod的标签从复制集中移除它们。此技术可用于从服务中删除Pod以进行调试、数据恢复等。以这种方式删除的Pod将被自动替换（假设副本的数量也没有改变）。


### Scaling a ReplicaSet 缩放复制集

只需更新`.spec.replicas`字段来缩小或扩大ReplicaSet。ReplicaSet控制器会保证具有匹配标签选择器的所需数量的pod可用且可操作。

### ReplicaSet as a Horizontal Pod Autoscaler Target 复制集作为水平pod自动缩放目标

复制集也可以作为 [Horizontal Pod Autoscalers](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/)（HPA）的目标。也就是说，复制集可以由HPA自动缩放。下面是一个示例HPA，目标是我们在上一个示例中创建的复制集。

controllers/hpa-rs.yaml 

```yaml
apiVersion: autoscaling/v1
kind: HorizontalPodAutoscaler
metadata:
  name: frontend-scaler
spec:
  scaleTargetRef:
    kind: ReplicaSet
    name: frontend
  minReplicas: 3
  maxReplicas: 10
  targetCPUUtilizationPercentage: 50
```

将此清单保存到hpa-rs.yaml将它提交给Kubernetes集群，创建定义好的HPA，该HPA根据复制pod的CPU使用情况自动调整目标复制集。

```shell
kubectl apply -f https://k8s.io/examples/controllers/hpa-rs.yaml
```

或者，您可以使用kubectl autoscale命令来完成相同的操作（而且更简单！）
```shell
kubectl autoscale rs frontend --max=10 --min=3 --cpu-percent=50
```

## Alternatives to ReplicaSet 复制集的替代品
### Deployment (recommended)

Deployment是一个对象，它可以拥有复制集，并通过声明性的服务器端滚动更新来更新ReplicaSet及其pod。虽然复制集可以独立使用，但今天它们主要被Deployment用作协调Pod创建、删除和更新的机制。使用Deployment时，您不必担心管理Deployment创建的复制集。Deployment拥有并管理其复制集。因此，建议在需要复制集时使用Deployment。

### 纯/裸 Pods

与用户直接创建pod的情况不同，ReplicaSet替换由于任何原因被删除或终止的pod，例如在节点故障或中断性节点维护（如内核升级）的情况下。因此，我们建议您使用一个复制集，即使您的应用程序只需要一个Pod。可以把它想象成一个进程管理器，只是它管理多个节点上的多个pod，而不是单个节点上的单个进程。复制集将本地容器重新启动委托给节点上的某个代理（例如Kubelet或Docker）

### Job
对预期将自行终止的pod（即批处理作业）使用Job而不是复制集

### DaemonSet 守护集
对于提供机器级功能（如机器监视或机器日志记录）的pod，请使用守护程序集而不是复制集。这些Pod的生命周期与机器的生命周期相关联：Pod需要在其他Pod启动之前在机器上运行，并且在机器准备好重新启动/关闭时可以安全地终止

### ReplicationController

复制集是[复制控制器](https://kubernetes.io/docs/concepts/workloads/controllers/replicationcontroller/)的继承者。这两个功能相同，行为相似，只是复制控制器不支持[标签用户指南](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#label-selectors)中描述的基于集合的选择器要求。因此，复制集优于复制控制器