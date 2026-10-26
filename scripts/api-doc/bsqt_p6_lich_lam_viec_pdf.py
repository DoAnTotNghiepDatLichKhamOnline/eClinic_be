"""Tài liệu 2, bản 1.1 (DOANTOTNGH-13): route 5.5, bác sĩ tải bản in PDF lịch làm việc của mình, và dòng của nó ở bảng
route mục 2. Các route xếp lịch làm việc khác của DOANTOTNGH-13 (sao chép, Excel, bản in của quản trị viên, gửi lịch, nhường
ca, xin nghỉ nhiều ca) vẫn ngoài phạm vi, có ở docs/BAN-GIAO-FRONTEND-05-10.md. Chạy qua bsqt_build.py.
"""

import bsqt_p2_khung
import kit
from bsqt_p3_lich_hang_doi import BS, KHOANG, LOI_KHOANG, QUYEN, SERVICE
from p3_dat_lich import loi, tham_so

PHIEN_BAN = "1.1 — 19/10/2026"
ROUTE = ("5.5", "GET", f"{BS}/lich-lam-viec/pdf", "ep_bs_lich_lam_viec_pdf", "Tải bản in PDF lịch làm việc của tôi")
LICH_SU = (PHIEN_BAN, "Thêm 5.5: bác sĩ tải bản in PDF lịch làm việc của mình (23 route). Tài liệu đi kèm TK nay là bản "
                      "2.4.")
PHAM_VI_CU = ("22 route của phần lịch hẹn và khám bệnh phía bác sĩ (19 route) và quản trị viên (3 route). "
              "booking-service 15 route (bác sĩ 13, quản trị viên 2): ca làm việc của bác sĩ, lịch hẹn, tra cứu lịch "
              "hẹn theo mã, bác sĩ hủy lịch, hàng đợi khám, hồ sơ khám, sửa thông tin bệnh nhân, đối chiếu giấy tờ. "
              "medical-service 7 route (bác sĩ 6, quản trị viên 1): ghi, sửa, xem hồ sơ bệnh án, lịch sử sửa, gợi ý "
              "thuốc và mã ICD-10")
PHAM_VI = ("23 route của phần lịch hẹn và khám bệnh phía bác sĩ (20 route) và quản trị viên (3 route). "
           "booking-service 16 route (bác sĩ 14, quản trị viên 2): ca làm việc của bác sĩ và bản in PDF, lịch hẹn, tra "
           "cứu lịch hẹn theo mã, bác sĩ hủy lịch, hàng đợi khám, hồ sơ khám, sửa thông tin bệnh nhân, đối chiếu giấy "
           "tờ. medical-service 7 route (bác sĩ 6, quản trị viên 1): ghi, sửa, xem hồ sơ bệnh án, lịch sử sửa, gợi ý "
           "thuốc và mã ICD-10")
NGOAI_PHAM_VI_CU = ("Xếp lịch làm việc: quản trị viên tạo, sửa, hủy ca làm việc; lịch hẹn cần đổi khi ca bị hủy; yêu cầu "
                    "đổi ca và xin nghỉ của bác sĩ; danh sách phòng khám.")
NGOAI_PHAM_VI = ("Xếp lịch làm việc: quản trị viên tạo, sửa, hủy, sao chép ca, nhập / xuất Excel, bản in PDF, gửi lịch "
                 "cho bác sĩ, giờ làm theo tuần, lịch sử ca; lịch hẹn cần đổi khi ca bị hủy; yêu cầu đổi ca, xin nghỉ "
                 "(1 hoặc nhiều ca) và nhường ca của bác sĩ; danh sách phòng khám. Các route này có ở "
                 "docs/BAN-GIAO-FRONTEND-05-10.md trong mã nguồn backend.")


def muc_5_5(id_bookmark):
    muc, method, duong, bm, _ = ROUTE
    return kit.route(muc, "Tải bản in PDF lịch làm việc của tôi", method, duong, bm, id_bookmark, QUYEN,
                     "Tệp PDF (A4 dọc) các ca còn hoạt động của bác sĩ trong khoảng ngày, theo ngày rồi giờ bắt đầu: "
                     "ngày kèm thứ, giờ, phòng và tầng, cuối bảng là tổng số ca và số giờ. Chữ theo `Accept-Language` "
                     "của request. Quản trị viên tải được đúng tệp này cho từng bác sĩ (ngoài phạm vi tài liệu).", [
                         tham_so(KHOANG),
                         kit.nhan("RESPONSE THÀNH CÔNG — 200 OK"),
                         kit.doan("Thân response là **tệp PDF**, không phải JSON: `Content-Type: application/pdf`, "
                                  "`Content-Disposition: attachment; filename=\"lich-lam-viec_BS0001_2026-10-19_"
                                  "2026-11-15.pdf\"` (mã bác sĩ, `tuNgay`, `denNgay`). Không có ca nào thì vẫn trả tệp, "
                                  "trong tệp ghi không có ca."),
                         loi([LOI_KHOANG]),
                         kit.luu_y(["Lỗi vẫn trả JSON như mọi route khác (3.1 của TK), nên frontend kiểm tra status "
                                    "trước khi lưu tệp.",
                                    "Tải bằng `fetch` kèm access token rồi lưu `blob` (thẻ `<a href>` thường không gửi "
                                    "được token). Tên tệp lấy từ header `Content-Disposition`; api-gateway đã cho "
                                    "trình duyệt đọc header này (CORS).",
                                    "Ca đã hủy không có trong tệp; 5.1 vẫn liệt kê chúng."])])


def sua(tl):
    # --- Trang đầu, mục 1 ---
    cu = ">1.0 — 16/10/2026</w:t>"
    vi_tri = tl.xml.index(cu)  # ô Phiên bản của trang đầu (đứng trước bảng lịch sử)
    tl.xml = tl.xml[:vi_tri] + f">{PHIEN_BAN}</w:t>" + tl.xml[vi_tri + len(cu):]
    tl.thay_noi_dung(PHAM_VI_CU, PHAM_VI, trong_bang=True)
    tl.thay_noi_dung(NGOAI_PHAM_VI_CU, NGOAI_PHAM_VI)
    assert tl.xml.count("bản 2.3, gọi tắt là") == 2, "Cần đúng 2 chỗ nêu bản của TK"
    tl.xml = tl.xml.replace("bản 2.3, gọi tắt là", "bản 2.4, gọi tắt là")
    tl.them_dong("1. Giới thiệu và phạm vi", "LỊCH SỬ PHIÊN BẢN", kit.dong_bang(bsqt_p2_khung.RONG_PHIEN_BAN, LICH_SU))

    # --- Mục 2, mục 5 ---
    muc, method, duong, bm, chuc_nang = ROUTE
    tl.them_dong(bsqt_p2_khung.MUC_2, None,
                 bsqt_p2_khung.dong_route(muc, method, duong, bm, "BAC_SI", chuc_nang, SERVICE), sau_dong_chua="5.4")
    vi_tri = tl.vi_tri_tieu_de("6. Hàng đợi khám")
    tl.xml = tl.xml[:vi_tri] + muc_5_5(tl.id_bookmark_moi()) + tl.xml[vi_tri:]
