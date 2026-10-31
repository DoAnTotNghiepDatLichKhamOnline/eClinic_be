package iuh.fit.se.eclinic.booking.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;

/** Người giám hộ là các cột giam_ho_* của lịch hẹn; 1 hồ sơ + 1 CCCD người giám hộ = 1 nhóm lịch hẹn cùng thông tin. */
public interface NguoiGiamHoRepository extends Repository<LichHen, Long> {

    /** Lịch hẹn sớm nhất khai người giám hộ này cho hồ sơ này: thông tin người giám hộ đã lưu để dùng lại. */
    Optional<LichHen> findFirstByHoSoBenhNhanIdAndNguoiGiamHoCccdOrderByIdAsc(Long hoSoBenhNhanId, String cccd);

    /** Lịch hẹn sớm nhất có người giám hộ của hồ sơ: CCCD người này nằm trong khoá nhận diện của hồ sơ chưa có CCCD. */
    Optional<LichHen> findFirstByHoSoBenhNhanIdAndNguoiGiamHoCccdNotNullOrderByIdAsc(Long hoSoBenhNhanId);

    /**
     * Điền ngày sinh người giám hộ còn trống cho cả nhóm lịch hẹn (hồ sơ, CCCD). Không tăng version, không đổi
     * ngay_cap_nhat; không xoá persistence context vì lần đặt đang chạy còn giữ lượt khám / hồ sơ chưa flush.
     */
    @Modifying
    @Query(value = """
            update lich_hen
            set giam_ho_ngay_sinh = :ngaySinh
            where id_ho_so_benh_nhan = :idHoSoBenhNhan and giam_ho_cccd = :cccd and giam_ho_ngay_sinh is null
            """, nativeQuery = true)
    int ghiNgaySinh(Long idHoSoBenhNhan, String cccd, LocalDate ngaySinh);

}
