package iuh.fit.se.eclinic.identity;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import iuh.fit.se.eclinic.identity.dulieumau.DuLieuMauService;
import iuh.fit.se.eclinic.identity.dulieumau.KhoiTaoDuLieuMauRunner;

/**
 * Tạo dữ liệu mẫu lỗi thì chỉ ghi log: identity-service vẫn phải khởi động được.
 */
class KhoiTaoDuLieuMauRunnerTest {

    private final DuLieuMauService duLieuMauService = mock(DuLieuMauService.class);
    private final KhoiTaoDuLieuMauRunner runner = new KhoiTaoDuLieuMauRunner(duLieuMauService);

    @Test
    void chayBinhThuongTaoDuLieuNenRoiBoSungLich() {
        when(duLieuMauService.taoDuLieuNen()).thenReturn(true);

        runner.run(null);

        verify(duLieuMauService).taoDuLieuNen();
        verify(duLieuMauService).boSungLichLamViec(LocalDate.now());
    }

    @Test
    void taoDuLieuNenLoiKhongNemRaNgoai() {
        when(duLieuMauService.taoDuLieuNen()).thenThrow(new DataIntegrityViolationException("trùng số điện thoại"));

        assertThatCode(() -> runner.run(null)).doesNotThrowAnyException();

        verify(duLieuMauService, never()).boSungLichLamViec(any());
    }

    @Test
    void boSungLichLoiKhongNemRaNgoai() {
        when(duLieuMauService.boSungLichLamViec(any())).thenThrow(new IllegalStateException("mất kết nối DB"));

        assertThatCode(() -> runner.run(null)).doesNotThrowAnyException();
    }

}
