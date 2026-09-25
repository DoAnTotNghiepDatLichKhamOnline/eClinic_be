package iuh.fit.fe.be_websatlichkham.scheduling;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

import iuh.fit.fe.be_websatlichkham.common.exception.BusinessException;
import iuh.fit.fe.be_websatlichkham.common.exception.ErrorCode;
import iuh.fit.fe.be_websatlichkham.common.testsupport.MySqlTestcontainersConfiguration;
import iuh.fit.fe.be_websatlichkham.common.testsupport.TestFixtures;
import iuh.fit.fe.be_websatlichkham.common.testsupport.TestFixtures.CaLamViec;
import iuh.fit.fe.be_websatlichkham.scheduling.service.LichLamViecService;
import jakarta.persistence.EntityManager;

@SpringBootTest
@Import(MySqlTestcontainersConfiguration.class)
class LichLamViecServiceTest {

    @Autowired LichLamViecService lichLamViecService;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void overlappingWorkScheduleIsRejected() {
        CaLamViec ca = new TestFixtures(entityManager, transactionManager).taoCaLamViec();
        Long bacSiId = ca.bacSi().getId();
        Long phongKhamId = ca.phongKham().getId();

        assertThatThrownBy(() -> lichLamViecService.assertNoOverlap(bacSiId, phongKhamId, TestFixtures.NGAY_LAM_VIEC,
                LocalTime.of(9, 0), LocalTime.of(10, 0), null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SCHEDULE_OVERLAP);

        assertThatCode(() -> lichLamViecService.assertNoOverlap(bacSiId, phongKhamId, TestFixtures.NGAY_LAM_VIEC,
                LocalTime.of(12, 0), LocalTime.of(13, 0), null)).doesNotThrowAnyException();
    }

}
