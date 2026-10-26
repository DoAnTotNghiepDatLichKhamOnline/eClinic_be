package iuh.fit.se.eclinic.report.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.catalog.ChuyenKhoa;

/** Chỉ đọc: bác sĩ, chuyên khoa (catalog-service) và tài khoản (identity-service) cho báo cáo. */
public interface DanhMucChiDocRepository extends Repository<BacSi, Long> {

    interface BacSiTomTat {

        Long getId();

        String getHoTen();

        Long getIdChuyenKhoa();
    }

    @Query("""
            select b.id as id, t.hoTen as hoTen, b.chuyenKhoa.id as idChuyenKhoa from BacSi b join b.taiKhoan t
            where b.id in :idCacBacSi
            """)
    List<BacSiTomTat> timBacSi(Collection<Long> idCacBacSi);

    @Query("select ck from ChuyenKhoa ck order by ck.tenChuyenKhoa")
    List<ChuyenKhoa> timChuyenKhoa();

    @Query("select ck from ChuyenKhoa ck where ck.id = :id")
    Optional<ChuyenKhoa> timChuyenKhoa(Long id);

    /** Bác sĩ đang công tác có tài khoản đã kích hoạt, cùng nghĩa với ô trên Dashboard quản trị viên. */
    @Query("""
            select count(b) from BacSi b
            where b.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiBacSi.DANG_CONG_TAC
              and b.taiKhoan.trangThai = iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan.DA_KICH_HOAT
              and (:idChuyenKhoa is null or b.chuyenKhoa.id = :idChuyenKhoa)
            """)
    long demBacSiDangCongTac(Long idChuyenKhoa);

    /** Tài khoản bệnh nhân tạo trong {@code [tu, den)} đã xác thực email (đang dùng hoặc đã bị vô hiệu hoá sau đó). */
    @Query("""
            select count(t) from TaiKhoan t
            where t.vaiTro = iuh.fit.se.eclinic.common.enums.VaiTro.BENH_NHAN
              and t.trangThai <> iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan.CHO_XAC_NHAN
              and t.ngayTao >= :tu and t.ngayTao < :den
            """)
    long demTaiKhoanBenhNhanMoi(LocalDateTime tu, LocalDateTime den);

}
