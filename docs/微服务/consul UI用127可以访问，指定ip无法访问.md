<span>./consul agent -dev </span><span> </span><span><strong>-client 0.0.0.0 -ui</strong></span>

-client参数默认绑定了127.0.0.1，所以需要修改为0.0.0.0才能使用其他ip访问consul UI