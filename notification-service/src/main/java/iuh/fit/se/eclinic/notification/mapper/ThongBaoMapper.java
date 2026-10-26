package iuh.fit.se.eclinic.notification.mapper;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.notification.ThongBao;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.thongbao.SoanThongBao;
import iuh.fit.se.eclinic.notification.dto.response.ThongBaoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThongBaoMapper {

    private final SoanThongBao soanThongBao;

    /**
     * @param kemMaPhieuKham true khi người đọc là bệnh nhân. Mã phiếu khám mở được phiếu khám mà không cần đăng nhập, nên
     *                       không trả cho bác sĩ / quản trị viên (họ mở lịch hẹn bằng id).
     */
    public ThongBaoResponse toResponse(ThongBao thongBao, boolean kemMaPhieuKham) {
        LichHen lichHen = thongBao.getLichHen();
        return new ThongBaoResponse(thongBao.getId(), thongBao.getLoai(), noiDung(thongBao), thongBao.isDaDoc(),
                thongBao.getNgayTao(), lichHen == null ? null : lichHen.getId(),
                lichHen == null || !kemMaPhieuKham ? null : lichHen.getMaTokenPhieuKham(),
                // Chỉ đọc id của proxy, không tải yêu cầu
                thongBao.getYeuCauDoiLich() == null ? null : thongBao.getYeuCauDoiLich().getId());
    }


    /** Thông báo có mẫu thì soạn theo ngôn ngữ của request; thông báo cũ (trước V18) trả câu đã lưu. */
    private String noiDung(ThongBao thongBao) {
        if (thongBao.getKhoaMau() == null) {
            return thongBao.getNoiDung();
        }
        try {
            return soanThongBao.soan(thongBao.getKhoaMau(), thongBao.getThamSo(), NgonNgu.hienTai());
        } catch (IllegalArgumentException ex) {
            log.warn("Thông báo id={} có tham số không đọc được, trả câu đã lưu", thongBao.getId());
            return thongBao.getNoiDung();
        }
    }
}
