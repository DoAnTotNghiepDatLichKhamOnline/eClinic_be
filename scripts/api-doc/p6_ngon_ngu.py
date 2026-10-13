"""Bản 2.1 của tài liệu API: ngôn ngữ EN / VI, ngôn ngữ của tài khoản (route 6.4), email và lời nhắc lịch hẹn,
2 loại thông báo mới của bệnh nhân; bảng route ở mục 2 có thêm cột "Thay đổi".

Giai đoạn 6 của chuỗi dựng tài liệu; chạy qua build.py. JSON mẫu của 6.4 và 3.12 lấy từ response thật do lay_mau.py
ghi lại (temp/api-doc/mau). Giá trị mặc định của cấu hình nhắc lịch đọc thẳng từ application.yml của booking-service.

Quy tắc đánh dấu ở cột "Thay đổi": chỉ route có thay đổi của riêng nó (trường của request / response, hành vi của
chính route đó). Quy tắc chung "thongDiep theo Accept-Language" áp dụng cho mọi route nên nằm ở mục 3.12, không đánh
dấu từng dòng. Mỗi route được đánh dấu có 1 dòng "Thay đổi ở v2.1: ..." (hoặc "Mới ở v2.1: ...") trong mục chi tiết;
check.py kiểm tra bảng và các dòng này khớp nhau.
"""

import re

import kit
import p3_dat_lich
import p5_phu_luc
from p3_dat_lich import js, loi, ok

PHIEN_BAN = "2.1"
MUC_4 = "4. Mô hình dữ liệu (response)"
PHU_LUC_C = "Phụ lục C. Cấu hình ảnh hưởng tới API và tài khoản mẫu"
RONG_HAI_COT = (2000, 7638)
RONG_PHIEN_BAN = (1900, 7738)
RONG_DANH_MUC = (1500, 4200, 3938)
RONG_EMAIL = (2300, 4200, 3138)
MOI_VAI_TRO = "Mọi vai trò (cần access token)"

HO_SO = "như `GET /api/users/me` (6.1)"
TAI_KHOAN = "`TaiKhoanResponse` (4.1) có thêm trường `ngonNgu`"
THEO_NGON_NGU = "theo `Accept-Language` của request, thiếu bản tiếng Anh thì trả bản tiếng Việt (3.12)"

# (mục, tiêu đề của mục chi tiết, điều đã đổi ở bản 2.1). Danh sách này quyết định cột "Thay đổi" của bảng route.
CAP_NHAT = (
    ("5.1", "Đăng ký tài khoản bệnh nhân",
     f"`duLieu` là {TAI_KHOAN}. Ngôn ngữ ưa dùng của tài khoản mới lấy từ `Accept-Language` của chính request đăng ký "
     "(3.12); body không có trường nào cho việc này. Email kích hoạt được gửi bằng ngôn ngữ đó."),
    ("5.4", "Đăng nhập bằng email và mật khẩu", f"`duLieu.taiKhoan` là {TAI_KHOAN}."),
    ("5.5", "Làm mới phiên đăng nhập", f"`duLieu.taiKhoan` là {TAI_KHOAN}."),
    ("5.10", "Đăng nhập bằng Google",
     f"`duLieu.taiKhoan` là {TAI_KHOAN}. Tài khoản được tạo ở lần đăng nhập Google đầu tiên lấy ngôn ngữ ưa dùng từ "
     "`Accept-Language` của request đó; tài khoản đã có thì giữ nguyên."),
    ("6.1", "Xem hồ sơ cá nhân", "`duLieu` có thêm trường `ngonNgu` (`HoSoCaNhanResponse`, 4.3), đứng sau `trangThai`."),
    ("6.2", "Cập nhật hồ sơ cá nhân", "`duLieu` có thêm trường `ngonNgu`, như 6.1. Route này không đổi `ngonNgu`: "
                                      "dùng 6.4."),
    ("7.1", "Đổi ảnh đại diện", "`duLieu` có thêm trường `ngonNgu`, như 6.1."),
    ("7.2", "Bỏ ảnh đại diện", "`duLieu` có thêm trường `ngonNgu`, như 6.1."),
    ("11.1", "Danh sách chuyên khoa",
     f"`tenChuyenKhoa` và `moTa` trả {THEO_NGON_NGU}. `tuKhoa` khớp cả tên tiếng Việt lẫn tên tiếng Anh, bất kể ngôn "
     "ngữ của request. Danh sách sắp theo tên ở ngôn ngữ được yêu cầu, nên thứ tự của hai ngôn ngữ khác nhau."),
    ("11.2", "Chi tiết chuyên khoa", f"`tenChuyenKhoa` và `moTa` trả {THEO_NGON_NGU}."),
    ("11.3", "Tìm bác sĩ",
     f"`chucVu`, `gioiThieuNgan` và `tenChuyenKhoa` trả {THEO_NGON_NGU}. `hoTen` và `hocVi` không bao giờ dịch; tìm "
     "theo tên bác sĩ không đổi."),
    ("11.4", "Chi tiết bác sĩ",
     "`chucVu`, `gioiThieuNgan`, `tenChuyenKhoa`, `tieuSu`, `quaTrinhDaoTao`, `quaTrinhCongTac`, `linhVucKhamChua` và "
     f"`anh[].chuThich` trả {THEO_NGON_NGU}. `hoTen` và `hocVi` không bao giờ dịch."),
    ("13.1", "Đặt lịch khám",
     "Đặt thành công thì hệ thống gửi email \"đã nhận yêu cầu đặt lịch\" tới `benhNhan.email`, không nhập thì tới "
     "email của tài khoản đã đặt; khách không nhập email thì không nhận email nào (3.13). Với khách, ngôn ngữ của "
     "request này được lưu theo lịch hẹn và dùng cho email xác nhận và các lời nhắc về sau. Request và response "
     "không đổi."),
    ("14.4", "Đổi lịch bằng link phiếu khám",
     "Đổi thành công thì hệ thống gửi 1 email cho lịch mới, kèm liên kết phiếu khám mới (3.13). Request và response "
     "không đổi."),
    ("15.5", "Đổi lịch hẹn của tôi",
     "Đổi thành công thì hệ thống gửi 1 email cho lịch mới, kèm liên kết phiếu khám mới (3.13). Request và response "
     "không đổi."),
    ("18.1", "Danh sách thông báo của tôi",
     "`noiDung` được soạn theo `Accept-Language` của request đọc, không còn là câu tiếng Việt cố định. Bệnh nhân "
     "nhận thêm 2 loại thông báo: `NHAC_LICH_KHAM` và `NHAC_TAI_KHAM` (xem LƯU Ý). Cấu trúc response không đổi."),
)
MOI = (("6.4", "Lưu ngôn ngữ của tài khoản"),)

