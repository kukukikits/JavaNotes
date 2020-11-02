# CSI Volume Cloning

本文档描述了克隆Kubernetes中现有CSI卷的概念。建议先熟悉[卷](https://kubernetes.io/docs/concepts/storage/volumes)

## Introduction
- CSI: The Container Storage Interface (CSI) defines a standard interface to expose storage systems to containers.

[CSI](https://kubernetes.io/docs/concepts/storage/volumes/#csi)卷克隆功能增加在dataSource字段中指定现有pvc的支持，用来指示用户想要克隆卷。

克隆被定义为现有Kubernetes卷的副本，可以像任何标准卷一样使用。唯一的区别是，在配置时，后端设备将创建指定卷的精确副本，而不是创建“新”空卷。

从kubernetes api的角度来看，克隆的实现只是增加了在创建新PVC时将现有PVC指定为数据源的能力。源PVC必须绑定并可用（即不在使用状态）。

用户在使用此功能时需要注意以下几点：
- 克隆支持（VolumePVCDataSource）仅适用于CSI驱动程序。
- 克隆支持仅适用于动态配置。
- CSI驱动程序可能实现了也可能没有实现卷克隆功能
- 只有当源PVC与目标PVC位于同一命名空间中时，才能克隆该PVC（源和目标必须位于同一命名空间中）。
- 仅在同一存储类中支持克隆。
  - 目标卷与源卷的存储类storage class必须相同
  - 可以使用默认存储类，规范中省略了storageClassName
- 只能在使用相同卷模式设置的两个卷之间执行克隆（如果你请求了一个块模式block mode卷，则源卷也必须为块模式）

## Provisioning 配置

除了添加同一命名空间中现有PVC的数据源引用外，克隆的配置与任何其他PVC一样。

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
    name: clone-of-pvc-1
    namespace: myns
spec:
  accessModes:
  - ReadWriteOnce
  storageClassName: cloning
  resources:
    requests:
      storage: 5Gi
  dataSource:
    kind: PersistentVolumeClaim
    name: pvc-1
```

:notebook: 注意：必须指定`spec.resources.requests.storage`的值，并且指定的值必须等于或大于源卷的容量。

其结果是一个名为clone-of-PVC-1的新PVC，其数据内容与指定源PVC-1完全相同。

## Usage 
克隆的PVC就是一个新的PVC，其使用方式和其他PVC一样。在这一点上，新创建的PVC也是一个独立的对象。它可以独立使用、克隆、创建快照或删除，而不必考虑它的原始数据源PVC。这也意味着源没有以任何方式链接到新创建的克隆上，也可以在不影响新创建的克隆的情况下对源进行修改或删除。


