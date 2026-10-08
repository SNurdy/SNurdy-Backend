-- V65__dedupe_terms_second_pass.sql
-- V52 에서 의미 표기 차이로 누락된 중복 단어 2차 병합
-- canonical 은 그룹 최소 id 로 V52 규칙과 동일하며 참조 7개 테이블을 repoint 한 뒤 나머지를 소프트삭제
-- PK 충돌 시 UPDATE IGNORE 후 DELETE 로 canonical 쪽 데이터를 우선 유지
-- 영어 의미 약어는 중복 점검 엑셀 검토 결과를 반영하고 explanation 은 canonical 값을 유지

-- 그룹 1 대표 213 병합 295
UPDATE terms SET spelling='aspartate aminotransferase', meaning='아스파르트산아미노기전달효소, 아스파테이트아미노전달효소', abbreviation='AST', deleted_at=NULL WHERE id=213;
UPDATE IGNORE term_progresses SET term_id=213 WHERE term_id IN (295);
DELETE FROM term_progresses WHERE term_id IN (295);
UPDATE IGNORE bookmarks SET term_id=213 WHERE term_id IN (295);
DELETE FROM bookmarks WHERE term_id IN (295);
UPDATE IGNORE wrong_answer_terms SET term_id=213 WHERE term_id IN (295);
DELETE FROM wrong_answer_terms WHERE term_id IN (295);
UPDATE IGNORE custom_note_terms SET term_id=213 WHERE term_id IN (295);
DELETE FROM custom_note_terms WHERE term_id IN (295);
UPDATE IGNORE glossary_terms SET term_id=213 WHERE term_id IN (295);
DELETE FROM glossary_terms WHERE term_id IN (295);
UPDATE IGNORE term_morphemes SET term_id=213 WHERE term_id IN (295);
DELETE FROM term_morphemes WHERE term_id IN (295);
UPDATE IGNORE user_term_memos SET term_id=213 WHERE term_id IN (295);
DELETE FROM user_term_memos WHERE term_id IN (295);
UPDATE terms SET deleted_at=NOW() WHERE id IN (295) AND deleted_at IS NULL;

-- 그룹 2 대표 1077 병합 1149
UPDATE terms SET spelling='attention deficit hyperactivity disorder', meaning='주의력 결핍 과잉행동장애, 주의력 결핍 과다행동장애', abbreviation='ADHD', deleted_at=NULL WHERE id=1077;
UPDATE IGNORE term_progresses SET term_id=1077 WHERE term_id IN (1149);
DELETE FROM term_progresses WHERE term_id IN (1149);
UPDATE IGNORE bookmarks SET term_id=1077 WHERE term_id IN (1149);
DELETE FROM bookmarks WHERE term_id IN (1149);
UPDATE IGNORE wrong_answer_terms SET term_id=1077 WHERE term_id IN (1149);
DELETE FROM wrong_answer_terms WHERE term_id IN (1149);
UPDATE IGNORE custom_note_terms SET term_id=1077 WHERE term_id IN (1149);
DELETE FROM custom_note_terms WHERE term_id IN (1149);
UPDATE IGNORE glossary_terms SET term_id=1077 WHERE term_id IN (1149);
DELETE FROM glossary_terms WHERE term_id IN (1149);
UPDATE IGNORE term_morphemes SET term_id=1077 WHERE term_id IN (1149);
DELETE FROM term_morphemes WHERE term_id IN (1149);
UPDATE IGNORE user_term_memos SET term_id=1077 WHERE term_id IN (1149);
DELETE FROM user_term_memos WHERE term_id IN (1149);
UPDATE terms SET deleted_at=NOW() WHERE id IN (1149) AND deleted_at IS NULL;

-- 그룹 3 대표 16 병합 1040
UPDATE terms SET spelling='avascular necrosis', meaning='무혈관성 괴사, 무혈성 괴사', abbreviation='AVN', deleted_at=NULL WHERE id=16;
UPDATE IGNORE term_progresses SET term_id=16 WHERE term_id IN (1040);
DELETE FROM term_progresses WHERE term_id IN (1040);
UPDATE IGNORE bookmarks SET term_id=16 WHERE term_id IN (1040);
DELETE FROM bookmarks WHERE term_id IN (1040);
UPDATE IGNORE wrong_answer_terms SET term_id=16 WHERE term_id IN (1040);
DELETE FROM wrong_answer_terms WHERE term_id IN (1040);
UPDATE IGNORE custom_note_terms SET term_id=16 WHERE term_id IN (1040);
DELETE FROM custom_note_terms WHERE term_id IN (1040);
UPDATE IGNORE glossary_terms SET term_id=16 WHERE term_id IN (1040);
DELETE FROM glossary_terms WHERE term_id IN (1040);
UPDATE IGNORE term_morphemes SET term_id=16 WHERE term_id IN (1040);
DELETE FROM term_morphemes WHERE term_id IN (1040);
UPDATE IGNORE user_term_memos SET term_id=16 WHERE term_id IN (1040);
DELETE FROM user_term_memos WHERE term_id IN (1040);
UPDATE terms SET deleted_at=NOW() WHERE id IN (1040) AND deleted_at IS NULL;

-- 그룹 4 대표 310 병합 672
UPDATE terms SET spelling='computed tomography', meaning='컴퓨터 단층촬영, 전산화 단층촬영술', abbreviation='CT', deleted_at=NULL WHERE id=310;
UPDATE IGNORE term_progresses SET term_id=310 WHERE term_id IN (672);
DELETE FROM term_progresses WHERE term_id IN (672);
UPDATE IGNORE bookmarks SET term_id=310 WHERE term_id IN (672);
DELETE FROM bookmarks WHERE term_id IN (672);
UPDATE IGNORE wrong_answer_terms SET term_id=310 WHERE term_id IN (672);
DELETE FROM wrong_answer_terms WHERE term_id IN (672);
UPDATE IGNORE custom_note_terms SET term_id=310 WHERE term_id IN (672);
DELETE FROM custom_note_terms WHERE term_id IN (672);
UPDATE IGNORE glossary_terms SET term_id=310 WHERE term_id IN (672);
DELETE FROM glossary_terms WHERE term_id IN (672);
UPDATE IGNORE term_morphemes SET term_id=310 WHERE term_id IN (672);
DELETE FROM term_morphemes WHERE term_id IN (672);
UPDATE IGNORE user_term_memos SET term_id=310 WHERE term_id IN (672);
DELETE FROM user_term_memos WHERE term_id IN (672);
UPDATE terms SET deleted_at=NOW() WHERE id IN (672) AND deleted_at IS NULL;

-- 그룹 5 대표 540 병합 1023
UPDATE terms SET spelling='continuous positive airway pressure', meaning='지속 기도 양압, 지속적 양압 환기, 지속적 양압술', abbreviation='CPAP', deleted_at=NULL WHERE id=540;
UPDATE IGNORE term_progresses SET term_id=540 WHERE term_id IN (1023);
DELETE FROM term_progresses WHERE term_id IN (1023);
UPDATE IGNORE bookmarks SET term_id=540 WHERE term_id IN (1023);
DELETE FROM bookmarks WHERE term_id IN (1023);
UPDATE IGNORE wrong_answer_terms SET term_id=540 WHERE term_id IN (1023);
DELETE FROM wrong_answer_terms WHERE term_id IN (1023);
UPDATE IGNORE custom_note_terms SET term_id=540 WHERE term_id IN (1023);
DELETE FROM custom_note_terms WHERE term_id IN (1023);
UPDATE IGNORE glossary_terms SET term_id=540 WHERE term_id IN (1023);
DELETE FROM glossary_terms WHERE term_id IN (1023);
UPDATE IGNORE term_morphemes SET term_id=540 WHERE term_id IN (1023);
DELETE FROM term_morphemes WHERE term_id IN (1023);
UPDATE IGNORE user_term_memos SET term_id=540 WHERE term_id IN (1023);
DELETE FROM user_term_memos WHERE term_id IN (1023);
UPDATE terms SET deleted_at=NOW() WHERE id IN (1023) AND deleted_at IS NULL;

-- 그룹 6 대표 846 병합 1019
UPDATE terms SET spelling='dilation and curettage', meaning='자궁긁어냄술, 소파술, 자궁경관확장 및 소파술', abbreviation='D&C', deleted_at=NULL WHERE id=846;
UPDATE IGNORE term_progresses SET term_id=846 WHERE term_id IN (1019);
DELETE FROM term_progresses WHERE term_id IN (1019);
UPDATE IGNORE bookmarks SET term_id=846 WHERE term_id IN (1019);
DELETE FROM bookmarks WHERE term_id IN (1019);
UPDATE IGNORE wrong_answer_terms SET term_id=846 WHERE term_id IN (1019);
DELETE FROM wrong_answer_terms WHERE term_id IN (1019);
UPDATE IGNORE custom_note_terms SET term_id=846 WHERE term_id IN (1019);
DELETE FROM custom_note_terms WHERE term_id IN (1019);
UPDATE IGNORE glossary_terms SET term_id=846 WHERE term_id IN (1019);
DELETE FROM glossary_terms WHERE term_id IN (1019);
UPDATE IGNORE term_morphemes SET term_id=846 WHERE term_id IN (1019);
DELETE FROM term_morphemes WHERE term_id IN (1019);
UPDATE IGNORE user_term_memos SET term_id=846 WHERE term_id IN (1019);
DELETE FROM user_term_memos WHERE term_id IN (1019);
UPDATE terms SET deleted_at=NOW() WHERE id IN (1019) AND deleted_at IS NULL;

-- 그룹 7 대표 232 병합 318
UPDATE terms SET spelling='do not resuscitate', meaning='소생술 포기, 심폐소생술 거부, 연명치료 포기', abbreviation='DNR', deleted_at=NULL WHERE id=232;
UPDATE IGNORE term_progresses SET term_id=232 WHERE term_id IN (318);
DELETE FROM term_progresses WHERE term_id IN (318);
UPDATE IGNORE bookmarks SET term_id=232 WHERE term_id IN (318);
DELETE FROM bookmarks WHERE term_id IN (318);
UPDATE IGNORE wrong_answer_terms SET term_id=232 WHERE term_id IN (318);
DELETE FROM wrong_answer_terms WHERE term_id IN (318);
UPDATE IGNORE custom_note_terms SET term_id=232 WHERE term_id IN (318);
DELETE FROM custom_note_terms WHERE term_id IN (318);
UPDATE IGNORE glossary_terms SET term_id=232 WHERE term_id IN (318);
DELETE FROM glossary_terms WHERE term_id IN (318);
UPDATE IGNORE term_morphemes SET term_id=232 WHERE term_id IN (318);
DELETE FROM term_morphemes WHERE term_id IN (318);
UPDATE IGNORE user_term_memos SET term_id=232 WHERE term_id IN (318);
DELETE FROM user_term_memos WHERE term_id IN (318);
UPDATE terms SET deleted_at=NOW() WHERE id IN (318) AND deleted_at IS NULL;

-- 그룹 8 대표 242 병합 1193
UPDATE terms SET spelling='fine needle aspiration', meaning='미세침흡인검사, 미세침 흡인 세포검사, 가는바늘흡인세포검사', abbreviation='FNA', deleted_at=NULL WHERE id=242;
UPDATE IGNORE term_progresses SET term_id=242 WHERE term_id IN (1193);
DELETE FROM term_progresses WHERE term_id IN (1193);
UPDATE IGNORE bookmarks SET term_id=242 WHERE term_id IN (1193);
DELETE FROM bookmarks WHERE term_id IN (1193);
UPDATE IGNORE wrong_answer_terms SET term_id=242 WHERE term_id IN (1193);
DELETE FROM wrong_answer_terms WHERE term_id IN (1193);
UPDATE IGNORE custom_note_terms SET term_id=242 WHERE term_id IN (1193);
DELETE FROM custom_note_terms WHERE term_id IN (1193);
UPDATE IGNORE glossary_terms SET term_id=242 WHERE term_id IN (1193);
DELETE FROM glossary_terms WHERE term_id IN (1193);
UPDATE IGNORE term_morphemes SET term_id=242 WHERE term_id IN (1193);
DELETE FROM term_morphemes WHERE term_id IN (1193);
UPDATE IGNORE user_term_memos SET term_id=242 WHERE term_id IN (1193);
DELETE FROM user_term_memos WHERE term_id IN (1193);
UPDATE terms SET deleted_at=NOW() WHERE id IN (1193) AND deleted_at IS NULL;

