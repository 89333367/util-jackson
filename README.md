# util-jackson
JSON工具类，封装了jackson


# 引入依赖
```xml
<dependency>
    <groupId>sunyu.util</groupId>
    <artifactId>util-jackson</artifactId>
    <!-- {util.version}_{jdk.version}_{architecture.version} -->
    <version>1.0_jdk8_x64</version>
</dependency>
<!-- https://central.sonatype.com/artifact/com.fasterxml.jackson.core/jackson-core/versions -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-core</artifactId>
    <version>2.19.2</version>
</dependency>
<!-- https://central.sonatype.com/artifact/com.fasterxml.jackson.core/jackson-annotations/versions -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-annotations</artifactId>
    <version>2.19.2</version>
</dependency>
<!-- https://central.sonatype.com/artifact/com.fasterxml.jackson.core/jackson-databind/versions -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.19.2</version>
</dependency>
<!-- https://central.sonatype.com/artifact/com.fasterxml.jackson.datatype/jackson-datatype-jdk8/versions -->
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jdk8</artifactId>
    <version>2.19.2</version>
</dependency>
<!-- https://central.sonatype.com/artifact/com.fasterxml.jackson.datatype/jackson-datatype-jsr310/versions -->
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
    <version>2.19.2</version>
</dependency>
```

# 如果在springboot项目中使用，这样配置
```java
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartFile;
import sunyu.util.JacksonUtil;

import java.time.ZoneId;

/**
 * Jackson配置
 *
 * @author SunYu
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JacksonUtil jacksonUtil() {
        return JacksonUtil.builder().addMixIn(MultipartFile.class).setTimeZone(ZoneId.of("UTC")).build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return jacksonUtil().getObjectMapper();
    }

}
```

# 测试类
```java
@Test
void t001() {
    JacksonUtil jacksonUtil = JacksonUtil.builder().build();//构建实例

    /**
     * {
     *   "library": {
     *     "books": [
     *       {"id": 1, "title": "Java编程思想", "price": 108},
     *       {"id": 2, "title": "深入理解Java虚拟机", "price": 89},
     *       {"id": 3, "title": "Effective Java", "price": 79}
     *     ]
     *   }
     * }
     */
    String json = "{\n" +
            "  \"library\": {\n" +
            "    \"books\": [\n" +
            "      {\"id\": 1, \"title\": \"Java编程思想\", \"price\": 108},\n" +
            "      {\"id\": 2, \"title\": \"深入理解Java虚拟机\", \"price\": 89},\n" +
            "      {\"id\": 3, \"title\": \"Effective Java\", \"price\": 79}\n" +
            "    ]\n" +
            "  }\n" +
            "}";
    JsonNode rootNode = jacksonUtil.readTree(json);

    // 1. 先获取数组长度（需通过绝对路径）
    JsonNode booksNode = rootNode.at("/library/books");
    int bookCount = booksNode.size();

    // 2. 循环构建绝对路径访问title
    for (int i = 0; i < bookCount; i++) {
        // 动态构建路径：/library/books/0/title, /library/books/1/title...
        JsonNode titleNode = rootNode.at("/library/books/" + i + "/title");
        log.info("书名: {}", titleNode.asText());
    }

    if (!booksNode.isMissingNode()) {
        for (JsonNode jsonNode : booksNode) {
            log.info("书名: {}", jsonNode.get("title").asText());
        }
    }

    if (booksNode.isArray()) {
        // 2. 遍历数组节点（无路径解析开销）
        for (JsonNode bookNode : booksNode) {
            // 3. 子节点直接get()访问字段
            String title = bookNode.get("title").asText();
            double price = bookNode.get("price").asDouble();
            log.info("书名: {}, 价格: {}", title, price);
        }
    }

    Map o = jacksonUtil.jsonToObj(json, Map.class);
    log.info("{}", o);

    Map o2 = jacksonUtil.jsonToObj(json, new TypeReference<Map>() {
    });
    log.info("{}", o2);

    String jsonStr = jacksonUtil.objToJson(o);
    log.info("{}", jsonStr);

    jacksonUtil.close();//回收资源
}

@Test
void t002() {
    JacksonUtil jacksonUtil = JacksonUtil.builder().setTimeZone(ZoneId.of("GMT+8")).build();//构建实例

    /**
     * {
     *     "showVerification": false,
     *     "platformUrl": null,
     *     "data": {
     *         "token": "d10a0b2fd9a9437397a0543a3a08a90b",
     *         "realName": "洋马农业超级管理员",
     *         "roles": "超级管理员",
     *         "roleIds": [
     *             "4"
     *         ],
     *         "userId": "4",
     *         "orgName": "洋马农机（中国）有限公司",
     *         "username": "ymadmin",
     *         "rootOrgId": "4",
     *         "orgId": "4",
     *         "editPwdFlag": "0",
     *         "productCategory": "1",
     *         "displayAllFunctionFlag": "0",
     *         "tenantId": "4"
     *     },
     *     "status": 0,
     *     "message": null,
     *     "timestamp": null,
     *     "end": "2025-07-09 15:19:49",
     *     "execution": null,
     *     "trace": null
     * }
     */
    String json = "{\n" +
            "    \"showVerification\": false,\n" +
            "    \"platformUrl\": null,\n" +
            "    \"data\": {\n" +
            "        \"token\": \"d10a0b2fd9a9437397a0543a3a08a90b\",\n" +
            "        \"realName\": \"洋马农业超级管理员\",\n" +
            "        \"roles\": \"超级管理员\",\n" +
            "        \"roleIds\": [\n" +
            "            \"4\"\n" +
            "        ],\n" +
            "        \"userId\": \"4\",\n" +
            "        \"orgName\": \"洋马农机（中国）有限公司\",\n" +
            "        \"username\": \"ymadmin\",\n" +
            "        \"rootOrgId\": \"4\",\n" +
            "        \"orgId\": \"4\",\n" +
            "        \"editPwdFlag\": \"0\",\n" +
            "        \"productCategory\": \"1\",\n" +
            "        \"displayAllFunctionFlag\": \"0\",\n" +
            "        \"tenantId\": \"4\"\n" +
            "    },\n" +
            "    \"status\": 0,\n" +
            "    \"message\": null,\n" +
            "    \"timestamp\": null,\n" +
            "    \"end\": \"2025-07-09 15:19:49\",\n" +
            "    \"execution\": null,\n" +
            "    \"trace\": null\n" +
            "}";
    Map m = jacksonUtil.jsonToObj(json, Map.class);
    log.info("{}", m.get("end"));

    jacksonUtil.close();//回收资源
}
```
