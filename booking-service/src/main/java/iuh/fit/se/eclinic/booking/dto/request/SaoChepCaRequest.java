package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Sao chép các ca còn hoạt động của khoảng [tuNgay, denNgay] sang khoảng bắt đầu từ {@code ngayDich}, giữ nguyên thứ
 * trong tuần.
 *
 * @param tuNgay      ngày đầu của khoảng nguồn
 * @param denNgay     ngày cuối của khoảng nguồn (tính cả ngày này); khoảng tối đa 42 ngày
 * @param ngayDich    ngày nhận bản sao của {@code tuNgay}: sau {@code denNgay} và cùng thứ với {@code tuNgay}
 * @param soLan       số lần dán khoảng nguồn, lần sau cách lần trước số tuần đủ phủ khoảng nguồn
 * @param idChuyenKhoa null = mọi chuyên khoa
 * @param cacIdBacSi  null hoặc rỗng = mọi bác sĩ
 */
public record SaoChepCaRequest(
        @NotNull LocalDate tuNgay,
        @NotNull LocalDate denNgay,
        @NotNull LocalDate ngayDich,
        @NotNull @Min(1) @Max(12) Integer soLan,
        Long idChuyenKhoa,
        List<@NotNull Long> cacIdBacSi) {
}
