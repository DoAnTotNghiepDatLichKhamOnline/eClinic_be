package iuh.fit.se.eclinic.common.exception;

import lombok.Getter;

/**
 * Lỗi nghiệp vụ có mã lỗi. XuLyLoiHandler chuyển nó thành PhanHoiApi với HTTP status của MaLoi.
 * <p>
 * Lỗi mang KHOÁ thông điệp (thong-diep/*.properties) và tham số, không mang câu chữ: XuLyLoiHandler soạn câu theo ngôn
 * ngữ của request. {@link #getMessage()} trả về khoá.
 */
@Getter
public class LoiNghiepVu extends RuntimeException {

    private final MaLoi maLoi;
    private final String khoaThongDiep;
    private final transient Object[] thamSo;

    /** Thông điệp mặc định của mã lỗi (khoá loi.&lt;MA_LOI&gt;). */
    public LoiNghiepVu(MaLoi maLoi) {
        this(maLoi, maLoi.khoa());
    }

    /**
     * @param khoaThongDiep khoá trong thong-diep/*.properties, vd "loi.VUOT_GIOI_HAN_DAT_LICH.theoBenhNhan". Không tìm
     *                      thấy khoá thì chính chuỗi này được trả về (dùng khi chuyển nguyên thông điệp của service khác)
     * @param thamSo        giá trị cho {0}, {1}... của thông điệp; số và ngày giờ nên đổi sang chuỗi trước
     */
    public LoiNghiepVu(MaLoi maLoi, String khoaThongDiep, Object... thamSo) {
        super(khoaThongDiep);
        this.maLoi = maLoi;
        this.khoaThongDiep = khoaThongDiep;
        this.thamSo = thamSo;
    }

}
