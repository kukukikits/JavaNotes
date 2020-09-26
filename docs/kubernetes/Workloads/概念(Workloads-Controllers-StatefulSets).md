

# 本文描述所有Controllers

---
# StatefulSets

StatefulSet是用于管理有状态应用程序的工作负载API对象。

管理一组pod的部署和扩展，并保证这些pod的顺序和唯一性。

与deployment类似，StatefulSet管理基于相同容器规范的pod。与部署不同，StatefulSet为每个pod维护固定标识。这些pod是从同一个规范创建的，但是不可互换：每个pod都有一个持久标识符，它在任何重新调度时都会维护这个标识符。

如果要使用存储卷为工作负载提供持久性，可以使用StatefulSet作为解决方案的一部分。尽管StatefulSet中的单个Pod容易发生故障，但持久性Pod标识符使现有卷与新Pod（用于替换任何失败的Pod）更容易匹配

## Using StatefulSets

状态集对于需要以下一项或多项的应用程序很有价值：
- 稳定、唯一的网络标识符。
- 稳定、持久的存储。
- 有序、优雅的部署和扩展。
- 有序、自动滚动更新。

在上面，stable是持久性在跨Pod调度/重新调度的同义词。如果应用程序不需要任何稳定的标识符或有序的部署、删除或扩展，则应该使用提供一组无状态副本的工作负载对象来部署应用程序。部署或复制集可能更适合您的无状态需求。

## Limitations 

