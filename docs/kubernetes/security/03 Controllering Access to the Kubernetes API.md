# Controlling Access to the Kubernetes API

本页提供控制对kubernetesapi的访问的概述。

用户使用kubectl、客户端库或通过发出REST请求来访问[kubernetes api](https://kubernetes.io/docs/concepts/overview/kubernetes-api/)。人类用户和[Kubernetes服务帐户](https://kubernetes.io/docs/tasks/configure-pod-container/configure-service-account/)都可以被授权访问API。当请求到达API时，它将经历几个阶段，如下图所示：

```
                       -------------------------------------------------------
                       |            1                 2               3       | 
Human User  ---------→ | --→   module 1      -→    module 1    --→ module 1   |     object store
                    ↗ |                    /                 /               | 4
                   /   |       module 2 ---/       module 2 -/     module 2 --|---→ object store
                  /    |                                                      |
Pod              /     |     Authentication      Authorization    Admission   |     object store
(Kubernetes     /      |                                           Control    |
Service Account)       |                Kubernetes Api Server                 |
                       -------------------------------------------------------
```

## Transport security

在典型的Kubernetes集群中，API服务的端口是443，受TLS保护。API服务器提供证书。此证书可以使用私有证书颁发机构（CA）签名，也可以基于链接到公认CA的公钥基础设施(public key infrastructure)进行签名。

如果您的集群使用私有证书颁发机构，那么您需要在客户机上的`~/.kube/config`中配置该CA证书的副本，这样您就可以信任连接并确信它没有被截获。

在此阶段，您的客户端可以提供TLS客户端证书。

## Authentication 

一旦建立了TLS，HTTP请求将转移到身份验证步骤。如图中的步骤1所示。群集创建脚本或群集管理员将API服务器配置为运行一个或多个验证器模块。[身份验证](https://kubernetes.io/docs/reference/access-authn-authz/authentication/)中更详细地描述了认证器。

身份验证步骤的输入是整个HTTP请求；但是，它通常只检查头和/或客户端证书。

身份验证模块包括客户端证书、密码和普通令牌、引导令牌和JSON Web令牌（用于服务帐户）。

可以指定多个身份验证模块，在这种情况下，会依次尝试每个模块，直到其中一个模块成功。

如果请求无法通过身份验证，它将被拒绝，状态代码为401。否则，用户将以特定的用户名进行身份验证，该用户名可供后续步骤的决策。一些验证器还提供用户的组成员身份，而其他验证器则不提供。

虽然Kubernetes在访问控制决策和请求日志记录中使用用户名，但是它没有用户对象，也没有在API中存储用户名或其他有关用户的信息。

## Authorization 

当来自特定用户的请求认证通过后，必须对请求进行授权。如图中的步骤2所示。

请求必须包括请求者的用户名、请求的操作和受该操作影响的对象。如果现有策略声明用户有权限完成所请求的操作，则将授权该请求。

例如，如果Bob具有以下策略，则他只能在命名空间projectCaribou中读取pods：

```yaml
{
    "apiVersion": "abac.authorization.kubernetes.io/v1beta1",
    "kind": "Policy",
    "spec": {
        "user": "bob",
        "namespace": "projectCaribou",
        "resource": "pods",
        "readonly": true
    }
}
```

如果Bob发出以下请求，则该请求被授权，因为他被允许读取projectCaribou命名空间中的对象：

```yaml
{
  "apiVersion": "authorization.k8s.io/v1beta1",
  "kind": "SubjectAccessReview",
  "spec": {
    "resourceAttributes": {
      "namespace": "projectCaribou",
      "verb": "get",
      "group": "unicorn.example.org",
      "resource": "pods"
    }
  }
}
```

如果Bob请求写入（创建或更新）projectCaribou命名空间中的对象，则拒绝授权。如果Bob请求读取（获取）不同命名空间（如projectFish）中的对象，那么拒绝授权。

Kubernetes授权要求您使用公共REST属性与现有的组织范围或云提供商范围的访问控制系统进行交互。使用REST格式非常重要，因为这些控制系统可能与kubernetes api之外的其他API交互。

Kubernetes支持多种授权模块，如ABAC模式、RBAC模式和Webhook模式。当管理员创建集群时，他们配置应该在API服务器中使用的授权模块。如果配置了多个授权模块，Kubernetes会检查每个模块，如果有任何模块授权了请求，则请求可以继续。如果所有模块都拒绝请求，则请求被拒绝（HTTP状态代码403）。

要了解有关Kubernetes授权的更多信息，包括有关使用受支持的授权模块创建策略的详细信息，请参阅[授权](https://kubernetes.io/docs/reference/access-authn-authz/authorization/)。


## Admission control

准入控制模块是可以修改或拒绝请求的软件模块。除了授权模块可用的属性外，准入控制模块还可以访问正在创建或修改的对象的内容。

许可控制器作用于创建、修改、删除或连接（代理）对象的请求。许可控制器不会对仅仅读取对象的请求执行操作。配置多个准入控制器时，按顺序调用它们。

如图中的步骤3所示。

与身份验证和授权模块不同，如果任何许可控制器模块拒绝，则请求将立即被拒绝。

除了拒绝对象之外，准入控制器还可以为字段设置复杂的默认值。

可用的准入控制模块在[准入控制器](https://kubernetes.io/docs/reference/access-authn-authz/admission-controllers/)中有描述。

一旦一个请求通过了所有的许可控制器，它将使用相应API对象的验证例程进行验证，然后写入对象存储（如步骤4所示）。


## API server ports and IPs 

前面的讨论适用于发送到API服务器安全端口的请求。API服务器实际上可以在两个端口上提供服务：

默认情况下，Kubernetes API服务器在两个端口上提供HTTP服务：

1. `localhost` port:

    - 用于测试和引导，以及主节点的其他组件（调度程序、控制器管理器）与API对话
    - 无TLS
    - 默认值端口8080，用`--insecure-port`标志更改。
    - 默认IP为localhost，用`--insecure-bind-address`标志更改。
    - 请求绕过身份验证和授权模块。
    - 请求由许可控制模块处理。
    - 受需要访问主机的保护

2. “Secure port”:
    - 尽可能使用
    - 使用TLS。使用`--tls-cert-file`设置证书，使用`--tls-private-key-file`标志设置密钥。
    - 默认值端口6443，使用`--secure-port`标志更改。
    - 默认IP是第一个非本地主机网络接口，用`--bind-address`标志更改。
    - 请求由验证和授权模块处理。
    - 请求由许可控制模块处理。
    - 运行了验证和授权模块


## What's next
Read more documentation on authentication, authorization and API access control:

- [Authenticating](https://kubernetes.io/docs/reference/access-authn-authz/authentication/)
    - [Authenticating with Bootstrap Tokens](https://kubernetes.io/docs/reference/access-authn-authz/bootstrap-tokens/)
- [Admission Controllers](https://kubernetes.io/docs/reference/access-authn-authz/admission-controllers/)
    - [Dynamic Admission Control](https://kubernetes.io/docs/reference/access-authn-authz/extensible-admission-controllers/)
- [Authorization](https://kubernetes.io/docs/reference/access-authn-authz/authorization/)
  * [Role Based Access Control](https://kubernetes.io/docs/reference/access-authn-authz/rbac/)
  * [Attribute Based Access Control](https://kubernetes.io/docs/reference/access-authn-authz/abac/)
  * [Node Authorization](https://kubernetes.io/docs/reference/access-authn-authz/node/)
  * [Webhook Authorization](https://kubernetes.io/docs/reference/access-authn-authz/webhook/)
- [Certificate Signing Requests](https://kubernetes.io/docs/reference/access-authn-authz/certificate-signing-requests/)
  - including [CSR approval](https://kubernetes.io/docs/reference/access-authn-authz/certificate-signing-requests/#approval-rejection) and [certificate signing](https://kubernetes.io/docs/reference/access-authn-authz/certificate-signing-requests/#signing)
- Service accounts
  * [Developer guide](https://kubernetes.io/docs/tasks/configure-pod-container/configure-service-account/)
  * [Administration](https://kubernetes.io/docs/reference/access-authn-authz/service-accounts-admin/)


You can learn about:

- how Pods can use [Secrets](https://kubernetes.io/docs/concepts/configuration/secret/#service-accounts-automatically-create-and-attach-secrets-with-api-credentials) to obtain API credentials.