# Resource Quotas

当多个用户或团队共享一个使用固定数量节点的集群时，存在一个问题就是有团队可能会使用超过其公平份额的资源。

资源配额是管理员解决此问题的工具。

由ResourceQuota对象定义的资源配额限制每个命名空间的聚合资源消耗。它可以按类型限制命名空间中可以创建的对象数量，以及该命名空间中的资源可能消耗的计算资源总量。

资源配额的工作原理如下：

- 不同的团队在不同的命名空间中工作。目前这是自愿的，但计划通过ACL来强制支持这一点。
- 管理员为每个命名空间创建一个ResourceQuota。
- 用户在命名空间中创建资源（pod、服务等），配额系统跟踪使用情况，以确保其不会超过ResourceQuota中硬性定义的资源限制。
- 如果创建或更新资源违反了配额约束，则请求将失败，返回HTTP状态代码403 FORBIDDEN，并显示一条消息说明已违反的约束。
- 如果在命名空间中为cpu和内存等计算资源启用了配额，则用户必须为这些值指定请求或限制；否则，配额系统可能会拒绝pod创建。提示：使用LimitRanger许可控制器强制设置默认值给没有定义计算资源需求的pod。
    有关如何避免此问题的示例，请参见[演练](https://kubernetes.io/docs/tasks/administer-cluster/manage-resources/quota-memory-cpu-namespace/)。

ResourceQuota对象的名称必须是有效的DNS子域名。

可以使用命名空间和配额创建的策略示例如下：
- 在一个容量为32 GiB RAM和16个内核的集群中，让团队A使用20 GiB和10个内核，让B使用10个GiB和4个内核，并保留2GiB和2个内核以备将来分配。
- 将“testing”命名空间限制为使用1个内核和1GiB RAM。让“production”命名空间使用任意数量。

在集群的总容量小于命名空间配额之和的情况下，可能存在资源争用。这是通过在先到先得的原则处理的。

对配额的争用或更改都不会影响已创建的资源。

## Enabling Resource Quota 

默认情况下，许多Kubernetes发行版都启用了资源配额支持。当API服务器`--enable-admission-plugins=`标志将`ResourceQuota`作为其参数之一时，就会启用它。

当特定命名空间中存在ResourceQuota时，将在该命名空间中强制实施资源配额。

## Compute Resource Quota 

可以限制给定命名空间中可以请求的计算资源总数。

支持以下资源类型：
Resource Name | Description
--------------|------------
limits.cpu | 在非终结状态下的所有pod中，CPU限制的总和不能超过此值。
limits.memory | 对于非终结状态的所有pod，内存限制的总和不能超过此值。
requests.cpu  | 对于非终结状态的所有pod，CPU请求总数不能超过此值。
requests.memory | 对于非终结状态的所有pod，内存请求的总和不能超过此值。
`hugepages-<size>` | 对于非终结状态的所有pod，指定大小的huge page请求数不能超过此值。
cpu             | Same as requests.cpu
memory          | Same as requests.memory


### Resource Quota For Extended Resources 

除了上面提到的资源之外，在1.10版中，增加了对[扩展资源](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/#extended-resources)的配额支持。

由于扩展资源不允许超限使用，因此在配额中为同一扩展资源同时指定请求和限制是没有意义的。因此，对于扩展资源，只允许带有前缀`requests.`的配额项。

以GPU资源为例，如果资源名为`nvidia.com/gpu`，如果要将命名空间中请求的GPU总数限制为4，则可以按如下方式定义配额：

- `requests.nvidia.com/gpu: 4`

有关详细信息，请参阅[查看和设置配额](https://kubernetes.io/docs/concepts/policy/resource-quotas/#viewing-and-setting-quotas)


## Storage Resource Quota 

您可以限制给定命名空间中可以请求的[存储资源](https://kubernetes.io/docs/concepts/storage/persistent-volumes/)总量。

此外，还可以根据关联的存储类限制存储资源的消耗。

Resource Name | Description
--------------|------------
requests.storage    |   所有持久卷声明，对应存储请求的总和不能超过此值。
persistentvolumeclaims  |   命名空间中可以存在的PersistentVolumeClaims的总个数。
`<storage-class-name>.storageclass.storage.k8s.io/requests.storage` |   与`<storage-class-name>`关联的所有持久卷声明，其存储请求的总和不能超过此值。
`<storage-class-name>.storageclass.storage.k8s.io/persistentvolumeclaims`   |   与`storage-class-name`关联的所有持久卷声明，命名空间中可以存在的持久卷声明的总个数。


例如，如果一个操作员希望使用`gold`存储类与`bronze`存储类分开来对存储进行配额，则可以按如下方式定义配额：
- `gold.storageclass.storage.k8s.io/requests.storage: 500Gi`
- `bronze.storageclass.storage.k8s.io/requests.storage: 100Gi`

在版本1.8中，本地临时存储的配额支持是一个alpha功能：
Resource Name | Description
--------------|------------
requests.ephemeral-storage  |   命名空间中的所有pod，本地临时存储请求的总和不能超过此值。
limits.ephemeral-storage    |   命名空间中的所有pod，本地临时存储限制的总和不能超过此值。
ephemeral-storage           |   Same as requests.ephemeral-storage


## Object Count Quota 

您可以使用以下语法为所有标准、命名空间中的资源类型的资源的总数设置配额：
* 对来自非核心组的资源使用`count/<resource>.<group>`
* 对来自核心组的资源使用`count/<resource>`


下面是一组用户可能希望放在对象计数配额下的资源示例：
* count/persistentvolumeclaims
* count/services
* count/secrets
* count/configmaps
* count/replicationcontrollers
* count/deployments.apps
* count/replicasets.apps
* count/statefulsets.apps
* count/jobs.batch
* count/cronjobs.batch


自定义资源也可以使用相同的语法。例如，在`example.com`API group中创建一个`widgets`自定义资源的配额，可以使用`count/widgets.example.com`

使用`count/*`资源配额时，如果某个对象存在于服务器存储中，则会根据该配额进行消耗。这些类型的配额对于防止存储资源耗尽非常有用。例如，由于Secret很大，您可能希望限制服务器中Secret的数量。群集中如果有过多的Secret实际上会阻止服务器和控制器启动。您可以为作业设置配额，以防止配置不良的CronJob。在命名空间中创建过多Job的CronJobs可能导致拒绝服务。

也可以对有限的资源集执行通用对象计数配额。支持以下类型：

Resource Name  | Description
---------------|------------
configmaps      |       命名空间中可以存在的ConfigMaps总数。
persistentvolumeclaims  |   命名空间中可以存在的PersistentVolumeClaims的总数。
pods    |   命名空间中可以存在的非终结状态的Pod总数。如果`.status.phase in (Failed, Succeeded)`为true，那么Pod是终结的状态
replicationcontrollers  |   命名空间中可以存在的ReplicationControllers总数。
resourcequotas  |   命名空间中可以存在的ResourceQuotas总数。
services    |   命名空间中可以存在的服务总数。
services.loadbalancers  |   命名空间中可以存在的LoadBalancer类型的服务总数。
services.nodeports  |   命名空间中可以存在的NodePort类型的服务总数
secrets |   命名空间中可以存在的Secret总数。

例如，pods配额，限制了单个命名空间中创建的非终结状态的pods数量的最大值。您可能需要在命名空间上设置一个pods配额，以避免用户创建许多小的pods并耗尽集群的Pod ip供应的情况。


## Quota Scopes

每个配额可以有一组关联的作用域scopes。配额仅在与枚举作用域的交集匹配时计算资源的使用情况。

当给配额添加作用域时，会将配额支持的资源数量限制为与该作用域相关的数值。在配额上指定的资源如果不在允许的集和内，则会导致验证错误。

Scope | Description
------|------------
Terminating     |       匹配`.spec.activeDeadlineSeconds >= 0`的pods
NotTerminating  |   匹配`.spec.activeDeadlineSeconds is nil`的Pods
BestEffort      |   匹配具有最佳服务质量的Pods。
NotBestEffort   |   匹配不具有最佳服务质量的Pods。
PriorityClass   |   匹配引用了[priority class](https://kubernetes.io/docs/concepts/configuration/pod-priority-preemption)的pod。

BestEffort作用域将配额限制为跟踪以下资源：
- pods

Termining、NotTermining、NotBestEffort和PriorityClass作用域将配额限制为跟踪以下资源：
* pods
* cpu
* memory
* requests.cpu
* requests.memory
* limits.cpu
* limits.memory

请注意，不能在同一配额中同时指定`Terminating`和`NotTerminating`作用域，也不能在同一配额中同时指定`BestEffort`和`NotBestEffort`作用域。

scopeSelector支持`operator`字段使用以下值：
* In
* NotIn
* Exists
* DoesNotExist

在定义scopeSelector，使用以下值之一作为`scopeName`时，`operator`必须存在。
* Terminating
* NotTerminating
* BestEffort
* NotBestEffort
  
如果运算符为In或NotIn，则“values”字段必须至少有一个值。例如：
```yaml
  scopeSelector:
    matchExpressions:
      - scopeName: PriorityClass
        operator: In
        values:
          - middle
```

如果运算符是Exists或DoesNotExist，则不能指定values字段。

### Resource Quota Per PriorityClass
**FEATURE STATE: Kubernetes v1.17 [stable]**

可以按照特定的[优先级](https://kubernetes.io/docs/concepts/configuration/pod-priority-preemption/#pod-priority)创建pod。通过使用配额规范中的scopeSelector字段，可以根据pod的优先级控制pod对系统资源的消耗。

只有在配额规范中的scopeSelector选择pod时，才会匹配和使用配额。

当配额使用scopeSelector字段限定priority class的作用域时，配额对象被限制为仅跟踪以下资源：
* pods
* cpu
* memory
* ephemeral-storage
* limits.cpu
* limits.memory
* limits.ephemeral-storage
* requests.cpu
* requests.memory
* requests.ephemeral-storage

本例创建一个quota对象，特定优先级的pods与之匹配。示例如下所示：
- 集群中pod的优先级有“低”、“中”、“高”三种，每个Pod拥有一种。
- 每种优先级都有一个对应的配额对象

将以下YAML保存到一个quota.yml文件.
```yaml
apiVersion: v1
kind: List
items:
- apiVersion: v1
  kind: ResourceQuota
  metadata:
    name: pods-high
  spec:
    hard:
      cpu: "1000"
      memory: 200Gi
      pods: "10"
    scopeSelector:
      matchExpressions:
      - operator : In
        scopeName: PriorityClass
        values: ["high"]
- apiVersion: v1
  kind: ResourceQuota
  metadata:
    name: pods-medium
  spec:
    hard:
      cpu: "10"
      memory: 20Gi
      pods: "10"
    scopeSelector:
      matchExpressions:
      - operator : In
        scopeName: PriorityClass
        values: ["medium"]
- apiVersion: v1
  kind: ResourceQuota
  metadata:
    name: pods-low
  spec:
    hard:
      cpu: "5"
      memory: 10Gi
      pods: "10"
    scopeSelector:
      matchExpressions:
      - operator : In
        scopeName: PriorityClass
        values: ["low"]
```

使用kubectl create应用YAML。
```sh
kubectl create -f ./quota.yml
```
```
resourcequota/pods-high created
resourcequota/pods-medium created
resourcequota/pods-low created
```

使用`kubectl describe quota`验证Used quota为0。

```sh
kubectl describe quota
```
```
Name:       pods-high
Namespace:  default
Resource    Used  Hard
--------    ----  ----
cpu         0     1k
memory      0     200Gi
pods        0     10


Name:       pods-low
Namespace:  default
Resource    Used  Hard
--------    ----  ----
cpu         0     5
memory      0     10Gi
pods        0     10


Name:       pods-medium
Namespace:  default
Resource    Used  Hard
--------    ----  ----
cpu         0     10
memory      0     20Gi
pods        0     10
```

创建一个优先级为“high”的Pod。将以下YAML保存到`high-priority-pod.yml`文件
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: high-priority
spec:
  containers:
  - name: high-priority
    image: ubuntu
    command: ["/bin/sh"]
    args: ["-c", "while true; do echo hello; sleep 10;done"]
    resources:
      requests:
        memory: "10Gi"
        cpu: "500m"
      limits:
        memory: "10Gi"
        cpu: "500m"
  priorityClassName: high
```

Apply it with `kubectl create`.

```sh
kubectl create -f ./high-priority-pod.yml
```

验证“high”优先级配额pods-high的使用状态已发送变化，其他两个配额没有改变。
```sh
kubectl describe quota
```
```
Name:       pods-high
Namespace:  default
Resource    Used  Hard
--------    ----  ----
cpu         500m  1k
memory      10Gi  200Gi
pods        1     10


Name:       pods-low
Namespace:  default
Resource    Used  Hard
--------    ----  ----
cpu         0     5
memory      0     10Gi
pods        0     10


Name:       pods-medium
Namespace:  default
Resource    Used  Hard
--------    ----  ----
cpu         0     10
memory      0     20Gi
pods        0     10
```

## Requests compared to Limits 

在分配计算资源时，每个容器可以为CPU或内存指定一个请求和一个限制值。可以使用quota给请求和限制分别制定配额。

如果指定了`requests.cpu`或`requests.memory`的配额，则要求每个传入容器对这些资源发出显式请求。如果配额指定了`limits.cpu`或`limits.memory`，则要求每个传入容器为这些资源指定一个显式限制。

## Viewing and Setting Quotas 

Kubectl支持创建、更新和查看配额：
```sh
kubectl create namespace myspace
```
```sh
cat <<EOF > compute-resources.yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: compute-resources
spec:
  hard:
    requests.cpu: "1"
    requests.memory: 1Gi
    limits.cpu: "2"
    limits.memory: 2Gi
    requests.nvidia.com/gpu: 4
EOF
```

```sh
kubectl create -f ./compute-resources.yaml --namespace=myspace
```

```sh
cat <<EOF > object-counts.yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: object-counts
spec:
  hard:
    configmaps: "10"
    persistentvolumeclaims: "4"
    pods: "4"
    replicationcontrollers: "20"
    secrets: "10"
    services: "10"
    services.loadbalancers: "2"
EOF
```

```sh
kubectl create -f ./object-counts.yaml --namespace=myspace
```
```sh
kubectl get quota --namespace=myspace
```
```sh
NAME                    AGE
compute-resources       30s
object-counts           32s
```

```sh
kubectl describe quota compute-resources --namespace=myspace
```
```sh
Name:                    compute-resources
Namespace:               myspace
Resource                 Used  Hard
--------                 ----  ----
limits.cpu               0     2
limits.memory            0     2Gi
requests.cpu             0     1
requests.memory          0     1Gi
requests.nvidia.com/gpu  0     4
```

```sh
kubectl describe quota object-counts --namespace=myspace
```
```sh
Name:                   object-counts
Namespace:              myspace
Resource                Used    Hard
--------                ----    ----
configmaps              0       10
persistentvolumeclaims  0       4
pods                    0       4
replicationcontrollers  0       20
secrets                 1       10
services                0       10
services.loadbalancers  0       2
```


Kubectl还支持使用语法`count/<resource>.<group>`为所有标准命名空间中的资源设置对象计数配额：

```sh
kubectl create namespace myspace
```
```sh
kubectl create quota test --hard=count/deployments.apps=2,count/replicasets.apps=4,count/pods=3,count/secrets=4 --namespace=myspace
```
```sh
kubectl create deployment nginx --image=nginx --namespace=myspace --replicas=2
```
```sh
kubectl describe quota --namespace=myspace
```
```sh
Name:                         test
Namespace:                    myspace
Resource                      Used  Hard
--------                      ----  ----
count/deployments.apps        1     2
count/pods                    2     3
count/replicasets.apps        1     4
count/secrets                 1     4
```

## Quota and Cluster Capacity

资源配额独立于群集容量。它们以绝对单位表示。因此，如果将节点添加到集群中，这不会自动为每个命名空间提供消耗更多资源的能力。

有时可能需要更复杂的策略，例如：
* 按比例将集群资源分配给多个团队。
* 允许每个租户根据需要增加资源使用量，但要有一个限制，以防止资源意外耗尽。
* 从一个命名空间检测需求，添加节点，并增加配额。

这样的策略可以使用ResourceQuotas作为构建块来实现，方法是编写一个“控制器”，监视配额使用情况，并根据其他信号调整每个命名空间的配额硬限制。

请注意，资源配额会划分聚合集群资源，但它对节点没有任何限制：来自多个命名空间的pod可以在同一个节点上运行

## Limit Priority Class consumption by default
当且仅当存在匹配的配额对象时，可能希望在命名空间中允许具有特定优先级的pod，例如“cluster-services”。

通过这种机制，操作符可以将某些高优先级类的使用限制在有限数量的命名空间中，并且不是每个命名空间都可以默认使用这些优先级类。

要强制执行此操作，应使用kube apiserver标志`--admission-control-config-file`并把值配置为下配置文件的路径：
```yaml
apiVersion: apiserver.config.k8s.io/v1
kind: AdmissionConfiguration
plugins:
- name: "ResourceQuota"
  configuration:
    apiVersion: apiserver.config.k8s.io/v1
    kind: ResourceQuotaConfiguration
    limitedResources:
    - resource: pods
      matchScopes:
      - scopeName: PriorityClass
        operator: In
        values: ["cluster-services"]
```

现在，"cluster-services" pods只允许在拥有配额对象，且配额对象和`scopeSelector`匹配的命名空间中使用。例如：
```yaml
    scopeSelector:
      matchExpressions:
      - scopeName: PriorityClass
        operator: In
        values: ["cluster-services"]
```

## What's next
* See [ResourceQuota design doc](https://git.k8s.io/community/contributors/design-proposals/resource-management/admission_control_resource_quota.md) for more information.
* See a [detailed example for how to use resource quota.](https://kubernetes.io/docs/tasks/administer-cluster/quota-api-object/)
* Read [Quota support for priority class design doc.](https://github.com/kubernetes/community/blob/master/contributors/design-proposals/scheduling/pod-priority-resourcequota.md)
* See [LimitedResources](https://github.com/kubernetes/kubernetes/pull/36765)
