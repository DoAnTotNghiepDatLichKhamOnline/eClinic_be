package iuh.fit.se.eclinic.catalog.service;

import iuh.fit.se.eclinic.catalog.dto.request.CapNhatHoSoCuaToiRequest;
import iuh.fit.se.eclinic.catalog.dto.response.AnhBacSiQuanTriResponse;
import iuh.fit.se.eclinic.catalog.dto.response.HoSoBacSiQuanTriResponse;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;

/**
 * Bác sĩ đang đăng nhập xem hồ sơ giới thiệu của mình và tự sửa phần được phép (DOANTOTNGH-10 B7): giới thiệu ngắn, tiểu
 * sử, ngôn ngữ khám, giải thưởng, công trình nghiên cứu, ảnh làm việc. Có hiệu lực ngay, không chờ duyệt. Ảnh đại diện,
 * số điện thoại vẫn sửa ở /api/users/me.
 * <p>
 * Mọi thao tác ném KHONG_CO_QUYEN (403) nếu tài khoản không có hồ sơ bác sĩ, TAI_KHOAN_BI_VO_HIEU_HOA (403) nếu tài
 * khoản bị vô hiệu hoá.
 */
public interface HoSoCuaBacSiService {

    HoSoBacSiQuanTriResponse layHoSo(Long idTaiKhoan);

    /** Lỗi như {@link HoSoBacSiService#capNhatHoSoCuaToi}. */
    HoSoBacSiQuanTriResponse capNhatHoSo(Long idTaiKhoan, CapNhatHoSoCuaToiRequest request);

    /**
     * Thêm 1 ảnh làm việc (luôn là ANH_CONG_VIEC). Lỗi như {@link AnhBacSiService#them}.
     *
     * @throws LoiNghiepVu ANH_KHONG_HOP_LE (400), VUOT_SO_ANH_BAC_SI (409), LUU_TRU_ANH_KHONG_KHA_DUNG (503)
     */
    AnhBacSiQuanTriResponse themAnh(Long idTaiKhoan, String chuThich, String chuThichEn, byte[] noiDung);

    /** Ảnh chứng chỉ: KHONG_CO_QUYEN (403); ảnh của bác sĩ khác: KHONG_TIM_THAY. */
    AnhBacSiQuanTriResponse capNhatAnh(Long idTaiKhoan, Long idAnh, String chuThich, String chuThichEn);

    /** Như {@link #capNhatAnh}. */
    void xoaAnh(Long idTaiKhoan, Long idAnh);

}
