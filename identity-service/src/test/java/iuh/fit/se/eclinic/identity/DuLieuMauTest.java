package iuh.fit.se.eclinic.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiKhungGio;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.testsupport.MySqlTestcontainersConfiguration;
import iuh.fit.se.eclinic.common.testsupport.RedisTestcontainersConfiguration;
import iuh.fit.se.eclinic.identity.dulieumau.DuLieuMauService;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * Khởi động identity-service với app.du-lieu-mau.bat = true trên DB trống (MySQL + Redis thật): dữ liệu mẫu được tạo
 * đúng 1 lần, ca làm việc chỉ phủ 14 ngày tới và được bổ sung dần.
 * <p>
 * Các test dùng chung 1 DB và test sau thêm ca / đổi trạng thái, nên phải chạy theo thứ tự.
 */
@SpringBootTest(properties = "app.du-lieu-mau.bat=true")
@Import({ MySqlTestcontainersConfiguration.class, RedisTestcontainersConfiguration.class })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DuLieuMauTest {

    /** 14 ngày liền = mỗi thứ 2 lần: 8 bác sĩ nhóm CHINH làm 6 ca / tuần, 4 bác sĩ nhóm PHU làm 5 ca / tuần. */
    private static final long SO_CA_14_NGAY = 8 * 12 + 4 * 10;
    private static final long SO_CA_7_NGAY = 8 * 6 + 4 * 5;
    private static final long SO_KHUNG_MOI_CA = 7;

    @Autowired DuLieuMauService duLieuMauService;
    @Autowired TaiKhoanRepository taiKhoanRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;

    private final LocalDate homNay = LocalDate.now();

    @Test
    @Order(1)
    void khoiDongTaoDuLieuNen() {
        assertThat(dem("select count(c) from ChuyenKhoa c")).isEqualTo(8);
        assertThat(dem("select count(p) from PhongKham p")).isEqualTo(8);
        assertThat(dem("select count(b) from BacSi b")).isEqualTo(12);
        assertThat(dem("select count(t) from TaiKhoan t where t.vaiTro = ?1 and t.trangThai = ?2", VaiTro.BAC_SI,
                TrangThaiTaiKhoan.DA_KICH_HOAT)).isEqualTo(12);
        assertThat(dem("select count(b) from BacSi b where b.chuyenKhoa.tenChuyenKhoa = ?1", "Nhi khoa")).isEqualTo(2);
        assertThat(dem("select count(b) from BacSi b where b.chuyenKhoa.tenChuyenKhoa = ?1", "Tim mạch")).isEqualTo(1);
        assertThat(dem("select count(t) from TaiKhoan t where t.vaiTro = ?1", VaiTro.BENH_NHAN)).isEqualTo(6);
        assertThat(dem("select count(h) from HoSoBenhNhan h where h.taiKhoan is not null and h.trangThaiLienKet = ?1",
                TrangThaiLienKet.DA_LIEN_KET)).isEqualTo(4);
        assertThat(dem("select count(h) from HoSoBenhNhan h")).isEqualTo(4);

        TaiKhoan bacSi = taiKhoanRepository.findByEmail("bacsi01@eclinic.local").orElseThrow();
        assertThat(passwordEncoder.matches("Demo@123", bacSi.getMatKhauHash())).isTrue();
        assertThat(bacSi.getSoDienThoai()).isEqualTo("0901000001");

        assertThat(taiKhoanRepository.findByEmail("benhnhan01@eclinic.local").orElseThrow().getTrangThai())
                .isEqualTo(TrangThaiTaiKhoan.DA_KICH_HOAT);
        assertThat(taiKhoanRepository.findByEmail("benhnhan05@eclinic.local").orElseThrow().getTrangThai())
                .isEqualTo(TrangThaiTaiKhoan.CHO_XAC_NHAN);
        TaiKhoan biVoHieuHoa = taiKhoanRepository.findByEmail("benhnhan06@eclinic.local").orElseThrow();
        assertThat(biVoHieuHoa.getTrangThai()).isEqualTo(TrangThaiTaiKhoan.VO_HIEU_HOA);
        assertThat(biVoHieuHoa.getLyDoVoHieuHoa()).isNotBlank();
    }

    @Test
    @Order(2)
    void khoiDongTaoCaLamViecCho14NgayToi() {
        assertThat(dem("select count(l) from LichLamViec l")).isEqualTo(SO_CA_14_NGAY);
        assertThat(dem("select count(l) from LichLamViec l where l.ngayLamViec < ?1 or l.ngayLamViec > ?2", homNay,
                homNay.plusDays(13))).isZero();
        // 14 ngày có 2 chủ nhật, không ai làm chủ nhật
        List<LocalDate> cacNgay = entityManager
                .createQuery("select distinct l.ngayLamViec from LichLamViec l", LocalDate.class).getResultList();
        assertThat(cacNgay).hasSize(12).noneMatch(ngay -> ngay.getDayOfWeek() == DayOfWeek.SUNDAY);

        // Mỗi ca đúng 7 khung giờ còn trống
        assertThat(dem("select count(l) from LichLamViec l where l.soBenhNhanToiDa <> ?1 or l.trangThai <> ?2",
                (int) SO_KHUNG_MOI_CA, TrangThaiLichLamViec.HOAT_DONG)).isZero();
        assertThat(dem("select count(k) from KhungGioKham k")).isEqualTo(SO_CA_14_NGAY * SO_KHUNG_MOI_CA);
        assertThat(dem("select count(k) from KhungGioKham k where k.trangThai <> ?1", TrangThaiKhungGio.CON_TRONG)).isZero();
        assertThat(entityManager.createQuery("select count(k) from KhungGioKham k group by k.lichLamViec.id", Long.class)
                .getResultList()).hasSize((int) SO_CA_14_NGAY).containsOnly(SO_KHUNG_MOI_CA);

        // Không trùng giờ theo phòng, mỗi bác sĩ tối đa 1 ca / ngày
        assertThat(dem("""
                select count(a) from LichLamViec a, LichLamViec b
                where a.id < b.id and a.phongKham.id = b.phongKham.id and a.ngayLamViec = b.ngayLamViec
                  and a.gioBatDau < b.gioKetThuc and a.gioKetThuc > b.gioBatDau
                """)).isZero();
        assertThat(entityManager.createQuery("select count(l) from LichLamViec l group by l.bacSi.id, l.ngayLamViec",
                Long.class).getResultList()).containsOnly(1L);

        // Khung giờ 30 phút phủ kín ca sáng 08:00–11:30
        Object[] caSang = entityManager.createQuery("""
                select l.id, l.ngayLamViec from LichLamViec l where l.gioBatDau = ?1 order by l.id
                """, Object[].class).setParameter(1, LocalTime.of(8, 0)).setMaxResults(1).getSingleResult();
        LocalDate ngay = (LocalDate) caSang[1];
        List<LocalDateTime> gioBatDau = entityManager.createQuery(
                "select k.gioBatDau from KhungGioKham k where k.lichLamViec.id = ?1 order by k.gioBatDau",
                LocalDateTime.class).setParameter(1, caSang[0]).getResultList();
        assertThat(gioBatDau).first().isEqualTo(ngay.atTime(8, 0));
        assertThat(gioBatDau).last().isEqualTo(ngay.atTime(11, 0));
        assertThat(dem("select count(k) from KhungGioKham k where k.lichLamViec.id = ?1 and k.gioKetThuc = ?2", caSang[0],
                ngay.atTime(11, 30))).isEqualTo(1);
    }

    @Test
    @Order(3)
    void chayLaiKhongTaoThem() {
        long soTaiKhoan = dem("select count(t) from TaiKhoan t");

        assertThat(duLieuMauService.taoDuLieuNen()).isFalse();
        assertThat(duLieuMauService.boSungLichLamViec(homNay)).isZero();

        assertThat(dem("select count(t) from TaiKhoan t")).isEqualTo(soTaiKhoan);
        assertThat(dem("select count(c) from ChuyenKhoa c")).isEqualTo(8);
        assertThat(dem("select count(l) from LichLamViec l")).isEqualTo(SO_CA_14_NGAY);
        assertThat(dem("select count(k) from KhungGioKham k")).isEqualTo(SO_CA_14_NGAY * SO_KHUNG_MOI_CA);
    }

    @Test
    @Order(4)
    void boSungTuNgayKhacChiTaoCaChoNgayMoi() {
        // Như 7 ngày sau chạy lại: 7 ngày đầu của cửa sổ đã có ca, chỉ 7 ngày cuối được tạo
        int soCa = duLieuMauService.boSungLichLamViec(homNay.plusDays(7));

        assertThat(soCa).isEqualTo((int) SO_CA_7_NGAY);
        assertThat(dem("select count(l) from LichLamViec l where l.ngayLamViec > ?1", homNay.plusDays(13)))
                .isEqualTo(SO_CA_7_NGAY);
        assertThat(dem("select count(l) from LichLamViec l where l.ngayLamViec > ?1", homNay.plusDays(20))).isZero();
        assertThat(dem("select count(l) from LichLamViec l")).isEqualTo(SO_CA_14_NGAY + SO_CA_7_NGAY);
    }

    @Test
    @Order(5)
    void caDaHuyKhongDuocTaoLai() {
        Object[] ca = entityManager.createQuery("select l.id, l.bacSi.id, l.ngayLamViec from LichLamViec l order by l.id",
                Object[].class).setMaxResults(1).getSingleResult();
        capNhat("update LichLamViec l set l.trangThai = ?1 where l.id = ?2", TrangThaiLichLamViec.DA_HUY, ca[0]);

        assertThat(duLieuMauService.boSungLichLamViec(homNay)).isZero();

        assertThat(dem("select count(l) from LichLamViec l where l.bacSi.id = ?1 and l.ngayLamViec = ?2", ca[1], ca[2]))
                .isEqualTo(1);
    }

    @Test
    @Order(6)
    void bacSiBiVoHieuHoaKhongDuocXepThemCa() {
        capNhat("update TaiKhoan t set t.trangThai = ?1 where t.email = ?2", TrangThaiTaiKhoan.VO_HIEU_HOA,
                "bacsi12@eclinic.local");
        LocalDate tuNgay = homNay.plusDays(28);

        int soCa = duLieuMauService.boSungLichLamViec(tuNgay);

        // bacsi12 thuộc nhóm CHINH: 12 ca / 14 ngày
        assertThat(soCa).isEqualTo((int) SO_CA_14_NGAY - 12);
        assertThat(dem("select count(l) from LichLamViec l where l.bacSi.taiKhoan.email = ?1 and l.ngayLamViec >= ?2",
                "bacsi12@eclinic.local", tuNgay)).isZero();
    }

    private long dem(String jpql, Object... thamSo) {
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        for (int i = 0; i < thamSo.length; i++) {
            query.setParameter(i + 1, thamSo[i]);
        }
        return query.getSingleResult();
    }

    private void capNhat(String jpql, Object... thamSo) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var query = entityManager.createQuery(jpql);
            for (int i = 0; i < thamSo.length; i++) {
                query.setParameter(i + 1, thamSo[i]);
            }
            query.executeUpdate();
        });
    }

}
