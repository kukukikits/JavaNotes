# 配置swagger，让其支持spring Pageable接口作为ApiModel

java配置
```java
import org.springframework.data.domain.Pageable;
@Configuration
@EnableSwagger2
public class SwaggerConfig {
    @Bean
    public Docket docket(){
        return new Docket(DocumentationType.SWAGGER_2).apiInfo(apiInfo())
                //修正Byte转string的Bug
                .directModelSubstitute(Byte.class, Integer.class)
                .alternateTypeRules(pageableTypeRule())
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any()).build();

    }

    private AlternateTypeRule pageableTypeRule() {
        return AlternateTypeRules.newRule(Pageable.class, Page.class, 1);
    }
    //构建api文档的详细信息函数
    private ApiInfo apiInfo(){
        return new ApiInfoBuilder()
                //页面标题
                .title("页面标题")
                //创建人
                .contact(new Contact("GeShengBin","","kukukiki19940703@163.com"))
                //版本号
                .version("1.0")
                //描述
                .description("API 描述")
                .build();
    }

    @ApiModel
    public static class Page {
        @ApiModelProperty(value = "第page页,从0开始计数", example = "0")
        private Integer page;

        @ApiModelProperty(value = "每页数据数量", example = "10")
        private Integer size;

        @ApiModelProperty("按这种格式写：GET /path/all?size=100&page=3&sort=id,asc&sort=name,desc")
        private List<String> sort;

        public Integer getPage() {
            return page;
        }

        public void setPage(Integer page) {
            this.page = page;
        }

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }

        public List<String> getSort() {
            return sort;
        }

        public void setSort(List<String> sort) {
            this.sort = sort;
        }
    }
}
```

Controller上使用Pageable

```java
import org.springframework.data.domain.Pageable;

public class MyController{
    @ApiOperation("分页请求, 请求格式GET /path/all?size=100&page=3&sort=id,asc&sort=name,desc")
    @PostMapping("/pageRequest")
    public Result listFacePhotos(@PageableDefault Pageable pageRequest)  {
        ...
    }
}
```