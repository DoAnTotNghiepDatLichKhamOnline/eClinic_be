"""Tài liệu 2, giai đoạn 2: trang đầu, mục 1 (giới thiệu và phạm vi), mục 2 (tiêu đề bảng route, chưa có dòng).
Chạy qua bsqt_build.py. Các giai đoạn sau dùng them_cuoi và dong_route của file này.
"""

import re

import kit

PHIEN_BAN = "1.0 — 16/10/2026"
MUC_2 = "2. Danh sách route"
RONG_THONG_TIN = (2300, 7338)
RONG_PHIEN_BAN = (1900, 7738)
# Bảng route có thêm cột "Service cần chạy" (không có cột "Thay đổi" như TK: đây là bản đầu)
RONG_ROUTE = (600, 850, 3450, 1250, 2088, 1400)
COT_ROUTE = ("Mục", "Method", "Đường dẫn", "Quyền", "Chức năng", "Service cần chạy")

THONG_TIN = (
    ("Phiên bản", PHIEN_BAN),
    ("Phạm vi", "22 route của phần lịch hẹn và khám bệnh phía bác sĩ (19 route) và quản trị viên (3 route). "
                "booking-service 15 route (bác sĩ 13, quản trị viên 2): ca làm việc của bác sĩ, lịch hẹn, tra cứu lịch "
                "hẹn theo mã, bác sĩ hủy lịch, hàng đợi khám, hồ sơ khám, sửa thông tin bệnh nhân, đối chiếu giấy tờ. "
                "medical-service 7 route (bác sĩ 6, quản trị viên 1): ghi, sửa, xem hồ sơ bệnh án, lịch sử sửa, gợi ý "
                "thuốc và mã ICD-10"),
    ("Căn cứ", "Mã nguồn trong eClinic_be.zip (controller, DTO, service, MaLoi, application.yml)"),
    ("Địa chỉ gọi API", "`http://localhost:8080` (api-gateway). Gọi thẳng từng service: booking 8084, medical 8085 "
                        "(xem 3.2)"),
    ("Tài liệu đi kèm", "`eClinic_API_XacThuc_TaiKhoan` bản 2.3, gọi tắt là **TK**: đăng nhập, khung phản hồi, lỗi "
                        "chung và toàn bộ phần phía bệnh nhân"),
)

NGOAI_PHAM_VI = (
    "Xếp lịch làm việc: quản trị viên tạo, sửa, hủy ca làm việc; lịch hẹn cần đổi khi ca bị hủy; yêu cầu đổi ca và xin "
    "nghỉ của bác sĩ; danh sách phòng khám.",
    "Trang tổng quan của bác sĩ và của quản trị viên.",
    "Đánh giá của bệnh nhân, phía bác sĩ và phía quản trị viên.",
    "Hồ sơ bác sĩ (bác sĩ tự sửa) và quản lý bác sĩ của quản trị viên.",
    "Quản lý hồ sơ bệnh nhân của quản trị viên (danh sách, xác minh liên kết tài khoản).",
    "Quản lý danh mục thuốc, phòng khám, chuyên khoa.",
    "Báo cáo, thống kê (report-service).",
)

LICH_SU = (
    (PHIEN_BAN, "Bản đầu: 22 route lịch hẹn và khám bệnh phía bác sĩ (19 route) và quản trị viên (3 route)."),
)


def them_cuoi(tl, xml):
    """Thêm khối vào cuối thân tài liệu (trước phần thiết lập trang)."""
    vi_tri = tl.xml.rindex("<w:sectPr")
    tl.xml = tl.xml[:vi_tri] + xml + tl.xml[vi_tri:]


def dong_route(muc, method, duong_dan, bookmark, quyen_goi, chuc_nang, service):
    """1 dòng của bảng route ở mục 2: như kit.dong_route, thêm ô "Service cần chạy" (markup)."""
    chu = lambda c: kit._run(kit._ARIAL + kit._KHONG_DAM + kit._co(18), c)
    lien_ket = (f'<w:hyperlink w:history="1" w:anchor="{bookmark}">'
                + kit._run(kit._CONSOLAS + '<w:color w:val="1565C0"/>' + kit._co(17) + '<w:u w:val="single"/>',
                           duong_dan)
                + "</w:hyperlink>")
    r = RONG_ROUTE
    return kit._dong([
        kit._o(r[0], chu(muc)),
        kit._o(r[1], kit._run(kit._CONSOLAS + kit._DAM + '<w:color w:val="FFFFFF"/>' + kit._co(18), method),
               kit.MAU_METHOD[method], giua=True),
        kit._o(r[2], lien_ket, doc=None, ppr='<w:spacing w:after="0"/>'),
        kit._o(r[3], chu(quyen_goi)),
        kit._o(r[4], chu(chuc_nang)),
        kit._o(r[5], kit.runs(service, True)),
    ])


