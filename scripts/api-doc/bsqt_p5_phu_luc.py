"""Tài liệu 2, giai đoạn 5: Phụ lục A (mã lỗi riêng của tài liệu này), Phụ lục B (luồng gọi mẫu), Phụ lục C (tài khoản và
dữ liệu để thử). Chạy qua bsqt_build.py, sau mọi giai đoạn thêm route.

Phụ lục A đọc thẳng từ mã nguồn (MaLoi.java, thong-diep/*.properties) và từ Phụ lục A của TK: mỗi mã thân bài nhắc tới mà
TK Phụ lục A chưa có thì có 1 dòng; mã TK đã có thì chỉ nêu tên và dẫn TK (không lặp TK).
"""

import re

import bsqt_p2_khung
import check
import kit

PHU_LUC_A = "Phụ lục A. Mã lỗi riêng của tài liệu này"
PHU_LUC_B = "Phụ lục B. Luồng gọi mẫu"
PHU_LUC_C = "Phụ lục C. Tài khoản và dữ liệu để thử"

# (tiêu đề, các bước) của các luồng ở Phụ lục B
LUONG = (
    ("B.1 Một ngày khám của bác sĩ", [
        "Đăng nhập bằng tài khoản bác sĩ (TK 5.4; lần đầu dùng mật khẩu mặc định: TK 5.11). "
        "`GET /api/booking/bac-si/toi/lich-lam-viec` (5.1) cho các ca của tuần, `GET .../hang-doi` (6.1) cho hàng đợi "
        "hôm nay; gọi lại 6.1 định kỳ khi màn hình đang mở (3.5).",
        "Gọi bệnh nhân: `POST .../hang-doi/goi-tiep?idLichLamViec=` (6.2) cho người có số thứ tự nhỏ nhất đang chờ, "
        "hoặc `POST .../lich-hen/{id}/goi-kham` (6.3) cho 1 người cụ thể. Đang có người khác `DANG_KHAM`: 409 "
        "`DANG_KHAM_BENH_NHAN_KHAC`; hết người chờ: 409 `HANG_DOI_TRONG`. Bệnh nhân không có mặt: "
        "`POST .../lich-hen/{id}/vang-mat` (6.4), gọi lại người đó sau bằng 6.3.",
        "Mở hồ sơ khám: `GET .../lich-hen/{id}/ho-so-kham` (7.3), hoặc quét QR trên phiếu rồi "
        "`GET .../lich-hen/tra-cuu/ho-so-kham?ma=` (7.2). `lichHen.doiChieu.canDoiChieu` = `true`: xem giấy tờ, sửa "
        "thông tin bệnh nhân nếu cần (7.4), rồi `POST .../lich-hen/{id}/da-doi-chieu` (7.5).",
        "Màn hình khám: điền sẵn `lyDoKham` của lịch hẹn; khi bác sĩ gõ, gọi `GET /api/medical/bac-si/toi/thuoc` (8.5) "
        "và `GET .../icd10` (8.6). Lưu: `POST /api/medical/bac-si/toi/lich-hen/{idLichHen}/benh-an` (8.1) → 201, lịch "
        "hẹn thành `DA_HOAN_THANH`, hàng đợi hiện `DA_KHAM`. Thuốc bị ngừng hoặc chưa xác minh trong lúc kê: 409 "
        "`THUOC_NGUNG_DUNG` / `THUOC_CHUA_XAC_MINH`, bỏ thuốc đó rồi lưu lại.",
        "Sửa trong hạn sửa (`hanSua`): `PUT .../benh-an` (8.2) với toàn bộ nội dung, `lyDoSua` và `phienBan` đang xem → "
        "200, `phienBan` tăng 1. Nhận 409 `DU_LIEU_DA_THAY_DOI`: tải lại bằng `GET .../benh-an` (8.3) rồi sửa lại; quá "
        "hạn: 409 `HET_HAN_SUA_BENH_AN`, chỉ còn xem. Các lần sửa: `GET .../benh-an/lich-su` (8.4).",
    ]),
    ("B.2 Bác sĩ hủy 1 lịch hẹn sắp tới", [
        "`GET /api/booking/bac-si/toi/lich-hen/lich?tuNgay=&denNgay=` (5.3, tối đa 42 ngày) cho màn hình lịch; bấm "
        "vào 1 lịch hẹn `DA_XAC_NHAN`.",
        "`POST .../lich-hen/{id}/huy` (5.4) với `lyDo` → 200, lịch hẹn thành `BI_TU_CHOI` và lượt khám được trả lại. "
        "Đã tới giờ khám: 409 `LICH_HEN_DA_QUA_GIO`; lịch đang chờ đổi lịch: 409 `LICH_HEN_CAN_DOI_LICH`; lịch không "
        "còn `DA_XAC_NHAN`: 409 `LICH_HEN_KHONG_HUY_DOI_DUOC` (3.4).",
        "Bệnh nhân nhận thông báo và email khi notification-service chạy (3.2, TK 3.13); route vẫn thành công khi "
        "service đó tắt.",
    ]),
    ("B.3 Quản trị viên tra phiếu khám và xem lịch sử sửa", [
        "Đăng nhập bằng tài khoản quản trị viên (TK 5.4). Bệnh nhân đọc mã trên phiếu khám, hoặc gửi ảnh QR: "
        "`GET /api/booking/quan-tri/lich-hen/tra-cuu?ma=` (9.1) → lịch hẹn kèm bác sĩ, phòng khám, trạng thái.",
        "`doiChieu.canDoiChieu` = `true`: đặt `benhNhan` cạnh `doiChieu`, xem giấy tờ, rồi "
        "`POST /api/booking/quan-tri/lich-hen/{id}/da-doi-chieu` (9.2) → 200. Thông tin bệnh nhân sai thì nhờ bác sĩ "
        "của lịch hẹn sửa (7.4).",
        "Lịch hẹn `DA_HOAN_THANH`: `GET /api/medical/quan-tri/lich-hen/{idLichHen}/benh-an/lich-su` (9.3) → các lần bác "
        "sĩ sửa hồ sơ bệnh án, ai sửa, lúc nào, lý do, nội dung trước và sau.",
    ]),
)

