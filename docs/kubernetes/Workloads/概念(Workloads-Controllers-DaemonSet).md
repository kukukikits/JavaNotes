

# 本文描述所有Controllers

---

# DaemonSet

DaemonSet确保所有（或部分）节点运行一个Pod的copy。当节点被添加到集群时，pod也被添加到集群中。当节点从集群中移除时，这些pod将被垃圾回收。删除DaemonSet将清理它创建的Pods。

DaemonSet的一些典型用法是：

- 在每个节点上运行群集存储守护程序
- 在每个节点上运行日志收集守护程序
- 在每个节点上运行节点监视守护程序

在一个简单的例子中，一个覆盖所有节点的DaemonSet将用于每种类型的守护进程。更复杂的设置可能会对单一类型的守护程序, 但是不同的flags，不同硬件类型的不同的内存和cpu请求使用多个DaemonSets。

## Writing a DaemonSet Spec
### Create a DaemonSet 
您可以在YAML文件中描述DaemonSet。例如daemonset.yaml下面的文件描述了运行fluentd-elasticsearch Docker映像的守护程序集：

controllers/daemonset.yaml 

```yaml
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: fluentd-elasticsearch
  namespace: kube-system
  labels:
    k8s-app: fluentd-logging
spec:
  selector:
    matchLabels:
      name: fluentd-elasticsearch
  template:
    metadata:
      labels:
        name: fluentd-elasticsearch
    spec:
      tolerations:
      # this toleration is to have the daemonset runnable on master nodes
      # remove it if your masters can't run pods
      - key: node-role.kubernetes.io/master
        effect: NoSchedule
      containers:
      - name: fluentd-elasticsearch
        image: quay.io/fluentd_elasticsearch/fluentd:v2.5.2
        resources:
          limits:
            memory: 200Mi
          requests:
            cpu: 100m
            memory: 200Mi
        volumeMounts:
        - name: varlog
          mountPath: /var/log
        - name: varlibdockercontainers
          mountPath: /var/lib/docker/containers
          readOnly: true
      terminationGracePeriodSeconds: 30
      volumes:
      - name: varlog
        hostPath:
          path: /var/log
      - name: varlibdockercontainers
        hostPath:
          path: /var/lib/docker/containers
```

Create a DaemonSet based on the YAML file:
```shell
kubectl apply -f https://k8s.io/examples/controllers/daemonset.yaml
```

