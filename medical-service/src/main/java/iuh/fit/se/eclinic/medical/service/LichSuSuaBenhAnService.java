package iuh.fit.se.eclinic.medical.service;

import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;
import iuh.fit.se.eclinic.medical.dto.response.LichSuSuaBenhAnResponse;
import iuh.fit.se.eclinic.medical.dto.response.NoiDungBenhAn;

/** Lịch sử sửa hồ sơ bệnh án: chỉ thêm, không sửa, không xoá. */
public interface LichSuSuaBenhAnService {

    /** Thêm 1 dòng lịch sử cho lần sửa vừa ghi, trong transaction của lần sửa. */
    void ghi(HoSoBenhAn benhAn, BacSi bacSi, String lyDo, NoiDungBenhAn truoc, NoiDungBenhAn sau);

    /** Lịch sử sửa của 1 lượt khám, khi lượt khám là của mình hoặc mình có lịch hẹn sau với cùng bệnh nhân. */
    LichSuSuaBenhAnResponse cuaBacSi(Long idTaiKhoanBacSi, Long idLichHen);

    /** Lịch sử sửa của lượt khám {@code idLichHen}, cho quản trị viên. Ném KHONG_TIM_THAY nếu chưa có hồ sơ bệnh án. */
    LichSuSuaBenhAnResponse choQuanTri(Long idLichHen);

}