LICH_SU = (
    ("1.0 — 03/10/2026", "Bản đầu: 25 route xác thực và tài khoản của identity-service (mục 5-10)."),
    ("1.1 — 09/10/2026", "Sửa phần tài khoản cho khớp mã nguồn; thêm route 5.11 (đặt mật khẩu lần đầu)."),
    ("2.0 — 09/10/2026", "Thêm phần đặt lịch khám phía bệnh nhân: 32 route (mục 11-18), quy ước 3.8-3.11, mô hình "
                         "4.10-4.36, phụ lục mã lỗi, luồng gọi và cấu hình của phần đặt lịch."),
    ("2.1 — 13/10/2026", "Ngôn ngữ EN / VI (3.12): câu chữ và nội dung danh mục theo `Accept-Language`; ngôn ngữ của "
                         "tài khoản `ngonNgu` và route mới 6.4; email và lời nhắc lịch hẹn (3.13); 2 loại thông báo "
                         "mới `NHAC_LICH_KHAM`, `NHAC_TAI_KHAM`; trợ lý AI (mục 19, 6 route); cột **Thay đổi** ở mục 2."),
)

# (biến môi trường, điều biến đó quyết định) của phần nhắc lịch, đọc mặc định từ application.yml của booking-service
CAU_HINH_NHAC = (
    ("BOOKING_REMINDER_BEFORE", "Các mốc nhắc lịch khám trước giờ khám, cách nhau bằng dấu phẩy. Lịch đặt khi đã ở "
                                "trong khoảng của 1 mốc thì bỏ mốc đó."),
    ("BOOKING_REMINDER_FROM", "Giờ bắt đầu được gửi lời nhắc (giờ của phòng khám)."),
    ("BOOKING_REMINDER_TO", "Giờ kết thúc được gửi lời nhắc; lời nhắc đến hạn ban đêm được gửi vào sáng hôm sau."),
    ("BOOKING_REMINDER_MIN_LEAD", "Lượt khám còn cách ít hơn khoảng này thì không nhắc nữa."),
    ("BOOKING_REMINDER_INTERVAL", "Chu kỳ tìm lịch hẹn đến hạn nhắc; cũng là độ trễ tối đa của thông báo và email "
                                  "\"bác sĩ hẹn tái khám\"."),
    ("FOLLOWUP_REMINDER_DAYS", "Nhắc tái khám trước ngày tái khám bấy nhiêu ngày (`0` = không nhắc trước)."),
    ("FOLLOWUP_BOOKING_PATH", "Trang đặt lịch của frontend: nút trong email tái khám mở `FRONTEND_URL` + đường dẫn "
                              "này."),
)

