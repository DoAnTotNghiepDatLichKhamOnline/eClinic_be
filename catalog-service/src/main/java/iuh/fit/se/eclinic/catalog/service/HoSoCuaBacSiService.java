package iuh.fit.se.eclinic.catalog.service;

import iuh.fit.se.eclinic.catalog.dto.request.CapNhatHoSoCuaToiRequest;
import iuh.fit.se.eclinic.catalog.dto.response.AnhBacSiQuanTriResponse;
import iuh.fit.se.eclinic.catalog.dto.response.HoSoBacSiQuanTriResponse;

/** Bác sĩ đang đăng nhập xem và tự sửa phần được phép của hồ sơ giới thiệu. */
public interface HoSoCuaBacSiService {

    HoSoBacSiQuanTriResponse layHoSo(Long idTaiKhoan);

    /** Lỗi như {@link HoSoBacSiService#capNhatHoSoCuaToi}. */
    HoSoBacSiQuanTriResponse capNhatHoSo(Long idTaiKhoan, CapNhatHoSoCuaToiRequest request);

    /** Thêm 1 ảnh làm việc (luôn là ANH_CONG_VIEC). */
    AnhBacSiQuanTriResponse themAnh(Long idTaiKhoan, String chuThich, String chuThichEn, byte[] noiDung);

    /** Ảnh chứng chỉ: KHONG_CO_QUYEN (403); ảnh của bác sĩ khác: KHONG_TIM_THAY. */
    AnhBacSiQuanTriResponse capNhatAnh(Long idTaiKhoan, Long idAnh, String chuThich, String chuThichEn);

    /** Như {@link #capNhatAnh}. */
    void xoaAnh(Long idTaiKhoan, Long idAnh);

}