DU_LIEU = [
    "**Danh mục thuốc:** 16 thuốc, tất cả đang dùng và đã xác minh, nên thuốc nào cũng tìm được bằng 8.5 và kê được, "
    "không gặp `THUOC_NGUNG_DUNG` / `THUOC_CHUA_XAC_MINH`. Thử 2 mã đó cần quản trị viên đổi trạng thái thuốc "
    "(quản lý danh mục thuốc, ngoài phạm vi tài liệu này).",
    "**Mã ICD-10:** 191 mã phổ biến (tên tiếng Việt và tiếng Anh) có sẵn trong CSDL, không phụ thuộc `SEED_DATA`.",
    "**Chưa có lịch hẹn, hồ sơ bệnh án:** đặt lịch như TK B.7 hoặc TK B.8 vào 1 ca **hôm nay** của bác sĩ muốn thử "
    "(khung giờ còn chỗ, chưa quá hạn đặt của TK 3.6). Lịch đặt xong đã là `DA_XAC_NHAN` (TK 3.11), nên có ngay trong "
    "hàng đợi (mục 6) của bác sĩ đó và ghi hồ sơ bệnh án (8.1) được. Lịch ở ngày sau thì gọi khám trả 409 "
    "`KHONG_PHAI_NGAY_KHAM`, ghi hồ sơ bệnh án trả 409 `CHUA_DEN_NGAY_KHAM`; mục 5, 7, 9 và hủy lịch (5.4) vẫn thử "
    "được.",
]


def phu_luc_a(tl, sau):
    """Dòng cho mã thân bài (và phần `sau` Phụ lục A) nhắc mà TK Phụ lục A chưa có; câu dẫn TK cho mã TK đã có."""
    code = check.ma_loi_trong_code()
    chu_tk = kit.dump(check.GOC / kit.TAI_LIEU)
    a_tk = chu_tk[chu_tk.index("[H1] Phụ lục A"):chu_tk.index("[H1] Phụ lục B")]
    co_o_tk = set(re.findall(r"^\| ([A-Z][A-Z_0-9]+) \| \d{3} \|", a_tk, re.M))
    nhac = [ma for ma in code if re.search(rf"\b{ma}\b", tl.xml + sau)]
    dung_chung = [ma for ma in nhac if ma in co_o_tk]
    rieng = [ma for ma in nhac if ma not in co_o_tk]
    x = kit.tieu_de(1, PHU_LUC_A)
    x += kit.doan("Các mã lỗi mà route trong tài liệu này trả và TK chưa có. Thông điệp dưới đây là mặc định; một số "
                  "route trả thông điệp cụ thể hơn trong cùng mã lỗi (vd nêu tên thuốc), nên frontend xử lý theo "
                  "`maLoi`, không theo thông điệp. Bảng ghi thông điệp tiếng Việt; tiếng Anh theo `Accept-Language` "
                  "(TK 3.12).")
    x += kit.bang(kit.RONG_MA_LOI, ("maLoi", "HTTP", "Thông điệp mặc định (tiếng Việt)"),
                  [(f"`{ma}`", str(code[ma][0]), code[ma][1]) for ma in rieng], cot_giua=(1,))
    x += kit.doan("Mã tài liệu này cũng dùng nhưng đã có ở TK Phụ lục A (thông điệp xem ở đó): "
                  + ", ".join(f"`{ma}`" for ma in dung_chung)
                  + ". Mã do api-gateway tự trả khi service cần cho route chưa chạy (`DICH_VU_KHONG_KHA_DUNG`...): "
                    "TK 3.4.")
    return x + kit.khoang_trong(60)


def phu_luc_b(tl):
    x = kit.tieu_de(1, PHU_LUC_B)
    x += kit.doan("Thứ tự gọi API cho các màn hình chính. `...` thay cho tiền tố của đường dẫn đầy đủ gần nhất phía "
                  "trước (`/api/booking/bac-si/toi` hoặc `/api/medical/bac-si/toi`); route đầy đủ ở mục 2.")
    for tieu_de, cac_buoc in LUONG:
        so = tl.danh_sach_so_moi()
        x += kit.tieu_de(2, tieu_de) + "".join(kit.cham(b, so) for b in cac_buoc) + kit.khoang_trong(60)
    return x


def phu_luc_c():
    x = kit.tieu_de(1, PHU_LUC_C)
    x += kit.doan("Tài khoản bác sĩ (`bacsi01` … `bacsi12`), quản trị viên, bệnh nhân mẫu, mật khẩu, ca làm việc và "
                  "biến môi trường: TK Phụ lục C. Dữ liệu mẫu chỉ được tạo khi chạy với `SEED_DATA=true`. Thêm cho "
                  "phía bác sĩ:")
    x += "".join(kit.cham(d) for d in DU_LIEU)
    x += kit.doan("**Chạy bằng IntelliJ:** service nay đọc file `.env` ở thư mục gốc của mã nguồn backend (README.md), "
                  "nên câu \"Khi chạy bằng IntelliJ, service không đọc file .env\" ở TK Phụ lục C không còn đúng.")
    return x + kit.khoang_trong(60)


def sua(tl):
    sau = phu_luc_b(tl) + phu_luc_c()
    bsqt_p2_khung.them_cuoi(tl, phu_luc_a(tl, sau) + sau)
