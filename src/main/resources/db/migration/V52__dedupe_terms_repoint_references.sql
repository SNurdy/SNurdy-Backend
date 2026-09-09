-- V52__dedupe_terms_repoint_references.sql
-- Phase 4 F: 중복 term 병합. 비-canonical 참조를 canonical 로 repoint 후 소프트삭제
-- canonical=그룹 최소 id (dev prod 수렴). PK충돌은 UPDATE IGNORE + DELETE 로 canonical 우선 병합
-- 동의어 3건 되살리기 + TIA 의미 확정 포함

-- [2] acute respiratory distress syndrome / 급성호흡곤란증후군  <= dup 211,284
UPDATE terms SET spelling='acute respiratory distress syndrome', meaning='급성호흡곤란증후군', abbreviation='ARDS', explanation='중증 감염이나 외상 후 폐포가 극심하게 손상되어 발생하는 심각한 호흡 부전입니다.', deleted_at=NULL WHERE id=2;
UPDATE IGNORE term_progresses SET term_id=2 WHERE term_id IN (211,284);
DELETE FROM term_progresses WHERE term_id IN (211,284);
UPDATE IGNORE bookmarks SET term_id=2 WHERE term_id IN (211,284);
DELETE FROM bookmarks WHERE term_id IN (211,284);
UPDATE IGNORE wrong_answer_terms SET term_id=2 WHERE term_id IN (211,284);
DELETE FROM wrong_answer_terms WHERE term_id IN (211,284);
UPDATE IGNORE custom_note_terms SET term_id=2 WHERE term_id IN (211,284);
DELETE FROM custom_note_terms WHERE term_id IN (211,284);
UPDATE IGNORE glossary_terms SET term_id=2 WHERE term_id IN (211,284);
DELETE FROM glossary_terms WHERE term_id IN (211,284);
UPDATE IGNORE term_morphemes SET term_id=2 WHERE term_id IN (211,284);
DELETE FROM term_morphemes WHERE term_id IN (211,284);
UPDATE terms SET deleted_at=NOW() WHERE id IN (211,284) AND deleted_at IS NULL;

-- [4] alert / 명료  <= dup 114
UPDATE terms SET spelling='alert', meaning='명료', abbreviation='', explanation='환자가 깨어 있으며, 주변 환경과 자극에 정상적으로 반응하는 의식 상태.', deleted_at=NULL WHERE id=4;
UPDATE IGNORE term_progresses SET term_id=4 WHERE term_id IN (114);
DELETE FROM term_progresses WHERE term_id IN (114);
UPDATE IGNORE bookmarks SET term_id=4 WHERE term_id IN (114);
DELETE FROM bookmarks WHERE term_id IN (114);
UPDATE IGNORE wrong_answer_terms SET term_id=4 WHERE term_id IN (114);
DELETE FROM wrong_answer_terms WHERE term_id IN (114);
UPDATE IGNORE custom_note_terms SET term_id=4 WHERE term_id IN (114);
DELETE FROM custom_note_terms WHERE term_id IN (114);
UPDATE IGNORE glossary_terms SET term_id=4 WHERE term_id IN (114);
DELETE FROM glossary_terms WHERE term_id IN (114);
UPDATE IGNORE term_morphemes SET term_id=4 WHERE term_id IN (114);
DELETE FROM term_morphemes WHERE term_id IN (114);
UPDATE terms SET deleted_at=NOW() WHERE id IN (114) AND deleted_at IS NULL;

-- [7] aphasia / 실어증  <= dup 290
UPDATE terms SET spelling='aphasia', meaning='실어증', abbreviation='', explanation='뇌 손상으로 인해 언어 이해나 표현 능력(말하기, 듣기, 읽기, 쓰기)이 저하 또는 상실된 상태.', deleted_at=NULL WHERE id=7;
UPDATE IGNORE term_progresses SET term_id=7 WHERE term_id IN (290);
DELETE FROM term_progresses WHERE term_id IN (290);
UPDATE IGNORE bookmarks SET term_id=7 WHERE term_id IN (290);
DELETE FROM bookmarks WHERE term_id IN (290);
UPDATE IGNORE wrong_answer_terms SET term_id=7 WHERE term_id IN (290);
DELETE FROM wrong_answer_terms WHERE term_id IN (290);
UPDATE IGNORE custom_note_terms SET term_id=7 WHERE term_id IN (290);
DELETE FROM custom_note_terms WHERE term_id IN (290);
UPDATE IGNORE glossary_terms SET term_id=7 WHERE term_id IN (290);
DELETE FROM glossary_terms WHERE term_id IN (290);
UPDATE IGNORE term_morphemes SET term_id=7 WHERE term_id IN (290);
DELETE FROM term_morphemes WHERE term_id IN (290);
UPDATE terms SET deleted_at=NOW() WHERE id IN (290) AND deleted_at IS NULL;

-- [8] apraxia / 행동불능증  <= dup 964
UPDATE terms SET spelling='apraxia', meaning='행동불능증', abbreviation='', explanation='운동 기능은 정상이지만 특정 목적의 행동을 수행하지 못하는 신경학적 장애.', deleted_at=NULL WHERE id=8;
UPDATE IGNORE term_progresses SET term_id=8 WHERE term_id IN (964);
DELETE FROM term_progresses WHERE term_id IN (964);
UPDATE IGNORE bookmarks SET term_id=8 WHERE term_id IN (964);
DELETE FROM bookmarks WHERE term_id IN (964);
UPDATE IGNORE wrong_answer_terms SET term_id=8 WHERE term_id IN (964);
DELETE FROM wrong_answer_terms WHERE term_id IN (964);
UPDATE IGNORE custom_note_terms SET term_id=8 WHERE term_id IN (964);
DELETE FROM custom_note_terms WHERE term_id IN (964);
UPDATE IGNORE glossary_terms SET term_id=8 WHERE term_id IN (964);
DELETE FROM glossary_terms WHERE term_id IN (964);
UPDATE IGNORE term_morphemes SET term_id=8 WHERE term_id IN (964);
DELETE FROM term_morphemes WHERE term_id IN (964);
UPDATE terms SET deleted_at=NOW() WHERE id IN (964) AND deleted_at IS NULL;

-- [9] arrhythmia / 부정맥  <= dup 655
UPDATE terms SET spelling='arrhythmia', meaning='부정맥', abbreviation='', explanation='심장 박동이 비정상적으로 빠르거나, 느리거나, 불규칙한 상태.', deleted_at=NULL WHERE id=9;
UPDATE IGNORE term_progresses SET term_id=9 WHERE term_id IN (655);
DELETE FROM term_progresses WHERE term_id IN (655);
UPDATE IGNORE bookmarks SET term_id=9 WHERE term_id IN (655);
DELETE FROM bookmarks WHERE term_id IN (655);
UPDATE IGNORE wrong_answer_terms SET term_id=9 WHERE term_id IN (655);
DELETE FROM wrong_answer_terms WHERE term_id IN (655);
UPDATE IGNORE custom_note_terms SET term_id=9 WHERE term_id IN (655);
DELETE FROM custom_note_terms WHERE term_id IN (655);
UPDATE IGNORE glossary_terms SET term_id=9 WHERE term_id IN (655);
DELETE FROM glossary_terms WHERE term_id IN (655);
UPDATE IGNORE term_morphemes SET term_id=9 WHERE term_id IN (655);
DELETE FROM term_morphemes WHERE term_id IN (655);
UPDATE terms SET deleted_at=NOW() WHERE id IN (655) AND deleted_at IS NULL;

-- [10] arthralgia / 관절통  <= dup 437
UPDATE terms SET spelling='arthralgia', meaning='관절통', abbreviation='', explanation='관절 부위에 발생하는 통증. 염증을 동반하지 않을 수 있음.', deleted_at=NULL WHERE id=10;
UPDATE IGNORE term_progresses SET term_id=10 WHERE term_id IN (437);
DELETE FROM term_progresses WHERE term_id IN (437);
UPDATE IGNORE bookmarks SET term_id=10 WHERE term_id IN (437);
DELETE FROM bookmarks WHERE term_id IN (437);
UPDATE IGNORE wrong_answer_terms SET term_id=10 WHERE term_id IN (437);
DELETE FROM wrong_answer_terms WHERE term_id IN (437);
UPDATE IGNORE custom_note_terms SET term_id=10 WHERE term_id IN (437);
DELETE FROM custom_note_terms WHERE term_id IN (437);
UPDATE IGNORE glossary_terms SET term_id=10 WHERE term_id IN (437);
DELETE FROM glossary_terms WHERE term_id IN (437);
UPDATE IGNORE term_morphemes SET term_id=10 WHERE term_id IN (437);
DELETE FROM term_morphemes WHERE term_id IN (437);
UPDATE terms SET deleted_at=NOW() WHERE id IN (437) AND deleted_at IS NULL;

-- [13] asthma / 천식  <= dup 506
UPDATE terms SET spelling='asthma', meaning='천식', abbreviation='', explanation='특정 자극에 의해 기도가 좁아져 호흡곤란, 기침, 천명음 등이 반복되는 만성 기도 염증 질환.', deleted_at=NULL WHERE id=13;
UPDATE IGNORE term_progresses SET term_id=13 WHERE term_id IN (506);
DELETE FROM term_progresses WHERE term_id IN (506);
UPDATE IGNORE bookmarks SET term_id=13 WHERE term_id IN (506);
DELETE FROM bookmarks WHERE term_id IN (506);
UPDATE IGNORE wrong_answer_terms SET term_id=13 WHERE term_id IN (506);
DELETE FROM wrong_answer_terms WHERE term_id IN (506);
UPDATE IGNORE custom_note_terms SET term_id=13 WHERE term_id IN (506);
DELETE FROM custom_note_terms WHERE term_id IN (506);
UPDATE IGNORE glossary_terms SET term_id=13 WHERE term_id IN (506);
DELETE FROM glossary_terms WHERE term_id IN (506);
UPDATE IGNORE term_morphemes SET term_id=13 WHERE term_id IN (506);
DELETE FROM term_morphemes WHERE term_id IN (506);
UPDATE terms SET deleted_at=NOW() WHERE id IN (506) AND deleted_at IS NULL;

-- [14] atelectasis / 무기폐  <= dup 296,509
UPDATE terms SET spelling='atelectasis', meaning='무기폐', abbreviation='', explanation='기관지가 막혀 폐포가 팽창하지 못하고 공기 없이 쪼그라든 상태입니다.', deleted_at=NULL WHERE id=14;
UPDATE IGNORE term_progresses SET term_id=14 WHERE term_id IN (296,509);
DELETE FROM term_progresses WHERE term_id IN (296,509);
UPDATE IGNORE bookmarks SET term_id=14 WHERE term_id IN (296,509);
DELETE FROM bookmarks WHERE term_id IN (296,509);
UPDATE IGNORE wrong_answer_terms SET term_id=14 WHERE term_id IN (296,509);
DELETE FROM wrong_answer_terms WHERE term_id IN (296,509);
UPDATE IGNORE custom_note_terms SET term_id=14 WHERE term_id IN (296,509);
DELETE FROM custom_note_terms WHERE term_id IN (296,509);
UPDATE IGNORE glossary_terms SET term_id=14 WHERE term_id IN (296,509);
DELETE FROM glossary_terms WHERE term_id IN (296,509);
UPDATE IGNORE term_morphemes SET term_id=14 WHERE term_id IN (296,509);
DELETE FROM term_morphemes WHERE term_id IN (296,509);
UPDATE terms SET deleted_at=NOW() WHERE id IN (296,509) AND deleted_at IS NULL;

-- [20] catalepsy / 강직증  <= dup 1113
UPDATE terms SET spelling='catalepsy', meaning='강직증', abbreviation='', explanation='신체가 뻣뻣하게 굳어 특정 자세를 유지하며 외부 자극에 반응이 없는 상태.', deleted_at=NULL WHERE id=20;
UPDATE IGNORE term_progresses SET term_id=20 WHERE term_id IN (1113);
DELETE FROM term_progresses WHERE term_id IN (1113);
UPDATE IGNORE bookmarks SET term_id=20 WHERE term_id IN (1113);
DELETE FROM bookmarks WHERE term_id IN (1113);
UPDATE IGNORE wrong_answer_terms SET term_id=20 WHERE term_id IN (1113);
DELETE FROM wrong_answer_terms WHERE term_id IN (1113);
UPDATE IGNORE custom_note_terms SET term_id=20 WHERE term_id IN (1113);
DELETE FROM custom_note_terms WHERE term_id IN (1113);
UPDATE IGNORE glossary_terms SET term_id=20 WHERE term_id IN (1113);
DELETE FROM glossary_terms WHERE term_id IN (1113);
UPDATE IGNORE term_morphemes SET term_id=20 WHERE term_id IN (1113);
DELETE FROM term_morphemes WHERE term_id IN (1113);
UPDATE terms SET deleted_at=NOW() WHERE id IN (1113) AND deleted_at IS NULL;

-- [22] chronic obstructive pulmonary disease / 만성폐쇄성폐질환  <= dup 306,507
UPDATE terms SET spelling='chronic obstructive pulmonary disease', meaning='만성폐쇄성폐질환', abbreviation='COPD', explanation='유해한 입자나 가스 흡입으로 인해 폐에 만성적인 염증이 발생하여 기도와 폐 실질이 손상되고 공기 흐름이 제한되는 질환입니다.', deleted_at=NULL WHERE id=22;
UPDATE IGNORE term_progresses SET term_id=22 WHERE term_id IN (306,507);
DELETE FROM term_progresses WHERE term_id IN (306,507);
UPDATE IGNORE bookmarks SET term_id=22 WHERE term_id IN (306,507);
DELETE FROM bookmarks WHERE term_id IN (306,507);
UPDATE IGNORE wrong_answer_terms SET term_id=22 WHERE term_id IN (306,507);
DELETE FROM wrong_answer_terms WHERE term_id IN (306,507);
UPDATE IGNORE custom_note_terms SET term_id=22 WHERE term_id IN (306,507);
DELETE FROM custom_note_terms WHERE term_id IN (306,507);
UPDATE IGNORE glossary_terms SET term_id=22 WHERE term_id IN (306,507);
DELETE FROM glossary_terms WHERE term_id IN (306,507);
UPDATE IGNORE term_morphemes SET term_id=22 WHERE term_id IN (306,507);
DELETE FROM term_morphemes WHERE term_id IN (306,507);
UPDATE terms SET deleted_at=NOW() WHERE id IN (306,507) AND deleted_at IS NULL;

