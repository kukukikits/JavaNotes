

# 本文描述所有Controllers

---

# Garbage Collection
The role of the Kubernetes garbage collector is to delete certain objects that once had an owner, but no longer have an owner

## Owners and dependents

一些Kubernetes对象是其他对象的所有者。例如，ReplicaSet是一组pod的所有者。被拥有的对象称为所有者对象的从属对象dependents。每个从属对象都`metadata.ownerReferences`字段指向所有者对象。

有时，Kubernetes会自动设置ownerReference的值。例如，创建ReplicaSet时，Kubernetes会自动设置复制集中每个Pod的ownerReference字段。在1.8中，Kubernetes自动为ReplicationController、replicset、StatefulSet、DaemonSet、Deployment、Job和CronJob创建或收养adopted的对象设置ownerReference的值。

也可以通过手动设置ownerReference字段来指定所有者和从属对象之间的关系。

下面是一个包含三个pod的复制集的配置文件：
controllers/replicaset.yaml 
```yaml
apiVersion: apps/v1
kind: ReplicaSet
metadata:
  name: my-repset
spec:
  replicas: 3
  selector:
    matchLabels:
      pod-is-for: garbage-collection-example
  template:
    metadata:
      labels:
        pod-is-for: garbage-collection-example
    spec:
      containers:
      - name: nginx
        image: nginx
```

If you create the ReplicaSet and then view the Pod metadata, you can see OwnerReferences field:

```shell
kubectl apply -f https://k8s.io/examples/controllers/replicaset.yaml
kubectl get pods --output=yaml
```

The output shows that the Pod owner is a ReplicaSet named my-repset:

```yaml
apiVersion: v1
kind: Pod
metadata:
  ...
  ownerReferences:
  - apiVersion: apps/v1
    controller: true
    blockOwnerDeletion: true
    kind: ReplicaSet
    name: my-repset
    uid: d9607e19-f88f-11e6-a518-42010a800195
  ...
```

> 注：
> 设计不允许跨命名空间所有者引用。这意味着：
> 1. 命名空间范围的从属对象只能指定同一命名空间中的所有者和群集范围的所有者。
> 2. 群集范围的从属对象只能指定群集范围的所有者，但不能指定命名空间范围的所有者。

## Controlling how the garbage collector deletes dependents 

删除对象时，可以指定是否也自动删除该对象的从属对象。自动删除从属项称为级联删除cascading deletion。级联删除有两种模式：background and foreground。

如果删除对象而不自动删除其从属对象，则该从属对象被称为孤立对象orphaned。

### Foreground cascading deletion 

在前台级联删除中，根对象首先进入“正在删除”状态。在“正在删除”状态下，会有以下几个状态：
- 对象仍然可以通过 REST API 看到
- 对象的deletionTimestamp已设置
- 对象的metadata.finalizers包含值“foregroundDeletion”。

一旦设置了“正在删除”状态，垃圾回收器将删除对象的从属对象。一旦垃圾回收器删除了所有“阻塞”从属对象（具有ownerReference.blockOwnerDeletion=true的对象)，它将删除所有者对象。

注意，在“foregroundelection”中，只有ownerReference.blockOwnerDeletion=true阻止删除所有者对象。Kubernetes 1.7版添加了一个[许可控制器admission controller](https://kubernetes.io/docs/reference/access-authn-authz/admission-controllers/#ownerreferencespermissionenforcement)，该控制器控制用户访问，根据所有者对象上的删除权限将blockownerdelection设置为true，以便未经授权的从属对象不能延迟对所有者对象的删除。

如果对象的ownerReferences字段由控制器（如Deployment或ReplicaSet）设置，则blockOwnerDeletion将自动设置，您不需要手动修改此字段。


### Background cascading deletion 
在后台级联删除中，Kubernetes立即删除所有者对象，然后垃圾回收器在后台删除从属对象

### Setting the cascading deletion policy
要控制级联删除策略，请在删除对象时设置deleteOptions参数的propagationPolicy字段。可能的值包括“Orphan”、“Foreground”或“Background”。

Here's an example that deletes dependents in background:
```shell
kubectl proxy --port=8080
curl -X DELETE localhost:8080/apis/apps/v1/namespaces/default/replicasets/my-repset \
  -d '{"kind":"DeleteOptions","apiVersion":"v1","propagationPolicy":"Background"}' \
  -H "Content-Type: application/json"
```

Here's an example that deletes dependents in foreground:
```shell
kubectl proxy --port=8080
curl -X DELETE localhost:8080/apis/apps/v1/namespaces/default/replicasets/my-repset \
  -d '{"kind":"DeleteOptions","apiVersion":"v1","propagationPolicy":"Foreground"}' \
  -H "Content-Type: application/json"
```

Here's an example that orphans dependents:
```shell
kubectl proxy --port=8080
curl -X DELETE localhost:8080/apis/apps/v1/namespaces/default/replicasets/my-repset \
  -d '{"kind":"DeleteOptions","apiVersion":"v1","propagationPolicy":"Orphan"}' \
  -H "Content-Type: application/json"
```

kubectl还支持级联删除。要使用kubectl自动删除依赖项，请将--cascade设置为true。对于孤立orphan从属项，将--cascade设置为false。--cascade的默认值为true。
Here's an example that orphans the dependents of a ReplicaSet:
```shell
kubectl delete replicaset my-repset --cascade=false
```

### Additional note on Deployments

在1.7之前，在Deployments中使用级联删除时，必须使用`propagationPolicy:Foreground`不仅删除创建的复制集，还删除它们的pod。如果不使用这种类型的传播策略，则只删除ReplicaSets，pod就会变成孤立的orphaned。有关详细信息，请参阅[kubeadm/#149](https://github.com/kubernetes/kubeadm/issues/149#issuecomment-284766613)。
