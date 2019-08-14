<h3>1.安装vagrant</h3>
既然是集群，当然需要多台计算机或者虚拟机的支持，这里我使用了vagrant建立集群。多台vagrant虚拟机的搭建详见<a href="http://47.93.1.79/wordpress/?p=386" target="_blank" rel="noopener">《vagrant集群创建》</a>

假设这里已经有三台虚拟机，分别是<!--?prettify linenums=true?-->
<pre class="prettyprint">node1: 192.168.56.1
node3: 192.168.56.2
node3: 192.168.59.3</pre>
<h3>2.安装Consul</h3>
接下来在三台机器上都安装Consul，下载地址为<a href="https://www.consul.io/downloads.html" target="_blank" rel="noopener">https://www.consul.io/downloads.html</a>

下载好consul软件后解压，复制文件到虚拟机的某个目录，这里把它复制到/home/consul目录下（注意/home/consul文件夹下就是consul可执行文件）。

然后设置虚拟机的环境变量，这里通过修改/etc/profile文件的方式进行修改：
<pre class="prettyprint">[vagrant@node1 ~]$ sudo vim /etc/profile

#...在最后一行加入下面的代码，然后保存退出
export PATH=$PATH:/home/consul

</pre>
修改完成后保存退出，输入下面代码使修改生效
<pre class="prettyprint">[vagrant@node1 ~]$ source /etc/profile</pre>
最后使用consul命令检查安装是否成功，如
<pre class="prettyprint">[vagrant@node1 ~]$ consul version
Consul v1.2.1
Protocol 2 spoken by default, understands 2 to 3 (agent will automatically use protocol &gt;2 when speaking to compatible agents)</pre>
<h3>3. 配置consul</h3>
<h3>3.1 配置consul server</h3>
接下来搭建consul server集群。在三台虚拟机上，创建配置文件，步骤如下：
<pre class="prettyprint">[vagrant@node1 ~]$ mkdir /home/consulData
[vagrant@node1 ~]$ sudo chmod -R 777 /home/consulData
[vagrant@node1 ~]$ cd /home/consulData
[vagrant@node1 ~]$ mkdir consul.d
[vagrant@node1 ~]$ sudo vim config.json

#在config.json文件中输入如下配置：
 {
  "datacenter": "vagrant-data-center",
  "data_dir": "/home/consulData/data",
  "log_level": "INFO",
  "node_name": "node1",      #节点名，可以任意，但三个虚拟机的配置不能相同
  "server": true,
  "bind_addr": "192.168.59.1",     #修改为node1的ip地址
  "bootstrap_expect": 3,        #一个数据中心datacenter，期望建立集群时server的数量，这个配置项必须一致
  "client_addr": "0.0.0.0",       #绑定客户端ip，默认情况下只能在本机使用127.0.0.1才能访问8500端口的服务
  "enable_script_checks": true,
  "ui": true                            #开启UI界面，默认端口为8500
}</pre>
以上为一个简单的配置，在真正应用到生产环境还要加入加密、权限控制等配置项。

三台虚拟机都配置好consul.d/config.json文件后，使用下面的命令启动consul server
<pre class="prettyprint">[vagrant@node1 ~]$ consul agent -config-dir=/home/consulData/consul.d
</pre>
当三台consul server都启动完成后，它们之间互不知晓对方的存在，需要引导它们加入一个集群。在任意一台虚拟机上输入下面的命令
<pre class="prettyprint">[vagrant@node1 ~]$ consul join 192.168.59.1 192.168.59.2 192.168.59.3
</pre>
最后检查一下集群成员，如下
<pre class="prettyprint">[vagrant@node1 ~]$ consul members
Node   Address            Status  Type    Build  Protocol  DC                   Segment
node1  192.168.59.1:8301  alive   server  1.2.1  2         vagrant-data-center  &lt;all&gt;
node2  192.168.59.2:8301  alive   server  1.2.1  2         vagrant-data-center  &lt;all&gt;
node3  192.168.59.3:8301  alive   server  1.2.1  2         vagrant-data-center  &lt;all&gt;</pre>
<span style="color: #800000;">注：要加入一个集群，只需要知道集群中的某一个成员的地址就可以。成员间通过gossip协议更新成员关系。</span>