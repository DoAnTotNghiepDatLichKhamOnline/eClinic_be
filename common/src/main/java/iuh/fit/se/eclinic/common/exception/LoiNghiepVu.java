package iuh.fit.se.eclinic.common.exception;

import lombok.Getter;

/** Lỗi nghiệp vụ có mã lỗi. */
@Getter
public class LoiNghiepVu extends RuntimeException {

    private final MaLoi maLoi;
    private final String khoaThongDiep;
    private final transient Object[] thamSo;

    /** Thông điệp mặc định của mã lỗi (khoá loi.&lt;MA_LOI&gt;). */
    public LoiNghiepVu(MaLoi maLoi) {
        this(maLoi, maLoi.khoa());
    }

    public LoiNghiepVu(MaLoi maLoi, String khoaThongDiep, Object... thamSo) {
        super(khoaThongDiep);
        this.maLoi = maLoi;
        this.khoaThongDiep = khoaThongDiep;
        this.thamSo = thamSo;
    }

}