# Response thật (temp/api-doc/mau: doi-ngon-ngu, loi-ngon-ngu-thieu, loi-ngon-ngu-thieu-en, loi-ngon-ngu-sai)
HO_SO_SAU_DOI = {
    "id": 21, "hoTen": "Nguyễn Văn An", "email": "an.nguyen@example.com", "soDienThoai": "0901234567",
    "anhDaiDien": None, "vaiTro": "BENH_NHAN", "trangThai": "DA_KICH_HOAT", "ngonNgu": "EN", "coMatKhau": True,
    "lienKetGoogle": False, "ngayTao": "2026-10-03T08:40:00", "bacSi": None, "hoSoBenhNhan": None}
LOI_TIENG_ANH = {"thanhCong": False, "maLoi": "DU_LIEU_KHONG_HOP_LE", "thongDiep": "Invalid data",
                 "chiTiet": [{"truong": "ngonNgu", "thongDiep": "Please choose a language"}]}
LOI_TIENG_VIET = {"thanhCong": False, "maLoi": "DU_LIEU_KHONG_HOP_LE", "thongDiep": "Dữ liệu không hợp lệ",
                  "chiTiet": [{"truong": "ngonNgu", "thongDiep": "Chưa chọn ngôn ngữ"}]}


def rong_cua_bang(tl, tieu_de_muc):
    """Độ rộng các cột của bảng đầu tiên trong mục (bảng của bản 1.0 không dùng hằng số của kit)."""
    dau, cuoi = tl._bang_sau(tl.vi_tri_tieu_de(tieu_de_muc))
    return tuple(int(r) for r in re.findall(r'<w:gridCol w:w="(\d+)"/>', tl.xml[dau:cuoi]))


def ghi_thay_doi(tl, muc, ten, markup, moi=False):
    """Thêm dòng "Thay đổi ở v2.1: ..." ngay dưới dòng Quyền truy cập của mục chi tiết."""
    nhan = f"**{'Mới' if moi else 'Thay đổi'} ở v{PHIEN_BAN}:** "
    tl.chen_sau_doan("Quyền truy cập:", kit.doan(nhan + markup), tu=tl.vi_tri_tieu_de(f"{muc}  {ten}"))


