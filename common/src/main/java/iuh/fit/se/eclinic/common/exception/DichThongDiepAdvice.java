package iuh.fit.se.eclinic.common.exception;

import org.springframework.context.MessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import lombok.RequiredArgsConstructor;

/**
 * Soạn thông điệp của phản hồi thành công theo ngôn ngữ của request: controller viết
 * {@code PhanHoiApi.ok(duLieu, "thanhCong.datLich")}, ở đây khoá được đổi thành câu trong thong-diep/*.properties.
 * Không tìm thấy khoá thì giữ nguyên chuỗi. Phản hồi lỗi không qua đây xử lý (XuLyLoiHandler đã soạn sẵn).
 * <p>
 * Thông điệp có tham số (vd số thiết bị): controller tự soạn bằng MessageSource rồi đưa câu hoàn chỉnh vào PhanHoiApi.
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class DichThongDiepAdvice implements ResponseBodyAdvice<Object> {

    private final MessageSource messageSource;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        if (body instanceof PhanHoiApi<?> phanHoi && phanHoi.thanhCong() && phanHoi.thongDiep() != null) {
            String khoa = phanHoi.thongDiep();
            return PhanHoiApi.ok(phanHoi.duLieu(), messageSource.getMessage(khoa, null, khoa, NgonNgu.hienTai().locale()));
        }
        return body;
    }

}
