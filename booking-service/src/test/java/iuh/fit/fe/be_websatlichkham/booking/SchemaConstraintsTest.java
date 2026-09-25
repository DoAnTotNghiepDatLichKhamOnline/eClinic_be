package iuh.fit.fe.be_websatlichkham.booking;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

import iuh.fit.fe.be_websatlichkham.booking.repository.HoSoBenhNhanRepository;
import iuh.fit.fe.be_websatlichkham.booking.repository.LichHenRepository;
import iuh.fit.fe.be_websatlichkham.common.entity.booking.HoSoBenhNhan;
import iuh.fit.fe.be_websatlichkham.common.entity.booking.LichHen;
import iuh.fit.fe.be_websatlichkham.common.entity.scheduling.YeuCauDoiLich;
import iuh.fit.fe.be_websatlichkham.common.enums.LoaiYeuCauDoiLich;
import iuh.fit.fe.be_websatlichkham.common.enums.TrangThaiLichHen;
import iuh.fit.fe.be_websatlichkham.common.enums.TrangThaiLienKet;
import iuh.fit.fe.be_websatlichkham.common.testsupport.MySqlTestcontainersConfiguration;
import iuh.fit.fe.be_websatlichkham.common.testsupport.TestFixtures;
import iuh.fit.fe.be_websatlichkham.common.testsupport.TestFixtures.CaLamViec;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

/**
 * Kiểm tra các ràng buộc toàn vẹn dữ liệu đặt ở mức DB (V1__init_schema.sql).
 * Context load thành công cũng có nghĩa là Flyway chạy được V1 và Hibernate validate khớp mọi entity.
 */
@SpringBootTest
@Import(MySqlTestcontainersConfiguration.class)
class SchemaConstraintsTest {

    @Autowired LichHenRepository lichHenRepository;
    @Autowired HoSoBenhNhanRepository hoSoBenhNhanRepository;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;

    TestFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new TestFixtures(entityManager, transactionManager);
    }

    @Test
    void activeAppointmentsCannotShareASlot_butCancelledSlotCanBeRebooked() {
        CaLamViec ca = fixtures.taoCaLamViec();
        HoSoBenhNhan hoSo = fixtures.taoHoSoBenhNhan(TestFixtures.cccdNgauNhien());

        LichHen first = lichHenRepository.saveAndFlush(newLichHen(ca, hoSo, 1));

        assertThatThrownBy(() -> lichHenRepository.saveAndFlush(newLichHen(ca, hoSo, 2)))
                .isInstanceOf(DataIntegrityViolationException.class);

        first.setTrangThai(TrangThaiLichHen.DA_HUY);
        lichHenRepository.saveAndFlush(first);

        assertThatCode(() -> lichHenRepository.saveAndFlush(newLichHen(ca, hoSo, 3))).doesNotThrowAnyException();
    }

    @Test
    void cccdIsUnique() {
        String cccd = TestFixtures.cccdNgauNhien();
        fixtures.taoHoSoBenhNhan(cccd);

        assertThatThrownBy(() -> fixtures.taoHoSoBenhNhan(cccd))
                .hasRootCauseInstanceOf(SQLIntegrityConstraintViolationException.class);
    }

    @Test
    void linkStatusMustMatchAccount() {
        HoSoBenhNhan hoSo = new HoSoBenhNhan();
        hoSo.setCccd(TestFixtures.cccdNgauNhien());
        hoSo.setHoTen("Khách");
        hoSo.setSoDienThoai("0900000000");
        hoSo.setTrangThaiLienKet(TrangThaiLienKet.DA_LIEN_KET); // nhưng không gắn tài khoản

        assertThatThrownBy(() -> hoSoBenhNhanRepository.saveAndFlush(hoSo)).isInstanceOf(DataAccessException.class);
    }

    @Test
    void workScheduleHasAtMostOnePendingChangeRequest() {
        CaLamViec ca = fixtures.taoCaLamViec();

        fixtures.persist(newYeuCauXinNghi(ca));

        assertThatThrownBy(() -> fixtures.persist(newYeuCauXinNghi(ca)))
                .hasRootCauseInstanceOf(SQLIntegrityConstraintViolationException.class);
    }

    @Test
    void shiftChangeRequestRequiresDesiredTime() {
        CaLamViec ca = fixtures.taoCaLamViec();
        YeuCauDoiLich doiCaThieuGio = newYeuCauXinNghi(ca);
        doiCaThieuGio.setLoaiYeuCau(LoaiYeuCauDoiLich.DOI_CA);

        assertThatThrownBy(() -> fixtures.persist(doiCaThieuGio)).isInstanceOf(PersistenceException.class);

        YeuCauDoiLich doiCa = newYeuCauXinNghi(ca);
        doiCa.setLoaiYeuCau(LoaiYeuCauDoiLich.DOI_CA);
        doiCa.setNgayMongMuon(TestFixtures.NGAY_LAM_VIEC.plusDays(1));
        doiCa.setGioBatDauMongMuon(LocalTime.of(13, 0));
        doiCa.setGioKetThucMongMuon(LocalTime.of(16, 0));

        assertThatCode(() -> fixtures.persist(doiCa)).doesNotThrowAnyException();
    }

    // ---------------------------------------------------------------- helpers

    private static LichHen newLichHen(CaLamViec ca, HoSoBenhNhan hoSo, int soThuTu) {
        LichHen lichHen = new LichHen();
        lichHen.setHoSoBenhNhan(hoSo);
        lichHen.setBacSi(ca.bacSi());
        lichHen.setKhungGio(ca.khungGio());
        lichHen.setPhongKham(ca.phongKham());
        lichHen.setSoThuTu(soThuTu);
        lichHen.setMaTokenPhieuKham(UUID.randomUUID().toString());
        return lichHen;
    }

    private static YeuCauDoiLich newYeuCauXinNghi(CaLamViec ca) {
        YeuCauDoiLich yeuCau = new YeuCauDoiLich();
        yeuCau.setBacSi(ca.bacSi());
        yeuCau.setLichLamViec(ca.lichLamViec());
        yeuCau.setLoaiYeuCau(LoaiYeuCauDoiLich.XIN_NGHI);
        yeuCau.setLyDo("Bận việc gia đình");
        return yeuCau;
    }

}
