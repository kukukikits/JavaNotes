<pre class="copyright">转载自：作者<b><a href="https://kiwenlau.com/" target="_blank" title="KiwenLau" rel="noopener">KiwenLau</a></b>地址：<b><a href="https://kiwenlau.com/2016/07/03/vagrant-vm-cluster/" target="_blank" title="使用Vagrant创建多节点虚拟机集群" rel="noopener">https://kiwenlau.com/2016/07/03/vagrant-vm-cluster/</a></b></pre>
<h2 id="一-集群创建">一. 集群创建</h2>
<h4 id="1-安装VirtualBox"><a href="https://kiwenlau.com/2016/07/03/vagrant-vm-cluster/#1-安装VirtualBox" class="headerlink" title="1. 安装VirtualBox"></a><strong>1. 安装<a href="https://www.virtualbox.org/wiki/Downloads" target="_blank" rel="noopener">VirtualBox</a></strong></h4>
<h4 id="2-安装Vagrant"><a href="https://kiwenlau.com/2016/07/03/vagrant-vm-cluster/#2-安装Vagrant" class="headerlink" title="2. 安装Vagrant"></a><strong>2. 安装<a href="https://www.vagrantup.com/downloads.html" target="_blank" rel="noopener">Vagrant</a></strong></h4>
<h4 id="3-下载Box"><a href="https://kiwenlau.com/2016/07/03/vagrant-vm-cluster/#3-下载Box" class="headerlink" title="3. 下载Box"></a><strong>3. 下载Box</strong></h4>
<figure class="highlight armasm">
<table>
<tbody>
<tr>
<td class="code">
<pre><span class="line"><span class="symbol">vagrant</span> <span class="keyword">box </span><span class="keyword">add </span>ubuntu/trusty64</span></pre>
</td>
</tr>
</tbody>
</table>
</figure>
<strong>Box</strong>相当于虚拟机所依赖的镜像文件。
<h4 id="4-编辑Vagrantfile"><a href="https://kiwenlau.com/2016/07/03/vagrant-vm-cluster/#4-编辑Vagrantfile" class="headerlink" title="4. 编辑Vagrantfile"></a><strong>4. 编辑Vagrantfile</strong></h4>
<figure class="highlight stata">
<table>
<tbody>
<tr>
<td class="code">
<pre><span class="line"><span class="keyword">mkdir</span> vagrant-<span class="keyword">cluster</span></span>
<span class="line"><span class="keyword">cd</span> vagrant-<span class="keyword">cluster</span></span>
<span class="line">vim Vagrantfile</span></pre>
</td>
</tr>
</tbody>
</table>
</figure>
Vagrantfile如下，可以通过注释理解每个自定义配置的含义：
<figure class="highlight crmsh">
<table>
<tbody>
<tr>
<td class="code">
<pre><span class="line">Vagrant.configure(<span class="string">"2"</span>) do |config|</span>
<span class="line"></span>
<span class="line">	(<span class="number">1</span>..<span class="number">3</span>).each do |i|</span>
<span class="line"></span>
<span class="line">		config.vm.define <span class="string">"node#{i}"</span> do |<span class="keyword">node</span><span class="title">|</span></span>
<span class="line"><span class="title"></span></span>
<span class="line"><span class="title">		# 设置虚拟机的Box</span></span>
<span class="line">		<span class="keyword">node</span>.<span class="title">vm</span>.box = <span class="string">"ubuntu/trusty64"</span></span>
<span class="line"></span>
<span class="line">		<span class="comment"># 设置虚拟机的主机名</span></span>
<span class="line">		<span class="keyword">node</span>.<span class="title">vm</span>.<span class="attr">hostname=</span><span class="string">"node#{i}"</span></span>
<span class="line"></span>
<span class="line">		<span class="comment"># 设置虚拟机的IP</span></span>
<span class="line">		<span class="keyword">node</span>.<span class="title">vm</span>.network <span class="string">"private_network"</span>, ip: <span class="string">"192.168.59.#{i}"</span></span>
<span class="line"></span>
<span class="line">		<span class="comment"># 设置主机与虚拟机的共享目录</span></span>
<span class="line">		<span class="keyword">node</span>.<span class="title">vm</span>.synced_folder <span class="string">"~/Desktop/share"</span>, <span class="string">"/home/vagrant/share"</span></span>
<span class="line"></span>
<span class="line">		<span class="comment"># VirtaulBox相关配置</span></span>
<span class="line">		<span class="keyword">node</span>.<span class="title">vm</span>.provider <span class="string">"virtualbox"</span> do |v|</span>
<span class="line"></span>
<span class="line">			<span class="comment"># 设置虚拟机的名称</span></span>
<span class="line">			v.name = <span class="string">"node#{i}"</span></span>
<span class="line"></span>
<span class="line">			<span class="comment"># 设置虚拟机的内存大小  </span></span>
<span class="line">			v.memory = <span class="number">2048</span></span>
<span class="line"></span>
<span class="line">			<span class="comment"># 设置虚拟机的CPU个数</span></span>
<span class="line">			v.cpus = <span class="number">1</span></span>
<span class="line">		end</span>
<span class="line">  </span>
<span class="line">		<span class="comment"># 使用shell脚本进行软件安装和配置</span></span>
<span class="line">		<span class="keyword">node</span>.<span class="title">vm</span>.provision <span class="string">"shell"</span>, inline: <span class="tag">&lt;&lt;-SHELL</span></span>
<span class="line"><span class="tag"></span></span>
<span class="line"><span class="tag">			# 安装docker 1.11.0</span></span>
<span class="line"><span class="tag">			wget -qO- https://get.docker.com/ | sed 's/docker-engine/docker-engine=1.11.0-0~trusty/' | sh</span></span>
<span class="line"><span class="tag">			usermod -aG docker vagrant</span></span>
<span class="line"><span class="tag">			</span></span>
<span class="line"><span class="tag">		SHELL</span></span>
<span class="line"><span class="tag"></span></span>
<span class="line"><span class="tag">		end</span></span>
<span class="line"><span class="tag">	end</span></span>
<span class="line"><span class="tag">end</span></span></pre>
</td>
</tr>
</tbody>
</table>
</figure>
与创建单个虚拟机相比，创建多个虚拟机时多了一层循环，而变量i可以用于设置节点的名称与IP，使用<strong>#{i}</strong>取值：
<figure class="highlight coq">
<table>
<tbody>
<tr>
<td class="code">
<pre><span class="line">(<span class="number">1.</span><span class="number">.3</span>).each <span class="built_in">do</span> |<span class="type">i</span>|<span class="type"></span></span>
<span class="line"><span class="type"></span></span>
<span class="line"><span class="type">end</span></span></pre>
</td>
</tr>
</tbody>
</table>
</figure>
可知，一共创建了3个虚拟机。
<h4 id="5-在桌面上创建share目录"><a href="https://kiwenlau.com/2016/07/03/vagrant-vm-cluster/#5-在桌面上创建share目录" class="headerlink" title="5. 在桌面上创建share目录"></a><strong>5. 在桌面上创建share目录</strong></h4>
桌面上的share目录将与虚拟机内的/home/vagrant/share目录内容实时同步
<figure class="highlight arduino">
<table>
<tbody>
<tr>
<td class="code">
<pre><span class="line"><span class="built_in">mkdir</span> ~/Desktop/share</span></pre>
</td>
</tr>
</tbody>
</table>
</figure>
<h4 id="6-创建虚拟机"><a href="https://kiwenlau.com/2016/07/03/vagrant-vm-cluster/#6-创建虚拟机" class="headerlink" title="6. 创建虚拟机"></a><strong>6. 创建虚拟机</strong></h4>
<figure class="highlight ebnf">
<table>
<tbody>
<tr>
<td class="code">
<pre><span class="line"><span class="attribute">vagrant up</span></span></pre>
</td>
</tr>
</tbody>
</table>
</figure>
创建3个虚拟机大概需要15分钟，当然这和机器性能还有网速相关。安装Docker可能会比较慢，不需要的话删除下面几行就可以了：
<figure class="highlight dockerfile">
<table>
<tbody>
<tr>
<td class="code">
<pre><span class="line"><span class="comment"># 使用shell脚本进行软件安装和配置</span></span>
<span class="line">node.vm.provision <span class="string">"shell"</span>, inline: &lt;&lt;-<span class="keyword">SHELL</span><span class="bash"></span></span>
<span class="line"><span class="bash"></span></span>
<span class="line"><span class="bash">	<span class="comment"># 安装docker 1.11.0</span></span></span>
<span class="line"><span class="bash">	wget -qO- https://get.docker.com/ | sed <span class="string">'s/docker-engine/docker-engine=1.11.0-0~trusty/'</span> | sh</span></span>
<span class="line"><span class="bash">	usermod -aG docker vagrant</span></span>
<span class="line"><span class="bash"></span></span>
<span class="line"><span class="bash">SHELL</span></span></pre>
</td>
</tr>
</tbody>
</table>
</figure>
下面是Vagrant虚拟机的配置，可以根据需要进行更改:
<ul>
 	<li>用户/密码: vagrant/vagrant</li>
 	<li>共享目录: 桌面上的share目录将与虚拟机内的/home/vagrant/share目录内容实时同步</li>
 	<li>内存：2GB</li>
 	<li>CPU: 1</li>
