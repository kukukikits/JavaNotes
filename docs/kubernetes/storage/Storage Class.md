# Storage Classes

本文描述了Kubernetes中StorageClass的概念。建议熟悉卷和持久卷

## Introduction

StorageClass为管理员提供了一种描述存储“类”的方法。不同的类可能映射到不同的服务质量级别、备份策略或由群集管理员确定的任意策略。Kubernetes本身并不关心类代表什么。在其他存储系统中，此概念有时称为“配置文件”。

## The StorageClass Resource
每个StorageClass都包含字段provisioner、parameters和reclaimPolicy，当属于该类的PersistentVolume需要动态配置时，将使用这些字段。

StorageClass对象的名称非常重要，它是用户请求特定类的方式。管理员在第一次创建StorageClass对象时设置类的名称和其他参数，对象一旦创建就无法更新。

管理员可以为不请求任何特定类的pvc指定一个默认的StorageClass：有关详细信息，请参阅[PersistentVolumeClaim部分](https://kubernetes.io/docs/concepts/storage/persistent-volumes/#persistentvolumeclaims)。

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: standard
provisioner: kubernetes.io/aws-ebs
parameters:
  type: gp2
reclaimPolicy: Retain
allowVolumeExpansion: true
mountOptions:
  - debug
volumeBindingMode: Immediate
```

### Provisioner

每个StorageClass都有一个provisioner，它决定使用哪个卷插件来配置PV。必须指定此字段。
Volume Plugin | Internal Provisioner | Config Example
--------------|----------------------|---------------
AWSElasticBlockStore | ✓ | [AWS EBS](https://kubernetes.io/docs/concepts/storage/storage-classes/#aws-ebs)
AzureFile | ✓ | [Azure File](https://kubernetes.io/docs/concepts/storage/storage-classes/#azure-file)
AzureDisk | ✓ | [Azure Disk](https://kubernetes.io/docs/concepts/storage/storage-classes/#azure-disk)
CephFS | - | -
Cinder | ✓ | [OpenStack Cinder](https://kubernetes.io/docs/concepts/storage/storage-classes/#openstack-cinder)
FC | - | -
FlexVolume | - | -
Flocker | ✓ | -
GCEPersistentDisk | ✓ | [GCE PD](https://kubernetes.io/docs/concepts/storage/storage-classes/#gce-pd)
Glusterfs | ✓ | [Glusterfs](https://kubernetes.io/docs/concepts/storage/storage-classes/#glusterfs)
iSCSI | - | -
Quobyte | ✓ | [Quobyte](https://kubernetes.io/docs/concepts/storage/storage-classes/#quobyte)
NFS | - | -
RBD | ✓ | [Ceph RBD](https://kubernetes.io/docs/concepts/storage/storage-classes/#ceph-rbd)
VsphereVolume | ✓ | [vSphere](https://kubernetes.io/docs/concepts/storage/storage-classes/#vsphere)
PortworxVolume | ✓ | [Portworx Volume](https://kubernetes.io/docs/concepts/storage/storage-classes/#portworx-volume)
ScaleIO | ✓ | [ScaleIO](https://kubernetes.io/docs/concepts/storage/storage-classes/#scaleio)
StorageOS | ✓ | [StorageOS](https://kubernetes.io/docs/concepts/storage/storage-classes/#storageos)
Local | - | [Local](https://kubernetes.io/docs/concepts/storage/storage-classes/#local)


您不限于使用此处列出的“内部”provisioner（它们的名字都是`kubernetes.io`开头的，同kubernetes一起发行的）。您还可以运行并指定外部供应器，这些外部供应器是遵循Kubernetes定义的[规范](https://git.k8s.io/community/contributors/design-proposals/storage/volume-provisioning.md)的独立程序。外部供应器的作者决定了供应器的代码放在哪里，如何传输，如何运行，以及使用什么卷插件（包括Flex）等。存储库[kubernetes-sigs/sig-storage-lib-external-provisioner](https://github.com/kubernetes-sigs/sig-storage-lib-external-provisioner)包含一个用于编写实现大部分规范的外部供应器的库。一些外部供应器列在存储库 [kubernetes-sigs/external-storage](https://github.com/kubernetes-sigs/external-dns)里。

例如，NFS不提供内部供应器，但可以使用外部供应器。也有第三方存储供应商提供自己的外部供应器的情况。

### Reclaim Policy

由StorageClass动态创建的PersistentVolumes将拥有存储类的reclaimPolicy字段中指定的回收策略，该策略可以是Delete或Retain。如果在创建StorageClass对象时未指定回收策略，则默认为Delete。

手动创建并通过StorageClass管理的PersistentVolume具有的回收策略是在创建时分配的。

### Allow Volume Expansion
*FEATURE STATE: Kubernetes v1.11 [beta]*
PersistentVolumes可以配置为可扩展。此功能设置为true时，允许用户通过编辑相应的PVC对象来调整卷的大小。

当底层StorageClass将allowVolumeExpansion字段设置为true时，以下类型的卷支持卷扩展。
Volume type | Required Kubernetes version
------------|----------------------------
gcePersistentDisk | 1.11
awsElasticBlockStore | 1.11
Cinder | 1.11
glusterfs | 1.11
rbd | 1.11
Azure File | 1.11
Azure Disk | 1.11
Portworx | 1.11
FlexVolume | 1.13
CSI | 1.14 (alpha), 1.16 (beta)

:notebook: 注意：只能使用卷扩展功能来增大卷，而不能收缩它。

### Mount Options 装载选项

由StorageClass动态创建的PersistentVolumes拥有该存储类在mountOptions字段中指定的装载选项。

如果卷插件不支持装载选项，但指定了装载选项，则配置失败。装载选项在类或PV上都不会被验证，所以只要其中一个选项无效，PV的装载就会失败。

### Volume Binding Mode 卷绑定模式

volumeBindingMode字段控制何时应该进行[卷绑定和动态配置](https://kubernetes.io/docs/concepts/storage/persistent-volumes/#provisioning)。

默认情况下，Immediate模式指示一旦创建PersistentVolumeClaim，就会进行卷绑定和动态配置。对于拓扑受限且不能从群集中的所有节点全局访问的存储后端，绑定或调配PersistentVolumes时不会考虑Pod的调度。这可能会导致pod不能被调度。

集群管理员可以通过指定WaitForFirstConsumer模式来解决这个问题，该模式将延迟PersistentVolume的绑定和配置，直到创建了一个使用PersistentVolumeClaim的Pod。PersistentVolumes将根据Pod的调度约束指定的拓扑进行选择或配置。这些包括但不限于[资源需求](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/)、[节点选择器](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/#nodeselector)、[pod亲和力和反亲和力](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/#affinity-and-anti-affinity)，以及[污染和容忍](https://kubernetes.io/docs/concepts/scheduling-eviction/taint-and-toleration)。

以下插件的动态配置支持WaitForFirstConsumer：

* [AWSElasticBlockStore](https://kubernetes.io/docs/concepts/storage/storage-classes/#aws-ebs)
* [GCEPersistentDisk](https://kubernetes.io/docs/concepts/storage/storage-classes/#gce-pd)
* [AzureDisk](https://kubernetes.io/docs/concepts/storage/storage-classes/#azure-disk)

以下插件在已经创建好了PersistentVolume绑定的时候支持WaitForFirstConsumer：
* All of the above
* [Local](https://kubernetes.io/docs/concepts/storage/storage-classes/#local)

*FEATURE STATE: Kubernetes v1.17 [stable]*

动态配置和预先创建的pv也支持CSI卷，但是您需要查看特定CSI驱动程序的文档，以查看其支持的拓扑键和示例

### Allowed Topologies 允许的拓扑

当集群操作员指定WaitForFirstConsumer卷绑定模式时，在大多数情况下，不再需要将配置限制到特定拓扑。但是，如果仍然需要，可以指定allowedTopologies。

此示例演示如何将已配置卷的拓扑限制为特定区域，并使用它来代替插件的zone和zones参数。

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: standard
provisioner: kubernetes.io/gce-pd
parameters:
  type: pd-standard
volumeBindingMode: WaitForFirstConsumer
allowedTopologies:
- matchLabelExpressions:
  - key: failure-domain.beta.kubernetes.io/zone
    values:
    - us-central1-a
    - us-central1-b
```

## Parameters

存储类有一些用于描述属于该存储类的卷的参数。根据供应器的不同，可以接受不同的参数。例如，参数type的值io1和参数iopsPerGB仅对EBS有效。当参数被省略时，将使用一些默认值。

最多可以为StorageClass定义512个参数。参数对象的总长度（包括其键和值）不能超过256 KiB

### AWS EBS
```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: slow
provisioner: kubernetes.io/aws-ebs
parameters:
  type: io1
  iopsPerGB: "10"
  fsType: ext4
```

- type: io1, gp2, sc1, st1. See [AWS docs](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/EBSVolumeTypes.html) for details. Default: gp2.
- zone（已废弃）：AWS区域。如果既没有指定zone，也没有指定zones，那么卷通常在kubernetes集群中有节点存在的活动区域之间使用round-robin循环。zone和zones参数不能同时使用。
- zones（已废弃）：以逗号分隔的AWS zone(s)列表。如果既没有指定zone，也没有指定zones，那么卷通常在kubernetes集群中有节点存在的活动区域之间使用round-robin循环。zone和zones参数不能同时使用。
- iopsPerGB：仅适用于io1卷。即每秒1 GiB的I/O操作数。AWS volume plugin将其乘以请求卷的大小，来计算卷的IOPS，并将其限制在20000 IOPS（AWS支持的最大值，请参阅[AWS文档](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/EBSVolumeTypes.html)）。这里需要的是一个字符串，即“10”，而不是数字10。
- fsType：kubernetes支持的fsType文件系统类型。默认值：“ext4”。
- encrypted：指示是否应加密EBS卷。有效值为“true”或“false”。这里需要一个字符串，即“true”，而不是true。
- kmsKeyId：可选。加密卷时要使用的密钥的完整Amazon资源名称。如果没有提供但encrypted为true，则AWS将生成一个密钥。有关有效的ARN值，请参阅AWS文档。

:warning: 注意：zone 和zones 参数已弃用，并被[allowedTopologies](https://kubernetes.io/docs/concepts/storage/storage-classes/#allowed-topologies)替换

### GCE PD
### Glusterfs
### OpenStack Cinder
### vSphere
### Ceph RBD
### Quobyte
### Azure Disk
### Azure File
### Portworx Volume
### ScaleIO
### StorageOS
### Local
*FEATURE STATE: Kubernetes v1.14 [stable]*
```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: local-storage
provisioner: kubernetes.io/no-provisioner
volumeBindingMode: WaitForFirstConsumer
```

本地卷当前不支持动态资源调配，但是仍应创建一个StorageClass以将卷绑定延迟到Pod调度的时候。使用WaitForFirstConsumer卷绑定模式实现这一目的。

延迟卷绑定允许调度器在为PersistentVolumeClaim选择适当的PersistentVolume时考虑Pod的所有调度约束。











