状态: 连接建立，等待欢迎消息...
状态: 不安全的服务器，不支持 FTP over TLS。
状态: 已登录
状态: 读取目录列表...
命令: PWD
响应: 257 "/"
命令: TYPE I
响应: 200 Switching to Binary mode.
命令: PASV
响应: 227 Entering Passive Mode (0,0,0,0,66,136).
命令: LIST
<span style="color: #ff0000;">错误: 无法建立数据连接: WSAEADDRNOTAVAIL - 无法分配请求的地址</span>
<span style="color: #ff0000;">错误: 20 秒后无活动，连接超时</span>
<span style="color: #ff0000;">错误: 读取目录列表失败</span>

<!--more-->

在搭建vsftpd服务器的时候遇上面的问题，FileZilla客户端可以登录，也可以获取目录列表，但是无法建立数据连接。

解决方案：把FileZilla设置为主动模式。使用windows资源管理器连接时，修改Internet选项。把Internet选项&gt;高级&gt;设置中，“使用被动FTP（用于防火墙和DSL调制解调器的兼容）”这一选项取消勾选。问题解决。

但是最后还是不能解决为什么被动模式下不能建立数据连接的问题。。。最后发现FTP服务器和客户端之间还有一层服务器，具体情况为登录成功，但是list目录和文件的时候卡住。这时候我们用lsof -i:21
<pre>vsftpd   22411   nobody    0u  IPv4  68905      0t0  TCP 10.140.41.65:ftp-&gt;10.10.10.98:43380 (ESTABLISHED)
vsftpd   22411   nobody    1u  IPv4  68905      0t0  TCP 10.140.41.65:ftp-&gt;10.10.10.98:43380 (ESTABLISHED)</pre>
这时候可以看到机器的真正IP。

我们需要设置
<div class="cnblogs_code">
<pre>pasv_address=本机ip【就是我们能访问的外网IP】
pasv_addr_resolve=yes</pre>
</div>
这样ftp客户端就可以解析IP，访问成功

&nbsp;