</ul>
<h2 id="二-集群管理"><a href="https://kiwenlau.com/2016/07/03/vagrant-vm-cluster/#二-集群管理" class="headerlink" title="二. 集群管理"></a>二. 集群管理</h2>
<h4 id="1-常用命令"><a href="https://kiwenlau.com/2016/07/03/vagrant-vm-cluster/#1-常用命令" class="headerlink" title="1. 常用命令"></a><strong>1. 常用命令</strong></h4>
下面是一些常用的Vagrant管理命令，操作特定虚拟机时仅需指定虚拟机的名称。
<ul>
 	<li><strong>vagrant ssh:</strong> SSH登陆虚拟机</li>
 	<li><strong>vagrant halt:</strong> 关闭虚拟机</li>
 	<li><strong>vagrant destroy:</strong> 删除虚拟机</li>
 	<li><strong>vagrant ssh-config</strong> 查看虚拟机SSH配置</li>
</ul>
<strong>启动单个虚拟机：</strong>
<figure class="highlight nginx">
<table>
<tbody>
<tr>
<td class="code">
<pre><span class="line"><span class="attribute">vagrant</span> up node1</span></pre>
</td>
</tr>
</tbody>
</table>
</figure>
<strong>启动多个虚拟机：</strong>
<figure class="highlight nginx">
<table>
<tbody>
<tr>
<td class="code">
<pre><span class="line"><span class="attribute">vagrant</span> up node1 node3</span></pre>
</td>
</tr>
</tbody>
</table>
</figure>
<strong>启动所有虚拟机：</strong>
<figure class="highlight ebnf">
<table>
<tbody>
<tr>
<td class="code">
<pre><span class="line"><span class="attribute">vagrant up</span></span></pre>
</td>
</tr>
</tbody>
</table>
</figure>
<h4 id="2-SSH免密码登陆"><a href="https://kiwenlau.com/2016/07/03/vagrant-vm-cluster/#2-SSH免密码登陆" class="headerlink" title="2. SSH免密码登陆"></a><strong>2. SSH免密码登陆</strong></h4>
使用<strong>vagrant ssh</strong>命令登陆虚拟机必须切换到Vagrantfile所在的目录，而直接使用虚拟机IP登陆虚拟机则更为方便:
<figure class="highlight css">
<table>
<tbody>
<tr>
<td class="code">
<pre><span class="line"><span class="selector-tag">ssh</span> <span class="selector-tag">vagrant</span>@<span class="keyword">192</span>.<span class="keyword">168</span>.<span class="keyword">59</span>.<span class="keyword">2</span></span></pre>
</td>
</tr>
</tbody>
</table>
</figure>
也可以切换到vagrantfile所在的目录使用如下命令登录
<pre class="prettyprint">vagrant ssh node1       #node1为创建的虚拟机名， 可以使用vagrant global-status查看所有虚拟机状态</pre>
此时SSH登陆需要输入虚拟机vagrant用户的密码，即<strong>vagrant</strong>