-- 그룹 9 대표 46 병합 245
UPDATE terms SET spelling='glasgow coma scale', meaning='글라스고 혼수척도, 글라스고우 혼수척도', abbreviation='GCS', deleted_at=NULL WHERE id=46;
UPDATE IGNORE term_progresses SET term_id=46 WHERE term_id IN (245);
DELETE FROM term_progresses WHERE term_id IN (245);
UPDATE IGNORE bookmarks SET term_id=46 WHERE term_id IN (245);
DELETE FROM bookmarks WHERE term_id IN (245);
UPDATE IGNORE wrong_answer_terms SET term_id=46 WHERE term_id IN (245);
DELETE FROM wrong_answer_terms WHERE term_id IN (245);
UPDATE IGNORE custom_note_terms SET term_id=46 WHERE term_id IN (245);
DELETE FROM custom_note_terms WHERE term_id IN (245);
UPDATE IGNORE glossary_terms SET term_id=46 WHERE term_id IN (245);
DELETE FROM glossary_terms WHERE term_id IN (245);
UPDATE IGNORE term_morphemes SET term_id=46 WHERE term_id IN (245);
DELETE FROM term_morphemes WHERE term_id IN (245);
UPDATE IGNORE user_term_memos SET term_id=46 WHERE term_id IN (245);
DELETE FROM user_term_memos WHERE term_id IN (245);
UPDATE terms SET deleted_at=NOW() WHERE id IN (245) AND deleted_at IS NULL;

-- 그룹 10 대표 248 병합 332
UPDATE terms SET spelling='hepatocellular carcinoma', meaning='간세포암, 간세포암종', abbreviation='HCC', deleted_at=NULL WHERE id=248;
UPDATE IGNORE term_progresses SET term_id=248 WHERE term_id IN (332);
DELETE FROM term_progresses WHERE term_id IN (332);
UPDATE IGNORE bookmarks SET term_id=248 WHERE term_id IN (332);
DELETE FROM bookmarks WHERE term_id IN (332);
UPDATE IGNORE wrong_answer_terms SET term_id=248 WHERE term_id IN (332);
DELETE FROM wrong_answer_terms WHERE term_id IN (332);
UPDATE IGNORE custom_note_terms SET term_id=248 WHERE term_id IN (332);
DELETE FROM custom_note_terms WHERE term_id IN (332);
UPDATE IGNORE glossary_terms SET term_id=248 WHERE term_id IN (332);
DELETE FROM glossary_terms WHERE term_id IN (332);
UPDATE IGNORE term_morphemes SET term_id=248 WHERE term_id IN (332);
DELETE FROM term_morphemes WHERE term_id IN (332);
UPDATE IGNORE user_term_memos SET term_id=248 WHERE term_id IN (332);
DELETE FROM user_term_memos WHERE term_id IN (332);
UPDATE terms SET deleted_at=NOW() WHERE id IN (332) AND deleted_at IS NULL;

-- 그룹 11 대표 257 병합 334
UPDATE terms SET spelling='impression', meaning='예진, 추정진단', abbreviation='IMP, Imp', deleted_at=NULL WHERE id=257;
UPDATE IGNORE term_progresses SET term_id=257 WHERE term_id IN (334);
DELETE FROM term_progresses WHERE term_id IN (334);
UPDATE IGNORE bookmarks SET term_id=257 WHERE term_id IN (334);
DELETE FROM bookmarks WHERE term_id IN (334);
UPDATE IGNORE wrong_answer_terms SET term_id=257 WHERE term_id IN (334);
DELETE FROM wrong_answer_terms WHERE term_id IN (334);
UPDATE IGNORE custom_note_terms SET term_id=257 WHERE term_id IN (334);
DELETE FROM custom_note_terms WHERE term_id IN (334);
UPDATE IGNORE glossary_terms SET term_id=257 WHERE term_id IN (334);
DELETE FROM glossary_terms WHERE term_id IN (334);
UPDATE IGNORE term_morphemes SET term_id=257 WHERE term_id IN (334);
DELETE FROM term_morphemes WHERE term_id IN (334);
UPDATE IGNORE user_term_memos SET term_id=257 WHERE term_id IN (334);
DELETE FROM user_term_memos WHERE term_id IN (334);
UPDATE terms SET deleted_at=NOW() WHERE id IN (334) AND deleted_at IS NULL;

-- 그룹 12 대표 340 병합 763
UPDATE terms SET spelling='ischemic heart disease', meaning='허혈성 심질환, 허혈심장질환', abbreviation='IHD', deleted_at=NULL WHERE id=340;
UPDATE IGNORE term_progresses SET term_id=340 WHERE term_id IN (763);
DELETE FROM term_progresses WHERE term_id IN (763);
UPDATE IGNORE bookmarks SET term_id=340 WHERE term_id IN (763);
DELETE FROM bookmarks WHERE term_id IN (763);
UPDATE IGNORE wrong_answer_terms SET term_id=340 WHERE term_id IN (763);
DELETE FROM wrong_answer_terms WHERE term_id IN (763);
UPDATE IGNORE custom_note_terms SET term_id=340 WHERE term_id IN (763);
DELETE FROM custom_note_terms WHERE term_id IN (763);
UPDATE IGNORE glossary_terms SET term_id=340 WHERE term_id IN (763);
DELETE FROM glossary_terms WHERE term_id IN (763);
UPDATE IGNORE term_morphemes SET term_id=340 WHERE term_id IN (763);
DELETE FROM term_morphemes WHERE term_id IN (763);
UPDATE IGNORE user_term_memos SET term_id=340 WHERE term_id IN (763);
DELETE FROM user_term_memos WHERE term_id IN (763);
UPDATE terms SET deleted_at=NOW() WHERE id IN (763) AND deleted_at IS NULL;

-- 그룹 13 대표 267 병합 764
UPDATE terms SET spelling='left ventricular hypertrophy', meaning='좌심실비대, 좌심실비후', abbreviation='LVH', deleted_at=NULL WHERE id=267;
UPDATE IGNORE term_progresses SET term_id=267 WHERE term_id IN (764);
DELETE FROM term_progresses WHERE term_id IN (764);
UPDATE IGNORE bookmarks SET term_id=267 WHERE term_id IN (764);
DELETE FROM bookmarks WHERE term_id IN (764);
UPDATE IGNORE wrong_answer_terms SET term_id=267 WHERE term_id IN (764);
DELETE FROM wrong_answer_terms WHERE term_id IN (764);
UPDATE IGNORE custom_note_terms SET term_id=267 WHERE term_id IN (764);
DELETE FROM custom_note_terms WHERE term_id IN (764);
UPDATE IGNORE glossary_terms SET term_id=267 WHERE term_id IN (764);
DELETE FROM glossary_terms WHERE term_id IN (764);
UPDATE IGNORE term_morphemes SET term_id=267 WHERE term_id IN (764);
DELETE FROM term_morphemes WHERE term_id IN (764);
UPDATE IGNORE user_term_memos SET term_id=267 WHERE term_id IN (764);
DELETE FROM user_term_memos WHERE term_id IN (764);
UPDATE terms SET deleted_at=NOW() WHERE id IN (764) AND deleted_at IS NULL;

-- 그룹 14 대표 342 병합 619
UPDATE terms SET spelling='liver cirrhosis', meaning='간경변, 간경화, 간경변증, 간경화증', abbreviation='LC', deleted_at=NULL WHERE id=342;
UPDATE IGNORE term_progresses SET term_id=342 WHERE term_id IN (619);
DELETE FROM term_progresses WHERE term_id IN (619);
UPDATE IGNORE bookmarks SET term_id=342 WHERE term_id IN (619);
DELETE FROM bookmarks WHERE term_id IN (619);
UPDATE IGNORE wrong_answer_terms SET term_id=342 WHERE term_id IN (619);
DELETE FROM wrong_answer_terms WHERE term_id IN (619);
UPDATE IGNORE custom_note_terms SET term_id=342 WHERE term_id IN (619);
DELETE FROM custom_note_terms WHERE term_id IN (619);
UPDATE IGNORE glossary_terms SET term_id=342 WHERE term_id IN (619);
DELETE FROM glossary_terms WHERE term_id IN (619);
UPDATE IGNORE term_morphemes SET term_id=342 WHERE term_id IN (619);
DELETE FROM term_morphemes WHERE term_id IN (619);
UPDATE IGNORE user_term_memos SET term_id=342 WHERE term_id IN (619);
DELETE FROM user_term_memos WHERE term_id IN (619);
UPDATE terms SET deleted_at=NOW() WHERE id IN (619) AND deleted_at IS NULL;

-- 그룹 15 대표 264 병합 343
UPDATE terms SET spelling='loss of consciousness', meaning='의식 상실, 의식 소실', abbreviation='LOC', deleted_at=NULL WHERE id=264;
UPDATE IGNORE term_progresses SET term_id=264 WHERE term_id IN (343);
DELETE FROM term_progresses WHERE term_id IN (343);
UPDATE IGNORE bookmarks SET term_id=264 WHERE term_id IN (343);
DELETE FROM bookmarks WHERE term_id IN (343);
UPDATE IGNORE wrong_answer_terms SET term_id=264 WHERE term_id IN (343);
DELETE FROM wrong_answer_terms WHERE term_id IN (343);
UPDATE IGNORE custom_note_terms SET term_id=264 WHERE term_id IN (343);
DELETE FROM custom_note_terms WHERE term_id IN (343);
UPDATE IGNORE glossary_terms SET term_id=264 WHERE term_id IN (343);
DELETE FROM glossary_terms WHERE term_id IN (343);
UPDATE IGNORE term_morphemes SET term_id=264 WHERE term_id IN (343);
DELETE FROM term_morphemes WHERE term_id IN (343);
UPDATE IGNORE user_term_memos SET term_id=264 WHERE term_id IN (343);
DELETE FROM user_term_memos WHERE term_id IN (343);
UPDATE terms SET deleted_at=NOW() WHERE id IN (343) AND deleted_at IS NULL;

-- 그룹 16 대표 271 병합 345
UPDATE terms SET spelling='magnetic resonance imaging', meaning='자기공명영상, 자기공명영상법', abbreviation='MRI', deleted_at=NULL WHERE id=271;
UPDATE IGNORE term_progresses SET term_id=271 WHERE term_id IN (345);
DELETE FROM term_progresses WHERE term_id IN (345);
UPDATE IGNORE bookmarks SET term_id=271 WHERE term_id IN (345);
DELETE FROM bookmarks WHERE term_id IN (345);
UPDATE IGNORE wrong_answer_terms SET term_id=271 WHERE term_id IN (345);
DELETE FROM wrong_answer_terms WHERE term_id IN (345);
UPDATE IGNORE custom_note_terms SET term_id=271 WHERE term_id IN (345);
DELETE FROM custom_note_terms WHERE term_id IN (345);
UPDATE IGNORE glossary_terms SET term_id=271 WHERE term_id IN (345);
DELETE FROM glossary_terms WHERE term_id IN (345);
UPDATE IGNORE term_morphemes SET term_id=271 WHERE term_id IN (345);
DELETE FROM term_morphemes WHERE term_id IN (345);
UPDATE IGNORE user_term_memos SET term_id=271 WHERE term_id IN (345);
DELETE FROM user_term_memos WHERE term_id IN (345);
UPDATE terms SET deleted_at=NOW() WHERE id IN (345) AND deleted_at IS NULL;

