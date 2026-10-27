package iuh.fit.se.eclinic.catalog.dto.request;

/** Giới hạn dùng chung của hồ sơ giới thiệu bác sĩ, cho cả bản quản trị viên sửa và bản bác sĩ tự sửa. */
final class GioiHanHoSoBacSi {

    static final int SO_Y_TOI_DA = 20;
    static final int DO_DAI_Y = 300;
    static final String MOT_DONG = "[^\r\n]*";
    static final String MOI_Y_KHONG_TRONG = "{kiemTra.moiY.batBuoc}";
    static final String MOI_Y_TOI_DA = "{kiemTra.moiY.toiDa}";
    static final String MOI_Y_MOT_DONG = "{kiemTra.moiY.motDong}";

    static final int SO_NGON_NGU_TOI_DA = 10;
    static final String MA_NGON_NGU = "[A-Za-z]{2,3}";
    static final String MA_NGON_NGU_KHONG_HOP_LE = "{kiemTra.ngonNguKham.ma}";

    private GioiHanHoSoBacSi() {
    }
}
