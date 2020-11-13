# Pod Security Standards

pod的安全设置通常通过使用[安全上下文](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/)来应用。安全上下文允许在每个Pod的基础上定义特权和访问控制。

集群级别的安全上下文的enforcement实施和基于策略的定义，以前是使用[Pod安全策略](https://kubernetes.io/docs/concepts/policy/pod-security-policy/)实现的。Pod安全策略是集群级别的资源，控制了Pod规范中安全相关的内容。

然而，大量的策略实施方法的出现，增强或取代了PodSecurityPolicy的使用。本页的目的是详细介绍推荐的Pod安全配置，与任何特定的实例化无关。

## Policy Types

需要使用一些基本的策略定义来涵盖整个安全的范畴。从高度限制到高度灵活，策略类型可分为：
- 特权Privileged —— 不受限制的策略，提供尽可能广泛的权限级别。此策略允许已知权限升级。
- 基线/默认Baseline/Default —— 在防止已知权限升级的同时使用最小限制的策略。允许默认的（minimally specified最小配置的）Pod配置。
- 受限Restricted  —— 严格限制策略，严格遵循当前Pod。

## Policies
### Privileged

特权策略是有意开放的，完全不受限制。这种类型的策略通常针对由特权、受信任用户管理的系统和基础级别的工作负载。

The privileged policy is defined by an absence of restrictions. For allow-by-default enforcement mechanisms (such as gatekeeper), the privileged profile may be an absence of applied constraints rather than an instantiated policy. In contrast, for a deny-by-default mechanism (such as Pod Security Policy) the privileged policy should enable all controls (disable all restrictions).
特权策略的定义是没有限制的。对于默认允许的强制机制（例如gatekeeper），privileged profile可能没有应用的约束，而不是没有实例化的策略。相反，对于默认拒绝机制（如Pod安全策略），特权策略可以启用所有控制（也就是禁用了所有限制）。

### Baseline/Default
The Baseline/Default policy is aimed at ease of adoption for common containerized workloads while preventing known privilege escalations. This policy is targeted at application operators and developers of non-critical applications. The following listed controls should be enforced/disallowed:
基线/默认策略的目的是在防止已知权限升级的同时，简化常见容器化工作负载使用安全策略。此策略针对非关键应用程序的应用程序操作员和开发人员。应强制/禁止以下列出的控制措施：

```
Control        Policy
Host            必须禁止共享主机命名空间。
Namespaces      限制字段：
                    spec.hostNetwork
                    spec.hostPID
                    spec.hostIPC
                Allowed Values: false

Privileged      特权Pods会禁用大多数安全机制，必须禁止使用。
Containers      限制字段：
                    spec.containers[*].securityContext.privileged
                    spec.initContainers[*].securityContext.privileged
                Allowed Values: false, undefined/nil

Capabilities    必须禁止在默认设置之外添加其他功能。
                限制字段：
                    spec.containers[*].securityContext.capabilities.add
                    spec.initContainers[*].securityContext.capabilities.add
                Allowed Values: empty (or restricted to a known list)

HostPath        必须禁止主机路径卷。
Volumes         限制字段：
                    spec.volumes[*].hostPath
                Allowed Values: undefined/nil

Host Ports      主机端口应该是不允许的，或者至少限于已知列表。
                限制字段：
                    spec.containers[*].ports[*].hostPort
                    spec.initContainers[*].ports[*].hostPort
                Allowed Values: 0, undefined (or restricted to a known list)

AppArmor        在受支持的主机上，默认应用"runtime/default" AppArmor配置文件。这个默认策略应防止
(optional)      重写或禁用该策略，或将重写限制为允许的一组配置文件。
                限制字段：
                    metadata.annotations['container.apparmor.security.beta.kubernetes.io/*']
                Allowed Values: 'runtime/default', undefined

SELinux         不允许设置自定义SELinux选项。
(optional)      限制字段：
                    spec.securityContext.seLinuxOptions
                    spec.containers[*].securityContext.seLinuxOptions
                    spec.initContainers[*].securityContext.seLinuxOptions
                Allowed Values: undefined/nil

/proc Mount     默认/proc掩码是为了减少攻击面而设置的，应该是必需的。
Type            限制字段：
                    spec.containers[*].securityContext.procMount
                    spec.initContainers[*].securityContext.procMount
                Allowed Values: undefined/nil, 'Default'

Sysctls         Sysctls可以禁用安全机制或影响主机上的所有容器，除了允许的“安全”子集外，
                应该禁止使用。如果sysctl在容器或Pod中是限定了命名空间的，并且它与同一节
                点上的其他Pod或进程隔离，则认为此时使用sysctl是安全的。    
                限制字段：
                    spec.securityContext.sysctls
                Allowed Values:
                    kernel.shm_rmid_forced
                    net.ipv4.ip_local_port_range
                    net.ipv4.tcp_syncookies
                    net.ipv4.ping_group_range
                    undefined/empty  
```


### Restricted

限制策略旨在以牺牲兼容性为代价，强制执行当前的Pod的安全防护措施。专门针对security-critical应用程序的操作员和开发人员，以及可信度低的用户。用户应强制/禁止以下列出的控制措施：
```
Control         Policy
Everything from the default profile.
Volume Types    除了限制HostPath卷之外，restricted profile还将非核心卷类型的使用限制为
                通过PersistentVolumes定义的卷类型。
                限制字段：
                    spec.volumes[*].hostPath
                    spec.volumes[*].gcePersistentDisk
                    spec.volumes[*].awsElasticBlockStore
                    spec.volumes[*].gitRepo
                    spec.volumes[*].nfs
                    spec.volumes[*].iscsi
                    spec.volumes[*].glusterfs
                    spec.volumes[*].rbd
                    spec.volumes[*].flexVolume
                    spec.volumes[*].cinder
                    spec.volumes[*].cephFS
                    spec.volumes[*].flocker
                    spec.volumes[*].fc
                    spec.volumes[*].azureFile
                    spec.volumes[*].vsphereVolume
                    spec.volumes[*].quobyte
                    spec.volumes[*].azureDisk
                    spec.volumes[*].portworxVolume
                    spec.volumes[*].scaleIO
                    spec.volumes[*].storageos
                    spec.volumes[*].csi
                Allowed Values: undefined/nil

Privilege       不应允许权限提升（例如通过set user ID或set group ID file mode）。
Escalation      限制字段：
                    spec.containers[*].securityContext.allowPrivilegeEscalation
                    spec.initContainers[*].securityContext.allowPrivilegeEscalation
                Allowed Values: false

Running as      必须要求容器以非根用户身份运行。
Non-root        限制字段：
                    spec.securityContext.runAsNonRoot
                    spec.containers[*].securityContext.runAsNonRoot
                    spec.initContainers[*].securityContext.runAsNonRoot
                Allowed Values: true

Non-root        应该禁止容器与根主GID或辅助GID一起运行。
groups          限制字段：
(optional)          spec.securityContext.runAsGroup
                    spec.securityContext.supplementalGroups[*]
                    spec.securityContext.fsGroup
                    spec.containers[*].securityContext.runAsGroup
                    spec.initContainers[*].securityContext.runAsGroup
                Allowed Values:
                    non-zero
                    undefined / nil (except for `*.runAsGroup`)

Seccomp         运行时默认的seccomp配置文件是必需的，或者允许特定的附加配置文件。
                限制字段：
                    spec.securityContext.seccompProfile.type
                    spec.containers[*].securityContext.seccompProfile
                    spec.initContainers[*].securityContext.seccompProfile
                Allowed Values:
                    'runtime/default'
                    undefined / nil    
```


## Policy Instantiation

将策略定义与策略实例化分离，可以跨集群对策略有一个共同的理解和一致的语言，而不依赖于底层的强制机制。

随着机制的成熟，将根据每个策略对其进行定义。执行策略的方法在这里没有定义。

[PodSecurityPolicy](https://kubernetes.io/docs/concepts/policy/pod-security-policy/)
* [Privileged](https://raw.githubusercontent.com/kubernetes/website/master/content/en/examples/policy/privileged-psp.yaml)
* [Baseline](https://raw.githubusercontent.com/kubernetes/website/master/content/en/examples/policy/baseline-psp.yaml)
* [Restricted](https://raw.githubusercontent.com/kubernetes/website/master/content/en/examples/policy/restricted-psp.yaml)

## FAQ

**Why isn't there a profile between privileged and default?为什么在特权和默认之间没有一个配置文件？**

这里定义的三个profiles有一个从最安全（受限）到最不安全（特权）的明显线性过渡，并涵盖了广泛的工作负载集。特权在基准策略之上且通常是特定于应用程序的，因此我们不提供对应的标准profile。这并不是说在这种情况下应该始终使用特权profile，而是需要根据具体情况定义相应策略。

SIG Auth may reconsider this position in the future, should a clear need for other profiles arise.
SIG Auth可能会重新考虑这一立场，如果对其他配置文件有明确的需求。 

**What's the difference between a security policy and a security context?安全策略和安全上下文有什么区别？**

[安全上下文](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/)在运行时配置pod和容器。安全上下文被定义为Pod清单中Pod和容器规范的一部分，并表示容器运行时的参数。

安全策略是控制平面机制，用于在安全上下文中强制配置特定的设置，以及配置在安全上下文之外的其他参数。截至2020年2月，当前强制执行这些安全策略的原生解决方案是[Pod security Policy](https://kubernetes.io/docs/concepts/policy/pod-security-policy/) —— 一种在集群中集中实施Pod安全策略的机制。Kubernetes生态系统中正在开发其他执行安全策略的替代方案，例如[OPA Gatekeeper](https://github.com/open-policy-agent/gatekeeper)。

**What profiles should I apply to my Windows Pods?我应该将哪些配置文件应用于我的Windows Pods？**

Windows在Kubernetes中与基于Linux的标准工作负载有一些限制和区别。具体来说，Pod SecurityContext字段[对Windows没有作用](https://kubernetes.io/docs/setup/production-environment/windows/intro-windows-in-kubernetes/#v1-podsecuritycontext)。因此，目前还没有标准化的Pod安全配置文件。


**What about sandboxed Pods?沙盒Pods呢？**

目前还没有一个API标准来控制Pod是否被视为沙盒。沙盒pod可以通过使用沙盒运行时（例如gVisor或Kata容器）来标识，但是对于什么是沙盒运行时没有标准的定义。

沙盒工作负载所需的保护可能不同于其他工作负载。例如，当工作负载与底层内核隔离时，限制特权权限的需要会比较少。这使得需要更高权限的工作负载仍然是隔离的。

此外，沙盒工作负载的保护高度依赖于沙盒方法。因此，还没有可以应用于所有沙盒工作负载的统一的推荐策略。