将主机的公钥复制到虚拟机的authorized_keys文件中即可实现SSH免密码登陆:
<figure class="highlight nginx">
<table>
<tbody>
<tr>
<td class="code">
<pre><span class="line"><span class="attribute">cat</span> <span class="variable">$HOME</span>/.ssh/id_rsa.pub | ssh vagrant@<span class="number">192.168.59.2</span> <span class="string">'cat &gt;&gt; <span class="variable">$HOME</span>/.ssh/authorized_keys'</span></span></pre>
</td>
</tr>
</tbody>
</table>
</figure>
<h4 id="3-重新安装软件"><a href="https://kiwenlau.com/2016/07/03/vagrant-vm-cluster/#3-重新安装软件" class="headerlink" title="3. 重新安装软件"></a><strong>3. 重新安装软件</strong></h4>
Vagrant中有下面一段内容：
<figure class="highlight dockerfile">
<table>
<tbody>
<tr>
<td class="code">
<pre><span class="line"><span class="comment"># 使用shell脚本进行软件安装和配置</span></span>
<span class="line">node.vm.provision <span class="string">"shell"</span>, inline: &lt;&lt;-<span class="keyword">SHELL</span><span class="bash"></span></span>
<span class="line"><span class="bash"></span></span>
<span class="line"><span class="bash">	<span class="comment"># 安装docker 1.11.0</span></span></span>
<span class="line"><span class="bash">	wget -qO- https://get.docker.com/ | sed <span class="string">'s/docker-engine/docker-engine=1.11.0-0~trusty/'</span> | sh</span></span>
<span class="line"><span class="bash">	usermod -aG docker vagrant</span></span>
<span class="line"><span class="bash">SHELL</span></span></pre>
</td>
</tr>
</tbody>
</table>
</figure>
其实就是嵌入了一段Shell脚本进行软件的安装和配置，这里我安装了<a href="https://www.docker.com/" target="_blank" rel="noopener">Docker</a>，当然也可以安装其他所需要的软件。修改此段内容之后，重新创建虚拟机需要使用”–provision”选项。
<figure class="highlight ada">
<table>
<tbody>
<tr>
<td class="code">
<pre><span class="line">vagrant halt</span>
<span class="line">vagrant up <span class="comment">--provision</span></span></pre>
</td>
</tr>
</tbody>
</table>
</figure>
<h4 id="4-共享目录挂载出错"><a href="https://kiwenlau.com/2016/07/03/vagrant-vm-cluster/#4-共享目录挂载出错" class="headerlink" title="4. 共享目录挂载出错"></a><strong>4. 共享目录挂载出错</strong></h4>
VirtualBox设置共享目录时需要在虚拟机中安装VirtualBox Guest Additions，这个Vagrant会自动安装。但是，VirtualBox Guest Additions是内核模块，当虚拟机的内核升级之后，VirtualBox Guest Additions会失效，导致共享目录挂载失败，出错信息如下:
<figure class="highlight routeros">
<table>
<tbody>
<tr>
<td class="code">
<pre><span class="line">Failed <span class="keyword">to</span> mount folders <span class="keyword">in</span> Linux guest. This is usually because</span>
<span class="line">the <span class="string">"vboxsf"</span> file<span class="built_in"> system </span>is <span class="keyword">not</span> available. Please verify that</span>
<span class="line">the guest additions are properly installed <span class="keyword">in</span> the guest <span class="keyword">and</span></span>
<span class="line">can work properly. The command attempted was:</span>
<span class="line"></span>
<span class="line">mount -t vboxsf -o <span class="attribute">uid</span>=`id -u vagrant`,<span class="attribute">gid</span>=`getent<span class="built_in"> group </span>vagrant | cut -d: -f3` vagrant /vagrant</span>
<span class="line">mount -t vboxsf -o <span class="attribute">uid</span>=`id -u vagrant`,<span class="attribute">gid</span>=`id -g vagrant` vagrant /vagrant</span>
<span class="line"></span>
<span class="line">The <span class="builtin-name">error</span> output <span class="keyword">from</span> the last command was:</span>
<span class="line"></span>
<span class="line">stdin: is <span class="keyword">not</span> a tty</span>
<span class="line">/sbin/mount.vboxsf: mounting failed with the error: <span class="literal">No</span> such device</span></pre>
</td>
</tr>
</tbody>
</table>
</figure>
安装Vagrant插件<a href="https://github.com/dotless-de/vagrant-vbguest" target="_blank" rel="noopener">vagrant-vbguest</a>可以解决这个问题，因为该插件会在虚拟机内核升级之后重新安装VirtualBox Guest Additions。
<figure class="highlight cmake">
<table>
<tbody>
<tr>
<td class="code">
<pre><span class="line">vagrant plugin <span class="keyword">install</span> vagrant-vbguest</span></pre>
</td>
</tr>
</tbody>
</table>
</figure>