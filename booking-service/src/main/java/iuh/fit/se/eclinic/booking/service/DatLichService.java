package iuh.fit.se.eclinic.booking.service;

import java.time.LocalDateTime;

import iuh.fit.se.eclinic.booking.dto.request.DatLichRequest;
import iuh.fit.se.eclinic.booking.dto.response.DatLichResponse;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;

public interface DatLichService {

    /**
     * Đặt lịch khám (BOOK-01, BOOK-03, BOOK-04, BOOK-11, BOOK-12) trong 1 transaction: kiểm tra lại ca và khung giờ,
     * khoá khung giờ rồi lấy lượt khám trống sớm nhất, tìm hoặc tạo hồ sơ bệnh nhân theo CCCD, tạo lịch hẹn
     * CHO_XAC_NHAN kèm số thứ tự và mã phiếu khám.
     * <p>
     * Số CCCD đã có hồ sơ: luôn dùng lại hồ sơ đó, kể cả khi họ tên / ngày sinh nhập vào khác hồ sơ. Hồ sơ không bị
     * sửa; lịch hẹn giữ bản sao thông tin đã nhập và được đánh dấu cần đối chiếu khi có khác biệt.
     * <p>
     * Bệnh nhân đã đăng nhập (BOOK-02, BOOK-07): lịch hẹn ghi nhận tài khoản đặt. Chỉ khi {@code datChoBanThan} thì hồ
     * sơ bệnh nhân mới là hồ sơ của tài khoản: tài khoản đã có hồ sơ thì CCCD nhập vào phải đúng hồ sơ đó; chưa có thì
     * CCCD phải là số chưa có hồ sơ, hồ sơ mới được gắn vào tài khoản. Không có cờ: hồ sơ theo CCCD như khách đặt.
     * <p>
     * Ném: KHONG_TIM_THAY (không có ca), DU_LIEU_KHONG_HOP_LE (khung giờ không thuộc ca), KHUNG_GIO_KHONG_KHA_DUNG,
     * KHUNG_GIO_KHONG_CON_TRONG, THIEU_NGUOI_GIAM_HO, NGUOI_GIAM_HO_KHONG_HOP_LE, THONG_TIN_BENH_NHAN_KHONG_KHOP (chỉ
     * khi đặt cho bản thân với số CCCD khác hồ sơ của tài khoản), LICH_HEN_TRUNG_GIO, VUOT_GIOI_HAN_DAT_LICH, CCCD_DA_CO_HO_SO, HO_SO_CHO_XAC_MINH và các mã của
     * {@link TaiKhoanService#layBenhNhanDangHoatDong}.
     *
     * @param idTaiKhoan tài khoản đang đăng nhập; null = khách đặt
     */
    DatLichResponse datLich(DatLichRequest request, Long idTaiKhoan);

    /**
     * Phần "tạo lịch mới" của đổi lịch (quy tắc #10): chọn và khoá lượt khám của khung giờ mới như {@link #datLich}, rồi
     * tạo lịch hẹn CHO_XAC_NHAN với phiếu khám mới, {@code lichHenCu} trỏ về {@code cu}. Hồ sơ bệnh nhân, người giám hộ,
     * tài khoản đặt, lý do khám, liên hệ và thông tin đã nhập lấy nguyên từ {@code cu}. Phải gọi trong transaction
     * READ_COMMITTED đã chuyển {@code cu} sang DA_HUY_DO_DOI_LICH và đã flush.
     * <p>
     * Ném: DU_LIEU_KHONG_HOP_LE (thiếu / thừa lựa chọn ca, khung giờ không thuộc ca, hoặc trùng khung giờ đang giữ),
     * KHONG_TIM_THAY (không có ca), KHUNG_GIO_KHONG_KHA_DUNG, KHUNG_GIO_KHONG_CON_TRONG, LICH_HEN_TRUNG_GIO,
     * VUOT_GIOI_HAN_DAT_LICH.
     *
     * @param idLichLamViec ca đã chọn; null khi chọn "bác sĩ bất kỳ" bằng {@code idChuyenKhoa}
     * @param bayGio        mốc thời gian của cả thao tác đổi lịch
     */
    DatLichResponse datLaiTuLichCu(LichHen cu, Long idLichLamViec, Long idChuyenKhoa, LocalDateTime gioBatDauKhung,
            LocalDateTime bayGio);

}
