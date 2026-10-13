# Brief: make demo screens bilingual (Vietnamese / English)

Repo: `C:\Users\buiho\Downloads\doAnTotNghiep\eClinic_be`. The dev demo page lives in `scripts/demo/`:
`chung.js` (shared helpers, DO NOT edit it, someone else is editing it right now) and one plain script per screen in
`scripts/demo/man-hinh/*.js`. You edit ONLY the screen files assigned to you in your prompt. Do not touch any other
file, do not run git commands that change anything, do not start servers or docker.

## Goal
Every piece of text the user can see on your screens must exist in Vietnamese and English and follow the language
toggle. The page redraws the open screen when the language changes, so text must be chosen **at draw time**.

## Helpers already defined in `chung.js` (globals, use directly)
- `t(vi, en)` returns the string for the current language. Call it where the text is used:
  `nut(t('Lưu', 'Save'), ...)`, `bao('id', t('Đã lưu.', 'Saved.'), 'tot')`,
  ``t(`Còn ${n} chỗ`, `${n} left`)`` (template literals with values are fine in both arguments).
- `cap(vi, en)` is a pair resolved later; use it ONLY for text that is declared once when the file loads:
  the `tieuDe:` of `dangKy({...})` and module-level constants holding labels. `chuCua(x)` turns a `cap` into the
  string (a `cap` also converts itself when used as `textContent` or inside a template literal).
  `nhan({ KEY: cap('…', '…') })` wraps a label map so `MAP[key]` reads the current language (see top of `chung.js`).
- Enum label maps `TRANG_THAI`, `LOAI_YEU_CAU`, `TRANG_THAI_YEU_CAU`, `GIOI_TINH`, `QUAN_HE`, `NGUOI_DAT`, `LIEN_KET`,
  `VAI_TRO`, `THU` and the constant `CAN_DOI_LICH` are already bilingual: keep using them unchanged.

## Rules
1. Wrap every user-visible string: labels, headings, buttons, hints, placeholders, `title` / `alt` text, table
   headers, `<option>` texts, `confirm(...)` / `alert(...)` / `prompt(...)` texts, client-side validation messages,
   default error texts passed to `thongDiepLoi(json, '...')`, status lines, empty-state notes, calendar event titles
   and legends built in the script.
2. HTML templates given to `khuon()` (or `insertAdjacentHTML`): keep ONE template and put `${t('…', '…')}` at each
   text position, e.g. ``<label>${t('Họ tên', 'Full name')}</label>``, ``<input placeholder="${t('…', '…')}">``.
   Do not duplicate the whole template per language. A template stored in a module-level `const X = \`...\`` is
   evaluated once at load, so turn it into a function (`const X = () => \`...\``) and call it where it is used.
   Same for any other module-level constant that holds visible text: make it a function, or use `cap()` / `nhan()`.
   Inside HTML templates the English text must not contain `<`, `>` or `"` (use plain words); `'` is fine in text
   nodes, and inside a JS single-quoted string write it as `\'` or use a template literal / double quotes.
3. NEVER pass API data through `t()`. Names, codes, notes, `json.thongDiep`, catalog text and anything typed by a
   user arrive already in the right language and must be shown as they are. Only the fixed words around them are
   translated: ``t(`Bác sĩ ${ten} nghỉ`, `Dr. ${ten} is off`)`` is fine because `ten` is only interpolated.
4. Keep behaviour identical: no logic changes, no renamed ids / classes / dataset keys / form field names / API
   paths / enum values (`value="NAM"` stays; only the visible text changes). Do not reformat untouched code. Keep
   existing comments (they stay Vietnamese; comments are not translated). Keep line endings as LF and UTF-8.
5. `t` is now a global function. If a local variable or parameter named `t` (e.g. `.map((t) => ...)`,
   `function f(t)`, `const t = ...`) is in scope where you need to call `t(...)`, rename that local (e.g. `thuoc`,
   `muc`) consistently. Also do not declare new locals named `t`, `cap`, `nhan` or `chuCua`.
