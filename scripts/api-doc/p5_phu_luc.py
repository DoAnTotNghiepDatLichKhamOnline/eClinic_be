"""Bản 2.0 của tài liệu API: trang đầu, bố cục ở mục 1, và ba phụ lục cho phần đặt lịch (mã lỗi, luồng gọi mẫu,
cấu hình và dữ liệu mẫu).

Giai đoạn 5 của chuỗi dựng tài liệu; chạy qua build.py. Dòng mã lỗi ở Phụ lục A và giá trị mặc định của cấu hình
đặt lịch đọc thẳng từ mã nguồn (MaLoi.java, application.yml của booking-service) nên không lệch được với code.
"""

import re

import check
import kit

PHU_LUC_A = "Phụ lục A. Bảng mã lỗi (maLoi)"
PHU_LUC_C = "Phụ lục C. Cấu hình ảnh hưởng tới API và tài khoản mẫu"
RONG_LOI_CHUNG = (800, 3100, 5738)
RONG_CAU_HINH = (3300, 1100, 5238)
CAU_HINH_BOOKING = check.GOC / "booking-service/src/main/resources/application.yml"

# (biến môi trường, giới hạn mà biến đó quyết định)
CAU_HINH_DAT_LICH = (
    ("BOOKING_MIN_LEAD", "Phải đặt trước giờ bắt đầu lượt khám ít nhất bấy lâu (3.6)."),
    ("BOOKING_MAX_DAYS", "Số ngày tới được đặt trước, tính cả hôm nay (3.6)."),
    ("BOOKING_MAX_ACTIVE_PER_PATIENT", "Số lịch sắp tới còn hiệu lực tối đa của 1 hồ sơ bệnh nhân "
                                       "(409 `VUOT_GIOI_HAN_DAT_LICH`)."),
    ("BOOKING_MAX_ACTIVE_PER_PHONE", "Số lịch sắp tới còn hiệu lực tối đa của 1 số điện thoại liên hệ "
                                     "(409 `VUOT_GIOI_HAN_DAT_LICH`)."),
    ("BOOKING_IP_LIMIT", "Số lần đặt / đổi / hủy bằng link từ 1 địa chỉ IP trong 1 cửa sổ (429 `GUI_LAI_QUA_NHANH`)."),
    ("BOOKING_IP_WINDOW", "Độ dài cửa sổ đếm của giới hạn theo IP."),
    ("BOOKING_CHANGE_LEAD", "Hạn hủy / đổi lịch trước giờ khám dự kiến (409 `QUA_HAN_HUY_DOI_LICH`)."),
    ("BOOKING_MAX_RESCHEDULES", "Số lần đổi lịch của 1 lần đặt (409 `VUOT_SO_LAN_DOI_LICH`)."),
    ("RATING_EDIT_DAYS", "Số ngày được sửa đánh giá kể từ lúc gửi (409 `HET_HAN_SUA_DANH_GIA`)."),
    ("BOOKING_NOTIFY_INTERVAL", "Chu kỳ gửi thông báo: thông báo xuất hiện chậm nhất chừng ấy sau sự kiện."),
)

