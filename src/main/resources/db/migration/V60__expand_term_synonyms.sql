-- 동의어 확장 병기 및 일부 spelling 정규화
-- 카드 생성 때 콤마로 파싱해 acceptableAnswers 로 내려주기 위함

-- spelling 과 meaning 동시 정정
-- 241 하이픈 제거 띄어쓰기
UPDATE terms
SET spelling='foreign body airway obstruction',
    meaning='기도 이물 폐쇄, 기도 이물 폐쇄증, 이물질 기도 폐쇄'
WHERE id=241;

-- 346 Staphylococcus 는 속명이라 대문자
UPDATE terms
SET spelling='methicillin resistant Staphylococcus aureus',
    meaning='메티실린 내성 황색포도상구균, 메티실린 내성 황색포도알균'
WHERE id=346;

-- 382 Staphylococcus 대문자
UPDATE terms
SET spelling='vancomycin resistant Staphylococcus aureus',
    meaning='반코마이신 내성 황색포도상구균, 반코마이신 내성 황색포도알균'
WHERE id=382;

-- 385 하이픈 제거 붙임
UPDATE terms
SET spelling='ventriculoperitoneal shunt',
    meaning='뇌실 복강 단락술, 뇌실 복막 단락술, 뇌실복강 간 단락술'
WHERE id=385;

-- 1115 단수형
UPDATE terms
SET spelling='hypoactivity',
    meaning='과소활동, 과소행동, 행동저하, 활동저하'
WHERE id=1115;

-- 1116 단수형
UPDATE terms
SET spelling='hyperactivity',
    meaning='과다활동, 과잉활동, 과잉행동, 과다행동, 활동과다, 행동과다'
WHERE id=1116;

-- meaning 만 확장
UPDATE terms SET meaning='비스테로이드성 항염증제, 비스테로이드성 소염제, 비스테로이드성 소염진통제' WHERE id=349;
UPDATE terms SET meaning='자가통증조절, 자가통증조절법, 환자자가통증조절법, 자가통증조절장치' WHERE id=354;
UPDATE terms SET meaning='호기말 양압, 호기말양압, 호기말 양압법' WHERE id=359;
UPDATE terms SET meaning='지역사회획득폐렴, 지역사회 획득 폐렴, 지역사회성 폐렴' WHERE id=512;
UPDATE terms SET meaning='알파태아단백, 알파태아단백질' WHERE id=860;
UPDATE terms SET meaning='블라록 타우시그 단락술, 블레이록 타우시그 단락술, 블레이록 토시그 단락술' WHERE id=1031;
UPDATE terms SET meaning='수족구병, 손발입병' WHERE id=1144;
