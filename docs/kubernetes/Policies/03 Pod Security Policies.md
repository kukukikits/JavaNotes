# Pod Security Policies
**FEATURE STATE: Kubernetes v1.19 [beta]**

Pod安全策略支持细粒度授权Pod创建和更新。

## What is a Pod Security Policy? 

Pod安全策略是控制Pod规范中安全相关内容的集群级资源。[PodSecurityPolicy](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#podsecuritypolicy-v1beta1-policy)对象定义了一组pod运行的时候必须使用的条件，和相关字段的默认值。PodSecurityPolicy对象允许管理员控制以下内容：

Control Aspect | Field Names
---------------|------------
运行特权容器privileged containers | [privileged](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#privileged)
host命名空间的使用  |   [hostPID](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#host-namespaces), [hostIPC](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#host-namespaces)
主机网络和端口的使用    |   [hostNetwork](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#host-namespaces), [hostPorts](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#host-namespaces)
卷类型的使用    |   [volumes](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#volumes-and-file-systems)
主机文件系统的使用  |   [allowedHostPaths](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#volumes-and-file-systems)
允许特定的FlexVolume驱动    |   [allowedFlexVolumes](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#flexvolume-drivers)
分配拥有pod卷的FSGroup  |   [fsGroup](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#volumes-and-file-systems)
需要只读root文件系统  | [readOnlyRootFilesystem](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#volumes-and-file-systems)
容器的用户和组ID    |   [runAsUser](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#users-and-groups), [runAsGroup](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#users-and-groups), [supplementalGroups](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#users-and-groups)
限制升级为root权限  |   [allowPrivilegeEscalation](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#privilege-escalation), [defaultAllowPrivilegeEscalation](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#privilege-escalation)
Linux capabilities  |   [defaultAddCapabilities](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#capabilities), [requiredDropCapabilities](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#capabilities), [allowedCapabilities](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#capabilities)
容器的SELinux上下文 |   [seLinux](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#selinux)
容器允许的Proc Mount类型    |   [allowedProcMountTypes](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#allowedprocmounttypes)
容器使用的AppArmor配置文件  |   [annotations](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#apparmor)
容器使用的seccomp配置文件   |   [annotations](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#seccomp)
容器使用的sysctl配置文件    |   [forbiddenSysctls](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#sysctl),[allowedUnsafeSysctls](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#sysctl)

## Enabling Pod Security Policies 

Pod安全策略的控制功能由许可控制器(可选的，但推荐使用)实现。PodSecurityPolicies是通过[启用许可控制器admission controller](https://kubernetes.io/docs/reference/access-authn-authz/admission-controllers/#how-do-i-turn-on-an-admission-control-plug-in)来开启的，但是在不授权任何策略的情况下这样做将阻止在集群中创建任何pods。

由于pod安全策略API（`policy/v1beta1/podsecuritypolicy`）是独立于许可控制器启用的，因此对于现有群集，建议在启用许可控制器之前添加和授权策略。

## Authorizing Policies 授权策略

创建PodSecurityPolicy资源时，Authorizing Policies什么也不做。为了使用Authorizing Policies，必须授权用户或目标pod的[服务帐户](https://kubernetes.io/docs/tasks/configure-pod-container/configure-service-account/)使用Authorizing Policies，通过在policy上使用`user`动词实现。

大多数kubernetes pod不是由用户直接创建的。相反，它们通常是通过控制器管理器作为部署Deployment、复制集ReplicaSet或其他模板控制器的一部分间接创建的。如果授予控制器对策略的访问权，那么所有控制器创建的pod都会被授予访问权限，因此授权策略的首选方法是给pod服务帐户授予访问权限（参见[示例](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#run-another-pod)）

### Via RBAC

[RBAC](https://kubernetes.io/docs/reference/access-authn-authz/rbac/)是一种标准的Kubernetes授权模式，可以很容易地用来授权策略的使用。

首先，Role或ClusterRole需要授予所需策略的访问权限。授予访问权限的方法如下所示：
```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: <role name>
rules:
- apiGroups: ['policy']
  resources: ['podsecuritypolicies']
  verbs:     ['use']
  resourceNames:
  - <list of policies to authorize>
```

然后将（群集）角色绑定到授权用户：
```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: <binding name>
roleRef:
  kind: ClusterRole
  name: <role name>
  apiGroup: rbac.authorization.k8s.io
subjects:
# Authorize specific service accounts:
- kind: ServiceAccount
  name: <authorized service account name>
  namespace: <authorized pod namespace>
# Authorize specific users (not recommended):
- kind: User
  apiGroup: rbac.authorization.k8s.io
  name: <authorized user name>
```

如果使用RoleBinding（而不是ClusterRoleBinding），只能给与绑定时相同的命名空间中运行的pod授予使用权限。RoleBinding可以与system groups配对使用，来给运行在命名空间中的所有Pods授予访问权限：
```yaml
# Authorize all service accounts in a namespace:
- kind: Group
  apiGroup: rbac.authorization.k8s.io
  name: system:serviceaccounts
# Or equivalently, all authenticated users in a namespace:
- kind: Group
  apiGroup: rbac.authorization.k8s.io
  name: system:authenticated
```

有关RBAC绑定的更多示例，请参阅[角色绑定示例](https://kubernetes.io/docs/reference/access-authn-authz/rbac#role-binding-examples)。有关授权PodSecurityPolicy的完整示例，请参见[下文](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#example)

### Troubleshooting 

- [控制器管理器](https://kubernetes.io/docs/reference/command-line-tools-reference/kube-controller-manager/)必须在安全的API端口运行，并且不得具有超级用户权限。要学习有关AR server访问控制的内容，请参阅控制对[控制访问Kubernetes API](https://kubernetes.io/docs/concepts/security/controlling-access)。

    如果控制器管理器通过可信API端口（也称为本地主机侦听器）连接，则请求将绕过身份验证和授权模块；所有PodSecurityPolicy对象都将被允许，users would be able to create grant themselves the ability to create privileged containers. 并且用户将能够给自己创建特权容器。

    有关配置控制器管理器授权的更多详细信息，请参阅[控制器角色](https://kubernetes.io/docs/reference/access-authn-authz/rbac/#controller-roles)。


## Policy Order
  
除了限制pod的创建和更新，pod安全策略还可以用于为它控制的许多字段提供默认值。当多个策略可用时，pod安全策略控制器根据以下条件选择策略：

1. PodSecurityPolicies允许pod保持原样，而不改变默认值或改变pod，这是首选的。这些不变的PodSecurityPolicies的顺序并不重要。
2. 如果必须对pod进行默认设置或更改，那么选择第一个允许Pod的PodSecurityPolicy（按名称排序）。

> 注意：在更新操作期间（在此期间不允许对pod规范进行修改），只有没有改变的PodSecurityPolicies才用于验证pod。


## Example 

本例假设您有一个正在运行的集群，并且启用了PodSecurityPolicy许可控制器，并且您具有集群管理权限

### 1. Set up

首先设置一个命名空间和一个服务帐户。我们将使用此服务帐户模拟非管理员用户。

```sh
kubectl create namespace psp-example
kubectl create serviceaccount -n psp-example fake-user
kubectl create rolebinding -n psp-example fake-editor --clusterrole=edit --serviceaccount=psp-example:fake-user
```

为了明确我们所模拟的用户并保存一些输入，创建两个别名：

```sh
alias kubectl-admin='kubectl -n psp-example'
alias kubectl-user='kubectl --as=system:serviceaccount:psp-example:fake-user -n psp-example'
Create a policy and a pod 
```

### Create a policy and a pod

在文件中定义示例的PodSecurityPolicy对象。这是一个简单地阻止创建特权pod的策略。PodSecurityPolicy对象的名称必须是有效的DNS子域名。

policy/example-psp.yaml 
```yaml
apiVersion: policy/v1beta1
kind: PodSecurityPolicy
metadata:
  name: example
spec:
  privileged: false  # Don't allow privileged pods!
  # The rest fills in some required fields.
  seLinux:
    rule: RunAsAny
  supplementalGroups:
    rule: RunAsAny
  runAsUser:
    rule: RunAsAny
  fsGroup:
    rule: RunAsAny
  volumes:
  - '*'

```

And create it with kubectl:
```sh
kubectl-admin create -f example-psp.yaml
```

现在，以非特权用户，尝试创建一个简单的pod：
```sh
kubectl-user create -f- <<EOF
apiVersion: v1
kind: Pod
metadata:
  name:      pause
spec:
  containers:
    - name:  pause
      image: k8s.gcr.io/pause
EOF
Error from server (Forbidden): error when creating "STDIN": pods "pause" is forbidden: unable to validate against any pod security policy: []
```

发生了什么？虽然创建了PodSecurityPolicy，但是pod的服务帐户和`fake-user`都没有使用新策略的权限：
```sh
kubectl-user auth can-i use podsecuritypolicy/example
no
```

创建rolebinding来给`fake-user`授予示例策略的`use`谓词：
> 注意：这不是推荐的方法！有关首选方法，请参阅[下一节](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#run-another-pod)。

```sh
kubectl-admin create role psp:unprivileged \
    --verb=use \
    --resource=podsecuritypolicy \
    --resource-name=example
role "psp:unprivileged" created

kubectl-admin create rolebinding fake-user:psp:unprivileged \
    --role=psp:unprivileged \
    --serviceaccount=psp-example:fake-user
rolebinding "fake-user:psp:unprivileged" created

kubectl-user auth can-i use podsecuritypolicy/example
yes
```

现在重试创建pod：

```yaml
kubectl-user create -f- <<EOF
apiVersion: v1
kind: Pod
metadata:
  name:      pause
spec:
  containers:
    - name:  pause
      image: k8s.gcr.io/pause
EOF
pod "pause" created
```

这个可以正常创建！但是，尝试任何创建特权pod时都会被拒绝：
```yaml
kubectl-user create -f- <<EOF
apiVersion: v1
kind: Pod
metadata:
  name:      privileged
spec:
  containers:
    - name:  pause
      image: k8s.gcr.io/pause
      securityContext:
        privileged: true
EOF
Error from server (Forbidden): error when creating "STDIN": pods "privileged" is forbidden: unable to validate against any pod security policy: [spec.containers[0].securityContext.privileged: Invalid value: true: Privileged containers are not allowed]
```

在继续之前删除pod：
```yaml
kubectl-user delete pod pause
```


