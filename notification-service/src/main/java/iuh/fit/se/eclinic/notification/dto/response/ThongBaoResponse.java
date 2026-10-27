package iuh.fit.se.eclinic.notification.dto.response;

import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.LoaiThongBao;

/** 1 thông báo của tài khoản đang đăng nhập. */
public record ThongBaoResponse(Long id, LoaiThongBao loai, String noiDung, boolean daDoc, LocalDateTime ngayTao,
        Long idLichHen, String maPhieuKham, Long idYeuCau) {
}
