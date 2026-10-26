package iuh.fit.se.eclinic.booking.service;

import java.time.LocalDateTime;

import iuh.fit.se.eclinic.booking.dto.request.DatLichRequest;
import iuh.fit.se.eclinic.booking.dto.response.DatLichResponse;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;

public interface DatLichService {

    /** Đặt lịch khám trong 1 transaction: khoá khung giờ, lấy lượt trống sớm nhất, tạo lịch hẹn DA_XAC_NHAN. */
    DatLichResponse datLich(DatLichRequest request, Long idTaiKhoan);

    /** Quản trị viên đặt thay người gọi tổng đài hoặc đến quầy, như khách đặt nhưng kênh HOTLINE. */
    DatLichResponse datQuaHotline(DatLichRequest request, QuanTriVien quanTri, NgonNgu ngonNgu);

    /** Phần tạo lịch mới của đổi lịch: khoá lượt khám của khung giờ mới và tạo lịch hẹn trỏ về lịch cũ. */
    DatLichResponse datLaiTuLichCu(LichHen cu, Long idLichLamViec, Long idChuyenKhoa, LocalDateTime gioBatDauKhung,
            LocalDateTime bayGio, NgonNgu ngonNgu);

}
