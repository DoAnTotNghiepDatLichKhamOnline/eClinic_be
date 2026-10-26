package iuh.fit.se.eclinic.booking.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.data.redis.autoconfigure.LettuceClientOptionsBuilderCustomizer;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import io.lettuce.core.ClientOptions.DisconnectedBehavior;
import iuh.fit.se.eclinic.booking.dto.response.NgayNghiResponse;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

/** Cache trên Redis cho danh sách ngày bệnh viện nghỉ công khai. */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    public static final String NGAY_NGHI = "booking:ngay-nghi";

    private static final Duration THOI_HAN = Duration.ofMinutes(10);

    @Bean
    RedisCacheManagerBuilderCustomizer cacheNgayNghi(JsonMapper jsonMapper) {
        JavaType danhSachNgayNghi = jsonMapper.getTypeFactory()
                .constructCollectionType(List.class, NgayNghiResponse.class);
        return builder -> builder.transactionAware()
                .withCacheConfiguration(NGAY_NGHI, RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(THOI_HAN)
                        .disableCachingNullValues()
                        .serializeValuesWith(SerializationPair.fromSerializer(
                                new JacksonJsonRedisSerializer<>(jsonMapper, danhSachNgayNghi))));
    }

    @Bean
    LettuceClientOptionsBuilderCustomizer tuChoiKhiMatKetNoi() {
        return options -> options.disconnectedBehavior(DisconnectedBehavior.REJECT_COMMANDS);
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }

}
