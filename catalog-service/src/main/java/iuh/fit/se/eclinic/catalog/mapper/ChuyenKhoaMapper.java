package iuh.fit.se.eclinic.catalog.mapper;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.catalog.dto.request.ChuyenKhoaRequest;
import iuh.fit.se.eclinic.catalog.dto.response.ChuyenKhoaResponse;
import iuh.fit.se.eclinic.common.entity.catalog.ChuyenKhoa;

/**
 * Chuyển đổi entity <-> DTO của chuyên khoa. Viết tay (không dùng MapStruct) cho dễ đọc, dễ debug.
 */
@Component
public class ChuyenKhoaMapper {

    public ChuyenKhoaResponse toResponse(ChuyenKhoa chuyenKhoa) {
        return new ChuyenKhoaResponse(chuyenKhoa.getId(), chuyenKhoa.getTenChuyenKhoa(), chuyenKhoa.getMoTa());
    }

    public ChuyenKhoa toEntity(ChuyenKhoaRequest request) {
        ChuyenKhoa chuyenKhoa = new ChuyenKhoa();
        ganDuLieu(chuyenKhoa, request);
        return chuyenKhoa;
    }

    public void capNhat(ChuyenKhoa chuyenKhoa, ChuyenKhoaRequest request) {
        ganDuLieu(chuyenKhoa, request);
    }

    /** Chuẩn hoá: bỏ khoảng trắng thừa ở tên, mô tả rỗng thành null. */
    private void ganDuLieu(ChuyenKhoa chuyenKhoa, ChuyenKhoaRequest request) {
        chuyenKhoa.setTenChuyenKhoa(request.tenChuyenKhoa().trim());
        chuyenKhoa.setMoTa(request.moTa() == null || request.moTa().isBlank() ? null : request.moTa().trim());
    }

}
