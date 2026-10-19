package iuh.fit.se.eclinic.booking.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import iuh.fit.se.eclinic.booking.dto.request.DongCaNhap;
import iuh.fit.se.eclinic.booking.dto.request.SaoChepCaRequest;
import iuh.fit.se.eclinic.booking.dto.request.SuaCaRequest;
import iuh.fit.se.eclinic.booking.dto.request.TaoCaRequest;
import iuh.fit.se.eclinic.booking.dto.response.CaLamViecResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaHuyCaResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaNhapExcelResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaSaoChepResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaTaoCaResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;

/**
 * SCHED-01/05/06: quản trị viên xếp, sửa, hủy ca làm việc. Các lượt khám (khung_gio_kham) được sinh / chỉnh theo ca.
 * <p>
 * Mọi lần xếp / sửa / hủy (kể cả khi duyệt yêu cầu, khi bác sĩ ngừng công tác) đi qua các phương thức ở đây, nên đều:
 * <ul>
 *   <li>kiểm tra giới hạn giờ làm của bác sĩ trong tuần thứ Hai - Chủ nhật (SCHED-05, app.lich-lam-viec.so-gio-toi-da-moi-tuan):
 *       xếp ca mới vượt giới hạn thì VUOT_GIO_LAM_TUAN (xếp lặp lại thì bỏ qua ngày đó); sửa ca chỉ bị chặn khi ca dài
 *       thêm và tuần vượt giới hạn;</li>
 *   <li>ghi 1 dòng lịch sử ca (lich_su_ca_lam_viec) với nguồn thay đổi ({@link NguonThayDoi}).</li>
 * </ul>
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
     * khám thì ném TRUNG_LICH_LAM_VIEC, vượt giờ làm trong tuần thì VUOT_GIO_LAM_TUAN. Có lặp lại: ngày trùng / vượt giờ bị
     * bỏ qua và liệt kê trong kết quả; không tạo được ca nào thì ném TRUNG_LICH_LAM_VIEC.
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
     * @param nguon nguồn ghi vào lịch sử ca; yêu cầu đang được duyệt trong cùng transaction ({@code nguon.idYeuCau()})
     *              không bị đóng
     * @return số lịch hẹn vừa được đánh dấu cần đổi lịch
     */
    int huyCaDaKhoa(LichLamViec ca, QuanTriVien quanTri, LyDoHuyCa lyDo, NguonThayDoi nguon);

    /**
     * Sửa 1 ca đã khoá (cùng quy tắc với {@link #sua}). Không báo bác sĩ: nơi gọi tự báo.
     *
     * @return true nếu có thay đổi (chỉ khi đó mới ghi lịch sử)
     */
    boolean suaCaDaKhoa(LichLamViec ca, QuanTriVien quanTri, Long idPhongKham, LocalTime gioBatDau,
            LocalTime gioKetThuc, int soLuotToiDaMoiGio, int thoiLuongLuotPhut, NguonThayDoi nguon);

    /**
     * Xếp 1 ca mới cho bác sĩ. Ném TRUNG_LICH_LAM_VIEC nếu trùng giờ, VUOT_GIO_LAM_TUAN nếu vượt giờ làm trong tuần. Không
     * báo bác sĩ: nơi gọi tự báo.
     */
    LichLamViec taoCaMoi(Long idBacSi, Long idPhongKham, QuanTriVien quanTri, LocalDate ngay, LocalTime gioBatDau,
            LocalTime gioKetThuc, int soLuotToiDaMoiGio, int thoiLuongLuotPhut, NguonThayDoi nguon);

    /**
     * Nhường 1 ca đã khoá bằng {@link #khoaCaConSuaDuoc} cho bác sĩ khác (duyệt yêu cầu NHUONG_CA): ca giữ nguyên id,
     * phòng, giờ, lượt khám; lịch hẹn còn hiệu lực chuyển sang bác sĩ mới, được đánh dấu {@code doiBacSi} (bệnh nhân hủy /
     * đổi tự do tới giờ khám) và bệnh nhân được báo. Bác sĩ mới phải đang công tác, cùng chuyên khoa với phòng của ca,
     * không trùng giờ và không vượt giờ làm trong tuần. Ném DU_LIEU_KHONG_HOP_LE, TRUNG_LICH_LAM_VIEC, VUOT_GIO_LAM_TUAN.
     * Không báo bác sĩ: nơi gọi tự báo.
     *
     * @return số lịch hẹn vừa chuyển sang bác sĩ mới
     */
    int doiBacSiCaDaKhoa(LichLamViec ca, QuanTriVien quanTri, Long idBacSiMoi, NguonThayDoi nguon);

    /**
     * Kiểm tra trước, không khoá: bác sĩ nhận thêm ca này có trùng giờ với ca khác của mình, có vượt giờ làm trong tuần
     * không. Chỉ để báo sớm khi gửi / đồng ý yêu cầu nhường ca; lúc duyệt mới kiểm tra thật (sau khi khoá). Ném
     * TRUNG_LICH_LAM_VIEC, VUOT_GIO_LAM_TUAN.
     */
    void kiemTraNhanCa(Long idBacSiNhan, LichLamViec ca);

    /**
     * Sao chép các ca còn hoạt động của khoảng nguồn (lọc theo chuyên khoa, bác sĩ) sang {@code ngayDich}, dán
     * {@code soLan} lần; lần sau cách lần trước số tuần đủ phủ khoảng nguồn, nên thứ trong tuần được giữ nguyên. Phòng,
     * giờ, N, t giữ như ca nguồn. Bản sao vào ngày đã qua / quá số ngày được xếp trước, của bác sĩ ngừng công tác, vào
     * phòng ngừng hoạt động / khác chuyên khoa, trùng giờ hoặc làm tuần vượt giới hạn giờ làm thì bị bỏ qua kèm lý do.
     * Khi lưu: mỗi bác sĩ được báo 1 lần, mỗi ca mới có dòng lịch sử nguồn SAO_CHEP.
     *
     * @param luu false = xem trước: chạy y như khi lưu rồi rollback, không báo ai
     */
    KetQuaSaoChepResponse saoChep(Long idTaiKhoanQuanTri, SaoChepCaRequest request, boolean luu);

    /**
     * Kiểm tra rồi lưu các dòng ca đã đọc từ tệp Excel, theo thứ tự dòng: bác sĩ / phòng phải có trong DB, rồi các quy
     * tắc của xếp ca (sức chứa, ngày, bác sĩ / phòng còn hoạt động, cùng chuyên khoa, trùng giờ, giờ làm trong tuần);
     * dòng trước trong tệp được tính khi xét dòng sau. Mỗi dòng báo lỗi đầu tiên của nó. Tất cả hoặc không: còn lỗi thì
     * không lưu dòng nào. Khi lưu: mỗi bác sĩ được báo 1 lần, mỗi ca mới có dòng lịch sử nguồn NHAP_EXCEL.
     *
     * @param luu false = xem trước: chạy y như khi lưu rồi rollback, không báo ai. true mà còn lỗi -> 400
     *            DU_LIEU_KHONG_HOP_LE (nhapExcelCoLoi), không lưu gì
     */
    KetQuaNhapExcelResponse nhapCa(Long idTaiKhoanQuanTri, List<DongCaNhap> cacDong, boolean luu);

    /** Ca kèm số lượt khám hiện tại (đã flush các thay đổi trong transaction). */
    CaLamViecResponse toResponse(LichLamViec ca);

}
