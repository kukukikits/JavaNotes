CAS是一个企业级的用于权限认证的单点登录系统。它是开源的，基于Java实现的。GitHub地址：<a href="https://github.com/apereo/cas">https://github.com/apereo/cas</a>

首先来理解一下他的架构
<h2>一、架构</h2>
[caption id="attachment_349" align="alignnone" width="585"]<img src="http://47.93.1.79/wordpress/wp-content/uploads/2018/03/cas_architecture.png" alt="CAS系统架构" width="585" height="728" class="size-full wp-image-349" /> CAS系统架构[/caption]

从上图可以发现，CAS主要由两个物理组件组成：CAS Server和CAS Clients

CAS Server是一个基于Spring构建的Java servlet，主要用于用户认证和授权登录。当用户成功登录时，服务器会生成票据TGT给用户，并在服务器上保存一个SSO session。TGT是ticket-granting tickets的缩写。当浏览器使用TGT作为令牌重定向时，服务器会给用户请求的服务分发一个服务票据（Service ticket, ST）。

CAS Clients是一个软件包，能够集成在各类软件上，使之能够使用CAS Server进行权限认证，登录系统等。

CAS从功能上由三个子系统组成：Web(Spring MVC/Spring Webflow)、Ticketing和Authentication。
<h2>二、CAS协议</h2>
首先有两个概念要明确：

TGT：Ticket Granting Ticket，保存在TGC cookie中，代表了一个SSO session。

ST：Service Ticket，携带在url中，作为GET请求的一个参数进行传递，代表了一个被授权的登录。
