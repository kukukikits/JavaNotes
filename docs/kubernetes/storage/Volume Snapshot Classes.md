# Volume Snapshot Classes

本文描述了Kubernetes中VolumeSnapshotClass的概念。建议熟悉卷快照和存储类

## Introduction

就像StorageClass为管理员提供了一种方法来描述他们在配置卷时提供的存储“类”，VolumeSnapshotClass在配置卷快照时提供了一种描述存储“类”的方法.

## VolumeSnapshotClass资源

每个VolumeSnapshotClass都包含字段driver、deletionPolicy和parameters，当需要动态配置属于该类的VolumeSnapshot时将使用这些字段。

VolumeSnapshotClass对象的名称非常重要，它是用户请求特定类的依据。管理员在第一次创建VolumeSnapshotClass对象时设置类的名称和其他参数，对象一旦创建就无法更新。

```yaml
apiVersion: snapshot.storage.k8s.io/v1beta1
kind: VolumeSnapshotClass
metadata:
  name: csi-hostpath-snapclass
driver: hostpath.csi.k8s.io
deletionPolicy: Delete
parameters:
```

管理员可以通过添加`snapshot.storage.kubernetes.io/is-default-class: "true"`的注释来为那些没有请求任何类绑定的VolumeSnapshots提供默认的VolumeSnapshotClass：

```yaml
apiVersion: snapshot.storage.k8s.io/v1beta1
kind: VolumeSnapshotClass
metadata:
  name: csi-hostpath-snapclass
  annotations:
    snapshot.storage.kubernetes.io/is-default-class: "true"
driver: hostpath.csi.k8s.io
deletionPolicy: Delete
parameters:
```

### Driver

卷快照类有一个驱动程序，该驱动程序确定用于配置卷快照的CSI卷插件。必须指定此字段。

### DeletionPolicy

卷快照类具有删除策略。用来决定当VolumeSnapshot对象被删除时，与之绑定的VolumeSnapshotContent如何进行处理。卷快照的删除策略可以是保留Retain或删除Delete。必须指定此字段。

如果deletionPolicy为Delete，则底层存储快照将与VolumeSnapshotContent对象一起删除。如果deletionPolicy为Retain，则底层快照和VolumeSnapshotContent都将保留。


## Parameters
卷快照类具有描述属于卷快照类的卷快照的参数。根据驱动器的不同，可以接受不同的参数。

