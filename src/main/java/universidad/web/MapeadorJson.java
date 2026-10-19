package universidad.web;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.javalin.json.JsonMapper;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.sql.Date;

/**
 * Serializa las fechas como "AAAA-MM-DD" (los formularios HTML usan input type=date)
 * en lugar del epoch que Jackson produce por defecto para java.sql.Date.
 */
public final class MapeadorJson implements JsonMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(
            new SimpleModule()
                    .addSerializer(Date.class, new JsonSerializer<Date>() {
                        @Override
                        public void serialize(Date valor, JsonGenerator g, SerializerProvider p) throws java.io.IOException {
                            g.writeString(valor.toString());
                        }
                    })
                    .addDeserializer(Date.class, new JsonDeserializer<Date>() {
                        @Override
                        public Date deserialize(JsonParser p, DeserializationContext c) throws java.io.IOException {
                            String texto = p.getText();
                            if (texto == null || texto.isBlank()) {
                                return null;
                            }
                            try {
                                return Date.valueOf(texto.trim());
                            } catch (IllegalArgumentException ex) {
                                throw c.weirdStringException(texto, Date.class,
                                        "la fecha debe tener el formato AAAA-MM-DD");
                            }
                        }
                    }));

    @Override
    public String toJsonString(Object obj, Type type) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public <T> T fromJsonString(String json, Type type) {
        try {
            return MAPPER.readValue(json, MAPPER.constructType(type));
        } catch (java.io.IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public <T> T fromJsonStream(InputStream json, Type type) {
        try {
            return MAPPER.readValue(new String(json.readAllBytes(), StandardCharsets.UTF_8), MAPPER.constructType(type));
        } catch (java.io.IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