-- 그룹 17 대표 270 병합 694
UPDATE terms SET spelling='myocardial infarction', meaning='심근경색증, 심근경색', abbreviation='MI', deleted_at=NULL WHERE id=270;
UPDATE IGNORE term_progresses SET term_id=270 WHERE term_id IN (694);
DELETE FROM term_progresses WHERE term_id IN (694);
UPDATE IGNORE bookmarks SET term_id=270 WHERE term_id IN (694);
DELETE FROM bookmarks WHERE term_id IN (694);
UPDATE IGNORE wrong_answer_terms SET term_id=270 WHERE term_id IN (694);
DELETE FROM wrong_answer_terms WHERE term_id IN (694);
UPDATE IGNORE custom_note_terms SET term_id=270 WHERE term_id IN (694);
DELETE FROM custom_note_terms WHERE term_id IN (694);
UPDATE IGNORE glossary_terms SET term_id=270 WHERE term_id IN (694);
DELETE FROM glossary_terms WHERE term_id IN (694);
UPDATE IGNORE term_morphemes SET term_id=270 WHERE term_id IN (694);
DELETE FROM term_morphemes WHERE term_id IN (694);
UPDATE IGNORE user_term_memos SET term_id=270 WHERE term_id IN (694);
DELETE FROM user_term_memos WHERE term_id IN (694);
UPDATE terms SET deleted_at=NOW() WHERE id IN (694) AND deleted_at IS NULL;

-- 그룹 18 대표 278 병합 355
UPDATE terms SET spelling='percutaneous transhepatic biliary drainage', meaning='경피경간 담도 배액술, 피부 간경 유쓸개관 배액술', abbreviation='PTBD', deleted_at=NULL WHERE id=278;
UPDATE IGNORE term_progresses SET term_id=278 WHERE term_id IN (355);
DELETE FROM term_progresses WHERE term_id IN (355);
UPDATE IGNORE bookmarks SET term_id=278 WHERE term_id IN (355);
DELETE FROM bookmarks WHERE term_id IN (355);
UPDATE IGNORE wrong_answer_terms SET term_id=278 WHERE term_id IN (355);
DELETE FROM wrong_answer_terms WHERE term_id IN (355);
UPDATE IGNORE custom_note_terms SET term_id=278 WHERE term_id IN (355);
DELETE FROM custom_note_terms WHERE term_id IN (355);
UPDATE IGNORE glossary_terms SET term_id=278 WHERE term_id IN (355);
DELETE FROM glossary_terms WHERE term_id IN (355);
UPDATE IGNORE term_morphemes SET term_id=278 WHERE term_id IN (355);
DELETE FROM term_morphemes WHERE term_id IN (355);
UPDATE IGNORE user_term_memos SET term_id=278 WHERE term_id IN (355);
DELETE FROM user_term_memos WHERE term_id IN (355);
UPDATE terms SET deleted_at=NOW() WHERE id IN (355) AND deleted_at IS NULL;

-- 그룹 19 대표 279 병합 361 781
UPDATE terms SET spelling='premature ventricular contraction', meaning='심실조기수축, 조기심실수축', abbreviation='PVC, VPC', deleted_at=NULL WHERE id=279;
UPDATE IGNORE term_progresses SET term_id=279 WHERE term_id IN (361,781);
DELETE FROM term_progresses WHERE term_id IN (361,781);
UPDATE IGNORE bookmarks SET term_id=279 WHERE term_id IN (361,781);
DELETE FROM bookmarks WHERE term_id IN (361,781);
UPDATE IGNORE wrong_answer_terms SET term_id=279 WHERE term_id IN (361,781);
DELETE FROM wrong_answer_terms WHERE term_id IN (361,781);
UPDATE IGNORE custom_note_terms SET term_id=279 WHERE term_id IN (361,781);
DELETE FROM custom_note_terms WHERE term_id IN (361,781);
UPDATE IGNORE glossary_terms SET term_id=279 WHERE term_id IN (361,781);
DELETE FROM glossary_terms WHERE term_id IN (361,781);
UPDATE IGNORE term_morphemes SET term_id=279 WHERE term_id IN (361,781);
DELETE FROM term_morphemes WHERE term_id IN (361,781);
UPDATE IGNORE user_term_memos SET term_id=279 WHERE term_id IN (361,781);
DELETE FROM user_term_memos WHERE term_id IN (361,781);
UPDATE terms SET deleted_at=NOW() WHERE id IN (361,781) AND deleted_at IS NULL;

-- 그룹 20 대표 370 병합 481
UPDATE terms SET spelling='shortness of breath', meaning='호흡곤란, 숨참', abbreviation='SOB', deleted_at=NULL WHERE id=370;
UPDATE IGNORE term_progresses SET term_id=370 WHERE term_id IN (481);
DELETE FROM term_progresses WHERE term_id IN (481);
UPDATE IGNORE bookmarks SET term_id=370 WHERE term_id IN (481);
DELETE FROM bookmarks WHERE term_id IN (481);
UPDATE IGNORE wrong_answer_terms SET term_id=370 WHERE term_id IN (481);
DELETE FROM wrong_answer_terms WHERE term_id IN (481);
UPDATE IGNORE custom_note_terms SET term_id=370 WHERE term_id IN (481);
DELETE FROM custom_note_terms WHERE term_id IN (481);
UPDATE IGNORE glossary_terms SET term_id=370 WHERE term_id IN (481);
DELETE FROM glossary_terms WHERE term_id IN (481);
UPDATE IGNORE term_morphemes SET term_id=370 WHERE term_id IN (481);
DELETE FROM term_morphemes WHERE term_id IN (481);
UPDATE IGNORE user_term_memos SET term_id=370 WHERE term_id IN (481);
DELETE FROM user_term_memos WHERE term_id IN (481);
UPDATE terms SET deleted_at=NOW() WHERE id IN (481) AND deleted_at IS NULL;

-- 그룹 21 대표 221 병합 576
UPDATE terms SET spelling='common bile duct', meaning='총담관, 온쓸개관', abbreviation='CBD', deleted_at=NULL WHERE id=221;
UPDATE IGNORE term_progresses SET term_id=221 WHERE term_id IN (576);
DELETE FROM term_progresses WHERE term_id IN (576);
UPDATE IGNORE bookmarks SET term_id=221 WHERE term_id IN (576);
DELETE FROM bookmarks WHERE term_id IN (576);
UPDATE IGNORE wrong_answer_terms SET term_id=221 WHERE term_id IN (576);
DELETE FROM wrong_answer_terms WHERE term_id IN (576);
UPDATE IGNORE custom_note_terms SET term_id=221 WHERE term_id IN (576);
DELETE FROM custom_note_terms WHERE term_id IN (576);
UPDATE IGNORE glossary_terms SET term_id=221 WHERE term_id IN (576);
DELETE FROM glossary_terms WHERE term_id IN (576);
UPDATE IGNORE term_morphemes SET term_id=221 WHERE term_id IN (576);
DELETE FROM term_morphemes WHERE term_id IN (576);
UPDATE IGNORE user_term_memos SET term_id=221 WHERE term_id IN (576);
DELETE FROM user_term_memos WHERE term_id IN (576);
UPDATE terms SET deleted_at=NOW() WHERE id IN (576) AND deleted_at IS NULL;

-- 그룹 22 대표 385 병합 976
UPDATE terms SET spelling='ventriculoperitoneal shunt', meaning='뇌실 복강 단락술, 뇌실 복막 단락술, 뇌실복강 간 단락술', abbreviation='VP shunt', deleted_at=NULL WHERE id=385;
UPDATE IGNORE term_progresses SET term_id=385 WHERE term_id IN (976);
DELETE FROM term_progresses WHERE term_id IN (976);
UPDATE IGNORE bookmarks SET term_id=385 WHERE term_id IN (976);
DELETE FROM bookmarks WHERE term_id IN (976);
UPDATE IGNORE wrong_answer_terms SET term_id=385 WHERE term_id IN (976);
DELETE FROM wrong_answer_terms WHERE term_id IN (976);
UPDATE IGNORE custom_note_terms SET term_id=385 WHERE term_id IN (976);
DELETE FROM custom_note_terms WHERE term_id IN (976);
UPDATE IGNORE glossary_terms SET term_id=385 WHERE term_id IN (976);
DELETE FROM glossary_terms WHERE term_id IN (976);
UPDATE IGNORE term_morphemes SET term_id=385 WHERE term_id IN (976);
DELETE FROM term_morphemes WHERE term_id IN (976);
UPDATE IGNORE user_term_memos SET term_id=385 WHERE term_id IN (976);
DELETE FROM user_term_memos WHERE term_id IN (976);
UPDATE terms SET deleted_at=NOW() WHERE id IN (976) AND deleted_at IS NULL;

-- 그룹 23 대표 126 병합 670
UPDATE terms SET spelling='cardioversion', meaning='심장율동전환', abbreviation=NULL, deleted_at=NULL WHERE id=126;
UPDATE IGNORE term_progresses SET term_id=126 WHERE term_id IN (670);
DELETE FROM term_progresses WHERE term_id IN (670);
UPDATE IGNORE bookmarks SET term_id=126 WHERE term_id IN (670);
DELETE FROM bookmarks WHERE term_id IN (670);
UPDATE IGNORE wrong_answer_terms SET term_id=126 WHERE term_id IN (670);
DELETE FROM wrong_answer_terms WHERE term_id IN (670);
UPDATE IGNORE custom_note_terms SET term_id=126 WHERE term_id IN (670);
DELETE FROM custom_note_terms WHERE term_id IN (670);
UPDATE IGNORE glossary_terms SET term_id=126 WHERE term_id IN (670);
DELETE FROM glossary_terms WHERE term_id IN (670);
UPDATE IGNORE term_morphemes SET term_id=126 WHERE term_id IN (670);
DELETE FROM term_morphemes WHERE term_id IN (670);
UPDATE IGNORE user_term_memos SET term_id=126 WHERE term_id IN (670);
DELETE FROM user_term_memos WHERE term_id IN (670);
UPDATE terms SET deleted_at=NOW() WHERE id IN (670) AND deleted_at IS NULL;

-- 그룹 24 대표 134 병합 1063
UPDATE terms SET spelling='confusion', meaning='혼란, 혼동, 의식 혼동', abbreviation=NULL, deleted_at=NULL WHERE id=134;
UPDATE IGNORE term_progresses SET term_id=134 WHERE term_id IN (1063);
DELETE FROM term_progresses WHERE term_id IN (1063);
UPDATE IGNORE bookmarks SET term_id=134 WHERE term_id IN (1063);
DELETE FROM bookmarks WHERE term_id IN (1063);
UPDATE IGNORE wrong_answer_terms SET term_id=134 WHERE term_id IN (1063);
DELETE FROM wrong_answer_terms WHERE term_id IN (1063);
UPDATE IGNORE custom_note_terms SET term_id=134 WHERE term_id IN (1063);
DELETE FROM custom_note_terms WHERE term_id IN (1063);
UPDATE IGNORE glossary_terms SET term_id=134 WHERE term_id IN (1063);
DELETE FROM glossary_terms WHERE term_id IN (1063);
UPDATE IGNORE term_morphemes SET term_id=134 WHERE term_id IN (1063);
DELETE FROM term_morphemes WHERE term_id IN (1063);
UPDATE IGNORE user_term_memos SET term_id=134 WHERE term_id IN (1063);
DELETE FROM user_term_memos WHERE term_id IN (1063);
UPDATE terms SET deleted_at=NOW() WHERE id IN (1063) AND deleted_at IS NULL;

-- 그룹 25 대표 34 병합 140
UPDATE terms SET spelling='drowsy', meaning='기면, 졸림, 나른함', abbreviation=NULL, deleted_at=NULL WHERE id=34;
UPDATE IGNORE term_progresses SET term_id=34 WHERE term_id IN (140);
DELETE FROM term_progresses WHERE term_id IN (140);
UPDATE IGNORE bookmarks SET term_id=34 WHERE term_id IN (140);
DELETE FROM bookmarks WHERE term_id IN (140);
UPDATE IGNORE wrong_answer_terms SET term_id=34 WHERE term_id IN (140);
DELETE FROM wrong_answer_terms WHERE term_id IN (140);
UPDATE IGNORE custom_note_terms SET term_id=34 WHERE term_id IN (140);
DELETE FROM custom_note_terms WHERE term_id IN (140);
UPDATE IGNORE glossary_terms SET term_id=34 WHERE term_id IN (140);
DELETE FROM glossary_terms WHERE term_id IN (140);
UPDATE IGNORE term_morphemes SET term_id=34 WHERE term_id IN (140);
DELETE FROM term_morphemes WHERE term_id IN (140);
UPDATE IGNORE user_term_memos SET term_id=34 WHERE term_id IN (140);
DELETE FROM user_term_memos WHERE term_id IN (140);
UPDATE terms SET deleted_at=NOW() WHERE id IN (140) AND deleted_at IS NULL;

