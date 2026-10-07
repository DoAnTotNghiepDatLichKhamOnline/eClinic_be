package iuh.fit.se.eclinic.booking.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import iuh.fit.se.eclinic.booking.client.ThongBaoClient;
import iuh.fit.se.eclinic.booking.client.ThongBaoClient.ThongBaoCanGui;
import iuh.fit.se.eclinic.booking.repository.SuKienThongBaoRepository;
import iuh.fit.se.eclinic.booking.service.GuiThongBaoService;
import iuh.fit.se.eclinic.common.entity.booking.SuKienThongBao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Không có {@code @Transactional}: lời gọi sang notification-service không nằm trong transaction DB. Đọc các dòng chờ
 * gửi, gọi, rồi mới đánh dấu đã gửi bằng 1 lệnh update ngắn. Gọi xong mà chưa kịp đánh dấu (service tắt giữa chừng) thì
 * lần sau gửi lại; notification-service nhận ra nhờ {@code maNguon} nên không tạo thông báo trùng.
 * <p>
 * Chỉ 1 thread của scheduler gọi class này (booking-service chạy 1 bản), nên không cần khoá dòng khi đọc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuiThongBaoServiceImpl implements GuiThongBaoService {

    /** Tiền tố của maNguon: notification-service nhận sự kiện từ nhiều service, mỗi service 1 dãy id riêng. */
    private static final String NGUON = "booking:";
    /** 1 dòng bị notification-service từ chối (4xx) đủ số lần này thì bỏ qua, không để nó chặn các dòng sau. */
    private static final int SO_LAN_LOI_TOI_DA = 3;
    private static final Duration GIU_DA_GUI = Duration.ofDays(7);

    private final SuKienThongBaoRepository suKienThongBaoRepository;
    private final ThongBaoClient thongBaoClient;

    /** Chỉ ghi log khi chuyển trạng thái, để notification-service ngừng lâu không làm đầy log. */
    private boolean dangMatKetNoi;

    @Override
    public int guiDangCho() {
        List<SuKienThongBao> choGui = suKienThongBaoRepository
                .findTop100ByNgayGuiIsNullAndSoLanLoiLessThanOrderByIdAsc(SO_LAN_LOI_TOI_DA);
        if (choGui.isEmpty()) {
            return 0;
        }
        List<Long> daGui = new ArrayList<>();
        try {
            thongBaoClient.gui(choGui.stream().map(GuiThongBaoServiceImpl::toCanGui).toList());
            choGui.forEach(suKien -> daGui.add(suKien.getId()));
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            // Sai khoá nội bộ: lỗi cấu hình của cả 2 service, không phải của từng dòng
            baoMatKetNoi("HTTP " + ex.getStatusCode().value() + " (kiểm tra INTERNAL_API_KEY)");
            return 0;
        } catch (HttpClientErrorException ex) {
            // Cả lô bị từ chối vì có dòng sai: gửi từng dòng để tìm ra, các dòng đúng vẫn đi
            guiTungDong(choGui, daGui);
        } catch (RestClientException ex) {
            baoMatKetNoi(ex.getClass().getSimpleName());
            return 0;
        }
        if (dangMatKetNoi) {
            dangMatKetNoi = false;
            log.info("Đã gửi lại được thông báo sang notification-service");
        }
        if (!daGui.isEmpty()) {
            suKienThongBaoRepository.danhDauDaGui(daGui, LocalDateTime.now());
        }
        return daGui.size();
    }

    private void guiTungDong(List<SuKienThongBao> choGui, List<Long> daGui) {
        for (SuKienThongBao suKien : choGui) {
            try {
                thongBaoClient.gui(List.of(toCanGui(suKien)));
                daGui.add(suKien.getId());
            } catch (HttpClientErrorException ex) {
                suKienThongBaoRepository.tangSoLanLoi(suKien.getId());
                // Không ghi nội dung thông báo ra log (có họ tên bệnh nhân)
                log.warn("notification-service từ chối sự kiện thông báo id={} (HTTP {}), lần {}/{}", suKien.getId(),
                        ex.getStatusCode().value(), suKien.getSoLanLoi() + 1, SO_LAN_LOI_TOI_DA);
            } catch (RestClientException ex) {
                baoMatKetNoi(ex.getClass().getSimpleName());
                return;
            }
        }
    }

    private void baoMatKetNoi(String lyDo) {
        if (!dangMatKetNoi) {
            dangMatKetNoi = true;
            log.warn("Chưa gửi được thông báo sang notification-service ({}); các thông báo nằm chờ và được gửi lại", lyDo);
        }
    }

    private static ThongBaoCanGui toCanGui(SuKienThongBao suKien) {
        return new ThongBaoCanGui(NGUON + suKien.getId(), suKien.getLoai(), suKien.getIdTaiKhoan(),
                suKien.getIdLichHen(), suKien.getIdYeuCau(), suKien.getNoiDung());
    }

    @Override
    public int donDaGui() {
        return suKienThongBaoRepository.xoaDaGuiTruoc(LocalDateTime.now().minus(GIU_DA_GUI));
    }

}
