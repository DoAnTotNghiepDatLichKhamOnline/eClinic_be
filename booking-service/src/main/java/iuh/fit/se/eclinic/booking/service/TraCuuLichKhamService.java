package iuh.fit.se.eclinic.booking.service;

import java.time.LocalDate;
import java.util.List;

import iuh.fit.se.eclinic.booking.dto.response.CaKhamResponse;
import iuh.fit.se.eclinic.booking.dto.response.KhungGioGopResponse;
import iuh.fit.se.eclinic.booking.dto.response.NgayConChoResponse;
import iuh.fit.se.eclinic.booking.dto.response.NgaySomNhatResponse;

/** Xem khung giờ khám khi đặt lịch. */
public interface TraCuuLichKhamService {

    /** Các ca của ngày kèm khung 1 giờ. */
    List<CaKhamResponse> timKhungGioTheoNgay(LocalDate ngay, Long idBacSi, Long idChuyenKhoa);

    /** Các ngày có ca trong khoảng, kèm số chỗ còn lại. */
    List<NgayConChoResponse> timNgayConCho(LocalDate tuNgay, LocalDate denNgay, Long idBacSi, Long idChuyenKhoa);

    /** Khung giờ của 1 bác sĩ trên tối đa 7 ngày liền; mặc định từ hôm nay. */
    List<CaKhamResponse> timKhungGioNhieuNgay(Long idBacSi, LocalDate tuNgay, LocalDate denNgay);

    /** Khung giờ của chuyên khoa trong 1 ngày, gộp các khung cùng giờ của mọi bác sĩ. */
    List<KhungGioGopResponse> timKhungGioGop(Long idChuyenKhoa, LocalDate ngay);

    /** Ngày còn chỗ sớm nhất của từng bác sĩ thuộc chuyên khoa, trong hạn đặt trước tối đa. */
    List<NgaySomNhatResponse> timNgaySomNhat(Long idChuyenKhoa);

}
