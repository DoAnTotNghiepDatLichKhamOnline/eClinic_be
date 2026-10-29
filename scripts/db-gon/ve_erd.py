"""Vẽ ERD (Mermaid) từ schema đang chạy của stack thử -> docs/ERD.md + docs/ERD.mmd (DOANTOTNGH-19).

    PYTHONUTF8=1 /c/Users/buiho/AppData/Local/Python/bin/python scripts/db-gon/ve_erd.py

Cần MySQL của stack thử (`chung`, container eclinic-mysql) đã chạy Flyway tới bản mới nhất. Đọc information_schema:
mọi bảng (trừ flyway_schema_history), mọi cột với kiểu gốc (không kèm độ dài: kiểu Mermaid không chứa dấu phẩy), khoá
PK / FK / UK, đường quan hệ từ khoá ngoại. Vẽ PNG: xem lệnh mermaid-cli trong docs/ERD.md.
"""

import sys

import chung

# Ghi chú ngắn trên các cột không tự nói lên ý nghĩa (cột JSON, cột phân loại của bảng gộp)
GHI_CHU = {
    ("bac_si", "anh_gioi_thieu"): "JSON: ảnh làm việc / chứng chỉ",
    ("bac_si", "anh_id_cuoi"): "id ảnh lớn nhất đã cấp",
    ("ho_so_benh_nhan", "tu_choi_lien_ket"): "JSON: các lần từ chối liên kết",
    ("ho_so_benh_an", "ma_icd_chinh"): "mã trong danh-muc/icd10.tsv",
    ("lich_su_thay_doi", "loai"): "LICH_HEN | CA_LAM_VIEC | CAU_HINH | BENH_AN",
    ("hang_cho_gui", "hang"): "SU_KIEN | EMAIL_GUI",
}

# 13 bảng đã bỏ -> chỗ mới (33 -> 22 bảng)
DA_BO = [
    ("danh_gia", "cột `danh_gia_*` của `lich_hen`"),
    ("nhac_lich_hen", "cột `nhac_moc_da_xu_ly`, `nhac_moc_da_gui` của `lich_hen`"),
    ("nguoi_giam_ho", "cột `giam_ho_*` của `lich_hen` (người giám hộ của từng lượt khám)"),
    ("nhac_tai_kham", "cột `tai_kham_ngay_da_bao`, `tai_kham_da_nhac_truoc` của `ho_so_benh_an`"),
    ("lien_ket_ho_so_bi_tu_choi", "cột JSON `tu_choi_lien_ket` của `ho_so_benh_nhan`"),
    ("lich_su_lich_hen", "`lich_su_thay_doi`, `loai = LICH_HEN`"),
    ("lich_su_ca_lam_viec", "`lich_su_thay_doi`, `loai = CA_LAM_VIEC`"),
    ("lich_su_cau_hinh", "`lich_su_thay_doi`, `loai = CAU_HINH`"),
    ("lich_su_sua_benh_an", "`lich_su_thay_doi`, `loai = BENH_AN`"),
    ("su_kien_thong_bao", "`hang_cho_gui`, `hang = SU_KIEN`"),
    ("email_gui", "`hang_cho_gui`, `hang = EMAIL_GUI`"),
    ("icd10", "tệp `common/src/main/resources/danh-muc/icd10.tsv` (191 mã, nạp vào bộ nhớ)"),
    ("anh_bac_si", "cột JSON `anh_gioi_thieu` + bộ đếm `anh_id_cuoi` của `bac_si`"),
]


def doc_schema():
    cot = chung.sql_dong(
        "SELECT table_name, column_name, data_type, column_key, is_nullable FROM information_schema.columns "
        "WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history' "
        "ORDER BY table_name, ordinal_position")
    fk = chung.sql_dong(
        "SELECT table_name, column_name, referenced_table_name FROM information_schema.key_column_usage "
        "WHERE table_schema = DATABASE() AND referenced_table_name IS NOT NULL ORDER BY table_name, column_name")
    return cot, fk


def ve(cot, fk):
    la_fk = {(b, c) for b, c, _ in fk}
    nullable = {(b, c): n == "YES" for b, c, _, _, n in cot}
    unique = {(b, c) for b, c, _, k, _ in cot if k in ("PRI", "UNI")}
    dong = ["erDiagram"]
    for bang, cha in sorted({(b, r) for b, _, r in fk}):
        for _, c, r in [x for x in fk if x[0] == bang and x[2] == cha]:
            ben_cha = "|o" if nullable[(bang, c)] else "||"
            ben_con = "o|" if (bang, c) in unique else "o{"
            dong.append(f'    {cha} {ben_cha}--{ben_con} {bang} : "{c}"')
    bang_hien = None
    for bang, c, kieu, khoa, _ in cot:
        if bang != bang_hien:
            if bang_hien:
                dong.append("    }")
            dong.append(f"    {bang} {{")
            bang_hien = bang
        k = [x for x, co in (("PK", khoa == "PRI"), ("FK", (bang, c) in la_fk), ("UK", khoa == "UNI")) if co]
        ghi_chu = GHI_CHU.get((bang, c))
        dong.append(f"        {kieu} {c}" + (" " + ", ".join(k) if k else "")
                    + (f' "{ghi_chu}"' if ghi_chu else ""))
    dong.append("    }")
    return "\n".join(dong) + "\n", len({b for b, *_ in cot})


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    cot, fk = doc_schema()
    mmd, so_bang = ve(cot, fk)
    thu_muc = chung.GOC / "docs"
    thu_muc.mkdir(exist_ok=True)
    (thu_muc / "ERD.mmd").write_text(mmd, encoding="utf-8", newline="\n")
    bo = "\n".join(f"| `{b}` | {moi} |" for b, moi in DA_BO)
    md = f"""# ERD — {so_bang} bảng

Sinh từ schema đang chạy bằng `scripts/db-gon/ve_erd.py` (DOANTOTNGH-19). Không sửa tay: chạy lại script sau mỗi
migration. Ảnh: `ERD.png` cạnh tệp này (dùng thay Hình 3.3 trong báo cáo).

Vẽ lại ảnh (Git Bash, thư mục gốc repo):

```bash
MSYS_NO_PATHCONV=1 docker run --rm -v "$PWD/docs":/data minlag/mermaid-cli:11.4.2 \\
  -i /data/ERD.mmd -o /data/ERD.png -w 4000 -b white
```

Kiểu cột là kiểu gốc của MySQL (bỏ độ dài). `lich_su_thay_doi` và `hang_cho_gui` là bảng gộp: cột `loai` / `hang`
cho biết dòng thuộc loại nào, các cột còn lại chỉ dùng cho loại tương ứng (ràng buộc CHECK theo loại trong V42 / V43).
`hang_cho_gui` không có khoá ngoại.

```mermaid
{mmd}```

## 13 bảng cũ đã bỏ (33 → {so_bang} bảng)

| Bảng cũ | Nay nằm ở |
|---|---|
{bo}
"""
    (thu_muc / "ERD.md").write_text(md, encoding="utf-8", newline="\n")
    print(f"docs/ERD.md, docs/ERD.mmd: {so_bang} bảng, {len(fk)} khoá ngoại")


if __name__ == "__main__":
    main()
