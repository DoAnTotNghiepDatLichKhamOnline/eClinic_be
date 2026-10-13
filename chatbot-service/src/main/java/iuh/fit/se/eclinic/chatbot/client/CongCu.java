package iuh.fit.se.eclinic.chatbot.client;

import java.util.Map;

/**
 * 1 hàm tra cứu cố định mà mô hình được xin gọi ("tool" / "function" của API kiểu OpenAI).
 *
 * @param moTa   mô tả cho mô hình: hàm trả gì, khi nào nên gọi
 * @param thamSo JSON Schema của tham số
 */
public record CongCu(String ten, String moTa, Map<String, Object> thamSo) {
}
