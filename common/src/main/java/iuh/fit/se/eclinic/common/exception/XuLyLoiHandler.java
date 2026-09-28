package iuh.fit.se.eclinic.common.exception;

import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import iuh.fit.se.eclinic.common.dto.ChiTietLoi;
import iuh.fit.se.eclinic.common.dto.PhanHoiApi;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;

/**
 * Chuyển mọi exception của controller thành PhanHoiApi, HTTP status lấy từ MaLoi.
 * Lỗi 401/403 xảy ra trước controller (filter bảo mật) do LoiBaoMatHandler xử lý.
 */
@Slf4j
@RestControllerAdvice
public class XuLyLoiHandler {

    private static final AuthenticationTrustResolver TRUST_RESOLVER = new AuthenticationTrustResolverImpl();

    @ExceptionHandler(LoiNghiepVu.class)
    public ResponseEntity<PhanHoiApi<Void>> xuLyLoiNghiepVu(LoiNghiepVu ex) {
        return traVe(ex.getMaLoi(), ex.getMessage(), null);
    }

    // ---- 400: dữ liệu đầu vào ----

    /** @Valid trên @RequestBody. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<PhanHoiApi<Void>> xuLyBodyKhongHopLe(MethodArgumentNotValidException ex) {
        List<ChiTietLoi> chiTiet = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fe -> chiTiet.add(chiTietTu(fe)));
        ex.getBindingResult().getGlobalErrors()
                .forEach(ge -> chiTiet.add(new ChiTietLoi(ge.getObjectName(), ge.getDefaultMessage())));
        return traVe(MaLoi.DU_LIEU_KHONG_HOP_LE, null, chiTiet);
    }

    /** Ràng buộc trên tham số của controller (@RequestParam @Min, @PathVariable @Positive...). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<PhanHoiApi<Void>> xuLyThamSoKhongHopLe(HandlerMethodValidationException ex) {
        List<ChiTietLoi> chiTiet = new ArrayList<>();
        ex.getParameterValidationResults().forEach(ketQua -> {
            if (ketQua instanceof ParameterErrors loiTruong) {
                loiTruong.getFieldErrors().forEach(fe -> chiTiet.add(chiTietTu(fe)));
            } else {
                String tenThamSo = ketQua.getMethodParameter().getParameterName();
                ketQua.getResolvableErrors()
                        .forEach(loi -> chiTiet.add(new ChiTietLoi(tenThamSo, loi.getDefaultMessage())));
            }
        });
        return traVe(MaLoi.DU_LIEU_KHONG_HOP_LE, null, chiTiet);
    }

    /** @Validated ở tầng service. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<PhanHoiApi<Void>> xuLyRangBuocKhongHopLe(ConstraintViolationException ex) {
        List<ChiTietLoi> chiTiet = new ArrayList<>();
        for (ConstraintViolation<?> vp : ex.getConstraintViolations()) {
            chiTiet.add(new ChiTietLoi(tenNutCuoi(vp.getPropertyPath()), vp.getMessage()));
        }
        return traVe(MaLoi.DU_LIEU_KHONG_HOP_LE, null, chiTiet);
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, HttpMediaTypeNotSupportedException.class })
    public ResponseEntity<PhanHoiApi<Void>> xuLyYeuCauSai(Exception ex) {
        String thongDiep = null;
        if (ex instanceof HttpMessageNotReadableException) {
            thongDiep = "Nội dung request không đọc được (JSON sai định dạng)";
        } else if (ex instanceof MethodArgumentTypeMismatchException e) {
            thongDiep = "Tham số '" + e.getName() + "' sai kiểu dữ liệu";
        } else if (ex instanceof MissingServletRequestParameterException e) {
            thongDiep = "Thiếu tham số '" + e.getParameterName() + "'";
        } else if (ex instanceof HttpMediaTypeNotSupportedException e) {
            thongDiep = "Content-Type không được hỗ trợ: " + e.getContentType();
        }
        return traVe(MaLoi.DU_LIEU_KHONG_HOP_LE, thongDiep, null);
    }

    // ---- 401/403/404/405/409 ----

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<PhanHoiApi<Void>> xuLySaiPhuongThuc(HttpRequestMethodNotSupportedException ex) {
        return traVe(MaLoi.PHUONG_THUC_KHONG_HO_TRO, null, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<PhanHoiApi<Void>> xuLyKhongCoDuongDan(NoResourceFoundException ex) {
        return traVe(MaLoi.KHONG_TIM_THAY, "Không tìm thấy đường dẫn /" + ex.getResourcePath(), null);
    }

    /**
     * @PreAuthorize ném lỗi bên trong controller nên tới đây chứ không tới LoiBaoMatHandler.
     * Thiếu handler này thì mọi lỗi phân quyền sẽ rơi vào 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<PhanHoiApi<Void>> xuLyKhongCoQuyen(AccessDeniedException ex) {
        boolean anDanh = TRUST_RESOLVER.isAnonymous(SecurityContextHolder.getContext().getAuthentication());
        return traVe(anDanh ? MaLoi.CHUA_DANG_NHAP : MaLoi.KHONG_CO_QUYEN, null, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<PhanHoiApi<Void>> xuLyViPhamRangBuocDb(DataIntegrityViolationException ex) {
        log.warn("Vi phạm ràng buộc DB: {}", ex.getMostSpecificCause().getMessage());
        return traVe(MaLoi.XUNG_DOT_DU_LIEU, null, null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<PhanHoiApi<Void>> xuLyDuLieuDaThayDoi(OptimisticLockingFailureException ex) {
        return traVe(MaLoi.DU_LIEU_DA_THAY_DOI, null, null);
    }

    // ---- 500 ----

    @ExceptionHandler(Exception.class)
    public ResponseEntity<PhanHoiApi<Void>> xuLyLoiKhac(Exception ex) {
        log.error("Lỗi không mong đợi", ex);
        return traVe(MaLoi.LOI_HE_THONG, null, null);
    }

    private static ResponseEntity<PhanHoiApi<Void>> traVe(MaLoi maLoi, String thongDiep, List<ChiTietLoi> chiTiet) {
        String noiDung = thongDiep != null ? thongDiep : maLoi.getThongDiepMacDinh();
        return ResponseEntity.status(maLoi.getTrangThaiHttp()).body(PhanHoiApi.loi(maLoi, noiDung, chiTiet));
    }

    private static ChiTietLoi chiTietTu(FieldError fe) {
        return new ChiTietLoi(fe.getField(), fe.getDefaultMessage());
    }

    private static String tenNutCuoi(Path duongDan) {
        String ten = null;
        for (Path.Node nut : duongDan) {
            ten = nut.getName();
        }
        return ten;
    }

}
