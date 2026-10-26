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

/**
 * Cache trên Redis cho danh sách ngày bệnh viện nghỉ công khai (NgayNghiServiceImpl#congKhai, lịch của trang đặt lịch).
 * Khoá bắt đầu bằng ngôn ngữ của request vì lý do nghỉ theo ngôn ngữ.
 * <ul>
 * <li>Chỉ NgayNghiServiceImpl ghi bảng ngay_nghi_benh_vien: khai báo (khi lưu) và xoá ngày nghỉ xoá hết cache SAU khi
 * commit (transactionAware). Hết hạn sau 10 phút chỉ để phòng hờ.</li>
 * <li>Giá trị lưu dạng JSON thường (không kèm tên class).</li>
 * <li>Redis lỗi: ghi log cảnh báo rồi đọc thẳng DB. Mất kết nối thì Lettuce từ chối lệnh ngay, không chờ timeout (áp
 * dụng cho cả các bộ đếm giới hạn GioiHan*Service, vốn đã bỏ qua giới hạn khi Redis lỗi).</li>
 * </ul>
 */
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
