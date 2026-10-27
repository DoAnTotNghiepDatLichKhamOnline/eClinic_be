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

/** Quản trị viên xếp, sửa, hủy ca làm việc. */
public interface QuanLyCaService {

    /** Xếp 1 ca, hoặc nhiều ca cùng giờ khi có {@code lapLai}. */
    KetQuaTaoCaResponse tao(Long idTaiKhoanQuanTri, TaoCaRequest request);

    CaLamViecResponse sua(Long idTaiKhoanQuanTri, Long idLichLamViec, SuaCaRequest request);

    KetQuaHuyCaResponse huy(Long idTaiKhoanQuanTri, Long idLichLamViec, String lyDo);

    /** Như {@link #huy} nhưng không báo bác sĩ: dùng khi hủy hàng loạt ca của bác sĩ ngừng công tác. */
    int huyKhongBaoBacSi(Long idTaiKhoanQuanTri, Long idLichLamViec, String lyDo);

    /** Lịch hẹn còn hiệu lực đang chờ bệnh nhân đổi lịch vì ca khám bị hủy, giờ khám cũ sớm nhất trước. */
    TrangDuLieu<LichHenTrongCaResponse> lichHenCanDoi(int trang, int kichThuoc);

    // ----- Các bước dùng lại khi duyệt yêu cầu đổi ca / xin nghỉ; phải gọi trong transaction của nơi gọi -----

    /** Hồ sơ quản trị viên của tài khoản đang đăng nhập. Ném KHONG_CO_QUYEN nếu tài khoản không có hồ sơ này. */
    QuanTriVien layQuanTriVien(Long idTaiKhoan);

    /** Khoá dòng ca và kiểm tra ca còn sửa / hủy được. Ném KHONG_TIM_THAY, CA_KHONG_SUA_DUOC. */
    LichLamViec khoaCaConSuaDuoc(Long idLichLamViec);

    /** Hủy ca đã khoá: hủy lượt khám, đánh dấu và báo lịch hẹn còn hiệu lực, đóng yêu cầu đang chờ. */
    int huyCaDaKhoa(LichLamViec ca, QuanTriVien quanTri, LyDoHuyCa lyDo, NguonThayDoi nguon);

    /** Sửa 1 ca đã khoá (cùng quy tắc với {@link #sua}). */
    boolean suaCaDaKhoa(LichLamViec ca, QuanTriVien quanTri, Long idPhongKham, LocalTime gioBatDau,
            LocalTime gioKetThuc, int soLuotToiDaMoiGio, int thoiLuongLuotPhut, NguonThayDoi nguon);

    /** Xếp 1 ca mới cho bác sĩ. */
    LichLamViec taoCaMoi(Long idBacSi, Long idPhongKham, QuanTriVien quanTri, LocalDate ngay, LocalTime gioBatDau,
            LocalTime gioKetThuc, int soLuotToiDaMoiGio, int thoiLuongLuotPhut, NguonThayDoi nguon);

    /** Nhường ca đã khoá cho bác sĩ khác: lịch hẹn còn hiệu lực chuyển sang bác sĩ mới và bệnh nhân được báo. */
    int doiBacSiCaDaKhoa(LichLamViec ca, QuanTriVien quanTri, Long idBacSiMoi, NguonThayDoi nguon);

    /** Kiểm tra trước, không khoá: bác sĩ nhận ca có trùng giờ hay vượt giờ làm trong tuần không. */
    void kiemTraNhanCa(Long idBacSiNhan, LichLamViec ca);

    /** Ném NGAY_BENH_VIEN_NGHI (kèm ngày và lý do) nếu bệnh viện nghỉ ngày {@code ngay}. Không khoá. */
    void kiemTraNgayKhongNghi(LocalDate ngay);

    /** Khoá xếp ca toàn viện, chế độ chia sẻ: khoá chia sẻ dòng cấu hình hệ thống tới hết transaction. */
    void giuKhoaXepCa();

    /** Sao chép ca của khoảng nguồn sang {@code ngayDich} {@code soLan} lần, giữ nguyên thứ trong tuần. */
    KetQuaSaoChepResponse saoChep(Long idTaiKhoanQuanTri, SaoChepCaRequest request, boolean luu);

    /** Kiểm tra rồi lưu các dòng ca đọc từ Excel theo thứ tự; dòng trước được tính khi xét dòng sau. */
    KetQuaNhapExcelResponse nhapCa(Long idTaiKhoanQuanTri, List<DongCaNhap> cacDong, boolean luu);

    /** Ca kèm số lượt khám hiện tại (đã flush các thay đổi trong transaction). */
    CaLamViecResponse toResponse(LichLamViec ca);

}
