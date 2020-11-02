# Storage Capacity

存储容量是有限的，并且可能因pod运行的节点而异：网络连接的存储可能不是所有节点都可以访问的，或者存储首先是某个节点的本地存储。

*FEATURE STATE: Kubernetes v1.19 [alpha]*

本页介绍Kubernetes如何跟踪存储容量，以及调度器如何使用这些信息将pod调度到节点上，这些节点可以访问足够的存储容量来容纳剩余的缺失卷。如果没有存储容量跟踪，调度程序可能会选择一个没有足够容量来配置卷的节点，这样可能需要多次重试调度。

容器存储接口（CSI）驱动程序支持跟踪存储容量，安装CSI驱动程序时需要[启用跟踪存储容量](https://kubernetes.io/docs/concepts/storage/storage-capacity/#enabling-storage-capacity-tracking)功能。

## API
此功能有两个API扩展：
- [CSIStorageCapacity](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#csistoragecapacity-v1alpha1-storage-k8s-io)对象：这些对象由CSI驱动程序在安装该驱动程序的命名空间中生成。每个对象包含一个存储类的容量信息，并定义哪些节点可以访问该存储。
- [The CSIDriverSpec.StorageCapacity field](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#csidriverspec-v1-storage-k8s-io)：当设置为true时，Kubernetes调度程序将考虑使用CSI驱动程序的卷的存储容量。

## Scheduling
如果出现以下情况，Kubernetes调度程序将使用存储容量信息：
- CSIStorageCapacity 特性已开启
- Pod使用尚未创建的卷
- 该卷使用一个引用CSI驱动程序并使用WaitForFirstConsumer[卷绑定模式](https://kubernetes.io/docs/concepts/storage/storage-classes/#volume-binding-mode)的StorageClass，以及
- 驱动程序的CSIDriver对象的StorageCapacity设置为true。

在这种情况下，调度程序只考虑对于Pod来说具有足够可用存储空间的节点。这种检查非常简单，只需要将卷的大小与CSIStorageCapacity对象（其拓扑结构包括节点）中列出来的容量进行比较即可。

对于具有`Immediate`卷绑定模式的卷，存储驱动程序决定在何处创建卷，而与要使用该卷的pod无关。然后，调度程序将pod调度到卷创建后卷可用的节点上。

对于[CSI临时卷](https://kubernetes.io/docs/concepts/storage/volumes/#csi)，调度时总是不考虑存储容量。这是基于这样一个假设，即此卷类型仅由特定的CSI驱动程序使用，这些驱动程序在节点上是本地的，并且不需要大量的资源。

## Rescheduling 
当为一个具有WaitForFirstConsumer卷的Pod选择了一个节点时，选择这个节点的决定仍然是暂时的。下一步是要求CSI存储驱动程序创建卷，并提示该卷应该在所选节点上可用。

因为Kubernetes可能已经根据过时的容量信息选择了一个节点，所以可能无法真正创建卷。然后节点选择的结果会被重置，Kubernetes调度器再次尝试为Pod找一个节点。

## Limitations

存储容量跟踪增加了调度在第一次尝试时起作用的可能性，但不能保证这一点，因为调度程序必须基于可能过时的信息来决定。通常，相同的重试机制下如果没有任何容量信息，那么调度将失败。

调度可能永久失败的一种情况是Pod使用多个卷：一个卷可能已经在拓扑段中创建，而拓扑段中已经没有足够的容量用于另一个卷。这需要手动干预来恢复，例如通过增加容量或删除已创建的卷。kubernetes还需要[进一步的工作](https://github.com/kubernetes/enhancements/pull/1703)来自动处理这个问题。


## Enabling storage capacity tracking  启用存储容量跟踪

存储容量跟踪是一个alpha功能，仅当CSIStorageCapacity [feature gate](https://kubernetes.io/docs/reference/command-line-tools-reference/feature-gates/) 和`storage.k8s.io/v1alpha1` API group被启用时才启用。有关详细信息，请参阅--feature-gates和--runtime-configg [kube-apiserver parameters](https://kubernetes.io/docs/reference/command-line-tools-reference/kube-apiserver/)。


快速检查Kubernetes集群是否支持该功能的方法是列出CSIStorageCapacity对象：
```shell
kubectl get csistoragecapacities --all-namespaces
```

如果您的集群支持CSIStorageCapacity，则响应要么是CSIStorageCapacity对象的列表，要么是：
```shell
No resources found
```

如果不支持，则会打印此错误：
```sh
error: the server doesn't have a resource type "csistoragecapacities"
```

除了在集群中启用该功能外，CSI驱动程序也必须要支持。有关详细信息，请参阅驱动文档





