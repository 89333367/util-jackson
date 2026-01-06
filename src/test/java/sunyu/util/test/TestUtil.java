package sunyu.util.test;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import sunyu.util.JsonUtil;

import java.util.ArrayList;
import java.util.Map;

public class TestUtil {
    private final Log log = LogFactory.get();

    @Test
    void t001() {
        JsonUtil jsonUtil = JsonUtil.builder().build();//构建实例

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
        JsonNode rootNode = jsonUtil.readTree(json);

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

        Map o = jsonUtil.jsonToObj(json, Map.class);
        log.info("{}", o);

        Map o2 = jsonUtil.jsonToObj(json, new TypeReference<Map>() {
        });
        log.info("{}", o2);

        String jsonStr = jsonUtil.objToJson(o);
        log.info("{}", jsonStr);

        jsonUtil.close();//回收资源
    }

    @Test
    void t002() {
        JsonUtil jsonUtil = JsonUtil.builder().setTimeZone("GMT+8").build();//构建实例

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
        Map m = jsonUtil.jsonToObj(json, Map.class);
        log.info("{}", m.get("end"));

        jsonUtil.close();//回收资源
    }

    @Test
    void t003() {
        JsonUtil jsonUtil = JsonUtil.builder().setTimeZone("UTC").build();//构建实例
        jsonUtil.close();
    }

    @Test
    void t004() {
        String json = "{\n" +
                "  \"username\": \"yxadmin\",\n" +
                "  \"password\": \"yxzg-12345678\"\n" +
                "}";
        JsonUtil jsonUtil = JsonUtil.builder().build();
        JsonNode root = jsonUtil.readTree(json);
        log.info("{}", root);
        JsonNode username = root.at("/username");
        log.info("{}", username);
        jsonUtil.setValueByJsonPtrExpr(root, "/username", "sunyu");
        log.info("{}", root);
        boolean b = jsonUtil.setValueByJsonPtrExpr(root, "/newNode", 123);
        log.info("{}", b);
        log.info("{}", root);
        jsonUtil.setValueByJsonPtrExpr(root, "/newNode2", new String[]{null, null, "索引为2"});
        log.info("{}", root);
        jsonUtil.setValueByJsonPtrExpr(root, "/newNode3", new ArrayList<String>() {
            {
                add(null);
                add(null);
                add("索引为2");
            }
        });
        jsonUtil.setValueByJsonPtrExpr(root, "/username", null);
        log.info("{}", root);
        log.info("{}", jsonUtil.objToJson(root));
        jsonUtil.close();
    }

}