-- [24] coma / 혼수  <= dup 132
UPDATE terms SET spelling='coma', meaning='혼수', abbreviation='', explanation='외부 자극에 전혀 반응하지 못하고 깨울 수 없는 심각한 의식 불명 상태.', deleted_at=NULL WHERE id=24;
UPDATE IGNORE term_progresses SET term_id=24 WHERE term_id IN (132);
DELETE FROM term_progresses WHERE term_id IN (132);
UPDATE IGNORE bookmarks SET term_id=24 WHERE term_id IN (132);
DELETE FROM bookmarks WHERE term_id IN (132);
UPDATE IGNORE wrong_answer_terms SET term_id=24 WHERE term_id IN (132);
DELETE FROM wrong_answer_terms WHERE term_id IN (132);
UPDATE IGNORE custom_note_terms SET term_id=24 WHERE term_id IN (132);
DELETE FROM custom_note_terms WHERE term_id IN (132);
UPDATE IGNORE glossary_terms SET term_id=24 WHERE term_id IN (132);
DELETE FROM glossary_terms WHERE term_id IN (132);
UPDATE IGNORE term_morphemes SET term_id=24 WHERE term_id IN (132);
DELETE FROM term_morphemes WHERE term_id IN (132);
UPDATE terms SET deleted_at=NOW() WHERE id IN (132) AND deleted_at IS NULL;

-- [26] deep vein thrombosis / 심부정맥혈전증  <= dup 234,315,677
UPDATE terms SET spelling='deep vein thrombosis', meaning='심부정맥혈전증', abbreviation='DVT', explanation='주로 다리 깊은 정맥에 혈전(피떡)이 생기는 질환. 폐색전증 유발 가능.', deleted_at=NULL WHERE id=26;
UPDATE IGNORE term_progresses SET term_id=26 WHERE term_id IN (234,315,677);
DELETE FROM term_progresses WHERE term_id IN (234,315,677);
UPDATE IGNORE bookmarks SET term_id=26 WHERE term_id IN (234,315,677);
DELETE FROM bookmarks WHERE term_id IN (234,315,677);
UPDATE IGNORE wrong_answer_terms SET term_id=26 WHERE term_id IN (234,315,677);
DELETE FROM wrong_answer_terms WHERE term_id IN (234,315,677);
UPDATE IGNORE custom_note_terms SET term_id=26 WHERE term_id IN (234,315,677);
DELETE FROM custom_note_terms WHERE term_id IN (234,315,677);
UPDATE IGNORE glossary_terms SET term_id=26 WHERE term_id IN (234,315,677);
DELETE FROM glossary_terms WHERE term_id IN (234,315,677);
UPDATE IGNORE term_morphemes SET term_id=26 WHERE term_id IN (234,315,677);
DELETE FROM term_morphemes WHERE term_id IN (234,315,677);
UPDATE terms SET deleted_at=NOW() WHERE id IN (234,315,677) AND deleted_at IS NULL;

-- [28] delusion / 망상  <= dup 1066
UPDATE terms SET spelling='delusion', meaning='망상', abbreviation='', explanation='사실과 다른 잘못된 믿음을 견고하게 고수하며 교정되지 않는 사고 장애.', deleted_at=NULL WHERE id=28;
UPDATE IGNORE term_progresses SET term_id=28 WHERE term_id IN (1066);
DELETE FROM term_progresses WHERE term_id IN (1066);
UPDATE IGNORE bookmarks SET term_id=28 WHERE term_id IN (1066);
DELETE FROM bookmarks WHERE term_id IN (1066);
UPDATE IGNORE wrong_answer_terms SET term_id=28 WHERE term_id IN (1066);
DELETE FROM wrong_answer_terms WHERE term_id IN (1066);
UPDATE IGNORE custom_note_terms SET term_id=28 WHERE term_id IN (1066);
DELETE FROM custom_note_terms WHERE term_id IN (1066);
UPDATE IGNORE glossary_terms SET term_id=28 WHERE term_id IN (1066);
DELETE FROM glossary_terms WHERE term_id IN (1066);
UPDATE IGNORE term_morphemes SET term_id=28 WHERE term_id IN (1066);
DELETE FROM term_morphemes WHERE term_id IN (1066);
UPDATE terms SET deleted_at=NOW() WHERE id IN (1066) AND deleted_at IS NULL;

-- [30] depression / 우울  <= dup 1109
UPDATE terms SET spelling='depression', meaning='우울', abbreviation='', explanation='슬픔, 절망감, 흥미 상실 등이 지속되어 일상 기능에 영향을 미치는 기분 장애.', deleted_at=NULL WHERE id=30;
UPDATE IGNORE term_progresses SET term_id=30 WHERE term_id IN (1109);
DELETE FROM term_progresses WHERE term_id IN (1109);
UPDATE IGNORE bookmarks SET term_id=30 WHERE term_id IN (1109);
DELETE FROM bookmarks WHERE term_id IN (1109);
UPDATE IGNORE wrong_answer_terms SET term_id=30 WHERE term_id IN (1109);
DELETE FROM wrong_answer_terms WHERE term_id IN (1109);
UPDATE IGNORE custom_note_terms SET term_id=30 WHERE term_id IN (1109);
DELETE FROM custom_note_terms WHERE term_id IN (1109);
UPDATE IGNORE glossary_terms SET term_id=30 WHERE term_id IN (1109);
DELETE FROM glossary_terms WHERE term_id IN (1109);
UPDATE IGNORE term_morphemes SET term_id=30 WHERE term_id IN (1109);
DELETE FROM term_morphemes WHERE term_id IN (1109);
UPDATE terms SET deleted_at=NOW() WHERE id IN (1109) AND deleted_at IS NULL;

-- [39] epiglottitis / 후두개염  <= dup 147
UPDATE terms SET spelling='epiglottitis', meaning='후두개염', abbreviation='', explanation='후두덮개(음식물의 기도 유입 방지 연골)의 염증으로, 심하면 기도 폐쇄 유발.', deleted_at=NULL WHERE id=39;
UPDATE IGNORE term_progresses SET term_id=39 WHERE term_id IN (147);
DELETE FROM term_progresses WHERE term_id IN (147);
UPDATE IGNORE bookmarks SET term_id=39 WHERE term_id IN (147);
DELETE FROM bookmarks WHERE term_id IN (147);
UPDATE IGNORE wrong_answer_terms SET term_id=39 WHERE term_id IN (147);
DELETE FROM wrong_answer_terms WHERE term_id IN (147);
UPDATE IGNORE custom_note_terms SET term_id=39 WHERE term_id IN (147);
DELETE FROM custom_note_terms WHERE term_id IN (147);
UPDATE IGNORE glossary_terms SET term_id=39 WHERE term_id IN (147);
DELETE FROM glossary_terms WHERE term_id IN (147);
UPDATE IGNORE term_morphemes SET term_id=39 WHERE term_id IN (147);
DELETE FROM term_morphemes WHERE term_id IN (147);
UPDATE terms SET deleted_at=NOW() WHERE id IN (147) AND deleted_at IS NULL;

-- [44] gastric ulcer / 위궤양  <= dup 608
UPDATE terms SET spelling='gastric ulcer', meaning='위궤양', abbreviation='', explanation='위 점막이 손상되어 움푹 패인 상태. 속쓰림, 복통 등 유발.', deleted_at=NULL WHERE id=44;
UPDATE IGNORE term_progresses SET term_id=44 WHERE term_id IN (608);
DELETE FROM term_progresses WHERE term_id IN (608);
UPDATE IGNORE bookmarks SET term_id=44 WHERE term_id IN (608);
DELETE FROM bookmarks WHERE term_id IN (608);
UPDATE IGNORE wrong_answer_terms SET term_id=44 WHERE term_id IN (608);
DELETE FROM wrong_answer_terms WHERE term_id IN (608);
UPDATE IGNORE custom_note_terms SET term_id=44 WHERE term_id IN (608);
DELETE FROM custom_note_terms WHERE term_id IN (608);
UPDATE IGNORE glossary_terms SET term_id=44 WHERE term_id IN (608);
DELETE FROM glossary_terms WHERE term_id IN (608);
UPDATE IGNORE term_morphemes SET term_id=44 WHERE term_id IN (608);
DELETE FROM term_morphemes WHERE term_id IN (608);
UPDATE terms SET deleted_at=NOW() WHERE id IN (608) AND deleted_at IS NULL;

-- [45] gastroesophageal reflux disease / 위식도역류질환  <= dup 246,610
UPDATE terms SET spelling='gastroesophageal reflux disease', meaning='위식도역류질환', abbreviation='GERD', explanation='위산이나 위 내용물이 식도로 역류하여 가슴쓰림, 신물 등의 증상을 유발하는 질환.', deleted_at=NULL WHERE id=45;
UPDATE IGNORE term_progresses SET term_id=45 WHERE term_id IN (246,610);
DELETE FROM term_progresses WHERE term_id IN (246,610);
UPDATE IGNORE bookmarks SET term_id=45 WHERE term_id IN (246,610);
DELETE FROM bookmarks WHERE term_id IN (246,610);
UPDATE IGNORE wrong_answer_terms SET term_id=45 WHERE term_id IN (246,610);
DELETE FROM wrong_answer_terms WHERE term_id IN (246,610);
UPDATE IGNORE custom_note_terms SET term_id=45 WHERE term_id IN (246,610);
DELETE FROM custom_note_terms WHERE term_id IN (246,610);
UPDATE IGNORE glossary_terms SET term_id=45 WHERE term_id IN (246,610);
DELETE FROM glossary_terms WHERE term_id IN (246,610);
UPDATE IGNORE term_morphemes SET term_id=45 WHERE term_id IN (246,610);
DELETE FROM term_morphemes WHERE term_id IN (246,610);
UPDATE terms SET deleted_at=NOW() WHERE id IN (246,610) AND deleted_at IS NULL;

-- [48] gout / 통풍  <= dup 396
UPDATE terms SET spelling='gout', meaning='통풍', abbreviation='', explanation='혈액 내 요산 수치 상승으로 요산 결정이 관절 등에 쌓여 급성 염증과 통증을 유발하는 대사성 질환.', deleted_at=NULL WHERE id=48;
UPDATE IGNORE term_progresses SET term_id=48 WHERE term_id IN (396);
DELETE FROM term_progresses WHERE term_id IN (396);
UPDATE IGNORE bookmarks SET term_id=48 WHERE term_id IN (396);
DELETE FROM bookmarks WHERE term_id IN (396);
UPDATE IGNORE wrong_answer_terms SET term_id=48 WHERE term_id IN (396);
DELETE FROM wrong_answer_terms WHERE term_id IN (396);
UPDATE IGNORE custom_note_terms SET term_id=48 WHERE term_id IN (396);
DELETE FROM custom_note_terms WHERE term_id IN (396);
UPDATE IGNORE glossary_terms SET term_id=48 WHERE term_id IN (396);
DELETE FROM glossary_terms WHERE term_id IN (396);
UPDATE IGNORE term_morphemes SET term_id=48 WHERE term_id IN (396);
DELETE FROM term_morphemes WHERE term_id IN (396);
UPDATE terms SET deleted_at=NOW() WHERE id IN (396) AND deleted_at IS NULL;

-- [70] osteoporosis / 골다공증  <= dup 403
UPDATE terms SET spelling='osteoporosis', meaning='골다공증', abbreviation='', explanation='뼈의 양이 감소하고 질이 약해져서 작은 충격에도 쉽게 골절이 일어나는 상태입니다.', deleted_at=NULL WHERE id=70;
UPDATE IGNORE term_progresses SET term_id=70 WHERE term_id IN (403);
DELETE FROM term_progresses WHERE term_id IN (403);
UPDATE IGNORE bookmarks SET term_id=70 WHERE term_id IN (403);
DELETE FROM bookmarks WHERE term_id IN (403);
UPDATE IGNORE wrong_answer_terms SET term_id=70 WHERE term_id IN (403);
DELETE FROM wrong_answer_terms WHERE term_id IN (403);
UPDATE IGNORE custom_note_terms SET term_id=70 WHERE term_id IN (403);
DELETE FROM custom_note_terms WHERE term_id IN (403);
UPDATE IGNORE glossary_terms SET term_id=70 WHERE term_id IN (403);
DELETE FROM glossary_terms WHERE term_id IN (403);
UPDATE IGNORE term_morphemes SET term_id=70 WHERE term_id IN (403);
DELETE FROM term_morphemes WHERE term_id IN (403);
UPDATE terms SET deleted_at=NOW() WHERE id IN (403) AND deleted_at IS NULL;

-- [76] pneumonia / 폐렴  <= dup 511
UPDATE terms SET spelling='pneumonia', meaning='폐렴', abbreviation='', explanation='세균, 바이러스, 곰팡이 등의 감염으로 폐 조직에 염증이 발생하는 질환.', deleted_at=NULL WHERE id=76;
UPDATE IGNORE term_progresses SET term_id=76 WHERE term_id IN (511);
DELETE FROM term_progresses WHERE term_id IN (511);
UPDATE IGNORE bookmarks SET term_id=76 WHERE term_id IN (511);
DELETE FROM bookmarks WHERE term_id IN (511);
UPDATE IGNORE wrong_answer_terms SET term_id=76 WHERE term_id IN (511);
DELETE FROM wrong_answer_terms WHERE term_id IN (511);
UPDATE IGNORE custom_note_terms SET term_id=76 WHERE term_id IN (511);
DELETE FROM custom_note_terms WHERE term_id IN (511);
UPDATE IGNORE glossary_terms SET term_id=76 WHERE term_id IN (511);
DELETE FROM glossary_terms WHERE term_id IN (511);
UPDATE IGNORE term_morphemes SET term_id=76 WHERE term_id IN (511);
DELETE FROM term_morphemes WHERE term_id IN (511);
UPDATE terms SET deleted_at=NOW() WHERE id IN (511) AND deleted_at IS NULL;

