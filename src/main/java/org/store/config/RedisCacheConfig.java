package org.store.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Configuration du cache Redis. Active {@link EnableCaching} et fournit un {@link RedisCacheManager}
 * avec sérialisation JSON Jackson (gère LocalDate/LocalDateTime via JavaTimeModule, default typing
 * activé pour permettre la désérialisation polymorphe des collections imbriquées). TTL par défaut
 * 10 min ; named caches surchargent ce défaut (ex. "public-catalog" 5 min).
 * <p>Désactivé en test via {@code spring.cache.type=none} (system property dans le plugin surefire).
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class RedisCacheConfig {

    public static final String PUBLIC_CATALOG = "public-catalog";
    public static final String CATEGORIES_PRODUCT_BY_ENTREPRISE = "categories-product-by-entreprise";
    public static final String QUALITIES_BY_ENTREPRISE = "qualities-by-entreprise";
    public static final String FOURNISSEURS_BY_ENTREPRISE = "fournisseurs-by-entreprise";
    public static final String EXPENSE_CATEGORIES_BY_ENTREPRISE = "expense-categories-by-entreprise";

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = buildBaseConfig().entryTtl(Duration.ofMinutes(10));

        Map<String, RedisCacheConfiguration> namedCaches = Map.of(
                PUBLIC_CATALOG, buildBaseConfig().entryTtl(Duration.ofMinutes(5)),
                CATEGORIES_PRODUCT_BY_ENTREPRISE, buildBaseConfig().entryTtl(Duration.ofHours(1)),
                QUALITIES_BY_ENTREPRISE, buildBaseConfig().entryTtl(Duration.ofHours(1)),
                FOURNISSEURS_BY_ENTREPRISE, buildBaseConfig().entryTtl(Duration.ofMinutes(30)),
                EXPENSE_CATEGORIES_BY_ENTREPRISE, buildBaseConfig().entryTtl(Duration.ofHours(1))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(namedCaches)
                .build();
    }

    private RedisCacheConfiguration buildBaseConfig() {
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Object.class)
                .build();

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL);

        GenericJackson2JsonRedisSerializer jsonSerializer = GenericJackson2JsonRedisSerializer.builder()
                .objectMapper(objectMapper)
                .build();

        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();
    }
}