-- 그룹 26 대표 236 병합 320 678
UPDATE terms SET spelling='electrocardiogram', meaning='심전도', abbreviation='ECG, EKG', deleted_at=NULL WHERE id=236;
UPDATE IGNORE term_progresses SET term_id=236 WHERE term_id IN (320,678);
DELETE FROM term_progresses WHERE term_id IN (320,678);
UPDATE IGNORE bookmarks SET term_id=236 WHERE term_id IN (320,678);
DELETE FROM bookmarks WHERE term_id IN (320,678);
UPDATE IGNORE wrong_answer_terms SET term_id=236 WHERE term_id IN (320,678);
DELETE FROM wrong_answer_terms WHERE term_id IN (320,678);
UPDATE IGNORE custom_note_terms SET term_id=236 WHERE term_id IN (320,678);
DELETE FROM custom_note_terms WHERE term_id IN (320,678);
UPDATE IGNORE glossary_terms SET term_id=236 WHERE term_id IN (320,678);
DELETE FROM glossary_terms WHERE term_id IN (320,678);
UPDATE IGNORE term_morphemes SET term_id=236 WHERE term_id IN (320,678);
DELETE FROM term_morphemes WHERE term_id IN (320,678);
UPDATE IGNORE user_term_memos SET term_id=236 WHERE term_id IN (320,678);
DELETE FROM user_term_memos WHERE term_id IN (320,678);
UPDATE terms SET deleted_at=NOW() WHERE id IN (320,678) AND deleted_at IS NULL;

-- 그룹 27 대표 40 병합 151
UPDATE terms SET spelling='fall down', meaning='낙상', abbreviation=NULL, deleted_at=NULL WHERE id=40;
UPDATE IGNORE term_progresses SET term_id=40 WHERE term_id IN (151);
DELETE FROM term_progresses WHERE term_id IN (151);
UPDATE IGNORE bookmarks SET term_id=40 WHERE term_id IN (151);
DELETE FROM bookmarks WHERE term_id IN (151);
UPDATE IGNORE wrong_answer_terms SET term_id=40 WHERE term_id IN (151);
DELETE FROM wrong_answer_terms WHERE term_id IN (151);
UPDATE IGNORE custom_note_terms SET term_id=40 WHERE term_id IN (151);
DELETE FROM custom_note_terms WHERE term_id IN (151);
UPDATE IGNORE glossary_terms SET term_id=40 WHERE term_id IN (151);
DELETE FROM glossary_terms WHERE term_id IN (151);
UPDATE IGNORE term_morphemes SET term_id=40 WHERE term_id IN (151);
DELETE FROM term_morphemes WHERE term_id IN (151);
UPDATE IGNORE user_term_memos SET term_id=40 WHERE term_id IN (151);
DELETE FROM user_term_memos WHERE term_id IN (151);
UPDATE terms SET deleted_at=NOW() WHERE id IN (151) AND deleted_at IS NULL;

-- 그룹 28 대표 52 병합 329
UPDATE terms SET spelling='hemiplegia', meaning='편측마비, 반마비, 편마비', abbreviation=NULL, deleted_at=NULL WHERE id=52;
UPDATE IGNORE term_progresses SET term_id=52 WHERE term_id IN (329);
DELETE FROM term_progresses WHERE term_id IN (329);
UPDATE IGNORE bookmarks SET term_id=52 WHERE term_id IN (329);
DELETE FROM bookmarks WHERE term_id IN (329);
UPDATE IGNORE wrong_answer_terms SET term_id=52 WHERE term_id IN (329);
DELETE FROM wrong_answer_terms WHERE term_id IN (329);
UPDATE IGNORE custom_note_terms SET term_id=52 WHERE term_id IN (329);
DELETE FROM custom_note_terms WHERE term_id IN (329);
UPDATE IGNORE glossary_terms SET term_id=52 WHERE term_id IN (329);
DELETE FROM glossary_terms WHERE term_id IN (329);
UPDATE IGNORE term_morphemes SET term_id=52 WHERE term_id IN (329);
DELETE FROM term_morphemes WHERE term_id IN (329);
UPDATE IGNORE user_term_memos SET term_id=52 WHERE term_id IN (329);
DELETE FROM user_term_memos WHERE term_id IN (329);
UPDATE terms SET deleted_at=NOW() WHERE id IN (329) AND deleted_at IS NULL;

-- 그룹 29 대표 66 병합 418
UPDATE terms SET spelling='myasthenia gravis', meaning='중증근무력증', abbreviation='MG', deleted_at=NULL WHERE id=66;
UPDATE IGNORE term_progresses SET term_id=66 WHERE term_id IN (418);
DELETE FROM term_progresses WHERE term_id IN (418);
UPDATE IGNORE bookmarks SET term_id=66 WHERE term_id IN (418);
DELETE FROM bookmarks WHERE term_id IN (418);
UPDATE IGNORE wrong_answer_terms SET term_id=66 WHERE term_id IN (418);
DELETE FROM wrong_answer_terms WHERE term_id IN (418);
UPDATE IGNORE custom_note_terms SET term_id=66 WHERE term_id IN (418);
DELETE FROM custom_note_terms WHERE term_id IN (418);
UPDATE IGNORE glossary_terms SET term_id=66 WHERE term_id IN (418);
DELETE FROM glossary_terms WHERE term_id IN (418);
UPDATE IGNORE term_morphemes SET term_id=66 WHERE term_id IN (418);
DELETE FROM term_morphemes WHERE term_id IN (418);
UPDATE IGNORE user_term_memos SET term_id=66 WHERE term_id IN (418);
DELETE FROM user_term_memos WHERE term_id IN (418);
UPDATE terms SET deleted_at=NOW() WHERE id IN (418) AND deleted_at IS NULL;

-- 그룹 30 대표 87 병합 485
UPDATE terms SET spelling='rhonchi', meaning='건성수포음', abbreviation=NULL, deleted_at=NULL WHERE id=87;
UPDATE IGNORE term_progresses SET term_id=87 WHERE term_id IN (485);
DELETE FROM term_progresses WHERE term_id IN (485);
UPDATE IGNORE bookmarks SET term_id=87 WHERE term_id IN (485);
DELETE FROM bookmarks WHERE term_id IN (485);
UPDATE IGNORE wrong_answer_terms SET term_id=87 WHERE term_id IN (485);
DELETE FROM wrong_answer_terms WHERE term_id IN (485);
UPDATE IGNORE custom_note_terms SET term_id=87 WHERE term_id IN (485);
DELETE FROM custom_note_terms WHERE term_id IN (485);
UPDATE IGNORE glossary_terms SET term_id=87 WHERE term_id IN (485);
DELETE FROM glossary_terms WHERE term_id IN (485);
UPDATE IGNORE term_morphemes SET term_id=87 WHERE term_id IN (485);
DELETE FROM term_morphemes WHERE term_id IN (485);
UPDATE IGNORE user_term_memos SET term_id=87 WHERE term_id IN (485);
DELETE FROM user_term_memos WHERE term_id IN (485);
UPDATE terms SET deleted_at=NOW() WHERE id IN (485) AND deleted_at IS NULL;

-- 그룹 31 대표 705 병합 982
UPDATE terms SET spelling='stenosis', meaning='협착, 협착증', abbreviation=NULL, deleted_at=NULL WHERE id=705;
UPDATE IGNORE term_progresses SET term_id=705 WHERE term_id IN (982);
DELETE FROM term_progresses WHERE term_id IN (982);
UPDATE IGNORE bookmarks SET term_id=705 WHERE term_id IN (982);
DELETE FROM bookmarks WHERE term_id IN (982);
UPDATE IGNORE wrong_answer_terms SET term_id=705 WHERE term_id IN (982);
DELETE FROM wrong_answer_terms WHERE term_id IN (982);
UPDATE IGNORE custom_note_terms SET term_id=705 WHERE term_id IN (982);
DELETE FROM custom_note_terms WHERE term_id IN (982);
UPDATE IGNORE glossary_terms SET term_id=705 WHERE term_id IN (982);
DELETE FROM glossary_terms WHERE term_id IN (982);
UPDATE IGNORE term_morphemes SET term_id=705 WHERE term_id IN (982);
DELETE FROM term_morphemes WHERE term_id IN (982);
UPDATE IGNORE user_term_memos SET term_id=705 WHERE term_id IN (982);
DELETE FROM user_term_memos WHERE term_id IN (982);
UPDATE terms SET deleted_at=NOW() WHERE id IN (982) AND deleted_at IS NULL;

-- 그룹 32 대표 125 병합 665
UPDATE terms SET spelling='cardiac tamponade', meaning='심장눌림증, 심낭압전, 심장압전', abbreviation=NULL, deleted_at=NULL WHERE id=125;
UPDATE IGNORE term_progresses SET term_id=125 WHERE term_id IN (665);
DELETE FROM term_progresses WHERE term_id IN (665);
UPDATE IGNORE bookmarks SET term_id=125 WHERE term_id IN (665);
DELETE FROM bookmarks WHERE term_id IN (665);
UPDATE IGNORE wrong_answer_terms SET term_id=125 WHERE term_id IN (665);
DELETE FROM wrong_answer_terms WHERE term_id IN (665);
UPDATE IGNORE custom_note_terms SET term_id=125 WHERE term_id IN (665);
DELETE FROM custom_note_terms WHERE term_id IN (665);
UPDATE IGNORE glossary_terms SET term_id=125 WHERE term_id IN (665);
DELETE FROM glossary_terms WHERE term_id IN (665);
UPDATE IGNORE term_morphemes SET term_id=125 WHERE term_id IN (665);
DELETE FROM term_morphemes WHERE term_id IN (665);
UPDATE IGNORE user_term_memos SET term_id=125 WHERE term_id IN (665);
DELETE FROM user_term_memos WHERE term_id IN (665);
UPDATE terms SET deleted_at=NOW() WHERE id IN (665) AND deleted_at IS NULL;

-- 그룹 33 대표 129 병합 497
UPDATE terms SET spelling='choking', meaning='질식', abbreviation=NULL, deleted_at=NULL WHERE id=129;
UPDATE IGNORE term_progresses SET term_id=129 WHERE term_id IN (497);
DELETE FROM term_progresses WHERE term_id IN (497);
UPDATE IGNORE bookmarks SET term_id=129 WHERE term_id IN (497);
DELETE FROM bookmarks WHERE term_id IN (497);
UPDATE IGNORE wrong_answer_terms SET term_id=129 WHERE term_id IN (497);
DELETE FROM wrong_answer_terms WHERE term_id IN (497);
UPDATE IGNORE custom_note_terms SET term_id=129 WHERE term_id IN (497);
DELETE FROM custom_note_terms WHERE term_id IN (497);
UPDATE IGNORE glossary_terms SET term_id=129 WHERE term_id IN (497);
DELETE FROM glossary_terms WHERE term_id IN (497);
UPDATE IGNORE term_morphemes SET term_id=129 WHERE term_id IN (497);
DELETE FROM term_morphemes WHERE term_id IN (497);
UPDATE IGNORE user_term_memos SET term_id=129 WHERE term_id IN (497);
DELETE FROM user_term_memos WHERE term_id IN (497);
UPDATE terms SET deleted_at=NOW() WHERE id IN (497) AND deleted_at IS NULL;

-- 그룹 34 대표 131 병합 622
UPDATE terms SET spelling='cholelithiasis', meaning='담석증, 쓸개돌증', abbreviation=NULL, deleted_at=NULL WHERE id=131;
UPDATE IGNORE term_progresses SET term_id=131 WHERE term_id IN (622);
DELETE FROM term_progresses WHERE term_id IN (622);
UPDATE IGNORE bookmarks SET term_id=131 WHERE term_id IN (622);
DELETE FROM bookmarks WHERE term_id IN (622);
UPDATE IGNORE wrong_answer_terms SET term_id=131 WHERE term_id IN (622);
DELETE FROM wrong_answer_terms WHERE term_id IN (622);
UPDATE IGNORE custom_note_terms SET term_id=131 WHERE term_id IN (622);
DELETE FROM custom_note_terms WHERE term_id IN (622);
UPDATE IGNORE glossary_terms SET term_id=131 WHERE term_id IN (622);
DELETE FROM glossary_terms WHERE term_id IN (622);
UPDATE IGNORE term_morphemes SET term_id=131 WHERE term_id IN (622);
DELETE FROM term_morphemes WHERE term_id IN (622);
UPDATE IGNORE user_term_memos SET term_id=131 WHERE term_id IN (622);
DELETE FROM user_term_memos WHERE term_id IN (622);
UPDATE terms SET deleted_at=NOW() WHERE id IN (622) AND deleted_at IS NULL;

