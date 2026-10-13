package iuh.fit.se.eclinic.common.exception;

public class LoiKhongTimThay extends LoiNghiepVu {

    public LoiKhongTimThay(String tenDoiTuong, Object id) {
        super(MaLoi.KHONG_TIM_THAY, "loi.KHONG_TIM_THAY.theoId", tenDoiTuong, String.valueOf(id));
    }

}
