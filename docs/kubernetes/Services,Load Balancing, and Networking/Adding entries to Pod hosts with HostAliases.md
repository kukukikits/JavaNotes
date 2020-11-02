# Adding entries to Pod /etc/hosts with HostAliases

当DNS和其他选项不适用时，向Pod的/etc/hosts文件添加条目将提供Pod级别的主机名解析覆盖。您可以使用PodSpec中的HostAliases字段添加这些自定义条目。

不建议使用其他方式（非HostAliases）进行修改，因为/etc/hosts文件由kubelet管理，并且可以在Pod创建/重新启动期间被覆盖

## Default hosts file content

启动一个分配了Pod IP的Nginx Pod：
```shell
kubectl run nginx --image nginx
kubectl get pods --output=wide

#output
NAME     READY     STATUS    RESTARTS   AGE    IP           NODE
nginx    1/1       Running   0          13s    10.200.0.4   worker0
```

hosts文件内容如下所示：
```yaml
kubectl exec nginx -- cat /etc/hosts

# Kubernetes-managed hosts file.
127.0.0.1	localhost
::1	localhost ip6-localhost ip6-loopback
fe00::0	ip6-localnet
fe00::0	ip6-mcastprefix
fe00::1	ip6-allnodes
fe00::2	ip6-allrouters
10.200.0.4	nginx
```

默认情况下，hosts文件只包含IPv4和IPv6模板，比如localhost和它自己的主机名

## Adding additional entries with hostAliases 

除了默认的样板文件外，还可以向hosts文件添加其他条目。例如：将`foo.local, bar.local`解析为`127.0.0.1`，并且将`foo.remote，bar.remote` 解析到 `10.1.2.3`，您可以在pod的`.spec.hostAliases`字段上配置HostAliases ：
service/networking/hostaliases-pod.yaml 
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: hostaliases-pod
spec:
  restartPolicy: Never
  hostAliases:
  - ip: "127.0.0.1"
    hostnames:
    - "foo.local"
    - "bar.local"
  - ip: "10.1.2.3"
    hostnames:
    - "foo.remote"
    - "bar.remote"
  containers:
  - name: cat-hosts
    image: busybox
    command:
    - cat
    args:
    - "/etc/hosts"
```

You can start a Pod with that configuration by running:

```shell
kubectl apply -f https://k8s.io/examples/service/networking/hostaliases-pod.yaml
```

Examine a Pod's details to see its IPv4 address and its status:

```shell
kubectl get pod --output=wide

NAME                           READY     STATUS      RESTARTS   AGE       IP              NODE
hostaliases-pod                0/1       Completed   0          6s        10.200.0.5      worker0
```

The `hosts` file content looks like this:

```shell
kubectl logs hostaliases-pod

# Kubernetes-managed hosts file.
127.0.0.1	localhost
::1	localhost ip6-localhost ip6-loopback
fe00::0	ip6-localnet
fe00::0	ip6-mcastprefix
fe00::1	ip6-allnodes
fe00::2	ip6-allrouters
10.200.0.5	hostaliases-pod

# Entries added by HostAliases.
127.0.0.1	foo.local	bar.local
10.1.2.3	foo.remote	bar.remote
```

with the additional entries specified at the bottom.


## Why does the kubelet manage the hosts file?

kubelet管理Pod的每个容器的hosts文件，以防止Docker在容器已经启动后修改文件。

> :warning: 注意事项：
> 避免对容器中的hosts文件进行手动更改。
> 如果手动更改hosts文件，则当容器退出时，这些更改将丢失。