-- [84] quadriplegia / 사지마비  <= dup 366
UPDATE terms SET spelling='quadriplegia', meaning='사지마비', abbreviation='', explanation='목 부위 척수 손상 등으로 팔, 다리, 몸통 전체에 마비가 온 상태.', deleted_at=NULL WHERE id=84;
UPDATE IGNORE term_progresses SET term_id=84 WHERE term_id IN (366);
DELETE FROM term_progresses WHERE term_id IN (366);
UPDATE IGNORE bookmarks SET term_id=84 WHERE term_id IN (366);
DELETE FROM bookmarks WHERE term_id IN (366);
UPDATE IGNORE wrong_answer_terms SET term_id=84 WHERE term_id IN (366);
DELETE FROM wrong_answer_terms WHERE term_id IN (366);
UPDATE IGNORE custom_note_terms SET term_id=84 WHERE term_id IN (366);
DELETE FROM custom_note_terms WHERE term_id IN (366);
UPDATE IGNORE glossary_terms SET term_id=84 WHERE term_id IN (366);
DELETE FROM glossary_terms WHERE term_id IN (366);
UPDATE IGNORE term_morphemes SET term_id=84 WHERE term_id IN (366);
DELETE FROM term_morphemes WHERE term_id IN (366);
UPDATE terms SET deleted_at=NOW() WHERE id IN (366) AND deleted_at IS NULL;

-- [85] range of motion / 관절가동범위  <= dup 444
UPDATE terms SET spelling='range of motion', meaning='관절가동범위', abbreviation='ROM', explanation='각 관절이 정상적으로 움직일 수 있는 최대한의 각도나 범위.', deleted_at=NULL WHERE id=85;
UPDATE IGNORE term_progresses SET term_id=85 WHERE term_id IN (444);
DELETE FROM term_progresses WHERE term_id IN (444);
UPDATE IGNORE bookmarks SET term_id=85 WHERE term_id IN (444);
DELETE FROM bookmarks WHERE term_id IN (444);
UPDATE IGNORE wrong_answer_terms SET term_id=85 WHERE term_id IN (444);
DELETE FROM wrong_answer_terms WHERE term_id IN (444);
UPDATE IGNORE custom_note_terms SET term_id=85 WHERE term_id IN (444);
DELETE FROM custom_note_terms WHERE term_id IN (444);
UPDATE IGNORE glossary_terms SET term_id=85 WHERE term_id IN (444);
DELETE FROM glossary_terms WHERE term_id IN (444);
UPDATE IGNORE term_morphemes SET term_id=85 WHERE term_id IN (444);
DELETE FROM term_morphemes WHERE term_id IN (444);
UPDATE terms SET deleted_at=NOW() WHERE id IN (444) AND deleted_at IS NULL;

-- [86] rheumatoid arthritis / 류마티스성 관절염  <= dup 433
UPDATE terms SET spelling='rheumatoid arthritis', meaning='류마티스성 관절염', abbreviation='RA', explanation='관절 활막의 만성 염증을 특징으로 하는 자가면역질환. 관절 변형 및 기능 장애 유발.', deleted_at=NULL WHERE id=86;
UPDATE IGNORE term_progresses SET term_id=86 WHERE term_id IN (433);
DELETE FROM term_progresses WHERE term_id IN (433);
UPDATE IGNORE bookmarks SET term_id=86 WHERE term_id IN (433);
DELETE FROM bookmarks WHERE term_id IN (433);
UPDATE IGNORE wrong_answer_terms SET term_id=86 WHERE term_id IN (433);
DELETE FROM wrong_answer_terms WHERE term_id IN (433);
UPDATE IGNORE custom_note_terms SET term_id=86 WHERE term_id IN (433);
DELETE FROM custom_note_terms WHERE term_id IN (433);
UPDATE IGNORE glossary_terms SET term_id=86 WHERE term_id IN (433);
DELETE FROM glossary_terms WHERE term_id IN (433);
UPDATE IGNORE term_morphemes SET term_id=86 WHERE term_id IN (433);
DELETE FROM term_morphemes WHERE term_id IN (433);
UPDATE terms SET deleted_at=NOW() WHERE id IN (433) AND deleted_at IS NULL;

-- [88] semicoma / 반혼수  <= dup 181
UPDATE terms SET spelling='semicoma', meaning='반혼수', abbreviation='', explanation='강한 자극에만 약간 반응하는 깊은 의식 저하 상태. 혼수보다 얕은 단계.', deleted_at=NULL WHERE id=88;
UPDATE IGNORE term_progresses SET term_id=88 WHERE term_id IN (181);
DELETE FROM term_progresses WHERE term_id IN (181);
UPDATE IGNORE bookmarks SET term_id=88 WHERE term_id IN (181);
DELETE FROM bookmarks WHERE term_id IN (181);
UPDATE IGNORE wrong_answer_terms SET term_id=88 WHERE term_id IN (181);
DELETE FROM wrong_answer_terms WHERE term_id IN (181);
UPDATE IGNORE custom_note_terms SET term_id=88 WHERE term_id IN (181);
DELETE FROM custom_note_terms WHERE term_id IN (181);
UPDATE IGNORE glossary_terms SET term_id=88 WHERE term_id IN (181);
DELETE FROM glossary_terms WHERE term_id IN (181);
UPDATE IGNORE term_morphemes SET term_id=88 WHERE term_id IN (181);
DELETE FROM term_morphemes WHERE term_id IN (181);
UPDATE terms SET deleted_at=NOW() WHERE id IN (181) AND deleted_at IS NULL;

-- [97] stroke / 뇌졸중  <= dup 187
UPDATE terms SET spelling='stroke', meaning='뇌졸중', abbreviation='', explanation='뇌혈관 문제(막힘 또는 터짐)로 뇌 기능이 손상되어 신경학적 증상을 유발하는 질환.', deleted_at=NULL WHERE id=97;
UPDATE IGNORE term_progresses SET term_id=97 WHERE term_id IN (187);
DELETE FROM term_progresses WHERE term_id IN (187);
UPDATE IGNORE bookmarks SET term_id=97 WHERE term_id IN (187);
DELETE FROM bookmarks WHERE term_id IN (187);
UPDATE IGNORE wrong_answer_terms SET term_id=97 WHERE term_id IN (187);
DELETE FROM wrong_answer_terms WHERE term_id IN (187);
UPDATE IGNORE custom_note_terms SET term_id=97 WHERE term_id IN (187);
DELETE FROM custom_note_terms WHERE term_id IN (187);
UPDATE IGNORE glossary_terms SET term_id=97 WHERE term_id IN (187);
DELETE FROM glossary_terms WHERE term_id IN (187);
UPDATE IGNORE term_morphemes SET term_id=97 WHERE term_id IN (187);
DELETE FROM term_morphemes WHERE term_id IN (187);
UPDATE terms SET deleted_at=NOW() WHERE id IN (187) AND deleted_at IS NULL;

-- [98] stupor / 혼미  <= dup 188
UPDATE terms SET spelling='stupor', meaning='혼미', abbreviation='', explanation='강한 통증 자극에만 겨우 반응하고 자발적 움직임이 거의 없는 심각한 의식 저하 상태.', deleted_at=NULL WHERE id=98;
UPDATE IGNORE term_progresses SET term_id=98 WHERE term_id IN (188);
DELETE FROM term_progresses WHERE term_id IN (188);
UPDATE IGNORE bookmarks SET term_id=98 WHERE term_id IN (188);
DELETE FROM bookmarks WHERE term_id IN (188);
UPDATE IGNORE wrong_answer_terms SET term_id=98 WHERE term_id IN (188);
DELETE FROM wrong_answer_terms WHERE term_id IN (188);
UPDATE IGNORE custom_note_terms SET term_id=98 WHERE term_id IN (188);
DELETE FROM custom_note_terms WHERE term_id IN (188);
UPDATE IGNORE glossary_terms SET term_id=98 WHERE term_id IN (188);
DELETE FROM glossary_terms WHERE term_id IN (188);
UPDATE IGNORE term_morphemes SET term_id=98 WHERE term_id IN (188);
DELETE FROM term_morphemes WHERE term_id IN (188);
UPDATE terms SET deleted_at=NOW() WHERE id IN (188) AND deleted_at IS NULL;

-- [107] vertigo / 현기증  <= dup 139
UPDATE terms SET spelling='vertigo', meaning='현기증', abbreviation='', explanation='자신이나 주변 환경이 움직이거나 회전하는 듯한 느낌을 받는 어지럼증의 한 종류.', deleted_at=NULL WHERE id=107;
UPDATE IGNORE term_progresses SET term_id=107 WHERE term_id IN (139);
DELETE FROM term_progresses WHERE term_id IN (139);
UPDATE IGNORE bookmarks SET term_id=107 WHERE term_id IN (139);
DELETE FROM bookmarks WHERE term_id IN (139);
UPDATE IGNORE wrong_answer_terms SET term_id=107 WHERE term_id IN (139);
DELETE FROM wrong_answer_terms WHERE term_id IN (139);
UPDATE IGNORE custom_note_terms SET term_id=107 WHERE term_id IN (139);
DELETE FROM custom_note_terms WHERE term_id IN (139);
UPDATE IGNORE glossary_terms SET term_id=107 WHERE term_id IN (139);
DELETE FROM glossary_terms WHERE term_id IN (139);
UPDATE IGNORE term_morphemes SET term_id=107 WHERE term_id IN (139);
DELETE FROM term_morphemes WHERE term_id IN (139);
UPDATE terms SET deleted_at=NOW() WHERE id IN (139) AND deleted_at IS NULL;

-- [115] amputation / 절단  <= dup 389
UPDATE terms SET spelling='amputation', meaning='절단', abbreviation='', explanation='질병이나 외상으로 인해 신체의 일부(주로 팔, 다리)를 외과적으로 제거하는 것입니다.', deleted_at=NULL WHERE id=115;
UPDATE IGNORE term_progresses SET term_id=115 WHERE term_id IN (389);
DELETE FROM term_progresses WHERE term_id IN (389);
UPDATE IGNORE bookmarks SET term_id=115 WHERE term_id IN (389);
DELETE FROM bookmarks WHERE term_id IN (389);
UPDATE IGNORE wrong_answer_terms SET term_id=115 WHERE term_id IN (389);
DELETE FROM wrong_answer_terms WHERE term_id IN (389);
UPDATE IGNORE custom_note_terms SET term_id=115 WHERE term_id IN (389);
DELETE FROM custom_note_terms WHERE term_id IN (389);
UPDATE IGNORE glossary_terms SET term_id=115 WHERE term_id IN (389);
DELETE FROM glossary_terms WHERE term_id IN (389);
UPDATE IGNORE term_morphemes SET term_id=115 WHERE term_id IN (389);
DELETE FROM term_morphemes WHERE term_id IN (389);
UPDATE terms SET deleted_at=NOW() WHERE id IN (389) AND deleted_at IS NULL;

-- [116] angina pectoris / 협심증  <= dup 724
UPDATE terms SET spelling='angina pectoris', meaning='협심증', abbreviation='', explanation='관상동맥이 좁아져 심장 근육에 산소 공급이 부족해지면서 발생하는 가슴 통증입니다.', deleted_at=NULL WHERE id=116;
UPDATE IGNORE term_progresses SET term_id=116 WHERE term_id IN (724);
DELETE FROM term_progresses WHERE term_id IN (724);
UPDATE IGNORE bookmarks SET term_id=116 WHERE term_id IN (724);
DELETE FROM bookmarks WHERE term_id IN (724);
UPDATE IGNORE wrong_answer_terms SET term_id=116 WHERE term_id IN (724);
DELETE FROM wrong_answer_terms WHERE term_id IN (724);
UPDATE IGNORE custom_note_terms SET term_id=116 WHERE term_id IN (724);
DELETE FROM custom_note_terms WHERE term_id IN (724);
UPDATE IGNORE glossary_terms SET term_id=116 WHERE term_id IN (724);
DELETE FROM glossary_terms WHERE term_id IN (724);
UPDATE IGNORE term_morphemes SET term_id=116 WHERE term_id IN (724);
DELETE FROM term_morphemes WHERE term_id IN (724);
UPDATE terms SET deleted_at=NOW() WHERE id IN (724) AND deleted_at IS NULL;

-- [119] appendicitis / 충수염  <= dup 617
UPDATE terms SET spelling='appendicitis', meaning='충수염', abbreviation='', explanation='충수(맹장 끝부분)에 발생하는 염증 및 감염.', deleted_at=NULL WHERE id=119;
UPDATE IGNORE term_progresses SET term_id=119 WHERE term_id IN (617);
DELETE FROM term_progresses WHERE term_id IN (617);
UPDATE IGNORE bookmarks SET term_id=119 WHERE term_id IN (617);
DELETE FROM bookmarks WHERE term_id IN (617);
UPDATE IGNORE wrong_answer_terms SET term_id=119 WHERE term_id IN (617);
DELETE FROM wrong_answer_terms WHERE term_id IN (617);
UPDATE IGNORE custom_note_terms SET term_id=119 WHERE term_id IN (617);
DELETE FROM custom_note_terms WHERE term_id IN (617);
UPDATE IGNORE glossary_terms SET term_id=119 WHERE term_id IN (617);
DELETE FROM glossary_terms WHERE term_id IN (617);
UPDATE IGNORE term_morphemes SET term_id=119 WHERE term_id IN (617);
DELETE FROM term_morphemes WHERE term_id IN (617);
UPDATE terms SET deleted_at=NOW() WHERE id IN (617) AND deleted_at IS NULL;