-- 그룹 35 대표 37 병합 589
UPDATE terms SET spelling='dysphagia', meaning='연하곤란, 삼킴곤란, 삼킴장애', abbreviation=NULL, deleted_at=NULL WHERE id=37;
UPDATE IGNORE term_progresses SET term_id=37 WHERE term_id IN (589);
DELETE FROM term_progresses WHERE term_id IN (589);
UPDATE IGNORE bookmarks SET term_id=37 WHERE term_id IN (589);
DELETE FROM bookmarks WHERE term_id IN (589);
UPDATE IGNORE wrong_answer_terms SET term_id=37 WHERE term_id IN (589);
DELETE FROM wrong_answer_terms WHERE term_id IN (589);
UPDATE IGNORE custom_note_terms SET term_id=37 WHERE term_id IN (589);
DELETE FROM custom_note_terms WHERE term_id IN (589);
UPDATE IGNORE glossary_terms SET term_id=37 WHERE term_id IN (589);
DELETE FROM glossary_terms WHERE term_id IN (589);
UPDATE IGNORE term_morphemes SET term_id=37 WHERE term_id IN (589);
DELETE FROM term_morphemes WHERE term_id IN (589);
UPDATE IGNORE user_term_memos SET term_id=37 WHERE term_id IN (589);
DELETE FROM user_term_memos WHERE term_id IN (589);
UPDATE terms SET deleted_at=NOW() WHERE id IN (589) AND deleted_at IS NULL;

-- 그룹 36 대표 141 병합 848
UPDATE terms SET spelling='eclampsia', meaning='자간증, 자간전증', abbreviation=NULL, deleted_at=NULL WHERE id=141;
UPDATE IGNORE term_progresses SET term_id=141 WHERE term_id IN (848);
DELETE FROM term_progresses WHERE term_id IN (848);
UPDATE IGNORE bookmarks SET term_id=141 WHERE term_id IN (848);
DELETE FROM bookmarks WHERE term_id IN (848);
UPDATE IGNORE wrong_answer_terms SET term_id=141 WHERE term_id IN (848);
DELETE FROM wrong_answer_terms WHERE term_id IN (848);
UPDATE IGNORE custom_note_terms SET term_id=141 WHERE term_id IN (848);
DELETE FROM custom_note_terms WHERE term_id IN (848);
UPDATE IGNORE glossary_terms SET term_id=141 WHERE term_id IN (848);
DELETE FROM glossary_terms WHERE term_id IN (848);
UPDATE IGNORE term_morphemes SET term_id=141 WHERE term_id IN (848);
DELETE FROM term_morphemes WHERE term_id IN (848);
UPDATE IGNORE user_term_memos SET term_id=141 WHERE term_id IN (848);
DELETE FROM user_term_memos WHERE term_id IN (848);
UPDATE terms SET deleted_at=NOW() WHERE id IN (848) AND deleted_at IS NULL;

-- 그룹 37 대표 273 병합 585
UPDATE terms SET spelling='nausea', meaning='오심, 메스꺼움, 구역', abbreviation=NULL, deleted_at=NULL WHERE id=273;
UPDATE IGNORE term_progresses SET term_id=273 WHERE term_id IN (585);
DELETE FROM term_progresses WHERE term_id IN (585);
UPDATE IGNORE bookmarks SET term_id=273 WHERE term_id IN (585);
DELETE FROM bookmarks WHERE term_id IN (585);
UPDATE IGNORE wrong_answer_terms SET term_id=273 WHERE term_id IN (585);
DELETE FROM wrong_answer_terms WHERE term_id IN (585);
UPDATE IGNORE custom_note_terms SET term_id=273 WHERE term_id IN (585);
DELETE FROM custom_note_terms WHERE term_id IN (585);
UPDATE IGNORE glossary_terms SET term_id=273 WHERE term_id IN (585);
DELETE FROM glossary_terms WHERE term_id IN (585);
UPDATE IGNORE term_morphemes SET term_id=273 WHERE term_id IN (585);
DELETE FROM term_morphemes WHERE term_id IN (585);
UPDATE IGNORE user_term_memos SET term_id=273 WHERE term_id IN (585);
DELETE FROM user_term_memos WHERE term_id IN (585);
UPDATE terms SET deleted_at=NOW() WHERE id IN (585) AND deleted_at IS NULL;

-- 그룹 38 대표 173 병합 700
UPDATE terms SET spelling='pericardiocentesis', meaning='심낭천자, 심장막천자', abbreviation=NULL, deleted_at=NULL WHERE id=173;
UPDATE IGNORE term_progresses SET term_id=173 WHERE term_id IN (700);
DELETE FROM term_progresses WHERE term_id IN (700);
UPDATE IGNORE bookmarks SET term_id=173 WHERE term_id IN (700);
DELETE FROM bookmarks WHERE term_id IN (700);
UPDATE IGNORE wrong_answer_terms SET term_id=173 WHERE term_id IN (700);
DELETE FROM wrong_answer_terms WHERE term_id IN (700);
UPDATE IGNORE custom_note_terms SET term_id=173 WHERE term_id IN (700);
DELETE FROM custom_note_terms WHERE term_id IN (700);
UPDATE IGNORE glossary_terms SET term_id=173 WHERE term_id IN (700);
DELETE FROM glossary_terms WHERE term_id IN (700);
UPDATE IGNORE term_morphemes SET term_id=173 WHERE term_id IN (700);
DELETE FROM term_morphemes WHERE term_id IN (700);
UPDATE IGNORE user_term_memos SET term_id=173 WHERE term_id IN (700);
DELETE FROM user_term_memos WHERE term_id IN (700);
UPDATE terms SET deleted_at=NOW() WHERE id IN (700) AND deleted_at IS NULL;

-- 그룹 39 대표 143 병합 625
UPDATE terms SET spelling='peritonitis', meaning='복막염', abbreviation=NULL, deleted_at=NULL WHERE id=143;
UPDATE IGNORE term_progresses SET term_id=143 WHERE term_id IN (625);
DELETE FROM term_progresses WHERE term_id IN (625);
UPDATE IGNORE bookmarks SET term_id=143 WHERE term_id IN (625);
DELETE FROM bookmarks WHERE term_id IN (625);
UPDATE IGNORE wrong_answer_terms SET term_id=143 WHERE term_id IN (625);
DELETE FROM wrong_answer_terms WHERE term_id IN (625);
UPDATE IGNORE custom_note_terms SET term_id=143 WHERE term_id IN (625);
DELETE FROM custom_note_terms WHERE term_id IN (625);
UPDATE IGNORE glossary_terms SET term_id=143 WHERE term_id IN (625);
DELETE FROM glossary_terms WHERE term_id IN (625);
UPDATE IGNORE term_morphemes SET term_id=143 WHERE term_id IN (625);
DELETE FROM term_morphemes WHERE term_id IN (625);
UPDATE IGNORE user_term_memos SET term_id=143 WHERE term_id IN (625);
DELETE FROM user_term_memos WHERE term_id IN (625);
UPDATE terms SET deleted_at=NOW() WHERE id IN (625) AND deleted_at IS NULL;

-- 그룹 40 대표 356 병합 518
UPDATE terms SET spelling='pleural effusion', meaning='흉수, 흉막삼출', abbreviation=NULL, deleted_at=NULL WHERE id=356;
UPDATE IGNORE term_progresses SET term_id=356 WHERE term_id IN (518);
DELETE FROM term_progresses WHERE term_id IN (518);
UPDATE IGNORE bookmarks SET term_id=356 WHERE term_id IN (518);
DELETE FROM bookmarks WHERE term_id IN (518);
UPDATE IGNORE wrong_answer_terms SET term_id=356 WHERE term_id IN (518);
DELETE FROM wrong_answer_terms WHERE term_id IN (518);
UPDATE IGNORE custom_note_terms SET term_id=356 WHERE term_id IN (518);
DELETE FROM custom_note_terms WHERE term_id IN (518);
UPDATE IGNORE glossary_terms SET term_id=356 WHERE term_id IN (518);
DELETE FROM glossary_terms WHERE term_id IN (518);
UPDATE IGNORE term_morphemes SET term_id=356 WHERE term_id IN (518);
DELETE FROM term_morphemes WHERE term_id IN (518);
UPDATE IGNORE user_term_memos SET term_id=356 WHERE term_id IN (518);
DELETE FROM user_term_memos WHERE term_id IN (518);
UPDATE terms SET deleted_at=NOW() WHERE id IN (518) AND deleted_at IS NULL;

-- 그룹 41 대표 204 병합 1136
UPDATE terms SET spelling='arterial blood gas analysis', meaning='동맥혈가스분석', abbreviation='ABGA', deleted_at=NULL WHERE id=204;
UPDATE IGNORE term_progresses SET term_id=204 WHERE term_id IN (1136);
DELETE FROM term_progresses WHERE term_id IN (1136);
UPDATE IGNORE bookmarks SET term_id=204 WHERE term_id IN (1136);
DELETE FROM bookmarks WHERE term_id IN (1136);
UPDATE IGNORE wrong_answer_terms SET term_id=204 WHERE term_id IN (1136);
DELETE FROM wrong_answer_terms WHERE term_id IN (1136);
UPDATE IGNORE custom_note_terms SET term_id=204 WHERE term_id IN (1136);
DELETE FROM custom_note_terms WHERE term_id IN (1136);
UPDATE IGNORE glossary_terms SET term_id=204 WHERE term_id IN (1136);
DELETE FROM glossary_terms WHERE term_id IN (1136);
UPDATE IGNORE term_morphemes SET term_id=204 WHERE term_id IN (1136);
DELETE FROM term_morphemes WHERE term_id IN (1136);
UPDATE IGNORE user_term_memos SET term_id=204 WHERE term_id IN (1136);
DELETE FROM user_term_memos WHERE term_id IN (1136);
UPDATE terms SET deleted_at=NOW() WHERE id IN (1136) AND deleted_at IS NULL;

-- 그룹 42 대표 641 병합 659
UPDATE terms SET spelling='biopsy', meaning='생검, 조직검사', abbreviation='Bx', deleted_at=NULL WHERE id=641;
UPDATE IGNORE term_progresses SET term_id=641 WHERE term_id IN (659);
DELETE FROM term_progresses WHERE term_id IN (659);
UPDATE IGNORE bookmarks SET term_id=641 WHERE term_id IN (659);
DELETE FROM bookmarks WHERE term_id IN (659);
UPDATE IGNORE wrong_answer_terms SET term_id=641 WHERE term_id IN (659);
DELETE FROM wrong_answer_terms WHERE term_id IN (659);
UPDATE IGNORE custom_note_terms SET term_id=641 WHERE term_id IN (659);
DELETE FROM custom_note_terms WHERE term_id IN (659);
UPDATE IGNORE glossary_terms SET term_id=641 WHERE term_id IN (659);
DELETE FROM glossary_terms WHERE term_id IN (659);
UPDATE IGNORE term_morphemes SET term_id=641 WHERE term_id IN (659);
DELETE FROM term_morphemes WHERE term_id IN (659);
UPDATE IGNORE user_term_memos SET term_id=641 WHERE term_id IN (659);
DELETE FROM user_term_memos WHERE term_id IN (659);
UPDATE terms SET deleted_at=NOW() WHERE id IN (659) AND deleted_at IS NULL;

