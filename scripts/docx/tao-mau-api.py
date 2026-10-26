"""Dựng tệp mẫu định dạng (reference doc) cho pandoc khi xuất tài liệu API docs/api/*.md ra .docx
(scripts/xuat-docx-api.js). Lấy reference.docx mặc định của pandoc rồi sửa style cho giống 2 tài liệu .docx cũ
(scripts/api-doc/kit.py): chữ Arial, code Consolas trên nền xám có vạch trái, bảng có viền và dòng tiêu đề nền xanh đậm,
trang A4 lề 2 cm. Thêm các style mà scripts/docx/bo-cuc.lua gán qua custom-style: NhanBia, Title, Subtitle (trang bìa),
ThanhRoute (dải phương thức + đường dẫn dưới tiêu đề route), QuyenTruyCap (dòng "Quyền truy cập: ..." dưới dải đó),
NhanKhoi (nhãn khối trong route). Đầu trang ghi
"eClinic — Tài liệu API: VAI_TRO_TAI_LIEU"; xuat-docx-api.js thay chữ VAI_TRO_TAI_LIEU bằng vai trò của từng tài liệu.
Chân trang "Trang X / N". Ghi đè scripts/docx/mau-api.docx; chạy lại cho cùng kết quả.
Chỉ dùng thư viện chuẩn, cần Docker để lấy tệp mặc định của pandoc.

    python scripts/docx/tao-mau-api.py
"""

import re
import subprocess
import zipfile
from io import BytesIO
from pathlib import Path

ANH_PANDOC = "pandoc/core:3.6.4"
TEP_RA = Path(__file__).resolve().parent / "mau-api.docx"
NGAY_CO_DINH = (2026, 10, 21, 0, 0, 0)

XANH_DAM = "1F3A5F"
XANH_CODE = "0B4F8A"
NEN_CODE = "F3F5F8"
NEN_CODE_TRONG_DONG = "EAF0F7"
VIEN_BANG = "C9D1DA"
MAU_LIEN_KET = "1565C0"
XAM_CHU = "5F6B7A"
VAI_TRO = "VAI_TRO_TAI_LIEU"

ARIAL = '<w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:eastAsia="Arial" w:cs="Arial"/>'
CONSOLAS = '<w:rFonts w:ascii="Consolas" w:hAnsi="Consolas" w:eastAsia="Consolas" w:cs="Consolas"/>'


def co(sz):
    return f'<w:sz w:val="{sz}"/><w:szCs w:val="{sz}"/>'


def vien(canh, mau, sz=4):
    return "".join(f'<w:{c} w:val="single" w:sz="{sz}" w:space="0" w:color="{mau}"/>' for c in canh)


def doan(style_id, ppr, rpr, ten=None):
    return (f'<w:style w:type="paragraph" w:customStyle="1" w:styleId="{style_id}"><w:name w:val="{ten or style_id}"/>'
            f'<w:basedOn w:val="Normal"/><w:next w:val="BodyText"/><w:qFormat/><w:pPr>{ppr}</w:pPr>'
            f'<w:rPr>{rpr}</w:rPr></w:style>')


def tieu_de(style_id, ten, cap, sz, ppr_them="", rpr_them=""):
    return (f'<w:style w:type="paragraph" w:styleId="{style_id}"><w:name w:val="{ten}"/><w:basedOn w:val="Normal"/>'
            f'<w:next w:val="BodyText"/><w:uiPriority w:val="9"/><w:qFormat/>'
            f'<w:pPr><w:keepNext/><w:keepLines/>{ppr_them}<w:outlineLvl w:val="{cap}"/></w:pPr>'
            f'<w:rPr>{rpr_them or ARIAL}<w:b/><w:bCs/><w:color w:val="{XANH_DAM}"/>{co(sz)}</w:rPr></w:style>')


