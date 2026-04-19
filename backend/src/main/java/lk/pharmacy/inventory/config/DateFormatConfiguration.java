package lk.pharmacy.inventory.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Configuration for global date format standardization.
 * All LocalDate instances are serialized/deserialized in dd-mm-yyyy format.
 */
@Configuration
public class DateFormatConfiguration {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /**
     * Custom JsonSerializer for LocalDate to dd-mm-yyyy format
     */
    public static class LocalDateSerializer extends JsonSerializer<LocalDate> {
        @Override
        public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeString(value.format(DATE_FORMATTER));
            }
        }
    }

    /**
     * Custom JsonDeserializer for parsing dd-mm-yyyy format strings to LocalDate
     */
    public static class LocalDateDeserializer extends JsonDeserializer<LocalDate> {
        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            String value = p.getValueAsString();
            if (value == null || value.trim().isEmpty()) {
                return null;
            }

            try {
                // Try parsing with dd-mm-yyyy format (user input format)
                return LocalDate.parse(value, DATE_FORMATTER);
            } catch (Exception e) {
                try {
                    // Fallback: try ISO format (YYYY-MM-DD) from HTML date inputs
                    return LocalDate.parse(value);
                } catch (Exception e2) {
                    throw new IllegalArgumentException(
                            "Invalid date format. Expected format: dd-mm-yyyy or YYYY-MM-DD, got: " + value, e2);
                }
            }
        }
    }

    /**
     * Register custom serializer and deserializer for LocalDate
     */
    @Bean
    public SimpleModule dateFormatModule() {
        SimpleModule module = new SimpleModule("DateFormatModule");
        module.addSerializer(LocalDate.class, new LocalDateSerializer());
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer());
        return module;
    }
}

