# RestTemplate config

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
	return httpClientBuilder;
    }
    
    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() { 
	HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
	clientHttpRequestFactory.setHttpClient(httpClientBuilder().build());
	clientHttpRequestFactory.setConnectTimeout(6000); // 连接超时，毫秒		
	clientHttpRequestFactory.setReadTimeout(6000); // 读写超时，毫秒		
	return clientHttpRequestFactory;
    }
}
```