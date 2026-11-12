package iuh.fit.se.eclinic.catalog.service;

import java.util.List;

import iuh.fit.se.eclinic.catalog.dto.request.CapNhatAnhBacSiRequest;
import iuh.fit.se.eclinic.catalog.dto.response.AnhBacSiQuanTriResponse;
import iuh.fit.se.eclinic.common.enums.LoaiAnhBacSi;

/**
 * Quản lý ảnh giới thiệu của bác sĩ (ảnh làm việc, chứng chỉ, giải thưởng). Quản trị viên quản lý ảnh của mọi bác sĩ,
 * bác sĩ tự quản lý mọi ảnh trong hồ sơ của mình.
 */
public interface AnhBacSiService {

    /** Số ảnh giới thiệu tối đa của 1 bác sĩ. */
    int SO_ANH_TOI_DA = 12;

    /** Tải 1 ảnh lên kho ảnh và thêm vào cuối danh sách. */
    AnhBacSiQuanTriResponse them(Long idBacSi, LoaiAnhBacSi loai, String chuThich, String chuThichEn, byte[] noiDung);

    /** Sửa loại và chú thích; request.loai() null = giữ loại đang có. Ảnh không thuộc bác sĩ này: KHONG_TIM_THAY. */
    AnhBacSiQuanTriResponse capNhat(Long idBacSi, Long idAnh, CapNhatAnhBacSiRequest request);

    /** Đặt lại thứ tự hiển thị. */
    List<AnhBacSiQuanTriResponse> sapXep(Long idBacSi, List<Long> idAnh);

    /** Xoá ảnh khỏi danh sách và khỏi kho ảnh (kho ảnh lỗi thì chỉ ghi log, ảnh vẫn bị xoá khỏi danh sách). */
    void xoa(Long idBacSi, Long idAnh);

}
