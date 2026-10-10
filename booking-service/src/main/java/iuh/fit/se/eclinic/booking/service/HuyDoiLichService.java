package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.request.DoiLichRequest;
import iuh.fit.se.eclinic.booking.dto.request.HuyLichRequest;
import iuh.fit.se.eclinic.booking.dto.response.DatLichResponse;
import iuh.fit.se.eclinic.booking.dto.response.PhieuKhamResponse;

/**
 * Bệnh nhân hủy / đổi lịch hẹn, theo mã phiếu khám. Hai cách chứng minh quyền:
 * <ul>
 * <li>đã đăng nhập ({@code ...CuaToi}): tài khoản đã đặt lịch này, hoặc người khám là chủ tài khoản (hồ sơ đã liên kết).
 * Lịch tài khoản chỉ thấy vì là người giám hộ theo CCCD: KHONG_CO_QUYEN. Lịch tài khoản không thấy: KHONG_TIM_THAY;</li>
 * <li>cầm link phiếu khám ({@code ...TheoPhieu}): nhập đúng SĐT liên hệ của lượt khám. Thiếu / sai dạng:
 * DU_LIEU_KHONG_HOP_LE; không khớp: SO_DIEN_THOAI_KHONG_KHOP. Mã phiếu khám không có: KHONG_TIM_THAY.</li>
 * </ul>
 * Chung cho cả hai: chỉ lịch CHO_XAC_NHAN / DA_XAC_NHAN (khác: LICH_HEN_KHONG_HUY_DOI_DUOC) và còn cách giờ khám ít nhất
 * {@code app.dat-lich.huy-doi-truoc-toi-thieu} (khác: QUA_HAN_HUY_DOI_LICH). Lượt khám cũ được mở lại cho người khác đặt.
 */
public interface HuyDoiLichService {

    /** -> DA_HUY, ghi lý do (nếu có). Trả phiếu khám sau khi hủy. */
    PhieuKhamResponse huyCuaToi(Long idTaiKhoan, String maPhieuKham, String lyDo);

    /**
     * Lịch cũ -> DA_HUY_DO_DOI_LICH, tạo lịch mới CHO_XAC_NHAN với phiếu khám mới, trong 1 transaction. Ném thêm
     * VUOT_SO_LAN_DOI_LICH và các mã của {@link DatLichService#datLaiTuLichCu}.
     */
    DatLichResponse doiCuaToi(Long idTaiKhoan, String maPhieuKham, DoiLichRequest request);

    /** Như {@link #huyCuaToi}, quyền theo {@code request.soDienThoai()}. */
    PhieuKhamResponse huyTheoPhieu(String maPhieuKham, HuyLichRequest request);

    /** Như {@link #doiCuaToi}, quyền theo {@code request.soDienThoai()}. */
    DatLichResponse doiTheoPhieu(String maPhieuKham, DoiLichRequest request);

}
