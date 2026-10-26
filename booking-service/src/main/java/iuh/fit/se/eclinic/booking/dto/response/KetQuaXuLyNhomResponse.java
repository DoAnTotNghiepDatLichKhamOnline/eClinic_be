package iuh.fit.se.eclinic.booking.dto.response;

import java.util.List;

/**
 * Kết quả quản trị viên duyệt / từ chối cả nhóm yêu cầu xin nghỉ nhiều ca.
 *
 * @param daXuLy các yêu cầu vừa được duyệt / từ chối, theo ngày của ca
 * @param boQua  các yêu cầu còn chờ duyệt nhưng không duyệt được (ca đã bắt đầu), kèm lý do; vẫn ở CHO_DUYET
 */
public record KetQuaXuLyNhomResponse(List<YeuCauDoiLichResponse> daXuLy, List<BoQua> boQua) {

    public record BoQua(Long idYeuCau, String lyDo) {
    }
}
