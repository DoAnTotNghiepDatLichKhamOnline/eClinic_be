"""Bản 2.4 của tài liệu API: phần phía bệnh nhân của DOANTOTNGH-13 (nhường ca cho bác sĩ khác).

- Quản trị viên duyệt yêu cầu nhường ca của bác sĩ thì ca khám và lịch hẹn của ca chuyển sang bác sĩ nhận; giờ, phòng
  không đổi. Phiếu khám và "lịch hẹn của tôi" có thêm `daDoiBacSi` (4.22, 4.25); với lịch này hủy / đổi được tới giờ khám
  và lần đổi không tính vào giới hạn (3.11).
- Thông báo mới `LICH_HEN_DOI_BAC_SI` cho bệnh nhân (3.7, mục 18) và email "đổi bác sĩ khám" (3.13).

Không thêm route: các route lịch làm việc, yêu cầu nhường ca là của bác sĩ / quản trị viên, có ở
docs/BAN-GIAO-FRONTEND-05-10.md. Giai đoạn 10 của chuỗi dựng tài liệu; chạy qua build.py.
"""

import re

import kit
from p6_ngon_ngu import RONG_EMAIL, RONG_PHIEN_BAN
from p8_benh_an_ho_so_bac_si import _pham_vi_muc, sua_json_mau, them_truong
from p9_xac_nhan_ngay_benh_an_day_du import MUC_1, MUC_2, MUC_311, MUC_313, _chu, _DOAN, chen_doan_sau, viet_lai

PHIEN_BAN = "2.4"
NGAY = "19/10/2026"

DA_DOI_BAC_SI = ("daDoiBacSi", "boolean",
                 "`true`: ca khám đã được chuyển cho bác sĩ khác (`bacSi` là bác sĩ mới), giờ và phòng không đổi; hủy / "
                 "đổi được tới giờ khám và lần đổi không tính vào số lần đổi (3.11). Có từ bản 2.4.")
THEM_TRUONG = "response có thêm `daDoiBacSi` (ca khám đã chuyển cho bác sĩ khác, 3.11)."

# (mục, tiêu đề mục chi tiết, điều đã đổi): quyết định dòng "Thay đổi ở v2.4" và cột "Thay đổi" của bảng route.
CAP_NHAT = (
    ("14.1", "Xem phiếu khám", "`PhieuKhamResponse` (4.22) có thêm `daDoiBacSi` (ca khám đã chuyển cho bác sĩ khác, "
                               "3.11)."),
    ("14.3", "Hủy lịch bằng link phiếu khám", THEM_TRUONG),
    ("15.1", "Danh sách lịch hẹn của tôi", "mỗi dòng (4.25) có thêm `daDoiBacSi` (ca khám đã chuyển cho bác sĩ khác, "
                                           "3.11)."),
    ("15.2", "Lịch hẹn của tôi theo khoảng ngày", "mỗi dòng có thêm `daDoiBacSi` như ở 15.1."),
    ("15.3", "Chi tiết lịch hẹn của tôi", THEM_TRUONG),
    ("15.4", "Hủy lịch hẹn của tôi", THEM_TRUONG),
    ("16.1", "Lịch sử khám của tôi", "mỗi lịch hẹn trong response có thêm `daDoiBacSi` như ở 15.1."),
    ("17.3", "Trang cá nhân của bệnh nhân", "mỗi lịch hẹn trong response có thêm `daDoiBacSi` như ở 15.1."),
    ("18.1", "Danh sách thông báo của tôi", "loại mới `LICH_HEN_DOI_BAC_SI`: ca khám chuyển cho bác sĩ khác (mục 18). "
                                            "Cấu trúc response không đổi."),
)

LICH_SU = (f"{PHIEN_BAN} — {NGAY}",
           "Ca khám có thể được chuyển cho bác sĩ khác (bác sĩ nhường ca): phiếu khám và lịch hẹn của tôi có thêm "
           "`daDoiBacSi`, hủy / đổi được tới giờ khám mà không tính lần đổi (3.11); thông báo `LICH_HEN_DOI_BAC_SI` và "
           "email \"đổi bác sĩ khám\" (3.13).")