-- 그룹 43 대표 400 병합 1037
UPDATE terms SET spelling='bone graft', meaning='골이식', abbreviation=NULL, deleted_at=NULL WHERE id=400;
UPDATE IGNORE term_progresses SET term_id=400 WHERE term_id IN (1037);
DELETE FROM term_progresses WHERE term_id IN (1037);
UPDATE IGNORE bookmarks SET term_id=400 WHERE term_id IN (1037);
DELETE FROM bookmarks WHERE term_id IN (1037);
UPDATE IGNORE wrong_answer_terms SET term_id=400 WHERE term_id IN (1037);
DELETE FROM wrong_answer_terms WHERE term_id IN (1037);
UPDATE IGNORE custom_note_terms SET term_id=400 WHERE term_id IN (1037);
DELETE FROM custom_note_terms WHERE term_id IN (1037);
UPDATE IGNORE glossary_terms SET term_id=400 WHERE term_id IN (1037);
DELETE FROM glossary_terms WHERE term_id IN (1037);
UPDATE IGNORE term_morphemes SET term_id=400 WHERE term_id IN (1037);
DELETE FROM term_morphemes WHERE term_id IN (1037);
UPDATE IGNORE user_term_memos SET term_id=400 WHERE term_id IN (1037);
DELETE FROM user_term_memos WHERE term_id IN (1037);
UPDATE terms SET deleted_at=NOW() WHERE id IN (1037) AND deleted_at IS NULL;

-- 그룹 44 대표 23 병합 226
UPDATE terms SET spelling='chronic renal failure', meaning='만성신부전', abbreviation='CRF', deleted_at=NULL WHERE id=23;
UPDATE IGNORE term_progresses SET term_id=23 WHERE term_id IN (226);
DELETE FROM term_progresses WHERE term_id IN (226);
UPDATE IGNORE bookmarks SET term_id=23 WHERE term_id IN (226);
DELETE FROM bookmarks WHERE term_id IN (226);
UPDATE IGNORE wrong_answer_terms SET term_id=23 WHERE term_id IN (226);
DELETE FROM wrong_answer_terms WHERE term_id IN (226);
UPDATE IGNORE custom_note_terms SET term_id=23 WHERE term_id IN (226);
DELETE FROM custom_note_terms WHERE term_id IN (226);
UPDATE IGNORE glossary_terms SET term_id=23 WHERE term_id IN (226);
DELETE FROM glossary_terms WHERE term_id IN (226);
UPDATE IGNORE term_morphemes SET term_id=23 WHERE term_id IN (226);
DELETE FROM term_morphemes WHERE term_id IN (226);
UPDATE IGNORE user_term_memos SET term_id=23 WHERE term_id IN (226);
DELETE FROM user_term_memos WHERE term_id IN (226);
UPDATE terms SET deleted_at=NOW() WHERE id IN (226) AND deleted_at IS NULL;

-- 그룹 45 대표 146 병합 324 1012
UPDATE terms SET spelling='epidural hematoma', meaning='경막외혈종, 경막외 출혈', abbreviation='EDH', deleted_at=NULL WHERE id=146;
UPDATE IGNORE term_progresses SET term_id=146 WHERE term_id IN (324,1012);
DELETE FROM term_progresses WHERE term_id IN (324,1012);
UPDATE IGNORE bookmarks SET term_id=146 WHERE term_id IN (324,1012);
DELETE FROM bookmarks WHERE term_id IN (324,1012);
UPDATE IGNORE wrong_answer_terms SET term_id=146 WHERE term_id IN (324,1012);
DELETE FROM wrong_answer_terms WHERE term_id IN (324,1012);
UPDATE IGNORE custom_note_terms SET term_id=146 WHERE term_id IN (324,1012);
DELETE FROM custom_note_terms WHERE term_id IN (324,1012);
UPDATE IGNORE glossary_terms SET term_id=146 WHERE term_id IN (324,1012);
DELETE FROM glossary_terms WHERE term_id IN (324,1012);
UPDATE IGNORE term_morphemes SET term_id=146 WHERE term_id IN (324,1012);
DELETE FROM term_morphemes WHERE term_id IN (324,1012);
UPDATE IGNORE user_term_memos SET term_id=146 WHERE term_id IN (324,1012);
DELETE FROM user_term_memos WHERE term_id IN (324,1012);
UPDATE terms SET deleted_at=NOW() WHERE id IN (324,1012) AND deleted_at IS NULL;

-- 그룹 46 대표 265 병합 344
UPDATE terms SET spelling='lumbar puncture', meaning='요추천자', abbreviation='LP', deleted_at=NULL WHERE id=265;
UPDATE IGNORE term_progresses SET term_id=265 WHERE term_id IN (344);
DELETE FROM term_progresses WHERE term_id IN (344);
UPDATE IGNORE bookmarks SET term_id=265 WHERE term_id IN (344);
DELETE FROM bookmarks WHERE term_id IN (344);
UPDATE IGNORE wrong_answer_terms SET term_id=265 WHERE term_id IN (344);
DELETE FROM wrong_answer_terms WHERE term_id IN (344);
UPDATE IGNORE custom_note_terms SET term_id=265 WHERE term_id IN (344);
DELETE FROM custom_note_terms WHERE term_id IN (344);
UPDATE IGNORE glossary_terms SET term_id=265 WHERE term_id IN (344);
DELETE FROM glossary_terms WHERE term_id IN (344);
UPDATE IGNORE term_morphemes SET term_id=265 WHERE term_id IN (344);
DELETE FROM term_morphemes WHERE term_id IN (344);
UPDATE IGNORE user_term_memos SET term_id=265 WHERE term_id IN (344);
DELETE FROM user_term_memos WHERE term_id IN (344);
UPDATE terms SET deleted_at=NOW() WHERE id IN (344) AND deleted_at IS NULL;

-- 그룹 47 대표 69 병합 432
UPDATE terms SET spelling='osteoarthritis', meaning='골관절염', abbreviation='OA', deleted_at=NULL WHERE id=69;
UPDATE IGNORE term_progresses SET term_id=69 WHERE term_id IN (432);
DELETE FROM term_progresses WHERE term_id IN (432);
UPDATE IGNORE bookmarks SET term_id=69 WHERE term_id IN (432);
DELETE FROM bookmarks WHERE term_id IN (432);
UPDATE IGNORE wrong_answer_terms SET term_id=69 WHERE term_id IN (432);
DELETE FROM wrong_answer_terms WHERE term_id IN (432);
UPDATE IGNORE custom_note_terms SET term_id=69 WHERE term_id IN (432);
DELETE FROM custom_note_terms WHERE term_id IN (432);
UPDATE IGNORE glossary_terms SET term_id=69 WHERE term_id IN (432);
DELETE FROM glossary_terms WHERE term_id IN (432);
UPDATE IGNORE term_morphemes SET term_id=69 WHERE term_id IN (432);
DELETE FROM term_morphemes WHERE term_id IN (432);
UPDATE IGNORE user_term_memos SET term_id=69 WHERE term_id IN (432);
DELETE FROM user_term_memos WHERE term_id IN (432);
UPDATE terms SET deleted_at=NOW() WHERE id IN (432) AND deleted_at IS NULL;

-- 그룹 48 대표 352 병합 409
UPDATE terms SET spelling='paraplegia', meaning='하반신 마비', abbreviation=NULL, deleted_at=NULL WHERE id=352;
UPDATE IGNORE term_progresses SET term_id=352 WHERE term_id IN (409);
DELETE FROM term_progresses WHERE term_id IN (409);
UPDATE IGNORE bookmarks SET term_id=352 WHERE term_id IN (409);
DELETE FROM bookmarks WHERE term_id IN (409);
UPDATE IGNORE wrong_answer_terms SET term_id=352 WHERE term_id IN (409);
DELETE FROM wrong_answer_terms WHERE term_id IN (409);
UPDATE IGNORE custom_note_terms SET term_id=352 WHERE term_id IN (409);
DELETE FROM custom_note_terms WHERE term_id IN (409);
UPDATE IGNORE glossary_terms SET term_id=352 WHERE term_id IN (409);
DELETE FROM glossary_terms WHERE term_id IN (409);
UPDATE IGNORE term_morphemes SET term_id=352 WHERE term_id IN (409);
DELETE FROM term_morphemes WHERE term_id IN (409);
UPDATE IGNORE user_term_memos SET term_id=352 WHERE term_id IN (409);
DELETE FROM user_term_memos WHERE term_id IN (409);
UPDATE terms SET deleted_at=NOW() WHERE id IN (409) AND deleted_at IS NULL;

-- 그룹 49 대표 75 병합 393
UPDATE terms SET spelling='physical therapy', meaning='물리치료', abbreviation='PT', deleted_at=NULL WHERE id=75;
UPDATE IGNORE term_progresses SET term_id=75 WHERE term_id IN (393);
DELETE FROM term_progresses WHERE term_id IN (393);
UPDATE IGNORE bookmarks SET term_id=75 WHERE term_id IN (393);
DELETE FROM bookmarks WHERE term_id IN (393);
UPDATE IGNORE wrong_answer_terms SET term_id=75 WHERE term_id IN (393);
DELETE FROM wrong_answer_terms WHERE term_id IN (393);
UPDATE IGNORE custom_note_terms SET term_id=75 WHERE term_id IN (393);
DELETE FROM custom_note_terms WHERE term_id IN (393);
UPDATE IGNORE glossary_terms SET term_id=75 WHERE term_id IN (393);
DELETE FROM glossary_terms WHERE term_id IN (393);
UPDATE IGNORE term_morphemes SET term_id=75 WHERE term_id IN (393);
DELETE FROM term_morphemes WHERE term_id IN (393);
UPDATE IGNORE user_term_memos SET term_id=75 WHERE term_id IN (393);
DELETE FROM user_term_memos WHERE term_id IN (393);
UPDATE terms SET deleted_at=NOW() WHERE id IN (393) AND deleted_at IS NULL;

-- 그룹 50 대표 83 병합 365
UPDATE terms SET spelling='pulmonary tuberculosis', meaning='폐결핵', abbreviation='TB, Tbc', deleted_at=NULL WHERE id=83;
UPDATE IGNORE term_progresses SET term_id=83 WHERE term_id IN (365);
DELETE FROM term_progresses WHERE term_id IN (365);
UPDATE IGNORE bookmarks SET term_id=83 WHERE term_id IN (365);
DELETE FROM bookmarks WHERE term_id IN (365);
UPDATE IGNORE wrong_answer_terms SET term_id=83 WHERE term_id IN (365);
DELETE FROM wrong_answer_terms WHERE term_id IN (365);
UPDATE IGNORE custom_note_terms SET term_id=83 WHERE term_id IN (365);
DELETE FROM custom_note_terms WHERE term_id IN (365);
UPDATE IGNORE glossary_terms SET term_id=83 WHERE term_id IN (365);
DELETE FROM glossary_terms WHERE term_id IN (365);
UPDATE IGNORE term_morphemes SET term_id=83 WHERE term_id IN (365);
DELETE FROM term_morphemes WHERE term_id IN (365);
UPDATE IGNORE user_term_memos SET term_id=83 WHERE term_id IN (365);
DELETE FROM user_term_memos WHERE term_id IN (365);
UPDATE terms SET deleted_at=NOW() WHERE id IN (365) AND deleted_at IS NULL;

-- 그룹 51 대표 369 병합 492
UPDATE terms SET spelling='secretion', meaning='분비물', abbreviation=NULL, deleted_at=NULL WHERE id=369;
UPDATE IGNORE term_progresses SET term_id=369 WHERE term_id IN (492);
DELETE FROM term_progresses WHERE term_id IN (492);
UPDATE IGNORE bookmarks SET term_id=369 WHERE term_id IN (492);
DELETE FROM bookmarks WHERE term_id IN (492);
UPDATE IGNORE wrong_answer_terms SET term_id=369 WHERE term_id IN (492);
DELETE FROM wrong_answer_terms WHERE term_id IN (492);
UPDATE IGNORE custom_note_terms SET term_id=369 WHERE term_id IN (492);
DELETE FROM custom_note_terms WHERE term_id IN (492);
UPDATE IGNORE glossary_terms SET term_id=369 WHERE term_id IN (492);
DELETE FROM glossary_terms WHERE term_id IN (492);
UPDATE IGNORE term_morphemes SET term_id=369 WHERE term_id IN (492);
DELETE FROM term_morphemes WHERE term_id IN (492);
UPDATE IGNORE user_term_memos SET term_id=369 WHERE term_id IN (492);
DELETE FROM user_term_memos WHERE term_id IN (492);
UPDATE terms SET deleted_at=NOW() WHERE id IN (492) AND deleted_at IS NULL;

