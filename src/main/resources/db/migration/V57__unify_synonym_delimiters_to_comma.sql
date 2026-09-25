-- 동의어 구분자를 콤마로 통일한다
-- 카드 생성 때 콤마로 파싱해 acceptableAnswers 로 내려주기 위함
-- 동의어가 아닌 값 파렌 슬래시는 사전 정리하고 유지 대상은 건드리지 않는다

-- 1. 동의어가 아닌 값 사전 정리
-- 260 KUB 는 한 검사명이라 spelling 자체를 약어로 둔다
UPDATE terms SET spelling='KUB' WHERE id=260;
-- 431 관절경 검사와 수술을 동의어로 재정의한다
UPDATE terms SET meaning='관절경검사, 관절내시경검사' WHERE id=431;
-- 368 감별 진단 뒤 영어 주석 제거
UPDATE terms SET meaning='배제하다, 배제 진단하다' WHERE id=368;
-- 492 괄호 안 글자는 단어 완성이라 합친다
UPDATE terms SET meaning='분비물' WHERE id=492;
-- 836 술 접미어 완성
UPDATE terms SET meaning='제왕절개술' WHERE id=836;
-- 1083 1084 1085 지남력 주석 제거
UPDATE terms SET meaning='사람' WHERE id=1083;
UPDATE terms SET meaning='장소' WHERE id=1084;
UPDATE terms SET meaning='시간' WHERE id=1085;

-- 2. spelling 동의어 파렌을 콤마로 통일한다
UPDATE terms SET spelling='Achilles tendon, tendo calcaneus' WHERE id=720;
UPDATE terms SET spelling='compound fracture, open fracture' WHERE id=309;
UPDATE terms SET spelling='icterus, jaundice' WHERE id=1172;
UPDATE terms SET spelling='pleurisy, pleuritis' WHERE id=517;
UPDATE terms SET spelling='rubeola, measles' WHERE id=1161;
UPDATE terms SET spelling='white blood cell, leukocyte' WHERE id=717;
UPDATE terms SET spelling='rales, crackles' WHERE id=484;

-- 3. meaning 동의어 파렌을 콤마로 통일한다
UPDATE terms SET meaning='루게릭병, 근위축성측삭경화증' WHERE id=6;
UPDATE terms SET meaning='동맥선, 동맥 카테터' WHERE id=293;
UPDATE terms SET meaning='복합골절, 개방골절' WHERE id=309;
UPDATE terms SET meaning='체외막산소공급기, 에크모' WHERE id=1137;
UPDATE terms SET meaning='늑막삼출, 흉수' WHERE id=356;
UPDATE terms SET meaning='흉막염, 늑막염' WHERE id=357;
UPDATE terms SET meaning='유행성 이하선염, 볼거리' WHERE id=1158;
UPDATE terms SET meaning='제대염, 배꼽염' WHERE id=1185;
-- 381 장알균 장구균은 명사 동의어라 반코마이신 내성 수식어를 양쪽에 분배한다
UPDATE terms SET meaning='반코마이신 내성 장알균, 반코마이신 내성 장구균' WHERE id=381;

-- 4. meaning 동의어 슬래시를 콤마로 통일한다
UPDATE terms SET meaning='구강, 입안' WHERE id=545;
-- 922 초산부는 primipara 라는 다른 개념이라 primigravida 는 초임부만 둔다
UPDATE terms SET meaning='초임부' WHERE id=922;
UPDATE terms SET meaning='질좌제, 질정' WHERE id=943;

-- 5. meaning 콤마 앞 공백 정규화
UPDATE terms SET meaning='혼란, 혼동' WHERE id=134;
UPDATE terms SET meaning='경련, 경기' WHERE id=135;
UPDATE terms SET meaning='졸림, 나른함' WHERE id=140;
UPDATE terms SET meaning='코피, 비출혈' WHERE id=149;

-- attention deficit hyperactivity disorder 의 슬래시는 정식 명칭 일부라 유지한다

-- 6. primipara 초산부 신규 추가
-- 922 primigravida 초임부에서 분리된 별개 개념이라 신규 term 으로 두고 단어장은 922 에서 상속한다
INSERT INTO terms (id, spelling, meaning) VALUES (1248, 'primipara', '초산부');
INSERT IGNORE INTO glossary_terms (glossary_id, term_id, created_at, updated_at)
SELECT glossary_id, 1248, NOW(), NOW() FROM glossary_terms WHERE term_id=922;
