package iuh.fit.se.eclinic.booking.service;

import java.time.LocalDate;

import iuh.fit.se.eclinic.booking.dto.request.QuanTriDatLichRequest;
import iuh.fit.se.eclinic.booking.dto.request.QuanTriDoiLichRequest;
import iuh.fit.se.eclinic.booking.dto.request.QuanTriHuyLichHenRequest;
import iuh.fit.se.eclinic.booking.dto.response.DatLichHotlineResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenQuanTriChiTietResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.KenhDatLich;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

/** Quản trị viên xem lịch hẹn toàn viện và hủy, đổi lịch thay bệnh nhân. */
public interface QuanTriLichHenService {

    /** Lịch hẹn trong khoảng ngày (mặc định 31 ngày từ hôm nay, tối đa 366), sớm nhất trước. */
    TrangDuLieu<LichHenTrongCaResponse> danhSach(LocalDate tuNgay, LocalDate denNgay, Long idChuyenKhoa, Long idBacSi,
            Long idPhongKham, TrangThaiLichHen trangThai, KenhDatLich kenhDat, Boolean canDoiLich,
            Boolean canDoiChieu, String tuKhoa, int trang, int kichThuoc);

    /** Ném KHONG_TIM_THAY. */
    LichHenQuanTriChiTietResponse chiTiet(Long idLichHen);

    /** DA_XAC_NHAN -> QUAN_TRI_HUY kèm lý do, mở lại lượt khám, báo bệnh nhân (thông báo + email) và bác sĩ. */
    LichHenQuanTriChiTietResponse huy(Long idTaiKhoan, Long idLichHen, QuanTriHuyLichHenRequest request);

    /** Đổi lịch như bệnh nhân nhưng không theo hạn và số lần đổi. */
    LichHenQuanTriChiTietResponse doiLich(Long idTaiKhoan, Long idLichHen, QuanTriDoiLichRequest request);

    /** Đặt lịch thay người gọi tổng đài hoặc đến quầy, không giới hạn theo IP. */
    DatLichHotlineResponse datHotline(Long idTaiKhoan, QuanTriDatLichRequest request);

}
