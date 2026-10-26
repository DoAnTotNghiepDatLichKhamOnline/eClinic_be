package iuh.fit.se.eclinic.identity.dulieumau;

import java.util.List;
import java.util.Map;

/**
 * Bản tiếng Anh của nội dung dữ liệu mẫu trong {@link DuLieuMau} và {@link HoSoBacSiMau}: chuyên khoa + phòng khám theo
 * tên tiếng Việt của chuyên khoa, hồ sơ bác sĩ theo số thứ tự. Họ tên, học vị (chữ viết tắt) không có ở đây vì không
 * dịch.
 */
public final class DuLieuMauTiengAnh {

    private DuLieuMauTiengAnh() {
    }

    public record ChuyenKhoaEn(String ten, String moTa, String tenPhong, String tang) {
    }

    /**
     * @param tieuSu bản tiếng Anh của {@link DuLieuMau.BacSiMau#tieuSu()}; các mục còn lại ứng với {@link HoSoBacSiMau}
     */
    public record HoSoBacSiEn(String tieuSu, String chucVu, String gioiThieuNgan, List<String> daoTao,
            List<String> congTac, List<String> linhVuc) {
    }

    /** Bản tiếng Anh của {@link DuLieuMau#TRIEU_CHUNG}, theo tên chuyên khoa tiếng Việt. */
    public static final Map<String, String> TRIEU_CHUNG = Map.of(
            "Nội tổng quát", "fever, tiredness, long-lasting cough, mild shortness of breath, stomach ache, bloating,"
                    + " heartburn, diarrhea, constipation, nausea, headache, dizziness, weight loss, diabetes,"
                    + " being very thirsty, general health check-up",
            "Nhi khoa", "child with fever, child coughing, runny nose, vomiting, diarrhea in children, poor appetite,"
                    + " slow weight gain, rash in children, fussy baby, nutrition and vaccination advice for children"
                    + " under 15",
            "Da liễu", "acne, itchy skin, red rash, hives, skin allergy, eczema, psoriasis, fungal skin infection,"
                    + " hair loss, dandruff, dark spots, freckles, warts, nail fungus",
            "Tai Mũi Họng", "sore throat, hoarse voice, blocked nose, runny nose, sinusitis, frequent sneezing, earache,"
                    + " ringing in the ears, hearing loss, ear discharge, snoring, small object stuck in the throat",
            "Tim mạch", "high blood pressure, palpitations, fast heartbeat, irregular heartbeat, chest tightness on exertion,"
                    + " shortness of breath on exertion, swollen legs, high cholesterol, heart disease follow-up",
            "Sản phụ khoa", "antenatal check-up, missed period, irregular periods, period pain, unusual vaginal discharge,"
                    + " vaginal itching, contraception advice, planning a pregnancy, routine gynecological check-up",
            "Mắt", "blurred vision, short-sightedness, long-sightedness, astigmatism, tired eyes, dry eyes, red eyes,"
                    + " itchy eyes, watery eyes, pink eye, stye, vision test, glasses prescription",
            "Răng Hàm Mặt", "toothache, tooth decay, sensitive teeth, bleeding gums, gum inflammation, bad breath,"
                    + " impacted wisdom tooth, chipped tooth, scaling, braces");

    public static final Map<String, ChuyenKhoaEn> CHUYEN_KHOA = Map.of(
            "Nội tổng quát", new ChuyenKhoaEn("General Internal Medicine",
                    "Examination and treatment of common internal diseases: respiratory, digestive, endocrine;"
                            + " general health check-ups.",
                    "Room 101", "Floor 1"),
            "Nhi khoa", new ChuyenKhoaEn("Pediatrics",
                    "Examination, nutrition advice and treatment for children from newborn to 15 years old.",
                    "Room 102", "Floor 1"),
            "Da liễu", new ChuyenKhoaEn("Dermatology",
                    "Examination and treatment of skin, hair and nail conditions: acne, dermatitis, allergies,"
                            + " fungal skin infections.",
                    "Room 103", "Floor 1"),
            "Tai Mũi Họng", new ChuyenKhoaEn("Ear, Nose and Throat",
                    "Examination and treatment of sinusitis, sore throat and middle ear infection; ENT endoscopy.",
                    "Room 104", "Floor 1"),
            "Tim mạch", new ChuyenKhoaEn("Cardiology",
                    "Examination and follow-up of high blood pressure, arrhythmia, coronary artery disease and"
                            + " heart failure.",
                    "Room 105", "Floor 1"),
            "Sản phụ khoa", new ChuyenKhoaEn("Obstetrics and Gynecology",
                    "Routine antenatal check-ups, gynecological examination, reproductive health advice.",
                    "Room 106", "Floor 1"),
            "Mắt", new ChuyenKhoaEn("Ophthalmology",
                    "Examination and treatment of refractive errors, conjunctivitis and cataracts; vision tests.",
                    "Room 107", "Floor 1"),
            "Răng Hàm Mặt", new ChuyenKhoaEn("Dentistry",
                    "Dental check-ups, fillings, extractions, scaling and orthodontic advice.",
                    "Room 108", "Floor 1"));

