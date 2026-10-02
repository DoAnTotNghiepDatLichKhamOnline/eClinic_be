package iuh.fit.se.eclinic.booking.mapper;

import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.booking.dto.response.HoSoCuaToiResponse;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;

@Component
public class HoSoBenhNhanMapper {

    /** Hồ sơ chờ xác minh chỉ trả trạng thái: chưa xác minh thì chủ tài khoản chưa được xem thông tin hồ sơ. */
    public HoSoCuaToiResponse toHoSoCuaToi(HoSoBenhNhan hoSo) {
        if (hoSo.getTrangThaiLienKet() == TrangThaiLienKet.CHO_XAC_MINH) {
            return new HoSoCuaToiResponse(TrangThaiLienKet.CHO_XAC_MINH, null, null, null, null, null, null, null,
                    null);
        }
        return new HoSoCuaToiResponse(hoSo.getTrangThaiLienKet(), hoSo.getCccd(), hoSo.getHoTen(),
                hoSo.getNgaySinh(), hoSo.getGioiTinh(), hoSo.getSoDienThoai(), hoSo.getDiaChi(),
                hoSo.getSoBaoHiemYTe(), hoSo.getTienSuBenhLy());
    }

}
