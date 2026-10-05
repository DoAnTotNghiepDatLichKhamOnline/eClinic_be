package iuh.fit.se.eclinic.booking.service;

import java.util.List;

import iuh.fit.se.eclinic.booking.dto.response.NguoiThanDaLuuResponse;
import iuh.fit.se.eclinic.booking.event.DaDatLichChoNguoiThanEvent;

/**
 * Người thân mà tài khoản đã đặt lịch cho, lưu đúng những gì tài khoản đó nhập để điền sẵn form lần sau.
 */
public interface NguoiThanDaLuuService {

    /** Số người thân tối đa lưu cho 1 tài khoản; vượt thì bỏ người đặt lịch lâu nhất. */
    int SO_NGUOI_TOI_DA = 10;

    /**
     * Thêm hoặc cập nhật người thân theo lần đặt lịch vừa xong, trong transaction RIÊNG (gọi sau khi transaction đặt
     * lịch đã commit).
     */
    void ghiNho(DaDatLichChoNguoiThanEvent event);

    /** Người thân đã lưu của tài khoản, người vừa đặt lịch gần nhất đứng trước. */
    List<NguoiThanDaLuuResponse> cuaTaiKhoan(Long idTaiKhoan);

    /** Bỏ 1 người thân đã lưu. Ném KHONG_TIM_THAY nếu không có hoặc là dòng của tài khoản khác. */
    void xoa(Long idTaiKhoan, Long id);

}