def muc_3_12():
    return (kit.tieu_de(2, "3.12 Ngôn ngữ (EN / VI)") + kit.doan(
        "Từ bản 2.1, backend trả câu chữ bằng tiếng Việt hoặc tiếng Anh. Ngôn ngữ của **một request** do header "
        "`Accept-Language` quyết định: `en`, `en-US`... là tiếng Anh; không gửi header, hoặc gửi ngôn ngữ khác, là "
        "tiếng Việt như trước. Frontend nên gửi header này ở **mọi** request, kể cả request công khai, và tải lại dữ "
        "liệu đang hiển thị sau khi người dùng đổi ngôn ngữ: câu chữ lấy từ API không tự đổi ở phía trình duyệt.")
        + kit.bang(RONG_HAI_COT, ("Việc", "Quy tắc"), [
            ("Đổi theo `Accept-Language`",
             "`thongDiep` (thành công và lỗi), `chiTiet[].thongDiep`, lỗi 401 / 403, lỗi do api-gateway tự trả (3.4); "
             "nội dung danh mục (bảng dưới); câu `noiDung` của thông báo (18.1)."),
            ("Không đổi", "`maLoi`, tên trường, giá trị enum, mã HTTP. Frontend rẽ nhánh theo `maLoi`, không theo câu "
                          "chữ."),
            ("Không bao giờ dịch",
             "Họ tên người (bệnh nhân, bác sĩ, người giám hộ), học vị `hocVi`, tên thuốc, mã tra cứu, và mọi chữ "
             "người dùng tự gõ: lý do khám, lý do hủy / từ chối, nhận xét đánh giá, chẩn đoán, lời dặn, ghi chú đơn "
             "thuốc. Hiển thị nguyên văn ở cả hai ngôn ngữ."),
            ("Enum", "API trả mã (`CHO_XAC_NHAN`, `NAM`...); nhãn hiển thị của hai ngôn ngữ do frontend tự đặt."),
            ("Ngày giờ", "Kiểu dữ liệu không đổi (mục 4). Trong câu thông báo soạn sẵn, ngày vẫn viết `dd/MM/yyyy` "
                         "ở cả hai ngôn ngữ."),
        ]) + kit.khoang_trong(60)
        + kit.nhan("CÙNG MỘT LỖI, KHÔNG GỬI HEADER (TIẾNG VIỆT)") + js(LOI_TIENG_VIET)
        + kit.nhan("VỚI ACCEPT-LANGUAGE: EN") + js(LOI_TIENG_ANH)
        + kit.doan(
            "**Nội dung danh mục.** Các route trong tài liệu này giữ nguyên cấu trúc response; cùng trường đó nay "
            "mang chữ của ngôn ngữ được yêu cầu, và trả bản tiếng Việt **theo từng trường** khi phòng khám chưa nhập "
            "bản tiếng Anh của trường đó. Frontend không cần biết trường nào đang thiếu bản dịch.")
        + kit.bang(RONG_DANH_MUC, ("Dữ liệu", "Trường đổi theo ngôn ngữ", "Xuất hiện ở"), [
            ("Chuyên khoa", "`tenChuyenKhoa`, `moTa`",
             "11.1, 11.2 và mọi response có tên chuyên khoa: bác sĩ, khung giờ, lịch hẹn, phiếu khám, lịch sử khám."),
            ("Phòng khám", "`tenPhong`, `tang`",
             "`PhongKhamTomTatResponse` (4.15) trong khung giờ, lịch hẹn và phiếu khám."),
            ("Bác sĩ", "`chucVu`, `gioiThieuNgan`, `tieuSu`, `quaTrinhDaoTao`, `quaTrinhCongTac`, `linhVucKhamChua`, "
                       "chú thích ảnh `chuThich`", "11.3, 11.4."),
            ("Thuốc trong đơn", "`donVi`", "`KetQuaKhamResponse` (4.27): kết quả khám ở 15.3 và 16.1."),
        ]) + kit.khoang_trong(60)
        + kit.doan(
            "**Ngôn ngữ của tài khoản.** Mỗi tài khoản lưu một ngôn ngữ ưa dùng `ngonNgu` (`VI` hoặc `EN`, enum "
            "`NgonNgu` ở 3.7). Giá trị này chỉ dùng cho những thứ hệ thống gửi đi khi không có request của người "
            "dùng: email và lời nhắc (3.13). Câu chữ của API vẫn theo header của từng request, không theo `ngonNgu`.")
        + "".join(kit.cham(d) for d in [
            "Tài khoản mới (5.1, và 5.10 ở lần đăng nhập Google đầu tiên): `ngonNgu` lấy từ `Accept-Language` của "
            "chính request đó.",
            "Tài khoản bác sĩ do quản trị viên tạo: `VI` cho tới khi bác sĩ tự đổi.",
            "Đọc: `taiKhoan.ngonNgu` ở 5.4, 5.5, 5.10 và `ngonNgu` ở 6.1. Đổi: `PUT /api/users/me/language` (6.4).",
            "Khách không đăng nhập không có tài khoản để lưu: email của một lịch do khách đặt dùng ngôn ngữ của "
            "request `POST /api/booking/lich-hen` (13.1) đã tạo lịch đó.",
            "Gợi ý cho frontend (trang demo đang làm vậy): lưu lựa chọn EN | VI ở `localStorage`. Sau khi đăng nhập, "
            "nếu trình duyệt này đã từng chọn thì lựa chọn đó thắng và được ghi vào tài khoản bằng 6.4; nếu chưa thì "
            "dùng `taiKhoan.ngonNgu`. Mỗi lần người dùng đổi ngôn ngữ khi đang đăng nhập cũng gọi 6.4.",
        ]) + kit.khoang_trong(60))


