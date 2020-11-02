# ConfigMaps
ConfigMap是一个API对象，用于在键值对中存储非机密数据。pod可以将ConfigMaps用作环境变量、命令行参数或卷中的配置文件。

ConfigMap允许您将特定于环境的配置与容器镜像分离，以便您的应用程序易于移植。

:cactus: 注意：ConfigMap不提供保密或加密。如果要存储的数据是机密的，请使用[Secret](https://kubernetes.io/docs/concepts/configuration/secret/)而不是ConfigMap，或者使用其他（第三方）工具来保持数据的隐私。

## Motivation 

使用ConfigMap将配置数据与应用程序代码分开设置。

例如，假设您正在开发一个应用程序，该应用程序可以在您自己的计算机（用于开发）和云（用于处理实际流量）上运行。编写代码查找DATABASE_HOST环境变量。在本地，该变量设置为localhost。在云中，您将其设置为引用Kubernetes服务的地址，该服务向集群公开数据库组件。这使您可以获取运行在云中的容器镜像，并在需要时在本地调试完全相同的代码。

ConfigMap不是设计用来保存大数据块的。存储在ConfigMap中的数据不能超过1 MiB。如果需要存储大于此限制的设置，可以考虑装入卷或使用单独的数据库或文件服务

## ConfigMap object 

ConfigMap是一个API对象，它允许您存储配置以供其他对象使用。与大多数有spec的Kubernetes对象不同，ConfigMap有`data`和`binaryData`字段。这些字段接受键值对作为它们的值。`data`字段和`binaryData`字段都是可选的。`data`字段设计用来包含UTF-8字节序列，而`binaryData`字段设计用来包含二进制数据。

配置映射的名称必须是有效的DNS子域名。

data或binaryData字段下的每个键必须由字母、数字、`-`, `_` or `.`组成。`data`字段中的键不能和`binaryData`字段中的键重复。

从v1.19开始，您可以在ConfigMap定义中添加一个`immutable`字段来创建一个[不可变ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/#configmap-immutable)。

## ConfigMaps and Pods 
您可以编写一个引用ConfigMap的Pod规范，并根据ConfigMap中的数据配置该Pod中的容器。Pod和ConfigMap必须位于同一命名空间中。

下面是一个ConfigMap示例，其中一些键具有单个值，其他键的值看起来像配置格式的片段。

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: game-demo
data:
  # property-like keys; each key maps to a simple value
  player_initial_lives: "3"
  ui_properties_file_name: "user-interface.properties"

  # file-like keys
  game.properties: |
    enemy.types=aliens,monsters
    player.maximum-lives=5
  user-interface.properties: |
    color.good=purple
    color.bad=yellow
    allow.textmode=true
```

有四种不同的方法可以使用ConfigMap配置Pod内的容器：
1. 容器入口点的命令行参数
2. 容器的环境变量
3. 在只读卷中添加一个文件，供应用程序读取
4. 编写在Pod中运行的代码，使用kubernetes api读取ConfigMap

这些不同的方法有助于使用不同的方法来建模需要使用的数据。对于前三种方法，kubelet会在为Pod启动容器时使用ConfigMap中的数据。

第四种方法意味着您必须编写代码来读取ConfigMap及其数据。但是，由于您直接使用kubernetes api，所以您的应用程序可以在ConfigMap发生更改时订阅获取更新，并在发生更改时做出响应。通过直接访问kubernetes api，此技术还允许您访问不同命名空间中的ConfigMap。

下面是一个使用`game-demo`中的值配置Pod的示例：
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: configmap-demo-pod
spec:
  containers:
    - name: demo
      image: alpine
      command: ["sleep", "3600"]
      env:
        # Define the environment variable
        - name: PLAYER_INITIAL_LIVES # Notice that the case is different here
                                     # from the key name in the ConfigMap.
          valueFrom:
            configMapKeyRef:
              name: game-demo           # The ConfigMap this value comes from.
              key: player_initial_lives # The key to fetch.
        - name: UI_PROPERTIES_FILE_NAME
          valueFrom:
            configMapKeyRef:
              name: game-demo
              key: ui_properties_file_name
      volumeMounts:
      - name: config
        mountPath: "/config"
        readOnly: true
  volumes:
    # You set volumes at the Pod level, then mount them into containers inside that Pod
    - name: config
      configMap:
        # Provide the name of the ConfigMap you want to mount.
        name: game-demo
        # An array of keys from the ConfigMap to create as files
        items:
        - key: "game.properties"
          path: "game.properties"
        - key: "user-interface.properties"
          path: "user-interface.properties"
```

ConfigMap不区分单行属性值和多行类似文件的值。重要的是pod和其他对象如何使用这些值。

对于本例，定义一个卷并将其装入demo容器中，并在/config中创建两个文件，`/config/game.properties`和`/config/user-interface.properties`，即使ConfigMap中有四个键。这是因为Pod定义在volumes部分指定了items数组。如果完全省略items数组，ConfigMap中的每个键都将成为一个与该键同名的文件，即您将得到4个文件。

## Using ConfigMaps

ConfigMaps可以作为数据卷装载。ConfigMaps也可以被系统的其他部分使用，而不需要直接暴露在Pod中。例如，ConfigMaps可以保存系统其他部分用于配置的数据。

使用ConfigMaps最常见的方法是为运行在同一命名空间中的Pod中的容器配置进行设置。您也可以单独使用ConfigMap。

例如，您可能会遇到基于ConfigMap调整其行为的[加载项addons](https://kubernetes.io/docs/concepts/cluster-administration/addons/)或 [operators(A specialized controller used to manage a custom resource)](https://kubernetes.io/docs/concepts/extend-kubernetes/operator/) 。

### Using ConfigMaps as files from a Pod 

要在Pod中的卷中使用ConfigMap，请执行以下操作：
1. 创建ConfigMap 或使用现有ConfigMap 。多个pod可以引用同一个ConfigMap。
2. 修改Pod定义，在`.spec.volumes[]`下面添加一个卷。定义卷名，然后将`.spec.volumes[].configMap.name`字段设置为ConfigMap对象的引用。
3. 给需要使用ConfigMap的容器添加`.spec.containers[].volumeMounts[]`。设置`.spec.containers[].volumeMounts[].readOnly = true`，设置`.spec.containers[].volumeMounts[].mountPath`为一个未使用的目录名（然后ConfigMap会在这个目录出现）。
4. 修改镜像或命令行，以便程序在该目录中查找文件。ConfigMap data映射中的每个键都成为mountPath下的文件名。

这是一个在卷中装载ConfigMap的Pod示例：

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: mypod
spec:
  containers:
  - name: mypod
    image: redis
    volumeMounts:
    - name: foo
      mountPath: "/etc/foo"
      readOnly: true
  volumes:
  - name: foo
    configMap:
      name: myconfigmap
```

每个需要使用的ConfigMap都需要在`.spec.volumes`中引用

如果Pod中有多个容器，那么每个容器都需要自己的volumeMounts块，但是每个ConfigMap只需要在`.spec.volumes`中引用一次。

#### Mounted ConfigMaps are updated automatically 装载的ConfigMaps会自动更新

当卷中当前使用的ConfigMap被更新时，映射的key最终也会被更新。kubelet检查挂载的ConfigMap是否在每次定期同步时都是新的。但是，kubelet使用其本地缓存来获取ConfigMap的当前值。可以使用[KubeletConfiguration结构](https://github.com/kubernetes/kubernetes/blob/master/staging/src/k8s.io/kubelet/config/v1beta1/types.go)中的`ConfigMapAndSecretChangeDetectionStrategy `字段配置缓存的类型。ConfigMap可以通过watch（默认）、基于ttl的方式传输更新，也可以简单地将所有请求直接重定向到API服务器。因此，从ConfigMap更新到新keys映射到Pod的总延迟是kubelet同步周期+缓存传输延迟的时间，其中缓存传输延迟取决于所选的缓存类型（等于watch传输延迟、缓存的ttl，或为零）。

作为环境变量使用的ConfigMaps不会自动更新，需要重启pod。


## Immutable ConfigMaps 
*FEATURE STATE: Kubernetes v1.19 [beta]*

Kubernetes beta特性`Immutable Secrets`和`Immutable ConfigMaps`提供了一个选项，可以将单个Secrets和ConfigMaps设置为不可变的。对于广泛使用ConfigMaps的集群（至少有上万个不同的ConfigMap用来给Pod挂载），防止对其数据进行更改具有以下优点：

- 防止意外（或不需要的）更新导致应用程序中断
- 通过关闭immutable configmap的监视watches，显著减少kube-apiserver上的负载，从而提高集群的性能。

这个特性由ImmutableEphemeralVolumes[特性门](https://kubernetes.io/docs/reference/command-line-tools-reference/feature-gates/)控制。通过将immutable字段设置为true，可以创建一个不可变的ConfigMap。例如：

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  ...
data:
  ...
immutable: true
```

一旦ConfigMap被标记为不可变的，就不可能恢复此更改，也不可能更改data或binaryData字段的内容。只能删除和重新创建ConfigMap。由于现有的Pods维护一个指向已删除ConfigMap的装载点mount point，因此建议重新创建这些Pods。


## What's next
* Read about [Secrets](https://kubernetes.io/docs/concepts/configuration/secret/).
* Read [Configure a Pod to Use a ConfigMap](https://kubernetes.io/docs/tasks/configure-pod-container/configure-pod-configmap/).
* Read [The Twelve-Factor App](https://12factor.net/) to understand the motivation for separating code from configuration.