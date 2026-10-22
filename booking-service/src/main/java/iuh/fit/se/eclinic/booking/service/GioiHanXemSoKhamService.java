package iuh.fit.se.eclinic.booking.service;

/**
 * Giới hạn số lần nhập sai 4 số cuối SĐT khi xem sổ khám bệnh, đếm trên Redis theo hồ sơ bệnh án (khoá
 * {@code so-kham:sai:<idHoSoBenhAn>}, không dùng mã sổ khám làm khoá). Mức giới hạn: {@code app.so-kham.so-lan-sai-toi-da}
 * lần trong {@code khoa-sau}. Redis không dùng được thì bỏ qua giới hạn (mã sổ khám 256 bit vẫn là lớp bảo vệ chính).
 */
public interface GioiHanXemSoKhamService {

    /** Ném GUI_LAI_QUA_NHANH nếu sổ khám đang bị khoá do nhập sai đủ số lần, kể cả khi lần này nhập đúng. */
    void kiemTraKhoa(Long idHoSoBenhAn);

    /** Ghi nhận 1 lần nhập sai; lần sai đầu mở cửa sổ đếm. */
    void ghiNhanSai(Long idHoSoBenhAn);

    /** Nhập đúng: xoá số lần sai đã đếm. */
    void xoa(Long idHoSoBenhAn);

}
