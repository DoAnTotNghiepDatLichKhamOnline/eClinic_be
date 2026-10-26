package iuh.fit.se.eclinic.booking.service;

import java.time.LocalDate;

import org.springframework.web.multipart.MultipartFile;

import iuh.fit.se.eclinic.booking.dto.response.KetQuaNhapExcelResponse;

/** Xuất / nhập lịch làm việc bằng tệp Excel (.xlsx) cho quản trị viên. */
public interface ExcelLichLamViecService {

    /** Xuất các ca còn hoạt động trong khoảng ngày (tối đa 42 ngày) theo bộ lọc. */
    byte[] xuat(LocalDate tuNgay, LocalDate denNgay, Long idChuyenKhoa, Long idBacSi, Long idPhongKham);

    /** Tệp mẫu nhập ca, kèm hướng dẫn và danh sách chọn bác sĩ, phòng. */
    byte[] mau();

    /** Đọc tệp, kiểm tra từng ô và trùng giờ giữa các dòng, rồi kiểm tra quy tắc xếp ca với DB. */
    KetQuaNhapExcelResponse nhap(Long idTaiKhoanQuanTri, MultipartFile tep, boolean luu);

}
