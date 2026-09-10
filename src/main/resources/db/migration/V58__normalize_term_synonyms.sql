-- incision and drainage 앰퍼샌드형과 and 형 및 한국어 동의어 추가
UPDATE terms
SET spelling='incision & drainage, incision and drainage',
    meaning='절개와 배농, 절개배농'
WHERE id=254;

-- thoracentesis 영어 변형과 한국어 동의어 추가
-- 376 은 V55 에서 193 으로 병합되어 소프트삭제 상태라 활성 캐논 193 에 반영한다
-- 표준 간호용어 흉강천자를 대표값으로 앞에 둔다
UPDATE terms
SET spelling='thoracentesis, thoracocentesis',
    meaning='흉강천자, 흉막천자'
WHERE id=193;

-- ADHD 는 하나의 질환명이라 슬래시 유지
-- 단순 하이픈 띄어쓰기 차이는 동의어로 저장하지 않고 채점때 정규화한다
