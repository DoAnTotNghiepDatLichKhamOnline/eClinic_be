package iuh.fit.se.eclinic.catalog.service;

import iuh.fit.se.eclinic.catalog.dto.request.CapNhatHoSoCuaToiRequest;
import iuh.fit.se.eclinic.catalog.dto.response.AnhBacSiQuanTriResponse;
import iuh.fit.se.eclinic.catalog.dto.response.HoSoBacSiQuanTriResponse;
import iuh.fit.se.eclinic.common.enums.LoaiAnhBacSi;

/** Bác sĩ đang đăng nhập xem và tự sửa phần được phép của hồ sơ giới thiệu. */
public interface HoSoCuaBacSiService {

    HoSoBacSiQuanTriResponse layHoSo(Long idTaiKhoan);

    /** Lỗi như {@link HoSoBacSiService#capNhatHoSoCuaToi}. */
    HoSoBacSiQuanTriResponse capNhatHoSo(Long idTaiKhoan, CapNhatHoSoCuaToiRequest request);

    /** Thêm 1 ảnh giới thiệu loại {@code loai} (ảnh làm việc, chứng chỉ hoặc giải thưởng). */
    AnhBacSiQuanTriResponse themAnh(Long idTaiKhoan, LoaiAnhBacSi loai, String chuThich, String chuThichEn,
            byte[] noiDung);

    /** Sửa chú thích, và loại nếu {@code loai} khác null. Ảnh của bác sĩ khác: KHONG_TIM_THAY. */
    AnhBacSiQuanTriResponse capNhatAnh(Long idTaiKhoan, Long idAnh, LoaiAnhBacSi loai, String chuThich,
            String chuThichEn);

    /** Xoá 1 ảnh trong hồ sơ của tôi, mọi loại. Ảnh của bác sĩ khác: KHONG_TIM_THAY. */
    void xoaAnh(Long idTaiKhoan, Long idAnh);

}