# (tiêu đề, các bước) của các luồng đặt lịch ở Phụ lục B
LUONG = (
    ("B.7 Khách đặt lịch và quản lý bằng phiếu khám", [
        "Không cần token. `GET /api/catalog/chuyen-khoa` (11.1) → chọn chuyên khoa; `GET /api/catalog/bac-si` (11.3) "
        "nếu người dùng muốn chọn bác sĩ.",
        "`GET /api/booking/khung-gio/ngay-con-cho` (12.2) để tô các ngày chọn được, rồi `GET /api/booking/khung-gio` "
        "(12.1, đã chọn bác sĩ) hoặc `GET /api/booking/khung-gio/gop` (12.4, bác sĩ bất kỳ) để lấy khung giờ của ngày.",
        "`POST /api/booking/lich-hen` (13.1) với `idLichLamViec` (hoặc `idChuyenKhoa`), `gioBatDauKhung`, `benhNhan`, "
        "`lyDoKham`; người khám dưới 18 tuổi kèm `nguoiGiamHo` → 201, lịch `CHO_XAC_NHAN`. Lưu `maPhieuKham` và hiện "
        "liên kết phiếu khám (3.10): khách không có cách nào khác để mở lại.",
        "Mở phiếu: `GET /api/booking/phieu-kham/{maPhieuKham}` (14.1) và ảnh QR (14.2). Thông tin cá nhân trên phiếu đã "
        "bị che.",
        "Đổi lịch: `POST /api/booking/phieu-kham/{maPhieuKham}/doi-lich` (14.4) với khung giờ mới và `soDienThoai` đã "
        "nhập lúc đặt → 201, trả **phiếu mới với mã mới**; phiếu cũ thành `DA_HUY_DO_DOI_LICH`. Chuyển người dùng "
        "sang liên kết của phiếu mới.",
        "Hủy: `POST /api/booking/phieu-kham/{maPhieuKham}/huy` (14.3) với `soDienThoai` → 200, phiếu `DA_HUY`. Sai số "
        "điện thoại → 403 `SO_DIEN_THOAI_KHONG_KHOP`; còn dưới 2 giờ trước giờ khám → 409 `QUA_HAN_HUY_DOI_LICH`.",
    ]),
    ("B.8 Bệnh nhân đã đăng nhập đặt, đổi và hủy lịch", [
        "`GET /api/booking/thong-tin-dat-lich/cua-toi` (13.2) → điền sẵn form bằng `banThan` hoặc 1 dòng của "
        "`nguoiThan`. `banThan.trangThaiLienKet = CHO_XAC_MINH` thì ẩn lựa chọn \"đặt cho bản thân\".",
        "Chọn khung giờ như B.7, rồi `POST /api/booking/lich-hen` (13.1) **kèm access token**; đặt cho chính mình thì "
        "gửi `datChoBanThan = true` → 201. Người thân vừa đặt được lưu lại cho lần sau.",
        "`GET /api/booking/lich-hen/cua-toi?loc=SAP_TOI` (15.1) cho danh sách, `GET .../cua-toi/lich` (15.2) cho màn "
        "hình lịch, `GET .../cua-toi/{maPhieuKham}` (15.3) cho chi tiết.",
        "Bác sĩ xác nhận hoặc từ chối: lịch thành `DA_XAC_NHAN` / `BI_TU_CHOI` và bệnh nhân nhận thông báo (B.11).",
        "Đổi lịch: `POST /api/booking/lich-hen/cua-toi/{maPhieuKham}/doi-lich` (15.5) → 201, lịch mới mang mã mới và trở "
        "lại `CHO_XAC_NHAN`; lịch cũ thành `DA_HUY_DO_DOI_LICH`. Mỗi lần đặt đổi được 2 lần.",
        "Hủy: `POST /api/booking/lich-hen/cua-toi/{maPhieuKham}/huy` (15.4), `lyDo` không bắt buộc → 200, `DA_HUY`.",
    ]),
    ("B.9 Lịch đã đặt như khách vào tài khoản", [
        "Khách đặt lịch (B.7) với `benhNhan.cccd`; hệ thống tạo hồ sơ bệnh nhân theo số CCCD đó.",
        "Người đó đăng ký `POST /api/auth/register` (5.1) với **cùng** `cccd`, họ tên và số điện thoại, kích hoạt rồi "
        "đăng nhập (B.1).",
        "Ở lần gọi đầu tiên tới một route `/cua-toi`, hệ thống tự tìm hồ sơ theo số CCCD đã khai. Họ tên và số điện "
        "thoại khớp → hồ sơ `DA_LIEN_KET`: `GET /api/booking/lich-hen/cua-toi` (15.1) trả cả các lịch đã đặt như khách, "
        "với `nguoiDat = KHACH`.",
        "Tài khoản không khai CCCD lúc đăng ký (hoặc đăng nhập Google): nhập ở form hồ sơ, "
        "`PUT /api/booking/ho-so-benh-nhan/cua-toi` (17.2). Số CCCD đã có hồ sơ và thông tin khớp → 200, "
        "`trangThaiLienKet = DA_LIEN_KET`, lịch cũ hiện ra như bước 3.",
        "Thông tin không khớp: **vẫn 200**, nhưng `trangThaiLienKet = CHO_XAC_MINH` và mọi trường khác `null`. Hiện "
        "\"hồ sơ đang chờ phòng khám xác minh\"; trong lúc chờ, đặt cho bản thân và sửa hồ sơ đều trả 409 "
        "`HO_SO_CHO_XAC_MINH`, đặt cho người thân vẫn được.",
        "Số CCCD thuộc hồ sơ của tài khoản khác → 409 `CCCD_DA_CO_HO_SO`.",
    ]),
    ("B.10 Sau khi khám: kết quả và đánh giá", [
        "Bác sĩ ghi kết quả khám → lịch thành `DA_HOAN_THANH`. Bệnh nhân **không** nhận thông báo cho việc này.",
        "`GET /api/booking/lich-su-kham/cua-toi` (16.1) → các lượt đã khám kèm chẩn đoán và đơn thuốc; hoặc "
        "`GET /api/booking/lich-hen/cua-toi/{maPhieuKham}` (15.3), trường `ketQua`.",
        "`POST /api/booking/lich-hen/cua-toi/{maPhieuKham}/danh-gia` (16.2) với `soSao` 1-5 và `nhanXet` → 201. Lịch "
        "chưa khám xong → 409 `LICH_HEN_CHUA_KHAM_XONG`; đã đánh giá rồi → 409 `DA_DANH_GIA`.",
        "Sửa trong 7 ngày: `PUT .../danh-gia` (16.3) → 200; quá hạn → 409 `HET_HAN_SUA_DANH_GIA`.",
    ]),
    ("B.11 Thông báo", [
        "Sau khi đăng nhập, gọi `GET /api/notification/thong-bao/so-chua-doc` (18.2) khoảng 30 giây một lần để hiện số "
        "trên chuông; tạm dừng khi tab bị ẩn. Không có đẩy realtime.",
        "Mở chuông: `GET /api/notification/thong-bao` (18.1), mới nhất trước; `chuaDoc=true` để chỉ lấy thông báo "
        "chưa đọc.",
        "Bấm vào 1 thông báo: `POST /api/notification/thong-bao/{id}/da-doc` (18.3) rồi mở lịch hẹn liên quan; "
        "\"đánh dấu tất cả\": `POST /api/notification/thong-bao/da-doc-tat-ca` (18.4).",
        "Bệnh nhân chỉ nhận thông báo khi bác sĩ xác nhận hoặc từ chối lịch, khi ca khám bị hủy (cần đổi lịch) và khi "
        "đổi phòng khám. Thao tác của chính bệnh nhân (đặt, đổi, hủy, đánh giá) và việc khám xong không sinh thông "
        "báo; thông báo xuất hiện sau sự kiện khoảng 5 giây.",
    ]),
)

