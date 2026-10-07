package hu.ogyh.voting.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.spi.nodenameprovider.JavaBeanProperty;
import org.hibernate.validator.spi.nodenameprovider.Property;
import org.hibernate.validator.spi.nodenameprovider.PropertyNodeNameProvider;
import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** A validációs hibák mezőnevei a JSON-neveket követik ({@code szavazatok[0].kepviselo}), nem a Java-neveket. */
@Configuration(proxyBeanMethods = false)
public class ValidationConfig {

    @Bean
    ValidationConfigurationCustomizer jsonPropertyNodeNames() {
        return configuration -> {
            if (configuration instanceof HibernateValidatorConfiguration hibernateConfiguration) {
                hibernateConfiguration.propertyNodeNameProvider(new JsonPropertyNodeNameProvider());
            }
        };
    }

    /**
     * A mező {@link JsonProperty} neve; ha nincs, a Java-név. Rekordnál a mezőről kell olvasni, mert a
     * {@code @JsonProperty} a rekordkomponensről csak a mezőre, az accessorra és a paraméterre öröklődik.
     */
    static final class JsonPropertyNodeNameProvider implements PropertyNodeNameProvider {

        @Override
        public String getName(Property property) {
            if (property instanceof JavaBeanProperty beanProperty) {
                try {
                    JsonProperty jsonProperty = beanProperty
                            .getDeclaringClass()
                            .getDeclaredField(property.getName())
                            .getAnnotation(JsonProperty.class);
                    if (jsonProperty != null && !jsonProperty.value().isEmpty()) {
                        return jsonProperty.value();
                    }
                } catch (NoSuchFieldException ignored) {
                    // getter mögötti mező nélküli tulajdonság: marad a Java-név
                }
            }
            return property.getName();
        }
    }
}
