-- meaning 컬럼에 한글이 포함되어 있으므로 ngram parser를 사용한다.
-- ngram_token_size 기본값(2)로 한글 2글자 이상, 영어 부분 문자열 검색 가능.
-- spelling, meaning, abbreviation 세 컬럼 복합 인덱스로 단일 MATCH 표현식에서 검색.
ALTER TABLE terms
    ADD FULLTEXT INDEX ft_terms_search (spelling, meaning, abbreviation) WITH PARSER ngram;
