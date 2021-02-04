# Assigning Pods to Nod

您可以将Pod限制为只能在特定节点上运行，或者更愿意在特定节点上运行。有几种方法可以做到这一点，推荐使用[标签选择器](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/)进行选择。一般来说，这样的约束是不必要的，因为调度器会自动进行合理的布局（例如，将您的pod分散到节点上，而不是将pod放置在空闲资源不足的节点上，等等），但是在某些情况下，您可能希望对pod所在的节点进行更多的控制，例如确保pod最终位于一个连接了SSD的机器，或者将来自两个不同服务的pod放在同一个可用区域中进行大量通信。

## nodeSelector 

nodeSelector是推荐的最简单的节点选择约束形式。nodeSelector是PodSpec的一个字段。它是一个键值对map。为了使pod有资格在节点上运行，节点必须拥有所有指定的键值对标签（它也可以有其他的标签）。最常见的用法是一个键值对。

让我们看一个如何使用nodeSelector的示例。

### Step Zero: Prerequisites 

这个例子假设您对kubernetes pods有基本的了解，并且已经[建立了一个Kubernetes集群](https://kubernetes.io/docs/setup/)

### Step One: Attach label to the node 

运行`kubectl get nodes`获取集群节点的名称。选择要添加标签的节点，然后运行`kubectl label nodes <node-name> <label-key>=<label-value>`将标签添加到所选节点。例如，如果我的节点名是'kubernetes-foo-node-1.c.a-robinson.internal'我想要的标签是'disktype=ssd'，然后我可以运行`kubectl label nodes kubernetes-foo-node-1.c.a-robinson.internal disktype=ssd`。

您可以通过重新运行`kubectl get nodes --show-labels`来进行验证，检查节点否有标签。您还可以使用`kubectl describe node "nodename"`查看给定节点的完整标签列表

### Step Two: Add a nodeSelector field to your pod configuration

获取您想要运行的pod的配置文件，并向其中添加一个nodeSelector部分，如下所示。例如，如果这是我的pod配置：

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
```

然后添加一个节点选择器，如下所示：
pods/pod-nginx.yaml
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
  nodeSelector:
    disktype: ssd
```

然后运行`kubectl apply -f https://k8s.io/examples/pods/pod-nginx.yaml`，Pod将在您附加标签的节点上进行调度。您可以通过运行`kubectl get pods -o wide`进行校验，查看分配了Pod的“节点”来验证。


## Interlude: built-in node labels 

除了你附加的标签外，节点还预先填充了一组标准标签。请参阅[知名标签、注释和污点](https://kubernetes.io/docs/reference/kubernetes-api/labels-annotations-taints/)，以获取这些内容的列表。

> 注意：这些标签的值是特定于云提供商的，不保证是可靠的。例如，值`kubernetes.io/hostname`在某些环境中，可能与节点名相同，而在其他环境中可能与节点名不同

## Node isolation/restriction 

向节点对象添加标签允许将pod定位到特定节点或节点组。这可以用来确保特定的pod只在具有特定隔离、安全或管理属性的节点上运行。在为此目的使用标签时，强烈建议您选择不能由节点上的kubelet进程修改的标签key。这可以防止受损节点使用其kubelet凭据在其自己的节点对象上设置这些标签，并影响调度程序将工作负载调度到受损节点。

NodeRestriction许可插件阻止kubelets设置或修改带有`node-restriction.kubernetes.io/`前缀的标签。要将该标签前缀用于节点隔离，请执行以下操作：
1. 请确保您正在使用[节点授权器](https://kubernetes.io/docs/reference/access-authn-authz/node/)并已启用[NodeRestriction许可插件](https://kubernetes.io/docs/reference/access-authn-authz/admission-controllers/#noderestriction)。
2. 在节点对象上添加`node-restriction.kubernetes.io/`前缀的标签，并在节点选择器中使用这些标签。例如，`example.com.node-restriction.kubernetes.io/fips=true`或者`example.com.node-restriction.kubernetes.io/pci-dss=true`。


## Affinity and anti-affinity

nodeSelector提供了一种非常简单的方法来将pods约束到具有特定标签的节点。亲和性/反亲和性则大大扩展了您可以表达的约束类型。主要包括：
1. 亲和性/反亲和性语言更具表现力。除了使用逻辑AND运算创建的精确匹配之外，该语言提供了更多的匹配规则；
2. 您可以指出规则是“软”/“首选项”而不是硬要求，因此如果调度程序不能满足它，pod仍将被调度；
3. 您可以限制节点上运行的其他Pod上的标签（或其他拓扑域），而不是针对节点本身的标签，允许哪些Pod可以共存或不能共存的规则

亲和性包括两种类型的亲和力，“节点亲和力”和“Pod间亲和力/反亲和力”。节点亲和性类似于现有的nodeSelector（但是具有上面列出的前两个优点），而pod间的亲和力/反亲和力针对pod标签而不是节点标签进行约束，如上面列出的第三项所述，此外还有上面列出的第一和第二个属性。

### Node affinity

Node affinity在概念上类似于nodeSelector——它允许您根据节点上的标签来约束有资格调度Pod的节点。

目前有两种类型的节点亲和性，称为`requiredDuringSchedulingIgnoredDuringExecution`和`preferredDuringSchedulingIgnoredDuringExecution`。您可以将它们分别视为“硬性”和“软性”，因为前者指定的规则必须满足，才能将pod调度到节点上（就像nodeSelector一样，但使用了更具表达性的语法），而后者只是指定了首选项，调度器将尝试强制执行但不保证成功。名称中的“IgnoredDuringExecution”部分意味着，与nodeSelector的工作方式类似，如果节点上的标签在运行时发生更改，从而不再满足pod上的亲和性规则，那么pod仍将继续在该节点上运行。将来，我们计划提供`requiredDuringSchedulingRequiredDuringExecution`，它将与`requiredDuringSchedulingIgnoredDuringExecution`一样，只是它将不再满足节点亲和性要求的Pod从节点中逐出。

因此，`requiredDuringSchedulingIgnoredDuringExecution`的示例将是“仅在具有Intel CPU的节点上运行pod”，而`preferredDuringSchedulingIgnoredDuringExecution`的示例将是“尝试在故障区域XYZ中运行这组pod，但如果不可能，则允许在其他位置运行一些”。

在PodSpec中，Node affinity在affinity字段的nodeAffinity中定义。

下面是一个使用节点亲和力的pod示例：

pods/pod-with-node-affinity.yaml
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: with-node-affinity
spec:
  affinity:
    nodeAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        nodeSelectorTerms:
        - matchExpressions:
          - key: kubernetes.io/e2e-az-name
            operator: In
            values:
            - e2e-az1
            - e2e-az2
      preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 1
        preference:
          matchExpressions:
          - key: another-node-label-key
            operator: In
            values:
            - another-node-label-value
  containers:
  - name: with-node-affinity
    image: k8s.gcr.io/pause:2.0
```

这个节点亲和性规则表示，pod只能放置在具有键为`kubernetes.io/e2e-az-name`，值为`e2e-az1`或`e2e-az2`的标签的节点上。此外，在满足该条件的节点中，具有键是`another-node-label-key`，且其值为`another-node-label-value`的标签的节点应该是首选的。

您可以看到在示例中使用的运算符`In`。新的节点亲和性语法支持以下运算符：`In`、`NotIn`、`Exists`、`DoesNotExist`、`Gt`、`Lt`。您可以使用`NotIn`和`DoesNotExist`来实现节点反亲和行为，或者使用[node taints](https://kubernetes.io/docs/concepts/scheduling-eviction/taint-and-toleration/)从特定节点排斥pod。

如果同时指定nodeSelector和nodeAffinity，则必须同时满足这两个条件，才能将pod调度到候选节点上。

如果指定多个与nodeAffinity类型关联的NodeSelectorTerms，则可以在满足其中一个nodeSelectorTerms的情况下将pod调度到一个节点上。

如果指定多个与nodeSelectorTerms关联的MatchExpression，则只有满足所有matchExpressions时，才能将pod调度到节点上。

如果移除或更改了pod的节点的标签，则不会移除pod。换句话说，亲和性选择只在调度pod时起作用。

The weight field in preferredDuringSchedulingIgnoredDuringExecution is in the range 1-100. For each node that meets all of the scheduling requirements (resource request, RequiredDuringScheduling affinity expressions, etc.), the scheduler will compute a sum by iterating through the elements of this field and adding "weight" to the sum if the node matches the corresponding MatchExpressions. This score is then combined with the scores of other priority functions for the node. The node(s) with the highest total score are the most preferred.
`preferredDuringSchedulingIgnoredDuringExecution`中的`weight`字段在1-100范围内。对于满足所有调度要求（资源请求、`RequiredDuringScheduling`亲和表达式等）的每个节点，调度器会遍历该字段中的元素，计算节点匹配MatchExpressions的和，然后就将`weight`加到这个和上。最后将该得分与该节点的其他优先级函数的得分相结合。总分最高的节点就是最优先的。


### Inter-pod affinity and anti-affinity

pod间亲和和反亲和允许您根据节点上已经运行的pod上的标签（而不是基于节点上的标签）来约束哪些节点符合调度条件。规则的形式是“如果X已经运行了一个或多个符合规则Y的pod，那么这个pod应该（或者，在反亲和的情况下，不应该）在X中运行”。Y表示为一个LabelSelector，它有一个可选的相关的命名空间列表；与节点不同，因为pod是有命名空间的（因此pod上的标签隐含命名空间），因此pod标签上的标签选择器必须指定选择器应应用于哪个命名空间。从概念上讲，X是一个拓扑域，如节点、机架、云提供商zone、云提供商region等。您可以使用`topologyKey`来表示它，`topologyKey`是系统用来表示此类拓扑域的节点标签的键；例如，请参阅上文“[Interlude: built-in node labels](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/#built-in-node-labels)”一节中列出的标签键。

> 注意：Pod间亲和力和反亲和力需要大量的处理过程，这会显著降低大集群中的调度速度。我们不建议在大于几百个节点的集群中使用它们。

> 注意：Pod反亲和性要求节点被一致地标记，换句话说，集群中的每个节点都必须有一个与topologyKey匹配的适当标签。如果某些或所有节点缺少指定的拓扑键标签，则可能导致意外行为。


与节点亲和力一样，目前有两种类型的pod亲和力和反亲和力，称为`requiredDuringSchedulingIgnoredDuringExecution`和`preferredDuringSchedulingIgnoredDuringExecution`，分别表示“硬”和“软”需求。请参阅前面的节点亲和部分中的描述。`requiredDuringSchedulingIgnoredDuringExecution` Affinity的一个例子是“将服务A和服务B的Pod放在同一个区域中，因为它们之间的通信量很大”，而一个`preferredDuringSchedulingIgnoredDuringExecution`的例子是“将此服务中的pods跨区域分布”（如果是硬性要求则没有意义，因为您的pods可能比zones还多）。

在PodSpec中，Inter-pod affinity在affinity字段的podAffinity中定义。在PodSpec中，Pod间反亲和力在affinity字段的podAntiAffinity中定义。

#### An example of a pod that uses pod affinity: 

pods/pod-with-pod-affinity.yaml
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: with-pod-affinity
spec:
  affinity:
    podAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
      - labelSelector:
          matchExpressions:
          - key: security
            operator: In
            values:
            - S1
        topologyKey: topology.kubernetes.io/zone
    podAntiAffinity:
      preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 100
        podAffinityTerm:
          labelSelector:
            matchExpressions:
            - key: security
              operator: In
              values:
              - S2
          topologyKey: topology.kubernetes.io/zone
  containers:
  - name: with-pod-affinity
    image: k8s.gcr.io/pause:2.0
```

此pod上定义了一个pod亲和性规则和一个pod反亲和性规则。在本例中，podAffinity是`requiredDuringSchedulingIgnoredDuringExecution`，而podAntiAffinity是`preferredDuringSchedulingIgnoredDuringExecution`。pod亲和性规则表示，只有节点在同一个zone, 并且至少有一个拥有key为"security"，值为"S1"的pod已经在这个zone中运行时，Pod才能调度到这个节点上（更准确地说，如果节点N有key为`topology.kubernetes.io/zone`，值为V，并且集群中至少有一个节点拥有key `topology.kubernetes.io/zone`和值`V`，并且这个节点上运行了一个key为"security"，值为"S1"的标签的Pod，那么Pod就可以被调度到节点N上）。pod anti-affinity规则指出，如果某个节点与标签是key “security”和值“S2”的pod位于同一区域，则无法将pod调度到该节点上。有关pod亲和力和反亲和力的更多示例，请参阅[设计文档](https://git.k8s.io/community/contributors/design-proposals/scheduling/podaffinity.md)，包括`requiredDuringSchedulingIgnoredDuringExecution`和`preferredDuringSchedulingIgnoredDuringExecution`。

pod亲和力和反亲和力的有效operator是`In`，`NotIn`，`Exists`，`DoesNotExist`。

原则上，topologyKey可以是任何合法的label-key。但是，出于性能和安全原因，拓扑键有一些限制：

1. 对于pod亲和力，requiredDuringSchedulingIgnoredDuringExecution和preferredDuringSchedulingIgnoredDuringExecution中都不允许空拓扑键。
2. 对于pod反亲和力，requiredDuringSchedulingIgnoredDuringExecution和preferredDuringSchedulingIgnoredDuringExecution中也不允许空拓扑键。
3. 对于requiredDuringSchedulingIgnoredDuringExecution类型的pod反亲和力，引入了admission controller `LimitPodHardAntiAffinityTopology` 来限`topologyKey`为`kubernetes.io/hostname`. 如果要使其对自定义拓扑有效，可以修改admission controller，或干脆将其禁用。
4. 除上述情况外，topologyKey可以是任何合法的label-key。

除了labelSelector和topologyKey，可选地您还可以指定一个`namespaces`列表（与labelSelector和topologyKey的定义在同一级别）。如果省略或为空，则默认为亲和力/反亲和力定义出现的pod的命名空间。

所有和`requiredDuringSchedulingIgnoredDuringExecution`亲和力和反亲和力相关的`matchExpressions`满足时，才能将pod调度到节点上。

#### More Practical Use-cases 

当Interpod Affinity和AntiAffinity与更高级别的集合（如ReplicaSets, StatefulSets, Deployments等）一起使用时，它们可能更有用。可以轻松配置一组工作负载应位于同一拓扑中，例如，同一节点中。

##### Always co-located in the same node

在三个节点的集群中，web应用程序具有内存缓存，如redis。我们希望web服务器尽可能与缓存位于同一位置。

下面是一个简单的redis部署的yaml片段，有三个副本和选择器标签`app=store`。 Deployment有一个`PodAntiAffinity`配置来保证副本不会调度到同一个节点上。

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis-cache
spec:
  selector:
    matchLabels:
      app: store
  replicas: 3
  template:
    metadata:
      labels:
        app: store
    spec:
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
          - labelSelector:
              matchExpressions:
              - key: app
                operator: In
                values:
                - store
            topologyKey: "kubernetes.io/hostname"
      containers:
      - name: redis-server
        image: redis:3.2-alpine
```

下面的web服务器部署的yaml片段配置了podAntiAffinity和podAffinity。这将通知调度程序，它的所有副本将与具有选择器标签`app=store`的pod位于同一位置。这还将确保每个web服务器副本不会调度到同一个节点上。

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: web-server
spec:
  selector:
    matchLabels:
      app: web-store
  replicas: 3
  template:
    metadata:
      labels:
        app: web-store
    spec:
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
          - labelSelector:
              matchExpressions:
              - key: app
                operator: In
                values:
                - web-store
            topologyKey: "kubernetes.io/hostname"
        podAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
          - labelSelector:
              matchExpressions:
              - key: app
                operator: In
                values:
                - store
            topologyKey: "kubernetes.io/hostname"
      containers:
      - name: web-app
        image: nginx:1.16-alpine
```

如果我们创建以上两个部署，我们的三个节点的集群应该如下所示。

node-1 | node-2 | node-3
-------|--------|-------
webserver-1 | webserver-2 | webserver-3
cache-1 | cache-2 | cache-3

如您所见，web服务器的所有3个副本都自动与缓存一起定位。

```sh
kubectl get pods -o wide
```

输出与此类似：
```
NAME                           READY     STATUS    RESTARTS   AGE       IP           NODE
redis-cache-1450370735-6dzlj   1/1       Running   0          8m        10.192.4.2   kube-node-3
redis-cache-1450370735-j2j96   1/1       Running   0          8m        10.192.2.2   kube-node-1
redis-cache-1450370735-z73mh   1/1       Running   0          8m        10.192.3.1   kube-node-2
web-server-1287567482-5d4dz    1/1       Running   0          7m        10.192.2.3   kube-node-1
web-server-1287567482-6f7v5    1/1       Running   0          7m        10.192.4.3   kube-node-3
web-server-1287567482-s330j    1/1       Running   0          7m        10.192.3.2   kube-node-2
```

##### Never co-located in the same node

上面的示例一起使用PodAntiAffinity规则和topologyKey：`kubernetes.io/hostname`来部署redis集群，这样没有两个实例可以位于同一主机上。请参阅[ZooKeeper教程](https://kubernetes.io/docs/tutorials/stateful-application/zookeeper/#tolerating-node-failure)，以获取使用相同技术配置高可用性的具有反亲和力的StatefulSet的示例。


## nodeName 

nodeName是节点选择约束的最简单形式，但由于其局限性，通常不使用它。nodeName是PodSpec中的一个字段。如果不是空的，调度程序会忽略这个pod，而在指定name的节点上运行的kubelet则会尝试运行这个pod。因此，如果在PodSpec中提供了nodeName，在节点选择时会优先于上面的方法。

使用nodeName选择节点的一些限制是：
- 如果对应name的节点不存在，pod将不会运行，在某些情况下可能会自动删除。
- 如果对应name的节点没有资源来容纳pod，那么pod将失败，其原因将指明原因，例如OutOfmemory或OutOfcpu。
- 云环境中的节点名并不总是可预测或稳定的。

下面是一个使用nodeName字段的pod配置文件示例：

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: nginx
spec:
  containers:
  - name: nginx
    image: nginx
  nodeName: kube-01
```

上述Pod将在kube-01节点上运行。

## 下一步是什么

[污点](https://kubernetes.io/docs/concepts/scheduling-eviction/taint-and-toleration/)允许一个节点排斥一组Pods。

[节点亲和力](https://git.k8s.io/community/contributors/design-proposals/scheduling/nodeaffinity.md)和[Pod间亲和力/反亲和力](https://git.k8s.io/community/contributors/design-proposals/scheduling/podaffinity.md)的设计文档包含有关这些特性的额外背景信息。

一旦一个Pod被分配给一个节点，kubelet运行这个Pod并分配节点本地资源。[拓扑管理器](https://kubernetes.io/docs/tasks/administer-cluster/topology-manager/)可以参与节点级资源分配决策。
