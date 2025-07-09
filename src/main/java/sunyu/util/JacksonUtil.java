package sunyu.util;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

/**
 * Jackson工具类
 *
 * @author SunYu
 */
public class JacksonUtil implements AutoCloseable {
    private final Log log = LogFactory.get();
    private final Config config;

    public static Builder builder() {
        return new Builder();
    }

    private JacksonUtil(Config config) {
        log.info("[构建JsonUtil] 开始");

        // 序列化：将 Java 对象转换为 JSON 字符串的过程
        // 反序列化：将 JSON 字符串转换为 Java 对象的过程

        // ========== 序列化配置 ==========
        // 禁用日期时间序列化为时间戳格式（如 1620000000）
        config.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 忽略 null 值字段，不参与序列化输出
        config.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // 当对象无任何可序列化属性时，返回空对象 {} 而非抛出异常
        config.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // ========== 反序列化配置 ==========
        // 忽略 JSON 中存在但 Java 类中无对应字段的属性，避免抛出异常
        config.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // ========== 时间格式与序列化器配置 ==========
        // 设置默认日期格式（java.util.Date 使用）
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone(config.zoneId));
        config.objectMapper.setDateFormat(dateFormat);

        // 创建自定义模块以注册 Java 8 时间类型序列化/反序列化器
        SimpleModule customModule = new SimpleModule();
        // 注册 Java 8 时间类型的序列化器
        customModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(config.zoneId)
        ));
        customModule.addSerializer(LocalDate.class, new LocalDateSerializer(
                DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(config.zoneId)
        ));

        // 将 Long 类型序列化为字符串，防止前端精度丢失问题
        customModule.addSerializer(Long.class, ToStringSerializer.instance);

        // 注册 Java 8 时间类型的反序列化器
        customModule.addDeserializer(LocalDateTime.class, new CustomLocalDateTimeDeserializer());
        customModule.addDeserializer(LocalDate.class, new CustomLocalDateDeserializer());

        // 注册自定义模块到 ObjectMapper
        config.objectMapper.registerModule(customModule);

        // ========== 混入配置（MixIn）==========
        // 添加忽略类型：使用 MixIn 接口忽略特定类的序列化/反序列化行为
        for (Class<?> mixin : config.mixins) {
            config.objectMapper.addMixIn(mixin, JsonIgnoreTypeInterface.class);
        }

        log.info("[构建JsonUtil] 结束");
        this.config = config;
    }

    private static class Config {
        private final ObjectMapper objectMapper = new ObjectMapper();
        private ZoneId zoneId = ZoneId.of("UTC");//ZoneId.of("GMT+8")
        private final Set<Class<?>> mixins = new HashSet<>();
    }

    public static class Builder {
        private final Config config = new Config();

        /**
         * 构建实例
         *
         * @return 工具实例
         */
        public JacksonUtil build() {
            return new JacksonUtil(config);
        }

        /**
         * 设置时区
         * ZoneId.of("GMT+8")
         * ZoneId.of("UTC")
         *
         * @param timeZone 时区
         * @return Builder实例
         */
        public Builder setTimeZone(ZoneId timeZone) {
            config.zoneId = timeZone;
            return this;
        }

        /**
         * 添加忽略类型
         *
         * @param target 目标对象
         * @return Builder实例
         */
        public Builder addMixIn(Class<?> target) {
            config.mixins.add(target);
            return this;
        }
    }

    /**
     * 回收资源
     */
    @Override
    public void close() {
        log.info("[销毁JsonUtil] 开始");
        log.info("[销毁JsonUtil] 结束");
    }

    /**
     * 获取ObjectMapper实例
     *
     * @return ObjectMapper实例
     */
    public ObjectMapper getObjectMapper() {
        return config.objectMapper;
    }

    /**
     * 对象转换
     *
     * @param obj  对象
     * @param type 类型
     * @param <T>  泛型类型
     * @return 转换后的对象
     */
    public <T> T convert(Object obj, Class<T> type) {
        return config.objectMapper.convertValue(obj, type);
    }

    /**
     * 对象转换
     *
     * @param obj 对象
     * @param ref 类型
     * @param <T> 泛型类型
     * @return 转换后的对象
     */
    public <T> T convert(Object obj, TypeReference<T> ref) {
        return config.objectMapper.convertValue(obj, ref);
    }

    /**
     * 将 JSON 字符串转换为指定类型的对象
     *
     * @param json JSON 字符串
     * @param type 目标对象的类型
     * @param <T>  泛型类型
     * @return 转换后的对象
     */
    public <T> T jsonToObj(String json, Class<T> type) {
        try {
            return config.objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            log.error("JSON 字符串转换为对象失败，JSON: {}, 目标类型: {}", json, type.getName(), e);
            return null;
        }
    }

    /**
     * 将对象转换为 JSON 字符串
     *
     * @param json JSON 字符串
     * @param ref  目标对象的类型
     * @param <T>  泛型类型
     * @return 转换后的对象
     */
    public <T> T jsonToObj(String json, TypeReference<T> ref) {
        try {
            return config.objectMapper.readValue(json, ref);
        } catch (JsonProcessingException e) {
            log.error("JSON 字符串转换为对象失败，JSON: {}, 目标类型: {}", json, ref.getType().getTypeName(), e);
            return null;
        }
    }

    /**
     * 将对象转换为 JSON 字符串
     *
     * @param obj 对象
     * @return JSON 字符串
     */
    public String objToJson(Object obj) {
        try {
            return config.objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("对象转换为 JSON 字符串失败，对象: {}", obj, e);
            return null;
        }
    }

    /**
     * 读取 JSON 树
     *
     * @param json JSON字符串
     * @return JSON树
     */
    public JsonNode readTree(String json) {
        try {
            return config.objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error(e);
            return null;
        }
    }

}