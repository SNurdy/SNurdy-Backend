-- V51__normalize_terms_casing_spacing_abbreviation.sql
-- Phase 4a: 대소문자(A) + 한글 띄어쓰기(D 가+나) + 약어 형식(E) 정규화
-- 순수 UPDATE 라 행 수 불변. 괄호/복수값 분리(B/C/약어 동의어)는 4b 에서 별도(iOS 동시배포 필요)

-- A. spelling 전체 소문자화
UPDATE terms SET spelling = LOWER(spelling) WHERE deleted_at IS NULL;

-- A. 고유명사/특수 표기 복원 (인명 토큰 및 A1c 대문자 유지)
UPDATE terms SET spelling = 'Alzheimer''s disease' WHERE id = 5;
UPDATE terms SET spelling = 'Glasgow coma scale' WHERE id = 46;
UPDATE terms SET spelling = 'Parkinson''s disease' WHERE id = 73;
UPDATE terms SET spelling = 'Crohn''s disease' WHERE id = 615;
UPDATE terms SET spelling = 'Achilles tendon (tendo calcaneus)' WHERE id = 720;
UPDATE terms SET spelling = 'Fallopian tube' WHERE id = 855;
UPDATE terms SET spelling = 'Blalock-Taussig shunt' WHERE id = 1031;
UPDATE terms SET spelling = 'Apgar score' WHERE id = 1146;
UPDATE terms SET spelling = 'tetralogy of Fallot' WHERE id = 1167;
UPDATE terms SET spelling = 'hemoglobin A1c' WHERE id = 1198;
UPDATE terms SET spelling = 'Addison''s disease' WHERE id = 1206;
UPDATE terms SET spelling = 'Cushing''s syndrome' WHERE id = 1207;

-- E. abbreviation 형식 정규화 (분리 불필요분)
UPDATE terms SET abbreviation = 'TIA' WHERE id = 105;
UPDATE terms SET abbreviation = 'I&D' WHERE id = 254;
UPDATE terms SET abbreviation = 'DT' WHERE id = 316;
UPDATE terms SET abbreviation = 'FiO2' WHERE id = 326;
UPDATE terms SET abbreviation = 'Imp' WHERE id = 334;
UPDATE terms SET abbreviation = 'NSAID' WHERE id = 349;
UPDATE terms SET abbreviation = 'PaO2' WHERE id = 353;
UPDATE terms SET abbreviation = 'R/O' WHERE id = 368;
UPDATE terms SET abbreviation = 'VP shunt' WHERE id = 385;
UPDATE terms SET abbreviation = 'Hct' WHERE id = 684;
UPDATE terms SET abbreviation = 'PaO2' WHERE id = 727;
UPDATE terms SET abbreviation = 'VP shunt' WHERE id = 976;
UPDATE terms SET abbreviation = 'EC' WHERE id = 1014;
UPDATE terms SET abbreviation = 'D&E' WHERE id = 1017;
UPDATE terms SET abbreviation = 'D&C' WHERE id = 1019;

-- D. 한글 띄어쓰기 붙임 통일 (중복쌍 + 급성/만성 병명), 21건
UPDATE terms SET meaning = '급성신부전' WHERE id = 283;
UPDATE terms SET meaning = '동맥혈가스분석' WHERE id = 292;
UPDATE terms SET meaning = '동맥혈가스분석' WHERE id = 656;
UPDATE terms SET meaning = '골이식' WHERE id = 1037;
UPDATE terms SET meaning = '만성폐쇄성폐질환' WHERE id = 507;
UPDATE terms SET meaning = '만성신부전' WHERE id = 307;
UPDATE terms SET meaning = '심부정맥혈전증' WHERE id = 234;
UPDATE terms SET meaning = '심부정맥혈전증' WHERE id = 315;
UPDATE terms SET meaning = '자궁외임신' WHERE id = 142;
UPDATE terms SET meaning = '경막외혈종' WHERE id = 146;
UPDATE terms SET meaning = '경막외혈종' WHERE id = 324;
UPDATE terms SET meaning = '경막외혈종' WHERE id = 1012;
UPDATE terms SET meaning = '좌심실비대' WHERE id = 341;
UPDATE terms SET meaning = '요추천자' WHERE id = 344;
UPDATE terms SET meaning = '경막하혈종' WHERE id = 372;
UPDATE terms SET meaning = '상기도감염' WHERE id = 500;
UPDATE terms SET meaning = '급성중이염' WHERE id = 111;
UPDATE terms SET meaning = '급성신장손상' WHERE id = 282;
UPDATE terms SET meaning = '급성호흡부전' WHERE id = 285;
UPDATE terms SET meaning = '만성개방각녹내장' WHERE id = 1026;
UPDATE terms SET meaning = '급성개방각녹내장' WHERE id = 1044;