def bang_thong_tin():
    """Bảng thông tin ở trang đầu: cột nhãn chữ đậm có nền, cột nội dung xen kẽ nền như bảng của bản 1.0."""
    nhan = kit._ARIAL + kit._DAM + kit._co(18)
    dong = [kit._dong([kit._o(RONG_THONG_TIN[0], kit._run(nhan, ten), kit.MAU_DONG_CHAN),
                       kit._o(RONG_THONG_TIN[1], kit.runs(noi_dung, True), kit.MAU_DONG_CHAN if i % 2 else None)])
            for i, (ten, noi_dung) in enumerate(THONG_TIN)]
    return kit._mo_bang(RONG_THONG_TIN) + "".join(dong) + "</w:tbl>"


def muc_1():
    x = kit.tieu_de(1, "1. Giới thiệu và phạm vi")
    x += kit.doan("Tài liệu mô tả route, request, response và mã lỗi của phần lịch hẹn và khám bệnh mà frontend cần để "
                  "làm màn hình của bác sĩ và của quản trị viên:")
    x += kit.cham("**Bác sĩ:** xem ca làm việc của mình; lịch hẹn theo ngày và theo khoảng ngày; tra cứu lịch hẹn bằng "
                  "mã trên phiếu khám hoặc mã QR; hủy lịch kèm lý do; hàng đợi khám (gọi bệnh nhân, đánh dấu vắng mặt); "
                  "hồ sơ khám (thông tin bệnh nhân và các lần khám trước); sửa thông tin hành chính của bệnh nhân; đối "
                  "chiếu giấy tờ; ghi, sửa, xem hồ sơ bệnh án và lịch sử sửa; gợi ý thuốc và mã ICD-10.")
    x += kit.cham("**Quản trị viên:** tra cứu lịch hẹn bằng mã; đánh dấu đã đối chiếu giấy tờ; xem lịch sử sửa hồ sơ "
                  "bệnh án.")
    x += kit.doan("Các route do booking-service và medical-service cung cấp; frontend gọi qua api-gateway. Mục 3.2 ghi "
                  "những service phải chạy cho từng nhóm route.")
    x += kit.doan("**Không có trong tài liệu này** (chưa hoàn thiện, hoặc thuộc phần khác của hệ thống):")
    x += "".join(kit.cham(d) for d in NGOAI_PHAM_VI)
    x += kit.doan("**Đọc cùng TK.** Tài liệu này đi kèm `eClinic_API_XacThuc_TaiKhoan` bản 2.3, gọi tắt là TK; "
                  "\"TK 3.4\" là mục 3.4 của tài liệu đó. Những gì TK đã viết không nhắc lại ở đây mà chỉ dẫn mục: "
                  "đăng nhập, làm mới phiên, đăng xuất và các route tài khoản dùng cho mọi vai trò (TK 5-10), khung phản "
                  "hồi (TK 3.1), access token (TK 3.2), lỗi chung (TK 3.4), các enum đã có (TK 3.7), vòng đời lịch hẹn "
                  "(TK 3.11), ngôn ngữ EN / VI (TK 3.12), các mô hình dùng chung (TK 4.14, TK 4.15, TK 4.27). Danh "
                  "sách đầy đủ ở 3.1.")
    x += kit.doan("Bố cục: mục 2 là bảng tất cả route, bấm để nhảy tới phần chi tiết, kèm service cần chạy; mục 3 là "
                  "quy ước riêng của phía bác sĩ và quản trị viên; mục 4 là mô hình dữ liệu của response chưa có ở TK; "
                  "mục 5-8 là chi tiết từng route của bác sĩ, mục 9 của quản trị viên. Phụ lục A: mã lỗi riêng của tài "
                  "liệu này; Phụ lục B: luồng gọi mẫu; Phụ lục C: tài khoản và dữ liệu để thử.")
    x += (kit.nhan("LỊCH SỬ PHIÊN BẢN") + kit.bang(RONG_PHIEN_BAN, ("Phiên bản", "Nội dung"), LICH_SU)
          + kit.khoang_trong(60))
    return x


def muc_2():
    return (kit.tieu_de(1, MUC_2)
            + kit.doan("Bấm vào đường dẫn để nhảy tới phần mô tả chi tiết. Cột **Service cần chạy** ghi service riêng "
                       "của route; mọi route còn cần mysql, redis, api-gateway và identity-service (3.2).")
            + kit.bang(RONG_ROUTE, COT_ROUTE, []) + kit.khoang_trong(60))


def sua(tl):
    # --- Trang đầu ---
    tl.thay_chu("Xác thực và Quản lý tài khoản (identity-service)",
                "Lịch hẹn và khám bệnh phía bác sĩ và quản trị viên")
    dau = tl.xml.index("<w:tbl>")
    cuoi = tl.xml.index("</w:tbl>", dau) + len("</w:tbl>")
    assert re.search(r">Phiên bản</w:t>", tl.xml[dau:cuoi]), "Bảng đầu tiên không phải bảng thông tin ở trang đầu"
    tl.xml = tl.xml[:dau] + bang_thong_tin() + tl.xml[cuoi:]

    # --- Mục 1, 2 ---
    them_cuoi(tl, muc_1() + muc_2())
