package iuh.fit.se.eclinic.booking;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

import iuh.fit.se.eclinic.booking.service.LichLamViecService;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.testsupport.MySqlTestcontainersConfiguration;
import iuh.fit.se.eclinic.common.testsupport.TestFixtures.CaLamViec;
import iuh.fit.se.eclinic.common.testsupport.TestFixtures;
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

        assertThatThrownBy(() -> lichLamViecService.kiemTraKhongTrungLich(bacSiId, phongKhamId, TestFixtures.NGAY_LAM_VIEC,
                LocalTime.of(9, 0), LocalTime.of(10, 0), null))
                .isInstanceOf(LoiNghiepVu.class)
                .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                .isEqualTo(MaLoi.TRUNG_LICH_LAM_VIEC);

        assertThatCode(() -> lichLamViecService.kiemTraKhongTrungLich(bacSiId, phongKhamId, TestFixtures.NGAY_LAM_VIEC,
                LocalTime.of(12, 0), LocalTime.of(13, 0), null)).doesNotThrowAnyException();
    }

}
