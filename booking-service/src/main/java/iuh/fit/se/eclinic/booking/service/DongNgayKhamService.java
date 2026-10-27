package iuh.fit.se.eclinic.booking.service;

/** Đóng các ngày khám đã qua: bệnh nhân chưa được khám thì lịch hẹn thành KHONG_DEN. */
public interface DongNgayKhamService {

    /** Lịch DA_XAC_NHAN của ngày đã qua mà bệnh nhân chưa được khám chuyển sang KHONG_DEN. */
    int dongNgayDaQua();

}
