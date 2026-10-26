package iuh.fit.se.eclinic.gateway.exception;

import lombok.Getter;

/** Địa chỉ IP gửi quá số request cho phép trong 1 phút (GioiHanTanSuatFilter) -> 429. */
@Getter
public class QuaNhieuYeuCauException extends RuntimeException {

    /** Số giây tới khi hết cửa sổ đếm hiện tại (header Retry-After). */
    private final long giayChoLai;

    public QuaNhieuYeuCauException(long giayChoLai) {
        super("Quá nhiều request, thử lại sau " + giayChoLai + " giây");
        this.giayChoLai = giayChoLai;
    }

}
