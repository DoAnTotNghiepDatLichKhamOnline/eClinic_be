package iuh.fit.se.eclinic.booking.dto.response;

import java.util.List;

/** Kết quả quản trị viên duyệt / từ chối cả nhóm yêu cầu xin nghỉ nhiều ca. */
public record KetQuaXuLyNhomResponse(List<YeuCauDoiLichResponse> daXuLy, List<BoQua> boQua) {

    public record BoQua(Long idYeuCau, String lyDo) {
    }
}
