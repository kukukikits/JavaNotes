# Pod Overhead
**FEATURE STATE: Kubernetes v1.18 [beta]**

当您在一个节点上运行一个Pod时，这个Pod本身会占用大量的系统资源。这些资源是运行容器所需的额外资源。Pod开销是一个用于计算Pod基础设施在容器请求和限制之上消耗的资源的特性。

在Kubernetes中，Pod的开销是根据与Pod [RuntimeClass](https://kubernetes.io/docs/concepts/containers/runtime-class/)相关联的开销在[admission](https://kubernetes.io/docs/reference/access-authn-authz/extensible-admission-controllers/#what-are-admission-webhooks)时设置的。

启用Pod开销时，开销是指除了在调度Pod时容器资源请求的总和之外，还需要考虑的开销。类似地，当计算Pod cgroup的大小和执行Pod逐出排序时，Kubelet将包含Pod开销

## Enabling Pod Overhead 

您需要确保在整个集群中启用了PodOverhead feature gate（从1.18开始默认是打开的），并且使用了一个定义了overhead字段的RuntimeClass。

## Usage example

要使用PodOverhead特性，您需要一个定义了overhead字段的RuntimeClass。例如，您可以将以下RuntimeClass定义与虚拟化容器运行时一起使用，虚拟机和guest OS的每个Pod大约使用120MiB：

```yaml
---
kind: RuntimeClass
apiVersion: node.k8s.io/v1beta1
metadata:
    name: kata-fc
handler: kata-fc
overhead:
    podFixed:
        memory: "120Mi"
        cpu: "250m"
```

使用`kata-fc` RuntimeClass handler创建的工作负载，在计算资源配额、节点调度，以及Pod cgroup大小调整时会考虑指定的内存和CPU开销。

考虑运行下面的工作负载，test pod：
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: test-pod
spec:
  runtimeClassName: kata-fc
  containers:
  - name: busybox-ctr
    image: busybox
    stdin: true
    tty: true
    resources:
      limits:
        cpu: 500m
        memory: 100Mi
  - name: nginx-ctr
    image: nginx
    resources:
      limits:
        cpu: 1500m
        memory: 100Mi
```

在admission时，RuntimeClass[许可控制器](https://kubernetes.io/docs/reference/access-authn-authz/admission-controllers/)更新工作负载的PodSpec，使RuntimeClass中描述的overhead并入PodSpec。如果PodSpec已经定义了这个字段，那么Pod将被拒绝。在给定的示例中，由于只指定了RuntimeClass名称，所以许可控制器对Pod进行了修改，添加了overhead字段给Pod。

在RuntimeClass许可控制器处理后，您可以查看更新的PodSpec：
```sh
kubectl get pod test-pod -o jsonpath='{.spec.overhead}'
```

The output is:
```sh
map[cpu:250m memory:120Mi]
```

如果定义了ResourceQuota，则会计算容器请求和overhead字段的总和。

当kube调度器决定使用哪个节点运行新的Pod时，调度器会考虑这个Pod的overhead以及该Pod的容器请求的总和。对于本例，调度程序会把请求总量和overhead相加，然后查找具有2.25cpu和320 MiB可用内存的节点。

一旦一个Pod被调度到一个节点上，该节点上的kubelet将为该Pod创建一个新的[cgroup](https://kubernetes.io/docs/reference/glossary/?all=true#term-cgroup)。底层容器运行时将在这个pod中创建容器

如果资源为每个容器定义了一个限制（Guaranteed QoS or Bustrable QoS with limits defined），kubelet将为与该资源关联的pod cgroup设置一个上限（cpu.cfs_quota_us for CPU和memory.limit_in_bytes memory）。这个上限是基于容器限制加上PodSpec中定义的overhead的总和。

对于CPU，如果Pod是Guaranteed有保证的或Burstable稳定的QoS，kubelet将基于PodSpec中定义的容器overhead加上请求的总和来设置`cpu.shares`。

在我们的示例中，验证工作负载的容器请求：
```sh
kubectl get pod test-pod -o jsonpath='{.spec.containers[*].resources.limits}'
```

容器请求总数为2000m CPU和200MB内存：
```sh
map[cpu: 500m memory:100Mi] map[cpu:1500m memory:100Mi]
```

根据节点观察到的情况：
```sh
kubectl describe node | grep test-pod -B2
```

输出显示请求2250m CPU和320MiB内存，其中包括Pod开销：
```sh
  Namespace                   Name                CPU Requests  CPU Limits   Memory Requests  Memory Limits  AGE
  ---------                   ----                ------------  ----------   ---------------  -------------  ---
  default                     test-pod            2250m (56%)   2250m (56%)  320Mi (1%)       320Mi (1%)     36m
```

## Verify Pod cgroup limits

在运行工作负载的节点上检查Pod的内存cgroup。在下面的示例中，在节点上使用了[crictl](https://github.com/kubernetes-sigs/cri-tools/blob/master/docs/crictl.md)，它为CRI兼容的容器运行时提供了一个CLI。这是一个高级示例，用于显示PodOverload Behavior，并且用户不需要直接在节点上检查cgroups。

首先，在特定节点上，找到Pod标识符：
```sh
# Run this on the node where the Pod is scheduled
POD_ID="$(sudo crictl pods --name test-pod -q)"
```

这样，您可以确定Pod的cgroup路径：
```sh
# Run this on the node where the Pod is scheduled
sudo crictl inspectp -o=json $POD_ID | grep cgroupsPath
```

生成的cgroup路径里包括了Pod的pause容器。Pod级别的cgroup是上级的一个目录

```sh
   "cgroupsPath": "/kubepods/podd7f4b509-cf94-4951-9417-d1087c92a5b2/7ccf55aee35dd16aca4189c952d83487297f3cd760f1bbf09620e206e7d0c27a"
```

在本例中，pod cgroup路径为`kubepods/podd7f4b509-cf94-4951-9417-d1087c92a5b2`。验证内存的Pod级cgroup设置：
```sh
# Run this on the node where the Pod is scheduled.
# Also, change the name of the cgroup to match the cgroup allocated for your pod.
 cat /sys/fs/cgroup/memory/kubepods/podd7f4b509-cf94-4951-9417-d1087c92a5b2/memory.limit_in_bytes
```

以上输出是320 MiB, 正如预期:
```sh
335544320
```

### Observability

[kube-state-metrics](https://github.com/kubernetes/kube-state-metrics)中的kube_pod_overhead metric，可以帮助确定何时使用PodOverhead，并帮助观察定义了开销的工作负载的稳定性。这个功能在kube-state-metrics的1.9版本中不可用，预期在下一个版本中可用。同时，用户需要从源代码构建kube-state-metrics。


## What's next
* [RuntimeClass](https://kubernetes.io/docs/concepts/containers/runtime-class/)
* [PodOverhead Design](https://github.com/kubernetes/enhancements/blob/master/keps/sig-node/20190226-pod-overhead.md)
