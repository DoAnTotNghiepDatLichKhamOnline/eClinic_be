package iuh.fit.se.eclinic.medical.service;

import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;
import iuh.fit.se.eclinic.medical.dto.response.LichSuSuaBenhAnResponse;
import iuh.fit.se.eclinic.medical.dto.response.NoiDungBenhAn;

/**
 * Lịch sử sửa hồ sơ bệnh án: chỉ thêm, không sửa, không xoá. Người xem: bác sĩ mở được hồ sơ khám có lượt khám đó và
 * quản trị viên; bệnh nhân chỉ thấy {@code ngaySuaCuoi} trong kết quả khám.
 */
public interface LichSuSuaBenhAnService {

    /** Thêm 1 dòng lịch sử cho lần sửa vừa đưa hồ sơ lên {@code benhAn.getPhienBan()}; gọi trong transaction của lần sửa. */
    void ghi(HoSoBenhAn benhAn, BacSi bacSi, String lyDo, NoiDungBenhAn truoc, NoiDungBenhAn sau);

    /**
     * Lịch sử sửa của lượt khám {@code idLichHen}, cho bác sĩ đang đăng nhập: được xem khi lượt khám là của mình, hoặc
     * mình có lịch hẹn với cùng bệnh nhân từ giờ khám đó trở đi (như danh sách lần khám trước trong hồ sơ khám). Không
     * được xem, lịch hẹn không có hoặc chưa có hồ sơ bệnh án đều ném KHONG_TIM_THAY.
     */
    LichSuSuaBenhAnResponse cuaBacSi(Long idTaiKhoanBacSi, Long idLichHen);

    /** Lịch sử sửa của lượt khám {@code idLichHen}, cho quản trị viên. Ném KHONG_TIM_THAY nếu chưa có hồ sơ bệnh án. */
    LichSuSuaBenhAnResponse choQuanTri(Long idLichHen);

}
