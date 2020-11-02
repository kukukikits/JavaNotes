# Dynamic Volume Provisioning

动态卷配置允许按需创建存储卷。如果没有动态资源调配，集群管理员必须手动调用他们的云或存储提供商来创建新的存储卷，然后创建PersistentVolume对象以在Kubernetes中表示存储卷。动态资源调配功能消除了群集管理员预先调配存储的需要。相反，它会在用户请求时自动提供存储空间。

## Background 
动态卷配置的实现基于API group `storage.k8s.io`中的API对象StorageClass。群集管理员可以根据需要定义任意多个StorageClass对象，每个对象都指定一个卷插件（也称为provisioner），该插件提供卷以及在配置时传递给该配置器的一组参数。群集管理员可以在群集中定义和公开多种类型的存储（来自相同或不同的存储系统），每种类型都有一组自定义参数。这种设计还确保最终用户不必担心存储资源调配方式的复杂性和细微差别，但仍然能够从多个可选的存储中进行选择。

More information on storage classes can be found [here](https://kubernetes.io/docs/concepts/storage/storage-classes/).

## Enabling Dynamic Provisioning

要启用动态资源调配，群集管理员需要为用户预创建一个或多个StorageClass对象。StorageClass对象定义在调用动态资源调配时应该使用哪个供应器以及应该向该供应器传递哪些参数。StorageClass对象的名称必须是有效的DNS子域名。

下面的清单创建了一个存储类“slow”，该类提供标准磁盘（如持久磁盘）。

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: slow
provisioner: kubernetes.io/gce-pd
parameters:
  type: pd-standard
```

下面的清单创建了一个存储类“fast”，它提供类似SSD的持久磁盘。

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: fast
provisioner: kubernetes.io/gce-pd
parameters:
  type: pd-ssd
```

## Using Dynamic Provisioning

用户通过在PersistentVolumeClaim中指定一个存储类来请求动态配置的存储。在kubernetes v1.6之前，这是通过`volume.beta.kubernetes.io/storage-class`注释实现的。但是，此注释自v1.6起已被弃用。用户现在可以而且应该使用PersistentVolumeClaim对象的storageClassName字段。此字段的值必须与管理员配置的StorageClass的名称匹配（请参见下文）。

例如，要选择“fast”存储类，用户将创建以下PersistentVolumeClaim：

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: claim1
spec:
  accessModes:
    - ReadWriteOnce
  storageClassName: fast
  resources:
    requests:
      storage: 30Gi
```

此声明的结果就是自动配置类似SSD的持久磁盘。当声明被删除时，卷将被销毁。

## Defaulting Behavior

可以在群集上启用动态资源调配，以便在未指定存储类的情况下动态调配所有声明。群集管理员可以通过以下方式启用此行为：

- 将一个StorageClass对象标记为默认值；
- 确保在API服务器上启用了[DefaultStorageClass许可控制器](https://kubernetes.io/docs/reference/access-authn-authz/admission-controllers/#defaultstorageclass)。

管理员可以通过添加`storageclass.kubernetes.io/is-default-class`注释给StorageClass把它标记为默认类。当群集中存在默认的StorageClass，并且用户创建未指定storageClassName的PersistentVolumeClaim时，DefaultStorageClass许可控制器会自动添加指向默认存储类的storageClassName字段。

请注意，集群上最多可以有一个默认存储类，否则无法创建没有显式指定storageClassName的PersistentVolumeClaim。

## Topology Awareness 

在多区域集群中，Pods可以分布在一个Region的各个Zone。一个Zone中的存储后端应该在调度pod的Zone中进行配置。这可以通过设置卷[绑定模式](https://kubernetes.io/docs/concepts/storage/storage-classes/#volume-binding-mode)来完成。