### Required Fields 
与所有其他Kubernetes配置一样，DaemonSet需要apiVersion、kind和metadata字段。有关使用配置文件的一般信息，请参阅使用kubectl文档[运行无状态应用程序](https://kubernetes.io/docs/tasks/run-application/run-stateless-application-deployment/)、[配置容器](https://kubernetes.io/docs/tasks/)和[对象管理](https://kubernetes.io/docs/concepts/overview/working-with-objects/object-management/)。

DaemonSet对象的名称必须是有效的DNS子域名。

DaemonSet还需要.spec部分。

### Pod Template 
`.spec.template`是.spec中的必需字段之一。

`.spec.template`是pod template。它的模式与Pod完全相同，只是它是嵌套的，没有apiVersion或kind。

除了Pod的必需字段外，DaemonSet中的Pod template还必须指定适当的标签（请参见[Pod选择器](https://kubernetes.io/docs/concepts/workloads/controllers/daemonset/#pod-selector)）。

DaemonSet中的Pod模板的RestartPolicy必须等于Always，或者未指定，默认为Always

### Pod Selector 

`.spec.selector`字段是pod选择器。它的工作原理pod的`.spec.selector`完全相同。

从kubernetes 1.8开始，必须指定一个与`.spec.template`指定label相匹配的pod selector. 当留空时，pod选择器将不再有默认。选择器默认设置与kubectl apply不兼容。而且，一旦创建了DaemonSet，它的`.spec.selector`就不能修改。改变pod选择器可能会导致无意中孤立Pods，这会让用户感到困惑。

`.spec.selector`是由两个字段组成的对象：

- `matchLabels` — 工作原理与[ReplicationController](https://kubernetes.io/docs/concepts/workloads/controllers/replicationcontroller/)的.spec.selector相同。
- `matchExpressions` — 允许通过指定键、值列表和与键和值相关的运算符来构建更复杂的选择器。
  
如果上面的两个都指定了，那么最终结果是And连接的。

如果`.spec.selector`指定，它必须与`.spec.template.metadata.labels`相匹配，否则会被API拒绝。

另外，通常不应该直接通过另一个DaemonSet, 或者另一个workload resource（如ReplicaSet）来创建任何标签与此选择器匹配的pod，否则DaemonSet控制器将认为这些pod是由它创建的。Kubernetes不会阻止你这么做的。您可能希望这样做的一种情况是在节点上手动创建一个具有不同值的Pod以进行测试。

### Running Pods on select Nodes
如果指定一个`.spec.template.spec.nodeSelector`，则DaemonSet controller将在与该节点选择器匹配的节点上创建pod。同样，如果指定一个`.spec.template.spec.affinity`，则DaemonSet controller将在与该节点 [node affinity](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/) 匹配的节点上创建pod。如果不指定任何一个，那么DaemonSet controller将在所有节点上创建pod

## How Daemon Pods are scheduled 
### Scheduled by default scheduler
*FEATURE STATE: Kubernetes v1.19 [stable]*

DaemonSet确保所有符合条件的节点都运行一个Pod的副本。通常，Pod运行的节点由Kubernetes调度器选择。但是，DaemonSet pod是由DaemonSet Controller创建和调度的。这带来了以下问题：
- 不一致的Pod行为：一般的Pod被创建并等待调度的时候是处于Pending状态，但是  DaemonSet Pod不是在Pending状态下创建的。这让用户感到困惑。
- [Pod抢占（Pod preemption ）](https://kubernetes.io/docs/concepts/configuration/pod-priority-preemption/)由默认调度程序处理。当启用抢占时，DaemonSet controller将在不考虑pod优先级和抢占的情况下做出调度决策。

ScheduleDaemonSetPods允许您使用默认 scheduler 而不是DaemonSet controller来调度DaemonSets，方法是将`NodeAffinity`术语(不是`.spec.nodeName`)添加到DaemonSet pods。然后默认scheduler将pod绑定到目标主机。如果DaemonSet pod的节点关联性node affinity已经存在，则会将其替换（在选择目标主机之前已考虑到原始节点关联性）。DaemonSet controller仅在创建或修改DaemonSet pod时执行这些操作，不会对DaemonSet的spec.template进行修改。

```yaml
nodeAffinity:
  requiredDuringSchedulingIgnoredDuringExecution:
    nodeSelectorTerms:
    - matchFields:
      - key: metadata.name
        operator: In
        values:
        - target-host-name
```

此外，`node.kubernetes.io/unschedulable:NoSchedule` toleration会自动添加到DaemonSet Pods。在调度DaemonSet pod时，默认scheduler 忽略不可调度的节点。

### Taints and Tolerations

尽管Daemon Pods遵循[污染和容忍（taints and tolerations）](https://kubernetes.io/docs/concepts/scheduling-eviction/taint-and-toleration/)，但是以下容忍会根据相关特性自动添加到守护进程Pods中。

Toleration Key | Effect | Version | Description
---------|----------|---------|---------
node.kubernetes.io/not-ready | NoExecute | 1.13+ | DaemonSet pods will not be evicted when there are node problems such as a network partition.
 node.kubernetes.io/unreachable | NoExecute | 1.13+ | DaemonSet pods will not be evicted when there are node problems such as a network partition.
 node.kubernetes.io/disk-pressure | NoSchedule | 1.8+ | 
 node.kubernetes.io/memory-pressure | NoSchedule | 1.8+	 |
 node.kubernetes.io/unschedulable |	NoSchedule |	1.12+	| DaemonSet pods tolerate unschedulable attributes by default scheduler.
 node.kubernetes.io/network-unavailable	| NoSchedule	| 1.12+	| DaemonSet pods, who uses host network, tolerate network-unavailable attributes by default scheduler.


## Communicating with Daemon Pods 

在DaemonSet中与pod通信的一些可能模式是：

- Push：DaemonSet中的pod被配置为向另一个服务（如stats数据库）发送更新。他们没有客户。
- NodeIP和已知Port：DaemonSet中的Pods可以使用hostPort，这样就可以通过节点IPs访问Pods。客户机以某种方式知道节点IP的列表，并且根据约定知道端口。
- DNS：使用相同的pod选择器创建一个headless服务，然后使用endpoints资源发现DaemonSets或从DNS检索多个a记录。
- Service：使用相同的Pod选择器创建一个服务，并使用该服务访问随机节点上的daemon。（无法到达特殊节点）

## Updating a DaemonSet 
如果更改了节点标签，DaemonSet将立即向新匹配的节点添加pod，并从新不匹配的节点中删除pod。

您可以修改DaemonSet创建的pod。但是，不是所有字段都允许更新。此外，DaemonSet控制器将在下次创建节点（即使具有相同名称）时使用原始模板。

您可以删除DaemonSet。如果使用kubectl指定--cascade=false，那么Pods将留在节点上。如果随后使用相同的选择器创建新的DaemonSet，则新DaemonSet将继续使用已存在的pod。如果有任何Pods需要替换，DaemonSet将根据其updateStrategy替换它们。

您可以对DaemonSet执行[滚动更新](https://kubernetes.io/docs/tasks/manage-daemon/update-daemon-set/)。


## Alternatives to DaemonSet 守护进程的替代方案
### Init scripts

当然可以通过在节点上直接启动守护进程来运行它们（例如使用init、upstarted或systemd）。这很好的方法。但是，通过DaemonSet运行这些进程有几个优点：

- 能够以与应用程序相同的方式监视和管理守护程序的日志。
- 为守护程序和应用程序提供相同的配置语言和工具（例如Pod模板、kubectl）。
- 在具有资源限制的容器中运行守护程序会增加守护程序与应用程序容器之间的隔离。但是，也可以通过在容器中而不是在Pod中运行守护进程来实现（例如，通过Docker直接启动）。

### Bare Pods 
可以直接创建指定要运行的特定节点的pod。但是，DaemonSet会替换由于任何原因被删除或终止的pod，例如在节点故障或中断性节点维护（如内核升级）的情况下。因此，您应该使用DaemonSet，而不是创建单独的pod

### Static Pods
可以通过将文件写入Kubelet监视的某个目录来创建pod。这些被称为[静态pod](https://kubernetes.io/docs/tasks/configure-pod-container/static-pod/)。与守护进程不同，静态Pods不能用kubectl或其他kubernetesapi客户机管理。静态Pods不依赖于apiserver，这使得它们在集群引导情况下非常有用。此外，将来可能不推荐使用静态Pods

### Deployments 
DaemonSets与部署类似，因为它们都创建pod，并且这些pod具有不希望终止的进程（例如web服务器、存储服务器）。

为无状态服务（如前端）使用部署，在这种情况下，增加和减少副本数量和推出更新比精确控制Pod在哪个主机上运行更为重要。当一个Pod的副本必须始终在所有或某些主机上运行，并且需要在其他Pod之前启动时，请使用DaemonSet。

