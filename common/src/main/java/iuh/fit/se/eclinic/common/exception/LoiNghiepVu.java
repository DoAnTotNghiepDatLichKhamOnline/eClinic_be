package iuh.fit.se.eclinic.common.exception;

import lombok.Getter;

/**
 * Lỗi nghiệp vụ có mã lỗi. XuLyLoiHandler chuyển nó thành PhanHoiApi với HTTP status của MaLoi.
 */
@Getter
public class LoiNghiepVu extends RuntimeException {

    private final MaLoi maLoi;

    public LoiNghiepVu(MaLoi maLoi) {
        this(maLoi, maLoi.getThongDiepMacDinh());
    }

    public LoiNghiepVu(MaLoi maLoi, String thongDiep) {
        super(thongDiep);
        this.maLoi = maLoi;
    }

}
