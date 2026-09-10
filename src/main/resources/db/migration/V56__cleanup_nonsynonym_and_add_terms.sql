-- V56__cleanup_nonsynonym_and_add_terms.sql
-- 비동의어(오염/오역/괄호) 정리 + 별개 개념 신규 term 추가. 신규는 부모 term 의 단어장 상속

-- 기존 term 뜻 수정
UPDATE terms SET meaning='콩팥요관방광단순촬영' WHERE id=260;
UPDATE terms SET meaning='유착태반' WHERE id=911;
UPDATE terms SET meaning='생식능력' WHERE id=856;
UPDATE terms SET meaning='골반' WHERE id=909;
UPDATE terms SET meaning='펩신' WHERE id=584;
UPDATE terms SET meaning='침' WHERE id=580;
UPDATE terms SET meaning='조기분만, 조산' WHERE id=921;
UPDATE terms SET meaning='교질삼투압' WHERE id=770;
UPDATE terms SET meaning='질확대경' WHERE id=842;
UPDATE terms SET meaning='산욕열' WHERE id=923;
UPDATE terms SET meaning='태아경, 태아경검사' WHERE id=861;
UPDATE terms SET meaning='유량계' WHERE id=756;
UPDATE terms SET meaning='난모세포' WHERE id=899;
UPDATE terms SET meaning='확장기, 심장확장' WHERE id=751;
UPDATE terms SET meaning='수축기, 심장수축' WHERE id=801;
UPDATE terms SET meaning='심장의, 관상동맥의' WHERE id=674;
UPDATE terms SET meaning='당뇨, 요당' WHERE id=1211;
UPDATE terms SET meaning='질경련' WHERE id=944;
UPDATE terms SET meaning='아스파테이트아미노전달효소' WHERE id=295;
UPDATE terms SET meaning='하반신마비' WHERE id=409;
UPDATE terms SET meaning='심폐소생술 불필요, 연명치료 포기' WHERE id=318;
UPDATE terms SET meaning='유뇨증' WHERE id=1153;
UPDATE terms SET meaning='복벽균열' WHERE id=1183;
UPDATE terms SET meaning='미숙아' WHERE id=1189;
UPDATE terms SET meaning='성장장애, 성장부진' WHERE id=1147;
UPDATE terms SET meaning='크룹, 급성폐쇄성후두염' WHERE id=1165;
UPDATE terms SET meaning='추정진단' WHERE id=334;
UPDATE terms SET meaning='간성뇌증' WHERE id=250;
UPDATE terms SET meaning='간세포암' WHERE id=332;
UPDATE terms SET meaning='색전증' WHERE id=679;
UPDATE terms SET meaning='자궁근종' WHERE id=862;
UPDATE terms SET meaning='심장잡음' WHERE id=693;
UPDATE terms SET meaning='복수천자' WHERE id=640;
UPDATE terms SET meaning='골반장기탈출증' WHERE id=908;
UPDATE terms SET meaning='불임술' WHERE id=928;
UPDATE terms SET meaning='인공루, 창냄' WHERE id=648;
UPDATE terms SET meaning='회음절개술' WHERE id=854;
UPDATE terms SET meaning='긁어냄술, 소파술' WHERE id=845;
UPDATE terms SET meaning='승모판절개술' WHERE id=767;
UPDATE terms SET meaning='정복술' WHERE id=408;
UPDATE terms SET meaning='작은판막, 판막' WHERE id=808;
UPDATE terms SET meaning='심장율동전환' WHERE id=670;
UPDATE terms SET meaning='가슴고름증, 농흉' WHERE id=521;

-- spelling 도 수정 (괄호 완성/설명 제거)
UPDATE terms SET spelling='rheumatic heart disease', meaning='류마티스심장병, 류마티스심장질환' WHERE id=789;
UPDATE terms SET spelling='pro re nata', meaning='필요시' WHERE id=362;

-- 신규 term
INSERT INTO terms (id, spelling, meaning) VALUES
(1229, 'kidney', '신장'),
(1230, 'ureter', '요관'),
(1231, 'bladder', '방광'),
(1232, 'placenta increta', '감입태반, 침윤태반'),
(1233, 'placenta percreta', '관통태반, 천공태반'),
(1234, 'fertilization', '수정'),
(1235, 'pelvimetry', '골반측정법'),
(1236, 'pepsinogen', '펩시노겐'),
(1237, 'salivation', '침분비'),
(1238, 'preterm labor', '조기진통'),
(1239, 'osmotic pressure', '삼투압'),
(1240, 'colposcopy', '질확대경검사'),
(1241, 'puerperal infection', '산욕감염, 산욕기감염'),
(1242, 'flowmetry', '유량측정법'),
(1243, 'ovum', '난자'),
(1244, 'vaginitis', '질염'),
(1245, 'nocturnal enuresis', '야뇨증'),
(1246, 'gastric rupture', '위벽파열'),
(1247, 'preterm infant', '조산아');