-- 그룹 52 대표 99 병합 1054
UPDATE terms SET spelling='tardive dyskinesia', meaning='지연발생운동이상증, 지연성 운동장애', abbreviation='TD', deleted_at=NULL WHERE id=99;
UPDATE IGNORE term_progresses SET term_id=99 WHERE term_id IN (1054);
DELETE FROM term_progresses WHERE term_id IN (1054);
UPDATE IGNORE bookmarks SET term_id=99 WHERE term_id IN (1054);
DELETE FROM bookmarks WHERE term_id IN (1054);
UPDATE IGNORE wrong_answer_terms SET term_id=99 WHERE term_id IN (1054);
DELETE FROM wrong_answer_terms WHERE term_id IN (1054);
UPDATE IGNORE custom_note_terms SET term_id=99 WHERE term_id IN (1054);
DELETE FROM custom_note_terms WHERE term_id IN (1054);
UPDATE IGNORE glossary_terms SET term_id=99 WHERE term_id IN (1054);
DELETE FROM glossary_terms WHERE term_id IN (1054);
UPDATE IGNORE term_morphemes SET term_id=99 WHERE term_id IN (1054);
DELETE FROM term_morphemes WHERE term_id IN (1054);
UPDATE IGNORE user_term_memos SET term_id=99 WHERE term_id IN (1054);
DELETE FROM user_term_memos WHERE term_id IN (1054);
UPDATE terms SET deleted_at=NOW() WHERE id IN (1054) AND deleted_at IS NULL;

-- 그룹 53 대표 21 병합 302
UPDATE terms SET spelling='cerebral infarction', meaning='뇌경색', abbreviation=NULL, deleted_at=NULL WHERE id=21;
UPDATE IGNORE term_progresses SET term_id=21 WHERE term_id IN (302);
DELETE FROM term_progresses WHERE term_id IN (302);
UPDATE IGNORE bookmarks SET term_id=21 WHERE term_id IN (302);
DELETE FROM bookmarks WHERE term_id IN (302);
UPDATE IGNORE wrong_answer_terms SET term_id=21 WHERE term_id IN (302);
DELETE FROM wrong_answer_terms WHERE term_id IN (302);
UPDATE IGNORE custom_note_terms SET term_id=21 WHERE term_id IN (302);
DELETE FROM custom_note_terms WHERE term_id IN (302);
UPDATE IGNORE glossary_terms SET term_id=21 WHERE term_id IN (302);
DELETE FROM glossary_terms WHERE term_id IN (302);
UPDATE IGNORE term_morphemes SET term_id=21 WHERE term_id IN (302);
DELETE FROM term_morphemes WHERE term_id IN (302);
UPDATE IGNORE user_term_memos SET term_id=21 WHERE term_id IN (302);
DELETE FROM user_term_memos WHERE term_id IN (302);
UPDATE terms SET deleted_at=NOW() WHERE id IN (302) AND deleted_at IS NULL;

-- 그룹 54 대표 105 병합 379
UPDATE terms SET spelling='transient ischemic attack', meaning='일과성 허혈 발작, 일과성 뇌허혈증', abbreviation='TIA', deleted_at=NULL WHERE id=105;
UPDATE IGNORE term_progresses SET term_id=105 WHERE term_id IN (379);
DELETE FROM term_progresses WHERE term_id IN (379);
UPDATE IGNORE bookmarks SET term_id=105 WHERE term_id IN (379);
DELETE FROM bookmarks WHERE term_id IN (379);
UPDATE IGNORE wrong_answer_terms SET term_id=105 WHERE term_id IN (379);
DELETE FROM wrong_answer_terms WHERE term_id IN (379);
UPDATE IGNORE custom_note_terms SET term_id=105 WHERE term_id IN (379);
DELETE FROM custom_note_terms WHERE term_id IN (379);
UPDATE IGNORE glossary_terms SET term_id=105 WHERE term_id IN (379);
DELETE FROM glossary_terms WHERE term_id IN (379);
UPDATE IGNORE term_morphemes SET term_id=105 WHERE term_id IN (379);
DELETE FROM term_morphemes WHERE term_id IN (379);
UPDATE IGNORE user_term_memos SET term_id=105 WHERE term_id IN (379);
DELETE FROM user_term_memos WHERE term_id IN (379);
UPDATE terms SET deleted_at=NOW() WHERE id IN (379) AND deleted_at IS NULL;

-- 그룹 55 대표 243 병합 1004
UPDATE terms SET spelling='fever of unknown origin', meaning='불명열, 원인을 알 수 없는 열', abbreviation='FUO', deleted_at=NULL WHERE id=243;
UPDATE IGNORE term_progresses SET term_id=243 WHERE term_id IN (1004);
DELETE FROM term_progresses WHERE term_id IN (1004);
UPDATE IGNORE bookmarks SET term_id=243 WHERE term_id IN (1004);
DELETE FROM bookmarks WHERE term_id IN (1004);
UPDATE IGNORE wrong_answer_terms SET term_id=243 WHERE term_id IN (1004);
DELETE FROM wrong_answer_terms WHERE term_id IN (1004);
UPDATE IGNORE custom_note_terms SET term_id=243 WHERE term_id IN (1004);
DELETE FROM custom_note_terms WHERE term_id IN (1004);
UPDATE IGNORE glossary_terms SET term_id=243 WHERE term_id IN (1004);
DELETE FROM glossary_terms WHERE term_id IN (1004);
UPDATE IGNORE term_morphemes SET term_id=243 WHERE term_id IN (1004);
DELETE FROM term_morphemes WHERE term_id IN (1004);
UPDATE IGNORE user_term_memos SET term_id=243 WHERE term_id IN (1004);
DELETE FROM user_term_memos WHERE term_id IN (1004);
UPDATE terms SET deleted_at=NOW() WHERE id IN (1004) AND deleted_at IS NULL;

-- 그룹 56 대표 276 병합 526
UPDATE terms SET spelling='pulmonary function test', meaning='폐기능검사', abbreviation='PFT', deleted_at=NULL WHERE id=276;
UPDATE IGNORE term_progresses SET term_id=276 WHERE term_id IN (526);
DELETE FROM term_progresses WHERE term_id IN (526);
UPDATE IGNORE bookmarks SET term_id=276 WHERE term_id IN (526);
DELETE FROM bookmarks WHERE term_id IN (526);
UPDATE IGNORE wrong_answer_terms SET term_id=276 WHERE term_id IN (526);
DELETE FROM wrong_answer_terms WHERE term_id IN (526);
UPDATE IGNORE custom_note_terms SET term_id=276 WHERE term_id IN (526);
DELETE FROM custom_note_terms WHERE term_id IN (526);
UPDATE IGNORE glossary_terms SET term_id=276 WHERE term_id IN (526);
DELETE FROM glossary_terms WHERE term_id IN (526);
UPDATE IGNORE term_morphemes SET term_id=276 WHERE term_id IN (526);
DELETE FROM term_morphemes WHERE term_id IN (526);
UPDATE IGNORE user_term_memos SET term_id=276 WHERE term_id IN (526);
DELETE FROM user_term_memos WHERE term_id IN (526);
UPDATE terms SET deleted_at=NOW() WHERE id IN (526) AND deleted_at IS NULL;

-- 그룹 57 대표 308 병합 671
UPDATE terms SET spelling='complete blood count', meaning='전혈구검사, 일반혈액검사', abbreviation='CBC', deleted_at=NULL WHERE id=308;
UPDATE IGNORE term_progresses SET term_id=308 WHERE term_id IN (671);
DELETE FROM term_progresses WHERE term_id IN (671);
UPDATE IGNORE bookmarks SET term_id=308 WHERE term_id IN (671);
DELETE FROM bookmarks WHERE term_id IN (671);
UPDATE IGNORE wrong_answer_terms SET term_id=308 WHERE term_id IN (671);
DELETE FROM wrong_answer_terms WHERE term_id IN (671);
UPDATE IGNORE custom_note_terms SET term_id=308 WHERE term_id IN (671);
DELETE FROM custom_note_terms WHERE term_id IN (671);
UPDATE IGNORE glossary_terms SET term_id=308 WHERE term_id IN (671);
DELETE FROM glossary_terms WHERE term_id IN (671);
UPDATE IGNORE term_morphemes SET term_id=308 WHERE term_id IN (671);
DELETE FROM term_morphemes WHERE term_id IN (671);
UPDATE IGNORE user_term_memos SET term_id=308 WHERE term_id IN (671);
DELETE FROM user_term_memos WHERE term_id IN (671);
UPDATE terms SET deleted_at=NOW() WHERE id IN (671) AND deleted_at IS NULL;

-- 그룹 58 대표 322 병합 974
UPDATE terms SET spelling='electromyography', meaning='근전도 검사', abbreviation='EMG', deleted_at=NULL WHERE id=322;
UPDATE IGNORE term_progresses SET term_id=322 WHERE term_id IN (974);
DELETE FROM term_progresses WHERE term_id IN (974);
UPDATE IGNORE bookmarks SET term_id=322 WHERE term_id IN (974);
DELETE FROM bookmarks WHERE term_id IN (974);
UPDATE IGNORE wrong_answer_terms SET term_id=322 WHERE term_id IN (974);
DELETE FROM wrong_answer_terms WHERE term_id IN (974);
UPDATE IGNORE custom_note_terms SET term_id=322 WHERE term_id IN (974);
DELETE FROM custom_note_terms WHERE term_id IN (974);
UPDATE IGNORE glossary_terms SET term_id=322 WHERE term_id IN (974);
DELETE FROM glossary_terms WHERE term_id IN (974);
UPDATE IGNORE term_morphemes SET term_id=322 WHERE term_id IN (974);
DELETE FROM term_morphemes WHERE term_id IN (974);
UPDATE IGNORE user_term_memos SET term_id=322 WHERE term_id IN (974);
DELETE FROM user_term_memos WHERE term_id IN (974);
UPDATE terms SET deleted_at=NOW() WHERE id IN (974) AND deleted_at IS NULL;

-- 그룹 59 대표 703 병합 786
UPDATE terms SET spelling='red blood cell', meaning='적혈구', abbreviation='RBC', deleted_at=NULL WHERE id=703;
UPDATE IGNORE term_progresses SET term_id=703 WHERE term_id IN (786);
DELETE FROM term_progresses WHERE term_id IN (786);
UPDATE IGNORE bookmarks SET term_id=703 WHERE term_id IN (786);
DELETE FROM bookmarks WHERE term_id IN (786);
UPDATE IGNORE wrong_answer_terms SET term_id=703 WHERE term_id IN (786);
DELETE FROM wrong_answer_terms WHERE term_id IN (786);
UPDATE IGNORE custom_note_terms SET term_id=703 WHERE term_id IN (786);
DELETE FROM custom_note_terms WHERE term_id IN (786);
UPDATE IGNORE glossary_terms SET term_id=703 WHERE term_id IN (786);
DELETE FROM glossary_terms WHERE term_id IN (786);
UPDATE IGNORE term_morphemes SET term_id=703 WHERE term_id IN (786);
DELETE FROM term_morphemes WHERE term_id IN (786);
UPDATE IGNORE user_term_memos SET term_id=703 WHERE term_id IN (786);
DELETE FROM user_term_memos WHERE term_id IN (786);
UPDATE terms SET deleted_at=NOW() WHERE id IN (786) AND deleted_at IS NULL;

