package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.request.DoiLichRequest;
import iuh.fit.se.eclinic.booking.dto.request.HuyLichRequest;
import iuh.fit.se.eclinic.booking.dto.response.DatLichResponse;
import iuh.fit.se.eclinic.booking.dto.response.PhieuKhamResponse;

/** Bệnh nhân hủy / đổi lịch hẹn, theo mã phiếu khám. */
public interface HuyDoiLichService {

    /** -> DA_HUY, ghi lý do (nếu có). Trả phiếu khám sau khi hủy. */
    PhieuKhamResponse huyCuaToi(Long idTaiKhoan, String maPhieuKham, String lyDo);

    /** Lịch cũ -> DA_HUY_DO_DOI_LICH, tạo lịch mới DA_XAC_NHAN với phiếu khám mới, trong 1 transaction. */
    DatLichResponse doiCuaToi(Long idTaiKhoan, String maPhieuKham, DoiLichRequest request);

    /** Như {@link #huyCuaToi}, quyền theo {@code request.soDienThoai()}. */
    PhieuKhamResponse huyTheoPhieu(String maPhieuKham, HuyLichRequest request);

    /** Như {@link #doiCuaToi}, quyền theo {@code request.soDienThoai()}. */
    DatLichResponse doiTheoPhieu(String maPhieuKham, DoiLichRequest request);

}
