package iuh.fit.se.eclinic.chatbot.client;

import java.util.Map;

/** 1 hàm tra cứu cố định mà mô hình được xin gọi ("tool" / "function" của API kiểu OpenAI). */
public record CongCu(String ten, String moTa, Map<String, Object> thamSo) {
}
