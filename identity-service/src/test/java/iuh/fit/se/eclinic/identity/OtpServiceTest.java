package iuh.fit.se.eclinic.identity;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.testsupport.MySqlTestcontainersConfiguration;
import iuh.fit.se.eclinic.common.testsupport.RedisTestcontainersConfiguration;
import iuh.fit.se.eclinic.identity.enums.OtpPurpose;
import iuh.fit.se.eclinic.identity.service.OtpService;

/**
 * Context load thành công = Flyway chạy được V1__init_schema.sql và Hibernate validate khớp mọi entity.
 */
@SpringBootTest
@Import({ MySqlTestcontainersConfiguration.class, RedisTestcontainersConfiguration.class })
class OtpServiceTest {

    @Autowired OtpService otpService;

    @Test
    void otpCanBeVerifiedOnlyOnce() {
        long userId = System.nanoTime();
        String code = otpService.tao(userId, OtpPurpose.REGISTER);

        assertThatThrownBy(() -> otpService.xacThuc(userId, OtpPurpose.REGISTER, "wrong"))
                .isInstanceOf(LoiNghiepVu.class);
        assertThatCode(() -> otpService.xacThuc(userId, OtpPurpose.REGISTER, code)).doesNotThrowAnyException();
        assertThatThrownBy(() -> otpService.xacThuc(userId, OtpPurpose.REGISTER, code))
                .isInstanceOf(LoiNghiepVu.class);
    }

}
