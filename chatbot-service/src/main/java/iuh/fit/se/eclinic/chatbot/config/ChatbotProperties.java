package iuh.fit.se.eclinic.chatbot.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình chatbot, prefix {@code app.chatbot} trong application.yml.
 *
 * @param urlGoc                      gốc của API kiểu OpenAI ("chat completions"); request gửi tới {@code urlGoc/chat/completions}
 * @param khoaApi                     khoá API của nhà cung cấp mô hình; để trống = chatbot tắt (gửi tin nhắn trả 503)
 * @param moHinh                      tên mô hình
 * @param mucSuyLuan                  {@code reasoning_effort} gửi kèm (minimal / low / medium / high); để trống = không gửi
 * @param thoiGianCho                 thời gian chờ của 1 lần gọi mô hình
 * @param tongThoiGianCho             tổng thời gian của 1 tin nhắn (mọi lần gọi mô hình và tra cứu), phải nhỏ hơn
 *                                    read-timeout của gateway
 * @param soLanGoiMoHinhToiDa         số lần gọi mô hình tối đa cho 1 tin nhắn (các lần mô hình xin tra cứu + lần trả lời)
 * @param catalogUrl                  địa chỉ catalog-service (API công khai: bác sĩ)
 * @param bookingUrl                  địa chỉ booking-service (API công khai: khung giờ còn chỗ)
 * @param traCuuCaNhan                false: tắt hẳn việc chatbot đọc lịch hẹn, lượt khám của bệnh nhân đang chat
 * @param soTokenTraLoiToiDa          {@code max_tokens} của 1 câu trả lời
 * @param soTinNhanLichSu             số tin nhắn gần nhất của phiên gửi kèm cho mô hình
 * @param soTinNhanMoiCuaSo           số tin nhắn tối đa của 1 tài khoản / 1 địa chỉ IP của khách trong {@code cuaSo}
 * @param cuaSo                       khoảng thời gian đếm {@code soTinNhanMoiCuaSo}
 * @param soTinNhanMoiNgay            số tin nhắn tối đa của 1 tài khoản / 1 địa chỉ IP của khách trong 24 giờ
 * @param soPhienMoiGio               số phiên chat tối đa 1 tài khoản / 1 địa chỉ IP của khách mở trong 1 giờ
 * @param soTinNhanToanHeThongMoiNgay số lần gọi mô hình tối đa của cả hệ thống trong 24 giờ (giữ hạn mức của nhà cung cấp)
 */
@ConfigurationProperties("app.chatbot")
public record ChatbotProperties(String urlGoc, String khoaApi, String moHinh, String mucSuyLuan, Duration thoiGianCho,
        Duration tongThoiGianCho, int soLanGoiMoHinhToiDa, String catalogUrl, String bookingUrl,
        boolean traCuuCaNhan, int soTokenTraLoiToiDa, int soTinNhanLichSu, int soTinNhanMoiCuaSo, Duration cuaSo, int soTinNhanMoiNgay,
        int soPhienMoiGio, int soTinNhanToanHeThongMoiNgay) {

    public boolean daCauHinh() {
        return khoaApi != null && !khoaApi.isBlank();
    }

}
