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
 * Cómo escribe y lee JSON la API REST (JSON es el texto con llaves que se mandan navegador y servidor,
 * y la API REST son las rutas HTTP que el navegador llama para traer datos).
 * Lo único que se cambia del comportamiento que trae Javalin por defecto son las fechas: salen como
 * "AAAA-MM-DD", el formato que usan los campos de fecha de los formularios HTML. Sin esto Java escribiría
 * un número de milisegundos y el formulario no sabría dónde ponerlo.
 */
public final class MapeadorJson implements JsonMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(
            new SimpleModule()
                    // Una fecha de la base de datos se escribe como texto AAAA-MM-DD.
                    .addSerializer(Date.class, new JsonSerializer<Date>() {
                        @Override
                        public void serialize(Date valor, JsonGenerator g, SerializerProvider p) throws java.io.IOException {
                            g.writeString(valor.toString());
                        }
                    })
                    // Y al revés: el texto del formulario se convierte en fecha. Si no tiene el formato,
                    // se avisa; la ruta que lee el cuerpo responde entonces un 400.
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

    /** Objeto de Java -> texto JSON, para mandar la respuesta al navegador. */
    @Override
    public String toJsonString(Object obj, Type type) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /** Texto JSON -> objeto de Java, para leer el cuerpo que llegó en la petición. */
    @Override
    public <T> T fromJsonString(String json, Type type) {
        try {
            return MAPPER.readValue(json, MAPPER.constructType(type));
        } catch (java.io.IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /** Lo mismo, pero cuando el JSON llega como bytes aún sin descifrar; aquí se pasan a texto con UTF-8. */
    @Override
    public <T> T fromJsonStream(InputStream json, Type type) {
        try {
            return MAPPER.readValue(new String(json.readAllBytes(), StandardCharsets.UTF_8), MAPPER.constructType(type));
        } catch (java.io.IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