    public static final Map<Integer, HoSoBacSiEn> BAC_SI = Map.ofEntries(
            Map.entry(1, new HoSoBacSiEn(
                    "Over 18 years of examining and treating internal diseases, with strengths in digestive and"
                            + " endocrine conditions.",
                    "Head of General Internal Medicine",
                    "PhD in internal medicine with 18 years of treating digestive and endocrine diseases.",
                    List.of("2001 - 2007: Doctor of Medicine, University of Medicine and Pharmacy at Ho Chi Minh City",
                            "2008 - 2011: Master of Internal Medicine,"
                                    + " University of Medicine and Pharmacy at Ho Chi Minh City",
                            "2013 - 2017: PhD in Medicine, specialising in Gastroenterology"),
                    List.of("2007 - 2015: Attending physician, Gastroenterology department, city-level hospital",
                            "2015 - 2021: Deputy head of the General Internal Medicine department",
                            "2021 - present: Head of General Internal Medicine, eClinic"),
                    List.of("Stomach and duodenal ulcers, gastroesophageal reflux",
                            "Diabetes, thyroid disease",
                            "High blood pressure, lipid disorders",
                            "General health check-ups for adults"))),
            Map.entry(2, new HoSoBacSiEn(
                    "Focuses on general health check-ups and managing chronic conditions: diabetes, lipid disorders.",
                    "Attending physician",
                    "Long-term follow-up for patients with diabetes and lipid disorders.",
                    List.of("2010 - 2016: Doctor of Medicine, Pham Ngoc Thach University of Medicine",
                            "2018 - 2020: Master of Internal Medicine,"
                                    + " University of Medicine and Pharmacy at Ho Chi Minh City"),
                    List.of("2016 - 2022: Physician, General Internal Medicine department, district hospital",
                            "2022 - present: Attending physician, General Internal Medicine, eClinic"),
                    List.of("General health check-ups, screening for chronic diseases",
                            "Type 2 diabetes, prediabetes",
                            "Lipid disorders, fatty liver",
                            "Nutrition and lifestyle advice"))),
            Map.entry(3, new HoSoBacSiEn(
                    "20 years of experience in pediatrics,"
                            + " specialising in children's respiratory health and nutrition.",
                    "Head of Pediatrics",
                    "20 years of caring for children, specialising in respiratory diseases and nutrition.",
                    List.of("1999 - 2005: Doctor of Medicine, University of Medicine and Pharmacy at Ho Chi Minh City",
                            "2007 - 2009: Specialist Level I in Pediatrics",
                            "2012 - 2014: Specialist Level II in Pediatrics"),
                    List.of("2005 - 2016: Physician, Respiratory department, city-level children's hospital",
                            "2016 - 2020: Deputy head of the General Pediatrics department",
                            "2020 - present: Head of Pediatrics, eClinic"),
                    List.of("Bronchitis, pneumonia and asthma in children",
                            "Malnutrition, poor appetite, slow weight gain",
                            "Fever, diarrhea and common infections",
                            "Age-appropriate nutrition advice"))),
            Map.entry(4, new HoSoBacSiEn(
                    "Examination and vaccination advice, monitoring the physical development of young children.",
                    "Attending physician",
                    "General pediatric care, vaccination advice and child development follow-up.",
                    List.of("2011 - 2017: Doctor of Medicine, Pham Ngoc Thach University of Medicine",
                            "2019 - 2021: Specialist Level I in Pediatrics"),
                    List.of("2017 - 2022: Physician, Pediatrics department, district hospital",
                            "2022 - present: Attending physician, Pediatrics, eClinic"),
                    List.of("Routine check-ups for newborns and young children",
                            "Vaccination schedule advice",
                            "Monitoring height, weight and motor development",
                            "Common respiratory and digestive illnesses in children"))),
            Map.entry(5, new HoSoBacSiEn(
                    "Treats acne, atopic dermatitis, psoriasis and allergic skin diseases.",
                    "Head of Dermatology",
                    "Treatment of acne, atopic dermatitis, psoriasis and allergic skin diseases.",
                    List.of("2007 - 2013: Doctor of Medicine, University of Medicine and Pharmacy at Ho Chi Minh City",
                            "2015 - 2017: Master of Dermatology,"
                                    + " University of Medicine and Pharmacy at Ho Chi Minh City"),
                    List.of("2013 - 2020: Attending physician, dermatology hospital",
                            "2020 - present: Head of Dermatology, eClinic"),
                    List.of("Acne and acne scars",
                            "Atopic dermatitis, eczema, hives",
                            "Psoriasis, vitiligo",
                            "Skin allergies caused by medicines and cosmetics"))),
            Map.entry(6, new HoSoBacSiEn(
                    "General dermatology, skin care advice and treatment of fungal skin infections.",
                    "Attending physician",
                    "General dermatology, skin care advice and treatment of fungal skin infections.",
                    List.of("2014 - 2020: Doctor of Medicine, Pham Ngoc Thach University of Medicine",
                            "2021: Certificate of orientation training in Dermatology"),
                    List.of("2020 - 2023: Physician, Dermatology department, district hospital",
                            "2023 - present: Attending physician, Dermatology, eClinic"),
                    List.of("Fungal skin infections, tinea versicolor, ringworm",
                            "Contact dermatitis, seborrheic dermatitis",
                            "Skin care advice by skin type",
                            "Warts, skin tags"))),
            Map.entry(7, new HoSoBacSiEn(
                    "Endoscopy and treatment of chronic sinusitis, middle ear infection and tonsillitis.",
                    "Head of Ear, Nose and Throat",
                    "Endoscopy and treatment of chronic sinusitis, middle ear infection and tonsillitis.",
                    List.of("2003 - 2009: Doctor of Medicine, University of Medicine and Pharmacy at Ho Chi Minh City",
                            "2011 - 2013: Specialist Level I in Otolaryngology",
                            "2016 - 2018: Specialist Level II in Otolaryngology"),
                    List.of("2009 - 2019: Attending physician, ear, nose and throat hospital",
                            "2019 - present: Head of Ear, Nose and Throat, eClinic"),
                    List.of("Acute and chronic rhinosinusitis",
                            "Middle ear infection, tinnitus, hearing loss",
                            "Tonsillitis, adenoiditis",
                            "ENT endoscopy"))),
            Map.entry(8, new HoSoBacSiEn(
                    "ENT examination for adults and children, treatment of allergic rhinitis.",
                    "Attending physician",
                    "ENT care for adults and children, treatment of allergic rhinitis.",
                    List.of("2012 - 2018: Doctor of Medicine, Pham Ngoc Thach University of Medicine",
                            "2020 - 2022: Specialist Level I in Otolaryngology"),
                    List.of("2018 - 2023: Physician, ENT department, district hospital",
                            "2023 - present: Attending physician, Ear, Nose and Throat, eClinic"),
                    List.of("Allergic rhinitis",
                            "Sore throat, laryngitis",
                            "Earwax removal, foreign bodies in the ear, nose and throat",
                            "ENT conditions in children"))),
            Map.entry(9, new HoSoBacSiEn(
                    "25 years in cardiology, specialising in high blood pressure and coronary artery disease.",
                    "Senior Cardiology Advisor",
                    "Associate professor with 25 years in cardiology, specialising in high blood pressure and coronary"
                            + " artery disease.",
                    List.of("1994 - 2000: Doctor of Medicine, University of Medicine and Pharmacy at Ho Chi Minh City",
                            "2002 - 2005: Master of Cardiology",
                            "2007 - 2011: PhD in Medicine, specialising in Cardiology",
                            "2018: Awarded the title of Associate Professor"),
                    List.of("2000 - 2012: Attending physician, Cardiology department, national-level hospital",
                            "2012 - 2022: Head of the Cardiology department",
                            "2022 - present: Senior Cardiology Advisor, eClinic"),
                    List.of("High blood pressure",
                            "Coronary artery disease, angina",
                            "Heart failure, arrhythmia",
                            "Echocardiography, electrocardiogram"))),
            Map.entry(10, new HoSoBacSiEn(
                    "Antenatal care, obstetric ultrasound and treatment of common gynecological conditions.",
                    "Head of Obstetrics and Gynecology",
                    "Antenatal care, obstetric ultrasound and treatment of common gynecological conditions.",
                    List.of("2005 - 2011: Doctor of Medicine, University of Medicine and Pharmacy at Ho Chi Minh City",
                            "2013 - 2015: Master of Obstetrics and Gynecology,"
                                    + " University of Medicine and Pharmacy at Ho Chi Minh City"),
                    List.of("2011 - 2021: Attending physician, obstetrics and gynecology hospital",
                            "2021 - present: Head of Obstetrics and Gynecology, eClinic"),
                    List.of("Routine antenatal check-ups, pregnancy ultrasound",
                            "Gynecological infections",
                            "Menstrual disorders, uterine fibroids",
                            "Family planning advice"))),
            Map.entry(11, new HoSoBacSiEn(
                    "Refractive error examination, treatment of conjunctivitis and advice on cataract surgery.",
                    "Head of Ophthalmology",
                    "Refractive error examination, treatment of conjunctivitis and advice on cataract surgery.",
                    List.of("2009 - 2015: Doctor of Medicine, Pham Ngoc Thach University of Medicine",
                            "2017 - 2019: Specialist Level I in Ophthalmology"),
                    List.of("2015 - 2022: Attending physician, eye hospital",
                            "2022 - present: Head of Ophthalmology, eClinic"),
                    List.of("Nearsightedness, farsightedness, astigmatism",
                            "Conjunctivitis, dry eye",
                            "Cataracts, glaucoma",
                            "Eye examinations for children"))),
            Map.entry(12, new HoSoBacSiEn(
                    "General dental examination and treatment, pediatric dentistry.",
                    "Head of Dentistry",
                    "General dental examination and treatment, pediatric dentistry.",
                    List.of("2013 - 2019: Doctor of Dental Surgery,"
                            + " University of Medicine and Pharmacy at Ho Chi Minh City",
                            "2021: Certificate in Pediatric Dentistry"),
                    List.of("2019 - 2023: Attending dentist, dental clinic",
                            "2023 - present: Head of Dentistry, eClinic"),
                    List.of("Fillings, root canal treatment",
                            "Extractions, scaling",
                            "Pediatric dentistry",
                            "Orthodontic advice"))));