-- 그룹 60 대표 717 병합 817
UPDATE terms SET spelling='white blood cell', meaning='백혈구', abbreviation='WBC', deleted_at=NULL WHERE id=717;
UPDATE IGNORE term_progresses SET term_id=717 WHERE term_id IN (817);
DELETE FROM term_progresses WHERE term_id IN (817);
UPDATE IGNORE bookmarks SET term_id=717 WHERE term_id IN (817);
DELETE FROM bookmarks WHERE term_id IN (817);
UPDATE IGNORE wrong_answer_terms SET term_id=717 WHERE term_id IN (817);
DELETE FROM wrong_answer_terms WHERE term_id IN (817);
UPDATE IGNORE custom_note_terms SET term_id=717 WHERE term_id IN (817);
DELETE FROM custom_note_terms WHERE term_id IN (817);
UPDATE IGNORE glossary_terms SET term_id=717 WHERE term_id IN (817);
DELETE FROM glossary_terms WHERE term_id IN (817);
UPDATE IGNORE term_morphemes SET term_id=717 WHERE term_id IN (817);
DELETE FROM term_morphemes WHERE term_id IN (817);
UPDATE IGNORE user_term_memos SET term_id=717 WHERE term_id IN (817);
DELETE FROM user_term_memos WHERE term_id IN (817);
UPDATE terms SET deleted_at=NOW() WHERE id IN (817) AND deleted_at IS NULL;

-- 그룹 61 대표 219 병합 1140
UPDATE terms SET spelling='coronary artery bypass grafting', meaning='관상동맥우회술', abbreviation='CABG', deleted_at=NULL WHERE id=219;
UPDATE IGNORE term_progresses SET term_id=219 WHERE term_id IN (1140);
DELETE FROM term_progresses WHERE term_id IN (1140);
UPDATE IGNORE bookmarks SET term_id=219 WHERE term_id IN (1140);
DELETE FROM bookmarks WHERE term_id IN (1140);
UPDATE IGNORE wrong_answer_terms SET term_id=219 WHERE term_id IN (1140);
DELETE FROM wrong_answer_terms WHERE term_id IN (1140);
UPDATE IGNORE custom_note_terms SET term_id=219 WHERE term_id IN (1140);
DELETE FROM custom_note_terms WHERE term_id IN (1140);
UPDATE IGNORE glossary_terms SET term_id=219 WHERE term_id IN (1140);
DELETE FROM glossary_terms WHERE term_id IN (1140);
UPDATE IGNORE term_morphemes SET term_id=219 WHERE term_id IN (1140);
DELETE FROM term_morphemes WHERE term_id IN (1140);
UPDATE IGNORE user_term_memos SET term_id=219 WHERE term_id IN (1140);
DELETE FROM user_term_memos WHERE term_id IN (1140);
UPDATE terms SET deleted_at=NOW() WHERE id IN (1140) AND deleted_at IS NULL;

-- 그룹 62 대표 357 병합 517
UPDATE terms SET spelling='pleurisy, pleuritis', meaning='흉막염, 가슴막염', abbreviation=NULL, deleted_at=NULL WHERE id=357;
UPDATE IGNORE term_progresses SET term_id=357 WHERE term_id IN (517);
DELETE FROM term_progresses WHERE term_id IN (517);
UPDATE IGNORE bookmarks SET term_id=357 WHERE term_id IN (517);
DELETE FROM bookmarks WHERE term_id IN (517);
UPDATE IGNORE wrong_answer_terms SET term_id=357 WHERE term_id IN (517);
DELETE FROM wrong_answer_terms WHERE term_id IN (517);
UPDATE IGNORE custom_note_terms SET term_id=357 WHERE term_id IN (517);
DELETE FROM custom_note_terms WHERE term_id IN (517);
UPDATE IGNORE glossary_terms SET term_id=357 WHERE term_id IN (517);
DELETE FROM glossary_terms WHERE term_id IN (517);
UPDATE IGNORE term_morphemes SET term_id=357 WHERE term_id IN (517);
DELETE FROM term_morphemes WHERE term_id IN (517);
UPDATE IGNORE user_term_memos SET term_id=357 WHERE term_id IN (517);
DELETE FROM user_term_memos WHERE term_id IN (517);
UPDATE terms SET deleted_at=NOW() WHERE id IN (517) AND deleted_at IS NULL;

-- 그룹 63 대표 855 병합 936
UPDATE terms SET spelling='uterine tube, Fallopian tube', meaning='자궁관, 난관', abbreviation=NULL, deleted_at=NULL WHERE id=855;
UPDATE IGNORE term_progresses SET term_id=855 WHERE term_id IN (936);
DELETE FROM term_progresses WHERE term_id IN (936);
UPDATE IGNORE bookmarks SET term_id=855 WHERE term_id IN (936);
DELETE FROM bookmarks WHERE term_id IN (936);
UPDATE IGNORE wrong_answer_terms SET term_id=855 WHERE term_id IN (936);
DELETE FROM wrong_answer_terms WHERE term_id IN (936);
UPDATE IGNORE custom_note_terms SET term_id=855 WHERE term_id IN (936);
DELETE FROM custom_note_terms WHERE term_id IN (936);
UPDATE IGNORE glossary_terms SET term_id=855 WHERE term_id IN (936);
DELETE FROM glossary_terms WHERE term_id IN (936);
UPDATE IGNORE term_morphemes SET term_id=855 WHERE term_id IN (936);
DELETE FROM term_morphemes WHERE term_id IN (936);
UPDATE IGNORE user_term_memos SET term_id=855 WHERE term_id IN (936);
DELETE FROM user_term_memos WHERE term_id IN (936);
UPDATE terms SET deleted_at=NOW() WHERE id IN (936) AND deleted_at IS NULL;

-- 그룹 64 대표 601 병합 1172
UPDATE terms SET spelling='jaundice', meaning='황달', abbreviation=NULL, deleted_at=NULL WHERE id=601;
UPDATE IGNORE term_progresses SET term_id=601 WHERE term_id IN (1172);
DELETE FROM term_progresses WHERE term_id IN (1172);
UPDATE IGNORE bookmarks SET term_id=601 WHERE term_id IN (1172);
DELETE FROM bookmarks WHERE term_id IN (1172);
UPDATE IGNORE wrong_answer_terms SET term_id=601 WHERE term_id IN (1172);
DELETE FROM wrong_answer_terms WHERE term_id IN (1172);
UPDATE IGNORE custom_note_terms SET term_id=601 WHERE term_id IN (1172);
DELETE FROM custom_note_terms WHERE term_id IN (1172);
UPDATE IGNORE glossary_terms SET term_id=601 WHERE term_id IN (1172);
DELETE FROM glossary_terms WHERE term_id IN (1172);
UPDATE IGNORE term_morphemes SET term_id=601 WHERE term_id IN (1172);
DELETE FROM term_morphemes WHERE term_id IN (1172);
UPDATE IGNORE user_term_memos SET term_id=601 WHERE term_id IN (1172);
DELETE FROM user_term_memos WHERE term_id IN (1172);
UPDATE terms SET deleted_at=NOW() WHERE id IN (1172) AND deleted_at IS NULL;

-- 그룹 65 대표 2 병합 525
UPDATE terms SET spelling='acute respiratory distress syndrome', meaning='급성호흡곤란증후군', abbreviation='ARDS', deleted_at=NULL WHERE id=2;
UPDATE IGNORE term_progresses SET term_id=2 WHERE term_id IN (525);
DELETE FROM term_progresses WHERE term_id IN (525);
UPDATE IGNORE bookmarks SET term_id=2 WHERE term_id IN (525);
DELETE FROM bookmarks WHERE term_id IN (525);
UPDATE IGNORE wrong_answer_terms SET term_id=2 WHERE term_id IN (525);
DELETE FROM wrong_answer_terms WHERE term_id IN (525);
UPDATE IGNORE custom_note_terms SET term_id=2 WHERE term_id IN (525);
DELETE FROM custom_note_terms WHERE term_id IN (525);
UPDATE IGNORE glossary_terms SET term_id=2 WHERE term_id IN (525);
DELETE FROM glossary_terms WHERE term_id IN (525);
UPDATE IGNORE term_morphemes SET term_id=2 WHERE term_id IN (525);
DELETE FROM term_morphemes WHERE term_id IN (525);
UPDATE IGNORE user_term_memos SET term_id=2 WHERE term_id IN (525);
DELETE FROM user_term_memos WHERE term_id IN (525);
UPDATE terms SET deleted_at=NOW() WHERE id IN (525) AND deleted_at IS NULL;

-- 그룹 66 대표 17 병합 218
UPDATE terms SET spelling='benign prostatic hyperplasia', meaning='전립선비대증, 양성 전립선비대증', abbreviation='BPH', deleted_at=NULL WHERE id=17;
UPDATE IGNORE term_progresses SET term_id=17 WHERE term_id IN (218);
DELETE FROM term_progresses WHERE term_id IN (218);
UPDATE IGNORE bookmarks SET term_id=17 WHERE term_id IN (218);
DELETE FROM bookmarks WHERE term_id IN (218);
UPDATE IGNORE wrong_answer_terms SET term_id=17 WHERE term_id IN (218);
DELETE FROM wrong_answer_terms WHERE term_id IN (218);
UPDATE IGNORE custom_note_terms SET term_id=17 WHERE term_id IN (218);
DELETE FROM custom_note_terms WHERE term_id IN (218);
UPDATE IGNORE glossary_terms SET term_id=17 WHERE term_id IN (218);
DELETE FROM glossary_terms WHERE term_id IN (218);
UPDATE IGNORE term_morphemes SET term_id=17 WHERE term_id IN (218);
DELETE FROM term_morphemes WHERE term_id IN (218);
UPDATE IGNORE user_term_memos SET term_id=17 WHERE term_id IN (218);
DELETE FROM user_term_memos WHERE term_id IN (218);
UPDATE terms SET deleted_at=NOW() WHERE id IN (218) AND deleted_at IS NULL;

-- 그룹 67 대표 325 병합 1010
UPDATE terms SET spelling='external ventricular drainage', meaning='뇌실외 배액술, 체외 뇌실 배액술', abbreviation='EVD', deleted_at=NULL WHERE id=325;
UPDATE IGNORE term_progresses SET term_id=325 WHERE term_id IN (1010);
DELETE FROM term_progresses WHERE term_id IN (1010);
UPDATE IGNORE bookmarks SET term_id=325 WHERE term_id IN (1010);
DELETE FROM bookmarks WHERE term_id IN (1010);
UPDATE IGNORE wrong_answer_terms SET term_id=325 WHERE term_id IN (1010);
DELETE FROM wrong_answer_terms WHERE term_id IN (1010);
UPDATE IGNORE custom_note_terms SET term_id=325 WHERE term_id IN (1010);
DELETE FROM custom_note_terms WHERE term_id IN (1010);
UPDATE IGNORE glossary_terms SET term_id=325 WHERE term_id IN (1010);
DELETE FROM glossary_terms WHERE term_id IN (1010);
UPDATE IGNORE term_morphemes SET term_id=325 WHERE term_id IN (1010);
DELETE FROM term_morphemes WHERE term_id IN (1010);
UPDATE IGNORE user_term_memos SET term_id=325 WHERE term_id IN (1010);
DELETE FROM user_term_memos WHERE term_id IN (1010);
UPDATE terms SET deleted_at=NOW() WHERE id IN (1010) AND deleted_at IS NULL;

-- 그룹 68 대표 353 병합 727
UPDATE terms SET spelling='partial pressure of oxygen in arterial blood', meaning='동맥혈 산소분압', abbreviation='PaO2', deleted_at=NULL WHERE id=353;
UPDATE IGNORE term_progresses SET term_id=353 WHERE term_id IN (727);
DELETE FROM term_progresses WHERE term_id IN (727);
UPDATE IGNORE bookmarks SET term_id=353 WHERE term_id IN (727);
DELETE FROM bookmarks WHERE term_id IN (727);
UPDATE IGNORE wrong_answer_terms SET term_id=353 WHERE term_id IN (727);
DELETE FROM wrong_answer_terms WHERE term_id IN (727);
UPDATE IGNORE custom_note_terms SET term_id=353 WHERE term_id IN (727);
DELETE FROM custom_note_terms WHERE term_id IN (727);
UPDATE IGNORE glossary_terms SET term_id=353 WHERE term_id IN (727);
DELETE FROM glossary_terms WHERE term_id IN (727);
UPDATE IGNORE term_morphemes SET term_id=353 WHERE term_id IN (727);
DELETE FROM term_morphemes WHERE term_id IN (727);
UPDATE IGNORE user_term_memos SET term_id=353 WHERE term_id IN (727);
DELETE FROM user_term_memos WHERE term_id IN (727);
UPDATE terms SET deleted_at=NOW() WHERE id IN (727) AND deleted_at IS NULL;
