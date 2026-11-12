package iuh.fit.se.eclinic.catalog.service;

import iuh.fit.se.eclinic.catalog.dto.response.ChuyenKhoaQuanTriResponse;

/**
 * Ảnh minh hoạ của chuyên khoa (quản trị viên). Mỗi chuyên khoa có đúng 1 ảnh riêng trong kho, mã cố định theo id: tải
 * lên lần nữa thì ghi đè, xoá thì xoá theo mã đó. Ảnh mẫu dùng chung (eclinic/mau/…) không bao giờ bị xoá.
 */
public interface AnhChuyenKhoaService {

    /** Ném ANH_KHONG_HOP_LE nếu không phải JPEG / PNG / WebP, LUU_TRU_ANH_KHONG_KHA_DUNG nếu chưa cấu hình kho ảnh. */
    ChuyenKhoaQuanTriResponse taiLen(Long idChuyenKhoa, byte[] noiDung);

    /** Bỏ ảnh của chuyên khoa (anhMinhHoa = null); chưa có ảnh thì không làm gì. */
    ChuyenKhoaQuanTriResponse xoa(Long idChuyenKhoa);

}
