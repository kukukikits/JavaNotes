<h2>1.配置ACL</h2>
在consul的配置目录下新建acl.json配置文件（每台consul server上都要配置），输入如下配置信息：
```json
{
  "acl_datacenter": "vagrant-data-center",
  "acl_default_policy": "allow",
  "acl_down_policy": "extend-cache"
}
```
这里需要注意的是acl_datacenter属性，它的值必须是一个有效的datacenter的值，也就是说不能随便乱起名字，必须选择一个已经存在的datacenter的值，否则acl系统在启动时找不到datacenter就会一直报错。

重启consul server，最好每次只重启一台。

接下来需要配置<code><a href="https://www.consul.io/docs/agent/options.html#acl_master_token">acl_master_token</a></code>，这是一个“management”类型的令牌，也就是说使用这个令牌可以进行任何操作。

ACL Token由ID（随机的UUID）、name（名字）、type（类型）和rule set（规则集）组成。其中type有两类：“client”和“management”。“client”类型的令牌是不能用来修改ACL规则。“management”类型的令牌则可以进行任何操作。

在使用HTTP远程请求被保护的服务时，必须使用X-Consul-Token请求头包含令牌的ID值，否则请求失败。

在上面的配置我们没有在配置文件中显示地配置acl_master_token，这里我们使用另一种方式进行配置：

consul server启动后在另一个命令行里使用API初始化acl_master_token
```bash
$ curl \
    --request PUT \
    http://127.0.0.1:8500/v1/acl/bootstrap
```
返回值：{"ID":"df58d467-b067-5a51-f113-101cebab053d"}
请求v1/acl/bootstrap接口后会返回一个ID值，这就是初始化的acl_master_token。

在同一个datacenter集群中只需要在其中的一台consul server上初始化acl_master_token就可以了。
<h2>2. 创建客户端令牌Agent Token</h2>
以上配置完成后在consul server的终端会有permission denied的错误
```bash
2017/07/08 23:38:24 [WARN] agent: Node info update blocked by ACLs
2017/07/08 23:38:44 [WARN] agent: Coordinate update blocked by ACLs
```
这是因为客户端还没有配置acl_agent_token。接下来我们来使用API创建一个令牌：
```bash
$ curl \
    --request PUT \
    --header "X-Consul-Token: df58d467-b067-5a51-f113-101cebab053d" \
    --data \
'{
  "Name": "Agent Token",
  "Type": "client",
  "Rules": "node \"\" { policy = \"write\" } service \"\" { policy = \"read\" }"
}' http://127.0.0.1:8500/v1/acl/create

返回值：{"ID":"fe3b8d40-0ee0-8783-6cc2-ab1aa9bb16c1"}
```
好了，创建令牌的这个动作只需要进行一次就行。这里创建的令牌是client类型的，它的规则限制都在Rules里进行了申明。下面来解读一下规则

其中node指该令牌与节点node相关，权限由policy属性指定，只有write权限。service指定了令牌与服务相关的权限，可知该令牌只有service的read权限。那node和service后面的“”是什么意思呢？node ""表示给所有的节点分配权限，service ""表示给所有的服务分配权限。举个例子service "consul"表示给所有consul服务分配权限。

好了，创建好令牌后，我们要把这个令牌分配给每一台consul server，这里使用API进行分配（把请求的地址替换为你的consul server的ip，或者在每一台consul server上都执行下面的命令）：
```bash
$ curl \
    --request PUT \
    --header "X-Consul-Token: b1gs33cr3t" \
    --data \
'{
  "Token": "fe3b8d40-0ee0-8783-6cc2-ab1aa9bb16c1"
}' http://127.0.0.1:8500/v1/agent/token/acl_agent_token
```
第二种分配方式是，把这个令牌直接写到配置里，如下：
```json
{
  "acl_datacenter": "dc1",
  "acl_default_policy": "deny",
  "acl_down_policy": "extend-cache",
  "acl_agent_token": "fe3b8d40-0ee0-8783-6cc2-ab1aa9bb16c1"
}
```
分配完成后，在consul server的终端可以看到如下的日志：
```text
2017/07/08 23:42:59 [INFO] agent: Synced node info
```