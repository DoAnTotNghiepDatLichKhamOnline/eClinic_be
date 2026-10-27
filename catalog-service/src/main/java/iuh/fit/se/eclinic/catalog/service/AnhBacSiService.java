package iuh.fit.se.eclinic.catalog.service;

import java.util.List;

import iuh.fit.se.eclinic.catalog.dto.request.CapNhatAnhBacSiRequest;
import iuh.fit.se.eclinic.catalog.dto.response.AnhBacSiQuanTriResponse;
import iuh.fit.se.eclinic.common.enums.LoaiAnhBacSi;

/** Quản lý ảnh giới thiệu của bác sĩ: quản trị viên mọi loại, bác sĩ tự quản lý ảnh làm việc. */
public interface AnhBacSiService {

    /** Số ảnh giới thiệu tối đa của 1 bác sĩ. */
    int SO_ANH_TOI_DA = 12;

    /** Tải 1 ảnh lên kho ảnh và thêm vào cuối danh sách. */
    AnhBacSiQuanTriResponse them(Long idBacSi, LoaiAnhBacSi loai, String chuThich, String chuThichEn, byte[] noiDung);

    AnhBacSiQuanTriResponse capNhat(Long idBacSi, Long idAnh, CapNhatAnhBacSiRequest request);

    /** Đặt lại thứ tự hiển thị. */
    List<AnhBacSiQuanTriResponse> sapXep(Long idBacSi, List<Long> idAnh);

    /** Xoá ảnh khỏi danh sách và khỏi kho ảnh (kho ảnh lỗi thì chỉ ghi log, ảnh vẫn bị xoá khỏi danh sách). */
    void xoa(Long idBacSi, Long idAnh);

    /** Bác sĩ tự sửa chú thích ảnh làm việc của mình (không đổi loại ảnh). */
    AnhBacSiQuanTriResponse capNhatChuThichAnhCongViec(Long idBacSi, Long idAnh, String chuThich, String chuThichEn);

    /** Bác sĩ tự xoá ảnh làm việc của mình, như {@link #xoa}. */
    void xoaAnhCongViec(Long idBacSi, Long idAnh);

}