    /** Bản tiếng Anh của nơi cấp giấy phép hành nghề mẫu. */
    public static final String NOI_CAP_GIAY_PHEP = "Ho Chi Minh City Department of Health";

    /** Bản tiếng Anh của {@link HoSoBacSiMau#GIAI_THUONG}, cùng số thứ tự. */
    public static final Map<Integer, HoSoBacSiMau.GiaiThuongMau> GIAI_THUONG = Map.of(
            1, new HoSoBacSiMau.GiaiThuongMau(
                    List.of("2019: Certificate of merit from the Ho Chi Minh City Department of Health"),
                    List.of("2016: Clinical features of Helicobacter pylori peptic ulcer in adults")),
            3, new HoSoBacSiMau.GiaiThuongMau(
                    List.of("2018: Outstanding paediatric physician of the city"),
                    List.of("2015: Nebulisation in the treatment of bronchiolitis in children under 2",
                            "2020: Malnutrition in children under 5 in the inner city")),
            9, new HoSoBacSiMau.GiaiThuongMau(
                    List.of("2017: Title of Meritorious Physician", "2021: Research award of the Cardiology Association"),
                    List.of("2011: Uncontrolled hypertension in people over 60",
                            "2019: Outcomes of percutaneous coronary intervention in diabetic patients")));