-- [124] cardiac arrest / 심정지  <= dup 664
UPDATE terms SET spelling='cardiac arrest', meaning='심정지', abbreviation='', explanation='심장 기능이 갑자기 정지하여 혈액 순환이 멈춘 상태.', deleted_at=NULL WHERE id=124;
UPDATE IGNORE term_progresses SET term_id=124 WHERE term_id IN (664);
DELETE FROM term_progresses WHERE term_id IN (664);
UPDATE IGNORE bookmarks SET term_id=124 WHERE term_id IN (664);
DELETE FROM bookmarks WHERE term_id IN (664);
UPDATE IGNORE wrong_answer_terms SET term_id=124 WHERE term_id IN (664);
DELETE FROM wrong_answer_terms WHERE term_id IN (664);
UPDATE IGNORE custom_note_terms SET term_id=124 WHERE term_id IN (664);
DELETE FROM custom_note_terms WHERE term_id IN (664);
UPDATE IGNORE glossary_terms SET term_id=124 WHERE term_id IN (664);
DELETE FROM glossary_terms WHERE term_id IN (664);
UPDATE IGNORE term_morphemes SET term_id=124 WHERE term_id IN (664);
DELETE FROM term_morphemes WHERE term_id IN (664);
UPDATE terms SET deleted_at=NOW() WHERE id IN (664) AND deleted_at IS NULL;

-- [137] defibrillation / 제세동  <= dup 750
UPDATE terms SET spelling='defibrillation', meaning='제세동', abbreviation='', explanation='심실세동 등 치명적 부정맥 발생 시 전기 충격을 가해 심장 박동을 정상화하는 응급 시술.', deleted_at=NULL WHERE id=137;
UPDATE IGNORE term_progresses SET term_id=137 WHERE term_id IN (750);
DELETE FROM term_progresses WHERE term_id IN (750);
UPDATE IGNORE bookmarks SET term_id=137 WHERE term_id IN (750);
DELETE FROM bookmarks WHERE term_id IN (750);
UPDATE IGNORE wrong_answer_terms SET term_id=137 WHERE term_id IN (750);
DELETE FROM wrong_answer_terms WHERE term_id IN (750);
UPDATE IGNORE custom_note_terms SET term_id=137 WHERE term_id IN (750);
DELETE FROM custom_note_terms WHERE term_id IN (750);
UPDATE IGNORE glossary_terms SET term_id=137 WHERE term_id IN (750);
DELETE FROM glossary_terms WHERE term_id IN (750);
UPDATE IGNORE term_morphemes SET term_id=137 WHERE term_id IN (750);
DELETE FROM term_morphemes WHERE term_id IN (750);
UPDATE terms SET deleted_at=NOW() WHERE id IN (750) AND deleted_at IS NULL;

-- [142] ectopic pregnancy / 자궁외임신  <= dup 849
UPDATE terms SET spelling='ectopic pregnancy', meaning='자궁외임신', abbreviation='', explanation='수정란이 자궁 내막이 아닌 다른 곳(주로 난관)에 착상한 임신.', deleted_at=NULL WHERE id=142;
UPDATE IGNORE term_progresses SET term_id=142 WHERE term_id IN (849);
DELETE FROM term_progresses WHERE term_id IN (849);
UPDATE IGNORE bookmarks SET term_id=142 WHERE term_id IN (849);
DELETE FROM bookmarks WHERE term_id IN (849);
UPDATE IGNORE wrong_answer_terms SET term_id=142 WHERE term_id IN (849);
DELETE FROM wrong_answer_terms WHERE term_id IN (849);
UPDATE IGNORE custom_note_terms SET term_id=142 WHERE term_id IN (849);
DELETE FROM custom_note_terms WHERE term_id IN (849);
UPDATE IGNORE glossary_terms SET term_id=142 WHERE term_id IN (849);
DELETE FROM glossary_terms WHERE term_id IN (849);
UPDATE IGNORE term_morphemes SET term_id=142 WHERE term_id IN (849);
DELETE FROM term_morphemes WHERE term_id IN (849);
UPDATE terms SET deleted_at=NOW() WHERE id IN (849) AND deleted_at IS NULL;

-- [153] gastric lavage / 위세척  <= dup 650
UPDATE terms SET spelling='gastric lavage', meaning='위세척', abbreviation='', explanation='독극물 섭취 등 응급 상황에서 위 내용물을 제거하기 위해 관을 삽입하여 세척하는 시술.', deleted_at=NULL WHERE id=153;
UPDATE IGNORE term_progresses SET term_id=153 WHERE term_id IN (650);
DELETE FROM term_progresses WHERE term_id IN (650);
UPDATE IGNORE bookmarks SET term_id=153 WHERE term_id IN (650);
DELETE FROM bookmarks WHERE term_id IN (650);
UPDATE IGNORE wrong_answer_terms SET term_id=153 WHERE term_id IN (650);
DELETE FROM wrong_answer_terms WHERE term_id IN (650);
UPDATE IGNORE custom_note_terms SET term_id=153 WHERE term_id IN (650);
DELETE FROM custom_note_terms WHERE term_id IN (650);
UPDATE IGNORE glossary_terms SET term_id=153 WHERE term_id IN (650);
DELETE FROM glossary_terms WHERE term_id IN (650);
UPDATE IGNORE term_morphemes SET term_id=153 WHERE term_id IN (650);
DELETE FROM term_morphemes WHERE term_id IN (650);
UPDATE terms SET deleted_at=NOW() WHERE id IN (650) AND deleted_at IS NULL;

-- [155] hematemesis / 토혈  <= dup 594,685
UPDATE terms SET spelling='hematemesis', meaning='토혈', abbreviation='', explanation='식도, 위, 십이지장 등 상부 위장관 출혈로 인해 피를 토하는 증상.', deleted_at=NULL WHERE id=155;
UPDATE IGNORE term_progresses SET term_id=155 WHERE term_id IN (594,685);
DELETE FROM term_progresses WHERE term_id IN (594,685);
UPDATE IGNORE bookmarks SET term_id=155 WHERE term_id IN (594,685);
DELETE FROM bookmarks WHERE term_id IN (594,685);
UPDATE IGNORE wrong_answer_terms SET term_id=155 WHERE term_id IN (594,685);
DELETE FROM wrong_answer_terms WHERE term_id IN (594,685);
UPDATE IGNORE custom_note_terms SET term_id=155 WHERE term_id IN (594,685);
DELETE FROM custom_note_terms WHERE term_id IN (594,685);
UPDATE IGNORE glossary_terms SET term_id=155 WHERE term_id IN (594,685);
DELETE FROM glossary_terms WHERE term_id IN (594,685);
UPDATE IGNORE term_morphemes SET term_id=155 WHERE term_id IN (594,685);
DELETE FROM term_morphemes WHERE term_id IN (594,685);
UPDATE terms SET deleted_at=NOW() WHERE id IN (594,685) AND deleted_at IS NULL;

-- [156] hematochezia / 혈변배설  <= dup 595
UPDATE terms SET spelling='hematochezia', meaning='혈변배설', abbreviation='', explanation='대변에 피가 섞여 나오는 증상. 출혈 위치에 따라 색이 다름.', deleted_at=NULL WHERE id=156;
UPDATE IGNORE term_progresses SET term_id=156 WHERE term_id IN (595);
DELETE FROM term_progresses WHERE term_id IN (595);
UPDATE IGNORE bookmarks SET term_id=156 WHERE term_id IN (595);
DELETE FROM bookmarks WHERE term_id IN (595);
UPDATE IGNORE wrong_answer_terms SET term_id=156 WHERE term_id IN (595);
DELETE FROM wrong_answer_terms WHERE term_id IN (595);
UPDATE IGNORE custom_note_terms SET term_id=156 WHERE term_id IN (595);
DELETE FROM custom_note_terms WHERE term_id IN (595);
UPDATE IGNORE glossary_terms SET term_id=156 WHERE term_id IN (595);
DELETE FROM glossary_terms WHERE term_id IN (595);
UPDATE IGNORE term_morphemes SET term_id=156 WHERE term_id IN (595);
DELETE FROM term_morphemes WHERE term_id IN (595);
UPDATE terms SET deleted_at=NOW() WHERE id IN (595) AND deleted_at IS NULL;

-- [160] hemothorax / 혈흉  <= dup 331,520
UPDATE terms SET spelling='hemothorax', meaning='혈흉', abbreviation='', explanation='외상 등으로 인해 흉막강 내에 피가 고인 상태입니다.', deleted_at=NULL WHERE id=160;
UPDATE IGNORE term_progresses SET term_id=160 WHERE term_id IN (331,520);
DELETE FROM term_progresses WHERE term_id IN (331,520);
UPDATE IGNORE bookmarks SET term_id=160 WHERE term_id IN (331,520);
DELETE FROM bookmarks WHERE term_id IN (331,520);
UPDATE IGNORE wrong_answer_terms SET term_id=160 WHERE term_id IN (331,520);
DELETE FROM wrong_answer_terms WHERE term_id IN (331,520);
UPDATE IGNORE custom_note_terms SET term_id=160 WHERE term_id IN (331,520);
DELETE FROM custom_note_terms WHERE term_id IN (331,520);
UPDATE IGNORE glossary_terms SET term_id=160 WHERE term_id IN (331,520);
DELETE FROM glossary_terms WHERE term_id IN (331,520);
UPDATE IGNORE term_morphemes SET term_id=160 WHERE term_id IN (331,520);
DELETE FROM term_morphemes WHERE term_id IN (331,520);
UPDATE terms SET deleted_at=NOW() WHERE id IN (331,520) AND deleted_at IS NULL;

-- [165] hypoglycemia / 저혈당증  <= dup 1212
UPDATE terms SET spelling='hypoglycemia', meaning='저혈당증', abbreviation='', explanation='혈액 내 포도당 농도가 정상 수치보다 낮은 상태.', deleted_at=NULL WHERE id=165;
UPDATE IGNORE term_progresses SET term_id=165 WHERE term_id IN (1212);
DELETE FROM term_progresses WHERE term_id IN (1212);
UPDATE IGNORE bookmarks SET term_id=165 WHERE term_id IN (1212);
DELETE FROM bookmarks WHERE term_id IN (1212);
UPDATE IGNORE wrong_answer_terms SET term_id=165 WHERE term_id IN (1212);
DELETE FROM wrong_answer_terms WHERE term_id IN (1212);
UPDATE IGNORE custom_note_terms SET term_id=165 WHERE term_id IN (1212);
DELETE FROM custom_note_terms WHERE term_id IN (1212);
UPDATE IGNORE glossary_terms SET term_id=165 WHERE term_id IN (1212);
DELETE FROM glossary_terms WHERE term_id IN (1212);
UPDATE IGNORE term_morphemes SET term_id=165 WHERE term_id IN (1212);
DELETE FROM term_morphemes WHERE term_id IN (1212);
UPDATE terms SET deleted_at=NOW() WHERE id IN (1212) AND deleted_at IS NULL;

-- [168] mechanical ventilation / 기계적 환기  <= dup 539
UPDATE terms SET spelling='mechanical ventilation', meaning='기계적 환기', abbreviation='', explanation='자발 호흡이 불가능하거나 부족할 때 기계 장치를 이용해 인공적으로 호흡을 보조하거나 대체하는 치료.', deleted_at=NULL WHERE id=168;
UPDATE IGNORE term_progresses SET term_id=168 WHERE term_id IN (539);
DELETE FROM term_progresses WHERE term_id IN (539);
UPDATE IGNORE bookmarks SET term_id=168 WHERE term_id IN (539);
DELETE FROM bookmarks WHERE term_id IN (539);
UPDATE IGNORE wrong_answer_terms SET term_id=168 WHERE term_id IN (539);
DELETE FROM wrong_answer_terms WHERE term_id IN (539);
UPDATE IGNORE custom_note_terms SET term_id=168 WHERE term_id IN (539);
DELETE FROM custom_note_terms WHERE term_id IN (539);
UPDATE IGNORE glossary_terms SET term_id=168 WHERE term_id IN (539);
DELETE FROM glossary_terms WHERE term_id IN (539);
UPDATE IGNORE term_morphemes SET term_id=168 WHERE term_id IN (539);
DELETE FROM term_morphemes WHERE term_id IN (539);
UPDATE terms SET deleted_at=NOW() WHERE id IN (539) AND deleted_at IS NULL;

-- [169] melena / 흑색변  <= dup 596
UPDATE terms SET spelling='melena', meaning='흑색변', abbreviation='', explanation='상부 위장관 출혈로 인해 혈액이 위산과 반응하여 변색된, 타르처럼 검고 끈적한 변.', deleted_at=NULL WHERE id=169;
UPDATE IGNORE term_progresses SET term_id=169 WHERE term_id IN (596);
DELETE FROM term_progresses WHERE term_id IN (596);
UPDATE IGNORE bookmarks SET term_id=169 WHERE term_id IN (596);
DELETE FROM bookmarks WHERE term_id IN (596);
UPDATE IGNORE wrong_answer_terms SET term_id=169 WHERE term_id IN (596);
DELETE FROM wrong_answer_terms WHERE term_id IN (596);
UPDATE IGNORE custom_note_terms SET term_id=169 WHERE term_id IN (596);
DELETE FROM custom_note_terms WHERE term_id IN (596);
UPDATE IGNORE glossary_terms SET term_id=169 WHERE term_id IN (596);
DELETE FROM glossary_terms WHERE term_id IN (596);
UPDATE IGNORE term_morphemes SET term_id=169 WHERE term_id IN (596);
DELETE FROM term_morphemes WHERE term_id IN (596);
UPDATE terms SET deleted_at=NOW() WHERE id IN (596) AND deleted_at IS NULL;

