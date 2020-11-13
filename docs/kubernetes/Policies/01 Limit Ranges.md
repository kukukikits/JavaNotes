# Limit Ranges

默认情况下，容器在Kubernetes集群上使用无限的[计算资源](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/)运行。使用资源配额，集群管理员可以基于命名空间限制资源消耗和创建。在一个命名空间中，一个Pod或容器可以消耗与命名空间的资源配额所定义的一样多的CPU和内存。人们担心一个Pod或容器可能消耗所有可用资源。LimitRange是一种限制命名空间中资源分配（到pod或容器）的策略。

LimitRange提供的约束可以：
* 强制命名空间中每个Pod或容器的最小和最大计算资源使用量。
* 强制命名空间中每个PersistentVolumeClaim的最小和最大存储请求。
* 在命名空间中强制资源的请求和限制之间的比率。
* 为命名空间中的计算资源设置默认请求/限制，并在运行时自动将它们注入容器


## Enabling LimitRange 

自Kubernetes 1.10以来，默认情况下已启用LimitRange支持。

当某个命名空间中有一个LimitRange对象时，会在该命名空间中强制实施LimitRange。

LimitRange对象的名称必须是有效的DNS子域名

### Overview of Limit Range 

* 管理员在命名空间中创建LimitRange。
* 用户在命名空间中创建资源，如pod、容器和PersistentVolumeClaims。
* LimitRanger许可控制器对未设置计算资源需求的所有Pod和容器强制执行默认值和限制，并且跟踪资源的使用率，以确保其不超过命名空间中任何LimitRange中定义的资源最小值、最大值和比率。
* 如果创建或更新违反LimitRange约束的资源（Pod、Container、PersistentVolumeClaim），则对API服务器的请求将失败，并显示一条HTTP状态代码403 FORBIDDEN，并显示一条消息来解释已违反的约束。
* 如果在命名空间中为cpu和内存等计算资源使用了LimitRange，则用户必须为这些值指定请求或限制。否则，系统可能会拒绝创建Pod。
* LimitRange验证仅在Pod Admission阶段发生，不会在运行中的Pod上进行。

使用“限制范围”的示例如下：
- 在一个容量为8 GiB RAM和16核的2个节点的集群中，命名空间中的Pod请求使用100m CPU，CPU最大限制为500m，请求使用200Mi内存，最大限制600Mi。
- 设置默认CPU限制和请求为150m，内存默认请求为300Mi，以满足其规范中没有CPU和内存请求的容器。

如果命名空间的总限制小于pod/容器限制的总和，则可能存在资源争用。在这种情况下，将不会创建容器或Pod。

对资源的竞争和对LimitRange的修改都不会影响已创建的资源。

## What's next
Refer to the [LimitRanger design document](https://git.k8s.io/community/contributors/design-proposals/resource-management/admission_control_limit_range.md) for more information.

For examples on using limits, see:

* [how to configure minimum and maximum CPU constraints per namespace.](https://kubernetes.io/docs/tasks/administer-cluster/manage-resources/cpu-constraint-namespace/)
* [how to configure minimum and maximum Memory constraints per namespace.](https://kubernetes.io/docs/tasks/administer-cluster/manage-resources/memory-constraint-namespace/)
* [how to configure default CPU Requests and Limits per namespace.](https://kubernetes.io/docs/tasks/administer-cluster/manage-resources/cpu-default-namespace/)
* [how to configure default Memory Requests and Limits per namespace.](https://kubernetes.io/docs/tasks/administer-cluster/manage-resources/memory-default-namespace/)
* [how to configure minimum and maximum Storage consumption per namespace.](https://kubernetes.io/docs/tasks/administer-cluster/limit-storage-consumption/#limitrange-to-limit-requests-for-storage)
* [a detailed example on configuring quota per namespace.](https://kubernetes.io/docs/tasks/administer-cluster/manage-resources/quota-memory-cpu-namespace/)




