<h2><span style="color: #ff0000;">注：display属性不能设置为none，否则没有过渡效果</span></h2>
<h2>实例</h2>
把鼠标指针放到 div 元素上，其宽度会从 100px 逐渐变为 300px：
<pre>div
{
width:100px;
transition: width 2s;
-moz-transition: width 2s; <span class="code_comment">/* Firefox 4 */</span>
-webkit-transition: width 2s; <span class="code_comment">/* Safari 和 Chrome */</span>
-o-transition: width 2s; <span class="code_comment">/* Opera */</span>
}</pre>
<div>
<h2>定义和用法</h2>
transition 属性是一个简写属性，用于设置四个过渡属性：
<ul>
 	<li>transition-property</li>
 	<li>transition-duration</li>
 	<li>transition-timing-function</li>
 	<li>transition-delay</li>
</ul>
<p class="note">注释：请始终设置 <a title="CSS3 transition-duration 属性" href="http://www.w3school.com.cn/cssref/pr_transition-duration.asp">transition-duration</a> 属性，否则时长为 0，就不会产生过渡效果。</p>

<table class="dataintable">
<tbody>
<tr>
<th>默认值：</th>
<td>all 0 ease 0</td>
</tr>
<tr>
<th>继承性：</th>
<td>no</td>
</tr>
<tr>
<th>版本：</th>
<td>CSS3</td>
</tr>
<tr>
<th>JavaScript 语法：</th>
<td><i>object</i>.style.transition="width 2s"</td>
</tr>
</tbody>
</table>
</div>
<div>
<h2>语法</h2>
<pre>transition: <i>property</i> <i>duration</i> <i>timing-function</i> <i>delay</i>;</pre>
<table class="dataintable">
<tbody>
<tr>
<th>值</th>
<th>描述</th>
</tr>
<tr>
<td><a title="CSS3 transition-property 属性" href="http://www.w3school.com.cn/cssref/pr_transition-property.asp">transition-property</a></td>
<td>规定设置过渡效果的 CSS 属性的名称。</td>
</tr>
<tr>
<td><a title="CSS3 transition-duration 属性" href="http://www.w3school.com.cn/cssref/pr_transition-duration.asp">transition-duration</a></td>
<td>规定完成过渡效果需要多少秒或毫秒。</td>
</tr>
<tr>
<td><a title="CSS3 transition-timing-function 属性" href="http://www.w3school.com.cn/cssref/pr_transition-timing-function.asp">transition-timing-function</a></td>
<td>规定速度效果的速度曲线。</td>
</tr>
<tr>
<td><a title="CSS3 transition-delay 属性" href="http://www.w3school.com.cn/cssref/pr_transition-delay.asp">transition-delay</a></td>
<td>定义过渡效果何时开始。</td>
</tr>
</tbody>
</table>
</div>