-- [172] oxygen therapy / 산소요법  <= dup 536
UPDATE terms SET spelling='oxygen therapy', meaning='산소요법', abbreviation='', explanation='혈중 산소 농도가 낮은 환자에게 산소를 공급하여 저산소증을 개선하는 치료.', deleted_at=NULL WHERE id=172;
UPDATE IGNORE term_progresses SET term_id=172 WHERE term_id IN (536);
DELETE FROM term_progresses WHERE term_id IN (536);
UPDATE IGNORE bookmarks SET term_id=172 WHERE term_id IN (536);
DELETE FROM bookmarks WHERE term_id IN (536);
UPDATE IGNORE wrong_answer_terms SET term_id=172 WHERE term_id IN (536);
DELETE FROM wrong_answer_terms WHERE term_id IN (536);
UPDATE IGNORE custom_note_terms SET term_id=172 WHERE term_id IN (536);
DELETE FROM custom_note_terms WHERE term_id IN (536);
UPDATE IGNORE glossary_terms SET term_id=172 WHERE term_id IN (536);
DELETE FROM glossary_terms WHERE term_id IN (536);
UPDATE IGNORE term_morphemes SET term_id=172 WHERE term_id IN (536);
DELETE FROM term_morphemes WHERE term_id IN (536);
UPDATE terms SET deleted_at=NOW() WHERE id IN (536) AND deleted_at IS NULL;

-- [175] placenta previa / 전치태반  <= dup 912
UPDATE terms SET spelling='placenta previa', meaning='전치태반', abbreviation='', explanation='태반이 자궁 입구(자궁경부 내구)를 전체 또는 부분적으로 덮고 있는 상태. 임신 중 출혈 위험.', deleted_at=NULL WHERE id=175;
UPDATE IGNORE term_progresses SET term_id=175 WHERE term_id IN (912);
DELETE FROM term_progresses WHERE term_id IN (912);
UPDATE IGNORE bookmarks SET term_id=175 WHERE term_id IN (912);
DELETE FROM bookmarks WHERE term_id IN (912);
UPDATE IGNORE wrong_answer_terms SET term_id=175 WHERE term_id IN (912);
DELETE FROM wrong_answer_terms WHERE term_id IN (912);
UPDATE IGNORE custom_note_terms SET term_id=175 WHERE term_id IN (912);
DELETE FROM custom_note_terms WHERE term_id IN (912);
UPDATE IGNORE glossary_terms SET term_id=175 WHERE term_id IN (912);
DELETE FROM glossary_terms WHERE term_id IN (912);
UPDATE IGNORE term_morphemes SET term_id=175 WHERE term_id IN (912);
DELETE FROM term_morphemes WHERE term_id IN (912);
UPDATE terms SET deleted_at=NOW() WHERE id IN (912) AND deleted_at IS NULL;

-- [176] pulmonary edema / 폐부종  <= dup 363,515
UPDATE terms SET spelling='pulmonary edema', meaning='폐부종', abbreviation='', explanation='폐 조직 내에 비정상적으로 체액이 축적되어 가스 교환 장애를 유발하는 상태.', deleted_at=NULL WHERE id=176;
UPDATE IGNORE term_progresses SET term_id=176 WHERE term_id IN (363,515);
DELETE FROM term_progresses WHERE term_id IN (363,515);
UPDATE IGNORE bookmarks SET term_id=176 WHERE term_id IN (363,515);
DELETE FROM bookmarks WHERE term_id IN (363,515);
UPDATE IGNORE wrong_answer_terms SET term_id=176 WHERE term_id IN (363,515);
DELETE FROM wrong_answer_terms WHERE term_id IN (363,515);
UPDATE IGNORE custom_note_terms SET term_id=176 WHERE term_id IN (363,515);
DELETE FROM custom_note_terms WHERE term_id IN (363,515);
UPDATE IGNORE glossary_terms SET term_id=176 WHERE term_id IN (363,515);
DELETE FROM glossary_terms WHERE term_id IN (363,515);
UPDATE IGNORE term_morphemes SET term_id=176 WHERE term_id IN (363,515);
DELETE FROM term_morphemes WHERE term_id IN (363,515);
UPDATE terms SET deleted_at=NOW() WHERE id IN (363,515) AND deleted_at IS NULL;

-- [179] respiratory acidosis / 호흡성 산증  <= dup 524
UPDATE terms SET spelling='respiratory acidosis', meaning='호흡성 산증', abbreviation='', explanation='폐의 환기 기능 저하로 이산화탄소가 체내에 축적되어 혈액이 산성화되는 상태.', deleted_at=NULL WHERE id=179;
UPDATE IGNORE term_progresses SET term_id=179 WHERE term_id IN (524);
DELETE FROM term_progresses WHERE term_id IN (524);
UPDATE IGNORE bookmarks SET term_id=179 WHERE term_id IN (524);
DELETE FROM bookmarks WHERE term_id IN (524);
UPDATE IGNORE wrong_answer_terms SET term_id=179 WHERE term_id IN (524);
DELETE FROM wrong_answer_terms WHERE term_id IN (524);
UPDATE IGNORE custom_note_terms SET term_id=179 WHERE term_id IN (524);
DELETE FROM custom_note_terms WHERE term_id IN (524);
UPDATE IGNORE glossary_terms SET term_id=179 WHERE term_id IN (524);
DELETE FROM glossary_terms WHERE term_id IN (524);
UPDATE IGNORE term_morphemes SET term_id=179 WHERE term_id IN (524);
DELETE FROM term_morphemes WHERE term_id IN (524);
UPDATE terms SET deleted_at=NOW() WHERE id IN (524) AND deleted_at IS NULL;

-- [182] shock / 쇼크  <= dup 704
UPDATE terms SET spelling='shock', meaning='쇼크', abbreviation='', explanation='조직으로의 혈액 순환 및 산소 공급이 급격히 감소하여 세포 기능 장애를 유발하는 생명을 위협하는 상태.', deleted_at=NULL WHERE id=182;
UPDATE IGNORE term_progresses SET term_id=182 WHERE term_id IN (704);
DELETE FROM term_progresses WHERE term_id IN (704);
UPDATE IGNORE bookmarks SET term_id=182 WHERE term_id IN (704);
DELETE FROM bookmarks WHERE term_id IN (704);
UPDATE IGNORE wrong_answer_terms SET term_id=182 WHERE term_id IN (704);
DELETE FROM wrong_answer_terms WHERE term_id IN (704);
UPDATE IGNORE custom_note_terms SET term_id=182 WHERE term_id IN (704);
DELETE FROM custom_note_terms WHERE term_id IN (704);
UPDATE IGNORE glossary_terms SET term_id=182 WHERE term_id IN (704);
DELETE FROM glossary_terms WHERE term_id IN (704);
UPDATE IGNORE term_morphemes SET term_id=182 WHERE term_id IN (704);
DELETE FROM term_morphemes WHERE term_id IN (704);
UPDATE terms SET deleted_at=NOW() WHERE id IN (704) AND deleted_at IS NULL;

-- [184] spontaneous abortion / 자연유산  <= dup 927
UPDATE terms SET spelling='spontaneous abortion', meaning='자연유산', abbreviation='', explanation='의학적 시술 없이 임신 20주 이전에 태아가 자궁 밖으로 배출되거나 사망하는 현상.', deleted_at=NULL WHERE id=184;
UPDATE IGNORE term_progresses SET term_id=184 WHERE term_id IN (927);
DELETE FROM term_progresses WHERE term_id IN (927);
UPDATE IGNORE bookmarks SET term_id=184 WHERE term_id IN (927);
DELETE FROM bookmarks WHERE term_id IN (927);
UPDATE IGNORE wrong_answer_terms SET term_id=184 WHERE term_id IN (927);
DELETE FROM wrong_answer_terms WHERE term_id IN (927);
UPDATE IGNORE custom_note_terms SET term_id=184 WHERE term_id IN (927);
DELETE FROM custom_note_terms WHERE term_id IN (927);
UPDATE IGNORE glossary_terms SET term_id=184 WHERE term_id IN (927);
DELETE FROM glossary_terms WHERE term_id IN (927);
UPDATE IGNORE term_morphemes SET term_id=184 WHERE term_id IN (927);
DELETE FROM term_morphemes WHERE term_id IN (927);
UPDATE terms SET deleted_at=NOW() WHERE id IN (927) AND deleted_at IS NULL;

-- [193] thoracentesis / 흉강천자  <= dup 529
UPDATE terms SET spelling='thoracentesis', meaning='흉강천자', abbreviation='', explanation='진단 또는 치료 목적으로 흉강(가슴 안 공간)에 바늘을 삽입하여 체액이나 공기를 뽑아내는 시술.', deleted_at=NULL WHERE id=193;
UPDATE IGNORE term_progresses SET term_id=193 WHERE term_id IN (529);
DELETE FROM term_progresses WHERE term_id IN (529);
UPDATE IGNORE bookmarks SET term_id=193 WHERE term_id IN (529);
DELETE FROM bookmarks WHERE term_id IN (529);
UPDATE IGNORE wrong_answer_terms SET term_id=193 WHERE term_id IN (529);
DELETE FROM wrong_answer_terms WHERE term_id IN (529);
UPDATE IGNORE custom_note_terms SET term_id=193 WHERE term_id IN (529);
DELETE FROM custom_note_terms WHERE term_id IN (529);
UPDATE IGNORE glossary_terms SET term_id=193 WHERE term_id IN (529);
DELETE FROM glossary_terms WHERE term_id IN (529);
UPDATE IGNORE term_morphemes SET term_id=193 WHERE term_id IN (529);
DELETE FROM term_morphemes WHERE term_id IN (529);
UPDATE terms SET deleted_at=NOW() WHERE id IN (529) AND deleted_at IS NULL;

-- [196] transfusion / 수혈  <= dup 807
UPDATE terms SET spelling='transfusion', meaning='수혈', abbreviation='', explanation='출혈, 빈혈 등으로 부족해진 혈액 성분(적혈구, 혈소판 등)을 정맥을 통해 공급하는 치료.', deleted_at=NULL WHERE id=196;
UPDATE IGNORE term_progresses SET term_id=196 WHERE term_id IN (807);
DELETE FROM term_progresses WHERE term_id IN (807);
UPDATE IGNORE bookmarks SET term_id=196 WHERE term_id IN (807);
DELETE FROM bookmarks WHERE term_id IN (807);
UPDATE IGNORE wrong_answer_terms SET term_id=196 WHERE term_id IN (807);
DELETE FROM wrong_answer_terms WHERE term_id IN (807);
UPDATE IGNORE custom_note_terms SET term_id=196 WHERE term_id IN (807);
DELETE FROM custom_note_terms WHERE term_id IN (807);
UPDATE IGNORE glossary_terms SET term_id=196 WHERE term_id IN (807);
DELETE FROM glossary_terms WHERE term_id IN (807);
UPDATE IGNORE term_morphemes SET term_id=196 WHERE term_id IN (807);
DELETE FROM term_morphemes WHERE term_id IN (807);
UPDATE terms SET deleted_at=NOW() WHERE id IN (807) AND deleted_at IS NULL;

-- [201] upper respiratory infection / 상기도감염  <= dup 500
UPDATE terms SET spelling='upper respiratory infection', meaning='상기도감염', abbreviation='URI', explanation='코, 인두, 후두, 부비동 등 상부 호흡기관에 발생하는 감염. (예: 감기, 인후염)', deleted_at=NULL WHERE id=201;
UPDATE IGNORE term_progresses SET term_id=201 WHERE term_id IN (500);
DELETE FROM term_progresses WHERE term_id IN (500);
UPDATE IGNORE bookmarks SET term_id=201 WHERE term_id IN (500);
DELETE FROM bookmarks WHERE term_id IN (500);
UPDATE IGNORE wrong_answer_terms SET term_id=201 WHERE term_id IN (500);
DELETE FROM wrong_answer_terms WHERE term_id IN (500);
UPDATE IGNORE custom_note_terms SET term_id=201 WHERE term_id IN (500);
DELETE FROM custom_note_terms WHERE term_id IN (500);
UPDATE IGNORE glossary_terms SET term_id=201 WHERE term_id IN (500);
DELETE FROM glossary_terms WHERE term_id IN (500);
UPDATE IGNORE term_morphemes SET term_id=201 WHERE term_id IN (500);
DELETE FROM term_morphemes WHERE term_id IN (500);
UPDATE terms SET deleted_at=NOW() WHERE id IN (500) AND deleted_at IS NULL;

-- [202] urinary tract infection / 요로감염  <= dup 380
UPDATE terms SET spelling='urinary tract infection', meaning='요로감염', abbreviation='UTI', explanation='신장, 요관, 방광, 요도 등 비뇨기계에 발생하는 감염.', deleted_at=NULL WHERE id=202;
UPDATE IGNORE term_progresses SET term_id=202 WHERE term_id IN (380);
DELETE FROM term_progresses WHERE term_id IN (380);
UPDATE IGNORE bookmarks SET term_id=202 WHERE term_id IN (380);
DELETE FROM bookmarks WHERE term_id IN (380);
UPDATE IGNORE wrong_answer_terms SET term_id=202 WHERE term_id IN (380);
DELETE FROM wrong_answer_terms WHERE term_id IN (380);
UPDATE IGNORE custom_note_terms SET term_id=202 WHERE term_id IN (380);
DELETE FROM custom_note_terms WHERE term_id IN (380);
UPDATE IGNORE glossary_terms SET term_id=202 WHERE term_id IN (380);
DELETE FROM glossary_terms WHERE term_id IN (380);
UPDATE IGNORE term_morphemes SET term_id=202 WHERE term_id IN (380);
DELETE FROM term_morphemes WHERE term_id IN (380);
UPDATE terms SET deleted_at=NOW() WHERE id IN (380) AND deleted_at IS NULL;

