package iuh.fit.se.eclinic.notification.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

import iuh.fit.se.eclinic.common.enums.LoaiThongBao;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 1 thông báo do service khác gửi sang (API nội bộ).
 *
 * @param maNguon    mã duy nhất của sự kiện ở service nguồn, dạng {@code <service>:<id>} (vd {@code booking:15}); gửi lại
 *                   cùng mã thì không tạo thông báo thứ hai
 * @param idTaiKhoan tài khoản nhận
 * @param idLichHen  lịch hẹn liên quan, có thể null
 * @param idYeuCau   yêu cầu đổi ca / xin nghỉ liên quan, có thể null
 * @param khoaMau    khoá mẫu câu trong thong-diep/ (vd {@code thongBao.lichHenMoi}); câu được soạn theo ngôn ngữ người đọc
 * @param thamSo     tham số của mẫu, mảng JSON (xem {@code ThamSoThongBao}); null nếu mẫu không có tham số
 * @param noiDung    câu soạn sẵn, chỉ dùng khi không có {@code khoaMau} (sự kiện ghi trước V18)
 */
public record TaoThongBaoRequest(
        @NotBlank @Size(max = 64) @Pattern(regexp = "[a-z-]+:[0-9]+", message = "{kiemTra.maNguon.dinhDang}")
        String maNguon,
        @NotNull LoaiThongBao loai,
        @NotNull Long idTaiKhoan,
        Long idLichHen,
        Long idYeuCau,
        @Size(max = 80) String khoaMau,
        @Size(max = 4000) String thamSo,
        @Size(max = 2000) String noiDung) {

    @JsonIgnore
    @AssertTrue(message = "{kiemTra.thongBao.thieuNoiDung}")
    public boolean isCoNoiDung() {
        return (khoaMau != null && !khoaMau.isBlank()) || (noiDung != null && !noiDung.isBlank());
    }
}