STYLE = {
    "BodyText": ('<w:style w:type="paragraph" w:styleId="BodyText"><w:name w:val="Body Text"/>'
                 '<w:basedOn w:val="Normal"/><w:qFormat/><w:pPr><w:spacing w:before="60" w:after="120"/></w:pPr>'
                 '</w:style>'),
    "Compact": ('<w:style w:type="paragraph" w:customStyle="1" w:styleId="Compact"><w:name w:val="Compact"/>'
                '<w:basedOn w:val="BodyText"/><w:qFormat/><w:pPr><w:spacing w:before="20" w:after="20"/></w:pPr>'
                '</w:style>'),
    # Trang bìa: NhanBia "eClinic Backend", Title "Tài liệu API", Subtitle = vai trò có gạch dưới, rồi bảng thông tin
    "NhanBia": doan("NhanBia", '<w:spacing w:before="0" w:after="60"/>',
                    f'{ARIAL}<w:b/><w:bCs/><w:color w:val="{XAM_CHU}"/><w:spacing w:val="40"/>{co(22)}'),
    "Title": ('<w:style w:type="paragraph" w:styleId="Title"><w:name w:val="Title"/><w:basedOn w:val="Normal"/>'
              '<w:next w:val="BodyText"/><w:uiPriority w:val="10"/><w:qFormat/>'
              '<w:pPr><w:spacing w:before="0" w:after="80"/></w:pPr>'
              f'<w:rPr>{ARIAL}<w:b/><w:bCs/><w:color w:val="{XANH_DAM}"/>{co(56)}</w:rPr></w:style>'),
    "Subtitle": ('<w:style w:type="paragraph" w:styleId="Subtitle"><w:name w:val="Subtitle"/><w:basedOn w:val="Normal"/>'
                 '<w:next w:val="BodyText"/><w:uiPriority w:val="11"/><w:qFormat/>'
                 f'<w:pPr><w:pBdr>{vien(["bottom"], XANH_DAM, 12).replace("w:space=\"0\"", "w:space=\"8\"")}</w:pBdr>'
                 '<w:spacing w:before="0" w:after="200"/></w:pPr>'
                 f'<w:rPr>{ARIAL}<w:color w:val="{XANH_DAM}"/>{co(30)}</w:rPr></w:style>'),
    # Dải dưới tiêu đề route: huy hiệu phương thức (run tô nền, do bo-cuc.lua dựng) + đường dẫn
    "ThanhRoute": doan("ThanhRoute",
                       '<w:keepNext/>'
                       f'<w:pBdr>{vien(["left"], XANH_DAM, 18).replace("w:space=\"0\"", "w:space=\"6\"")}</w:pBdr>'
                       f'<w:shd w:val="clear" w:color="auto" w:fill="{NEN_CODE}"/><w:spacing w:before="0" w:after="120"/>',
                       f'{CONSOLAS}<w:b/><w:bCs/>{co(18)}'),
    # Dòng "Quyền truy cập: ..." ngay dưới dải route; nhãn đậm nhờ Strong
    "QuyenTruyCap": doan("QuyenTruyCap", '<w:keepNext/><w:spacing w:before="0" w:after="80"/>',
                         f'{ARIAL}<w:color w:val="{XAM_CHU}"/>{co(18)}'),
    # Nhãn khối trong route (REQUEST, RESPONSE THÀNH CÔNG, MÃ LỖI...)
    "NhanKhoi": doan("NhanKhoi", '<w:keepNext/><w:spacing w:before="160" w:after="60"/>',
                     f'{ARIAL}<w:b/><w:bCs/><w:caps/><w:color w:val="{XAM_CHU}"/><w:spacing w:val="20"/>{co(16)}'),
    "Heading1": tieu_de("Heading1", "heading 1", 0, 30, '<w:spacing w:before="480" w:after="160"/>'),
    "Heading2": tieu_de("Heading2", "heading 2", 1, 25, '<w:spacing w:before="320" w:after="100"/>'),
    # Tiêu đề route (#### METHOD /đường/dẫn) và mô hình (#### `Ten`): dải nền xanh nhạt, vạch trái đậm
    "Heading3": tieu_de("Heading3", "heading 3", 2, 20,
                        f'<w:pBdr>{vien(["left"], XANH_DAM, 18).replace("w:space=\"0\"", "w:space=\"6\"")}</w:pBdr>'
                        f'<w:shd w:val="clear" w:color="auto" w:fill="{NEN_CODE_TRONG_DONG}"/>'
                        '<w:spacing w:before="280" w:after="100"/>', CONSOLAS),
    "Heading4": tieu_de("Heading4", "heading 4", 3, 20, '<w:spacing w:before="200" w:after="60"/>'),
    "SourceCode": ('<w:style w:type="paragraph" w:customStyle="1" w:styleId="SourceCode"><w:name w:val="Source Code"/>'
                   '<w:basedOn w:val="Normal"/><w:link w:val="VerbatimChar"/>'
                   '<w:pPr><w:wordWrap w:val="off"/><w:keepLines/>'
                   f'<w:pBdr>{vien(["left"], XANH_DAM, 18).replace("w:space=\"0\"", "w:space=\"6\"")}</w:pBdr>'
                   f'<w:shd w:val="clear" w:color="auto" w:fill="{NEN_CODE}"/>'
                   '<w:spacing w:before="60" w:after="160" w:line="252" w:lineRule="auto"/></w:pPr>'
                   f'<w:rPr>{CONSOLAS}{co(17)}</w:rPr></w:style>'),
    "VerbatimChar": ('<w:style w:type="character" w:customStyle="1" w:styleId="VerbatimChar">'
                     '<w:name w:val="Verbatim Char"/><w:basedOn w:val="DefaultParagraphFont"/><w:link w:val="SourceCode"/>'
                     f'<w:rPr>{CONSOLAS}<w:color w:val="{XANH_CODE}"/>{co(18)}</w:rPr></w:style>'),
    "Hyperlink": ('<w:style w:type="character" w:styleId="Hyperlink"><w:name w:val="Hyperlink"/>'
                  f'<w:basedOn w:val="DefaultParagraphFont"/><w:rPr><w:color w:val="{MAU_LIEN_KET}"/></w:rPr></w:style>'),
    "Table": ('<w:style w:type="table" w:default="1" w:styleId="Table"><w:name w:val="Table"/>'
              '<w:basedOn w:val="TableNormal"/><w:qFormat/>'
              '<w:tblPr><w:tblInd w:w="0" w:type="dxa"/>'
              f'<w:tblBorders>{vien(["top", "left", "bottom", "right", "insideH", "insideV"], VIEN_BANG)}</w:tblBorders>'
              '<w:tblCellMar><w:top w:w="40" w:type="dxa"/><w:left w:w="80" w:type="dxa"/>'
              '<w:bottom w:w="40" w:type="dxa"/><w:right w:w="80" w:type="dxa"/></w:tblCellMar></w:tblPr>'
              '<w:tblStylePr w:type="firstRow"><w:rPr><w:b/><w:bCs/><w:color w:val="FFFFFF"/></w:rPr>'
              f'<w:tcPr><w:shd w:val="clear" w:color="auto" w:fill="{XANH_DAM}"/><w:vAlign w:val="bottom"/></w:tcPr>'
              '</w:tblStylePr></w:style>'),
}

