# Managing Resources for Containers

当您指定一个Pod时，您可以选择指定一个容器需要多少资源。要指定的最常见的资源是CPU和内存（RAM）；还有其他资源。

当您为Pod中的容器指定资源请求时，调度程序使用此信息来决定将Pod放置在哪个节点上。当您为容器指定资源限制时，kubelet将强制执行这些限制，使运行中的容器使用的资源不超过您设置的资源限制。 The kubelet also reserves at least the request amount of that system resource specifically for that container to use。但kubelet至少会保留容器请求的系统资源量，以供容器使用。

## Requests and limits 

如果运行Pod的节点有足够的可用资源，那么容器可能（并且允许）使用比请求的指定资源更多的资源。但是，不允许容器使用超过其限制的资源。

例如，如果您为一个容器设置了256MiB的内存请求，并且该容器位于一个调度到具有8GiB内存且没有其他Pods的节点的Pod中，那么容器可以尝试使用更多的RAM。

如果为该容器设置了4GiB的内存限制，kubelet（和容器运行时）将强制执行该限制。运行时防止容器使用超过配置的资源限制。例如：当容器中的一个进程试图消耗超过允许的内存量时，系统内核会终止尝试分配的进程，并出现内存不足（OOM）错误。

限制可以是反应性地实现（系统一旦发现违规就干预）或强制执行（系统防止容器超出限制）。不同的运行时可以有不同的方法来实现相同的限制。

> :airplane: 注意：如果一个容器指定了它自己的内存限制，但是没有指定内存请求，Kubernetes会自动分配一个与该限制相匹配的内存请求。类似地，如果一个容器指定了它自己的CPU限制，但是没有指定CPU请求，Kubernetes会自动分配一个与该限制相匹配的CPU请求

## Resource types 

