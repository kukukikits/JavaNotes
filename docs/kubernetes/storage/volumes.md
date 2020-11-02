# Volumes
容器中的磁盘文件是短暂的，这给在容器中运行的应用程序带来了一些问题。首先，当一个容器崩溃时，kubelet将重新启动它，但是文件将丢失 —— 容器以干净的状态启动。其次，当在一个Pod中一起运行容器时，常常需要在这些容器之间共享文件。Kubernetes 卷（Volume）抽象解决了这两个问题。

## Background

Docker也有一个[卷](https://docs.docker.com/storage/)的概念，但是它有点松散，管理较少。在Docker中，卷只是磁盘上或其他容器中的一个目录。生命周期不受管理，直到最近才有本地磁盘备份卷。Docker现在提供卷驱动程序，但功能目前非常有限（例如，从Docker 1.7开始，每个容器只允许一个卷驱动程序，并且无法将参数传递到卷）。

另一方面，一个Kubernetes卷有一个显式的生存期 —— 与enclose（封闭？）它的Pod相同。因此，卷比Pod中运行的任何容器都活得久，并且在容器重新启动时数据将被保留。当然，当一个pod不复存在时，卷也将不复存在。或许更重要的是，Kubernetes支持多种类型的卷，而Pod可以同时使用任意数量的卷。

在其核心，卷只是一个目录，其中可能包含一些数据，Pod中的容器可以访问它。该目录是如何形成的，支持它的介质以及它的内容都由所使用的卷类型决定。

要使用卷，Pod要指定使用什么卷（`.spec.volumes`字段）以及指定要将它们装入容器的位置(`.spec.containers[*].volumeMounts`字段）。

容器中的进程可以看到由Docker镜像和卷组成的文件系统视图。Docker镜像位于文件系统层次结构的根目录，任何卷都安装在镜像中指定的路径上。卷无法装载到其他卷上，也无法与其他卷进行硬链接。Pod中的每个容器都必须独立指定装载每个卷的位置。

## Types of Volumes
Kubernetes支持的几种类型的卷：
* awsElasticBlockStore
* azureDisk
* azureFile
* cephfs
* cinder
* configMap
* csi
* downwardAPI
* emptyDir
* fc (fibre channel)
* flexVolume
* flocker
* gcePersistentDisk
* gitRepo (deprecated)
* glusterfs
* hostPath
* iscsi
* local
* nfs
* persistentVolumeClaim
* projected
* portworxVolume
* quobyte
* rbd
* scaleIO
* secret
* storageos
* vsphereVolume


### cephfs （一种分布式文件系统）

cepfs卷允许将现有的cepfs卷挂载在Pod上。与emptyDir不同，emptyDir在移除Pod时会被删除，cepfs卷的内容会被保留，而该卷只是被卸载。这意味着cepfs卷可以预先填充数据，并且数据可以在pod之间“传递”。cepfs可以由多个写入程序同时装入。

> 注意：在使用共享之前，必须运行自己的Ceph服务器并导出共享。

See the [CephFS example](https://github.com/kubernetes/examples/tree/master/volumes/cephfs/) for more details


### configMap

[configMap](https://kubernetes.io/docs/tasks/configure-pod-container/configure-pod-configmap/)资源提供了一种将配置数据注入pod的方法。存储在ConfigMap对象中的数据可以在ConfigMap类型的卷中引用，然后由运行在Pod中的容器化应用程序使用。

引用configMap对象时，只需在卷中提供其名称即可引用它。您还可以自定义路径用于ConfigMap中的特定entry。例如，要将`log-config` ConfigMap装载到名为`configmap-pod`的Pod上，可以使用下面的YAML：

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: configmap-pod
spec:
  containers:
    - name: test
      image: busybox
      volumeMounts:
        - name: config-vol
          mountPath: /etc/config
  volumes:
    - name: config-vol
      configMap:
        name: log-config
        items:
          - key: log_level
            path: log_level
```

`log-config` ConfigMap作为一个卷装载，存储在其`log_level`条目中的所有内容都装载到pod的“/etc/config/log_level”路径中。请注意，此路径是从卷的mountPath派生出来的，并且该路径使用key: log_level作为文件名。

> 注意：必须先创建一个ConfigMap，然后才能使用它。

> 注意：使用ConfigMap作为[子路径](https://kubernetes.io/docs/concepts/storage/volumes/#using-subpath)卷装入的容器将不会接收ConfigMap的更新。

> 注意：文本数据使用UTF-8字符编码作为文件公开。要使用其他字符编码，请使用二进制数据

### downwardAPI

`downwardAPI`卷用于使应用程序可以使用downward API数据。它挂载一个目录并将请求的数据写入纯文本文件。

> 注意：使用Downward API作为子路径卷装入的容器将不会接收Downward API的更新。

See the [downwardAPI volume example](https://kubernetes.io/docs/tasks/inject-data-application/downward-api-volume-expose-pod-information/) for more details.

### emptyDir

当一个Pod被分配给一个节点时，emptyDir卷首先被创建，并且只要Pod在该节点上运行，它就会一直存在。顾名思义，它最初是空的。Pod中的容器可以在emptyDir卷中读写相同的文件，尽管该卷可以安装在每个容器中相同或不同的路径上。当一个Pod由于任何原因从节点中移除时，emptyDir中的数据将永远被删除。

> 注意：容器崩溃不会从节点中删除Pod，因此emptyDir卷中的数据在容器崩溃时是安全的。

emptyDir的一些用途是：
- 暂存空间，例如用于基于磁盘的合并排序
- 检查点从崩溃中恢复的长时间计算
- 保存内容管理器容器获取到的文件，web服务器容器为之提供数据

默认情况下，emptyDir卷存储在支持节点的任何介质上 —— 可能是磁盘、SSD或网络存储，具体取决于您的环境。但是，您可以设置`emptyDir.medium`字段为`Memory`，告诉Kubernetes为您挂载tmpfs（RAM存储文件系统）。虽然tmpfs非常快，但请注意，与磁盘不同，tmpfs在节点重新启动时被清除，并且您写入的任何文件都将计入容器的内存限制。

Example Pod：
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: test-pd
spec:
  containers:
  - image: k8s.gcr.io/test-webserver
    name: test-container
    volumeMounts:
    - mountPath: /cache
      name: cache-volume
  volumes:
  - name: cache-volume
    emptyDir: {}
```

### flocker

[Flocker](https://github.com/ClusterHQ/flocker)是一个开源的集群容器数据卷管理器。它提供了数据卷（由各种存储后端支持的数据卷）的管理和编排

flocker卷允许将Flocker数据集挂载到Pod上。如果Flocker中不存在数据集，则需要首先使用Flocker CLI或使用Flocker API创建它。如果数据集已经存在，它将被Flocker重新挂载到Pod所在的节点上。这意味着数据可以根据需要在各个pod之间“传递”。

> 注意：您必须提前自己安装运行Flocker。

See the [Flocker example](https://github.com/kubernetes/examples/tree/master/staging/volumes/flocker) for more details

### glusterfs
A glusterfs volume allows a Glusterfs (an open source networked filesystem) volume to be mounted into your Pod. Unlike emptyDir, which is erased when a Pod is removed, the contents of a glusterfs volume are preserved and the volume is merely unmounted. This means that a glusterfs volume can be pre-populated with data, and that data can be "handed off" between Pods. GlusterFS can be mounted by multiple writers simultaneously.

> Caution: You must have your own GlusterFS installation running before you can use it.
See the [GlusterFS example](https://github.com/kubernetes/examples/tree/master/volumes/glusterfs) for more details

### hostPath

主机路径卷将文件或目录从主机节点的文件系统装载到您的pod中。这不是大多数Pods所需要的，但它为某些应用程序提供了一个强大的逃生舱口。

例如，主机路径的一些用途是：
- 运行需要访问Docker内部构件的容器；使用主机路径：/var/lib/Docker
- 在容器中运行cAdvisor；使用主机路径：/sys
- 允许Pod指定给定的主机路径是否应该在Pod运行之前存在，是否应该创建它，以及它应该以什么形式存在

除了必需的path属性外，用户还可以选择为hostPath卷指定type。

type字段支持的值为：

```
Value	            Behavior
                    空字符串（默认值）用于向后兼容，这意味着在装入主机路径卷之前不会执行任何检查。
DirectoryOrCreate	如果给定的路径不存在，则将根据需要在那里创建一个空目录，权限设置为0755，与Kubelet具有相同的组和所有权。 
Directory           给定路径上必须存在目录
FileOrCreate        如果给定路径不存在，则将根据需要在那里创建一个空文件，权限设置为0644，具有与Kubelet相同的组和所有权。
File                文件必须存在于给定路径
Socket              给定路径中必须存在UNIX套接字
CharDevice          字符设备必须存在于给定路径
BlockDevice         块设备必须存在于给定路径
```

使用这种类型的卷时要小心，因为：
- 由于节点上的文件不同，具有相同配置（例如从podTemplate创建的）的pod在不同节点上的行为可能不同
- 当Kubernetes按照计划进行资源感知型(resource-aware)调度时，将无法把hostPath使用的资源考虑进去
- 在底层主机上创建的文件或目录只能由root用户写入。您需要在特权容器中以root用户身份运行进程，或者修改主机上的文件权限，以便能够写入主机路径卷

Example Pod
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: test-pd
spec:
  containers:
  - image: k8s.gcr.io/test-webserver
    name: test-container
    volumeMounts:
    - mountPath: /test-pd
      name: test-volume
  volumes:
  - name: test-volume
    hostPath:
      # directory location on host
      path: /data
      # this field is optional
      type: Directory
```

> 注意：应该注意，FileOrCreate模式不会创建文件的父目录。如果挂载文件的父目录不存在，则pod无法启动。为了确保此模式正常工作，您可以尝试分别装载目录和文件，如下所示

Example Pod FileOrCreate 
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: test-webserver
spec:
  containers:
  - name: test-webserver
    image: k8s.gcr.io/test-webserver:latest
    volumeMounts:
    - mountPath: /var/local/aaa
      name: mydir
    - mountPath: /var/local/aaa/1.txt
      name: myfile
  volumes:
  - name: mydir
    hostPath:
      # Ensure the file directory is created.
      path: /var/local/aaa
      type: DirectoryOrCreate
  - name: myfile
    hostPath:
      path: /var/local/aaa/1.txt
      type: FileOrCreate
```


### local
**FEATURE STATE: Kubernetes v1.14 [stable]**

local本地卷表示装入的本地存储设备，如磁盘、分区或目录。

本地卷只能用作静态创建的PersistentVolume。尚不支持动态配置。

与hostPath卷相比，本地卷可以以持久和可移植的方式使用，而无需手动将pod调度到节点，因为系统通过查看PersistentVolume上的节点亲和力来了解卷的节点约束。

但是，本地卷仍然取决于底层节点的可用性，并不适合所有应用程序。如果一个节点不正常，那么本地卷也将变得不可访问，使用它的Pod将无法运行。使用本地卷的应用程序必须能够忍受这种可用性的降低，以及潜在的数据丢失，这取决于底层磁盘的耐久性特征。

以下是使用本地卷和nodeAffinity的PersistentVolume规范示例：
```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: example-pv
spec:
  capacity:
    storage: 100Gi
  volumeMode: Filesystem
  accessModes:
  - ReadWriteOnce
  persistentVolumeReclaimPolicy: Delete
  storageClassName: local-storage
  local:
    path: /mnt/disks/ssd1
  nodeAffinity:
    required:
      nodeSelectorTerms:
      - matchExpressions:
        - key: kubernetes.io/hostname
          operator: In
          values:
          - example-node
```

使用本地卷时需要指定PersistentVolume nodeAffinity。它使Kubernetes调度器能够将使用local volumes的Pods正确调度到正确的节点。

PersistentVolume `volumeMode` 可以设置为“Block”（而不是默认值“Filesystem”），可以将本地卷公开为原始块设备。

建议在使用“本地存储”类时创建一个StorageClass，并设置这个`StorageClass`的`volumeBindingMode`为`WaitForFirstConsumer`。参见[示例](https://kubernetes.io/docs/concepts/storage/storage-classes/#local)。延迟卷绑定可确保PersistentVolumeClaim绑定决策时也将Pod可能具有的任何其他节点约束考虑进去，例如节点资源需求、节点选择器、Pod亲和力和Pod反亲和力。

外部静态资源调配器可以单独运行，以改进本地卷生命周期的管理。请注意，此供应器尚不支持动态供应。有关如何运行外部本地配置程序的示例，请参阅[本地卷配置程序用户指南](https://github.com/kubernetes-sigs/sig-storage-local-static-provisioner)。

> 注意：如果外部静态供应器不用于管理卷生命周期，则本地PersistentVolume需要用户手动清理和删除


### nfs

nfs卷允许将现有的nfs（网络文件系统）共享装载到Pod中。与emptyDir不同，当移除Pod时会删除它，而nfs卷的内容会被保留，卷只是被卸载而已。这意味着NFS卷可以预先填充数据，并且数据可以在pod之间“传递”。NFS可以由多个写入程序同时装载。

> 注意：在使用共享之前，必须运行自己的NFS服务器并导出共享。

See the [NFS example](https://github.com/kubernetes/examples/tree/master/staging/volumes/nfs) for more details

### persistentVolumeClaim

persistentVolumeClaim卷用于将PersistentVolume装载到Pod中。PersistentVolumeClaims是一种让用户在不了解特定云环境细节的情况下“声明”持久存储（如GCE PersistentDisk或iSCSI卷）的方法。

See the [PersistentVolumes example](https://kubernetes.io/docs/concepts/storage/persistent-volumes/) for more details

### projected

投影卷将多个现有卷源映射到同一目录中。

目前，以下类型的卷可以被投影：
* [secret](https://kubernetes.io/docs/concepts/storage/volumes/#secret)
* [downwardAPI](https://kubernetes.io/docs/concepts/storage/volumes/#downwardapi)
* [configMap](https://kubernetes.io/docs/concepts/storage/volumes/#configmap)
* serviceAccountToken

所有源都必须与Pod位于同一个命名空间中。有关详细信息，请参阅[一体化卷设计文档](https://github.com/kubernetes/community/blob/master/contributors/design-proposals/node/all-in-one-volume.md)。

服务帐户令牌（service account tokens）的投影是kubernetes 1.11中引入的一个特性，并在1.12中升级为Beta版本。要在1.11上启用这个特性，您需要显式地将`TokenRequestProjection`特性门设置为True

Example Pod with a secret, a downward API, and a configmap.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: volume-test
spec:
  containers:
  - name: container-test
    image: busybox
    volumeMounts:
    - name: all-in-one
      mountPath: "/projected-volume"
      readOnly: true
  volumes:
  - name: all-in-one
    projected:
      sources:
      - secret:
          name: mysecret
          items:
            - key: username
              path: my-group/my-username
      - downwardAPI:
          items:
            - path: "labels"
              fieldRef:
                fieldPath: metadata.labels
            - path: "cpu_limit"
              resourceFieldRef:
                containerName: container-test
                resource: limits.cpu
      - configMap:
          name: myconfigmap
          items:
            - key: config
              path: my-group/my-config
```

Example Pod with multiple secrets with a non-default permission mode set

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: volume-test
spec:
  containers:
  - name: container-test
    image: busybox
    volumeMounts:
    - name: all-in-one
      mountPath: "/projected-volume"
      readOnly: true
  volumes:
  - name: all-in-one
    projected:
      sources:
      - secret:
          name: mysecret
          items:
            - key: username
              path: my-group/my-username
      - secret:
          name: mysecret2
          items:
            - key: password
              path: my-group/my-password
              mode: 511
```

每个投影的源卷都列在spec的sources字段里。参数几乎相同，但有两个例外：

- 对于secrets，secretName字段已更改为name以便于ConfigMap命名一致。
- defaultMode只能在投影级别指定，而不能为每个卷源指定。但是，如上所示，可以显式地为每个单独的投影设置`mode`。

启用`TokenRequestProjection`功能后，可以将当前服务帐户的令牌注入到Pod的指定路径中。下面是一个例子：

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: sa-token-test
spec:
  containers:
  - name: container-test
    image: busybox
    volumeMounts:
    - name: token-vol
      mountPath: "/service-account"
      readOnly: true
  volumes:
  - name: token-vol
    projected:
      sources:
      - serviceAccountToken:
          audience: api
          expirationSeconds: 3600
          path: token
```

示例Pod有一个包含注入的服务帐户令牌的投影卷。例如，Pod容器可以使用这个令牌来访问kubernetes api服务器。`audience`字段包含令牌的预期audience。令牌的接收方在接收到令牌后必须验证令牌的audience信息中指定的identifier, 这个identifier必须是令牌接收方自己，否则令牌接收方应该拒绝这个令牌。（A recipient of the token must identify itself with an identifier specified in the audience of the token, and otherwise should reject the token）。`audience`字段是可选的，它默认为API服务器的标识符。

expirationSeconds是服务帐户令牌的预期有效期。默认为1小时，并且必须至少为10分钟（600秒）。管理员还可以通过为API服务器指定`--service-account-max-token-expiration`选项来限制其最大值。path字段指定到投影卷的挂载点的相对路径。

> 注意：使用投影卷源作为子路径卷装入的容器将不会接收这些卷源的更新

### portworxVolume 

### quobyte

### rbd

rbd卷允许将Rados块设备卷装入您的pod。与emptyDir不同，当移除一个Pod时会删除它，rbd卷的内容会被保留，而该卷只是被卸载。这意味着RBD卷可以预先填充数据，并且数据可以在pod之间“传递”。

> 注意：在使用RBD之前，必须安装运行自己的Ceph。

RBD的一个特点是它可以被多个用户同时挂载为只读。这意味着您可以使用数据集预填充卷，然后根据需要从多个pod并行提供服务。不幸的是，RBD卷只能由一个使用者以读写模式装入，不允许同时写入。

See the [RBD example](https://github.com/kubernetes/examples/tree/master/volumes/rbd) for more details


### scaleIO
ScaleIO是一个基于软件的存储平台，可以使用现有硬件创建可扩展共享块网络存储的集群。scaleIO卷插件允许部署的pod访问现有的scaleIO卷（或者它可以动态地为persistent volume claims提供新卷，请参阅[scaleIO持久卷](https://kubernetes.io/docs/concepts/storage/persistent-volumes/#scaleio)）。

> 注意：在使用创建的卷之前，必须已安装并运行现有的ScaleIO集群。

The following is an example of Pod configuration with ScaleIO:
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: pod-0
spec:
  containers:
  - image: k8s.gcr.io/test-webserver
    name: pod-0
    volumeMounts:
    - mountPath: /test-pd
      name: vol-0
  volumes:
  - name: vol-0
    scaleIO:
      gateway: https://localhost:443/api
      system: scaleio
      protectionDomain: sd0
      storagePool: sp1
      volumeName: vol-0
      secretRef:
        name: sio-secret
      fsType: xfs
```
For further detail, please see the [ScaleIO examples](https://github.com/kubernetes/examples/tree/master/staging/volumes/scaleio).

### secret

secret卷用于将密码等敏感信息传递到pod。您可以在kubernetes api中存储secret，并将其作为文件挂载以供Pods使用，而无需直接耦合到Kubernetes。secret卷由tmpfs（RAM存储文件系统）支持，因此它们永远不会写入固定（永久性）存储器。

> 注意：您必须在kubernetes api中创建一个secret，然后才能使用它。

> 注意：使用secret作为子路径卷装入的容器将不会接收secret更新。

Secrets are described in more detail [here](https://kubernetes.io/docs/concepts/configuration/secret/)


### storageOS

storageOS卷允许将现有的storageOS卷装载到pod中。

StorageOS作为Kubernetes环境中的容器运行，使本地或连接的存储可以从Kubernetes集群中的任何节点访问。数据可被复制以防止节点故障。精简的资源调配和压缩可以提高利用率并降低成本。

StorageOS的核心是为容器提供块存储，并通过文件系统访问。

StorageOS容器需要64位 Linux系统，没有其他额外的依赖项。提供免费的开发人员许可证。

> 注意：必须在需要访问StorageOS卷的节点上，或者给存储容量池提供容量的节点上安装运行StorageOS容器。有关安装说明，请参阅[StorageOS文档](https://docs.storageos.com/)。

```yaml
apiVersion: v1
kind: Pod
metadata:
  labels:
    name: redis
    role: master
  name: test-storageos-redis
spec:
  containers:
    - name: master
      image: kubernetes/redis:v1
      env:
        - name: MASTER
          value: "true"
      ports:
        - containerPort: 6379
      volumeMounts:
        - mountPath: /redis-master-data
          name: redis-data
  volumes:
    - name: redis-data
      storageos:
        # The `redis-vol01` volume must already exist within StorageOS in the `default` namespace.
        volumeName: redis-vol01
        fsType: ext4
```

For more information including Dynamic Provisioning and Persistent Volume Claims, please see the [StorageOS examples](https://github.com/kubernetes/examples/blob/master/volumes/storageos)

### vsphereVolume


## Using subPath

有时，在一个Pod中共享一个卷供多个使用是很有用的。这个`volumeMounts.subPath`属性可用于指定被引用卷内部的子路径，而不是其根路径。

下面是一个带有LAMP（Linux Apache Mysql PHP）的Pod示例，它使用单个共享卷。HTML内容映射到其HTML文件夹，数据库将存储在其mysql文件夹中：

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: my-lamp-site
spec:
    containers:
    - name: mysql
      image: mysql
      env:
      - name: MYSQL_ROOT_PASSWORD
        value: "rootpasswd"
      volumeMounts:
      - mountPath: /var/lib/mysql
        name: site-data
        subPath: mysql
    - name: php
      image: php:7.0-apache
      volumeMounts:
      - mountPath: /var/www/html
        name: site-data
        subPath: html
    volumes:
    - name: site-data
      persistentVolumeClaim:
        claimName: my-lamp-site-data
```

### Using subPath with expanded environment variables 

*FEATURE STATE: Kubernetes v1.17 [stable]*

使用`subPathExpr`字段从Downward API环境变量构造子路径目录名。subPath和subPathExpr属性是互斥的。

在本例中，Pod使用subPathExpr在hostPath卷/var/log/pods中创建一个目录pod1，该subPathExpr使用了Downward API中的Pod名称来创建。最后主机目录/var/log/pods/pod1挂载在容器的/logs路径中。

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: pod1
spec:
  containers:
  - name: container1
    env:
    - name: POD_NAME
      valueFrom:
        fieldRef:
          apiVersion: v1
          fieldPath: metadata.name
    image: busybox
    command: [ "sh", "-c", "while [ true ]; do echo 'Hello'; sleep 10; done | tee -a /logs/hello.txt" ]
    volumeMounts:
    - name: workdir1
      mountPath: /logs
      subPathExpr: $(POD_NAME)
  restartPolicy: Never
  volumes:
  - name: workdir1
    hostPath:
      path: /var/log/pods
```

## Out-of-Tree Volume Plugins

Out-of-Tree Volume插件包括容器存储接口（CSI）和FlexVolume。它们使存储供应商能够创建自定义存储插件，而无需将它们添加到Kubernetes存储库中。

在CSI和FlexVolume引入之前，所有的卷插件（如上面列到的卷）都是“in-tree”的，也就是说这些插件都是使用核心kubernetes二进制文件和核心Kubernetes API构建、连接、编译以及发行的。这就意味着向Kubernetes添加新的存储系统（卷插件）需要将代码检入核心Kubernetes代码库。

CSI和FlexVolume都允许卷插件独立于Kubernetes代码库开发，并作为扩展部署（安装）在Kubernetes集群上。

对于希望创建树外卷插件的存储供应商，请参阅此常见[问题解答](https://github.com/kubernetes/community/blob/master/sig-storage/volume-plugin-faq.md)。

### CSI

[容器存储接口（Container Storage Interface： CSI）](https://github.com/container-storage-interface/spec/blob/master/spec.md)为容器编排系统（如Kubernetes）定义了一个标准接口，以将任意存储系统暴露于它们的容器工作负载中。

更多信息，请阅读[CSI设计方案](https://github.com/kubernetes/community/blob/master/contributors/design-proposals/storage/container-storage-interface.md)。

CSI的支持在kubernetes v1.9中作为alpha引入，在kubernetes v1.10中迁移到beta，在kubernetes v1.13中是GA。

> 注意：在kubernetesv1.13中，不推荐使用CSI规范版本0.2和0.3，将来的版本中会删除。

> 注意：CSI驱动程序可能不能一次性兼容所有Kubernetes版本。请查看特定的CSI驱动程序文档，了解每个Kubernetes版本的支持部署步骤和兼容性列表。

一旦在Kubernetes集群上部署了CSI兼容的卷驱动程序，用户就可以使用CSI卷类型来附加、挂载CSI驱动程序公开的卷等。

csi卷可在pod中以三种不同的方式使用：
- 通过[persistentVolumeClaim](https://kubernetes.io/docs/concepts/storage/volumes/#persistentvolumeclaim)里引用的方式使用
- 通过[generic ephemeral volume](https://kubernetes.io/docs/concepts/storage/ephemeral-volumes/#generic-ephemeral-volume)（alpha特征）
- 通过[CSI ephemeral volume](https://kubernetes.io/docs/concepts/storage/ephemeral-volumes/#csi-ephemeral-volume)，前提是驱动程序支持（beta功能）


以下字段可供存储管理员配置CSI永久卷：
- driver：一个字符串值，指定要使用的卷驱动程序的名称。此值必须与[CSI规范](https://github.com/container-storage-interface/spec/blob/master/spec.md#getplugininfo)中定义的CSI驱动程序在GetPluginInfoResponse中返回的值相对应。Kubernetes使用它来标识要调用哪个CSI驱动程序，CSI驱动程序组件使用它来标识哪些PV对象属于CSI驱动程序。
- volumeHandle：唯一标识卷的字符串值。此值必须与[CSI规范](https://github.com/container-storage-interface/spec/blob/master/spec.md#createvolume)中定义的CSI驱动程序响应的CreateVolumeResponse的volume.id字段相对应。当引用卷时，该值将作为`volume_id`传递给CSI卷驱动程序。
- readOnly：一个可选的布尔值，指示卷是否以只读方式“ControllerPublished”（attached）。默认值为false。此值通过ControllerPublishVolumeRequest中的readonly字段传递给CSI驱动程序。
- fsType：如果PV的VolumeMode是Filesystem，那么这个字段可以用来指定应该用来装载卷的文件系统。如果卷尚未格式化且支持格式化，则此值将用于格式化卷。此值通过ControllerPublishVolumeRequest、NodeStageVolumeRequest和NodePublishVolumeRequest的VolumeCapability字段传递给CSI驱动程序
- volumeAttributes：字符串到字符串的映射map，指定卷的静态属性。此映射必须与CSI驱动程序在[CSI规范](https://github.com/container-storage-interface/spec/blob/master/spec.md#createvolume)中定义的CreateVolumeResponse的volume.attributes字段对应。这个map通过ControllerPublishVolumeRequest、NodeStageVolumeRequest和NodePublishVolumeRequest中的`volume_context`字段传递给CSI驱动程序。
- controllerPublishSecretRef：是一个包含敏感信息的secret对象的引用，该对象要传递给CSI驱动程序，以完成CSI ControllerPublishVolume和ControllerUnpublishVolume的调用。此字段是可选的，如果不需要secret，则可以为空。如果secret对象包含多个secret，则传递所有secret。
- nodeStageSecretRef：一个包含敏感信息的secret对象的引用，该对象包含的信息要传递给CSI驱动程序以完成CSI NodeStageVolume的调用。此字段是可选的，如果不需要secret，则可以为空。如果secret对象包含多个secret，则传递所有secret。
- nodePublishSecretRef：一个包含敏感信息的secret对象的引用，该对象要传递给CSI驱动程序以完成CSI NodePublishVolume调用。此字段是可选的，如果不需要secret，则可以为空。如果secret对象包含多个secret，则传递所有secret。

#### CSI raw block volume support
*FEATURE STATE: Kubernetes v1.18 [stable]*

具有外部CSI驱动程序的供应商可以在Kubernetes工作负载中实现原始块卷支持(raw block volumes support)。

您可以像往常一样[使用原始块容量支持来设置PV/PVC](https://kubernetes.io/docs/concepts/storage/persistent-volumes/#raw-block-volume-support)，而不需要任何特定于CSI的更改

#### CSI ephemeral volumes
*FEATURE STATE: Kubernetes v1.16 [beta]*
您可以在Pod规范中直接配置CSI卷。以这种方式指定的卷是短暂的，不会在Pod重新启动时保持不变。有关详细信息，请参见[临时卷](https://kubernetes.io/docs/concepts/storage/ephemeral-volumes/#csi-ephemeral-volume)

#### What's next
For more information on how to develop a CSI driver, refer to the [kubernetes-csi documentation](https://kubernetes-csi.github.io/docs/)

#### Migrating to CSI drivers from in-tree plugins 
*FEATURE STATE: Kubernetes v1.14 [alpha]*

CSI迁移特性启用后，将现有in-tree插件的操作定向到相应的CSI插件（需要提前安装和配置好这些插件）。该特性实现了必要的转换逻辑和shims，无缝地重新路由操作。因此，当过渡到取代in-tree插件的CSI驱动程序时，操作员不必修改现有Storage Classes、PV或PVC（指in-tree插件）的任何配置。

在alpha状态下，支持的操作和功能包括配置/删除、附加(attach)/分离(detach)、装载(mount)/卸载(unmount)和调整卷大小。

支持CSI迁移并实现相应CSI驱动程序的in-tree插件在上面的“Types of Volumes”一节中有列出


### FlexVolume

FlexVolume是一个out-of-tree插件接口，自1.2版（CSI之前）就存在于Kubernetes中。它使用基于exec的模型与驱动程序接口。FlexVolume驱动程序二进制文件必须安装在每个节点（在某些情况下是主节点）上的预定义卷插件(pre-defined volume plugin)路径中。

Pods通过`flexvolume` in-tree插件与FlexVolume驱动程序交互。更多细节可以在[这里](https://github.com/kubernetes/community/blob/master/contributors/devel/sig-storage/flexvolume.md)找到


## Mount propagation

Mount propagation允许我们把挂载在一个容器上的卷共享给另一个在相同的pod里的容器，甚至可以共享给同一个节点上不同的pod里的容器。

卷的Mount propagation由Container.volumeMounts中的`mountPropagation`字段控制，其值可以是：
- None —— 此卷装载将不会接收由主机装载到此卷或其任何子目录的任何后续装载。以类似的方式，容器创建的装载在主机上不可见。这是默认模式。
    此模式等同于[Linux内核文档](https://www.kernel.org/doc/Documentation/filesystems/sharedsubtree.txt)中描述的私有装载传播
- HostToContainer —— 此卷装载将接收装载到此卷或其任何子目录的所有后续装载。
    换句话说，如果主机在卷装载内部装载了任何东西，容器将看到它装载在那里。
    类似地，如果pod在同一个volume上具有双向装载传播能力，那么pod在这个volume里装载的任何东西都能被具有HostToContainer装载传播能力的容器看到。
    此模式等同于[Linux内核文档](https://www.kernel.org/doc/Documentation/filesystems/sharedsubtree.txt)中描述的rslave挂载传播
- Bidirectional —— 此卷装载与HostToContainer装载的行为相同。此外，容器创建的所有卷装载都将传播回主机和使用同一卷的所有pod的所有容器。
    这种模式的一个典型用例是带有FlexVolume或CSI驱动程序的Pod，或者需要使用hostPath卷在主机上挂载某些内容的Pod。
    此模式等同于[Linux内核文档](https://www.kernel.org/doc/Documentation/filesystems/sharedsubtree.txt)中描述的rshared挂载传播

> 注意：Bidirectional挂载传播可能很危险。它会损坏主机操作系统，因此只允许在特权容器中使用。强烈建议您先熟悉Linux内核行为。此外，由Pods中的容器创建的任何卷装载都必须在终止时被容器销毁（卸载）。

### Configuration 

在挂载传播在某些部署（CoreOS、RedHat/Centos、Ubuntu）上正常工作之前，必须在Docker中正确配置挂载共享，如下所示。

编辑你的Docker的systemd服务文件。按如下方式设置MountFlags：

```
MountFlags=shared
```

或者，删除MountFlags=slave（如果存在）。然后重新启动Docker守护程序：
```
sudo systemctl daemon-reload
sudo systemctl restart docker
```

## What's next
Follow an example of [deploying WordPress and MySQL with Persistent Volumes](https://kubernetes.io/docs/tutorials/stateful-application/mysql-wordpress-persistent-volume/).