- 给定Pod的存储必须由[PersistentVolume Provisioner](https://github.com/kubernetes/examples/tree/master/staging/persistent-volume-provisioning/README.md)根据请求的存储类`storage class`进行配置，或者由管理员预先配置。
- 删除和/或缩小StatefulSet不会删除与StatefulSet关联的卷。这样做是为了确保数据安全，这通常比自动清除所有相关的StatefulSet资源更有用。
- StatefulSets当前需要一个[Headless Service](https://kubernetes.io/docs/concepts/services-networking/service/#headless-services)来负责Pods的网络标识。需要自行创建此服务。
- StatefulSet不提供在删除StatefulSet时终止pods的任何保证。为了在StatefulSet中实现有序和优雅的终止，可以在删除StatefulSet之前将StatefulSet缩小到0。
- 当使用默认 [Pod Management Policy](https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/#pod-management-policies)（OrderedReady）来[滚动更新](https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/#rolling-updates)时，[可能会出现损坏状态，此时需要手动干预进行修复](https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/#forced-rollback)。
  
## Components
The example below demonstrates the components of a StatefulSet.
```yaml
apiVersion: v1
kind: Service
metadata:
  name: nginx
  labels:
    app: nginx
spec:
  ports:
  - port: 80
    name: web
  clusterIP: None
  selector:
    app: nginx
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: web
spec:
  selector:
    matchLabels:
      app: nginx # has to match .spec.template.metadata.labels
  serviceName: "nginx"
  replicas: 3 # by default is 1
  template:
    metadata:
      labels:
        app: nginx # has to match .spec.selector.matchLabels
    spec:
      terminationGracePeriodSeconds: 10
      containers:
      - name: nginx
        image: k8s.gcr.io/nginx-slim:0.8
        ports:
        - containerPort: 80
          name: web
        volumeMounts:
        - name: www
          mountPath: /usr/share/nginx/html
  volumeClaimTemplates:
  - metadata:
      name: www
    spec:
      accessModes: [ "ReadWriteOnce" ]
      storageClassName: "my-storage-class"
      resources:
        requests:
          storage: 1Gi
```

在上述示例中：
- 一个名为nginx的Headless Service用于控制网络域。
- 名为web的StatefulSet有一个规范，指明nginx容器的3个副本将在唯一的Pods中启动。
- volumeClaimTemplates将使用PersistentVolume Provisioner的PersistentVolumes提供稳定的存储。

StatefulSet对象的名称必须是有效的DNS子域名。


## Pod Selector
你必须设置StatefulSet的`.spec.selector`字段，来匹配其标签`.spec.template.metadata.labels`。在Kubernetes 1.8之前`.spec.selector`字段在省略时为默认值。在1.8及更高版本中，未能指定匹配的Pod selector将导致StatefulSet创建期间出现验证错误。

## Pod Identity

StatefulSet pods有一个唯一的标识，它由一个序号、一个稳定的网络标识和一个稳定的存储组成。无论在哪个节点上（重新）调度，标识都会保留在Pod上

### Ordinal Index 序数索引
对于一个有N个副本的StatefulSet，StatefulSet中的每个Pod都将被分配一个整数序数，从0到N-1，在集合上是唯一的。

### Stable Network ID

StatefulSet中的每个Pod从StatefulSet的名称和Pod的序号派生其主机名。构造的主机名的模式是`$(statefulset name)-$(ordinal)`。上面的示例将创建三个名为web-0、web-1、web-2的pod。StatefulSet可以使用Headless Service来控制其pod的域。此服务管理的域采用以下形式：`$(service name).$(namespace).svc.cluster.local`，其中`cluster.local`是群集域。创建每个Pod时，它将获得一个匹配的DNS子域，格式为：`$(podname).$(governing service domain)`，其中governing service由StatefulSet上的serviceName字段定义。

根据DNS在群集中的配置方式，您可能无法立即查找新运行的Pod的DNS名称。当群集中的其他客户机在创建Pod之前已经发送了对Pod的主机名的查询时，可能会发生这种现象。负缓存（在DNS中是正常的）意味着即使在Pod运行之后，也会记住并重用以前失败的查找结果至少几秒钟。

如果您需要在创建Pod后立即发现它们，您有以下几种选择：
- 直接查询kubernetes api（例如，使用watch），而不是依赖于DNS查找。
- 减少Kubernetes DNS提供程序中的缓存时间（这意味着要编辑CoreDNS的配置映射，它当前缓存30秒）。

正如限制部分中提到的，您要自行创建负责pods的网络标识的Headless服务。

下面是一些集群域、服务名称、状态集名称的示例，以及它们如何影响StatefulSet的pod的DNS名称。

Cluster Domain | Service (ns/name) | StatefulSet (ns/name) | StatefulSet Domain | Pod DNS | Pod Hostname
---------|----------|---------|---------|---------|---------
cluster.local	| default/nginx	| default/web	| nginx.default.svc.cluster.local |	web-{0..N-1}.nginx.default.svc.cluster.local |web-{0..N-1}
cluster.local	| foo/nginx	| foo/web	| nginx.foo.svc.cluster.local	| web-{0..N-1}.nginx.foo.svc.cluster.local |	web-{0..N-1}
kube.local | foo/nginx | foo/web | nginx.foo.svc.kube.local | web-{0..N-1}.nginx.foo.svc.kube.local | web-{0..N-1}


> 注意：群集域将设置为`cluster.local`除非[另有配置](https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/)

### Stable Storage

Kubernetes为每个VolumeClaimTemplate创建一个PersistentVolume。在上面的nginx示例中，每个Pod将接收一个PersistentVolume，其中一个StorageClass是`my-storage-class`，并且配置了1Gib是的存储。如果未指定StorageClass，则将使用默认的StorageClass。将Pod（重新）调度到节点上时，其volumeMounts装载与其PersistentVolume声明关联的PersistentVolumes。请注意，在删除Pods或StatefulSet时，不会删除与Pods的PersistentVolume声明关联的PersistentVolumes。PersistentVolume必须手动删除。

### Pod Name Label
当StatefulSet控制器创建一个Pod时，它会添加一个label，`statefulset.kubernetes.io/pod-name`，即设置为pod的名称。此标签允许您将服务附加到StatefulSet中的特定Pod

## Deployment and Scaling Guarantees

- 对于具有N个副本的StatefulSet，当部署pod时，它们是按顺序创建的，从{0..N-1}开始。
- 当pod被删除时，它们以相反的顺序终止，从{N-1..0}。
- 在将缩放操作应用于某一个Pod之前，它前面的所有pod必须已运行并准备就绪。
- 在某一个Pod被终止之前，它后面的(序号较小的)所有pod必须完全关闭。

StatefulSet不应指定`pod.Spec.TerminationGracePeriodSeconds`为0。这种做法是不安全的，强烈反对。有关进一步的解释，请参阅[强制删除StatefulSet Pods](https://kubernetes.io/docs/tasks/run-application/force-delete-stateful-set-pod/)。

在创建上面的nginx示例时，将按照web-0、web-1、web-2的顺序部署三个pod。在web-0运行并准备就绪之前不会部署web-1，在web-1运行并准备就绪之前不会部署web-2。如果web-0失败，则在web-1运行并准备就绪之后，但在web-2启动之前，web-2将不会启动，直到web-0成功重新启动并开始运行并准备就绪。

### Pod Management Policies 
在kubernetes 1.7及更高版本中，StatefulSet允许您放松其排序保证，同时通过`.spec.podManagementPolicy`字段保持其唯一性和身份。

1. OrderedReady Pod Management
   `OrderedReady` pod管理是StatefulSets的默认设置。它实现Deployment and Scaling Guarantees所述行为
2. Parallel Pod Management
   并行pod管理告诉StatefulSet控制器并行启动或终止所有pod，在启动或终止另一个pod之前，不要等待pod开始运行并准备就绪或完全终止。此选项仅影响缩放操作的行为, 更新不受影响

## Update Strategies 

在Kubernetes 1.7和更高版本中，StatefulSet的`.spec.updateStrategy`字段允许您配置和禁用StatefulSet的Pod的容器、labels、resources request/limits、注释的自动滚动更新。

### On Delete

OnDelete更新策略实现了遗留（1.6及更早版本）行为。当一个状态集的`.spec.updateStrategy.type`设置为OnDelete时，StatefulSet控制器将不会自动更新StatefulSet中的Pods。用户必须手动删除pod，然后控制器才会去创建新的pod，保持StatefulSet的`.spec.template`所做的修改保持一致.

### Rolling Updates

RollingUpdate更新策略为StatefulSet中的pod实现自动的滚动更新。在没有指定`.spec.updateStrategy`时，这是默认值。当一个状态集的`.spec.updateStrategy.type`设置为RollingUpdate时，StatefulSet控制器将删除并重新创建StatefulSet中的每个Pod。它将按照与Pod终止相同的顺序进行（从最大的顺序到最小的顺序），一次更新一个Pod。它将等待更新后的Pod正在运行并准备就绪，然后再更新下一个(predecessor: 前面的，也就是序号小的)。

1. Partitions
   通过指定一个`.spec.updateStrategy.rollingUpdate.partition`可以让RollingUpdate更新策略分区。如果指定了分区，则当StatefulSet的`.spec.template`更新时，所有序号大于等于partition值的pod都会被更新。所有序号小于partition值的pod都不会被更新，并且即使这些pod已经被删除，也会重新创建和之前版本一样的pod。如果状态集的`.spec.updateStrategy.rollingUpdate.partition`大于它的`.spec.replicas`，`.spec.template`更新将不会传播到它的pod（也就是不会更新pod）。通常情况下，您不需要使用分区，但是如果您想要阶段化更新、推出canary或执行分阶段roll out，则这些分区非常有用。

2. Forced Rollback
   当使用默认Pod Management Policy（OrderedReady）的滚动更新时，可能会进入broken状态，此时需要手动干预进行修复。
   
   如果您将Pod template更新为一个永远不会运行和准备就绪的配置（例如，由于错误的二进制或应用程序级配置错误），StatefulSet将停止rollout并一直等待。
   
   在这种状态下，仅仅将Pod模板恢复到一个正确配置是不够的。由于一个[已知的问题](https://github.com/kubernetes/kubernetes/issues/67250)，在尝试将其恢复到工作配置前，StatefulSet将继续等待损坏的Pod准备就绪（事实上并不会就绪）。
  
   恢复模板后，还必须删除StatefulSet已尝试使用错误配置运行的任何pod。然后StatefulSet才会开始使用恢复的模板重新创建Pods

## What's next
- Follow an example of [deploying a stateful application](https://kubernetes.io/docs/tutorials/stateful-application/basic-stateful-set/).
- Follow an example of [deploying Cassandra with Stateful Sets](https://kubernetes.io/docs/tutorials/stateful-application/cassandra/).
- Follow an example of [running a replicated stateful application](https://kubernetes.io/docs/tasks/run-application/run-replicated-stateful-application/).