def danh_dau_route(tl, muc):
    """Ô "Thay đổi" của 1 dòng bảng route -> "Cập nhật · v2.4"."""
    dau, cuoi = tl._bang_sau(tl.vi_tri_tieu_de(MUC_2))
    bang = tl.xml[dau:cuoi]
    cac_dong = [m for m in re.finditer(r"<w:tr>.*?</w:tr>", bang, re.S)
                if re.search(r"<w:t[^>]*>([^<]*)</w:t>", m.group(0)).group(1) == muc]
    assert len(cac_dong) == 1, f"Bảng route: cần đúng 1 dòng mục {muc}, thấy {len(cac_dong)}"
    dong = cac_dong[0].group(0)
    o_cuoi = dong.rindex("<w:tc>")
    chu = kit._run(kit._ARIAL + kit._DAM + f'<w:color w:val="{kit.MAU_THAY_DOI["Cập nhật"]}"/>' + kit._co(17),
                   f"Cập nhật · v{PHIEN_BAN}")
    bang = bang.replace(dong, dong[:o_cuoi] + kit._o(kit.RONG_ROUTE_THAY_DOI[5], chu) + "</w:tr>", 1)
    tl.xml = tl.xml[:dau] + kit.to_lai_nen(bang) + tl.xml[cuoi:]


def ghi_thay_doi(tl, muc, ten, markup):
    """Mỗi mục chỉ 1 dòng ghi thay đổi. Mục đã có dòng của bản trước: 1 dòng gộp "Thay đổi ở v2.4: ... Ở v2.x: <dòng
    cũ>", giữ nguyên chữ và định dạng của dòng cũ. Chưa có: thêm dưới dòng Quyền truy cập."""
    dau, cuoi = _pham_vi_muc(tl, f"{muc}  {ten}")
    cu = [m for m in _DOAN.finditer(tl.xml, dau, cuoi) if re.match(r"Thay đổi ở v2\.[123]:", _chu(m.group(0)))]
    if not cu:
        tl.chen_sau_doan("Quyền truy cập:", kit.doan(f"**Thay đổi ở v{PHIEN_BAN}:** " + markup), tu=dau)
        return
    doan = cu[0].group(0)
    nhan = re.search(r"<w:r><w:rPr>(?:(?!</w:r>).)*>Thay đổi ở v(2\.[123]):</w:t></w:r>", doan, re.S)
    moi = (doan[:doan.index("<w:r>")] + kit.runs(f"**Thay đổi ở v{PHIEN_BAN}:** {markup} **Ở v{nhan.group(1)}:**")
           + doan[nhan.end():])
    tl.xml = tl.xml[:cu[0].start()] + moi + tl.xml[cu[0].end():]


def _them_da_doi_bac_si(d):
    """Chèn "daDoiBacSi": false ngay sau canDoiLich (đúng thứ tự record Java) ở mọi lịch hẹn của mẫu."""
    if not isinstance(d, dict):
        if isinstance(d, list):
            for v in d:
                _them_da_doi_bac_si(v)
        return
    if "canDoiLich" in d and "daDoiBacSi" not in d and "maPhieuKham" in d:
        moi = {}
        for k, v in d.items():
            moi[k] = v
            if k == "canDoiLich":
                moi["daDoiBacSi"] = False
        d.clear()
        d.update(moi)
    for v in list(d.values()):
        _them_da_doi_bac_si(v)


