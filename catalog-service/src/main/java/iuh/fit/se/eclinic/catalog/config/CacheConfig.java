package iuh.fit.se.eclinic.catalog.config;

import java.time.Duration;

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
import iuh.fit.se.eclinic.catalog.dto.response.ChuyenKhoaResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

/** Cache trên Redis cho danh mục chuyên khoa công khai: danh sách không từ khoá và chi tiết. */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    public static final String CHUYEN_KHOA = "catalog:chuyen-khoa";
    public static final String CHUYEN_KHOA_CHI_TIET = "catalog:chuyen-khoa-chi-tiet";

    private static final Duration THOI_HAN = Duration.ofMinutes(10);

    @Bean
    RedisCacheManagerBuilderCustomizer cacheChuyenKhoa(JsonMapper jsonMapper) {
        JavaType trangChuyenKhoa = jsonMapper.getTypeFactory()
                .constructParametricType(TrangDuLieu.class, ChuyenKhoaResponse.class);
        return builder -> builder.transactionAware()
                .withCacheConfiguration(CHUYEN_KHOA, cauHinh(jsonMapper, trangChuyenKhoa))
                .withCacheConfiguration(CHUYEN_KHOA_CHI_TIET,
                        cauHinh(jsonMapper, jsonMapper.getTypeFactory().constructType(ChuyenKhoaResponse.class)));
    }

    private static RedisCacheConfiguration cauHinh(JsonMapper jsonMapper, JavaType kieu) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(THOI_HAN)
                .disableCachingNullValues()
                .serializeValuesWith(SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(jsonMapper, kieu)));
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
