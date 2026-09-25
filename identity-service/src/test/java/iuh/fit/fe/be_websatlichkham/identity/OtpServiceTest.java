package iuh.fit.fe.be_websatlichkham.identity;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import iuh.fit.fe.be_websatlichkham.common.exception.BusinessException;
import iuh.fit.fe.be_websatlichkham.common.testsupport.MySqlTestcontainersConfiguration;
import iuh.fit.fe.be_websatlichkham.common.testsupport.RedisTestcontainersConfiguration;
import iuh.fit.fe.be_websatlichkham.identity.enums.OtpPurpose;
import iuh.fit.fe.be_websatlichkham.identity.service.OtpService;

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
        String code = otpService.generate(userId, OtpPurpose.REGISTER);

        assertThatThrownBy(() -> otpService.verify(userId, OtpPurpose.REGISTER, "wrong"))
                .isInstanceOf(BusinessException.class);
        assertThatCode(() -> otpService.verify(userId, OtpPurpose.REGISTER, code)).doesNotThrowAnyException();
        assertThatThrownBy(() -> otpService.verify(userId, OtpPurpose.REGISTER, code))
                .isInstanceOf(BusinessException.class);
    }

}