DU_LIEU_MAU = [
    "**8 chuyên khoa**, mỗi chuyên khoa 1 phòng khám (Phòng 101-108): Nội tổng quát, Nhi khoa, Da liễu, Tai Mũi Họng, "
    "Tim mạch, Sản phụ khoa, Mắt, Răng Hàm Mặt.",
    "**12 bác sĩ** (`bacsi01` … `bacsi12`): 4 chuyên khoa đầu có 2 bác sĩ làm lệch buổi nhau, 4 chuyên khoa sau có 1 "
    "bác sĩ. Vì vậy khung giờ gộp (12.4) của dữ liệu mẫu luôn có `soBacSi = 1`.",
    "**Ca làm việc** có sẵn cho 14 ngày tính từ hôm nay và được bổ sung mỗi ngày lúc 0 giờ 10: sáng 08:00-11:30, "
    "chiều 13:30-17:00, từ thứ 2 đến thứ 7 (chủ nhật nghỉ). Mỗi lượt khám 10 phút, mỗi khung 1 giờ có 6 lượt.",
    "`benhnhan01` … `benhnhan04` đã có hồ sơ bệnh nhân liên kết sẵn (`DA_LIEN_KET`): số CCCD `079000000001` … "
    "`079000000004`, số điện thoại `0902000001` … `0902000004`. Đặt \"cho bản thân\" bằng các tài khoản này phải "
    "gửi đúng số CCCD đó.",
    "Chưa có lịch hẹn, kết quả khám hay thông báo nào: muốn có lịch `DA_XAC_NHAN` / `DA_HOAN_THANH` thì đăng nhập "
    "tài khoản bác sĩ của ca khám để xác nhận và ghi kết quả.",
]


