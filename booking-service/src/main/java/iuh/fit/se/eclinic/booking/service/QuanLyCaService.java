package iuh.fit.se.eclinic.booking.service;

import java.time.LocalDate;
import java.time.LocalTime;

import iuh.fit.se.eclinic.booking.dto.request.SuaCaRequest;
import iuh.fit.se.eclinic.booking.dto.request.TaoCaRequest;
import iuh.fit.se.eclinic.booking.dto.response.CaLamViecResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaHuyCaResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaTaoCaResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;

/**
 * SCHED-01/05/06: quản trị viên xếp, sửa, hủy ca làm việc. Các lượt khám (khung_gio_kham) được sinh / chỉnh theo ca.
 * <p>
 * Quy tắc với ca đã có người đặt:
 * <ul>
 *   <li>Sửa: phòng khám đổi được bất cứ lúc nào (lịch hẹn còn hiệu lực mang phòng mới, bệnh nhân được báo). Giờ và sức
 *       chứa chỉ đổi được khi mọi lượt đã có người đặt vẫn còn nguyên giờ bắt đầu / kết thúc; không thì CA_CON_LICH_HEN.</li>
 *   <li>Hủy: lịch hẹn còn hiệu lực giữ nguyên trạng thái và được đánh dấu {@code canDoiLich}; bệnh nhân được báo để đổi
 *       sang khung giờ khác hoặc hủy.</li>
 * </ul>
 * Ca đã bắt đầu hoặc đã hủy thì không sửa / hủy được (CA_KHONG_SUA_DUOC).
 */
public interface QuanLyCaService {

    /**
     * Xếp 1 ca, hoặc nhiều ca cùng giờ khi có {@code lapLai}. Không lặp lại: trùng giờ với ca khác của bác sĩ / phòng
     * khám thì ném TRUNG_LICH_LAM_VIEC. Có lặp lại: ngày trùng bị bỏ qua và liệt kê trong kết quả; không tạo được ca nào
     * thì ném TRUNG_LICH_LAM_VIEC.
     */
    KetQuaTaoCaResponse tao(Long idTaiKhoanQuanTri, TaoCaRequest request);

    CaLamViecResponse sua(Long idTaiKhoanQuanTri, Long idLichLamViec, SuaCaRequest request);

    KetQuaHuyCaResponse huy(Long idTaiKhoanQuanTri, Long idLichLamViec, String lyDo);

    /**
     * Như {@link #huy} nhưng không báo bác sĩ: dùng khi hủy hàng loạt ca của bác sĩ ngừng công tác.
     *
     * @return số lịch hẹn vừa được đánh dấu cần đổi lịch
     */
    int huyKhongBaoBacSi(Long idTaiKhoanQuanTri, Long idLichLamViec, String lyDo);

    /** Lịch hẹn còn hiệu lực đang chờ bệnh nhân đổi lịch vì ca khám bị hủy, giờ khám cũ sớm nhất trước. */
    TrangDuLieu<LichHenTrongCaResponse> lichHenCanDoi(int trang, int kichThuoc);

    // ----- Các bước dùng lại khi duyệt yêu cầu đổi ca / xin nghỉ; phải gọi trong transaction của nơi gọi -----

    /** Hồ sơ quản trị viên của tài khoản đang đăng nhập. Ném KHONG_CO_QUYEN nếu tài khoản không có hồ sơ này. */
    QuanTriVien layQuanTriVien(Long idTaiKhoan);

    /** Khoá dòng ca và kiểm tra ca còn sửa / hủy được. Ném KHONG_TIM_THAY, CA_KHONG_SUA_DUOC. */
    LichLamViec khoaCaConSuaDuoc(Long idLichLamViec);

    /**
     * Hủy 1 ca đã khoá bằng {@link #khoaCaConSuaDuoc}: hủy mọi lượt khám, đánh dấu và báo các lịch hẹn còn hiệu lực,
     * đóng yêu cầu đang chờ duyệt của ca (trừ {@code idYeuCauDangDuyet}). Không báo bác sĩ: nơi gọi tự báo.
     *
     * @param idYeuCauDangDuyet yêu cầu đang được duyệt trong cùng transaction (không bị đóng); null khi hủy trực tiếp
     * @return số lịch hẹn vừa được đánh dấu cần đổi lịch
     */
    int huyCaDaKhoa(LichLamViec ca, QuanTriVien quanTri, String lyDo, Long idYeuCauDangDuyet);

    /**
     * Sửa 1 ca đã khoá (cùng quy tắc với {@link #sua}). Không báo bác sĩ: nơi gọi tự báo.
     *
     * @return true nếu có thay đổi
     */
    boolean suaCaDaKhoa(LichLamViec ca, Long idPhongKham, LocalTime gioBatDau, LocalTime gioKetThuc,
            int soLuotToiDaMoiGio, int thoiLuongLuotPhut);

    /** Xếp 1 ca mới cho bác sĩ. Ném TRUNG_LICH_LAM_VIEC nếu trùng giờ. Không báo bác sĩ: nơi gọi tự báo. */
    LichLamViec taoCaMoi(Long idBacSi, Long idPhongKham, QuanTriVien quanTri, LocalDate ngay, LocalTime gioBatDau,
            LocalTime gioKetThuc, int soLuotToiDaMoiGio, int thoiLuongLuotPhut);

    /** Ca kèm số lượt khám hiện tại (đã flush các thay đổi trong transaction). */
    CaLamViecResponse toResponse(LichLamViec ca);

}
