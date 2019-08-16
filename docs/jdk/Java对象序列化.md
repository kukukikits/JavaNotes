Java对象序列化的意义在于，可以将Java对象转换成字节数据，保存在磁盘上，或者直接通过网络传播。同时通过反序列化可以把保存在磁盘，或者网络上的java字节数据恢复成原来的对象。所以序列化使Java对象可以脱离程序而独立存在。

Java类实现可序列化的要求是，实现下面的两个接口之一：
<ul>
 	<li>Serializable</li>
 	<li>Externalizable</li>
</ul>
<h3>一、使用对象流实现序列化</h3>

两个步骤实现序列化：
1. 创建ObjectOutputStream处理流
```java
//处理流必须建立在节点流FileOutputStream上
ObjectOutputStream oos = new ObjectOutputStream(
      new FileOutputStream("Object.txt")
);
```
2. 调用writeObject()方法输出可序列化对象：
```java
oos.writeObject(per1);
oos.writeObject(per2);
```
两个步骤实现反序列化：
1. 创建一个ObjectInputStream输入流
```java
//处理流必须建立在节点流上
ObjectInputStream ois = new ObjectInputStream(
    new FileInputStream("Object.txt")
);
```
2. 调用ObjectInputStream对象的readObject()方法读取流中的对象
```java
//读取顺序与序列化时的顺序一致
Person p1 = (Person) ois.readObject();
Person p2 = (Person) ois.readObject();
```
<span style="color: #ff0000;"><strong>Tips:</strong></span>
<ul>
 	<li>反序列化时必须提供Java对象所属类的class文件，否则没办法转换类</li>
 	<li>反序列化机制无需通过构造器来初始化Java对象</li>
 	<li>父类也必须使可序列化的，不然子类不能正常序列化</li>
 	<li>Java序列化机制只有在第一次调用writeobject()时，才会将对象转换成字节序列，并写入到ObjectOutputStream。在后面程序中即使该对象的实例变量发生了变化，再次调用writeObject()方法输出该对象时，改变后的实例变量也不会被输出。

```java
Person per = new Person("孙悟空", 500);
oos.writeObject(per);

per.setName("猪八戒");
//系统只输出序列化编号，改变后的name不会被序列化
oos.writeObject(per);

Person p1 = (Person) ois.readObject();
Person p2 = (Person) ois.readObject();

//p1 == p2 返回 ture
//p2.getName() 还是孙悟空
```
</li>
</ul>
<h3>二、自定义序列化</h3>

通过实现特殊签名方法，实现自定义序列化，这些方法有：

```java
private void writeObject(java.io.ObjectOutputStream out) throws IOException
private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
private void readObjectNoData() throws ObjectStreamException

//该可以在序列化时替换掉原来的对象
ANY-ACCESS-MODIFIER Object writeReplace() throws ObjectStreamException
//与writeReplace相对地，下面的方法可以实现保护性复制整个对象
ANY-ACCESS-MODIFIER Object readResolve() throws ObjectStreamException
```
实现writeObject()和readObject方法的例子如下：
```java
public class Person implements java.io.Serializable
{
	private String name;
	private int age;
	// 注意可以不提供无参数的构造器!
	public Person(String name , int age)
	{
		System.out.println("有参数的构造器");
		this.name = name;
		this.age = age;
	}
	// 省略name与age的setter和getter方法

        //在序列化时会调用该方法，所以要手动将
	private void writeObject(java.io.ObjectOutputStream out) throws IOException
	{
		// 将name实例变量的值反转后写入二进制流
		out.writeObject(new StringBuffer(name).reverse());
		out.writeInt(age);
	}
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		// 将读取的字符串反转后赋给name实例变量
               //注意，这里的读取顺序必须和writeObject里的写入顺序要保持一致
		this.name = ((StringBuffer)in.readObject()).reverse().toString();
		this.age = in.readInt();
	}
}
```
关于writeReplace和readResolve的使用方法这里就不再详述了。详细内容见《疯狂Java讲义》15.8章
<h3>三、另一种自定义序列化机制</h3>
实现Externalizable接口并实现下面的两个方法，方法的重写方法和writeObject、readObject一样，但需要注意，使用这种方法自定义序列化时，必须提供public的无参数的构造器。

程序在反序列化时会先调用public的无参数的构造器创建实例，然后才执行readExternal()方法进行反序列化。
```java
public void writeExternal(java.io.ObjectOutput out) throws IOEception

public void readExternal(java.io.ObjectInput in) throws IOException, ClassNotFoundException
```
<h3>四、版本</h3>
总是在每个要序列化的类中加入版本信息是一个好的习惯：版本信息由serialVersionUID这个变量表示，在要序列化的类中直接定义即可：
```java
public class test{
    private static final long serialVersionUID = 512L;
   ....
}
```
<h3>五、对象序列化需要主要的内容</h3>
<ul>
 	<li><span style="color: #ff0000;">对象的类名、实例变量都会被序列化；方法、类变量、transient修饰的实例变量都不会被序列化</span></li>
 	<li><span style="color: #ff0000;">保证对象的引用型实例变量也是可序列化的，否则使用transient关键字标注，让序列化机制忽略该变量</span></li>
 	<li><span style="color: #ff0000;">反序列化对象时必须有对应的class文件</span></li>
 	<li><span style="color: #ff0000;">通过文件、网络来读取序列化后的对象时，必须按实际写入的顺序读取</span></li>
</ul>