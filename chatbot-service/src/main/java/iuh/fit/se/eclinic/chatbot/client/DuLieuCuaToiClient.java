package iuh.fit.se.eclinic.chatbot.client;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import iuh.fit.se.eclinic.chatbot.config.ChatbotProperties;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;

/**
 * Đọc lịch hẹn sắp tới và các lượt đã khám của CHÍNH người đang chat qua 2 API "của tôi" của booking-service, gửi kèm
 * JWT của người đó. "Của ai" và "được xem những gì" do booking-service quyết định từ token (cùng luật với màn hình của
 * bệnh nhân); ở đây không có tham số nào để chọn người khác.
 * <p>
 * Các record chỉ khai báo những trường chatbot cần: lý do khám, lý do hủy, chẩn đoán, lời dặn, đơn thuốc, số điện thoại
 * có trong phản hồi của API nhưng bị bỏ ngay khi đọc JSON, không vào bộ nhớ của service này. Token không được ghi log.
 */
@Component
public class DuLieuCuaToiClient {

    private static final int SO_DONG_TOI_DA = 10;

    private static final ParameterizedTypeReference<PhanHoiApi<TrangDuLieu<LichHenCuaToi>>> KIEU_LICH_HEN =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<PhanHoiApi<TrangDuLieu<LanKham>>> KIEU_LAN_KHAM =
            new ParameterizedTypeReference<>() {
            };

    /**
     * @param hoTenBenhNhan chỉ để vẽ thẻ cho người dùng; KHÔNG gửi cho mô hình (cũng vậy với 3 mã)
     * @param ngay          yyyy-MM-dd; 2 giờ dạng ISO
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LichHenCuaToi(String maPhieuKham, String maTraCuu, String linkPhieuKham, String trangThai, String ngay,
            String gioBatDauKhung, String gioKetThucKhung, BacSi bacSi, String tenChuyenKhoa, PhongKham phongKham,
            String hoTenBenhNhan, boolean laBanThan, boolean canDoiLich) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BacSi(String hoTen, String hocVi) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PhongKham(String tenPhong, String tang) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LanKham(LichHenCuaToi lichHen, KetQua ketQua) {
    }

    /** Của kết quả khám chỉ lấy ngày tái khám đề xuất. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KetQua(String ngayTaiKhamDeXuat) {
    }

    private final RestClient booking;

    public DuLieuCuaToiClient(ChatbotProperties properties) {
        this.booking = DuLieuPhongKhamClient.taoRestClient(properties.bookingUrl());
    }

    /** Tối đa 10 lịch hẹn còn phải khám của người cầm {@code token}, gần nhất trước. */
    public List<LichHenCuaToi> lichHenSapToi(String token) {
        return DuLieuPhongKhamClient.duLieu(DuLieuPhongKhamClient.goi("booking-service", () -> booking.get()
                .uri("/api/booking/lich-hen/cua-toi?loc=SAP_TOI&kichThuoc={n}", SO_DONG_TOI_DA)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(KIEU_LICH_HEN))).noiDung();
    }

    /** Tối đa 10 lượt đã khám mà người cầm {@code token} được xem kết quả, mới nhất trước. */
    public List<LanKham> lanKham(String token) {
        return DuLieuPhongKhamClient.duLieu(DuLieuPhongKhamClient.goi("booking-service", () -> booking.get()
                .uri("/api/booking/lich-su-kham/cua-toi?kichThuoc={n}", SO_DONG_TOI_DA)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(KIEU_LAN_KHAM))).noiDung();
    }

}