CPU和内存都是一种资源类型。资源类型有一个基本单位。CPU表示计算处理，以[Kubernetes CPUs](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/#meaning-of-cpu)为单位指定。内存是以字节为单位指定的。如果您使用的是kubernetes v1.14或更新版本，您可以指定huge page资源。Huge pages是Linux特有的特性，其中节点内核分配比默认page size大得多的内存块。

例如，在默认page size为4KiB的系统上，可以指定一个限制，`hugepages-2Mi: 80Mi`。如果容器尝试分配超过40个2MiB的huge pages（总共80 MiB），那么分配将失败。

> :art: 注意：你不能过度使用`hugepages-*`资源。这与内存和cpu资源不同。

CPU和内存统称为计算资源，或者仅仅是资源。计算资源是可以请求、分配和消耗的可测量数量。它们不同于[API资源](https://kubernetes.io/docs/concepts/overview/kubernetes-api/)。API资源，如pod和[Services](https://kubernetes.io/docs/concepts/services-networking/service/)是可以通过kubernetes api服务器读取和修改的对象

## Resource requests and limits of Pod and Container
Pod的每个容器可以指定以下一个或多个：

* `spec.containers[].resources.limits.cpu`
* `spec.containers[].resources.limits.memory`
* `spec.containers[].resources.limits.hugepages-<size>`
* `spec.containers[].resources.requests.cpu`
* `spec.containers[].resources.requests.memory`
* `spec.containers[].resources.requests.hugepages-<size>`

虽然请求和限制只能在单个容器上指定，但是讨论Pod资源请求和限制是很方便的。特定资源类型的Pod资源请求/限制是Pod中每个容器对该类型资源请求/限制的总和。

## Resource units in Kubernetes

### Meaning of CPU

对CPU资源的限制和请求是以CPU为单位度量的。在Kubernetes，一个cpu相当于云提供商的1个vCPU/Core和裸金属Intel处理器上的1个超线程(hyperthread)。

允许小数的请求。一个`spec.containers[].resources.requests.cpu`为0.5的容器，相当于请求1个CPU的一半。0.1则相当于表达式100m，可以读作“100毫秒CPU(millicpu)”。有人说“一百毫核(millicores)”，可以认为这俩是同样的。API将小数点为0.1的请求转换为100m，精度不允许小于1m。因此，100m的格式可能是首选。

CPU总是作为绝对数量而不是相对数量来请求；在单核、双核或48核机器上，0.1描述相同的CPU数量

### Meaning of memory 

内存的限制和请求以字节为单位。你可以用一个普通整数或一个定点数来表示内存，并使用这些后缀：E，P，T，G，M，K。你也可以使用2的整数幂表示的：Ei，Pi，Ti，Gi，Mi，Ki。例如，下面的值大致相同：
```
128974848, 129e6, 129M, 123Mi
```

这里有一个例子。下面的Pod有两个容器。每个容器有0.25cpu和64MiB（$2^{26}$字节）的内存请求。每个容器有0.5cpu和128MiB内存的限制。你可以说Pod有0.5cpu和128mib内存的请求，以及一个cpu和256MiB内存的限制。

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: frontend
spec:
  containers:
  - name: app
    image: images.my-company.example/app:v4
    resources:
      requests:
        memory: "64Mi"
        cpu: "250m"
      limits:
        memory: "128Mi"
        cpu: "500m"
  - name: log-aggregator
    image: images.my-company.example/log-aggregator:v6
    resources:
      requests:
        memory: "64Mi"
        cpu: "250m"
      limits:
        memory: "128Mi"
        cpu: "500m"
```

## How Pods with resource requests are scheduled 

当您创建一个Pod时，Kubernetes调度器会选择一个节点来运行Pod。每个节点对于每种资源类型都有一个最大容量：它可以为pod提供的CPU和内存总量。调度程序确保，对于每种资源类型，调度容器的资源请求之和小于节点的容量。注意，尽管节点上的实际内存或CPU资源使用率非常低，但是如果容量检查失败，调度程序仍然拒绝在节点上放置Pod。这可以防止在以后资源使用量增加时（例如，在每日请求率峰值期间）节点上出现资源短缺的情况。

## How Pods with resource limits are run

当kubelet启动一个Pod的容器时，它将CPU和内存限制传递给容器运行时。

使用Docker时：
- `spec.containers[].resources.requests.cpu`转换为它的核心数，它可能是小数，然后乘以1024。在docker run命令中，这个数字或2中的较大者作为[--cpu-shares](https://docs.docker.com/engine/reference/run/#cpu-share-constraint)标志的值。
- `spec.containers[].resources.limits.cpu`转换为其millicore值并乘以100。结果值是一个容器每100毫秒可以使用的CPU时间的总量。在这个间隔期间，容器使用的CPU时间不能超过它的份额。
  > :articulated_lorry: 注意：默认的配额周期为100ms，CPU配额的最小分辨率为1ms。
- `spec.containers[].resources.limits.memory`转换为整数，并用作docker run命令中[--memory](https://docs.docker.com/engine/reference/run/#/user-memory-constraints)标志的值。

如果容器超出其内存限制，则可能会终止它。如果它是可重启的，kubelet将重新启动它，就像任何其他类型的运行时failure一样。

如果一个容器超过了它的内存请求，它的Pod很可能会在节点内存用完时被逐出。

容器可能被允许或不允许在较长时间内超过其CPU限制。但是，它不会因为CPU使用过多而被杀死。

要确定容器是否因资源限制而无法调度或正在被终止，请参阅“[疑难解答](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/#troubleshooting)”部分。

### Monitoring compute & memory resource usage 
Pod的资源使用情况报告为Pod状态的一部分。

如果集群中有可选的[监视工具](https://kubernetes.io/docs/tasks/debug-application-cluster/resource-usage-monitoring/)，那么可以直接从[metrics api](https://kubernetes.io/docs/tasks/debug-application-cluster/resource-metrics-pipeline/#the-metrics-api)或从监视工具中检索Pod资源使用情况。

## Local ephemeral storage 本地临时存储
*FEATURE STATE: Kubernetes v1.10 [beta]*

节点具有本地临时存储，由本地连接的可写设备或有时由RAM支持。“临时”意味着不保证长时间存储。

pod使用临时本地存储来存储暂存空间、缓存和日志。kubelet可以使用本地临时存储为pod提供暂存空间，以便将[emptyDir](https://kubernetes.io/docs/concepts/storage/volumes/#emptydir)卷装载到容器中。

kubelet还使用这种存储来保存节点级容器日志、容器镜像和运行容器的可写层。

> :balance_scale: 注意：如果一个节点发生故障，其临时存储中的数据可能会丢失。您的应用程序不能期望本地临时存储有任何性能SLAs（例如磁盘IOPS）。

作为一个beta特性，Kubernetes允许您跟踪、保留和限制Pod可以消耗的临时本地存储。

### Configurations for local ephemeral storage 

Kubernetes支持两种在节点上配置本地临时存储的方法：

1. Single filesystem
  
    >    在这种配置中，您可以将所有不同类型的临时本地数据（emptyDir卷、可写层、容器映像、日志）放入一个文件系统中。配置kubelet的最有效方法是将这个文件系统专用于Kubernetes (kubelet) 数据。

    >    kubelet还编写[节点级容器日志](https://kubernetes.io/docs/concepts/cluster-administration/logging/#logging-at-the-node-level)，并将这些日志类似于临时本地存储进行处理。

    >    kubelet将日志写入其配置的日志目录（默认为/var/log）中的文件，并为其他本地存储的数据提供一个基本目录（默认为/var/lib/kubelet）。

    >    通常，/var/lib/kubelet和/var/log都位于系统根文件系统上，kubelet的设计考虑了这种布局。

2. Two filesystems
    > 在节点上有一个文件系统，用于处理运行Pods时产生的临时数据：日志和emptyDir卷。您可以将此文件系统用于其他数据（例如：与Kubernetes无关的系统日志）；它甚至可以是根文件系统。

    >  kubelet还将[节点级容器日志](https://kubernetes.io/docs/concepts/cluster-administration/logging/#logging-at-the-node-level)写入第一个文件系统，并将这些日志类似于临时本地存储进行处理。

    > 您还可以使用单独的文件系统，由不同的逻辑存储设备支持。在这个配置中，告诉kubelet放置容器镜像层和可写层的目录位于第二个文件系统上。

    > 第一个文件系统不包含任何镜像层或可写层。


您的节点可以有任意多个不用于Kubernetes的其他文件系统。

kubelet可以计算它使用了多少本地存储。但是前提是：
- LocalStorageCapacityIsolation特型门已启用（该功能在默认情况下处于打开状态），并且
- 您已经使用支持的本地临时存储配置设置了节点。

如果您有不同的配置，那么kubelet不会对临时本地存储设置资源限制。

> :zap: 注意：kubelet跟踪tmpfs emptyDir卷作为容器内存使用，而不是作为本地临时存储

### Setting requests and limits for local ephemeral storage 

您可以使用ephemeral-storage来管理本地临时存储。Pod的每个容器可以指定以下一个或多个：
* spec.containers[].resources.limits.ephemeral-storage
* spec.containers[].resources.requests.ephemeral-storage

ephemeral-storage的限制和请求以字节为单位。你可以用一个普通整数或一个定点数来表示存储量：E，P，T，G，M，K。你还可以使用2的整数幂的表示：Ei，Pi，Ti，Gi，Mi，Ki。例如，下面的值大致相同：
```
128974848, 129e6, 129M, 123Mi
```

在下面的示例中，Pod有两个容器。每个容器都有2GiB的本地临时存储请求。每个容器的本地临时存储量限制为4GiB。因此，Pod的本地临时存储请求为4GiB，本地临时存储的限制为8GiB。

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: frontend
spec:
  containers:
  - name: app
    image: images.my-company.example/app:v4
    resources:
      requests:
        ephemeral-storage: "2Gi"
      limits:
        ephemeral-storage: "4Gi"
  - name: log-aggregator
    image: images.my-company.example/log-aggregator:v6
    resources:
      requests:
        ephemeral-storage: "2Gi"
      limits:
        ephemeral-storage: "4Gi"
```

### How Pods with ephemeral-storage requests are scheduled 

当您创建一个Pod时，Kubernetes调度器会选择一个节点来运行Pod。每个节点都有一个可以为pod提供的最大本地临时存储量。有关详细信息，请参见[节点可分配](https://kubernetes.io/docs/tasks/administer-cluster/reserve-compute-resources/#node-allocatable)。

调度程序确保调度容器的资源请求之和小于节点的容量。

### Ephemeral storage consumption management 

如果kubelet将本地临时存储作为一种资源进行管理，那么kubelet将测量下面几种情况中存储的使用情况：
- emptyDir卷，tmpfs emptyDir卷除外
- 保存节点级日志的目录
- 可写容器层

如果一个Pod使用的临时存储空间超过了您允许的范围，kubelet会设置一个驱逐信号来触发Pod驱逐。

对于容器级别的隔离，如果容器的可写层和日志使用量超过其存储限制，kubelet会将Pod标记然后逐出。

对于Pod级别的隔离，kubelet通过计算出Pod中容器的限制来计算出一个总的Pod存储限制。在这种情况下，如果来自所有容器的本地临时存储使用量和Pod的emptyDir卷的总和超过了Pod的总体存储限制，那么kubelet也会将Pod标记为逐出。

> :zap: 注意事项：
> 如果kubelet没有测量本地临时存储，那么超过本地存储限制的Pod不会因为违反本地存储资源限制而被逐出。
> 
> 但是，如果可写容器层、节点级日志或emptyDir卷的文件系统空间不足，则节点会因本地存储不足而受到污染，并且此污染将使那些不能容忍该污染的pod逐出。
>
> 请参阅支持的临时本地存储[配置](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/#configurations-for-local-ephemeral-storage)。

kubelet支持不同的测量Pod存储的方法：
1. Periodic scanning 周期性扫描
    kubelet执行定期的调度检查，扫描每个emptyDir卷、容器日志目录和可写容器层。 
    该扫描测量使用了多少空间。
    > :zap: 注：
    > 在这种模式下，kubelet不会跟踪已删除文件的打开文件描述符(open file descriptors)。
    > 
    > 如果您（或容器）在emptyDir卷中创建了一个文件，某个程序打开了该文件，然后在该文件仍处于打开状态时将其删除，则删除文件的索引节点(inode)将一直保留，直到您关闭该文件，但kubelet不会将该空间归类为正在使用中。
2. Filesystem project quota 文件系统项目配额
    *FEATURE STATE: Kubernetes v1.15 [alpha]*
    项目配额是用于管理文件系统上的存储使用的操作系统级功能。使用Kubernetes，您可以启用项目配额来监视存储使用情况。确保节点上支持emptyDir卷的文件系统提供项目配额支持。例如，XFS和ext4fs提供项目配额。
    > :ballot_box: 注意：项目配额允许您监视存储使用情况；它们不强制限制使用。

    Kubernetes使用从1048576开始的项目ID。正在使用的id注册在/etc/projects和/etc/projid中。如果此范围内的项目ID用于系统上的其他用途，则必须在/etc/projects和/etc/projid中注册这些项目ID，以便Kubernetes不使用它们。

    配额比目录扫描更快、更准确。当一个目录被分配给一个项目时，在一个目录下创建的所有文件都将在该项目中创建，内核只需跟踪该项目中的文件使用了多少块。

    如果一个文件被创建和删除，但是有一个打开的文件描述符(open file descriptor)，它将继续消耗空间。配额跟踪会准确地记录这个使用的空间，而目录扫描会忽略已删除文件使用的存储。

    如果要使用项目配额，则应：
    - 在kubelet配置中启用`LocalStorageCapacityIsolationFSQuotaMonitoring=true`特性门。
    - 确保根文件系统（或可选的运行时文件系统）启用了项目配额。所有XFS文件系统都支持项目配额。对于ext4文件系统，您需要在文件系统未装入时启用项目配额跟踪功能。
        ```
        # For ext4, with /dev/block-device not mounted
        sudo tune2fs -O project -Q prjquota /dev/block-device
        ```
    - 确保根文件系统（或可选的运行时文件系统）是在启用项目配额的情况下装入的。对于XFS和ext4fs，mount选项名为prjquota。


## Extended resources

扩展资源是`kubernetes.io`域以外的全限定资源的统称。non-Kubernetes-built-in资源允许集群操作员advertise(公布？)，允许用户消费。

使用扩展资源需要两个步骤。首先，集群操作员必须公布扩展资源。第二，用户必须在Pods中请求扩展资源。

### Managing extended resources
#### 1. Node-level extended resources 

节点级扩展资源绑定到节点。

- Device plugin managed resources 

    有关如何在每个节点上公布设备插件管理的资源，请参阅[设备插件](https://kubernetes.io/docs/concepts/extend-kubernetes/compute-storage-net/device-plugins/)。

- Other resources

    要公布新的节点级扩展资源，群集操作员可以向API服务器提交PATCH HTTP请求，并在`status.capacity`中给集群节点指定可用的数量。此操作之后，节点的`status.capacity`将包含新资源。`status.allocatable`字段由kubelet用新资源异步地自动更新。请注意，因为调度程序使用节点`status.allocatable`评估Pod fitness适应度，在使用新资源修补节点容量和第一个请求这个资源的Pod调度之间可能会有短暂的延迟。

**Example**：
下面是一个示例，展示了如何使用curl，在主节点是`k8s-master`的`k8s-node-1`节点上，公布5个"example.com/foo"资源。

```sh
curl --header "Content-Type: application/json-patch+json" \
--request PATCH \
--data '[{"op": "add", "path": "/status/capacity/example.com~1foo", "value": "5"}]' \
http://k8s-master:8080/api/v1/nodes/k8s-node-1/status
```

> :anchor: 注意：在前面的请求中，`~1`是patch路径中字符`/`的编码。JSON-Patch中的operation path值被解释为JSON-Pointer。有关更多详细信息，请参阅[IETF RFC 6901，第3节](https://tools.ietf.org/html/rfc6901#section-3)。


#### 2. Cluster-level extended resources 

群集级扩展资源不绑定到节点。它们通常由调度程序扩展器管理，后者用于处理资源消耗和资源配额。

您可以在[调度策略配置](https://github.com/kubernetes/kubernetes/blob/release-1.10/pkg/scheduler/api/v1/types.go#L31)中指定由调度扩展程序处理的扩展资源。

**Example：**

调度程序策略的以下配置指示群集级别的扩展资源`example.com/foo`由调度程序扩展器处理。
- 仅当Pod请求`example.com/foo`时，调度器才会发送Pod到调度扩展器
- ignoredByScheduler字段指示调度程序，在`PodFitsResources` 断言时，不检查`example.com/foo`资源。

```json
{
  "kind": "Policy",
  "apiVersion": "v1",
  "extenders": [
    {
      "urlPrefix":"<extender-endpoint>",
      "bindVerb": "bind",
      "managedResources": [
        {
          "name": "example.com/foo",
          "ignoredByScheduler": true
        }
      ]
    }
  ]
}
```

### Consuming extended resources
 
用户可以像CPU和内存一样在Pod specs中使用扩展资源。调度程序负责计算资源，这样就不会同时向pod分配超过可用数量的资源。

API服务器将扩展资源的数量限制为整数。有效数量的例子是3, 3000m and 3Ki。无效数量的例子有0.5 和 1500m.

> :kaaba: 注意：扩展资源替换Opaque Integer Resources。用户可以使用除了保留的` kubernetes.io`以外的任何域名前缀

要在Pod中使用扩展资源，请将资源的name作为key在容器spec的`spec.containers[].resources.limits`中进行定义。

> :kick_scooter: 注意：扩展资源不能过度提交overcommitted，因此如果容器规范中同时存在请求和限制，则两者必须相等。

只有当所有资源请求（包括CPU、内存和任何扩展资源）都得到满足时，才调度Pod。只要无法满足资源请求，Pod就会保持挂起`PENDING`状态。

**Example:**
下面的Pod需要2个CPU和1个"example.com/foo"（扩展资源）。

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: my-pod
spec:
  containers:
  - name: my-container
    image: myimage
    resources:
      requests:
        cpu: 2
        example.com/foo: 1
      limits:
        example.com/foo: 1
```


## Troubleshooting 

### My Pods are pending with event message failedScheduling 

如果调度程序找不到任何可以容纳Pod的节点，那么Pod将保持未调度状态，直到找到位置为止。每次调度器找不到位置给pod时，都会生成一个事件，如下所示：

```sh
kubectl describe pod frontend | grep -A 3 Events
```
```
Events:
  FirstSeen LastSeen   Count  From          Subobject   PathReason      Message
  36s         5s         6    {scheduler }              FailedScheduling  Failed for reason PodExceedsFreeCPU and possibly others
```

在上面的示例中，由于节点上的CPU资源不足，无法调度名为“frontend”的Pod。类似的错误消息也可能提示由于内存不足而导致的故障（PodExceedsFreeMemory）。一般情况下，如果一个Pod因为这种类型的message而挂起，有几种方法可以尝试：
- 向群集添加更多节点。
- 终止不需要的Pod，为pending Pod腾出空间。
- 检查Pod是否大于所有节点。例如，如果所有节点的容量都是cpu:1，则永远不会调度请求为cpu:1.1的Pod。

您可使用`kubectl describe nodes`命令检查节点容量和已分配的量。例如：
```sh
kubectl describe nodes e2e-test-node-pool-4lw4
```
```
Name:            e2e-test-node-pool-4lw4
[ ... lines removed for clarity ...]
Capacity:
 cpu:                               2
 memory:                            7679792Ki
 pods:                              110
Allocatable:
 cpu:                               1800m
 memory:                            7474992Ki
 pods:                              110
[ ... lines removed for clarity ...]
Non-terminated Pods:        (5 in total)
  Namespace    Name                                  CPU Requests  CPU Limits  Memory Requests  Memory Limits
  ---------    ----                                  ------------  ----------  ---------------  -------------
  kube-system  fluentd-gcp-v1.38-28bv1               100m (5%)     0 (0%)      200Mi (2%)       200Mi (2%)
  kube-system  kube-dns-3297075139-61lj3             260m (13%)    0 (0%)      100Mi (1%)       170Mi (2%)
  kube-system  kube-proxy-e2e-test-...               100m (5%)     0 (0%)      0 (0%)           0 (0%)
  kube-system  monitoring-influxdb-grafana-v4-z1m12  200m (10%)    200m (10%)  600Mi (8%)       600Mi (8%)
  kube-system  node-problem-detector-v0.1-fj7m3      20m (1%)      200m (10%)  20Mi (0%)        100Mi (1%)
Allocated resources:
  (Total limits may be over 100 percent, i.e., overcommitted.)
  CPU Requests    CPU Limits    Memory Requests    Memory Limits
  ------------    ----------    ---------------    -------------
  680m (34%)      400m (20%)    920Mi (11%)        1070Mi (13%)
```

在前面的输出中，您可以看到，如果一个Pod请求超过1120m ($1800m - 680m = 1120m$)的cpu或6.23Gi ($7474992/1024/1024Gi - 920/1024 Gi = 6.23 Gi$) 的内存，那么它将不适合该节点。

通过查看Pods部分，您可以看到哪些Pods正在占用节点上的空间。

POD可用的资源量小于节点容量，因为系统守护进程使用部分可用资源。`allocatable`字段[NodeStatus](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#nodestatus-v1-core)给出了pod可用的资源量。有关详细信息，请参见[节点可分配资源](https://git.k8s.io/community/contributors/design-proposals/node/node-allocatable.md)。

[资源配额](https://kubernetes.io/docs/concepts/policy/resource-quotas/)功能可以配置为限制可以使用的资源总量。如果与命名空间一起使用，它可以防止一个团队占用所有资源。

### My Container is terminated 

您的容器可能因资源不足而终止。要检查容器是否因达到资源限制而被杀死，请使用kubectl describe pod查看对应pod：
```sh
kubectl describe pod simmemleak-hra99
```
```
Name:                           simmemleak-hra99
Namespace:                      default
Image(s):                       saadali/simmemleak
Node:                           kubernetes-node-tf0f/10.240.216.66
Labels:                         name=simmemleak
Status:                         Running
Reason:
Message:
IP:                             10.244.2.75
Replication Controllers:        simmemleak (1/1 replicas created)
Containers:
  simmemleak:
    Image:  saadali/simmemleak
    Limits:
      cpu:                      100m
      memory:                   50Mi
    State:                      Running
      Started:                  Tue, 07 Jul 2015 12:54:41 -0700
    Last Termination State:     Terminated
      Exit Code:                1
      Started:                  Fri, 07 Jul 2015 12:54:30 -0700
      Finished:                 Fri, 07 Jul 2015 12:54:33 -0700
    Ready:                      False
    Restart Count:              5
Conditions:
  Type      Status
  Ready     False
Events:
  FirstSeen                         LastSeen                         Count  From                              SubobjectPath                       Reason      Message
  Tue, 07 Jul 2015 12:53:51 -0700   Tue, 07 Jul 2015 12:53:51 -0700  1      {scheduler }                                                          scheduled   Successfully assigned simmemleak-hra99 to kubernetes-node-tf0f
  Tue, 07 Jul 2015 12:53:51 -0700   Tue, 07 Jul 2015 12:53:51 -0700  1      {kubelet kubernetes-node-tf0f}    implicitly required container POD   pulled      Pod container image "k8s.gcr.io/pause:0.8.0" already present on machine
  Tue, 07 Jul 2015 12:53:51 -0700   Tue, 07 Jul 2015 12:53:51 -0700  1      {kubelet kubernetes-node-tf0f}    implicitly required container POD   created     Created with docker id 6a41280f516d
  Tue, 07 Jul 2015 12:53:51 -0700   Tue, 07 Jul 2015 12:53:51 -0700  1      {kubelet kubernetes-node-tf0f}    implicitly required container POD   started     Started with docker id 6a41280f516d
  Tue, 07 Jul 2015 12:53:51 -0700   Tue, 07 Jul 2015 12:53:51 -0700  1      {kubelet kubernetes-node-tf0f}    spec.containers{simmemleak}         created     Created with docker id 87348f12526a
```

在上面的示例中，`Restart Count: 5`表示Pod中的simmemleak容器被终止并重新启动了五次。

您可以使用`kubectl get pod`加`-o go-template=...`选项来获取之前终止的容器的状态：
```sh
kubectl get pod -o go-template='{{range.status.containerStatuses}}{{"Container Name: "}}{{.name}}{{"\r\nLastState: "}}{{.lastState}}{{end}}'  simmemleak-hra99
```

```
Container Name: simmemleak
LastState: map[terminated:map[exitCode:137 reason:OOM Killed startedAt:2015-07-07T20:58:43Z finishedAt:2015-07-07T20:58:43Z containerID:docker://0e4095bba1feccdfe7ef9fb6ebffe972b4b14285d5acdec6f0d3ae8a22fad8b2]]
```

您可以看到容器被终止的原因是：`reason:OOM Killed`，其中OOM代表内存不足。


## What's next
* Get hands-on experience [assigning Memory resources to Containers and Pods](https://kubernetes.io/docs/tasks/configure-pod-container/assign-memory-resource/).

* Get hands-on experience [assigning CPU resources to Containers and Pods](https://kubernetes.io/docs/tasks/configure-pod-container/assign-cpu-resource/).

* For more details about the difference between requests and limits, see [Resource QoS](https://git.k8s.io/community/contributors/design-proposals/node/resource-qos.md).

* Read the [Container](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#container-v1-core) API reference

* Read the [ResourceRequirements](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#resourcerequirements-v1-core) API reference

* Read about [project quotas](https://xfs.org/docs/xfsdocs-xml-dev/XFS_User_Guide/tmp/en-US/html/xfs-quotas.html) in XFS

