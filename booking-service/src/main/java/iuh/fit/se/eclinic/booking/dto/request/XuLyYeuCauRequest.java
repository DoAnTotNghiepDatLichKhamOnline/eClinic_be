package iuh.fit.se.eclinic.booking.dto.request;

import jakarta.validation.constraints.Size;

/** Ghi chú của quản trị viên khi duyệt / từ chối yêu cầu đổi lịch (SCHED-03); bắt buộc khi từ chối. */
public record XuLyYeuCauRequest(@Size(max = 1000) String ghiChu) {
}
