package iuh.fit.se.eclinic.booking.mapper;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.booking.dto.request.CapNhatCauHinhRequest;
import iuh.fit.se.eclinic.booking.dto.response.CauHinhQuanTriResponse;
import iuh.fit.se.eclinic.booking.dto.response.CauHinhQuanTriResponse.QuanTriThucHien;
import iuh.fit.se.eclinic.booking.dto.response.LichSuCauHinhResponse;
import iuh.fit.se.eclinic.booking.dto.response.QuyTacDatLichResponse;
import iuh.fit.se.eclinic.booking.service.CauHinhHienTai;
import iuh.fit.se.eclinic.common.entity.booking.CauHinhHeThong;
import iuh.fit.se.eclinic.common.entity.booking.LichSuCauHinh;
import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;

/** Chuyển cấu hình hệ thống và lịch sử sửa cấu hình sang DTO. Gọi trong transaction (quản trị viên lấy lười). */
@Component
public class CauHinhMapper {

    public CauHinhQuanTriResponse toQuanTriResponse(CauHinhHeThong dong) {
        return new CauHinhQuanTriResponse(dong.getDatTruocToiThieuPhut(), dong.getSoNgayDatTruocToiDa(),
                dong.getSoLichHieuLucToiDaMoiHoSo(), dong.getSoLichHieuLucToiDaMoiSoDienThoai(),
                dong.getHuyDoiTruocToiThieuPhut(), dong.getSoLanDoiLichToiDa(), dong.getGuiYeuCauTruocToiThieuGio(),
                dong.getSoNgayXepTruocToiDa(), dong.getSoGioToiDaMoiTuan(), dong.getSoNgayDuocSuaDanhGia(),
                dong.getPhienBan(), dong.getNgaySua(), quanTri(dong.getQuanTriSua()));
    }

    public LichSuCauHinhResponse toLichSuResponse(LichSuCauHinh dong) {
        return new LichSuCauHinhResponse(dong.getId(), dong.getTenTruong(), dong.getGiaTriCu(), dong.getGiaTriMoi(),
                dong.getNgayTao(), quanTri(dong.getQuanTri()));
    }

    public QuyTacDatLichResponse toQuyTacDatLich(CauHinhHienTai cauHinh) {
        return new QuyTacDatLichResponse(cauHinh.datTruocToiThieuPhut(), cauHinh.soNgayDatTruocToiDa(),
                cauHinh.huyDoiTruocToiThieuPhut(), cauHinh.soLanDoiLichToiDa(), cauHinh.soLichHieuLucToiDaMoiHoSo());
    }

    /** Request đã qua {@code @Valid} nên không trường nào null. */
    public CauHinhHienTai toCauHinh(CapNhatCauHinhRequest request) {
        return new CauHinhHienTai(request.datTruocToiThieuPhut(), request.soNgayDatTruocToiDa(),
                request.soLichHieuLucToiDaMoiHoSo(), request.soLichHieuLucToiDaMoiSoDienThoai(),
                request.huyDoiTruocToiThieuPhut(), request.soLanDoiLichToiDa(), request.guiYeuCauTruocToiThieuGio(),
                request.soNgayXepTruocToiDa(), request.soGioToiDaMoiTuan(), request.soNgayDuocSuaDanhGia());
    }

    /** Ghi 10 giá trị quy tắc vào dòng cấu hình (không đụng phiên bản, người sửa). */
    public void capNhat(CauHinhHeThong dong, CauHinhHienTai giaTri) {
        dong.setDatTruocToiThieuPhut(giaTri.datTruocToiThieuPhut());
        dong.setSoNgayDatTruocToiDa(giaTri.soNgayDatTruocToiDa());
        dong.setSoLichHieuLucToiDaMoiHoSo(giaTri.soLichHieuLucToiDaMoiHoSo());
        dong.setSoLichHieuLucToiDaMoiSoDienThoai(giaTri.soLichHieuLucToiDaMoiSoDienThoai());
        dong.setHuyDoiTruocToiThieuPhut(giaTri.huyDoiTruocToiThieuPhut());
        dong.setSoLanDoiLichToiDa(giaTri.soLanDoiLichToiDa());
        dong.setGuiYeuCauTruocToiThieuGio(giaTri.guiYeuCauTruocToiThieuGio());
        dong.setSoNgayXepTruocToiDa(giaTri.soNgayXepTruocToiDa());
        dong.setSoGioToiDaMoiTuan(giaTri.soGioToiDaMoiTuan());
        dong.setSoNgayDuocSuaDanhGia(giaTri.soNgayDuocSuaDanhGia());
    }

    private static QuanTriThucHien quanTri(QuanTriVien quanTri) {
        return quanTri == null ? null : new QuanTriThucHien(quanTri.getId(), quanTri.getTaiKhoan().getHoTen());
    }

}