def muc_3_13():
    return (kit.tieu_de(2, "3.13 Email và lời nhắc lịch hẹn") + kit.doan(
        "Frontend không gọi API nào để gửi email. Mục này cho biết người dùng nhận được gì sau từng thao tác và liên "
        "kết trong email mở trang nào của frontend.")
        + kit.bang(RONG_EMAIL, ("Email", "Khi nào gửi", "Nút trong email mở"), [
            ("Đã nhận yêu cầu đặt lịch",
             "Ngay sau khi đặt lịch thành công (13.1), khách hay tài khoản đều có. Email nói rõ lịch đang chờ bác sĩ "
             "xác nhận.", "Phiếu khám: `linkPhieuKham` (3.10)."),
            ("Đã đổi lịch", "Sau khi bệnh nhân đổi lịch thành công (14.4, 15.5): 1 email cho lịch mới, mang mã mới.",
             "Phiếu khám của lịch mới."),
            ("Nhắc lịch khám",
             "Trước giờ khám 3 ngày và 1 ngày, chỉ gửi trong khoảng 07:00-21:00. Lịch chưa được xác nhận vẫn được "
             "nhắc, với câu nói rõ điều đó. Lịch đã hủy, bị từ chối hoặc đang chờ đổi (`canDoiLich`) không nhắc; lịch "
             "đặt sát giờ thì bỏ mốc đã qua.", "Phiếu khám."),
            ("Bác sĩ hẹn tái khám, hoặc đổi ngày tái khám",
             "Trong khoảng 5 phút sau khi bác sĩ ghi hoặc sửa `ngayTaiKhamDeXuat` của kết quả khám. Bác sĩ xoá ngày "
             "tái khám thì không gửi gì.", "Trang đặt lịch của frontend."),
            ("Nhắc tái khám",
             "7 ngày trước ngày tái khám; bỏ qua khi bệnh nhân đã có lịch hẹn sắp tới với bác sĩ cùng chuyên khoa.",
             "Trang đặt lịch của frontend."),
        ]) + kit.khoang_trong(60)
        + "".join(kit.cham(d) for d in [
            "**Gửi cho ai:** email liên hệ nhập lúc đặt lịch (`benhNhan.email`); không nhập thì email của tài khoản "
            "đã đặt. Khách không nhập email thì không nhận email nào: form đặt lịch nên nói rõ điều này cạnh ô email.",
            "**Ngôn ngữ:** `ngonNgu` của tài khoản đã đặt; với khách là ngôn ngữ của request đặt lịch (3.12).",
            "**Nội dung:** họ tên bệnh nhân, ngày giờ khám, bác sĩ, phòng và tầng, mã tra cứu, nút mở phiếu khám. "
            "Không có lý do khám, chẩn đoán, số điện thoại hay CCCD. Email tái khám nêu bệnh nhân, bác sĩ, ngày tái "
            "khám và nói rõ **chưa có lịch hẹn nào được đặt**.",
            "**Liên kết:** phiếu khám là `linkPhieuKham` như API trả (3.10). Nút đặt lịch tái khám mở `FRONTEND_URL` "
            "+ `FOLLOWUP_BOOKING_PATH` (mặc định `/dat-lich`, Phụ lục C): frontend cần có trang ở đường dẫn đó, hoặc "
            "đặt lại biến này.",
            "**Thông báo trên chuông đi kèm:** lời nhắc lịch khám sinh thông báo `NHAC_LICH_KHAM`, việc hẹn / đổi "
            "ngày tái khám và lời nhắc tái khám sinh thông báo `NHAC_TAI_KHAM` cho tài khoản phía bệnh nhân (mục 18). "
            "Email \"đã nhận yêu cầu\" và \"đã đổi lịch\" không sinh thông báo.",
            "Các email tài khoản đã có từ trước (kích hoạt, đặt lại mật khẩu, đổi email...) nay gửi theo `ngonNgu` "
            "của tài khoản; liên kết trong các email đó không đổi.",
            "Các mốc thời gian trên là giá trị mặc định, đổi được bằng cấu hình (Phụ lục C).",
        ]) + kit.khoang_trong(60))


def muc_6_4(id_bookmark):
    return kit.route(
        "6.4", "Lưu ngôn ngữ của tài khoản", "PUT", "/api/users/me/language", "ep_language", id_bookmark, MOI_VAI_TRO,
        [f"**Mới ở v{PHIEN_BAN}:** route này có từ bản {PHIEN_BAN}.",
         "Lưu ngôn ngữ ưa dùng của tài khoản đang đăng nhập. Giá trị này quyết định ngôn ngữ của email và lời nhắc "
         "gửi cho tài khoản (3.13); câu chữ của API vẫn theo `Accept-Language` của từng request (3.12)."],
        [kit.nhan("REQUEST BODY (JSON)"),
         kit.bang_truong([("ngonNgu", "enum NgonNgu", "Có", "`VI` hoặc `EN`.")]),
         js({"ngonNgu": "EN"}),
         kit.nhan("RESPONSE THÀNH CÔNG — 200 OK"),
         p3_dat_lich.dan(f"`duLieu` là hồ sơ sau khi lưu, {HO_SO}; `thongDiep`: \"Đã lưu ngôn ngữ\"."),
         js(ok(HO_SO_SAU_DOI, "Đã lưu ngôn ngữ")),
         loi([(400, "DU_LIEU_KHONG_HOP_LE",
               "Thiếu `ngonNgu` (kèm `chiTiet`), hoặc giá trị không phải `VI` / `EN` (không có `chiTiet`: body bị coi "
               "là JSON sai định dạng).")]),
         kit.luu_y([
             "Gửi lại giá trị đang lưu vẫn trả 200.",
             "Route này không đổi ngôn ngữ của response hiện tại hay của các request sau: frontend vẫn phải tự gửi "
             "`Accept-Language`.",
             "Khách không đăng nhập không gọi được route này (401 `CHUA_DANG_NHAP`); ngôn ngữ của khách chỉ là header "
             "của từng request.",
         ])])