def mac_dinh_cau_hinh():
    """{biến môi trường: giá trị mặc định} của booking-service."""
    return dict(re.findall(r"\$\{(\w+):([^}]*)\}", CAU_HINH_BOOKING.read_text(encoding="utf-8")))


def sua(tl):
    so_route = {s: len(check.route_trong_code(s)) for s in check.TIEN_TO}
    code = check.ma_loi_trong_code()

    # --- Trang đầu ---
    tl.thay_chu("1.1 — 09/10/2026", "2.0 — 09/10/2026")
    tl.thay_mot("Tài khoản (identity-service, 26 route, mọi vai trò)",
                f"{sum(so_route.values())} route. Tài khoản (identity-service, {so_route['identity']} route, mọi vai "
                "trò)")
    tl.thay_mot("(catalog-service, booking-service, notification-service): xem chuyên khoa, bác sĩ, khung giờ; đặt",
                f"(catalog-service {so_route['catalog']} route, booking-service {so_route['booking']} route, "
                f"notification-service {so_route['notification']} route): xem chuyên khoa, bác sĩ, khung giờ; đặt")

    # --- Mục 1: bố cục ---
    tl.chen_sau_doan("Frontend gọi mọi API qua ", kit.doan(
        "**Bố cục:** mục 2 là bảng tất cả route, bấm để nhảy tới phần chi tiết; mục 3 là quy ước chung (khung phản "
        "hồi, xác thực, lỗi chung, giới hạn, enum, quy ước của phần đặt lịch); mục 4 là mô hình dữ liệu của response; "
        "mục 5-10 là route tài khoản; mục 11-14 là xem chuyên khoa, bác sĩ, khung giờ, đặt lịch và phiếu khám (khách "
        "không đăng nhập dùng được); mục 15-18 là route của bệnh nhân đã đăng nhập (lịch hẹn của tôi, lịch sử khám và "
        "đánh giá, hồ sơ bệnh nhân, thông báo). Phụ lục A: bảng mã lỗi; Phụ lục B: luồng gọi mẫu; Phụ lục C: cấu hình "
        "và dữ liệu mẫu để thử."))

    # --- 3.4: lỗi do khoá dữ liệu, service nào cũng có thể trả ---
    tl.them_dong("3.4 Lỗi chung", None, kit.dong_bang(RONG_LOI_CHUNG, (
        "409", "`DU_LIEU_DA_THAY_DOI`",
        "Dữ liệu vừa bị một request khác thay đổi, hoặc hệ thống đang bận nên không khoá được dữ liệu. Tải lại dữ "
        "liệu rồi cho người dùng thử lại."), cot_giua=(0,)),
                 sau_dong_chua="Vi phạm ràng buộc duy nhất trong CSDL (thường do hai request chạy đồng thời).")

    # --- 3.6: giới hạn theo IP đếm những lần gọi nào ---
    tl.thay_noi_dung("Số lần gọi đặt lịch, đổi lịch, hủy bằng link phiếu khám từ 1 địa chỉ IP (đếm chung)",
                     "Số lần gọi đặt lịch, đổi lịch (cả bằng link phiếu khám lẫn `/cua-toi`) và hủy bằng link phiếu "
                     "khám từ 1 địa chỉ IP (đếm chung). Hủy qua `/cua-toi` và các route chỉ đọc không tính", True)

    # --- Phụ lục A: thêm mọi mã được nhắc trong thân bài mà bảng chưa có ---
    tl.thay_doan("Các mã lỗi thuộc phần xác thực và tài khoản. Thông điệp dưới đây là mặc định; một số route trả thông "
                 "điệp cụ thể hơn trong cùng mã lỗi.", kit.doan(
        "Mọi mã lỗi mà các route trong tài liệu có thể trả: lỗi chung, phần tài khoản, rồi phần đặt lịch. Thông điệp "
        "dưới đây là mặc định; một số route trả thông điệp cụ thể hơn trong cùng mã lỗi, nên frontend xử lý theo "
        "`maLoi`, không theo thông điệp. Bốn mã do api-gateway tự trả (`DICH_VU_KHONG_KHA_DUNG`, "
        "`DICH_VU_PHAN_HOI_QUA_LAU`, `LOI_KET_NOI_DICH_VU`, `YEU_CAU_QUA_LON`) ở mục 3.4."))
    tl.thay_chu("Chức năng ảnh đại diện tạm thời không khả dụng, vui lòng thử lại sau",
                code["LUU_TRU_ANH_KHONG_KHA_DUNG"][1])
    dau_a = tl.vi_tri_tieu_de(PHU_LUC_A)
    than, phu_luc = tl.xml[:dau_a], tl.xml[dau_a:]
    for ma, (status, thong_diep) in code.items():
        if re.search(rf"\b{ma}\b", than) and f">{ma}</w:t>" not in phu_luc:
            tl.them_dong(PHU_LUC_A, None, kit.dong_bang(kit.RONG_MA_LOI, (f"`{ma}`", str(status), thong_diep),
                                                        cot_giua=(1,)))

    # --- Phụ lục B: luồng của phần đặt lịch ---
    luong = ""
    for tieu_de, cac_buoc in LUONG:
        so = tl.danh_sach_so_moi()
        luong += kit.tieu_de(2, tieu_de) + "".join(kit.cham(b, so) for b in cac_buoc) + kit.khoang_trong(60)
    tl.chen_truoc_doan(PHU_LUC_C, luong)

    # --- Phụ lục C: cấu hình đặt lịch, dữ liệu mẫu của phần đặt lịch ---
    tl.thay_chu("Một số API chỉ hoạt động đầy đủ khi identity-service nhận được biến môi trường tương ứng. Khi chạy bằng ",
                "Một số API chỉ hoạt động đầy đủ khi service nhận được biến môi trường tương ứng; bảng đầu là của "
                "identity-service và api-gateway, bảng thứ hai là các giới hạn của booking-service. Khi chạy bằng ")
    mac_dinh = mac_dinh_cau_hinh()
    tl.chen_truoc_doan("Tài khoản có sẵn", kit.tieu_de(2, "Cấu hình đặt lịch (booking-service)") + kit.doan(
        "Các con số ở mục 3.6 là giá trị mặc định dưới đây. Môi trường thử có thể đặt khác đi (vd rút ngắn hạn hủy để "
        "thử), nên frontend đừng dựa vào các con số này để tự chặn thao tác: cứ gọi API và xử lý mã lỗi trả "
        "về.") + kit.bang(RONG_CAU_HINH, ("Biến môi trường", "Mặc định", "Quyết định"), [
            (f"`{bien}`", f"`{mac_dinh[bien]}`", mo_ta) for bien, mo_ta in CAU_HINH_DAT_LICH]) + kit.khoang_trong(60))
    tl.thay_noi_dung("Bệnh nhân mẫu đã kích hoạt.",
                     "Bệnh nhân mẫu đã kích hoạt, có sẵn hồ sơ bệnh nhân (xem dưới).", True)
    tl.chen_sau_doan("Dữ liệu mẫu chỉ được tạo khi CSDL chưa có bác sĩ nào và ",
                     kit.doan("Dữ liệu mẫu của phần đặt lịch (cùng điều kiện `SEED_DATA=true`):")
                     + "".join(kit.cham(d) for d in DU_LIEU_MAU))
