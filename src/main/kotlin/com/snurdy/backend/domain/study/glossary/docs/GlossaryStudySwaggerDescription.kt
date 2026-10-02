package com.snurdy.backend.domain.study.glossary.docs

object GlossaryStudySwaggerDescription {

    const val GLOSSARY_STUDY_API_TAG_NAME = "Glossary Study"
    const val GLOSSARY_STUDY_API_TAG_DESCRIPTION = "기본 단어장 학습 API"
    const val LIST_STUDY_CARDS_SUMMARY = "기본 단어장 학습 카드 조회"
    const val GLOSSARY_STUDY_CARDS_REQUEST = "기본 단어장 학습 카드 요청"
    const val GLOSSARY_STUDY_CARDS_REQUEST_COUNT = "학습할 단어 수. 1~50, 앱에서는 10/20/30/40/50 중 선택"
    const val GLOSSARY_STUDY_CARDS_RESPONSE = "기본 단어장 학습 카드 응답"
    const val GLOSSARY_STUDY_CARDS_RESPONSE_REQUESTED_COUNT = "클라이언트가 요청한 단어 수"
    const val GLOSSARY_STUDY_CARDS_RESPONSE_TERMS = "학습 화면에 노출할 학습 세트 단어. 아직 학습하지 않은 단어를 우선으로 뽑고, 단어장 전체 단어 수가 requestedCount 보다 적으면 그만큼만 내려감"
    const val GLOSSARY_STUDY_TERM_ITEM = "기본 단어장 학습 세트 단어 1개"
    const val GLOSSARY_STUDY_TERM_TERM_ID = "단어 id"
    const val GLOSSARY_STUDY_TERM_ITEM_SPELLING = "영문 용어"
    const val GLOSSARY_STUDY_TERM_MEANING = "한글 의미"
    const val GLOSSARY_STUDY_TERM_ABBREVIATION = "약어. 없으면 null"
    const val GLOSSARY_STUDY_TERM_EXPLANATION = "상세 설명. 없으면 null"
    const val GLOSSARY_STUDY_TERM_BOOKMARKED = "현재 학습 중인 단어장 안에서 사용자가 북마크했는지 여부"
    const val GLOSSARY_STUDY_TERM_ITEM_MORPHEMES = "어근/어미 분석 + 형광펜 하이라이팅 범위. 색은 클라이언트가 order 순서대로 매핑"
}