MAC_DINH = (f'<w:rPrDefault><w:rPr>{ARIAL}{co(20)}<w:lang w:val="vi-VN" w:eastAsia="en-US" w:bidi="ar-SA"/>'
            '</w:rPr></w:rPrDefault><w:pPrDefault><w:pPr><w:spacing w:after="120"/></w:pPr></w:pPrDefault>')

KHONG_GIAN_TEN = ('xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" '
                  'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"')
RPR_DAU_CHAN = f'<w:rPr><w:color w:val="{XAM_CHU}"/>{co(16)}</w:rPr>'


def chu(t):
    return f'<w:r>{RPR_DAU_CHAN}<w:t xml:space="preserve">{t}</w:t></w:r>'


def truong(lenh):
    return (f'<w:r>{RPR_DAU_CHAN}<w:fldChar w:fldCharType="begin"/></w:r>'
            f'<w:r>{RPR_DAU_CHAN}<w:instrText xml:space="preserve"> {lenh} </w:instrText></w:r>'
            f'<w:r>{RPR_DAU_CHAN}<w:fldChar w:fldCharType="separate"/></w:r>'
            f'{chu("1")}<w:r>{RPR_DAU_CHAN}<w:fldChar w:fldCharType="end"/></w:r>')


RID_DAU_TRANG = "rIdDauTrang1"
DAU_TRANG = ('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n'
             f'<w:hdr {KHONG_GIAN_TEN}><w:p><w:pPr>'
             f'<w:pBdr>{vien(["bottom"], VIEN_BANG).replace("w:space=\"0\"", "w:space=\"4\"")}</w:pBdr>'
             f'<w:jc w:val="right"/></w:pPr>{chu("eClinic — Tài liệu API: " + VAI_TRO)}</w:p></w:hdr>')
