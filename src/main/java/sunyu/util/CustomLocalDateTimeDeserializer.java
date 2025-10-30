package sunyu.util;

import java.io.IOException;
import java.time.LocalDateTime;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import cn.hutool.core.date.LocalDateTimeUtil;

/**
 * 自定义LocalDateTime反序列化器
 *
 * @author SunYu
 */
public class CustomLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    @Override
    public LocalDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        LocalDateTime t = null;
        try {
            t = LocalDateTimeUtil.parse(jsonParser.getText());
        } catch (Exception e) {
        }
        if (t == null) {
            try {
                t = LocalDateTimeUtil.parse(jsonParser.getText(), "yyyy-MM-dd HH:mm:ss");
            } catch (Exception e) {
            }
        }
        if (t == null) {
            try {
                t = LocalDateTimeUtil.parse(jsonParser.getText(), "yyyy-MM-dd");
            } catch (Exception e) {
            }
        }
        if (t == null) {
            try {
                t = LocalDateTimeUtil.parse(jsonParser.getText(), "yyyy-MM");
            } catch (Exception e) {
            }
        }
        if (t == null) {
            try {
                t = LocalDateTimeUtil.parse(jsonParser.getText(), "yyyy");
            } catch (Exception e) {
            }
        }
        return t;
    }
}
