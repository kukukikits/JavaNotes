# RestTemplate Using Tips

1. Use connection pool & Set request time out 

```java
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
 
@Configuration
public class RestTemplateUtil{
 
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
	RestTemplate restTemplate = builder.build();
	restTemplate.setRequestFactory(clientHttpRequestFactory()); 
	return restTemplate;
    }
	
    
    @Bean
    public HttpClientConnectionManager poolingConnectionManager() {
	PoolingHttpClientConnectionManager poolingConnectionManager = new PoolingHttpClientConnectionManager();
	poolingConnectionManager.setMaxTotal(1000); // 连接池最大连接数  
	poolingConnectionManager.setDefaultMaxPerRoute(100); // 每个主机的并发
	return poolingConnectionManager;
    }
    
    @Bean
    public HttpClientBuilder httpClientBuilder() {
	HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
	//设置HTTP连接管理器
	httpClientBuilder.setConnectionManager(poolingConnectionManager());
	// 重试次数，默认是3次，没有开启
	httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(2, true));
	// 保持长连接配置，需要在头添加Keep-Alive
	httpClientBuilder.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy());
	List<Header> headers = new ArrayList<>();
	headers.add(new BasicHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.16 Safari/537.36"));
	headers.add(new BasicHeader("Accept-Encoding", "gzip,deflate"));
	headers.add(new BasicHeader("Accept-Language", "zh-CN"));
	headers.add(new BasicHeader("Connection", "Keep-Alive"));
	httpClientBuilder.setDefaultHeaders(headers);
	return httpClientBuilder;
    }
    
    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() { 
	HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
	clientHttpRequestFactory.setHttpClient(httpClientBuilder().build());
	clientHttpRequestFactory.setConnectTimeout(6000); // 连接超时，毫秒		
	clientHttpRequestFactory.setReadTimeout(6000); // 读写超时，毫秒		
	// 连接不够用的等待时间，不宜过长，必须设置，比如连接不够用时，时间过长将是灾难性的
	clientHttpRequestFactory.setConnectionRequestTimeout(200);
	// 缓冲请求数据，默认值是true。通过POST或者PUT大量发送数据时，建议将此属性更改为false，以免耗尽内存。
	clientHttpRequestFactory.setBufferRequestBody(false);
	return clientHttpRequestFactory;
    }
}
```

2. Generic type support

```java
public class RestTemplateUtil{
   private static RestTemplate restTemplate = new RestTemplate();
   public static <T> List<T> exchangeAsList(String uri, ParameterizedTypeReference<List<T>> responseType) {
       return restTemplate.exchange(uri, HttpMethod.GET, null, responseType).getBody();
   }
   
   /**
    * You can invoke like this
    */
   public void test(){
       List<MyDto> dtoList = RestTemplateUtil.exchangeAsList("http://my/url", new ParameterizedTypeReference<List<MyDto>>() {});
   }
}

```

3. Request Example

```java
//1. using url variables
public class Test {
    private static RestTemplate restTemplate = new RestTemplate();
    private void urlVariablesTest(){
        Map<String, String> variables = new HashMap<>();
        variables.put("var1", "123456");
        variables.put("var2", "abcdef");
        restTemplate.exchange("http://localhost/query/{var1}?var2={var2}", HttpMethod.GET, HttpEntity.EMPTY, 
            responseType, map("empNo", empNo));
    }
    
}

```