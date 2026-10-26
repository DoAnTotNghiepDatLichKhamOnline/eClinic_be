package iuh.fit.se.eclinic.chatbot.dto.response;

import java.util.List;

/** Gợi ý kèm câu trả lời, do server dựng từ kết quả tra cứu để frontend vẽ thẻ bác sĩ, khung giờ. */
public record GoiYResponse(ChuyenKhoa chuyenKhoa, List<BacSi> bacSi, List<LichKham> lichKham,
        List<LichHenCuaToi> lichHenCuaToi, List<LanKham> lanKham) {

    /** Câu trả lời này có kèm dữ liệu cá nhân của người chat (kể cả khi tra cứu ra danh sách rỗng). */
    public boolean coDuLieuCaNhan() {
        return lichHenCuaToi != null || lanKham != null;
    }

    public record ChuyenKhoa(Long id, String ten) {
    }

    public record BacSi(Long id, String hoTen, String hocVi, String chucVu, String anhDaiDien, String ngaySomNhat,
            long soChoConLai) {
    }

    /** 1 ca làm việc còn chỗ. */
    public record LichKham(Long idBacSi, String hoTenBacSi, Long idLichLamViec, String ngay, String tenPhong,
            String tang, List<KhungGio> khungGio) {
    }

    /** 1 lịch hẹn sắp tới của người chat, như 1 dòng "lịch hẹn của tôi" thu gọn. */
    public record LichHenCuaToi(String maPhieuKham, String maTraCuu, String linkPhieuKham, String trangThai, String ngay,
            String gioBatDauKhung, String gioKetThucKhung, String hoTenBacSi, String tenChuyenKhoa, String tenPhong,
            String tang, String hoTenBenhNhan, boolean laBanThan, boolean canDoiLich) {
    }

    /** 1 lượt đã khám của người chat. */
    public record LanKham(String maPhieuKham, String maTraCuu, String ngay, String hoTenBacSi, String tenChuyenKhoa,
            String hoTenBenhNhan, boolean laBanThan, String ngayTaiKhamDeXuat) {
    }

    /** Khung 1 giờ còn chỗ; giờ dạng ISO như API khung giờ ({@code 2026-10-14T08:00:00}). */
    public record KhungGio(String gioBatDau, String gioKetThuc, int soChoConLai) {
    }

}