-- [204] arterial blood gas analysis / 동맥혈가스분석  <= dup 292,656
UPDATE terms SET spelling='arterial blood gas analysis', meaning='동맥혈가스분석', abbreviation='ABGA', explanation='동맥혈을 채취하여 혈액 내 산소, 이산화탄소 농도 및 산성도(pH) 등을 분석하는 검사입니다.', deleted_at=NULL WHERE id=204;
UPDATE IGNORE term_progresses SET term_id=204 WHERE term_id IN (292,656);
DELETE FROM term_progresses WHERE term_id IN (292,656);
UPDATE IGNORE bookmarks SET term_id=204 WHERE term_id IN (292,656);
DELETE FROM bookmarks WHERE term_id IN (292,656);
UPDATE IGNORE wrong_answer_terms SET term_id=204 WHERE term_id IN (292,656);
DELETE FROM wrong_answer_terms WHERE term_id IN (292,656);
UPDATE IGNORE custom_note_terms SET term_id=204 WHERE term_id IN (292,656);
DELETE FROM custom_note_terms WHERE term_id IN (292,656);
UPDATE IGNORE glossary_terms SET term_id=204 WHERE term_id IN (292,656);
DELETE FROM glossary_terms WHERE term_id IN (292,656);
UPDATE IGNORE term_morphemes SET term_id=204 WHERE term_id IN (292,656);
DELETE FROM term_morphemes WHERE term_id IN (292,656);
UPDATE terms SET deleted_at=NOW() WHERE id IN (292,656) AND deleted_at IS NULL;

-- [212] acute renal failure / 급성신부전  <= dup 283
UPDATE terms SET spelling='acute renal failure', meaning='급성신부전', abbreviation='ARF', explanation='신장 기능이 수 시간에서 수일 내에 급격히 저하되는 상태를 말합니다. (AKI와 유사하게 사용되기도 합니다)', deleted_at=NULL WHERE id=212;
UPDATE IGNORE term_progresses SET term_id=212 WHERE term_id IN (283);
DELETE FROM term_progresses WHERE term_id IN (283);
UPDATE IGNORE bookmarks SET term_id=212 WHERE term_id IN (283);
DELETE FROM bookmarks WHERE term_id IN (283);
UPDATE IGNORE wrong_answer_terms SET term_id=212 WHERE term_id IN (283);
DELETE FROM wrong_answer_terms WHERE term_id IN (283);
UPDATE IGNORE custom_note_terms SET term_id=212 WHERE term_id IN (283);
DELETE FROM custom_note_terms WHERE term_id IN (283);
UPDATE IGNORE glossary_terms SET term_id=212 WHERE term_id IN (283);
DELETE FROM glossary_terms WHERE term_id IN (283);
UPDATE IGNORE term_morphemes SET term_id=212 WHERE term_id IN (283);
DELETE FROM term_morphemes WHERE term_id IN (283);
UPDATE terms SET deleted_at=NOW() WHERE id IN (283) AND deleted_at IS NULL;

-- [219] coronary artery bypass graft / 관상동맥우회술  <= dup 675
UPDATE terms SET spelling='coronary artery bypass graft', meaning='관상동맥우회술', abbreviation='CABG', explanation='막히거나 좁아진 관상동맥을 대체 혈관으로 우회시켜 심장 근육에 혈류를 공급하는 수술.', deleted_at=NULL WHERE id=219;
UPDATE IGNORE term_progresses SET term_id=219 WHERE term_id IN (675);
DELETE FROM term_progresses WHERE term_id IN (675);
UPDATE IGNORE bookmarks SET term_id=219 WHERE term_id IN (675);
DELETE FROM bookmarks WHERE term_id IN (675);
UPDATE IGNORE wrong_answer_terms SET term_id=219 WHERE term_id IN (675);
DELETE FROM wrong_answer_terms WHERE term_id IN (675);
UPDATE IGNORE custom_note_terms SET term_id=219 WHERE term_id IN (675);
DELETE FROM custom_note_terms WHERE term_id IN (675);
UPDATE IGNORE glossary_terms SET term_id=219 WHERE term_id IN (675);
DELETE FROM glossary_terms WHERE term_id IN (675);
UPDATE IGNORE term_morphemes SET term_id=219 WHERE term_id IN (675);
DELETE FROM term_morphemes WHERE term_id IN (675);
UPDATE terms SET deleted_at=NOW() WHERE id IN (675) AND deleted_at IS NULL;

-- [221] common bile duct / 총담관  <= dup 1029
UPDATE terms SET spelling='common bile duct', meaning='총담관', abbreviation='CBD', explanation='간에서 만들어진 담즙이 십이지장으로 흘러가는 주된 통로.', deleted_at=NULL WHERE id=221;
UPDATE IGNORE term_progresses SET term_id=221 WHERE term_id IN (1029);
DELETE FROM term_progresses WHERE term_id IN (1029);
UPDATE IGNORE bookmarks SET term_id=221 WHERE term_id IN (1029);
DELETE FROM bookmarks WHERE term_id IN (1029);
UPDATE IGNORE wrong_answer_terms SET term_id=221 WHERE term_id IN (1029);
DELETE FROM wrong_answer_terms WHERE term_id IN (1029);
UPDATE IGNORE custom_note_terms SET term_id=221 WHERE term_id IN (1029);
DELETE FROM custom_note_terms WHERE term_id IN (1029);
UPDATE IGNORE glossary_terms SET term_id=221 WHERE term_id IN (1029);
DELETE FROM glossary_terms WHERE term_id IN (1029);
UPDATE IGNORE term_morphemes SET term_id=221 WHERE term_id IN (1029);
DELETE FROM term_morphemes WHERE term_id IN (1029);
UPDATE terms SET deleted_at=NOW() WHERE id IN (1029) AND deleted_at IS NULL;

-- [224] congestive heart failure / 울혈심부전  <= dup 744
UPDATE terms SET spelling='congestive heart failure', meaning='울혈심부전', abbreviation='CHF', explanation='심장 기능 저하로 혈액 순환이 원활하지 못해 폐나 전신에 체액이 정체(울혈)되는 상태. (위의 울혈성 심부전과 중복)', deleted_at=NULL WHERE id=224;
UPDATE IGNORE term_progresses SET term_id=224 WHERE term_id IN (744);
DELETE FROM term_progresses WHERE term_id IN (744);
UPDATE IGNORE bookmarks SET term_id=224 WHERE term_id IN (744);
DELETE FROM bookmarks WHERE term_id IN (744);
UPDATE IGNORE wrong_answer_terms SET term_id=224 WHERE term_id IN (744);
DELETE FROM wrong_answer_terms WHERE term_id IN (744);
UPDATE IGNORE custom_note_terms SET term_id=224 WHERE term_id IN (744);
DELETE FROM custom_note_terms WHERE term_id IN (744);
UPDATE IGNORE glossary_terms SET term_id=224 WHERE term_id IN (744);
DELETE FROM glossary_terms WHERE term_id IN (744);
UPDATE IGNORE term_morphemes SET term_id=224 WHERE term_id IN (744);
DELETE FROM term_morphemes WHERE term_id IN (744);
UPDATE terms SET deleted_at=NOW() WHERE id IN (744) AND deleted_at IS NULL;

-- [225] cardiopulmonary resuscitation / 심폐소생술  <= dup 299,668
UPDATE terms SET spelling='cardiopulmonary resuscitation', meaning='심폐소생술', abbreviation='CPR', explanation='심정지 환자의 심장 기능과 호흡을 되살리기 위한 응급 처치 (가슴 압박, 인공 호흡).', deleted_at=NULL WHERE id=225;
UPDATE IGNORE term_progresses SET term_id=225 WHERE term_id IN (299,668);
DELETE FROM term_progresses WHERE term_id IN (299,668);
UPDATE IGNORE bookmarks SET term_id=225 WHERE term_id IN (299,668);
DELETE FROM bookmarks WHERE term_id IN (299,668);
UPDATE IGNORE wrong_answer_terms SET term_id=225 WHERE term_id IN (299,668);
DELETE FROM wrong_answer_terms WHERE term_id IN (299,668);
UPDATE IGNORE custom_note_terms SET term_id=225 WHERE term_id IN (299,668);
DELETE FROM custom_note_terms WHERE term_id IN (299,668);
UPDATE IGNORE glossary_terms SET term_id=225 WHERE term_id IN (299,668);
DELETE FROM glossary_terms WHERE term_id IN (299,668);
UPDATE IGNORE term_morphemes SET term_id=225 WHERE term_id IN (299,668);
DELETE FROM term_morphemes WHERE term_id IN (299,668);
UPDATE terms SET deleted_at=NOW() WHERE id IN (299,668) AND deleted_at IS NULL;

-- [226] chronic renal failure / 만성신부전  <= dup 307
UPDATE terms SET spelling='chronic renal failure', meaning='만성신부전', abbreviation='CRF', explanation='신장 기능이 수개월에서 수년에 걸쳐 점진적, 비가역적으로 저하되는 상태. (위의 만성신부전과 중복)', deleted_at=NULL WHERE id=226;
UPDATE IGNORE term_progresses SET term_id=226 WHERE term_id IN (307);
DELETE FROM term_progresses WHERE term_id IN (307);
UPDATE IGNORE bookmarks SET term_id=226 WHERE term_id IN (307);
DELETE FROM bookmarks WHERE term_id IN (307);
UPDATE IGNORE wrong_answer_terms SET term_id=226 WHERE term_id IN (307);
DELETE FROM wrong_answer_terms WHERE term_id IN (307);
UPDATE IGNORE custom_note_terms SET term_id=226 WHERE term_id IN (307);
DELETE FROM custom_note_terms WHERE term_id IN (307);
UPDATE IGNORE glossary_terms SET term_id=226 WHERE term_id IN (307);
DELETE FROM glossary_terms WHERE term_id IN (307);
UPDATE IGNORE term_morphemes SET term_id=226 WHERE term_id IN (307);
DELETE FROM term_morphemes WHERE term_id IN (307);
UPDATE terms SET deleted_at=NOW() WHERE id IN (307) AND deleted_at IS NULL;

-- [227] cerebrospinal fluid / 뇌척수액  <= dup 303,740
UPDATE terms SET spelling='cerebrospinal fluid', meaning='뇌척수액', abbreviation='CSF', explanation='뇌와 척수를 둘러싸고 보호하며 영양을 공급하는 맑은 액체입니다.', deleted_at=NULL WHERE id=227;
UPDATE IGNORE term_progresses SET term_id=227 WHERE term_id IN (303,740);
DELETE FROM term_progresses WHERE term_id IN (303,740);
UPDATE IGNORE bookmarks SET term_id=227 WHERE term_id IN (303,740);
DELETE FROM bookmarks WHERE term_id IN (303,740);
UPDATE IGNORE wrong_answer_terms SET term_id=227 WHERE term_id IN (303,740);
DELETE FROM wrong_answer_terms WHERE term_id IN (303,740);
UPDATE IGNORE custom_note_terms SET term_id=227 WHERE term_id IN (303,740);
DELETE FROM custom_note_terms WHERE term_id IN (303,740);
UPDATE IGNORE glossary_terms SET term_id=227 WHERE term_id IN (303,740);
DELETE FROM glossary_terms WHERE term_id IN (303,740);
UPDATE IGNORE term_morphemes SET term_id=227 WHERE term_id IN (303,740);
DELETE FROM term_morphemes WHERE term_id IN (303,740);
UPDATE terms SET deleted_at=NOW() WHERE id IN (303,740) AND deleted_at IS NULL;

-- [228] central venous pressure / 중심정맥압  <= dup 300,739
UPDATE terms SET spelling='central venous pressure', meaning='중심정맥압', abbreviation='CVP', explanation='상대정맥 또는 우심방 근처의 압력. 체내 수분 상태 및 심장 기능 평가 지표.', deleted_at=NULL WHERE id=228;
UPDATE IGNORE term_progresses SET term_id=228 WHERE term_id IN (300,739);
DELETE FROM term_progresses WHERE term_id IN (300,739);
UPDATE IGNORE bookmarks SET term_id=228 WHERE term_id IN (300,739);
DELETE FROM bookmarks WHERE term_id IN (300,739);
UPDATE IGNORE wrong_answer_terms SET term_id=228 WHERE term_id IN (300,739);
DELETE FROM wrong_answer_terms WHERE term_id IN (300,739);
UPDATE IGNORE custom_note_terms SET term_id=228 WHERE term_id IN (300,739);
DELETE FROM custom_note_terms WHERE term_id IN (300,739);
UPDATE IGNORE glossary_terms SET term_id=228 WHERE term_id IN (300,739);
DELETE FROM glossary_terms WHERE term_id IN (300,739);
UPDATE IGNORE term_morphemes SET term_id=228 WHERE term_id IN (300,739);
DELETE FROM term_morphemes WHERE term_id IN (300,739);
UPDATE terms SET deleted_at=NOW() WHERE id IN (300,739) AND deleted_at IS NULL;

-- [229] diabetes insipidus / 요붕증  <= dup 1214
UPDATE terms SET spelling='diabetes insipidus', meaning='요붕증', abbreviation='DI', explanation='항이뇨호르몬 결핍 또는 작용 이상으로 소변량이 비정상적으로 많아지는 질환.', deleted_at=NULL WHERE id=229;
UPDATE IGNORE term_progresses SET term_id=229 WHERE term_id IN (1214);
DELETE FROM term_progresses WHERE term_id IN (1214);
UPDATE IGNORE bookmarks SET term_id=229 WHERE term_id IN (1214);
DELETE FROM bookmarks WHERE term_id IN (1214);
UPDATE IGNORE wrong_answer_terms SET term_id=229 WHERE term_id IN (1214);
DELETE FROM wrong_answer_terms WHERE term_id IN (1214);
UPDATE IGNORE custom_note_terms SET term_id=229 WHERE term_id IN (1214);
DELETE FROM custom_note_terms WHERE term_id IN (1214);
UPDATE IGNORE glossary_terms SET term_id=229 WHERE term_id IN (1214);
DELETE FROM glossary_terms WHERE term_id IN (1214);
UPDATE IGNORE term_morphemes SET term_id=229 WHERE term_id IN (1214);
DELETE FROM term_morphemes WHERE term_id IN (1214);
UPDATE terms SET deleted_at=NOW() WHERE id IN (1214) AND deleted_at IS NULL;

