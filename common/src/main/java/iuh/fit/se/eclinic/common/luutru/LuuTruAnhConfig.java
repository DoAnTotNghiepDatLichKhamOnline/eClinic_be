package iuh.fit.se.eclinic.common.luutru;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Kho ảnh chỉ có ở service đặt {@code app.cloudinary.bat=true} (identity-service, catalog-service). */
@Configuration
@ConditionalOnProperty(prefix = "app.cloudinary", name = "bat", havingValue = "true")
@EnableConfigurationProperties(CloudinaryProperties.class)
public class LuuTruAnhConfig {

    @Bean
    LuuTruAnh luuTruAnh(CloudinaryProperties properties) {
        return new LuuTruAnhCloudinary(properties);
    }

}
