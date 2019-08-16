Tips: 知识点总结
- Object类实现“克隆”三步走：step1 实现Cloneable接口；step2 实现clone()方法；step3 clone()方法内使用return (<ChildClass>)super.clone()。该方法为<span style="color: #ff0000;">“浅克隆”</span>。
- Objects.requireNonNull(Object)，Object为null时引发异常，该方法主要用来对方法形参进行<span style="color: #ff0000;">输入校验。</span>
- StringBuffer线程安全，StringBuilder非线程安全。
- 创造随机数：Random rand = new Random(System.currentTimeMillis())，由于Random产生的是伪随机数，所以这里使用时间作为Random对象的种子，保证随机。ThreadLocalRandom的使用类似。
- 不要使用BigDecimal(double val)，使用BingDecimal(String val)。如果使用double作为初始化参数，应该使用BigDecimal.valueOf(double value)。
- Calendar.setLenient(false)关闭日历类的容错性，让时间进行严格的检查。lenient模式下容错，non-lenient模式下字段值超出允许范围会抛出异常
- Calendar对象的set方法延迟修改，在获取时间时才会重新计算日历的时间
- Java*新增java.time包
- Java中正则表达式的反斜杠本身需要转义，如“c\\wt”，可以匹配cat、cbt、c0t等
- Pattern对象是正则表达式编译后在内存中的表示形式。Pattern不可变类，线程安全。
	```java
	Pattern p = Patern.compile("a*b");
	Matcher m = p.matcher("aaaaab");
	boolean b = m.matches(); // return true

	//或者
	boolean b = Pattern.matches("a*b", "aaaaab");
	```
 - Java国际化：
	```java 
	java.util.ResourceBundle：加载国家、语言资源包
	java.util.Locale：封装国家/区域、语言环境
	java.text.MessageFormat：格式化字符串

	资源文件的命名:
	baseName_language_country.properties
	baseName_language_properties
	baseName.properties
	其中baseName可以随意，language和country不能随意变化。

	ResourceBundle bundle = ResourceBundle.getBundle("baseName", myLocale); //加载资源文件
	```
- Java支持的国家和语言
	```java
	Locale[] localeList = Locale.getAvailableLocales(); //返回Java支持的全部国家和语言```
- 格式化字符串、数字、日期
	```java
	String msg = MessageFormat.format("Hello {0} , bye {1}", "Tom", "LiLei");

	//NumberFormat是抽象基类，无法使用构造器实例化
	NumberFormat nf = NumberFormat.getNumberInstance(); //NumberFormat.getPercentinstance()等方法
	String fnum = nf.format(123456.0007);

	//DateFormat
	DateFormat df = DateFormat.getDateInstance(); //或DateFormat.getTimeInstance() 或DateFormat.getDateTimeInstance()
	//df.setLenient(false) //关闭容错性
	String fdate = df.format(new Date());

	可以使用SimpleDateFormat格式化日期，功能比DateFormat更强大，自行翻阅书籍API吧 
	```
- Java8新增的日期、时间格式器
详细的API内容请看[官方文档](https://docs.oracle.com/javase/8/docs/technotes/guides/datetime/index.html)
	1. 获取DateTimeFormatter对象
	方法一：直接使用DateTimeFormatter类的静态常量
	方法二：通过FormatStyle枚举类，获取不同风格的DateTimeFormatter实例
	方法三：根据模式字符串来创建DateTimeFormatter对象

	2. 使用DateTimeFormatter格式化
	方法一：String fdate = DateTimeFormatter对象.format( TemporalAccessor temporal) //传入localDate、LocalDateTime、LocalTime等TemporalAccessor接口的实现类的对象
	方法二：调用localDate、LocalDateTime、LocalTime等对象的format(DateTimeFormatter formatter)

	3. 使用DateTimeFormatter解析字符串
	调用LocalDate、LocalDateTime、LocalTime等对象的parse(CharSequence text, DateTimeFormatter formatter)方法解析。
- 国际化资源文件搜索顺序
	```text
	baseName_zh_CN.class
	baseName_zh_CN.properties
	baseName_zh.class
	baseName_zh.properties
	baseName.class
	baseName.properties
	```