RID_CHAN_TRANG = "rIdChanTrang1"
CHAN_TRANG = ('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n'
              f'<w:ftr {KHONG_GIAN_TEN}><w:p><w:pPr><w:jc w:val="center"/></w:pPr>'
              f'{chu("Trang ")}{truong("PAGE")}{chu(" / ")}{truong("NUMPAGES")}</w:p></w:ftr>')
SECT_PR = (f'<w:sectPr><w:headerReference w:type="default" r:id="{RID_DAU_TRANG}"/>'
           f'<w:footerReference w:type="default" r:id="{RID_CHAN_TRANG}"/>'
           '<w:footnotePr><w:numRestart w:val="eachSect"/></w:footnotePr>'
           '<w:pgSz w:w="11906" w:h="16838"/>'
           '<w:pgMar w:top="1134" w:right="1134" w:bottom="1134" w:left="1134" w:header="567" w:footer="567" w:gutter="0"/>'
           '</w:sectPr>')


def sua_styles(x):
    x = re.sub(r"<w:rPrDefault>.*?</w:pPrDefault>", lambda _: MAC_DINH, x, count=1, flags=re.S)
    for style_id, xml in STYLE.items():
        mau = re.compile(rf'<w:style [^>]*w:styleId="{style_id}".*?</w:style>', re.S)
        if mau.search(x):
            x = mau.sub(lambda _: xml, x, count=1)
        else:
            x = x.replace("</w:styles>", xml + "</w:styles>")
    return x


def sua_document(x):
    x, n = re.subn(r"<w:sectPr>.*?</w:sectPr>", lambda _: SECT_PR, x, count=1, flags=re.S)
    if n != 1:
        raise SystemExit("Không thấy w:sectPr trong word/document.xml của tệp mặc định")
    return x


def main():
    goc = subprocess.run(["docker", "run", "--rm", ANH_PANDOC, "--print-default-data-file", "reference.docx"],
                         capture_output=True, check=True).stdout
    vao = zipfile.ZipFile(BytesIO(goc))
    ra = BytesIO()
    with zipfile.ZipFile(ra, "w", zipfile.ZIP_DEFLATED) as z:
        def ghi(ten, noi_dung):
            z.writestr(zipfile.ZipInfo(ten, NGAY_CO_DINH), noi_dung, zipfile.ZIP_DEFLATED)

        for ten in vao.namelist():
            noi_dung = vao.read(ten).decode("utf-8") if ten.endswith((".xml", ".rels")) else vao.read(ten)
            if ten == "word/styles.xml":
                noi_dung = sua_styles(noi_dung)
            elif ten == "word/document.xml":
                noi_dung = sua_document(noi_dung)
            elif ten == "word/_rels/document.xml.rels":
                noi_dung = noi_dung.replace("</Relationships>", "".join(
                    f'<Relationship Id="{rid}" Target="{loai}1.xml" Type="http://schemas.openxmlformats.org/'
                    f'officeDocument/2006/relationships/{loai}"/>'
                    for rid, loai in ((RID_DAU_TRANG, "header"), (RID_CHAN_TRANG, "footer"))) + "</Relationships>")
            elif ten == "[Content_Types].xml":
                noi_dung = noi_dung.replace("</Types>", "".join(
                    f'<Override PartName="/word/{loai}1.xml" ContentType="application/vnd.openxmlformats-officedocument.'
                    f'wordprocessingml.{loai}+xml"/>' for loai in ("header", "footer")) + "</Types>")
            ghi(ten, noi_dung)
        ghi("word/header1.xml", DAU_TRANG)
        ghi("word/footer1.xml", CHAN_TRANG)
    TEP_RA.write_bytes(ra.getvalue())
    print(f"Đã ghi {TEP_RA} ({len(ra.getvalue())} byte)")


if __name__ == "__main__":
    main()
