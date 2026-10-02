package iuh.fit.se.eclinic.booking.service;

import java.util.Optional;

import iuh.fit.se.eclinic.booking.dto.request.HoSoCuaToiRequest;
import iuh.fit.se.eclinic.booking.dto.response.HoSoCuaToiResponse;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;

public interface HoSoBenhNhanService {

    HoSoBenhNhan layTheoId(Long id);

    Optional<HoSoBenhNhan> timTheoTaiKhoanId(Long taiKhoanId);

    Optional<HoSoBenhNhan> timTheoCccd(String cccd);

    /**
     * PAT-01: hồ sơ bệnh nhân của tài khoản đang đăng nhập.
     * <p>
     * Ném: KHONG_TIM_THAY (tài khoản chưa có hồ sơ) và các mã của {@link TaiKhoanService#layBenhNhanDangHoatDong}.
     */
    HoSoCuaToiResponse xemCuaToi(Long idTaiKhoan);

    /**
     * PAT-01: tài khoản chưa có hồ sơ thì tạo hồ sơ gắn vào tài khoản (CCCD bắt buộc và phải là số chưa có hồ sơ); đã có
     * thì sửa họ tên, ngày sinh, giới tính, SĐT, địa chỉ, số BHYT, tiền sử bệnh lý. Số CCCD không đổi được.
     * <p>
     * Ném: DU_LIEU_KHONG_HOP_LE (thiếu CCCD khi tạo, CCCD khác số đang lưu), CCCD_DA_CO_HO_SO, HO_SO_CHO_XAC_MINH và các
     * mã của {@link TaiKhoanService#layBenhNhanDangHoatDong}.
     */
    HoSoCuaToiResponse luuCuaToi(Long idTaiKhoan, HoSoCuaToiRequest request);

}
