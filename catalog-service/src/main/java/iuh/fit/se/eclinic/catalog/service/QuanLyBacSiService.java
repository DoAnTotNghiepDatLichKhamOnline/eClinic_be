package iuh.fit.se.eclinic.catalog.service;

import iuh.fit.se.eclinic.catalog.dto.request.SuaBacSiRequest;
import iuh.fit.se.eclinic.catalog.dto.request.ThemBacSiRequest;
import iuh.fit.se.eclinic.catalog.dto.response.AnhHuongNgungCongTacResponse;
import iuh.fit.se.eclinic.catalog.dto.response.BacSiQuanTriResponse;
import iuh.fit.se.eclinic.catalog.dto.response.HoSoBacSiQuanTriResponse;
import iuh.fit.se.eclinic.catalog.dto.response.KetQuaNgungCongTacResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.TrangThaiBacSi;

/**
 * Danh bạ bác sĩ của quản trị viên (phase 6). catalog-service chỉ ghi bảng bac_si; tài khoản do identity-service ghi và
 * ca làm việc do booking-service ghi, qua API nội bộ. Service kia không phản hồi: DICH_VU_NOI_BO_LOI (503), gọi lại
 * cùng request là thao tác chạy tiếp phần còn dở.
 */
public interface QuanLyBacSiService {

    /**
     * @param tuKhoa       so với họ tên, email, số điện thoại, số giấy phép; null / rỗng = không lọc
     * @param idChuyenKhoa null = mọi chuyên khoa
     * @param trangThai    null = cả bác sĩ đang công tác và đã ngừng
     */
    TrangDuLieu<BacSiQuanTriResponse> danhSach(String tuKhoa, Long idChuyenKhoa, TrangThaiBacSi trangThai, int trang,
            int kichThuoc);

    /**
     * Tạo tài khoản (mật khẩu mặc định, phải đặt mật khẩu ở lần đăng nhập đầu) rồi tạo hồ sơ bác sĩ. Không tạo được hồ
     * sơ thì tài khoản vừa tạo bị xoá. Ném KHONG_TIM_THAY (chuyên khoa), SO_GIAY_PHEP_DA_TON_TAI, EMAIL_DA_TON_TAI,
     * SO_DIEN_THOAI_DA_TON_TAI.
     */
    HoSoBacSiQuanTriResponse them(ThemBacSiRequest request);

    /**
     * Sửa họ tên, số điện thoại, chuyên khoa, số giấy phép. Đổi chuyên khoa khi bác sĩ còn ca sắp tới: ném
     * BAC_SI_CON_CA_LAM_VIEC (phòng khám của ca thuộc chuyên khoa cũ).
     */
    HoSoBacSiQuanTriResponse sua(Long idBacSi, SuaBacSiRequest request);

    AnhHuongNgungCongTacResponse anhHuongNgungCongTac(Long idBacSi);

    /**
     * 3 bước, bước nào cũng gọi lại được: (1) bác sĩ thành NGUNG_CONG_TAC (không nhận ca và lịch hẹn mới, ẩn khỏi danh
     * sách công khai); (2) booking-service hủy mọi ca chưa bắt đầu, lịch hẹn được giữ và đánh dấu cần đổi lịch, bệnh
     * nhân được báo; (3) identity-service vô hiệu hoá tài khoản. Bước 2 hoặc 3 lỗi thì bác sĩ vẫn ở NGUNG_CONG_TAC; gửi
     * lại request để chạy nốt.
     */
    KetQuaNgungCongTacResponse ngungCongTac(Long idTaiKhoanQuanTri, Long idBacSi, String lyDo);

    /** Kích hoạt lại tài khoản rồi đưa bác sĩ về DANG_CONG_TAC. Các ca đã hủy không được khôi phục. */
    HoSoBacSiQuanTriResponse congTacLai(Long idBacSi);

}