6. Text that is already English-only on purpose (UI screen names such as `'Appointment Requests'`, button names
   quoted from the UI like Accept / Decline) may stay as a plain string when it reads fine in both languages; a mixed
   string such as `'No data yet — không có yêu cầu nào.'` becomes a proper pair
   (`t('Chưa có yêu cầu nào.', 'No requests yet.')`). A `tieuDe` that is Vietnamese becomes `cap('…', '…')`.
7. Vietnamese words WITHOUT diacritics are invisible to the check script, so look for them yourself and wrap them
   too: e.g. `'Nam'`, `'Email'` (same in English, may stay), `'CCCD'` → `t('CCCD', 'National ID')`, `'STT'` →
   `t('STT', 'No.')`, `'Thao tác'`, `'Id'`, `'Trang'`, units such as `phút`, `ngày`, `lượt`.
8. Dates: keep the existing helpers (`ngayVn`, `ngayGioVn`, `gio`, `thuCuaNgay`); they are handled in `chung.js`.

## Glossary (use these English terms consistently)
lịch hẹn = appointment · đặt lịch = book an appointment / booking · phiếu khám = appointment ticket · mã tra cứu =
lookup code · mã phiếu khám = ticket code · số thứ tự (STT) = queue number · ca / ca làm việc / ca khám = shift ·
khung giờ = time slot · lượt (khám) = visit slot / turn · chỗ = place(s) (`còn 3` = `3 left`, `Hết chỗ` = `Full`) ·
chuyên khoa = specialty · bác sĩ = doctor · bệnh nhân = patient · hồ sơ bệnh nhân = patient profile · người thân =
relative · người giám hộ = guardian · CCCD = national ID · số bảo hiểm y tế = health insurance number · phòng khám
(room) = room; phòng khám (the organisation) = the clinic · tầng = floor · lý do khám = reason for visit · triệu
chứng = symptoms · chẩn đoán = diagnosis · lời dặn = doctor's advice · đơn thuốc = prescription · thuốc = medicine ·
liều dùng = dosage · tái khám = follow-up visit · lượt khám / lần khám = visit · bệnh án / hồ sơ khám = medical
record · xác nhận = confirm · từ chối = decline (appointment) / reject (request) · hủy = cancel · đổi lịch =
reschedule · yêu cầu đổi ca = shift-change request · xin nghỉ = leave request · duyệt = approve · rút = withdraw ·
đánh giá = review / rating · vô hiệu hóa = deactivate · kích hoạt = activate · ngừng hoạt động = inactive · quản trị
viên = administrator · tài khoản = account · mật khẩu = password · đăng nhập = sign in · đăng xuất = sign out · đăng
ký = register · xác minh = verify · đối chiếu = identity check · tải lại = reload · trang = page (`Trang 1 / 3` =
`Page 1 / 3`) · thao tác = actions · trạng thái = status · ghi chú = note · không bắt buộc = optional · bắt buộc =
required · Đang tải... = Loading... · Không tải được X = Could not load X · Không thực hiện được = The action failed.

English style: plain, short, sentence case for sentences and labels, no exclamation marks.

## How to verify (run from the repo root, repeat until clean)
- `node --check scripts/demo/man-hinh/<file>.js` for each file you edited: must pass.
- `node scripts/kiem-tra-demo-ngon-ngu.js` lists `file:line: text` for every string literal that still contains
  Vietnamese letters outside the first argument of `t(` / `cap(`. Lines for YOUR files must be gone (lines for
  other files are other people's work in progress: ignore them). Tip: pipe through `grep <your file name>`.
- Read each of your files once more from top to bottom for: Vietnamese words without diacritics, a shadowed `t`,
  module-level text evaluated at load, API data wrongly inside `t()`, and broken quotes in English text.

## Report back (short)
Per file: number of pairs added (rough), any local you renamed, any module-level constant you turned into a
function, and anything you were unsure about or deliberately left as it was. Say plainly if a check did not pass.
