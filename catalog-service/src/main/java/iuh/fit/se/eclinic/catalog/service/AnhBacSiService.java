package iuh.fit.se.eclinic.catalog.service;

import java.util.List;

import iuh.fit.se.eclinic.catalog.dto.request.CapNhatAnhBacSiRequest;
import iuh.fit.se.eclinic.catalog.dto.response.AnhBacSiResponse;
import iuh.fit.se.eclinic.common.enums.LoaiAnhBacSi;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;

/**
 * Quản trị viên quản lý ảnh giới thiệu của bác sĩ (ảnh làm việc, chứng chỉ). Mọi thao tác ném KHONG_TIM_THAY nếu
 * không có bác sĩ, hoặc ảnh không phải của bác sĩ đó.
 */
public interface AnhBacSiService {

    /** Số ảnh giới thiệu tối đa của 1 bác sĩ. */
    int SO_ANH_TOI_DA = 12;

    /**
     * Tải 1 ảnh lên kho ảnh và thêm vào cuối danh sách.
     *
     * @param noiDung JPEG, PNG hoặc WebP (nhận ra bằng các byte đầu)
     * @throws LoiNghiepVu ANH_KHONG_HOP_LE (400), VUOT_SO_ANH_BAC_SI (409), LUU_TRU_ANH_KHONG_KHA_DUNG (503)
     */
    AnhBacSiResponse them(Long idBacSi, LoaiAnhBacSi loai, String chuThich, byte[] noiDung);

    AnhBacSiResponse capNhat(Long idBacSi, Long idAnh, CapNhatAnhBacSiRequest request);

    /**
     * Đặt lại thứ tự hiển thị.
     *
     * @param idAnh id của MỌI ảnh của bác sĩ theo thứ tự mới; thiếu, thừa hoặc lặp -> DU_LIEU_KHONG_HOP_LE (400)
     * @return các ảnh theo thứ tự mới
     */
    List<AnhBacSiResponse> sapXep(Long idBacSi, List<Long> idAnh);

    /** Xoá ảnh khỏi danh sách và khỏi kho ảnh (kho ảnh lỗi thì chỉ ghi log, ảnh vẫn bị xoá khỏi danh sách). */
    void xoa(Long idBacSi, Long idAnh);

}
