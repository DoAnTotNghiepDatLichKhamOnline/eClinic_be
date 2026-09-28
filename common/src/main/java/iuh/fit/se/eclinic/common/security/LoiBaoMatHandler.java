package iuh.fit.se.eclinic.common.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

/**
 * Trả lỗi 401/403 dạng PhanHoiApi khi request bị chặn ở filter bảo mật (thiếu token, token sai/hết hạn...).
 */
@Component
@RequiredArgsConstructor
public class LoiBaoMatHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        ghi(response, MaLoi.CHUA_DANG_NHAP);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        ghi(response, MaLoi.KHONG_CO_QUYEN);
    }

    private void ghi(HttpServletResponse response, MaLoi maLoi) throws IOException {
        response.setStatus(maLoi.getTrangThaiHttp().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(), PhanHoiApi.loi(maLoi, maLoi.getThongDiepMacDinh()));
    }

}
