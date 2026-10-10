package iuh.fit.se.eclinic.notification.mapper;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.notification.ThongBao;
import iuh.fit.se.eclinic.notification.dto.response.ThongBaoResponse;

@Component
public class ThongBaoMapper {

    /**
     * @param kemMaPhieuKham true khi người đọc là bệnh nhân. Mã phiếu khám mở được phiếu khám mà không cần đăng nhập, nên
     *                       không trả cho bác sĩ / quản trị viên (họ mở lịch hẹn bằng id).
     */
    public ThongBaoResponse toResponse(ThongBao thongBao, boolean kemMaPhieuKham) {
        LichHen lichHen = thongBao.getLichHen();
        return new ThongBaoResponse(thongBao.getId(), thongBao.getLoai(), thongBao.getNoiDung(), thongBao.isDaDoc(),
                thongBao.getNgayTao(), lichHen == null ? null : lichHen.getId(),
                lichHen == null || !kemMaPhieuKham ? null : lichHen.getMaTokenPhieuKham(),
                // Chỉ đọc id của proxy, không tải yêu cầu
                thongBao.getYeuCauDoiLich() == null ? null : thongBao.getYeuCauDoiLich().getId());
    }

}
