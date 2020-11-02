# Secrets 

Kubernetes Secrets允许您存储和管理敏感信息，例如密码、OAuth令牌和ssh密钥。将机密信息存储在Secret中比将其逐字放入Pod定义或容器映像中更安全、更灵活。有关详细信息，请参阅[Secrets设计文档](https://git.k8s.io/community/contributors/design-proposals/auth/secrets.md)。


Secret是包含少量敏感数据（如密码、令牌或密钥）的对象。这样的信息可以放在Pod规范或镜像中。用户可以创建Secret，系统也会创建一些Secret。

## Overview of Secrets

要使用Secret，Pod需要引用该Secret。Secret可以通过三种方式与Pod一起使用：
- 作为装入一个或多个容器的卷中的[文件](https://kubernetes.io/docs/concepts/configuration/secret/#using-secrets-as-files-from-a-pod)。
- 作为[容器环境变量](https://kubernetes.io/docs/concepts/configuration/secret/#using-secrets-as-environment-variables)。
- 当[为Pod拉取镜像](https://kubernetes.io/docs/concepts/configuration/secret/#using-imagepullsecrets)的时候。

Secret对象的名称必须是有效的DNS子域名。当您在给一个Secret创建一个配置文件的时候，可以指定一个`data` 和/或 `stringData`字段。data和stringData字段是可选的。数据字段中所有键的值必须是base64编码的字符串。如果不希望转换为base64字符串，则可以选择指定stringData字段，该字段接受任意字符串作为值。

data和stringData的键必须由字母数字字符、`-`、`_`或`.`组成。stringData字段中的所有键值对都在内部合并到data字段中。如果键同时出现在data和stringData字段中，则stringData字段中指定的值优先。

## Types of Secret

创建Secret时，可以使用Secret资源的type字段或某些等效的kubectl命令行标志（如果可用）来指定其类型。Secret类型用于促进Secret数据的编程处理。

Kubernetes为一些常见的使用场景提供了几种内置类型。这些类型因所执行的验证和Kubernetes对其施加的约束而异。

Builtin Type | Usage
-------------|------
Opaque | arbitrary user-defined data
kubernetes.io/service-account-token | service account token
kubernetes.io/dockercfg | serialized ~/.dockercfg file
kubernetes.io/dockerconfigjson | serialized ~/.docker/config.json file
kubernetes.io/basic-auth | credentials for basic authentication
kubernetes.io/ssh-auth | credentials for SSH authentication
kubernetes.io/tls | data for a TLS client or server
bootstrap.kubernetes.io/token | bootstrap token data


您可以通过给Secret对象的type值指定一个非空字符串，来定义为自定义的Secret type。空字符串被视为Opaque类型。Kubernetes对type名没有任何约束。但是，如果使用的是一种内置类型，则必须满足为该类型定义的所有要求。

### Opaque secrets

如果从Secret配置文件中省略Secret type，则Opaque是默认值。使用kubectl创建Secret时，需要使用generic子命令指示Opaque Secret类型。例如，以下命令创建Opaque类型的空Secret。

```sh
kubectl create secret generic empty-secret
kubectl get secret empty-secret
```

输出如下：

```sh
NAME           TYPE     DATA   AGE
empty-secret   Opaque   0      2m6s
```

DATA列显示存储在Secret中的数据项的数量。在本例中，0表示我们刚刚创建了一个空Secret。

### Service account token Secrets

`kubernetes.io/service-account-token` Secret类型用于存储服务帐户的令牌。使用此Secret类型时，需要确保将`kubernetes.io/service-account.name`注释的值设置为已有的service account name。Kubernetes控制器填充一些其他字段，例如`kubernetes.io/service-account.uid`注释和`data`字段中的`token` key设置为实际令牌内容。

以下示例配置声明了一个服务帐户令牌Secret：
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: secret-sa-sample
  annotations:
    kubernetes.io/service-account.name: "sa-name"
type: kubernetes.io/service-account-token
data:
  # You can include additional key value pairs as you do with Opaque Secrets
  extra: YmFyCg==
```

在创建Pod时，Kubernetes会自动创建一个服务帐户密码，并自动修改Pod以使用该密码。服务帐户令牌Secret包含访问API的凭据。

如果需要，可以禁用或重写API凭据的自动创建和使用。但是，如果您只需要安全地访问API服务器，这是推荐的工作流程。

有关[服务帐户](https://kubernetes.io/docs/tasks/configure-pod-container/configure-service-account/)如何工作的更多信息，请参阅ServiceAccount文档。您还可以检查Pod的automountServiceAccountToken字段和serviceAccountName字段，以获取有关从Pods引用服务帐户的信息。

### Docker config Secrets 

您可以使用以下类型值之一来创建一个Secret来存储访问Docker registry的凭据。

* kubernetes.io/dockercfg
* kubernetes.io/dockerconfigjson

这个`kubernetes.io/dockercfg`类型被保留以存储序列化的`~/.dockercfg`，这是配置Docker命令行的旧格式。使用此Secret类型时，必须确保Secret data字段包含一个`.dockercfg` key，其值为base64格式编码的`~/.dockercfg`文件的内容。

`kubernetes/dockerconfigjson`类型是为存储序列化的JSON而设计的，它与`~/.docker/config.json`的格式相同，是`~/.dockercfg`使用的新格式。使用此Secret类型时，Secret对象的data字段必须包含一个`.dockerconfigjson` key，其值也就是`~/.docker/config.json`文件的内容以base64编码的字符串形式提供。

下面是一个kubernetes.io/dockercfg类型的Secret：
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: secret-dockercfg
type: kubernetes.io/dockercfg
data:
  .dockercfg: |
    "<base64 encoded ~/.dockercfg file>"
```

:notebook: 注意：如果不想执行base64编码，可以选择使用stringData字段。

当您使用清单创建这些类型的Secret时，API服务器将检查data字段中是否存在所需的密钥，并验证提供的值是否可以解析为有效的JSON。API服务器不验证JSON是否是Docker配置文件。

如果您没有Docker配置文件，或者希望使用kubectl创建Docker registry Secret，可以执行以下操作：

```sh
kubectl create secret docker-registry secret-tiger-docker \
  --docker-username=tiger \
  --docker-password=pass113 \
  --docker-email=tiger@acme.com
```

此命令创建类型为`kubernetes.io/dockerconfigjson`的Secret. 如果从data字段中转储.`dockerconfigjson`的内容，将得到以下JSON内容，这是动态创建的有效的Docker配置：
```json
{
  "auths": {
    "https://index.docker.io/v1/": {
      "username": "tiger",
      "password": "pass113",
      "email": "tiger@acme.com",
      "auth": "dGlnZXI6cGFzczExMw=="
    }
  }
}
```

### Basic authentication Secret 

这个`kubernetes.io/basic-auth`类型用于存储基本身份验证所需的凭据。使用此Secret类型时，Secret的data字段必须包含以下两个key：
- username: the user name for authentication;
- password: the password or token for authentication.

以上两个键的值都是base64编码的字符串。当然，您可以使用stringData为Secret创建提供明文内容。

下面的YAML是基本身份验证密码的示例配置：

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: secret-basic-auth
type: kubernetes.io/basic-auth
stringData:
  username: admin
  password: t0p-Secret
```

基本认证密钥类型仅为方便用户而提供。你可以为用于基本身份验证的凭据创建一个Opaque类型的Secret。但是，使用内置的Secret类型有助于统一凭据的格式，并且API服务器会验证是否在Secret配置中提供了所需的密钥。

### SSH authentication secrets 

内置类型`kubernetes.io/ssh-auth`用于存储SSH身份验证中使用的数据。使用此Secret类型时，必须在data（或stringData）字段中指定`ssh-privatekey`键值对，作为要使用的SSH凭据。

以下YAML是SSH身份验证 Secret的示例配置：
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: secret-ssh-auth
type: kubernetes.io/ssh-auth
data:
  # the data is abbreviated in this example
  ssh-privatekey: |
     MIIEpQIBAAKCAQEAulqb/Y ...
```

SSH身份验证Secret类型仅为方便用户而提供。您可以为用于SSH身份验证的凭据创建一个Opaque类型的Secret。但是，使用内置的Secret类型有助于统一凭据的格式，并且API服务器会验证是否在Secret配置中提供了所需的密钥。

### TLS secrets 

Kubernetes提供了一种内置的Secret类型`kubernetes.io/tls`，用于存储通常用于TLS的证书及其关联密钥。此数据主要用于入口资源的TLS终端使用，但可以与其他资源一起使用，也可以直接由工作负载使用。当使用此类Secret时，`tls.key`以及`tls.crt`密钥必须在Secret配置的data（或stringData）字段中提供，尽管API服务器实际上并不验证每个密钥的值。

以下YAML包含TLS Secret的配置示例：

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: secret-tls
type: kubernetes.io/tls
data:
  # the data is abbreviated in this example
  tls.crt: |
    MIIC2DCCAcCgAwIBAgIBATANBgkqh ...
  tls.key: |
    MIIEpgIBAAKCAQEA7yn3bRHQ5FHMQ ...
```

TLS Secret类型是为方便用户而提供的。您可以为TLS服务器和/或客户端创建一个Opaque类型的Secret。但是，使用内置的Secret类型有助于确保项目中Secret格式的一致性；API服务器会验证是否在Secret配置中提供了所需的密钥。

使用kubectl创建TLS Secret时，可以使用TLS子命令，如下例所示：

```sh
kubectl create secret tls my-tls-secret \
  --cert=path/to/cert/file \
  --key=path/to/key/file
```

首先公钥和私钥对必须已经存在。--cert的公钥证书必须是.PEM编码的（Base64编码的DER格式），并与--key给定的私钥匹配。私钥必须是通常所说的PEM私钥格式，未加密。在这两种情况下，PEM的首行和最后一行（例如，------BEGIN CERTIFICATE-----和-----END CERTIFICATE----）都不包括在内。

### Bootstrap token Secrets 
可以通过显式地将Secret类型指定为`bootstrap.kubernetes.io/token`来创建引导令牌Secret. 这种类型的Secret是为节点引导过程中使用的令牌设计的。它存储用于签名ConfigMaps的令牌。

引导令牌Secret通常在`kube-system`命名空间中创建，并以`bootstrap-token-<token-id>`的形式命名，其中`<token-id>`是令6个字符的token ID。

作为Kubernetes清单，引导令牌Secret可能如下所示：

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: bootstrap-token-5emitj
  namespace: kube-system
type: bootstrap.kubernetes.io/token
data:
  auth-extra-groups: c3lzdGVtOmJvb3RzdHJhcHBlcnM6a3ViZWFkbTpkZWZhdWx0LW5vZGUtdG9rZW4=
  expiration: MjAyMC0wOS0xM1QwNDozOToxMFo=
  token-id: NWVtaXRq
  token-secret: a3E0Z2lodnN6emduMXAwcg==
  usage-bootstrap-authentication: dHJ1ZQ==
  usage-bootstrap-signing: dHJ1ZQ==
```

引导类型在data字段下指定了以下键：
- token_id： 作为令牌标识符的随机的6个字符。Required。
- token-secret：一个随机的16个字符的字符串，作为实际的令牌secret。Required。
- description1: 一个人类可读的字符串，用于描述令牌的用途。可选。
- expiration: 绝对UTC时间，使用RFC3339指定令牌应过期的时间。可选。
- `usage-bootstrap-<usage>` : 指示引导令牌可用于其他用途的布尔标志。
- auth-extra-groups： 以逗号分隔的组名列表，用于系统之外的其他身份验证：引导程序组。A comma-separated list of group names that will be authenticated as in addition to system:bootstrappers group.

上面的YAML可能看起来很混乱，因为这些值都是base64编码的字符串。事实上，您可以使用以下YAML创建一个相同的Secret，结果都是相同的Secret对象：

```yaml
apiVersion: v1
kind: Secret
metadata:
  # Note how the Secret is named
  name: bootstrap-token-5emitj
  # A bootstrap token Secret usually resides in the kube-system namespace
  namespace: kube-system
type: bootstrap.kubernetes.io/token
stringData:
  auth-extra-groups: "system:bootstrappers:kubeadm:default-node-token"
  expiration: "2020-09-13T04:39:10Z"
  # This token ID is used in the name
  token-id: "5emitj"
  token-secret: "kq4gihvszzgn1p0r"
  # This token can be used for authentication
  usage-bootstrap-authentication: "true"
  # and it can be used for signing
  usage-bootstrap-signing: "true"
```


## Creating a Secret

有几个选项可以创建Secret：
* [使用kubectl命令创建Secret](https://kubernetes.io/docs/tasks/configmap-secret/managing-secret-using-kubectl/)
* [从配置文件创建Secret](https://kubernetes.io/docs/tasks/configmap-secret/managing-secret-using-config-file/)
* [使用kustomize创建Secret](https://kubernetes.io/docs/tasks/configmap-secret/managing-secret-using-kustomize/)

## Editing a Secret 
可以使用以下命令编辑现有Secret：

```sh
kubectl edit secrets mysecret
```

这将打开默认配置的编辑器，并允许更新data字段中的base64编码的Secret值：

```yaml
# Please edit the object below. Lines beginning with a '#' will be ignored,
# and an empty file will abort the edit. If an error occurs while saving this file will be
# reopened with the relevant failures.
#
apiVersion: v1
data:
  username: YWRtaW4=
  password: MWYyZDFlMmU2N2Rm
kind: Secret
metadata:
  annotations:
    kubectl.kubernetes.io/last-applied-configuration: { ... }
  creationTimestamp: 2016-01-22T18:41:56Z
  name: mysecret
  namespace: default
  resourceVersion: "164619"
  uid: cfee02d6-c137-11e5-8d73-42010af00002
type: Opaque
```

## Using Secrets 

Secret可以作为数据卷装载，也可以作为环境变量公开，供Pod中的容器使用。机密也可以被系统的其他部分使用，而不必直接暴露在Pod中。例如，Secret可以保存系统其他部分使用的凭据，并代表您与外部系统进行交互。

### Using Secrets as files from a Pod
要在Pod中使用卷中的Secret：
1. 创建一个Secret或使用现有的Secret。多个Pod可以引用同一个Secret。
2. 修改Pod定义，在`.spec.volumes[]`下添加一个卷。卷名任意，然后把`.spec.volumes[].secret.secretName`字段的值设置为Secret对象的名字
3. 给需要使用secret的容器添加`.spec.containers[].volumeMounts[]`。指定`.spec.containers[].volumeMounts[].readOnly = true`，指定`.spec.containers[].volumeMounts[].mountPath`的值设置为未使用的目录，secret将在这个目录下创建。
4. 修改镜像或命令行，以便程序在该目录中查找文件。Secret data中的每个key都会成为mountPath目录下的一个以key为文件名的文件。

这是一个在卷中装载Secret的Pod示例：
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
    secret:
      secretName: mysecret
```

您要使用的每个secret都需要在`.spec.volumes`中引用.

如果Pod中有多个容器，那么每个容器都需要有自己的volumeMounts块，但`.spec.volumes`只需要一个Secret一个。

您可以将多个文件打包成一个Secret，也可以使用多个Secret，哪个方便就用哪个。

#### Projection of Secret keys to specific paths 

您还可以控制卷内映射密钥的路径。你可以使用`.spec.volumes[].secret.items`字段更改每个key的目标路径：
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
    secret:
      secretName: mysecret
      items:
      - key: username
        path: my-group/my-username
```

What will happen:
- username密码将存储在`/etc/foo/my-group/my-username`文件里，而不是`/etc/foo/username`文件
- password密码不会得到映射

如果使用`.spec.volumes[].secret.items`，那么只有在items里指定的keys才会映射。要使用Secret中的所有密钥，必须在items字段中列出所有密钥。所有列出的key必须存在于相应的Secret中。否则，不会创建卷。

#### Secret files permissions 

您可以为单个密钥设置文件访问权限位。如果未指定任何权限，则默认使用0644。如果需要，还可以为整个Secret卷设置默认模式，并覆盖所有key。

例如，可以指定如下默认模式：
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
  volumes:
  - name: foo
    secret:
      secretName: mysecret
      defaultMode: 0400
```

然后，Secret将被装载到/etc/foo上，并且由secret volume mount创建的所有文件将具有0400权限。

注意，JSON规范不支持八进制表示法，因此使用值256表示0400权限。如果Pod使用YAML而不是JSON，那么可以使用八进制表示法以更自然的方式指定权限。

注意，如果您使用kubectl exec进入Pod，则需要按照符号链接symlink找到预期的文件模式。例如，

检查Pod上的Secret文件模式。
```sh
kubectl exec mypod -it sh

cd /etc/foo
ls -l
```

The output is similar to this:

```sh
total 0
lrwxrwxrwx 1 root root 15 May 18 00:18 password -> ..data/password
lrwxrwxrwx 1 root root 15 May 18 00:18 username -> ..data/username
```

按照符号链接找到正确的文件模式。
```sh
cd /etc/foo/..data
ls -l
```
The output is similar to this:
```sh
total 8
-r-------- 1 root root 12 May 18 00:18 password
-r-------- 1 root root  5 May 18 00:18 username
```

也可以使用映射，如前一个示例中所示，并为不同的文件指定不同的权限，如下所示：

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
  volumes:
  - name: foo
    secret:
      secretName: mysecret
      items:
      - key: username
        path: my-group/my-username
        mode: 0777
```

在本例中，生成的`/etc/foo/my-group/my-username`文件的权限值将为0777。如果使用JSON，由于JSON的限制，必须以十进制表示法511指定模式。

请注意，如果您稍后阅读此权限值，它可能会以十进制表示法显示。

#### Consuming Secret values from volumes 
在装载Secret卷的容器中，密钥以文件的形式出现，secret值被base64解码并存储在这些文件中。这是在容器内执行上述示例中的命令的结果：
```sh
ls /etc/foo/
```
The output is similar to:
```sh
username
password
```

```sh
cat /etc/foo/username
```
The output is similar to:
```sh
admin
```

```sh
cat /etc/foo/password
```
The output is similar to:
```sh
1f2d1e2e67df
```

容器中的程序负责从文件中读取秘钥。

#### Mounted Secrets are updated automatically

当卷中当前使用的Secret被更新时，投影的密钥最终也会被更新。kubelet会在每次周期性同步中检查挂载的秘密是否是新的。但是，kubelet使用它的本地缓存来获取Secret的当前值。可以使用[KubeletConfiguration struct](https://github.com/kubernetes/kubernetes/blob/master/staging/src/k8s.io/kubelet/config/v1beta1/types.go)中的`ConfigMapAndSecretChangeDetectionStrategy`字段配置缓存的类型。Secret可以通过watch（默认），基于ttl，或者简单地将所有请求直接重定向到API服务器来传播(propagated)。因此，从Secret更新到新Secret投射到Pod的总延迟是，kubelet同步周期+缓存传播延迟，其中缓存传播延迟取决于所选的缓存类型（等于观察传播延迟、缓存的ttl或零）。

:notebook: 注意：使用Secret作为子路径卷装入的容器将不会接收Secret的更新。

### Using Secrets as environment variables

要在Pod中的环境变量中使用Secret，请执行以下操作：
1. 创建一个Secret或使用现有的Secret。多个Pod可以引用同一个Secret。
2. 修改要使用密钥值的每个容器的Pod定义，为要使用的每个密钥添加一个环境变量。环境变量要使用secret key需要在`env[].valueFrom.secretKeyRef`中指定secret的名字和key.
3. 修改镜像和/或命令行，以便程序从指定的环境变量中查找值。

这是一个从环境变量中使用Secret的Pod示例：
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: secret-env-pod
spec:
  containers:
  - name: mycontainer
    image: redis
    env:
      - name: SECRET_USERNAME
        valueFrom:
          secretKeyRef:
            name: mysecret
            key: username
      - name: SECRET_PASSWORD
        valueFrom:
          secretKeyRef:
            name: mysecret
            key: password
  restartPolicy: Never
```

#### Consuming Secret Values from environment variables
在使用环境变量中的secret的容器中，Secret keys显示为普通环境变量，其中包含Secret数据的base64解码值。这是在容器内执行上述示例中的命令的结果：
```sh
echo $SECRET_USERNAME
```
The output is similar to:
```sh
admin
```

```sh
echo $SECRET_PASSWORD
```
The output is similar to:
```sh
1f2d1e2e67df
```

## Immutable Secrets 

*FEATURE STATE: Kubernetes v1.19 [beta]*
Kubernetes beta特性不可变Secrets和ConfigMaps提供了一个选项，可以将Secret和ConfigMaps设置为不可变的。对于广泛使用Secrets的集群（至少有上万个不同的Pod挂载Secret），防止其数据更改具有以下优势：

- 防止意外（或不需要的）更新导致应用程序中断
- 通过关闭标记为不可变secret的watches，显著减少kube-apiserver上的负载，从而提高集群的性能。

这个特性由ImmutableEphemeralVolumes[特性门](https://kubernetes.io/docs/reference/command-line-tools-reference/feature-gates/)控制，从v1.19开始默认启用。您可以通过将immutable字段设置为true来创建一个不可变的secret。例如，
```yaml
apiVersion: v1
kind: Secret
metadata:
  ...
data:
  ...
immutable: true
```

:warning: 注意：一旦Secret或ConfigMap标记为不可变的，就不可能恢复此更改，也不可能更改数据字段的内容。您只能删除并重新创建该Secret。现有的Pod会保留一个挂载点，指向已删除的Secret, 建议重新创建这些pod

### Using imagePullSecrets

imagePullSecrets字段是同一命名空间中Secrets的引用列表。可以使用imagePullSecrets将包含Docker（或其他）image registry密码的secret传递给kubelet。kubelet使用这些信息来代表你的pod拉取私人镜像。有关imagePullSecrets字段的更多信息，请参见[podspec api](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#podspec-v1-core)。

1. Manually specifying an imagePullSecret
   您可以从[容器镜像文档](https://kubernetes.io/docs/concepts/containers/images/#specifying-imagepullsecrets-on-a-pod)了解如何指定ImagePullSecrets。

### Arranging for imagePullSecrets to be automatically attached 安排自动附加imagePullSecrets

您可以手动创建imagePullSecrets，并从ServiceAccount引用它。使用该ServiceAccount创建的或默认情况下使用该ServiceAccount创建的任何pod，都会将其imagePullSecrets字段设置为服务帐户的imagePullSecrets字段。有关该过程的详细说明，请参见[将ImagePullSecrets添加到服务帐户](https://kubernetes.io/docs/tasks/configure-pod-container/configure-service-account/#add-imagepullsecrets-to-a-service-account)

### Automatic mounting of manually created Secrets 自动安装手动创建的Secrets

手动创建的Secrets（例如，包含用于访问GitHub帐户的令牌）可以根据pods的服务帐户自动附加到pods。有关该过程的详细说明，请参见[使用PodPreset将信息注入Pods](https://kubernetes.io/docs/tasks/inject-data-application/podpreset/)。


## Details
### Restrictions
secret卷源要通过验证来确保指定的对象引用实际指向Secret类型的对象。因此，需要在任何依赖它的Pod之前创建Secret。

Secret资源位于命名空间中。Secret只能由同一命名空间中的pod引用。

单个Secret的大小限制在1MiB。这是为了阻止创建非常大的Secret，因为这会耗尽API服务器和kubelet内存。然而，创造许多小Secret也会耗尽内存。更全面的内存使用限制是一个正在计划中的功能。

kubelet只支持Pod使用从API服务器获取的Secret。包括使用kubectl或通过复制控制器间接创建的任何pod。但不包括由kubelet `--manifest-url`标志、它的`--config`标志或REST api创建的pod（这些不是创建pod的常用方法）

在作为环境变量在pod中使用之前，必须先创建Secret，除非它们被标记为可选。引用不存在的Secret将阻止Pod启动。

如果对Secret key的引用（secretKeyRef字段）不存在，将阻止Pod启动。

envFrom字段使用Secrets来填充环境变量，如果envFrom指定的key被认为是无效的环境变量名，则将跳过这些key。并允许Pod启动。然后会有一个事件触发，其原因是InvalidVariableNames，其消息中将包含跳过的无效key的列表。该示例显示了一个pod，它引用了default/mysecret，其中包含2个无效键：1badkey和2alsobad。

```sh
kubectl get events
```

The output is similar to:
```sh
LASTSEEN   FIRSTSEEN   COUNT     NAME            KIND      SUBOBJECT                         TYPE      REASON
0s         0s          1         dapi-test-pod   Pod                                         Warning   InvalidEnvironmentVariableNames   kubelet, 127.0.0.1      Keys [1badkey, 2alsobad] from the EnvFrom secret default/mysecret were skipped since they are considered invalid environment variable names.
```

### Secret and Pod lifetime interaction 

当通过调用kubernetes api创建Pod时，不会检查引用的secret是否存在。一旦一个Pod被调度，kubelet将尝试获取secret值。如果secret不存在或暂时无法与API服务器连接而无法获取secret，kubelet将定期重试。它将报告一个关于pod的事件，解释它尚未启动的原因。一旦获取了secret，kubelet将创建并装载一个包含secret的卷。在吊舱的所有卷都装上之前，所有的容器都不会启动。

## Use cases
### Use-Case: As container environment variables
Create a secret
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: mysecret
type: Opaque
data:
  USER_NAME: YWRtaW4=
  PASSWORD: MWYyZDFlMmU2N2Rm
```
Create the Secret:
```sh
kubectl apply -f mysecret.yaml
```

使用envFrom将secret的所有数据定义为容器环境变量。来自secret的key将成为Pod中的环境变量名。

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: secret-test-pod
spec:
  containers:
    - name: test-container
      image: k8s.gcr.io/busybox
      command: [ "/bin/sh", "-c", "env" ]
      envFrom:
      - secretRef:
          name: mysecret
  restartPolicy: Never
```

### Use-Case: Pod with ssh keys

创建一个包含一些ssh keys的secret：

```sh
kubectl create secret generic ssh-key-secret --from-file=ssh-privatekey=/path/to/.ssh/id_rsa --from-file=ssh-publickey=/path/to/.ssh/id_rsa.pub
```
The output is similar to:
```sh
secret "ssh-key-secret" created
```

也可以创建一个包含ssh keys的secretGenerator字段的`kustomization.yaml`文件。

:warning: 注意：在发送您自己的ssh keys之前请仔细考虑：集群的其他用户可能有权访问这个secret。使用一个服务帐户，让您希望与之共享Kubernetes集群的所有用户都可以访问该帐户，并且可以在用户受到危害时撤销该帐户。

现在，您可以创建一个Pod，该Pod引用了包含ssh key的secret，并在卷中使用secret：
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: secret-test-pod
  labels:
    name: secret-test
spec:
  volumes:
  - name: secret-volume
    secret:
      secretName: ssh-key-secret
  containers:
  - name: ssh-test-container
    image: mySshImage
    volumeMounts:
    - name: secret-volume
      readOnly: true
      mountPath: "/etc/secret-volume"
```

当容器的命令运行时，key将在以下位置可用：
```sh
/etc/secret-volume/ssh-publickey
/etc/secret-volume/ssh-privatekey
```

然后容器可以自由地使用secret数据来建立ssh连接


### Use-Case: Pods with prod / test credentials 

此示例演示了一个Pod使用生产环境凭据的Secret，和另一个Pod使用测试环境凭据的Secret。

您可以创建一个有secretGenerator字段的kustomization.yaml文件，或者运行`kubectl create secret`命令。

```sh
kubectl create secret generic prod-db-secret --from-literal=username=produser --from-literal=password=Y4nys7f11
```
The output is similar to:
```sh
secret "prod-db-secret" created
```

还可以为测试环境凭据创建secret。
```sh
kubectl create secret generic test-db-secret --from-literal=username=testuser --from-literal=password=iluvtests
```
The output is similar to:
```sh
secret "test-db-secret" created
```

> :cheese: 特殊字符，如`$`、`\`、`*`、`=`、和`!`会被shell解释，所以需要进行转义。在大多数shell中，转义密码的最简单方法是用单引号（'）将其括起来。例如，如果您的实际密码是`S!B\*d$zDsb=`，您应该执行以下命令：
> `kubectl create secret generic dev-db-secret --from-literal=username=devuser --from-literal=password='S!B\*d$zDsb='`
> 从文件中获取的密码则不需要进行转义（--from file）。

现在制作pod：
```sh
cat <<EOF > pod.yaml
apiVersion: v1
kind: List
items:
- kind: Pod
  apiVersion: v1
  metadata:
    name: prod-db-client-pod
    labels:
      name: prod-db-client
  spec:
    volumes:
    - name: secret-volume
      secret:
        secretName: prod-db-secret
    containers:
    - name: db-client-container
      image: myClientImage
      volumeMounts:
      - name: secret-volume
        readOnly: true
        mountPath: "/etc/secret-volume"
- kind: Pod
  apiVersion: v1
  metadata:
    name: test-db-client-pod
    labels:
      name: test-db-client
  spec:
    volumes:
    - name: secret-volume
      secret:
        secretName: test-db-secret
    containers:
    - name: db-client-container
      image: myClientImage
      volumeMounts:
      - name: secret-volume
        readOnly: true
        mountPath: "/etc/secret-volume"
EOF
```

把pod追加进同一个kustomization.yaml文件:
```sh
cat <<EOF >> kustomization.yaml
resources:
- pod.yaml
EOF
```

通过运行以下命令在API服务器上应用所有这些对象：

```sh
kubectl apply -k .
```
两个容器的文件系统上都会有以下文件，其中包含各自容器环境的值：
```sh
/etc/secret-volume/username
/etc/secret-volume/password
```

请注意两个Pod的规范只在一个字段上是不同的；这有助于从一个公共Pod模板创建具有不同功能的Pod。

您可以使用两个服务帐户进一步简化基本pod规范：
1. 具有prod-db-secret的prod-user
2. 使用test-db-secret测test-user

Pod规范则可以简化为：
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: prod-db-client-pod
  labels:
    name: prod-db-client
spec:
  serviceAccount: prod-db-client
  containers:
  - name: db-client-container
    image: myClientImage
```


### Use-case: dotfiles in a secret volume 

通过定义以点开头的key，可以使数据“隐藏”。此key表示了一个点文件dotfile或“隐藏”文件。例如，当以下Secret装入卷时，secret-volume：

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: dotfile-secret
data:
  .secret-file: dmFsdWUtMg0KDQo=
---
apiVersion: v1
kind: Pod
metadata:
  name: secret-dotfiles-pod
spec:
  volumes:
  - name: secret-volume
    secret:
      secretName: dotfile-secret
  containers:
  - name: dotfile-test-container
    image: k8s.gcr.io/busybox
    command:
    - ls
    - "-l"
    - "/etc/secret-volume"
    volumeMounts:
    - name: secret-volume
      readOnly: true
      mountPath: "/etc/secret-volume"
```

该卷将包含一个名为`.secret-file`的文件，`dotfile-test-container`将在路径`/etc/secret-volume/.secret-file`中包含该文件。

> :+1: 注意：以点字符开头的文件对`ls -l`的输出是隐藏的；当列出目录内容时，必须使用`ls -la`来查看它们。

### Use-case: Secret visible to one container in a Pod
考虑一个程序，它需要处理HTTP请求，执行一些复杂的业务逻辑，然后使用HMAC对一些消息进行签名。因为它具有复杂的应用程序逻辑，所以服务器中可能存在未被注意到的远程文件读取漏洞，这可能会将私钥暴露给攻击者。

这可以分为两个容器中的两个进程：一个前端容器处理用户交互和业务逻辑，但看不到私钥；一个签名者容器，可以看到私钥，并响应来自前端的简单签名请求（例如，通过本地主机网络）。

使用这种分区方法，攻击者现在必须诱使应用程序服务器执行任意操作，这可能比让它读取文件更困难。


## Best practices

### Clients that use the Secret API

当部署与secret api交互的应用程序时，应该使用[RBAC](https://kubernetes.io/docs/reference/access-authn-authz/rbac/)等[授权策略](https://kubernetes.io/docs/reference/access-authn-authz/authorization/)来限制访问。

Secrets often hold values that span a spectrum of importance, many of which can cause escalations within Kubernetes (e.g. service account tokens) and to external systems. Even if an individual app can reason about the power of the secrets it expects to interact with, other apps within the same namespace can render those assumptions invalid. 
Secrets包含的值通常跨越一系列重要的领域，其中许多可能导致Kubernetes（例如服务帐户令牌）内部和外部系统的升级。即使一个单独的应用程序可以推断出它希望与之交互的秘密的能力，但同一命名空间中的其他应用程序也可能使这些假设无效。

由于这些原因，监视watch和列出list命名空间中secret的请求是非常强大的功能，应该避免，因为列出secrets允许客户端检查该命名空间中所有secret的值。监视和列出集群中所有secret的能力应该只保留给最有特权的系统级组件。

需要访问Secret API的应用程序应该对它们需要的secrets执行get请求。这允许管理员限制对所有secrets的访问，同时使用[白名单访问策略来限制应用程序访问单个实例](https://kubernetes.io/docs/reference/access-authn-authz/rbac/#referring-to-resources)。

为了提高循环get的性能，客户端可以设计一个引用secret的资源，然后监视watch该资源，在引用更改时再重新请求secret。此外，还有一个让["bulk watch" API](https://github.com/kubernetes/community/blob/master/contributors/design-proposals/api-machinery/bulk_watch.md)支持客户机监视单个资源的提议，并可能在Kubernetes的未来版本中提供。

## Security properties 

### Protections 

因为secrets可以独立于使用它们的Pods创建，所以在创建、查看和编辑Pods的工作流中，secrets被暴露的风险更小。系统还可以对secrets采取额外的预防措施，例如尽可能避免将其写入磁盘。

只有当节点上的Pod需要时，才会将secret发送到该节点。kubelet将secret存储到tmpfs中，这样secret就不会写入磁盘存储。一旦依赖secret的Pod被删除，kubelet也将删除其本地的secret数据副本。

同一个节点上的几个pod可能都有secret。然而，只有Pod请求的secret在其容器中是可见的。因此，一个pod无法获得另一个pod的secret。

一个pod里可能有好几个容器。但是，Pod中的每个容器都必须在其volumeMounts中请求secret卷，以便在容器中可见。这可以用来[在Pod级别构造有用的安全分区](https://kubernetes.io/docs/concepts/configuration/secret/#use-case-secret-visible-to-one-container-in-a-pod)。

在大多数Kubernetes发行版中，用户与API服务器之间以及从API服务器到kubelets之间的通信都受SSL/TLS的保护。通过这些通道传输的secrets是受保护的。

*FEATURE STATE: Kubernetes v1.13 [beta]*
您可以对secret数据启用[encryption at rest](https://kubernetes.io/docs/tasks/administer-cluster/encrypt-data/)，这样secret就不会以明文方式存储在etcd中。


### Risks

- 在API Server中，Secret数据存储在etcd中，因此：
  - 管理员应为群集数据启用encryption at rest（需要v1.13或更高版本）。
  - 管理员应限制admin用户访问etcd。
  - 当不再使用时，管理员可能希望擦除/粉碎etcd使用的磁盘。
  - 如果在集群中运行etcd，管理员应该确保使用SSL/TLS进行etcd对等通信。
- 如果通过清单（JSON或YAML）文件配置secret，其中secret data使用base64加密，则如果共享此文件或将其上传到源存储库意味着secret已泄露。Base64编码不是一种加密方法，可以认为它与纯文本相同。
- 在从卷中读取secret之后，应用程序仍然需要保护secret的值，例如不要意外地将其记录下来或将其传输给不受信任的一方。
- 一个可以创建一个使用secret的Pod的用户也可以看到这个secret的值。即使API server策略不允许该用户读取secret，用户也可以运行一个公开secret的Pod。
- 目前，在任何节点上具有root权限的任何人都可以通过模拟kubelet从API服务器读取任何secret。计划中的一个功能是只向实际需要secret的节点发送secret，以限制root攻击对单个节点的影响。


## What's next
* Learn how to [manage Secret using kubectl](https://kubernetes.io/docs/tasks/configmap-secret/managing-secret-using-kubectl/)
* Learn how to [manage Secret using config file](https://kubernetes.io/docs/tasks/configmap-secret/managing-secret-using-config-file/)
* Learn how to [manage Secret using kustomize](https://kubernetes.io/docs/tasks/configmap-secret/managing-secret-using-kustomize/)