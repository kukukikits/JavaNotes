<h1 class="postTitle"><span style="font-size: 53.2px; line-height: 1.15em;">允许</span>mysql数据库被远程连接</h1>
<div class="clear"></div>
<div class="postBody">
<div id="cnblogs_post_body" class="blogpost-body">
<pre class="best-text mb-10">解决办法：修改mysql的访问策略，更改 “mysql” 数据库里的 “user” 表里的 “host” 项，从”localhost”改称'%'。 
或者新加条记录，“host” 项为要访问的ip地址，并授权。重启mysql服务。 

方法1。登录mysql服务器，登入mysql，更改"user" 表里的 "host" 项
使用命令： mysql -u root -p
登入后：
mysql&gt;use mysql;
mysql&gt;select 'host' from user where user='root'; #查看mysql库中的user表的host值（即可进行连接访问的主机/IP名称）
mysql&gt;update user set host = '%' where user = 'root';   #修改host值（以通配符%的内容增加主机/IP地址），当然也可以直接增加IP地址 
mysql&gt;select host, user from user;

方法2. 使用GRANT命令直接授权。 
#使用root用户使用mypassword，从任何主机连接到mysql服务器
GRANT ALL PRIVILEGES ON *.* TO 'myuser'@'%' IDENTIFIED BY 'mypassword' WITH GRANT OPTION; 
#允许root用户从ip为192.168.X.X的主机连接到mysql服务器，并使用mypassword作为密码 
GRANT ALL PRIVILEGES ON *.* TO 'myuser'@'192.168.X.X' IDENTIFIED BY 'mypassword' WITH GRANT OPTION; 

最后：  
mysql&gt;flush privileges;                                 #刷新MySQL的系统权限相关表

</pre>
</div>
</div>