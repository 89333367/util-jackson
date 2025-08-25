package sunyu.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

/**
 * JSON工具类
 *
 * @author SunYu
 */
public class JsonUtil implements AutoCloseable {
    private final Log log = LogFactory.get();
    private final Config config;

    /**
     * 创建 JacksonUtil 构建器
     *
     * @return Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 私有构造函数，通过 Builder 模式创建实例
     *
     * @param config 配置对象
     */
    private JsonUtil(Config config) {
        log.info("[{}] 构建开始", this.getClass().getSimpleName());

        // 序列化配置：禁用日期时间序列化为时间戳格式
        config.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 序列化配置：忽略 null 值字段，不参与序列化输出
        config.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // 序列化配置：当对象无任何可序列化属性时，返回空对象 {} 而非抛出异常
        config.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // 反序列化配置：忽略 JSON 中存在但 Java 类中无对应字段的属性，避免抛出异常
        config.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 时间格式配置：设置默认日期格式（java.util.Date 使用）
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone(config.zoneId));
        config.objectMapper.setDateFormat(dateFormat);

        // 自定义模块配置：注册 Java 8 时间类型序列化/反序列化器
        SimpleModule customModule = new SimpleModule();
        // 注册 LocalDateTime 序列化器
        customModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of(config.zoneId))
        ));
        // 注册 LocalDate 序列化器
        customModule.addSerializer(LocalDate.class, new LocalDateSerializer(
                DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of(config.zoneId))
        ));
        // 注册 Long 类型序列化器，将 Long 类型序列化为字符串，防止前端精度丢失问题
        customModule.addSerializer(Long.class, ToStringSerializer.instance);
        // 注册 LocalDateTime 反序列化器
        customModule.addDeserializer(LocalDateTime.class, new CustomLocalDateTimeDeserializer());
        // 注册 LocalDate 反序列化器
        customModule.addDeserializer(LocalDate.class, new CustomLocalDateDeserializer());

        // 注册自定义模块到 ObjectMapper
        config.objectMapper.registerModule(customModule);

        // 混入配置：添加忽略类型，使用 MixIn 接口忽略特定类的序列化/反序列化行为
        for (Class<?> mixin : config.mixins) {
            config.objectMapper.addMixIn(mixin, JsonIgnoreTypeInterface.class);
        }

        log.info("[{}] 构建结束", this.getClass().getSimpleName());
        this.config = config;
    }

    /**
     * 内部配置类，封装 ObjectMapper 及相关配置
     */
    private static class Config {
        /**
         * ObjectMapper 实例，用于 JSON 序列化和反序列化
         */
        private final ObjectMapper objectMapper = new ObjectMapper();

        /**
         * 时区配置，默认为 UTC
         */
        private String zoneId = "UTC";

        /**
         * 混入类型集合
         */
        private final Set<Class<?>> mixins = new HashSet<>();
    }

    /**
     * 构建器类，用于创建 JacksonUtil 实例
     */
    public static class Builder {
        private final Config config = new Config();

        /**
         * 构建 JacksonUtil 实例
         *
         * @return JacksonUtil 实例
         */
        public JsonUtil build() {
            // 增加时区非空校验
            if (config.zoneId == null || config.zoneId.isEmpty()) {
                throw new IllegalArgumentException("时区配置不能为空");
            }

            // 验证zoneId有效性
            try {
                ZoneId.of(config.zoneId);
            } catch (Exception e) {
                throw new IllegalArgumentException("无效的时区ID: " + config.zoneId, e);
            }

            return new JsonUtil(config);
        }

        /**
         * 设置时区
         *
         * @param zoneId 时区 ID，如 "UTC"、"GMT+8"
         *
         * @return Builder 实例
         *
         * @throws IllegalArgumentException 无效的时区ID
         */
        public Builder setTimeZone(String zoneId) {
            if (zoneId == null || zoneId.isEmpty()) {
                throw new IllegalArgumentException("时区ID不能为空");
            }

            try {
                ZoneId.of(zoneId); // 提前验证时区有效性
                config.zoneId = zoneId;
                return this;
            } catch (Exception e) {
                throw new IllegalArgumentException("无效的时区ID: " + zoneId, e);
            }
        }

        /**
         * 添加忽略类型，用于序列化/反序列化时忽略特定类
         *
         * @param target 目标类
         *
         * @return Builder 实例
         */
        public Builder addMixIn(Class<?> target) {
            config.mixins.add(target);
            return this;
        }
    }

    @Override
    public void close() {
        // 如果有需要关闭的资源，这里添加实际关闭逻辑
        // 目前ObjectMapper不需要显式关闭
        log.info("[{}] 销毁开始", this.getClass().getSimpleName());
        log.info("[{}] 销毁结束", this.getClass().getSimpleName());
    }

    /**
     * 获取节点的子节点
     * <p>
     * 根据键名获取子节点，支持对象的属性名和数组的索引访问
     * 支持数组的负索引访问，例如-1表示最后一个元素
     * </p>
     *
     * @param node 节点
     * @param key  键名（对象属性名或数组索引）
     *
     * @return 子节点，如果不存在则返回 null
     */
    private JsonNode getNodeChild(JsonNode node, String key) {
        if (node instanceof ObjectNode) {
            // 对象节点，通过属性名获取子节点
            return node.get(key);
        } else if (node instanceof ArrayNode && isNumeric(key)) {
            // 数组节点且键名为数字，通过索引获取子节点
            try {
                int index = Integer.parseInt(key);
                ArrayNode arrayNode = (ArrayNode) node;
                // 处理负索引
                if (index < 0) {
                    index = arrayNode.size() + index;
                }
                // 确保索引在有效范围内
                if (index >= 0 && index < arrayNode.size()) {
                    return arrayNode.get(index);
                }
            } catch (NumberFormatException e) {
                // 索引格式错误，忽略异常
            }
        }
        return null;
    }

    /**
     * 在指定对象节点上设置值
     * <p>
     * 根据值的类型选择合适的设置方法，支持各种基本类型和复杂对象
     * </p>
     *
     * @param node  对象节点
     * @param key   键名
     * @param value 值对象
     */
    private void setNodeValue(ObjectNode node, String key, Object value) {
        if (value == null) {
            // 设置 null 值
            node.putNull(key);
        } else if (value instanceof String) {
            // 设置字符串值
            node.put(key, (String) value);
        } else if (value instanceof Integer) {
            // 设置整数值
            node.put(key, (Integer) value);
        } else if (value instanceof Long) {
            // 设置长整数值
            node.put(key, (Long) value);
        } else if (value instanceof Double) {
            // 设置双精度浮点数值
            node.put(key, (Double) value);
        } else if (value instanceof Boolean) {
            // 设置布尔值
            node.put(key, (Boolean) value);
        } else if (value instanceof JsonNode) {
            // 设置 JsonNode 值
            node.set(key, (JsonNode) value);
        } else {
            // 其他类型，转换为 JsonNode 后设置
            try {
                JsonNode valueNode = config.objectMapper.valueToTree(value);
                node.set(key, valueNode);
            } catch (Exception e) {
                log.warn("无法序列化对象到JsonNode，类型: {}", value.getClass().getName(), e);
                node.putNull(key);
            }
        }
    }

    /**
     * 检查字符串是否为数字（仅用于数组索引解析）
     * <p>
     * 支持负数和正数整数，不支持浮点数和科学计数法
     * </p>
     */
    private boolean isNumeric(String str) {
        if (StrUtil.isBlank(str)) {
            return false;
        }

        int startIndex = 0;
        if (str.charAt(0) == '-') {
            if (str.length() == 1) {
                return false; // 仅负号
            }
            startIndex = 1;
        }

        for (int i = startIndex; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 根据对象创建 JsonNode
     * <p>
     * 将 Java 对象转换为 JsonNode，处理 null 值和已有的 JsonNode 对象
     * </p>
     *
     * @param value 对象值
     *
     * @return 对应的 JsonNode
     */
    private JsonNode createJsonNodeFromObject(Object value) {
        if (value == null) {
            // null 值转换为 nullNode
            return config.objectMapper.nullNode();
        } else if (value instanceof JsonNode) {
            // 已经是 JsonNode，直接返回
            return (JsonNode) value;
        } else {
            // 其他类型转换为 JsonNode
            return config.objectMapper.valueToTree(value);
        }
    }

    /**
     * 解析 JSON Pointer 路径表达式
     * <p>
     * 将 JSON Pointer 路径表达式（如 "/user/name"）解析为路径段数组（["user", "name"]）
     * 支持处理连续斜杠（如 "//user//name" 会解析为 ["user", "name"]）
     * </p>
     *
     * @param jsonPtrExpr JSON Pointer 路径表达式
     *
     * @return 路径段数组
     */
    private String[] parseJsonPointerPath(String jsonPtrExpr) {
        if (jsonPtrExpr == null || jsonPtrExpr.isEmpty() || "/".equals(jsonPtrExpr)) {
            return new String[0];
        }

        String path = jsonPtrExpr.startsWith("/") ? jsonPtrExpr.substring(1) : jsonPtrExpr;
        // 过滤掉空字符串段
        return java.util.Arrays.stream(path.split("/"))
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    /**
     * 反转义 JSON Pointer token
     * <p>
     * 处理 JSON Pointer 中的特殊转义字符：
     * ~0 表示 ~ 字符
     * ~1 表示 / 字符
     * </p>
     *
     * @param token 需要反转义的 token
     *
     * @return 反转义后的 token
     */
    private String unescapeJsonPointerToken(String token) {
        if (token == null) {
            return null;
        }
        // 先替换 ~0 为 ~，再替换 ~1 为 /（顺序不可调换）
        return token.replace("~0", "~").replace("~1", "/");
    }

    /**
     * 设置 JSON 节点的值（直接修改原节点）
     * <p>
     * 注意：此方法会直接修改传入的原始 JsonNode，不会创建副本。
     * 传入的 root 节点必须是可变的（ObjectNode 或 ArrayNode）。
     * 对于不存在的中间节点，会自动创建相应的对象或数组节点。
     * </p>
     *
     * @param root        JSON 根节点（必须是可变节点）
     * @param jsonPtrExpr JSON Pointer 表达式，如 "/user/name" 或 "/users/0/name"
     * @param value       要设置的值(不存在的节点将创建)
     *
     * @return 操作是否成功
     */
    public boolean setValueByJsonPtrExpr(JsonNode root, String jsonPtrExpr, Object value) {
        // 增强参数校验的空指针防护
        if (root == null || StrUtil.isBlank(jsonPtrExpr) ||
                !(root instanceof ObjectNode || root instanceof ArrayNode)) {
            log.warn("参数校验失败：root为{}，jsonPtrExpr为{}",
                    root == null ? "null" : "非可变节点", jsonPtrExpr);
            return false;
        }

        try {
            // 解析 JSON Pointer 路径表达式为路径段数组
            String[] parts = parseJsonPointerPath(jsonPtrExpr);
            if (parts.length == 0) {
                if (root instanceof ObjectNode) {
                    // 全路径删除时设置值为空对象
                    ((ObjectNode) root).removeAll();
                }
                return true;
            }

            // 从根节点开始，逐级查找父节点，如果不存在则创建
            JsonNode currentNode = root;
            // 遍历到倒数第二个路径段，找到要修改值的父节点
            for (int i = 0; i < parts.length - 1; i++) {
                String part = unescapeJsonPointerToken(parts[i]);
                JsonNode childNode = getNodeChild(currentNode, part);

                // 如果子节点不存在，则创建
                if (childNode == null) {
                    if (currentNode instanceof ObjectNode) {
                        // 判断下一个part是否为数字来决定创建ObjectNode还是ArrayNode
                        String nextPart = (i + 1 < parts.length) ? unescapeJsonPointerToken(parts[i + 1]) : null;
                        if (isNumeric(nextPart)) {
                            // 如果下一个是数字索引，则创建数组
                            ArrayNode newArrayNode = createArrayNode();
                            ((ObjectNode) currentNode).set(part, newArrayNode);
                            currentNode = newArrayNode;
                        } else {
                            // 否则创建对象节点
                            ObjectNode newObjectNode = createObjectNode();
                            ((ObjectNode) currentNode).set(part, newObjectNode);
                            currentNode = newObjectNode;
                        }
                    } else if (currentNode instanceof ArrayNode && isNumeric(part)) {
                        // 如果当前节点是数组且索引有效
                        int index = Integer.parseInt(part);
                        ArrayNode arrayNode = (ArrayNode) currentNode;
                        // 处理负索引
                        if (index < 0) {
                            // 负索引转换为正索引，空数组时index会是-1，此时保持为负数以触发后续检查
                            if (!arrayNode.isEmpty()) {
                                index = arrayNode.size() + index;
                            }
                        }
                        // 扩展数组到所需大小
                        if (index >= 0) {
                            while (arrayNode.size() <= index) {
                                arrayNode.addNull();
                            }
                        }
                        // 如果指定位置是null，根据下一个part决定创建ArrayNode还是ObjectNode
                        if (index >= 0 && index < arrayNode.size() && arrayNode.get(index).isNull()) {
                            String nextPart = (i + 1 < parts.length) ? unescapeJsonPointerToken(parts[i + 1]) : null;
                            if (isNumeric(nextPart)) {
                                // 如果下一个是数字索引，则创建数组
                                ArrayNode newArrayNode = createArrayNode();
                                arrayNode.set(index, newArrayNode);
                                currentNode = newArrayNode;
                            } else {
                                // 否则创建对象节点
                                ObjectNode newObjectNode = createObjectNode();
                                arrayNode.set(index, newObjectNode);
                                currentNode = newObjectNode;
                            }
                        } else if (index >= 0 && index < arrayNode.size()) {
                            currentNode = arrayNode.get(index);
                        } else {
                            // 负索引无效或正索引越界，无法创建节点
                            log.warn("索引越界，无法创建节点，index: {}, arraySize: {}", index, arrayNode.size());
                            return false;
                        }
                    } else {
                        // 路径无效，无法创建节点
                        log.warn("路径无效，无法创建节点，currentNode: {}, part: {}", currentNode, part);
                        return false;
                    }
                } else {
                    currentNode = childNode;
                }
            }

            // 获取最后一个路径段作为键名
            String lastKey = unescapeJsonPointerToken(parts[parts.length - 1]);

            // 根据父节点类型设置值
            if (currentNode instanceof ObjectNode) {
                // 父节点是对象类型，直接设置键值对
                setNodeValue((ObjectNode) currentNode, lastKey, value);
            } else if (currentNode instanceof ArrayNode && isNumeric(lastKey)) {
                // 父节点是数组类型且键名是数字索引
                try {
                    int index = Integer.parseInt(lastKey);
                    ArrayNode arrayNode = (ArrayNode) currentNode;
                    // 优化负索引处理逻辑
                    if (index < 0) {
                        index = arrayNode.size() + index;
                        if (index < 0) {
                            log.warn("无效的负索引，index: {}", index);
                            return false;
                        }
                    }
                    // 检查索引是否有效（支持在数组末尾追加元素）
                    // 优化数组扩展逻辑
                    if (index >= arrayNode.size()) {
                        // 直接设置到指定索引位置，自动填充null值
                        arrayNode.insert(index, createJsonNodeFromObject(value));
                    } else {
                        // 修改指定索引位置的元素
                        arrayNode.set(index, createJsonNodeFromObject(value));
                    }
                } catch (NumberFormatException e) {
                    log.error("索引格式错误: {}", lastKey, e);
                    return false;
                } catch (Exception e) {
                    log.error("意外错误，根节点类型: {}, 路径: {}",
                            root.getClass().getSimpleName(), // 直接访问已确认非空的root
                            jsonPtrExpr,
                            e);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            // 优化异常捕获类型，增加错误定位信息
            log.error("设置JSON节点值失败，根节点类型: {}, 路径: {}, 值类型: {} 原因: {}",
                    root.getClass().getSimpleName(), // 直接访问已确认非空的root
                    jsonPtrExpr,
                    value == null ? "null" : value.getClass().getSimpleName(),
                    e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取 ObjectMapper 实例
     *
     * @return ObjectMapper 实例
     */
    public ObjectMapper getObjectMapper() {
        return config.objectMapper;
    }

    /**
     * 对象转换，将源对象转换为目标类型
     *
     * @param obj  源对象
     * @param type 目标类型 Class
     * @param <T>  泛型类型
     *
     * @return 转换后的对象，如果 obj 为 null 返回 null
     */
    public <T> T convert(Object obj, Class<T> type) {
        if (obj == null) {
            log.warn("尝试转换 null 对象");
            return null;
        }
        return config.objectMapper.convertValue(obj, type);
    }

    /**
     * 对象转换，将源对象转换为目标类型（支持泛型）
     *
     * @param obj 源对象
     * @param ref 目标类型 TypeReference
     * @param <T> 泛型类型
     *
     * @return 转换后的对象
     */
    public <T> T convert(Object obj, TypeReference<T> ref) {
        return config.objectMapper.convertValue(obj, ref);
    }

    /**
     * JSON 字符串转对象（适用于小文件）
     *
     * @param json JSON 字符串
     * @param type 目标对象类型
     * @param <T>  泛型类型
     *
     * @return 转换后的对象
     */
    public <T> T jsonToObj(String json, Class<T> type) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        try {
            return config.objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            log.error("JSON 字符串转换为对象失败，JSON: {}, 目标类型: {} {}", json, type.getName(), e);
            return null;
        }
    }

    /**
     * JSON 字符串转对象（适用于小文件，支持泛型）
     *
     * @param json JSON 字符串
     * @param ref  目标对象类型引用
     * @param <T>  泛型类型
     *
     * @return 转换后的对象
     */
    public <T> T jsonToObj(String json, TypeReference<T> ref) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        try {
            return config.objectMapper.readValue(json, ref);
        } catch (JsonProcessingException e) {
            log.error("JSON 字符串转换为对象失败，JSON: {}, 目标类型: {} {}", json, ref.getType().getTypeName(), e);
            return null;
        }
    }

    /**
     * 对象转 JSON 字符串
     *
     * @param obj 对象
     *
     * @return JSON 字符串
     */
    public String objToJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return config.objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("对象转换为 JSON 字符串失败，对象: {} {}", obj, e);
            return null;
        }
    }

    /**
     * 流式读取大文件 JSON，使用 JsonParser 避免将整个文件加载到内存
     *
     * @param file JSON 文件
     *
     * @return JsonNode 对象
     */
    public JsonNode readTree(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        try (JsonParser parser = config.objectMapper.getFactory().createParser(file)) {
            return config.objectMapper.readTree(parser);
        } catch (IOException e) {
            log.error("读取 JSON 文件失败: {}", e);
            return null;
        }
    }

    /**
     * 流式读取 JSON 字符串，使用 JsonParser 避免创建大型中间对象
     *
     * @param json JSON 字符串
     *
     * @return JsonNode 对象
     */
    public JsonNode readTree(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        try (JsonParser parser = config.objectMapper.getFactory().createParser(json)) {
            return config.objectMapper.readTree(parser);
        } catch (IOException e) {
            log.error("读取 JSON 字符串失败，JSON: {} {}", json, e);
            return null;
        }
    }

    /**
     * 创建 JSON 对象节点
     *
     * @return ObjectNode 对象
     */
    public ObjectNode createObjectNode() {
        return config.objectMapper.createObjectNode();
    }

    /**
     * 创建 JSON 数组节点
     *
     * @return ArrayNode 对象
     */
    public ArrayNode createArrayNode() {
        return config.objectMapper.createArrayNode();
    }

    /**
     * 判断字符串是否为有效的 JSON 对象或数组，使用流式处理避免内存问题
     *
     * @param json 待检测的字符串
     *
     * @return 如果是 JSON 对象或数组则返回 true，否则返回 false
     */
    public boolean isJsonOrArray(String json) {
        if (StrUtil.isBlank(json)) {
            return false;
        }
        try (JsonParser parser = config.objectMapper.getFactory().createParser(json)) {
            JsonNode jsonNode = config.objectMapper.readTree(parser);
            return jsonNode != null && (jsonNode.isObject() || jsonNode.isArray());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取 JSON 节点，使用 JSON Pointer 表达式定位节点
     *
     * @param root        JSON 根节点
     * @param jsonPtrExpr JSON Pointer 表达式，如 "/user/name" 或 "/users/0/name"
     *
     * @return 对应路径的 JsonNode，如果路径不存在则返回 MissingNode
     */
    public JsonNode getJsonNodeByJsonPtrExpr(JsonNode root, String jsonPtrExpr) {
        return root.at(jsonPtrExpr);
    }

}