def sua(tl):
    # --- Trang đầu và mục 1 ---
    tl.thay_chu("2.0 — 09/10/2026", f"{PHIEN_BAN} — 13/10/2026")
    tl.chen_sau_doan("Không có trong tài liệu này:", kit.doan(
        f"**Từ bản {PHIEN_BAN}:** tài liệu có thêm quy ước ngôn ngữ EN / VI (3.12), email và lời nhắc lịch hẹn (3.13) "
        "và route lưu ngôn ngữ của tài khoản (6.4). Các route của chatbot (`/api/chatbot/**`) không thuộc phạm vi "
        "tài liệu này; chúng được mô tả ở `docs/BAN-GIAO-FRONTEND-NGON-NGU-EMAIL-CHATBOT.md` trong mã nguồn backend."))
    tl.chen_sau_doan("Bố cục:", kit.nhan("LỊCH SỬ PHIÊN BẢN")
                     + kit.bang(RONG_PHIEN_BAN, ("Phiên bản", "Nội dung"), LICH_SU) + kit.khoang_trong(60))

    # --- Mục 2: dòng 6.4; cột "Thay đổi" thêm ở cuối hàm, sau khi mọi dòng đã có ---
    tl.thay_noi_dung(
        "Bấm vào đường dẫn để nhảy tới phần mô tả chi tiết.",
        "Bấm vào đường dẫn để nhảy tới phần mô tả chi tiết. Cột **Thay đổi** ghi route **Mới** hoặc **Cập nhật** "
        "cùng phiên bản tài liệu mang thay đổi đó (lịch sử phiên bản ở mục 1); route không đổi kể từ bản 2.0 thì ô "
        "này để trống. Mục chi tiết của route được đánh dấu có 1 dòng \"Thay đổi ở v...\" nói rõ đổi gì. Quy tắc "
        "chung áp dụng cho mọi route (câu chữ theo `Accept-Language`, mục 3.12) không được đánh dấu ở từng dòng.")
    tl.them_dong("2. Danh sách route", None,
                 kit.dong_route("6.4", "PUT", "/api/users/me/language", "ep_language", "Đã đăng nhập",
                                "Lưu ngôn ngữ của tài khoản (VI / EN)"),
                 sau_dong_chua="/api/users/me/change-password")

    # --- 3.1, 3.7 ---
    tl.thay_noi_dung("Thông điệp tiếng Việt, hiển thị được cho người dùng.",
                     "Thông điệp hiển thị được cho người dùng, bằng tiếng Việt hoặc tiếng Anh theo header "
                     "`Accept-Language` của request (3.12).", True)
    tl.thay_noi_dung(
        " (giữ nguyên giờ, đổi phòng khám). Chỉ gửi cho bác sĩ / quản trị viên: ",
        "Gửi cho bệnh nhân: `LICH_HEN_DA_XAC_NHAN`, `LICH_HEN_BI_TU_CHOI`, `LICH_HEN_CAN_DOI` (ca khám bị hủy, cần đổi "
        "lịch), `LICH_HEN_DOI_PHONG` (giữ nguyên giờ, đổi phòng khám), `NHAC_LICH_KHAM` (nhắc trước giờ khám, từ bản "
        "2.1), `NHAC_TAI_KHAM` (bác sĩ hẹn / đổi ngày tái khám và lời nhắc trước ngày đó, từ bản 2.1). Chỉ gửi cho "
        "bác sĩ / quản trị viên: `LICH_HEN_MOI`, `LICH_HEN_DA_DOI`, `LICH_HEN_DA_HUY`, `KET_QUA_DUYET_DOI_LICH`, "
        "`CA_LAM_VIEC_THAY_DOI`, `YEU_CAU_DOI_LICH_MOI`. Đã khai báo nhưng bản hiện tại chưa gửi: `HE_THONG`; "
        "frontend nên hiển thị được mọi giá trị bằng `noiDung`", True)
    tl.them_dong("3.7 Giá trị enum", None, kit.dong_bang(rong_cua_bang(tl, "3.7 Giá trị enum"), (
        "`NgonNgu`", "`VI`, `EN`: ngôn ngữ ưa dùng của tài khoản (3.12)")))

    # --- 3.12, 3.13 ---
    tl.chen_truoc_doan(MUC_4, muc_3_12() + muc_3_13())

    # --- 4.1, 4.3, 4.34 ---
    tl.them_dong("4.1 TaiKhoanResponse", None, kit.dong_bang(rong_cua_bang(tl, "4.1 TaiKhoanResponse"), (
        "`ngonNgu`", "enum NgonNgu", "Ngôn ngữ ưa dùng của tài khoản (3.12). Có từ bản 2.1.")))
    tl.them_dong("4.3 HoSoCaNhanResponse", None, kit.dong_bang(rong_cua_bang(tl, "4.3 HoSoCaNhanResponse"), (
        "`ngonNgu`", "enum NgonNgu", "Ngôn ngữ ưa dùng của tài khoản (3.12); đổi bằng 6.4. Có từ bản 2.1.")),
                 sau_dong_chua="vaiTro, trangThai")
    tl.thay_noi_dung("Câu thông báo tiếng Việt đã soạn sẵn; hiển thị nguyên văn.",
                     "Câu thông báo đã soạn sẵn, bằng ngôn ngữ của request đọc (3.12); hiển thị nguyên văn.", True)

    # --- JSON mẫu có đối tượng tài khoản: 5.1, 5.4, 6.1 ---
    tl.thay_chu('    "trangThai": "CHO_XAC_NHAN"', '    "trangThai": "CHO_XAC_NHAN",')
    tl.chen_sau_doan('    "trangThai": "CHO_XAC_NHAN",', kit.dong_code('    "ngonNgu": "VI"'))
    tl.thay_chu('      "trangThai": "DA_KICH_HOAT"', '      "trangThai": "DA_KICH_HOAT",')
    tl.chen_sau_doan('      "trangThai": "DA_KICH_HOAT",', kit.dong_code('      "ngonNgu": "VI"'))
    tl.chen_sau_doan('    "trangThai": "DA_KICH_HOAT",', kit.dong_code('    "ngonNgu": "VI",'),
                     tu=tl.vi_tri_tieu_de("6.1  Xem hồ sơ cá nhân"))

    # --- 6.4: route mới ---
    tl.chen_truoc_doan("7. Ảnh đại diện", muc_6_4(tl.id_bookmark_moi()))

    # --- Mục 18: thông báo theo ngôn ngữ, 2 loại mới ---
    tl.thay_noi_dung(
        "Bệnh nhân nhận 4 loại (",
        "Bệnh nhân nhận 6 loại (`LoaiThongBao` ở 3.7): `LICH_HEN_DA_XAC_NHAN` và `LICH_HEN_BI_TU_CHOI` khi bác sĩ xác "
        "nhận / từ chối; `LICH_HEN_CAN_DOI` khi ca khám bị hủy và lịch cần đổi (3.11); `LICH_HEN_DOI_PHONG` khi ca "
        "khám đổi phòng; `NHAC_LICH_KHAM` trước giờ khám 3 ngày và 1 ngày; `NHAC_TAI_KHAM` khi bác sĩ hẹn hoặc đổi "
        "ngày tái khám và 7 ngày trước ngày đó (3.13). Người nhận là tài khoản đã đặt lịch và tài khoản của hồ sơ "
        "người khám. Bệnh nhân không nhận thông báo khi tự đặt, tự hủy, tự đổi lịch hoặc khi bác sĩ ghi kết quả khám "
        "mà không hẹn tái khám.")
    tl.thay_noi_dung(
        " là câu tiếng Việt đã soạn sẵn: hiển thị nguyên văn, không tự ghép lại từ ",
        "`noiDung` là câu đã soạn sẵn theo `Accept-Language` của request đọc: hiển thị nguyên văn, không tự ghép lại "
        "từ `loai`. Người dùng đổi ngôn ngữ thì tải lại danh sách để nhận câu ở ngôn ngữ mới. Họ tên, mã tra cứu và "
        "lý do do người dùng gõ nằm trong câu vẫn giữ nguyên văn (vd tiếng Anh: \"Dr. Hoàng Minh Đức confirmed the "
        "appointment of Nguyễn Thị Mai at 08:00 on 14/10/2026 (code ECL-20261014-4848).\"). Thông báo tạo trước bản "
        "2.1 chỉ có câu tiếng Việt.")
    tl.chen_sau_doan("Bấm vào thông báo của bệnh nhân: mở chi tiết lịch hẹn bằng ", kit.cham(
        "Riêng `NHAC_TAI_KHAM`: bấm vào thì mở **trang đặt lịch**, không mở chi tiết lịch hẹn. `idLichHen` và "
        "`maPhieuKham` của thông báo này là của lượt khám **đã xong**; chưa có lịch hẹn mới nào được tạo. "
        "`NHAC_LICH_KHAM` mở chi tiết lịch hẹn như các loại khác."))

    # --- Dòng "Thay đổi ở v2.1" của từng route đã đổi ---
    for muc, ten, markup in CAP_NHAT:
        ghi_thay_doi(tl, muc, ten, markup)

    # --- Phụ lục A ---
    tl.thay_chu("Thông điệp mặc định", "Thông điệp mặc định (tiếng Việt)")
    tl.chen_sau_doan(
        "Mọi mã lỗi mà các route trong tài liệu có thể trả: lỗi chung, phần tài khoản, rồi phần đặt lịch. Thông điệp "
        "dưới đây là mặc định; một số route trả thông điệp cụ thể hơn trong cùng mã lỗi, nên frontend xử lý theo ",
        kit.doan("Bảng ghi thông điệp tiếng Việt. Mỗi mã có thêm một thông điệp tiếng Anh, trả về khi request gửi "
                 "`Accept-Language: en` (3.12); `maLoi` và mã HTTP giống nhau ở cả hai ngôn ngữ."))

    # --- Phụ lục B ---
    tl.thay_noi_dung(
        "Bệnh nhân chỉ nhận thông báo khi bác sĩ xác nhận hoặc từ chối lịch, khi ca khám bị hủy (cần đổi lịch) và khi "
        "đổi phòng khám. Thao tác của chính bệnh nhân (đặt, đổi, hủy, đánh giá) và việc khám xong không sinh thông "
        "báo; thông báo xuất hiện sau sự kiện khoảng 5 giây.",
        "Bệnh nhân nhận thông báo khi bác sĩ xác nhận hoặc từ chối lịch, khi ca khám bị hủy (cần đổi lịch), khi đổi "
        "phòng khám, khi tới mốc nhắc lịch khám, và khi bác sĩ hẹn tái khám hoặc tới mốc nhắc tái khám (3.13). Thao "
        "tác của chính bệnh nhân (đặt, đổi, hủy, đánh giá) không sinh thông báo. Thông báo về xác nhận, từ chối, hủy "
        "ca và đổi phòng xuất hiện sau sự kiện khoảng 5 giây; lời nhắc và thông báo tái khám theo chu kỳ nhắc (mặc "
        "định 5 phút, chỉ trong 07:00-21:00).")
    so = tl.danh_sach_so_moi()
    tl.chen_truoc_doan(PHU_LUC_C, kit.tieu_de(2, "B.12 Đổi ngôn ngữ EN / VI") + "".join(kit.cham(d, so) for d in [
        "Frontend giữ lựa chọn ngôn ngữ của người dùng (vd ở `localStorage`) và gửi `Accept-Language: en` hoặc `vi` "
        "ở mọi request (3.12).",
        "Người dùng bấm đổi ngôn ngữ: đổi nhãn của giao diện, rồi **tải lại dữ liệu đang hiển thị** (danh sách chuyên "
        "khoa, bác sĩ, lịch hẹn, thông báo...) để nhận nội dung ở ngôn ngữ mới.",
        "Nếu đang đăng nhập: gọi thêm `PUT /api/users/me/language` (6.4) với `ngonNgu` mới, để email và lời nhắc về "
        "sau dùng ngôn ngữ đó.",
        "Sau khi đăng nhập (5.4, 5.10): trình duyệt đã có lựa chọn thì giữ lựa chọn đó và ghi vào tài khoản bằng 6.4 "
        "nếu khác `taiKhoan.ngonNgu`; chưa có thì dùng `taiKhoan.ngonNgu`.",
        "Khách đặt lịch: không gọi 6.4; email xác nhận và lời nhắc của lịch đó dùng ngôn ngữ của request đặt lịch "
        "(13.1).",
    ]) + kit.khoang_trong(60))

    # --- Phụ lục C: cấu hình nhắc lịch và email lịch hẹn ---
    mac_dinh = p5_phu_luc.mac_dinh_cau_hinh()
    tl.chen_truoc_doan("Tài khoản có sẵn", kit.tieu_de(2, "Cấu hình nhắc lịch và email lịch hẹn") + kit.doan(
        "Các mốc thời gian ở mục 3.13 là giá trị mặc định dưới đây (booking-service). Email lịch hẹn do "
        "notification-service gửi và dùng chung `MAIL_MODE` ở bảng đầu: với `console`, email không được gửi thật mà "
        "người nhận, tiêu đề và liên kết phiếu khám được ghi ra log của notification-service.")
        + kit.bang(p5_phu_luc.RONG_CAU_HINH, ("Biến môi trường", "Mặc định", "Quyết định"), [
            (f"`{bien}`", f"`{mac_dinh[bien]}`", mo_ta) for bien, mo_ta in CAU_HINH_NHAC]) + kit.khoang_trong(60))

    # --- Mục 2: cột "Thay đổi" (làm sau cùng: bảng đã đủ dòng, và các mục chi tiết đã có dòng ghi thay đổi) ---
    danh_dau = {muc: ("Cập nhật", PHIEN_BAN) for muc, _, _ in CAP_NHAT}
    danh_dau.update({muc: ("Mới", PHIEN_BAN) for muc, _ in MOI})
    dau, cuoi = tl._bang_sau(tl.vi_tri_tieu_de("2. Danh sách route"))
    tl.xml = tl.xml[:dau] + kit.them_cot_thay_doi(tl.xml[dau:cuoi], danh_dau) + tl.xml[cuoi:]