-- [244] fracture / 골절  <= dup 386
UPDATE terms SET spelling='fracture', meaning='골절', abbreviation='FX', explanation='뼈의 연속성이 완전하거나 불완전하게 끊어진 상태.', deleted_at=NULL WHERE id=244;
UPDATE IGNORE term_progresses SET term_id=244 WHERE term_id IN (386);
DELETE FROM term_progresses WHERE term_id IN (386);
UPDATE IGNORE bookmarks SET term_id=244 WHERE term_id IN (386);
DELETE FROM bookmarks WHERE term_id IN (386);
UPDATE IGNORE wrong_answer_terms SET term_id=244 WHERE term_id IN (386);
DELETE FROM wrong_answer_terms WHERE term_id IN (386);
UPDATE IGNORE custom_note_terms SET term_id=244 WHERE term_id IN (386);
DELETE FROM custom_note_terms WHERE term_id IN (386);
UPDATE IGNORE glossary_terms SET term_id=244 WHERE term_id IN (386);
DELETE FROM glossary_terms WHERE term_id IN (386);
UPDATE IGNORE term_morphemes SET term_id=244 WHERE term_id IN (386);
DELETE FROM term_morphemes WHERE term_id IN (386);
UPDATE terms SET deleted_at=NOW() WHERE id IN (386) AND deleted_at IS NULL;

-- [249] hemodialysis / 혈액투석  <= dup 330
UPDATE terms SET spelling='hemodialysis', meaning='혈액투석', abbreviation='HD', explanation='기계를 이용하여 혈액 내 노폐물과 과다 수분을 제거하고 전해질 균형을 맞추는 신대체 요법.', deleted_at=NULL WHERE id=249;
UPDATE IGNORE term_progresses SET term_id=249 WHERE term_id IN (330);
DELETE FROM term_progresses WHERE term_id IN (330);
UPDATE IGNORE bookmarks SET term_id=249 WHERE term_id IN (330);
DELETE FROM bookmarks WHERE term_id IN (330);
UPDATE IGNORE wrong_answer_terms SET term_id=249 WHERE term_id IN (330);
DELETE FROM wrong_answer_terms WHERE term_id IN (330);
UPDATE IGNORE custom_note_terms SET term_id=249 WHERE term_id IN (330);
DELETE FROM custom_note_terms WHERE term_id IN (330);
UPDATE IGNORE glossary_terms SET term_id=249 WHERE term_id IN (330);
DELETE FROM glossary_terms WHERE term_id IN (330);
UPDATE IGNORE term_morphemes SET term_id=249 WHERE term_id IN (330);
DELETE FROM term_morphemes WHERE term_id IN (330);
UPDATE terms SET deleted_at=NOW() WHERE id IN (330) AND deleted_at IS NULL;

-- [267] left ventricular hypertrophy / 좌심실비대  <= dup 341
UPDATE terms SET spelling='left ventricular hypertrophy', meaning='좌심실비대', abbreviation='LVH', explanation='심장의 좌심실 벽이 비정상적으로 두꺼워진 상태. 고혈압 등이 원인일 수 있음.', deleted_at=NULL WHERE id=267;
UPDATE IGNORE term_progresses SET term_id=267 WHERE term_id IN (341);
DELETE FROM term_progresses WHERE term_id IN (341);
UPDATE IGNORE bookmarks SET term_id=267 WHERE term_id IN (341);
DELETE FROM bookmarks WHERE term_id IN (341);
UPDATE IGNORE wrong_answer_terms SET term_id=267 WHERE term_id IN (341);
DELETE FROM wrong_answer_terms WHERE term_id IN (341);
UPDATE IGNORE custom_note_terms SET term_id=267 WHERE term_id IN (341);
DELETE FROM custom_note_terms WHERE term_id IN (341);
UPDATE IGNORE glossary_terms SET term_id=267 WHERE term_id IN (341);
DELETE FROM glossary_terms WHERE term_id IN (341);
UPDATE IGNORE term_morphemes SET term_id=267 WHERE term_id IN (341);
DELETE FROM term_morphemes WHERE term_id IN (341);
UPDATE terms SET deleted_at=NOW() WHERE id IN (341) AND deleted_at IS NULL;

-- [270] myocardial infarction / 심근경색증  <= dup 348
UPDATE terms SET spelling='myocardial infarction', meaning='심근경색증', abbreviation='MI', explanation='관상동맥 혈류 차단으로 심장 근육이 괴사하는 질환. (급성심근경색증과 유사)', deleted_at=NULL WHERE id=270;
UPDATE IGNORE term_progresses SET term_id=270 WHERE term_id IN (348);
DELETE FROM term_progresses WHERE term_id IN (348);
UPDATE IGNORE bookmarks SET term_id=270 WHERE term_id IN (348);
DELETE FROM bookmarks WHERE term_id IN (348);
UPDATE IGNORE wrong_answer_terms SET term_id=270 WHERE term_id IN (348);
DELETE FROM wrong_answer_terms WHERE term_id IN (348);
UPDATE IGNORE custom_note_terms SET term_id=270 WHERE term_id IN (348);
DELETE FROM custom_note_terms WHERE term_id IN (348);
UPDATE IGNORE glossary_terms SET term_id=270 WHERE term_id IN (348);
DELETE FROM glossary_terms WHERE term_id IN (348);
UPDATE IGNORE term_morphemes SET term_id=270 WHERE term_id IN (348);
DELETE FROM term_morphemes WHERE term_id IN (348);
UPDATE terms SET deleted_at=NOW() WHERE id IN (348) AND deleted_at IS NULL;

-- [280] subdural hematoma / 경막하혈종  <= dup 372
UPDATE terms SET spelling='subdural hematoma', meaning='경막하혈종', abbreviation='SDH', explanation='뇌를 감싸는 경막 아래, 지주막 위에 혈액이 고이는 질환으로, 급성 또는 만성으로 발생할 수 있습니다.(Hemorrhage)', deleted_at=NULL WHERE id=280;
UPDATE IGNORE term_progresses SET term_id=280 WHERE term_id IN (372);
DELETE FROM term_progresses WHERE term_id IN (372);
UPDATE IGNORE bookmarks SET term_id=280 WHERE term_id IN (372);
DELETE FROM bookmarks WHERE term_id IN (372);
UPDATE IGNORE wrong_answer_terms SET term_id=280 WHERE term_id IN (372);
DELETE FROM wrong_answer_terms WHERE term_id IN (372);
UPDATE IGNORE custom_note_terms SET term_id=280 WHERE term_id IN (372);
DELETE FROM custom_note_terms WHERE term_id IN (372);
UPDATE IGNORE glossary_terms SET term_id=280 WHERE term_id IN (372);
DELETE FROM glossary_terms WHERE term_id IN (372);
UPDATE IGNORE term_morphemes SET term_id=280 WHERE term_id IN (372);
DELETE FROM term_morphemes WHERE term_id IN (372);
UPDATE terms SET deleted_at=NOW() WHERE id IN (372) AND deleted_at IS NULL;

-- [291] apnea / 무호흡  <= dup 477
UPDATE terms SET spelling='apnea', meaning='무호흡', abbreviation='', explanation='일시적으로 호흡이 정지된 상태입니다.', deleted_at=NULL WHERE id=291;
UPDATE IGNORE term_progresses SET term_id=291 WHERE term_id IN (477);
DELETE FROM term_progresses WHERE term_id IN (477);
UPDATE IGNORE bookmarks SET term_id=291 WHERE term_id IN (477);
DELETE FROM bookmarks WHERE term_id IN (477);
UPDATE IGNORE wrong_answer_terms SET term_id=291 WHERE term_id IN (477);
DELETE FROM wrong_answer_terms WHERE term_id IN (477);
UPDATE IGNORE custom_note_terms SET term_id=291 WHERE term_id IN (477);
DELETE FROM custom_note_terms WHERE term_id IN (477);
UPDATE IGNORE glossary_terms SET term_id=291 WHERE term_id IN (477);
DELETE FROM glossary_terms WHERE term_id IN (477);
UPDATE IGNORE term_morphemes SET term_id=291 WHERE term_id IN (477);
DELETE FROM term_morphemes WHERE term_id IN (477);
UPDATE terms SET deleted_at=NOW() WHERE id IN (477) AND deleted_at IS NULL;

-- [336] intensive care unit / 중환자실  <= dup 690
UPDATE terms SET spelling='intensive care unit', meaning='중환자실', abbreviation='ICU', explanation='생명 유지를 위해 집중적인 치료와 감시가 필요한 중증 환자를 위한 병실입니다.', deleted_at=NULL WHERE id=336;
UPDATE IGNORE term_progresses SET term_id=336 WHERE term_id IN (690);
DELETE FROM term_progresses WHERE term_id IN (690);
UPDATE IGNORE bookmarks SET term_id=336 WHERE term_id IN (690);
DELETE FROM bookmarks WHERE term_id IN (690);
UPDATE IGNORE wrong_answer_terms SET term_id=336 WHERE term_id IN (690);
DELETE FROM wrong_answer_terms WHERE term_id IN (690);
UPDATE IGNORE custom_note_terms SET term_id=336 WHERE term_id IN (690);
DELETE FROM custom_note_terms WHERE term_id IN (690);
UPDATE IGNORE glossary_terms SET term_id=336 WHERE term_id IN (690);
DELETE FROM glossary_terms WHERE term_id IN (690);
UPDATE IGNORE term_morphemes SET term_id=336 WHERE term_id IN (690);
DELETE FROM term_morphemes WHERE term_id IN (690);
UPDATE terms SET deleted_at=NOW() WHERE id IN (690) AND deleted_at IS NULL;

-- [358] pneumothorax / 기흉  <= dup 522
UPDATE terms SET spelling='pneumothorax', meaning='기흉', abbreviation='', explanation='폐를 둘러싸고 있는 흉막강 내에 공기가 차서 폐가 허탈되는 상태입니다.', deleted_at=NULL WHERE id=358;
UPDATE IGNORE term_progresses SET term_id=358 WHERE term_id IN (522);
DELETE FROM term_progresses WHERE term_id IN (522);
UPDATE IGNORE bookmarks SET term_id=358 WHERE term_id IN (522);
DELETE FROM bookmarks WHERE term_id IN (522);
UPDATE IGNORE wrong_answer_terms SET term_id=358 WHERE term_id IN (522);
DELETE FROM wrong_answer_terms WHERE term_id IN (522);
UPDATE IGNORE custom_note_terms SET term_id=358 WHERE term_id IN (522);
DELETE FROM custom_note_terms WHERE term_id IN (522);
UPDATE IGNORE glossary_terms SET term_id=358 WHERE term_id IN (522);
DELETE FROM glossary_terms WHERE term_id IN (522);
UPDATE IGNORE term_morphemes SET term_id=358 WHERE term_id IN (522);
DELETE FROM term_morphemes WHERE term_id IN (522);
UPDATE terms SET deleted_at=NOW() WHERE id IN (522) AND deleted_at IS NULL;

-- [359] positive end-expiratory pressure / 호기말 양압  <= dup 541
UPDATE terms SET spelling='positive end-expiratory pressure', meaning='호기말 양압', abbreviation='PEEP', explanation='인공호흡기 치료 시 호기가 끝난 후에도 폐에 일정한 양압을 유지시켜 폐포의 허탈을 방지하고 산소화를 개선하는 방법입니다.', deleted_at=NULL WHERE id=359;
UPDATE IGNORE term_progresses SET term_id=359 WHERE term_id IN (541);
DELETE FROM term_progresses WHERE term_id IN (541);
UPDATE IGNORE bookmarks SET term_id=359 WHERE term_id IN (541);
DELETE FROM bookmarks WHERE term_id IN (541);
UPDATE IGNORE wrong_answer_terms SET term_id=359 WHERE term_id IN (541);
DELETE FROM wrong_answer_terms WHERE term_id IN (541);
UPDATE IGNORE custom_note_terms SET term_id=359 WHERE term_id IN (541);
DELETE FROM custom_note_terms WHERE term_id IN (541);
UPDATE IGNORE glossary_terms SET term_id=359 WHERE term_id IN (541);
DELETE FROM glossary_terms WHERE term_id IN (541);
UPDATE IGNORE term_morphemes SET term_id=359 WHERE term_id IN (541);
DELETE FROM term_morphemes WHERE term_id IN (541);
UPDATE terms SET deleted_at=NOW() WHERE id IN (541) AND deleted_at IS NULL;

-- [364] pulmonary embolism / 폐색전증  <= dup 516
UPDATE terms SET spelling='pulmonary embolism', meaning='폐색전증', abbreviation='PE', explanation='혈전(피떡)이 폐동맥을 막아 발생하는 질환으로, 심각한 호흡곤란을 유발할 수 있습니다.', deleted_at=NULL WHERE id=364;
UPDATE IGNORE term_progresses SET term_id=364 WHERE term_id IN (516);
DELETE FROM term_progresses WHERE term_id IN (516);
UPDATE IGNORE bookmarks SET term_id=364 WHERE term_id IN (516);
DELETE FROM bookmarks WHERE term_id IN (516);
UPDATE IGNORE wrong_answer_terms SET term_id=364 WHERE term_id IN (516);
DELETE FROM wrong_answer_terms WHERE term_id IN (516);
UPDATE IGNORE custom_note_terms SET term_id=364 WHERE term_id IN (516);
DELETE FROM custom_note_terms WHERE term_id IN (516);
UPDATE IGNORE glossary_terms SET term_id=364 WHERE term_id IN (516);
DELETE FROM glossary_terms WHERE term_id IN (516);
UPDATE IGNORE term_morphemes SET term_id=364 WHERE term_id IN (516);
DELETE FROM term_morphemes WHERE term_id IN (516);
UPDATE terms SET deleted_at=NOW() WHERE id IN (516) AND deleted_at IS NULL;

