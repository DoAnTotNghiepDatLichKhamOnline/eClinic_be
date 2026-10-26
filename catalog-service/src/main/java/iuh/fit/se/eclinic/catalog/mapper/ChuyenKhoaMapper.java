package iuh.fit.se.eclinic.catalog.mapper;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.catalog.dto.request.ChuyenKhoaRequest;
import iuh.fit.se.eclinic.catalog.dto.response.ChuyenKhoaQuanTriResponse;
import iuh.fit.se.eclinic.catalog.dto.response.ChuyenKhoaResponse;
import iuh.fit.se.eclinic.common.entity.catalog.ChuyenKhoa;
import iuh.fit.se.eclinic.common.enums.NgonNgu;

/** Chuyển đổi entity <-> DTO của chuyên khoa. */
@Component
public class ChuyenKhoaMapper {

    /** Bản công khai: tên, mô tả theo ngôn ngữ của request. */
    public ChuyenKhoaResponse toResponse(ChuyenKhoa chuyenKhoa) {
        NgonNgu ngonNgu = NgonNgu.hienTai();
        return new ChuyenKhoaResponse(chuyenKhoa.getId(), chuyenKhoa.ten(ngonNgu), chuyenKhoa.moTa(ngonNgu));
    }

    public ChuyenKhoaQuanTriResponse toQuanTriResponse(ChuyenKhoa chuyenKhoa) {
        return new ChuyenKhoaQuanTriResponse(chuyenKhoa.getId(), chuyenKhoa.getTenChuyenKhoa(),
                chuyenKhoa.getTenChuyenKhoaEn(), chuyenKhoa.getMoTa(), chuyenKhoa.getMoTaEn(),
                chuyenKhoa.getTrieuChung(), chuyenKhoa.getTrieuChungEn());
    }

    public ChuyenKhoa toEntity(ChuyenKhoaRequest request) {
        ChuyenKhoa chuyenKhoa = new ChuyenKhoa();
        ganDuLieu(chuyenKhoa, request);
        return chuyenKhoa;
    }

    public void capNhat(ChuyenKhoa chuyenKhoa, ChuyenKhoaRequest request) {
        ganDuLieu(chuyenKhoa, request);
    }

    /** Bỏ khoảng trắng thừa, chuỗi rỗng thành null. */
    public String chuanHoa(String giaTri) {
        return giaTri == null || giaTri.isBlank() ? null : giaTri.trim();
    }

    /** Chuẩn hoá: bỏ khoảng trắng thừa ở tên; tên tiếng Anh, mô tả rỗng thành null. */
    private void ganDuLieu(ChuyenKhoa chuyenKhoa, ChuyenKhoaRequest request) {
        chuyenKhoa.setTenChuyenKhoa(request.tenChuyenKhoa().trim());
        chuyenKhoa.setTenChuyenKhoaEn(chuanHoa(request.tenChuyenKhoaEn()));
        chuyenKhoa.setMoTa(chuanHoa(request.moTa()));
        chuyenKhoa.setMoTaEn(chuanHoa(request.moTaEn()));
        chuyenKhoa.setTrieuChung(chuanHoa(request.trieuChung()));
        chuyenKhoa.setTrieuChungEn(chuanHoa(request.trieuChungEn()));
    }

}
