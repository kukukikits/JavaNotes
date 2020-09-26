# DNS for Services and Pods

## Introduction
Kubernetes DNS在集群上调度DNS Pod和服务，并通过配置kubelets让每个容器使用DNS服务的IP来解析DNS名。

### 什么东西有DNS名称？ What things get DNS names?

群集中定义的每个服务（包括DNS服务器本身）都被分配一个DNS名称。默认情况下，客户机Pod的DNS搜索列表将包括Pod自己的命名空间和集群的默认域。这一点最好的例子是：

假设Kubernetes命名空间`bar`中有一个名为`foo`的服务。运行在命名空间`bar`中的Pod可以通过简单地对`foo`执行DNS查询来查找此服务。运行在命名空间`quux`中的Pod可以通过DNS查询`foo.bar`找到服务。

以下部分详细介绍了支持的记录类型和受支持的布局。发生在工作中的任何其他布局、名称或查询都被视为实现细节，可能会在没有警告的情况下进行更改。有关更多最新规范，请参阅[基于Kubernetes DNS的服务发现](https://github.com/kubernetes/dns/blob/master/docs/specification.md)。

## Services 

### A/AAAA records 

“正常”（非headless）服务被分配一个DNS A或AAAA记录，记录的格式为`my-svc.my-namespace.svc.cluster-domain.example`(svc: service的意思)，这取决于服务的IP族. 这个DNS记录解析后是服务的群集IP。

“Headless”（没有群集IP）服务也被分配一个DNS A或AAAA记录，记录格式为`my-svc.my-namespace.svc.cluster-domain.example`，这取决于服务的IP族. 与普通服务不同，这个记录将解析为所有被服务选择的pod的ip集合。此时客户机应该使用集合，或者使用标准round-robin方法从集合中选择。

### SRV records 

SRV记录是为正常或Headless服务的命名端口创建的。对于每个命名的端口，SRV记录的格式为`_my-port-name._my-port-protocol.my-svc.my-namespace.svc.cluster-domain.example`. 对于常规服务，这将解析为端口号, 以及域名：`my-svc.my-namespace.svc.cluster-domain.example`. 对于headless服务，这将解析为多个应答，服务后端的每个pod都有一个应答，包含pod的端口号, 以及域名：`auto-generated-name.my-svc.my-namespace.svc.cluster-domain.example`.

## Pods

### A/AAAA records 

一般来说，pod使用以下格式DNS：
`pod-ip-address.my-namespace.pod.cluster-domain.example`

例如，如果default命名空间中的一个pod的IP地址为172.17.0.3，并且集群的域名为`cluster.local`，则Pod有一个DNS名称：
`172-17-0-3.default.pod.cluster.local`

任何使用Deployment或DaemonSet创建的Pod，在使用Service暴露后都有如下可用的DNS：
`pod-ip-address.deployment-name.my-namespace.svc.cluster-domain.example`

### Pod's hostname and subdomain fields 

当前，当创建一个pod时，它的hostname是pod的`metadata.name`值。

Pod spec有一个可选的hostname字段，可用于指定Pod的主机名。指定时，它优先于Pod的名称作为Pod的主机名。例如，如果一个Pod的hostname设置为“my-host”，那么这个Pod的主机名将设置为“my-host”。

Pod spec还有一个可选的`subdomain`字段，可用于指定其子域。例如，在命名空间“my-namespace”中，`hostname`设置为“foo”、`subdomain`设置为“bar”的Pod将具有完全限定的域名（FQDN）`foo.bar.my-namespace.svc.cluster-domain.example`.

例子：
```yaml
apiVersion: v1
kind: Service
metadata:
  name: default-subdomain
spec:
  selector:
    name: busybox
  clusterIP: None
  ports:
  - name: foo # Actually, no port is needed.
    port: 1234
    targetPort: 1234
---
apiVersion: v1
kind: Pod
metadata:
  name: busybox1
  labels:
    name: busybox
spec:
  hostname: busybox-1
  subdomain: default-subdomain
  containers:
  - image: busybox:1.28
    command:
      - sleep
      - "3600"
    name: busybox
---
apiVersion: v1
kind: Pod
metadata:
  name: busybox2
  labels:
    name: busybox
spec:
  hostname: busybox-2
  subdomain: default-subdomain
  containers:
  - image: busybox:1.28
    command:
      - sleep
      - "3600"
    name: busybox
```

如果在与pod相同的命名空间中存在一个headless服务，并且与subdomain同名，那么集群的DNS服务器还会为pod的完全限定主机名返回A或AAAA记录。例如，给定一个主机名设置为“busybox-1”、子域设置为“default-subdomain”的Pod，以及同一命名空间中名为“default-subdomain”的headless服务，Pod将看到自己的FQDN为`busybox-1.default-subdomain.my-namespace.svc.cluster-domain.example`. DNS为该名称提供一个A或AAAA记录，指向Pod的IP。“busybox1”和“busybox2”都可以有各自不同的A或AAAA记录。

Endpoints对象可以指定任何端点地址的主机名及其IP。

> :warning: 注意：由于没有为pod name创建A或AAAA记录，因此需要hostname才能创建pod的A或AAAA记录。没有主机名但有子域的Pod只能为headless服务（`default-subdomain.my-namespace.svc.cluster-domain.example`)创建A或AAAA记录，指向pod的IP地址。另外，Pod需要准备就绪才能有记录，除非在服务上设置了`publishNotReadyAddresses=True`