-- [441] anterior cruciate ligament / 전방십자인대  <= dup 1047
UPDATE terms SET spelling='anterior cruciate ligament', meaning='전방십자인대', abbreviation='ACL', explanation='무릎 관절 중앙에서 정강뼈가 넙다리뼈보다 앞으로 밀리는 것을 막는 중요한 인대입니다.', deleted_at=NULL WHERE id=441;
UPDATE IGNORE term_progresses SET term_id=441 WHERE term_id IN (1047);
DELETE FROM term_progresses WHERE term_id IN (1047);
UPDATE IGNORE bookmarks SET term_id=441 WHERE term_id IN (1047);
DELETE FROM bookmarks WHERE term_id IN (1047);
UPDATE IGNORE wrong_answer_terms SET term_id=441 WHERE term_id IN (1047);
DELETE FROM wrong_answer_terms WHERE term_id IN (1047);
UPDATE IGNORE custom_note_terms SET term_id=441 WHERE term_id IN (1047);
DELETE FROM custom_note_terms WHERE term_id IN (1047);
UPDATE IGNORE glossary_terms SET term_id=441 WHERE term_id IN (1047);
DELETE FROM glossary_terms WHERE term_id IN (1047);
UPDATE IGNORE term_morphemes SET term_id=441 WHERE term_id IN (1047);
DELETE FROM term_morphemes WHERE term_id IN (1047);
UPDATE terms SET deleted_at=NOW() WHERE id IN (1047) AND deleted_at IS NULL;

-- [458] pharynx / 인두  <= dup 551
UPDATE terms SET spelling='pharynx', meaning='인두', abbreviation='', explanation='코안과 입안의 뒤쪽에 위치한, 공기와 음식이 모두 지나가는 통로입니다.', deleted_at=NULL WHERE id=458;
UPDATE IGNORE term_progresses SET term_id=458 WHERE term_id IN (551);
DELETE FROM term_progresses WHERE term_id IN (551);
UPDATE IGNORE bookmarks SET term_id=458 WHERE term_id IN (551);
DELETE FROM bookmarks WHERE term_id IN (551);
UPDATE IGNORE wrong_answer_terms SET term_id=458 WHERE term_id IN (551);
DELETE FROM wrong_answer_terms WHERE term_id IN (551);
UPDATE IGNORE custom_note_terms SET term_id=458 WHERE term_id IN (551);
DELETE FROM custom_note_terms WHERE term_id IN (551);
UPDATE IGNORE glossary_terms SET term_id=458 WHERE term_id IN (551);
DELETE FROM glossary_terms WHERE term_id IN (551);
UPDATE IGNORE term_morphemes SET term_id=458 WHERE term_id IN (551);
DELETE FROM term_morphemes WHERE term_id IN (551);
UPDATE terms SET deleted_at=NOW() WHERE id IN (551) AND deleted_at IS NULL;

-- [494] hiccup / 딸꾹질  <= dup 591
UPDATE terms SET spelling='hiccup', meaning='딸꾹질', abbreviation='', explanation='횡격막의 불수의적인 경련으로 인해 나타나는 현상입니다.', deleted_at=NULL WHERE id=494;
UPDATE IGNORE term_progresses SET term_id=494 WHERE term_id IN (591);
DELETE FROM term_progresses WHERE term_id IN (591);
UPDATE IGNORE bookmarks SET term_id=494 WHERE term_id IN (591);
DELETE FROM bookmarks WHERE term_id IN (591);
UPDATE IGNORE wrong_answer_terms SET term_id=494 WHERE term_id IN (591);
DELETE FROM wrong_answer_terms WHERE term_id IN (591);
UPDATE IGNORE custom_note_terms SET term_id=494 WHERE term_id IN (591);
DELETE FROM custom_note_terms WHERE term_id IN (591);
UPDATE IGNORE glossary_terms SET term_id=494 WHERE term_id IN (591);
DELETE FROM glossary_terms WHERE term_id IN (591);
UPDATE IGNORE term_morphemes SET term_id=494 WHERE term_id IN (591);
DELETE FROM term_morphemes WHERE term_id IN (591);
UPDATE terms SET deleted_at=NOW() WHERE id IN (591) AND deleted_at IS NULL;

-- [502] pharyngitis / 인두염  <= dup 604
UPDATE terms SET spelling='pharyngitis', meaning='인두염', abbreviation='', explanation='인두에 염증이 발생한 상태입니다.', deleted_at=NULL WHERE id=502;
UPDATE IGNORE term_progresses SET term_id=502 WHERE term_id IN (604);
DELETE FROM term_progresses WHERE term_id IN (604);
UPDATE IGNORE bookmarks SET term_id=502 WHERE term_id IN (604);
DELETE FROM bookmarks WHERE term_id IN (604);
UPDATE IGNORE wrong_answer_terms SET term_id=502 WHERE term_id IN (604);
DELETE FROM wrong_answer_terms WHERE term_id IN (604);
UPDATE IGNORE custom_note_terms SET term_id=502 WHERE term_id IN (604);
DELETE FROM custom_note_terms WHERE term_id IN (604);
UPDATE IGNORE glossary_terms SET term_id=502 WHERE term_id IN (604);
DELETE FROM glossary_terms WHERE term_id IN (604);
UPDATE IGNORE term_morphemes SET term_id=502 WHERE term_id IN (604);
DELETE FROM term_morphemes WHERE term_id IN (604);
UPDATE terms SET deleted_at=NOW() WHERE id IN (604) AND deleted_at IS NULL;

-- [503] tonsillitis / 편도염  <= dup 605,1154
UPDATE terms SET spelling='tonsillitis', meaning='편도염', abbreviation='', explanation='목젖 양쪽에 있는 구개편도에 염증이 생긴 것입니다.', deleted_at=NULL WHERE id=503;
UPDATE IGNORE term_progresses SET term_id=503 WHERE term_id IN (605,1154);
DELETE FROM term_progresses WHERE term_id IN (605,1154);
UPDATE IGNORE bookmarks SET term_id=503 WHERE term_id IN (605,1154);
DELETE FROM bookmarks WHERE term_id IN (605,1154);
UPDATE IGNORE wrong_answer_terms SET term_id=503 WHERE term_id IN (605,1154);
DELETE FROM wrong_answer_terms WHERE term_id IN (605,1154);
UPDATE IGNORE custom_note_terms SET term_id=503 WHERE term_id IN (605,1154);
DELETE FROM custom_note_terms WHERE term_id IN (605,1154);
UPDATE IGNORE glossary_terms SET term_id=503 WHERE term_id IN (605,1154);
DELETE FROM glossary_terms WHERE term_id IN (605,1154);
UPDATE IGNORE term_morphemes SET term_id=503 WHERE term_id IN (605,1154);
DELETE FROM term_morphemes WHERE term_id IN (605,1154);
UPDATE terms SET deleted_at=NOW() WHERE id IN (605,1154) AND deleted_at IS NULL;

-- [552] esophagus / 식도  <= dup 682
UPDATE terms SET spelling='esophagus', meaning='식도', abbreviation='', explanation='인두와 위를 연결하는 근육으로 된 관으로, 음식물을 위로 운반합니다.', deleted_at=NULL WHERE id=552;
UPDATE IGNORE term_progresses SET term_id=552 WHERE term_id IN (682);
DELETE FROM term_progresses WHERE term_id IN (682);
UPDATE IGNORE bookmarks SET term_id=552 WHERE term_id IN (682);
DELETE FROM bookmarks WHERE term_id IN (682);
UPDATE IGNORE wrong_answer_terms SET term_id=552 WHERE term_id IN (682);
DELETE FROM wrong_answer_terms WHERE term_id IN (682);
UPDATE IGNORE custom_note_terms SET term_id=552 WHERE term_id IN (682);
DELETE FROM custom_note_terms WHERE term_id IN (682);
UPDATE IGNORE glossary_terms SET term_id=552 WHERE term_id IN (682);
DELETE FROM glossary_terms WHERE term_id IN (682);
UPDATE IGNORE term_morphemes SET term_id=552 WHERE term_id IN (682);
DELETE FROM term_morphemes WHERE term_id IN (682);
UPDATE terms SET deleted_at=NOW() WHERE id IN (682) AND deleted_at IS NULL;

-- [553] abdomen / 복부  <= dup 652
UPDATE terms SET spelling='abdomen', meaning='복부', abbreviation='', explanation='가슴과 골반 사이에 위치한 신체 부위로, 여러 소화기관이 들어있습니다.', deleted_at=NULL WHERE id=553;
UPDATE IGNORE term_progresses SET term_id=553 WHERE term_id IN (652);
DELETE FROM term_progresses WHERE term_id IN (652);
UPDATE IGNORE bookmarks SET term_id=553 WHERE term_id IN (652);
DELETE FROM bookmarks WHERE term_id IN (652);
UPDATE IGNORE wrong_answer_terms SET term_id=553 WHERE term_id IN (652);
DELETE FROM wrong_answer_terms WHERE term_id IN (652);
UPDATE IGNORE custom_note_terms SET term_id=553 WHERE term_id IN (652);
DELETE FROM custom_note_terms WHERE term_id IN (652);
UPDATE IGNORE glossary_terms SET term_id=553 WHERE term_id IN (652);
DELETE FROM glossary_terms WHERE term_id IN (652);
UPDATE IGNORE term_morphemes SET term_id=553 WHERE term_id IN (652);
DELETE FROM term_morphemes WHERE term_id IN (652);
UPDATE terms SET deleted_at=NOW() WHERE id IN (652) AND deleted_at IS NULL;

-- [586] vomiting / 구토  <= dup 948
UPDATE terms SET spelling='vomiting', meaning='구토', abbreviation='', explanation='위의 내용물이 입을 통해 강력하게 뿜어져 나오는 현상입니다.', deleted_at=NULL WHERE id=586;
UPDATE IGNORE term_progresses SET term_id=586 WHERE term_id IN (948);
DELETE FROM term_progresses WHERE term_id IN (948);
UPDATE IGNORE bookmarks SET term_id=586 WHERE term_id IN (948);
DELETE FROM bookmarks WHERE term_id IN (948);
UPDATE IGNORE wrong_answer_terms SET term_id=586 WHERE term_id IN (948);
DELETE FROM wrong_answer_terms WHERE term_id IN (948);
UPDATE IGNORE custom_note_terms SET term_id=586 WHERE term_id IN (948);
DELETE FROM custom_note_terms WHERE term_id IN (948);
UPDATE IGNORE glossary_terms SET term_id=586 WHERE term_id IN (948);
DELETE FROM glossary_terms WHERE term_id IN (948);
UPDATE IGNORE term_morphemes SET term_id=586 WHERE term_id IN (948);
DELETE FROM term_morphemes WHERE term_id IN (948);
UPDATE terms SET deleted_at=NOW() WHERE id IN (948) AND deleted_at IS NULL;

-- [627] intussusception / 창자겹침증, 장중첩증  <= dup 1176
UPDATE terms SET spelling='intussusception', meaning='창자겹침증, 장중첩증', abbreviation='', explanation='장의 일부가 안쪽으로 말려 들어가는 질환으로, 주로 영유아에게서 발생합니다.', deleted_at=NULL WHERE id=627;
UPDATE IGNORE term_progresses SET term_id=627 WHERE term_id IN (1176);
DELETE FROM term_progresses WHERE term_id IN (1176);
UPDATE IGNORE bookmarks SET term_id=627 WHERE term_id IN (1176);
DELETE FROM bookmarks WHERE term_id IN (1176);
UPDATE IGNORE wrong_answer_terms SET term_id=627 WHERE term_id IN (1176);
DELETE FROM wrong_answer_terms WHERE term_id IN (1176);
UPDATE IGNORE custom_note_terms SET term_id=627 WHERE term_id IN (1176);
DELETE FROM custom_note_terms WHERE term_id IN (1176);
UPDATE IGNORE glossary_terms SET term_id=627 WHERE term_id IN (1176);
DELETE FROM glossary_terms WHERE term_id IN (1176);
UPDATE IGNORE term_morphemes SET term_id=627 WHERE term_id IN (1176);
DELETE FROM term_morphemes WHERE term_id IN (1176);
UPDATE terms SET deleted_at=NOW() WHERE id IN (1176) AND deleted_at IS NULL;

-- [722] amniotic fluid / 양수  <= dup 825
UPDATE terms SET spelling='amniotic fluid', meaning='양수', abbreviation='', explanation='양막 안에 차 있으며, 임신 기간 동안 태아를 보호하는 액체입니다.', deleted_at=NULL WHERE id=722;
UPDATE IGNORE term_progresses SET term_id=722 WHERE term_id IN (825);
DELETE FROM term_progresses WHERE term_id IN (825);
UPDATE IGNORE bookmarks SET term_id=722 WHERE term_id IN (825);
DELETE FROM bookmarks WHERE term_id IN (825);
UPDATE IGNORE wrong_answer_terms SET term_id=722 WHERE term_id IN (825);
DELETE FROM wrong_answer_terms WHERE term_id IN (825);
UPDATE IGNORE custom_note_terms SET term_id=722 WHERE term_id IN (825);
DELETE FROM custom_note_terms WHERE term_id IN (825);
UPDATE IGNORE glossary_terms SET term_id=722 WHERE term_id IN (825);
DELETE FROM glossary_terms WHERE term_id IN (825);
UPDATE IGNORE term_morphemes SET term_id=722 WHERE term_id IN (825);
DELETE FROM term_morphemes WHERE term_id IN (825);
UPDATE terms SET deleted_at=NOW() WHERE id IN (825) AND deleted_at IS NULL;

-- 동의어 되살리기 (대원칙상 별도 term 유지)
UPDATE terms SET spelling='hemiplegia', meaning='반신불수', deleted_at=NULL WHERE id=52;
UPDATE terms SET spelling='pulmonary embolism', meaning='폐색전', deleted_at=NULL WHERE id=177;
UPDATE terms SET spelling='thoracentesis', meaning='흉막천자 (흉강천자)', deleted_at=NULL WHERE id=376;

-- TIA 의미 확정
UPDATE terms SET meaning='일과성 허혈 발작' WHERE id=379;