-- 신규 term 단어장 (해부용어 3개는 비뇨기과/신장내과 명시, 나머지는 부모 상속)
INSERT IGNORE INTO glossary_terms (glossary_id, term_id, created_at, updated_at) VALUES (5, 1229, NOW(), NOW());
INSERT IGNORE INTO glossary_terms (glossary_id, term_id, created_at, updated_at) VALUES (27, 1229, NOW(), NOW());
INSERT IGNORE INTO glossary_terms (glossary_id, term_id, created_at, updated_at) VALUES (27, 1230, NOW(), NOW());
INSERT IGNORE INTO glossary_terms (glossary_id, term_id, created_at, updated_at) VALUES (27, 1231, NOW(), NOW());
INSERT IGNORE INTO glossary_terms (glossary_id, term_id, created_at, updated_at) SELECT glossary_id, 1232, NOW(), NOW() FROM glossary_terms WHERE term_id=911;
INSERT IGNORE INTO glossary_terms (glossary_id, term_id, created_at, updated_at) SELECT glossary_id, 1233, NOW(), NOW() FROM glossary_terms WHERE term_id=911;
INSERT IGNORE INTO glossary_terms (glossary_id, term_id, created_at, updated_at) SELECT glossary_id, 1234, NOW(), NOW() FROM glossary_terms WHERE term_id=856;
INSERT IGNORE INTO glossary_terms (glossary_id, term_id, created_at, updated_at) SELECT glossary_id, 1235, NOW(), NOW() FROM glossary_terms WHERE term_id=909;
INSERT IGNORE INTO glossary_terms (glossary_id, term_id, created_at, updated_at) SELECT glossary_id, 1236, NOW(), NOW() FROM glossary_terms WHERE term_id=584;
INSERT IGNORE INTO glossary_terms (glossary_id, term_id, created_at, updated_at) SELECT glossary_id, 1237, NOW(), NOW() FROM glossary_terms WHERE term_id=580;
INSERT IGNORE INTO glossary_terms (glossary_id, term_id, created_at, updated_at) SELECT glossary_id, 1238, NOW(), NOW() FROM glossary_terms WHERE term_id=921;
INSERT IGNORE INTO glossary_terms (glossary_id, term_id, created_at, updated_at) SELECT glossary_id, 1239, NOW(), NOW() FROM glossary_terms WHERE term_id=770;
INSERT IGNORE INTO glossary_terms (glossary_id, term_id, created_at, updated_at) SELECT glossary_id, 1240, NOW(), NOW() FROM glossary_terms WHERE term_id=842;
INSERT IGNORE INTO glossary_terms (glossary_id, term_id, created_at, updated_at) SELECT glossary_id, 1241, NOW(), NOW() FROM glossary_terms WHERE term_id=923;
INSERT IGNORE INTO glossary_terms (glossary_id, term_id, created_at, updated_at) SELECT glossary_id, 1242, NOW(), NOW() FROM glossary_terms WHERE term_id=756;
INSERT IGNORE INTO glossary_terms (glossary_id, term_id, created_at, updated_at) SELECT glossary_id, 1243, NOW(), NOW() FROM glossary_terms WHERE term_id=899;
INSERT IGNORE INTO glossary_terms (glossary_id, term_id, created_at, updated_at) SELECT glossary_id, 1244, NOW(), NOW() FROM glossary_terms WHERE term_id=944;
INSERT IGNORE INTO glossary_terms (glossary_id, term_id, created_at, updated_at) SELECT glossary_id, 1245, NOW(), NOW() FROM glossary_terms WHERE term_id=1153;
INSERT IGNORE INTO glossary_terms (glossary_id, term_id, created_at, updated_at) SELECT glossary_id, 1246, NOW(), NOW() FROM glossary_terms WHERE term_id=1183;
INSERT IGNORE INTO glossary_terms (glossary_id, term_id, created_at, updated_at) SELECT glossary_id, 1247, NOW(), NOW() FROM glossary_terms WHERE term_id=1189;
