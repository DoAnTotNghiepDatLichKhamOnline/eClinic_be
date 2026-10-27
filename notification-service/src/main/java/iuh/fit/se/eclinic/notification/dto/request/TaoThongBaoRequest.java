package iuh.fit.se.eclinic.notification.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

import iuh.fit.se.eclinic.common.enums.LoaiThongBao;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 1 thông báo do service khác gửi sang (API nội bộ). */
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
