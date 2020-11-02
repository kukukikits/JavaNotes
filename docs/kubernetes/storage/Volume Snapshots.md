# Volume Snapshots
*FEATURE STATE: Kubernetes v1.17 [beta]*
在Kubernetes中，卷快照表示存储系统上卷的快照。本文假设您已经熟悉Kubernetes[持久卷](https://kubernetes.io/docs/concepts/storage/persistent-volumes/)。

## Introduction

与如何使用API资源PersistentVolume和PersistentVolumeClaim为用户和管理员配置卷类似，VolumeSnapshotContent和VolumeSnapshot API资源用于为用户和管理员创建卷快照。

`VolumeSnapshotContent`是群集中由管理员配置的卷的快照。它和PersistentVolume一样都是集群中的资源。

`VolumeSnapshot`是用户对卷快照的请求。它类似于PersistentVolumeClaim。

`VolumeSnapshotClass`允许您指定属于VolumeSnapshot的不同属性。这些属性在存储系统上的同一卷的快照之间可能有所不同，因此无法使用PersistentVolumeClaim的相同StorageClass来表示。

用户在使用此功能时需要注意以下几点：
- API对象VolumeSnapshot、VolumeSnapshotContent和VolumeSnapshotClass是[CRD](https://kubernetes.io/docs/tasks/extend-kubernetes/custom-resources/custom-resource-definitions/)，而不是核心API的一部分。
- VolumeSnapshot功能仅适用于CSI驱动程序。
- 作为VolumeSnapshot测试版中部署过程的一部分，Kubernetes团队提供了一个快照控制器来部署到控制平面中，以及一个称为csi-snapshotter的sidecar辅助容器将与csi驱动程序一起部署。快照控制器监视VolumeSnapshot和VolumeSnapshotContent对象，并负责在动态配置中创建和删除VolumeSnapshotContent对象。sidecar csi-snapshotter监视VolumeSnapshotContent对象，并针对csi端点触发CreateSnapshot和DeleteSnapshot操作。
- CSI驱动程序可能实现了卷快照功能，也可能没有实现。为卷快照提供支持的CSI驱动程序可能会使用csi-snapshotter。有关详细信息，请参阅[CSI驱动程序文档](https://kubernetes-csi.github.io/docs/)。
- CRD和快照控制器的安装由Kubernetes发行版负责

## Lifecycle of a volume snapshot and volume snapshot content
VolumeSnapshotContents是群集中的资源。VolumeSnapshots是对这些资源的请求。volumeSnapshotContents和VolumeSnapshots之间的交互遵循此生命周期:

### Provisioning Volume Snapshot 配置卷快照
有两种方法可以配置快照：预配置或动态配置。
1. Pre-provisioned
群集管理员创建许多VolumeSnapshotContents。它们包含存储系统上真实卷快照的详细信息，供群集用户使用。它们存在于kubernetes api中，且可供消费。

2. Dynamic 
您可以动态地请求一个从PersistentVolumeClaim生成的快照，而不是使用预先存在的快照。VolumeSnapshotClass指定在拍摄快照时要使用的特定于存储提供程序的参数。

### Binding 
在预配置和动态配置场景中，快照控制器处理VolumeSnapshot对象与适当的VolumeSnapshotContent对象的绑定。绑定是一对一的映射。

在预配置绑定的情况下，在创建请求的VolumeSnapshotContent对象之前，VolumeSnapshot将保持未绑定状态

### Persistent Volume Claim as Snapshot Source Protection  作为快照源保护的持久卷声明
此保护的目的是确保正在使用的PersistentVolumeClaim API对象在从系统中获取快照时不会从系统中删除（因为移除可能会导致数据丢失）。

在对PersistentVolumeClaim生成快照时，该PersistentVolumeClaim正在使用中。如果删除正在作为快照源使用的PersistentVolumeClaim API对象，则不会立即删除PersistentVolumeClaim对象。相反，PersistentVolumeClaim对象的删除将推迟到快照已经可以使用readyToUse或中止aborted的时候。

### Delete 
删除VolumeSnapshot对象时触发删除，随后将执行删除策略。如果删除策略为Delete，则底层存储快照将与VolumeSnapshotContent对象一起删除。如果删除策略为保留，则底层快照和VolumeSnapshotContent都将保留

## VolumeSnapshots
每个卷快照都包含一个spec和一个status

```yaml
apiVersion: snapshot.storage.k8s.io/v1beta1
kind: VolumeSnapshot
metadata:
  name: new-snapshot-test
spec:
  volumeSnapshotClassName: csi-hostpath-snapclass
  source:
    persistentVolumeClaimName: pvc-test
```

persistentVolumeClaimName是快照的PersistentVolumeClaim数据源的名称。动态配置快照时需要此字段。

卷快照可以通过使用属性volumeSnapshotClassName指定[VolumeSnapshotClass](https://kubernetes.io/docs/concepts/storage/volume-snapshot-classes/)的名称来请求特定的类。如果没有设置任何内容，则使用默认类（如果可用）。

对于预配置的快照，您需要指定volumeSnapshotContentName作为快照的源，如下例所示。预配置的快照需要volumeSnapshotContentName源字段。

```yaml
apiVersion: snapshot.storage.k8s.io/v1beta1
kind: VolumeSnapshot
metadata:
  name: test-snapshot
spec:
  source:
    volumeSnapshotContentName: test-content
```

## Volume Snapshot Contents 
每个 VolumeSnapshotContent都包含一个 spec 和status。在动态资源调配中，快照公用控制器创建VolumeSnapshotContent对象。下面是一个例子：

```yaml
apiVersion: snapshot.storage.k8s.io/v1beta1
kind: VolumeSnapshotContent
metadata:
  name: snapcontent-72d9a349-aacd-42d2-a240-d775650d2455
spec:
  deletionPolicy: Delete
  driver: hostpath.csi.k8s.io
  source:
    volumeHandle: ee0cfb94-f8d4-11e9-b2d8-0242ac110002
  volumeSnapshotClassName: csi-hostpath-snapclass
  volumeSnapshotRef:
    name: new-snapshot-test
    namespace: default
    uid: 72d9a349-aacd-42d2-a240-d775650d2455
```

volumeHandle是在存储后端创建的卷的唯一标识符，在卷创建过程中由CSI驱动程序返回。动态配置快照时需要此字段。它指定了快照的卷源。

对于预配置的快照，您（作为群集管理员）负责创建VolumeSnapshotContent对象，如下所示。

```yaml
apiVersion: snapshot.storage.k8s.io/v1beta1
kind: VolumeSnapshotContent
metadata:
  name: new-snapshot-content-test
spec:
  deletionPolicy: Delete
  driver: hostpath.csi.k8s.io
  source:
    snapshotHandle: 7bdd0de3-aaeb-11e8-9aae-0242ac110002
  volumeSnapshotRef:
    name: new-snapshot-test
    namespace: default
```
snapshotHandle是在存储后端创建的卷快照的唯一标识符。此字段对于预配置的快照是必需的。它指定此VolumeSnapshotContent表示的存储系统上的CSI快照id。

## Provisioning Volumes from Snapshots
您可以使用PersistentVolumeClaim对象中的dataSource字段来配置一个新卷，该卷预填充了快照中的数据。

For more details, see Volume Snapshot and Restore Volume from Snapshot.