### Pod's setHostnameAsFQDN field
*FEATURE STATE: Kubernetes v1.19 [alpha]*
Prerequisites: The `SetHostnameAsFQDN` [feature gate](https://kubernetes.io/docs/reference/command-line-tools-reference/feature-gates/) must be enabled for the API Server

当Pod配置为具有完全限定域名（FQDN）时，其主机名为短主机名。例如，如果您有一个完全限定域名`busybox-1.default-subdomain.my-namespace.svc.cluster-domain.example`，那么默认情况下，在Pod中执行命令`hostname`将返回busybox-1，执行`hostname--fqdn`命令返回FQDN。

在Pod规范中设置`setHostnameAsFQDN: true`时，kubelet会将Pod的FQDN写入该Pod命名空间的hostname。这种情况下，`hostname`和`hostname--fqdn`命令都返回Pod的FQDN。

> :warning: 注：
> 在Linux中，内核的hostname字段（struct utsname的nodename字段）限制为64个字符。
> 
> 如果Pod启用此功能并且其FQDN超过64个字符，则无法启动。Pod将保持在Pending状态（使用kubectl则看到ContainerCreating），生成错误事件，事件内容类似为：无法从Pod主机名和群集域构造FQDN，FQDN `long FDQN`太长（最多64个字符，请求70个字符）。改善此场景中用户体验的一种方法是创建一个[admission webhook controller](https://kubernetes.io/docs/reference/access-authn-authz/extensible-admission-controllers/#admission-webhooks)，以便在用户创建顶级对象（例如Deployment）时控制FQDN大小。

### Pod's DNS Policy

DNS策略可以设置为每个pod的基础属性。目前Kubernetes支持以下特定于pod的DNS策略。这些策略在Pod规范的`dnsPolicy`字段中指定。
- “Default”：Pod从运行pods的节点继承名称解析配置。详见[相关讨论](https://kubernetes.io/docs/tasks/administer-cluster/dns-custom-nameservers/#inheriting-dns-from-the-node)。
- “ClusterFirst”：与配置的群集域后缀不匹配的任何DNS查询，例如`www.kubernetes.io`被转发到从节点继承的上游nameserver。群集管理员可能配置了额外的存根域stub-domain和上游DNS服务器。有关在这些情况下如何处理DNS查询的详细信息，请参阅[相关讨论](https://kubernetes.io/docs/tasks/administer-cluster/dns-custom-nameservers/#effects-on-pods)。
- “ClusterFirstWithHostNet”：对于使用hostNetwork运行的Pods，应该显式地设置其DNS策略为“ClusterFirstWithHostNet”。
- “None”：它允许Pod忽略Kubernetes环境中的DNS设置。所有DNS设置都应该使用Pod规范中的dnsConfig字段提供。请参阅下面[Pod的DNS配置](https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/#pod-dns-config)小节。

> :bookmark: 注意：“Default”不是默认的DNS策略。如果未显式指定dnsPolicy，则使用“ClusterFirst”。

下面的示例显示了一个Pod，其DNS策略设置为“ClusterFirstWithHostNet”，因为它将hostNetwork设置为true。
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: busybox
  namespace: default
spec:
  containers:
  - image: busybox:1.28
    command:
      - sleep
      - "3600"
    imagePullPolicy: IfNotPresent
    name: busybox
  restartPolicy: Always
  hostNetwork: true
  dnsPolicy: ClusterFirstWithHostNet
```

### Pod's DNS Config

Pod的DNS配置允许用户更多地控制Pod的DNS设置。

dnsConfig字段是可选的，它可以和任何dnsPolicy设置一起使用。但是，当Pod的dnsPolicy设置为“None”时，必须指定dnsConfig字段。

以下是用户可以在dnsConfig字段中指定的属性：

- nameservers：将用作Pod的DNS服务器的IP地址列表。最多可以指定3个IP地址。当Pod的dnsPolicy设置为“None”时，列表必须至少包含一个IP地址，否则此属性是可选的。列表列出的服务器IP会和指定DNS策略生成的DNS服务器IP合并，重复的IP地址会被删除。
- searches: Pod中用于hostname lookup的DNS搜索域列表。此属性是可选的。指定后，提供的列表将合并到DNS策略生成的基本搜索域名中, 重复域名会被删除。Kubernetes最多允许6个搜索域。
- options：一个可选的对象列表，其中每个对象可以有一个name属性（必需）和一个value属性（可选）。此属性中的内容将合并到指定DNS策略生成的选项中。重复条目会被删除。

The following is an example Pod with custom DNS settings:

service/networking/custom-dns.yaml

```yaml
apiVersion: v1
kind: Pod
metadata:
  namespace: default
  name: dns-example
spec:
  containers:
    - name: test
      image: nginx
  dnsPolicy: "None"
  dnsConfig:
    nameservers:
      - 1.2.3.4
    searches:
      - ns1.svc.cluster-domain.example
      - my.dns.search.suffix
    options:
      - name: ndots
        value: "2"
      - name: edns0
```

当上面的Pod被创建时，container test的/etc/resolv.conf文件中会有如下内容：
```
nameserver 1.2.3.4
search ns1.svc.cluster-domain.example my.dns.search.suffix
options ndots:2 edns0
```

如果安装了IPv6，搜索路径和name server的设置如下所示：
```
nameserver fd00:79:30::a
search default.svc.cluster-domain.example svc.cluster-domain.example cluster-domain.example
options ndots:5
```

### Feature availability 
Pod DNS config和DNS policy “None”的可用性如下所示:
```
k8s  |  version	Feature support

1.14	Stable
1.10	Beta (on by default)
1.9	    Alpha
```

## What's next
For guidance on administering DNS configurations, check [Configure DNS Service](https://kubernetes.io/docs/tasks/administer-cluster/dns-custom-nameservers/)