    /** Bản tiếng Anh của dạng bào chế, đơn vị, mô tả trong {@link DuLieuMau#THUOC}. */
    public record ThuocEn(String dangBaoChe, String donVi, String moTa) {
    }

    /** Theo tên thuốc mẫu (tên không dịch). */
    public static final Map<String, ThuocEn> THUOC = Map.ofEntries(
            Map.entry("Paracetamol", new ThuocEn("Tablet", "tablet", "Pain relief, fever reducer")),
            Map.entry("Hapacol 250", new ThuocEn("Effervescent powder", "sachet", "Fever and pain relief for children")),
            Map.entry("Amoxicillin", new ThuocEn("Capsule", "capsule", "Penicillin antibiotic")),
            Map.entry("Cefuroxim", new ThuocEn("Film-coated tablet", "tablet", "Cephalosporin antibiotic")),
            Map.entry("Ibuprofen", new ThuocEn("Film-coated tablet", "tablet", "Anti-inflammatory, pain and fever relief")),
            Map.entry("Loratadin", new ThuocEn("Tablet", "tablet", "Allergy relief (antihistamine)")),
            Map.entry("Cetirizin", new ThuocEn("Film-coated tablet", "tablet", "Allergy relief (antihistamine)")),
            Map.entry("Omeprazol", new ThuocEn("Gastro-resistant capsule", "capsule", "Reduces stomach acid")),
            Map.entry("Domperidon", new ThuocEn("Tablet", "tablet", "Relieves nausea and vomiting")),
            Map.entry("Salbutamol", new ThuocEn("Metered-dose inhaler", "inhaler", "Asthma reliever, bronchodilator")),
            Map.entry("Amlodipin", new ThuocEn("Tablet", "tablet", "Lowers blood pressure")),
            Map.entry("Metformin", new ThuocEn("Film-coated tablet", "tablet", "Type 2 diabetes treatment")),
            Map.entry("Vitamin C", new ThuocEn("Tablet", "tablet", "Vitamin C supplement")),
            Map.entry("Oresol", new ThuocEn("Powder for oral solution", "sachet", "Rehydration and electrolytes")),
            Map.entry("Povidon iod", new ThuocEn("Topical solution", "bottle", "Antiseptic for skin and wounds")),
            Map.entry("Cloramphenicol", new ThuocEn("Eye drops", "bottle", "Antibiotic eye drops")));

}
