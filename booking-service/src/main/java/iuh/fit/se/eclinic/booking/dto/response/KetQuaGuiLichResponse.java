package iuh.fit.se.eclinic.booking.dto.response;

import java.util.List;

/** Kết quả gửi lịch làm việc: mỗi bác sĩ được xếp 1 email (bảng các ca) và 1 thông báo trong ứng dụng. */
public record KetQuaGuiLichResponse(int soBacSi, int soCa, List<DaGui> cacBacSi) {

    /** @param soPhut tổng số phút các ca của bác sĩ này trong khoảng đã gửi */
    public record DaGui(BacSiTomTatResponse bacSi, int soCa, long soPhut) {
    }

}
