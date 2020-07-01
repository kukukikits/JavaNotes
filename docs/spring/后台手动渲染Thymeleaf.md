# 后台手动渲染Thymeleaf模板，然后返回HTML给前端

```java

public class MyController {
    @Autowired
    private ThymeleafViewResolver thymeleafViewResolver;
    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping(value = "/html", produces = {MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_XHTML_XML_VALUE, MediaType.APPLICATION_XML_VALUE})
    public String noPersonInfoPage(@ApiIgnore HttpServletRequest request, 
                                   @ApiIgnore HttpServletResponse response, @ApiIgnore  ModelMap modelMap) {
        Person person = new Person();
        person.setName("张三");
        person.setGender("man");
        modelMap.put("person", person);
        modelMap.put("objectMapper", objectMapper);
        WebContext ctx = new WebContext(request,response,request.getServletContext(),
                request.getLocale(), modelMap);

        // template是Thymeleaf模板的名字，一般在Spring boot项目中resources/templates文件夹下有个template.html文件与之对应
        String content = thymeleafViewResolver.getTemplateEngine().process("template", ctx);
        return content;
    }
}

```

本例的template.html模板如下：

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<body>
<form method="post" th:attr="action=@{/edit/person}, data-origin=${objectMapper.writeValueAsString(person)}" >
    <fieldset>
        <label for="name">姓名:</label>
        <input id="name" type="text" name="name" th:value="${person.name}"/>
        <label for="gender">性别:</label>
        <select id="gender" name="gender">
            <option value="m" th:selected="${person.gender} == 'man'">男</option>
            <option value="w" th:selected="${person.gender} == 'woman'">女</option>
        </select>
    </fieldset>
</form>
</body>
</html>

```
