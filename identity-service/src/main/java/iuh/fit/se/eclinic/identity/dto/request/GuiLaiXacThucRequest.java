package iuh.fit.se.eclinic.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Yêu cầu gửi lại liên kết kích hoạt. */
public record GuiLaiXacThucRequest(

        @NotBlank(message = "{kiemTra.email.batBuoc}")
        @Email(message = "{kiemTra.email.dinhDang}")
        @Size(max = 255, message = "{kiemTra.email.toiDa}")
        String email) {
}
