package sunyu.util;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 自定义LocalDate反序列化器
 *
 * @author SunYu
 */
public class CustomLocalDateDeserializer extends JsonDeserializer<LocalDate> {
    @Override
    public LocalDate deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
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
        if (t != null) {
            return t.toLocalDate();
        }
        return null;
    }
}
