# Persistent Volumes
## Introduction

管理存储与管理计算实例是一个截然不同的问题。PersistentVolume子系统为用户和管理员提供了一个API，该API从存储的使用方式抽象出了存储的提供方式的细节。为此，我们引入两个新的API资源：PersistentVolume和PersistentVolumeClaim。

PersistentVolume（PV）是集群中由管理员配置或使用[存储类Storage Classes](https://kubernetes.io/docs/concepts/storage/storage-classes/)动态配置的存储块。它是集群中的资源，就像节点是集群资源一样。PV是类似于卷的卷插件，但其生命周期独立于使用PV的任何单个Pod。此API对象捕获存储实现的详细信息，可以是NFS、iSCSI或特定于云提供商的存储系统。

PersistentVolumeClaim（PVC）是用户对存储的请求。它类似于Pod。pod消耗节点资源，pvc消耗PV资源。Pods可以请求特定级别的资源（CPU和内存）。PVC可以请求特定的大小和访问模式（例如，可以装入ReadWriteOnce、ReadOnlyMany或ReadWriteMany，参见[AccessModes](https://kubernetes.io/docs/concepts/storage/persistent-volumes/#access-modes)）。

虽然persistentVolumeClaimes允许用户使用抽象存储资源，但对于不同的问题，用户通常需要具有不同属性（例如性能）的PersistentVolumes。集群管理员需要能够提供不仅仅是大小和访问模式不同的各种persistentVolume，而不需要向用户公开这些卷的实现细节。对于这样的需求，我们提供了StorageClass资源。

See the [detailed walkthrough with working examples](https://kubernetes.io/docs/tasks/configure-pod-container/configure-persistent-volume-storage/)


## Lifecycle of a volume and claim
pv是集群中的资源。pvc是对这些资源的请求，同时也是对资源的声明检查。PV和PVC之间的交互遵循以下生命周期：

### Provisioning 配置
提供pv有两种方式：静态或动态

1. Static
集群管理员创建许多pv。它们携带真实存储的详细信息，供集群用户使用。它们存在于kubernetes api中，可供消费。

2. Dynamic
当管理员创建的静态pv中没有一个与用户的PersistentVolumeClaim匹配时，集群可能会尝试动态地为PVC提供一个卷。这种配置是基于StorageClasses的：PVC必须请求一个存储类，管理员必须已经创建并配置了该存储类才能进行动态资源调配。请求的类申明为“”，即禁用动态配置。

要启用基于存储类的动态存储资源调配，群集管理员需要在API服务器上启用DefaultStorageClass[许可控制器](https://kubernetes.io/docs/reference/access-authn-authz/admission-controllers/#defaultstorageclass)。例如，可以在--enable-admission-plugins标志的值中设置DefaultStorageClass来启用（标志的值是逗号分隔的有序列表）。有关API服务器命令行标志的更多信息，请查看[kube-apiserver文档](https://kubernetes.io/docs/admin/kube-apiserver/)

### Binding

用户创建了一个PersistentVolumeClaim，或者在动态资源调配的情况下，已经创建了一个PersistentVolumeClaim，它具有特定的请求存储量和特定的访问模式。主程序中的控制循环监视新的pvc，找到匹配的PV（如果可能），并将它们绑定在一起。如果一个PV是为一个新PVC动态配置的，那么这个循环将始终将该PV绑定到PVC。否则，用户将始终至少得到他们所要求的，但容量可能会超过所请求的量。一旦绑定，PersistentVolumeClaim绑定是独占的，而不管它们是如何绑定的。PVC到PV绑定是一对一的映射，使用ClaimRef表示映射关系，它是PersistentVolume和PersistentVolumeClaim之间的双向绑定。

如果不存在匹配卷，则PVC将无限期地保持未绑定状态。PVC将在匹配卷可用时绑定。例如，一个配置了许多50Gi pv的集群与请求100Gi的PVC不匹配。当一个100Gi的PV被添加到集群中时，PVC才可以被绑定。

### Using

pod把声明当做卷。集群通过检查声明以找到绑定的卷并为Pod装载该卷。对于支持多种访问模式的卷，用户在Pod中使用声明作为卷时，指定所使用的访问模式。

一旦用户声明已经被绑定，只要用户需要绑定的PV就可以被用户使用。用户通过在Pod的volumes中声明persistentVolumeClaim来调度Pod并访问声明的PVs。更多详情请参见[Claims As Volumes](https://kubernetes.io/docs/concepts/storage/persistent-volumes/#claims-as-volumes)。

### Storage Object in Use Protection 存储对象使用保护

存储对象使用保护功能的目的是确保Pod正在使用的（in active use by a Pod）PersistentVolumeClaims（PVC），以及绑定到PVC的PersistentVolume (PVs) 不被系统删除，因为这可能会导致数据丢失。

:bookmark: Note： PVC is in active use by a Pod when a Pod object exists that is using the PVC.注意：当存在使用PVC的Pod对象时，Pod使用的PVC就是正在使用中的状态。

如果用户删除了Pod正在使用的PVC，则不会立即删除该PVC。PVC的移除将推迟到PVC不再被任何Pod使用后。另外，如果管理员删除了绑定到PVC的PV，则不会立即删除PV。直到PV不再绑定到PVC上后，才真正移除PV。

您可以看到，当PVC的状态为Terminating并且Finalizers列表包含`kubernetes.io/pvc-protection`时，PVC是被保护起来的：

```shell
kubectl describe pvc hostpath
Name:          hostpath
Namespace:     default
StorageClass:  example-hostpath
Status:        Terminating
Volume:
Labels:        <none>
Annotations:   volume.beta.kubernetes.io/storage-class=example-hostpath
               volume.beta.kubernetes.io/storage-provisioner=example.com/hostpath
Finalizers:    [kubernetes.io/pvc-protection]
...
```

您可以看到当PV的状态为Terminating并且Finalizers列表包含`kubernetes.io/pv-protection`时，PV也是被保护起来的：
```shell
kubectl describe pv task-pv-volume
Name:            task-pv-volume
Labels:          type=local
Annotations:     <none>
Finalizers:      [kubernetes.io/pv-protection]
StorageClass:    standard
Status:          Terminating
Claim:
Reclaim Policy:  Delete
Access Modes:    RWO
Capacity:        1Gi
Message:
Source:
    Type:          HostPath (bare host directory volume)
    Path:          /tmp/data
    HostPathType:
Events:            <none>
```

### Reclaiming 回收

当用户使用完卷后，他们可以从允许资源回收的API中删除PVC对象。PersistentVolume的回收策略告诉集群在释放卷的声明后该如何处理该卷。当前的回收策略有：保留、回收或删除卷

1. Retain 保留

保留回收策略允许手动回收资源。当删除PersistentVolumeClaim后，PersistentVolume仍然存在，并且该卷被视为“已释放”。但由于前一位声明者的数据仍保留在卷上，因此还不能用于另一个声明。管理员可以通过以下步骤手动回收卷。
> 1. 删除PersistentVolume。删除PV后，外部基础设施中关联的存储资产（如AWS EBS、GCE PD、Azure磁盘或Cinder卷）仍然存在。
> 2. 相应地手动清理关联存储资产上的数据。
> 3. 手动删除关联的存储资产，或者如果要重用同一存储资产，请使用存储资产创建新的PersistentVolume

2. Delete

对于支持Delete reclaim策略的卷插件，删除操作会从Kubernetes中删除PersistentVolume对象，也会删除外部基础设施中相关的存储资产，如AWS EBS、GCE PD、Azure磁盘或Cinder卷。动态配置的卷继承[其StorageClass的回收策略](https://kubernetes.io/docs/concepts/storage/persistent-volumes/#reclaim-policy)，默认为删除。管理员应根据用户的期望配置StorageClass；否则，必须在PV创建后对其进行编辑或修补。请参阅[更改PersistentVolume的回收策略](https://kubernetes.io/docs/tasks/administer-cluster/change-pv-reclaim-policy/)

3. Recycle 
:warning: 警告：回收回收策略已弃用。相反，建议的方法是使用动态资源调配。

如果底层卷插件支持，回收策略将对卷执行基本清理（`rm -rf /thevolume/*`），并使其再次可用于新的声明。

但是，管理员可以使用Kubernetes controller manager命令行参数配置自定义回收器Pod模板。自定义回收站Pod模板必须包含卷volumes规范，如下例所示:
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: pv-recycler
  namespace: default
spec:
  restartPolicy: Never
  volumes:
  - name: vol
    hostPath:
      path: /any/path/it/will/be/replaced
  containers:
  - name: pv-recycler
    image: "k8s.gcr.io/busybox"
    command: ["/bin/sh", "-c", "test -e /scrub && rm -rf /scrub/..?* /scrub/.[!.]* /scrub/*  && test -z \"$(ls -A /scrub)\" || exit 1"]
    volumeMounts:
    - name: vol
      mountPath: /scrub
```

不过，在自定义回收器Pod模板的volumes规范中指定的path路径，要替换为正在回收的卷的特定路径。

### Reserving a PersistentVolume 预定PersistentVolume

control plane可以将[PersistentVolumeClaims绑定到集群中匹配的PersistentVolumes](https://kubernetes.io/docs/concepts/storage/persistent-volumes/#binding)。但是，如果你想要一个PVC绑定到一个特定的PV，你需要预先绑定它们。

通过在PersistentVolumeClaim中指定PersistentVolume，可以声明该特定PV和PVC之间的绑定。如果存在一个PersistentVolume并且没有使用claimRef字段预定PersistentVolumeClaims，那么PersistentVolume就会和PersistentVolumeClaim绑定。

在某些卷匹配条件（如节点亲和力条件）下，无论条件是什么都会发生绑定。控制面仍然检查[存储类](https://kubernetes.io/docs/concepts/storage/storage-classes/)、访问模式和请求的存储大小是否有效。

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: foo-pvc
  namespace: foo
spec:
  storageClassName: "" # Empty string must be explicitly set otherwise default StorageClass will be set
  volumeName: foo-pv
  ...
```

此方法不保证对PersistentVolume具有任何绑定特权。如果其他PersistentVolumeClaims可以使用您指定的PV，你需要预先预定该存储卷。在PV的claimRef字段中指定相关的PersistentVolumeClaim，以便其他pvc无法绑定到它。

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: foo-pv
spec:
  storageClassName: ""
  claimRef:
    name: foo-pvc
    namespace: foo
  ...
```

如果你想要使用claimPolicy为Retain的PersistentVolumes, 以及重用现有的PV时，这种方法是非常有用的。

### Expanding Persistent Volumes Claims 扩展持久卷声明
*FEATURE STATE: Kubernetes v1.11 [beta]*

默认情况下，对扩展PersistentVolumeClaims（PVC）的支持现在已启用。您可以扩展以下类型的卷：
* gcePersistentDisk
* awsElasticBlockStore
* Cinder
* glusterfs
* rbd
* Azure File
* Azure Disk
* Portworx
* FlexVolumes
* CSI

只有当PVC的存储类的allowVolumeExpansion字段设置为true时，才能扩展该PVC。

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: gluster-vol-default
provisioner: kubernetes.io/glusterfs
parameters:
  resturl: "http://192.168.10.100:8080"
  restuser: ""
  secretNamespace: ""
  secretName: ""
allowVolumeExpansion: true
```

要为PVC请求更大的容量，请编辑PVC对象并指定更大的容量。这将触发底层PersistentVolume对应卷进行扩展。不会创建新的PersistentVolume来满足声明。相反，通过调整现有卷的大小来进行扩展。

#### CSI Volume expansion
*FEATURE STATE: Kubernetes v1.16 [beta]*
默认情况下，支持扩展CSI卷是启用的，但它还需要特定的CSI驱动程序来支持卷扩展。有关更多信息，请参阅特定CSI驱动程序的文档

#### Resizing a volume containing a file system 调整包含文件系统的卷的大小
如果文件系统是XFS、Ext3或Ext4，则只能调整包含文件系统的卷的大小。

当卷包含文件系统时，只有在新的Pod以ReadWrite模式使用PersistentVolumeClaim时，才会调整文件系统的大小。文件系统扩展可以在Pod启动时或Pod正在运行且底层文件系统支持在线扩展时完成。

如果将驱动程序的`RequiresFSResize`功能设置为true，则FlexVolumes允许调整大小。FlexVolume可以在Pod重新启动时调整大小。

#### Resizing an in-use PersistentVolumeClaim
*FEATURE STATE: Kubernetes v1.15 [beta]*
> 注意：扩展正在使用中的PVCs功能从kubernetes1.15开始作为beta版本提供，从1.11版本开始作为alpha版本提供。`ExpandInUsePersistentVolumes`功能必须启用，这对于许多集群的beta特性来说是自动启用的。有关更多信息，请参阅[特性门](https://kubernetes.io/docs/reference/command-line-tools-reference/feature-gates/)文档。

在这种情况下，您不需要删除和重新创建使用现有PVC的Pod或deployment。任何正在使用的PVC文件系统一经扩展，就自动对其Pod可用。此功能对未被Pod或deployment使用的PVC没有影响。在扩展完成之前，必须创建一个使用PVC的Pod。

与其他卷类型类似，FlexVolume卷也可以在Pod使用时进行扩展

> 注意：FlexVolume resize只有在底层驱动程序支持resize时才可能实现

> 注意：扩展EBS卷是一项耗时的操作。另外，每卷的配额是每6小时修改一次

#### Recovering from Failure when Expanding Volumes 

如果扩展底层存储失败，集群管理员可以手动恢复持久卷声明（PVC）状态并取消调整大小的请求。否则，控制器会在没有管理员干预的情况下不断重试调整大小的请求。

1. 使用Retain回收策略标记绑定到PersistentVolumeClaim（PVC）的PersistentVolume（PV）。
2. 删除PVC。由于PV有Retain回收策略 - 我们在重新创建PVC时不会丢失任何数据。
3. 删除PV规范中的claimRef条目，以便新的PVC可以绑定到它。这将使PV可用。
4. 重新创建比PV小的PVC，并将PVC的volumeName字段设置为PV的名称。这将使新PVC与现有PV相互绑定。
5. 别忘了恢复PV的回收策略

## Types of Persistent Volumes 

PersistentVolume类型是作为插件实现的。Kubernetes目前支持以下插件：
* GCEPersistentDisk
* AWSElasticBlockStore
* AzureFile
* AzureDisk
* CSI
* FC (Fibre Channel)
* FlexVolume
* Flocker
* NFS
* iSCSI
* RBD (Ceph Block Device)
* CephFS
* Cinder (OpenStack block storage)
* Glusterfs
* VsphereVolume
* Quobyte Volumes
* HostPath (Single node testing only -- local storage is not supported in any way and WILL NOT WORK in a multi-node cluster)
* Portworx Volumes
* ScaleIO Volumes
* StorageOS

## Persistent Volumes 
每个PV都包含一个spec和status，即卷的规范和状态。PersistentVolume对象的名称必须是有效的DNS子域名。
```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: pv0003
spec:
  capacity:
    storage: 5Gi
  volumeMode: Filesystem
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Recycle
  storageClassName: slow
  mountOptions:
    - hard
    - nfsvers=4.1
  nfs:
    path: /tmp
    server: 172.17.0.2
```

> 注意：在集群中使用PersistentVolume时，可能需要与卷类型相关的帮助程序。在本例中，PersistentVolume的类型是NFS, 然后helper程序/sbin/mount.nfs是支持mounting(安装) NFS文件系统所必需的

### Capacity 

一般来说，一个PV有一个特定的存储容量。这是使用PV的capacity属性设置的。请参阅Kubernetes[资源模型](https://git.k8s.io/community/contributors/design-proposals/scheduling/resources.md)，了解容量的单位。

目前，存储大小是唯一可以设置或请求的资源。未来属性可能包括IOPS、吞吐量等。

### Volume Mode
*FEATURE STATE: Kubernetes v1.18 [stable]*

Kubernetes支持PersistentVolumes的两个卷模式volumeModes：文件系统Filesystem和块Block。

volumeMode是可选的API参数。Filesystem是省略volumeMode参数时使用的默认模式。

带有`volumeMode: Filesystem`的卷被装载到Pods中的一个目录中。如果卷其实是块设备，并且该设备是空的，Kuberneretes会在第一次装入该设备之前在该设备上创建一个文件系统。
 
可以将volumeMode的值设置为Block，以将卷用作原始块设备。这样的卷作为块设备呈现在Pod中，上面没有任何文件系统。此模式有助于为Pod提供访问卷的最快方式，而不需要在Pod和卷之间设置任何文件系统层。另一方面，在Pod中运行的应用程序必须知道如何处理原始块设备。有关如何在Pod中使用`volumeMode: Block`的卷示例，请参阅[原始块卷支持](https://kubernetes.io/docs/concepts/storage/persistent-volumes/#raw-block-volume-support)

### Access Modes 

PersistentVolume可以在资源提供商支持的情况下，以任何方式装载到主机上。如下表所示，提供商将具有不同的能力，每个PV的访问模式可以设置为特定卷支持的特定模式。例如，NFS可以支持多个读/写客户机，但是特定的nfs PV可以在服务器上设置为以只读方式导出。每个PV都有自己的一组访问模式来描述特定PV的能力。

访问模式有：
* ReadWriteOnce——卷可以由单个节点以读写方式装载
* ReadOnlyMany——卷可以由多个节点以只读方式装入
* ReadWriteMany——卷可以由多个节点以读写方式装载

在CLI中，访问模式缩写为：
* RWO - ReadWriteOnce
* ROX - ReadOnlyMany
* RWX - ReadWriteMany

:warning: 重要！一次只能使用一种访问模式装入卷，即使它支持多种访问模式。例如，GCEPersistentDisk可以由单个节点作为ReadWriteOnce装载，也可以由多个节点以readonly的形式挂载，但不能同时使用这两种模式。

Volume Plugin | ReadWriteOnce | ReadOnlyMany | ReadWriteMany
--------------|---------------|--------------|--------------
AWSElasticBlockStore | ✓ | - | -
AzureFile | ✓ | ✓ | ✓
AzureDisk | ✓ | - | -
CephFS | ✓ | ✓ | ✓
Cinder | ✓ | - | -
CSI | depends on the driver | depends on the driver | depends on the driver
FC | ✓ | ✓ | -
FlexVolume | ✓ | ✓ | depends on the driver
Flocker | ✓ | - | -
GCEPersistentDisk | ✓ | ✓ | -
Glusterfs | ✓ | ✓ | ✓
HostPath | ✓ | - | -
iSCSI | ✓ | ✓ | -
Quobyte | ✓ | ✓ | ✓
NFS | ✓ | ✓ | ✓
RBD | ✓ | ✓ | -
VsphereVolume | ✓ | - | - (works when Pods are collocated)
PortworxVolume | ✓ | - | ✓
ScaleIO | ✓ | ✓ | -
StorageOS | ✓ | - | -


### Class

PV可以有一个类，通过将storageClassName属性设置为[StorageClass](https://kubernetes.io/docs/concepts/storage/storage-classes/)的名称来指定。特定类的PV只能绑定到请求该类的PVC上。没有storageClassName的PV没有类，只能绑定到不请求特定类的PVC上。

在过去，使用注释`volume.beta.kubernetes.io/storage-class`代替storageClassName属性。目前这个注释仍然有效；但是，在将来的Kubernetes版本中，它将被完全弃用

### Reclaim Policy 回收策略

当前回收策略包括：
* Retain —— 人工回收
* Recycle —— 基本清理（`rm -rf /thevolume/*`）
* Delete —— 删除关联的存储资产，如AWS EBS、GCE PD、Azure Disk或OpenStack Cinder volume

目前，只有NFS和HostPath支持recycling。AWS EBS、GCE PD、Azure Disk和Cinder卷支持删除

### Mount Options 装载选项
Kubernetes管理员可以在节点上装载持久卷时指定其他额外的装载选项。

> 注意：不是所有的持久卷Persistent Volume类型都支持mount options。

The following volume types support mount options:
* AWSElasticBlockStore
* AzureDisk
* AzureFile
* CephFS
* Cinder (OpenStack block storage)
* GCEPersistentDisk
* Glusterfs
* NFS
* Quobyte Volumes
* RBD (Ceph Block Device)
* StorageOS
* VsphereVolume
* iSCSI

装载选项是不验证的，所以如果一个选项无效，装载就会失败。

在过去，使用注释`volume.beta.kubernetes.io/mount-options`，而不是mountOptions属性。但是这个注释仍然有效；不过在将来的Kubernetes版本中，它将被完全弃用。

### Node Affinity 节点亲和性

:notebook: 注意：对于大多数卷类型，不需要设置此字段。它是自动为AWS EBS、GCE PD和Azure磁盘卷块类型填充的。您需要为本地卷显式设置此值。

PV可以指定[节点亲和性](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#volumenodeaffinity-v1-core)来定义限制，即定义可以从哪些节点访问该卷的约束。使用PV的pod只能调度到由节点亲和性选择的节点上。

### Phase

卷将分为以下阶段之一：
* Available——尚未绑定到声明的免费资源
* Bound —— 卷绑定到声明
* Released——声明已被删除，但集群尚未回收资源
* Failed ——卷的自动回收失败

CLI将显示绑定到PV的PVC的名称。


## PersistentVolumeClaims

每个PVC都包含一个spec和status，即声明的规格和状态。PersistentVolumeClaim对象的名称必须是有效的DNS子域名。

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: myclaim
spec:
  accessModes:
    - ReadWriteOnce
  volumeMode: Filesystem
  resources:
    requests:
      storage: 8Gi
  storageClassName: slow
  selector:
    matchLabels:
      release: "stable"
    matchExpressions:
      - {key: environment, operator: In, values: [dev]}
```

### Access Modes

声明在请求具有特定访问模式的存储时使用与卷相同的约定。

### Volume Modes

声明使用与卷相同的约定来指示卷作为文件系统或块设备。

### Resources

声明，像Pods一样，可以请求特定数量的资源。在本例中，请求用于存储。相同的[资源模型](https://git.k8s.io/community/contributors/design-proposals/scheduling/resources.md)适用于卷和声明

### Selector 

声明可以指定标签选择器来进一步筛选卷集。只有标签与选择器匹配的卷才能绑定到声明。选择器可以由两个字段组成：
* matchLabels—卷必须具有与此值一样的标签
* matchExpressions—通过指定键、值列表和与键和值相关的运算符而生成的列表。有效运算符包括In、NotIn、Exists和DoesNotExist。

来自matchLabels和matchExpressions的所有条件都是“and”组合在一起的，它们必须全部满足才能匹配。

### Class 
声明可以通过使用属性storageClassName指定[StorageClass](https://kubernetes.io/docs/concepts/storage/storage-classes/)的名称来请求特定的类。只有对应请求类的pv（与PVC具有相同storageClassName的pv）才能绑定到PVC。

pvc不一定要请求一个类。storageClassName设置为`""`的PVC始终被解释为请求一个没有类的PV，因此它只能绑定到没有类的PV（没有注释或一个storageClassName等于`""`）。没有storageClassName的PVC并不完全相同，集群对它的处理也不同，这取决于是否打开了[DefaultStorageClass许可插件](https://kubernetes.io/docs/reference/access-authn-authz/admission-controllers/#defaultstorageclass)。

- 如果启用了许可插件，管理员可以指定一个默认的StorageClass。所有没有storageClassName的pvc只能绑定到该默认的pv。通过设置StorageClass对象的`storageclass.kubernetes.io/is-default-class`为true将StorageClass设置为默认类。如果管理员没有指定默认值，集群会认为许可插件关闭了，然后响应PVC的创建。如果指定了多个默认值，则许可插件将禁止创建所有pvc。
- 如果关闭了许可插件，就没有默认StorageClass的概念。所有没有storageClassName的pvc只能绑定到没有类的pv。在本例中，没有storageClassName的pvc与将其storageClassName设置为`""`的pvc的处理方式相同。

根据安装方法的不同，默认的StorageClass可能会在安装期间由addon manager部署到Kubernetes集群。

当PVC除了请求一个StorageClass之外还指定了一个selector时，这些需求被“and”组合在一起：只有拥有请求类和请求标签的PV才能绑定到PVC上。

> 注意：目前，带有非空选择器的PVC不能动态地配置PV。

在过去，使用注释`volume.beta.kubernetes.io/storage-class`，而不是storageClassName属性。这个注释仍然有效；但是，在将来的Kubernetes版本中不会支持它。

## Claims As Volumes 

Pods通过使用声明作为卷来访问存储。声明必须与使用声明的Pod位于相同的命名空间中。集群在Pod的名称空间中找到声明，并使用它获取声明对应的PersistentVolume。然后将卷装载到主机和Pod中。

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: mypod
spec:
  containers:
    - name: myfrontend
      image: nginx
      volumeMounts:
      - mountPath: "/var/www/html"
        name: mypd
  volumes:
    - name: mypd
      persistentVolumeClaim:
        claimName: myclaim
```

### 关于命名空间

PersistentVolumes绑定是独占的，由于PersistentVolumeClaims是命名空间的对象，因此只能在一个命名空间中使用“Many”模式（ROX、RWX）装载声明。

## Raw Block Volume Support 原始块卷支持
*FEATURE STATE: Kubernetes v1.18 [stable]*
以下卷插件支持原始块卷，包括适用的动态资源调配：
* AWSElasticBlockStore
* AzureDisk
* CSI
* FC (Fibre Channel)
* GCEPersistentDisk
* iSCSI
* Local volume
* OpenStack Cinder
* RBD (Ceph Block Device)
* VsphereVolume

### PersistentVolume using a Raw Block Volume

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: block-pv
spec:
  capacity:
    storage: 10Gi
  accessModes:
    - ReadWriteOnce
  volumeMode: Block
  persistentVolumeReclaimPolicy: Retain
  fc:
    targetWWNs: ["50060e801049cfd1"]
    lun: 0
    readOnly: false
```

### PersistentVolumeClaim requesting a Raw Block Volume 
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: block-pvc
spec:
  accessModes:
    - ReadWriteOnce
  volumeMode: Block
  resources:
    requests:
      storage: 10Gi
```

### Pod specification adding Raw Block Device path in container  在容器中添加原始块设备路径的Pod规范
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: pod-with-block-volume
spec:
  containers:
    - name: fc-container
      image: fedora:26
      command: ["/bin/sh", "-c"]
      args: [ "tail -f /dev/null" ]
      volumeDevices:
        - name: data
          devicePath: /dev/xvda
  volumes:
    - name: data
      persistentVolumeClaim:
        claimName: block-pvc
```

:warning: 注意：为Pod添加原始块设备时，请在容器中指定设备路径device path，而不是装载路径mount path。

### Binding Block Volumes 

如果用户通过使用PersistentVolumeClaim规范中的volumeMode字段来指示原始块卷，则绑定规则与不将此模式视为规范一部分的早期版本略有不同。下表列出的是当用户和管理员请求原始块设备时可能指定的组合。该表指示是否将绑定卷，静态配置卷的卷绑定规则如下：
PV volumeMode | PVC volumeMode | Result
--------------|----------------|-------
unspecified | unspecified | BIND
unspecified | Block | NO BIND
unspecified | Filesystem | BIND
Block | unspecified | NO BIND
Block | Block | BIND
Block | Filesystem | NO BIND
Filesystem | Filesystem | BIND
Filesystem | Block | NO BIND
Filesystem | unspecified | BIND

:warning: 注意：alpha版本只支持静态配置的卷。管理员在使用原始块设备时应注意考虑这些值

## Volume Snapshot and Restore Volume from Snapshot Support 卷快照和从快照还原卷
*FEATURE STATE: Kubernetes v1.17 [beta]*
卷快照功能仅支持CSI卷插件。有关详细信息，请参阅[卷快照](https://kubernetes.io/docs/concepts/storage/volume-snapshots/)。

要启用从卷快照数据源还原卷的功能，请在apiserver和controller manager上启用VolumeSnapshotDataSource特性

### Create a PersistentVolumeClaim from a Volume Snapshot 从卷快照创建PersistentVolumeClaim
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: restore-pvc
spec:
  storageClassName: csi-hostpath-sc
  dataSource:
    name: new-snapshot-test
    kind: VolumeSnapshot
    apiGroup: snapshot.storage.k8s.io
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
```

## Volume Cloning

[卷克隆](https://kubernetes.io/docs/concepts/storage/volume-pvc-datasource/)仅适用于CSI卷插件

### Create PersistentVolumeClaim from an existing PVC 
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: cloned-pvc
spec:
  storageClassName: my-csi-plugin
  dataSource:
    name: existing-src-pvc-name
    kind: PersistentVolumeClaim
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
```

## Writing Portable Configuration
如果您编写的配置模板或示例需要广泛地在群集上运行并需要持久存储，建议您使用以下模式：
- 在配置包bundle of config（与Deployments、ConfigMaps等一起）中包括PersistentVolumeClaim对象。
- 不要在配置中包含PersistentVolume对象，因为实例化配置的用户可能没有创建PersistentVolume的权限
- 在实例化模板时，为用户提供存储类名的选择项。
  - 如果用户提供了一个存储类名，则将该值放入persistentVolumeClaim.storageClassName字段。如果集群管理员启用了StorageClasses，则PVC与对应的存储类匹配。
  - 如果用户没有提供存储类名，请保留persistentVolumeClaim.storageClassName字段为nil。这将使PV自动配置集群中默认的StorageClass。许多群集环境都安装了默认StorageClass，或者管理员可以创建自己的默认StorageClass。
- 在您的工具中，注意一段时间后没有绑定的pvc，并向用户展示这一点，因为这可能表明集群没有动态存储支持（在这种情况下，用户应该创建一个匹配的PV）或者集群没有存储系统（在这种情况下，用户无法部署需要pvc的配置）。

## What's next
* Learn more about [Creating a PersistentVolume](https://kubernetes.io/docs/tasks/configure-pod-container/configure-persistent-volume-storage/#create-a-persistentvolume).
* Learn more about [Creating a PersistentVolumeClaim](https://kubernetes.io/docs/tasks/configure-pod-container/configure-persistent-volume-storage/#create-a-persistentvolumeclaim).
* Read the [Persistent Storage design document](https://git.k8s.io/community/contributors/design-proposals/storage/persistent-storage.md)




























