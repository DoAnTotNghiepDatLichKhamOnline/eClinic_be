package iuh.fit.fe.be_websatlichkham.common.testsupport;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import iuh.fit.fe.be_websatlichkham.common.entity.booking.HoSoBenhNhan;
import iuh.fit.fe.be_websatlichkham.common.entity.catalog.BacSi;
import iuh.fit.fe.be_websatlichkham.common.entity.catalog.ChuyenKhoa;
import iuh.fit.fe.be_websatlichkham.common.entity.catalog.PhongKham;
import iuh.fit.fe.be_websatlichkham.common.entity.identity.QuanTriVien;
import iuh.fit.fe.be_websatlichkham.common.entity.identity.TaiKhoan;
import iuh.fit.fe.be_websatlichkham.common.entity.scheduling.KhungGioKham;
import iuh.fit.fe.be_websatlichkham.common.entity.scheduling.LichLamViec;
import iuh.fit.fe.be_websatlichkham.common.enums.VaiTro;
import jakarta.persistence.EntityManager;

/**
 * Dựng dữ liệu mẫu cho test tích hợp. Mỗi service chỉ có repository của mình nên fixture dùng EntityManager trực tiếp.
 */
public class TestFixtures {

    public static final LocalDate NGAY_LAM_VIEC = LocalDate.of(2030, 1, 15);

    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    public TestFixtures(EntityManager entityManager, PlatformTransactionManager transactionManager) {
        this.entityManager = entityManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public record CaLamViec(BacSi bacSi, PhongKham phongKham, QuanTriVien quanTriVien, LichLamViec lichLamViec,
            KhungGioKham khungGio) {
    }

    /**
     * Bác sĩ + phòng khám cùng chuyên khoa + 1 ca 08:00–11:00 ngày {@link #NGAY_LAM_VIEC} (6 BN)
     * + 1 khung giờ 08:00–08:30.
     */
    public CaLamViec taoCaLamViec() {
        return transactionTemplate.execute(status -> {
            String suffix = UUID.randomUUID().toString();

            ChuyenKhoa chuyenKhoa = new ChuyenKhoa();
            chuyenKhoa.setTenChuyenKhoa("Chuyên khoa " + suffix);
            entityManager.persist(chuyenKhoa);

            BacSi bacSi = new BacSi();
            bacSi.setTaiKhoan(taoTaiKhoan(VaiTro.BAC_SI, suffix));
            bacSi.setChuyenKhoa(chuyenKhoa);
            entityManager.persist(bacSi);

            PhongKham phongKham = new PhongKham();
            phongKham.setChuyenKhoa(chuyenKhoa);
            phongKham.setTenPhong("Phòng " + suffix);
            entityManager.persist(phongKham);

            QuanTriVien quanTriVien = new QuanTriVien();
            quanTriVien.setTaiKhoan(taoTaiKhoan(VaiTro.QUAN_TRI_VIEN, suffix));
            entityManager.persist(quanTriVien);

            LichLamViec lichLamViec = new LichLamViec();
            lichLamViec.setBacSi(bacSi);
            lichLamViec.setPhongKham(phongKham);
            lichLamViec.setAdminTao(quanTriVien);
            lichLamViec.setNgayLamViec(NGAY_LAM_VIEC);
            lichLamViec.setGioBatDau(LocalTime.of(8, 0));
            lichLamViec.setGioKetThuc(LocalTime.of(11, 0));
            lichLamViec.setSoBenhNhanToiDa(6);
            entityManager.persist(lichLamViec);

            KhungGioKham khungGio = new KhungGioKham();
            khungGio.setLichLamViec(lichLamViec);
            khungGio.setGioBatDau(NGAY_LAM_VIEC.atTime(8, 0));
            khungGio.setGioKetThuc(NGAY_LAM_VIEC.atTime(8, 30));
            entityManager.persist(khungGio);

            return new CaLamViec(bacSi, phongKham, quanTriVien, lichLamViec, khungGio);
        });
    }

    /** Hồ sơ bệnh nhân của Khách (chưa liên kết tài khoản). */
    public HoSoBenhNhan taoHoSoBenhNhan(String cccd) {
        HoSoBenhNhan hoSo = new HoSoBenhNhan();
        hoSo.setCccd(cccd);
        hoSo.setHoTen("Khách " + cccd);
        hoSo.setSoDienThoai("0900000000");
        return persist(hoSo);
    }

    /** Lưu entity trong 1 transaction riêng (flush ngay để lỗi ràng buộc DB ném ra tại đây). */
    public <T> T persist(T entity) {
        return transactionTemplate.execute(status -> {
            entityManager.persist(entity);
            entityManager.flush();
            return entity;
        });
    }

    private TaiKhoan taoTaiKhoan(VaiTro vaiTro, String suffix) {
        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setEmail(vaiTro.name().toLowerCase() + "-" + suffix + "@test.local");
        taiKhoan.setMatKhauHash("x");
        taiKhoan.setVaiTro(vaiTro);
        entityManager.persist(taiKhoan);
        return taiKhoan;
    }

    /** CCCD 12 chữ số ngẫu nhiên. */
    public static String cccdNgauNhien() {
        return String.format("%012d", Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), 1_000_000_000_000L));
    }

}
