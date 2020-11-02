# Ephemeral Volumes

本文件描述了Kubernetes的临时卷。建议熟悉卷，特别是PersistentVolumeClaim和PersistentVolume。

有些应用程序需要额外的存储空间，但不关心这些数据是否在重新启动时持久存储。例如，缓存服务通常受内存大小的限制，可以将不经常使用的数据移动到比内存慢的存储中，而对总体性能影响很小。

其他应用程序希望一些只读输入数据出现在文件中，例如配置数据或密钥。

临时卷是为这些用例而设计的。因为卷遵循Pod的生命周期，并与Pod一起创建和删除，所以Pod可以停止和重新启动，而不受限于持久卷在哪里可用的问题。

临时卷在Pod spec中指定，这简化了应用程序部署和管理。

## Types of ephemeral volumes

Kubernetes为不同的目的提供了几种不同类型的临时卷：
- [emptyDir](https://kubernetes.io/docs/concepts/storage/volumes/#emptydir)：Pod启动时为空，存储来自本地kubelet的base目录（通常是根磁盘）或RAM
- [configMap](https://kubernetes.io/docs/concepts/storage/volumes/#configmap)，[downwardAPI](https://kubernetes.io/docs/concepts/storage/volumes/#downwardapi)，[secret](https://kubernetes.io/docs/concepts/storage/volumes/#secret)：将不同种类的Kubernetes数据注入到Pod中
- [CSI临时卷](https://kubernetes.io/docs/concepts/storage/ephemeral-volumes/#csi-ephemeral-volume)：类似于上面的卷类型，但由专门[支持此功能](https://kubernetes-csi.github.io/docs/drivers.html)的[CSI驱动程序](https://github.com/container-storage-interface/spec/blob/master/spec.md)提供
- [通用临时卷](https://kubernetes.io/docs/concepts/storage/ephemeral-volumes/#generic-ephemeral-volumes)，它可以由同时支持持久卷的所有存储驱动程序提供

emptyDir、configMap、downwardAPI、secret作为[本地临时存储](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/#local-ephemeral-storage)提供。它们由kubelet在每个节点上进行管理。

CSI临时卷必须由第三方CSI存储驱动程序提供。

通用临时卷可以由第三方CSI存储驱动程序提供，也可以由任何其他支持动态资源调配的存储驱动程序提供。有些CSI驱动程序是专门为CSI临时卷编写的，不支持动态配置：这些驱动程序不能用于一般的临时卷。

使用第三方驱动程序的优势在于，它们可以提供Kubernetes本身不支持的功能，例如与kubelet管理的磁盘相比具有不同性能特征，或者可以注入不同的数据。

## CSI ephemeral volumes 
*FEATURE STATE: Kubernetes v1.16 [beta]*
此功能要求启用CSIInlineVolume[特型门](https://kubernetes.io/docs/reference/command-line-tools-reference/feature-gates/)。从kubernetes1.16开始是默认启用的。

:notebook: 注意：CSI临时卷仅由CSI驱动程序的一个子集支持。Kubernetes CSI[驱动程序列表](https://kubernetes-csi.github.io/docs/drivers.html)显示哪些驱动程序支持临时卷。

从概念上讲，CSI临时卷类似于configMap、downwardAPI和secret卷类型：存储在每个节点上进行本地管理，并在将Pod调度到节点后与其他本地资源一起创建。Kubernetes在这个阶段没有重新调度rescheduling Pods的概念。卷的创建必须不太可能失败，否则Pod启动就会卡住。特别是，这些卷不支持[能够感知存储容量的Pod调度](https://kubernetes.io/docs/concepts/storage/storage-capacity/)。CSI临时卷目前也不在Pod的存储资源使用限制约束规则内，因为kubelet只能对自己管理的存储的usage进行约束。

下面是一个使用CSI临时存储的Pod的清单示例：
```yaml
kind: Pod
apiVersion: v1
metadata:
  name: my-csi-app
spec:
  containers:
    - name: my-frontend
      image: busybox
      volumeMounts:
      - mountPath: "/data"
        name: my-csi-inline-vol
      command: [ "sleep", "1000000" ]
  volumes:
    - name: my-csi-inline-vol
      csi:
        driver: inline.storage.kubernetes.io
        volumeAttributes:
          foo: bar
```

volumeAttributes确定驱动程序准备什么卷。每个驱动程序都有特定的这些attributes，并且没有标准。有关更多说明，请参阅每个CSI驱动程序的文档。

作为集群管理员，您可以使用[PodSecurityPolicy](https://kubernetes.io/docs/concepts/policy/pod-security-policy/)来控制哪些CSI驱动程序可以在Pod中使用，这是通过[allowedCSIDrivers](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#podsecuritypolicyspec-v1beta1-policy)字段指定的。


## Generic ephemeral volumes
*FEATURE STATE: Kubernetes v1.19 [alpha]*

此功能要求启用GenericEphemeralVolume特性门。因为这是一个alpha功能，所以默认情况下是禁用的。

通用的临时卷类似于emptyDir卷，只是更灵活：
- 存储可以是本地的，也可以是网络连接的。
- 卷可以有固定的大小，而Pods不能超过这个大小。
- 卷可能有一些初始数据，这取决于驱动程序和参数。
- 卷上支持的典型操作（假设驱动程序支持的话），包括[快照](https://kubernetes.io/docs/concepts/storage/volume-snapshots/)、[克隆](https://kubernetes.io/docs/concepts/storage/volume-pvc-datasource/)、[调整大小](https://kubernetes.io/docs/concepts/storage/persistent-volumes/#expanding-persistent-volumes-claims)和[存储容量跟踪](https://kubernetes.io/docs/concepts/storage/storage-capacity/)。

例子：
```yaml
kind: Pod
apiVersion: v1
metadata:
  name: my-app
spec:
  containers:
    - name: my-frontend
      image: busybox
      volumeMounts:
      - mountPath: "/scratch"
        name: scratch-volume
      command: [ "sleep", "1000000" ]
  volumes:
    - name: scratch-volume
      ephemeral:
        volumeClaimTemplate:
          metadata:
            labels:
              type: my-frontend-volume
          spec:
            accessModes: [ "ReadWriteOnce" ]
            storageClassName: "scratch-storage-class"
            resources:
              requests:
                storage: 1Gi
```

## Lifecycle and PersistentVolumeClaim 

关键的设计思想是[卷声明的参数](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#ephemeralvolumesource-v1alpha1-core)允许在pod的卷源内。被允许在pod的卷源内的参数包括PersistentVolumeClaim的labels、annotations和整个字段集。当创建这样一个Pod时，临时卷控制器将在与Pod相同的命名空间中创建一个PersistentVolumeClaim对象，并确保在Pod被删除时PersistentVolumeClaim被删除。

这将触发卷绑定和/或资源调配，无论是在StorageClass使用立即卷绑定时，还是在Pod尝试计划到节点上时（WaitForFirstConsumer卷绑定模式）都会触发。对于pod尝试计划到节点的情况，建议使用通用临时卷 generic ephemeral volumes，因为这样的话调度器可以自由地为Pod选择合适的节点。对于立即绑定的情况，调度程序将被迫选择一个卷可用时即可访问卷的节点。

就[资源所有权](https://kubernetes.io/docs/concepts/workloads/controllers/garbage-collection/#owners-and-dependents)而言，具有通用临时存储的Pod是提供该临时存储的PersistentVolumeClaim的所有者。当Pod被删除时，Kubernetes垃圾收集器会删除PVC，这通常会触发卷的删除，因为存储类的默认回收策略是删除卷。您可以使用带有retain回收策略的StorageClass来创建准临时本地存储：存储的生命周期大于Pod，在这种情况下，您需要确保单独进行卷清理。

当这些PVC存在时，它们可以像其他PVC一样使用。特别是，它们可以作为卷克隆或快照的数据源引用。此外，PVC对象还保存了卷的当前状态。

##  PersistentVolumeClaim naming 

自动创建的PVCS的命名是确定的：名称是POD名称和卷名的组合，中间有连字符（-）。在上面的例子中，PVC名称将是my-app-scratch-volume。这种确定性的命名让与PVC的交互更加容易，因为一旦知道了Pod名称和卷名，就不需要搜索PVC的名字了。

确定性命名还引入了不同的Pod之间的潜在冲突（一个名为“Pod-a”，卷为“scratch”的pod和另一个名为“Pod”,卷为“a-scratch”的Pod都使用相同的PVC名称：“Pod-a-scratch”）以及Pod和手动创建的PVC之间存在的潜在冲突。

这样的冲突会被检测到：（存在冲突的情况下, *这是我的理解*）如果PVC是为Pod创建的，那么它只能用作临时卷。此检查是基于所有权关系的。现有PVC不会被覆盖或修改。但这并不能解决冲突，因为没有合适的PVC，Pod无法启动。

:warning: 注意：在同一命名空间内命名pod和卷时要小心，以免发生这些冲突

## Security 

启用GenericEphemeralVolume特性允许用户间接创建pvc（如果他们可以创建pod），即使他们没有直接创建pvc的权限。群集管理员必须知道这一点。如果这不符合他们的安全要求，他们有两种选择：
- 通过特性门显式地禁用该特性，以避免在将来的Kubernetes版本默认启用它时感到惊讶。
- 使用[Pod安全策略](https://kubernetes.io/docs/concepts/policy/pod-security-policy/)，让卷列表不包含临时卷类型。

The normal namespace quota for PVCs in a namespace still applies, so even if users are allowed to use this new mechanism, they cannot use it to circumvent other policies.
但是pvc的命名空间配额仍然适用（也就是说即使使用了上面的机制禁止了PVC的间接创建，pvc的命名空间配额仍然存在？），因此即使允许用户使用这种新机制（指的是GenericEphemeralVolume特性?），他们也不能使用它来规避其他策略。


## What's next
。。。