def sua(tl):
    # --- Trang đầu và mục 1 ---
    cu = ">2.3 — 15/10/2026</w:t>"
    vi_tri = tl.xml.index(cu)  # ô Phiên bản của trang đầu (đứng trước bảng lịch sử)
    tl.xml = tl.xml[:vi_tri] + f">{PHIEN_BAN} — {NGAY}</w:t>" + tl.xml[vi_tri + len(cu):]
    tl.chen_sau_doan("Từ bản 2.3:", kit.doan(
        f"**Từ bản {PHIEN_BAN}:** bác sĩ có thể nhường ca khám cho 1 bác sĩ cùng chuyên khoa (quản trị viên duyệt); lịch "
        "hẹn của ca giữ nguyên giờ, phòng và chuyển sang bác sĩ mới. Phiếu khám và lịch hẹn của tôi có thêm "
        "`daDoiBacSi`; bệnh nhân nhận thông báo `LICH_HEN_DOI_BAC_SI` và email, hủy / đổi được tới giờ khám mà không "
        "tính lần đổi (3.11). Các route lịch làm việc và nhường ca của bác sĩ / quản trị viên có ở "
        "`docs/BAN-GIAO-FRONTEND-05-10.md` trong mã nguồn backend."))
    tl.them_dong(MUC_1, "LỊCH SỬ PHIÊN BẢN", kit.dong_bang(RONG_PHIEN_BAN, LICH_SU))

    # --- 3.7 ---
    viet_lai(tl, "3.7 Giá trị enum", "Gửi cho bệnh nhân: LICH_HEN_BI_TU_CHOI",
             "Gửi cho bệnh nhân: `LICH_HEN_BI_TU_CHOI` (bác sĩ hủy lịch đã xác nhận, kèm lý do; nghĩa này từ bản 2.3), "
             "`LICH_HEN_CAN_DOI` (ca khám bị hủy, cần đổi lịch), `LICH_HEN_DOI_PHONG` (giữ nguyên giờ, đổi phòng khám), "
             "`LICH_HEN_DOI_BAC_SI` (ca khám chuyển cho bác sĩ khác, giữ nguyên giờ và phòng, từ bản 2.4), "
             "`NHAC_LICH_KHAM` (nhắc trước giờ khám, từ bản 2.1), `NHAC_TAI_KHAM` (bác sĩ hẹn / đổi ngày tái khám và lời "
             "nhắc trước ngày đó, từ bản 2.1). `LICH_HEN_DA_XAC_NHAN` (bác sĩ xác nhận lịch) chỉ còn ở thông báo tạo "
             "trước bản 2.3. Chỉ gửi cho bác sĩ / quản trị viên: `LICH_HEN_MOI`, `LICH_HEN_DA_DOI`, `LICH_HEN_DA_HUY`, "
             "`KET_QUA_DUYET_DOI_LICH`, `CA_LAM_VIEC_THAY_DOI`, `YEU_CAU_DOI_LICH_MOI`, `YEU_CAU_NHUONG_CA`. Đã khai báo "
             "nhưng bản hiện tại chưa gửi: `HE_THONG`; frontend nên hiển thị được mọi giá trị bằng `noiDung`")

    # --- 3.11 ---
    chen_doan_sau(tl, MUC_311, "canDoiLich = true:",
                  "**daDoiBacSi = true**: ca khám đã được chuyển cho bác sĩ khác (bác sĩ cũ nhường ca, quản trị viên "
                  "duyệt); `bacSi` là bác sĩ mới, giờ khám, phòng và mã phiếu không đổi, lịch vẫn `DA_XAC_NHAN` và khám "
                  "bình thường. Bệnh nhân không đồng ý thì hủy / đổi được tới giờ khám (`hanHuyDoi` = giờ khám, không "
                  "trừ 2 giờ) và lần đổi này không tính vào số lần đổi; lịch mới sau khi đổi không còn cờ. Bệnh nhân có "
                  "tài khoản nhận thông báo `LICH_HEN_DOI_BAC_SI`, địa chỉ email liên hệ nhận email (3.13). Có từ bản "
                  "2.4.")

    # --- 3.13 ---
    tl.them_dong(MUC_313, None, kit.dong_bang(RONG_EMAIL, (
        "Đổi bác sĩ khám",
        "Ngay khi quản trị viên duyệt cho bác sĩ nhường ca khám cho bác sĩ khác: báo bác sĩ mới, giờ và phòng không đổi, "
        "và bệnh nhân hủy / đổi được tới giờ khám (3.11). Có từ bản 2.4.",
        "Phiếu khám.")), sau_dong_chua="Bác sĩ hủy lịch")
    viet_lai(tl, MUC_313, "Thông báo trên chuông đi kèm:",
             "**Thông báo trên chuông đi kèm:** lời nhắc lịch khám sinh thông báo `NHAC_LICH_KHAM`, bác sĩ hủy lịch sinh "
             "`LICH_HEN_BI_TU_CHOI`, đổi bác sĩ khám sinh `LICH_HEN_DOI_BAC_SI`, việc hẹn / đổi ngày tái khám và lời "
             "nhắc tái khám sinh thông báo `NHAC_TAI_KHAM` cho tài khoản phía bệnh nhân (mục 18). Email \"đặt lịch khám "
             "thành công\" và \"đã đổi lịch\" không sinh thông báo.")

    # --- Mô hình ---
    them_truong(tl, "4.22 PhieuKhamResponse", [DA_DOI_BAC_SI], "canDoiLich")
    them_truong(tl, "4.25 LichHenCuaToiResponse", [DA_DOI_BAC_SI], "canDoiLich")

    # --- Đoạn văn của các mục route ---
    viet_lai(tl, "14.1  Xem phiếu khám", "Hiện nút hủy / đổi theo",
             "Hiện nút hủy / đổi theo `duocHuyDoi`; khi `canDoiLich = true` hiện lời nhắc đổi lịch, khi `daDoiBacSi = "
             "true` hiện \"Đã đổi bác sĩ khám\" cạnh tên bác sĩ (3.11).")
    viet_lai(tl, "18. Thông báo", "Bệnh nhân nhận 5 loại",
             "Bệnh nhân nhận 6 loại (`LoaiThongBao` ở 3.7): `LICH_HEN_BI_TU_CHOI` khi bác sĩ hủy lịch (kèm lý do); "
             "`LICH_HEN_CAN_DOI` khi ca khám bị hủy và lịch cần đổi (3.11); `LICH_HEN_DOI_PHONG` khi ca khám đổi phòng; "
             "`LICH_HEN_DOI_BAC_SI` khi ca khám chuyển cho bác sĩ khác (3.11, từ bản 2.4); `NHAC_LICH_KHAM` trước giờ "
             "khám 3 ngày và 1 ngày; `NHAC_TAI_KHAM` khi bác sĩ hẹn hoặc đổi ngày tái khám và 7 ngày trước ngày đó "
             "(3.13). Thông báo cũ có thể còn loại `LICH_HEN_DA_XAC_NHAN` (bác sĩ xác nhận lịch, trước bản 2.3). Người "
             "nhận là tài khoản đã đặt lịch và tài khoản của hồ sơ người khám. Bệnh nhân không nhận thông báo khi tự "
             "đặt, tự hủy, tự đổi lịch hoặc khi bác sĩ ghi kết quả khám mà không hẹn tái khám.")
    viet_lai(tl, "B.11 Thông báo", "Bệnh nhân nhận thông báo khi bác sĩ",
             "Bệnh nhân nhận thông báo khi bác sĩ hủy lịch, khi ca khám bị hủy (cần đổi lịch), khi đổi phòng khám, khi "
             "ca khám chuyển cho bác sĩ khác, khi tới mốc nhắc lịch khám, và khi bác sĩ hẹn tái khám hoặc tới mốc nhắc "
             "tái khám (3.13). Thao tác của chính bệnh nhân (đặt, đổi, hủy, đánh giá) không sinh thông báo. Thông báo "
             "về bác sĩ hủy lịch, hủy ca, đổi phòng và đổi bác sĩ xuất hiện sau sự kiện khoảng 5 giây; lời nhắc và "
             "thông báo tái khám theo chu kỳ nhắc (mặc định 5 phút, chỉ trong 07:00-21:00).")

    # --- JSON mẫu ---
    for muc, ten, _ in CAP_NHAT[:-1]:
        sua_json_mau(tl, f"{muc}  {ten}", _them_da_doi_bac_si)

    # --- Dòng "Thay đổi ở v2.4" và cột "Thay đổi" ---
    for muc, ten, markup in CAP_NHAT:
        ghi_thay_doi(tl, muc, ten, markup)
        danh_dau_route(tl, muc)
