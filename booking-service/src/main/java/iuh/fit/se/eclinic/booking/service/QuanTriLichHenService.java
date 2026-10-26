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

/**
 * ADM-04: quản trị viên xem lịch hẹn toàn viện và can thiệp (hủy, đổi lịch thay bệnh nhân).
 * <p>
 * Hủy / đổi: chỉ lịch DA_XAC_NHAN (khác: LICH_HEN_KHONG_HUY_DOI_DUOC) chưa tới giờ khám (khác: LICH_HEN_DA_QUA_GIO);
 * lịch đang chờ đổi vì ca bị hủy ({@code canDoiLich}) thì lúc nào cũng được, vì job đóng ngày khám bỏ qua các lịch đó.
 * Không theo hạn hủy / đổi và giới hạn số lần đổi của bệnh nhân. Không trả mã phiếu khám, trừ phản hồi của
 * {@link #datHotline} lúc vừa đặt.
 */
public interface QuanTriLichHenService {

    /**
     * Lịch hẹn có giờ khám trong [tuNgay, denNgay] (mặc định từ hôm nay, 31 ngày; tối đa 366 ngày, khác:
     * DU_LIEU_KHONG_HOP_LE), sớm nhất trước. Từ khoá: mã ECL-... đầy đủ, số CCCD 12 chữ số (người khám hoặc người giám
     * hộ), hoặc 1 phần họ tên / SĐT / email liên hệ. Mọi bộ lọc null = không lọc.
     */
    TrangDuLieu<LichHenTrongCaResponse> danhSach(LocalDate tuNgay, LocalDate denNgay, Long idChuyenKhoa, Long idBacSi,
            Long idPhongKham, TrangThaiLichHen trangThai, KenhDatLich kenhDat, Boolean canDoiLich,
            Boolean canDoiChieu, String tuKhoa, int trang, int kichThuoc);

    /** Ném KHONG_TIM_THAY. */
    LichHenQuanTriChiTietResponse chiTiet(Long idLichHen);

    /** DA_XAC_NHAN -> QUAN_TRI_HUY kèm lý do, mở lại lượt khám, báo bệnh nhân (thông báo + email) và bác sĩ. */
    LichHenQuanTriChiTietResponse huy(Long idTaiKhoan, Long idLichHen, QuanTriHuyLichHenRequest request);

    /**
     * Như bệnh nhân đổi lịch (lịch cũ DA_HUY_DO_DOI_LICH, lịch mới DA_XAC_NHAN, cùng transaction) nhưng không theo hạn
     * và số lần đổi; khung giờ mới theo đúng quy tắc đặt lịch. Lịch mới giữ ngôn ngữ, kênh đặt của lịch cũ. Ném thêm các
     * mã của {@link DatLichService#datLaiTuLichCu}. Trả chi tiết lịch mới.
     */
    LichHenQuanTriChiTietResponse doiLich(Long idTaiKhoan, Long idLichHen, QuanTriDoiLichRequest request);

    /**
     * Đặt lịch thay người gọi tổng đài / người đến quầy ({@link DatLichService#datQuaHotline}): như khách đặt, không
     * giới hạn theo IP. Trả phiếu khám (kể cả mã phiếu khám) cho quản trị viên vừa đặt, để in hoặc mở mã QR.
     */
    DatLichHotlineResponse datHotline(Long idTaiKhoan, QuanTriDatLichRequest request);

}
