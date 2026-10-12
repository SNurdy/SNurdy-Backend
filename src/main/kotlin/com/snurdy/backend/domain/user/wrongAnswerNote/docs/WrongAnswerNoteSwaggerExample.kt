package com.snurdy.backend.domain.user.wrongAnswerNote.docs

object WrongAnswerNoteSwaggerExample {

    const val WRONG_ANSWER_PERIOD_CARD_PERIOD = "TODAY"
    const val WRONG_ANSWER_PERIOD_CARD_LABEL = "오늘 틀린 단어"
    const val WRONG_ANSWER_PERIOD_CARD_INCORRECT_TERM_COUNT = "12"
    const val WRONG_ANSWER_PERIOD_CARD_LIST_RESPONSE = """{"periods":[{"period":"TODAY","label":"오늘 틀린 단어","incorrectTermCount":12},{"period":"WEEK","label":"이번 주 틀린 단어","incorrectTermCount":34},{"period":"MONTH","label":"이번 달 틀린 단어","incorrectTermCount":120},{"period":"ALL","label":"전체 틀린 단어","incorrectTermCount":340}]}"""
    const val WRONG_ANSWER_GLOSSARY_CARD_GLOSSARY_ID = "18"
    const val WRONG_ANSWER_GLOSSARY_CARD_TITLE = "정형외과"
    const val WRONG_ANSWER_GLOSSARY_CARD_CATEGORY = "외과"
    const val WRONG_ANSWER_GLOSSARY_CARD_INCORRECT_TERM_COUNT = "7"
    const val WRONG_ANSWER_GLOSSARY_CARD_LIST_RESPONSE_GLOSSARIES = """[
  { "glossaryId": 1, "title": "일반내과", "category": "내과", "incorrectTermCount": 3 },
  { "glossaryId": 2, "title": "내분비내과", "category": "내과", "incorrectTermCount": 0 },
  { "glossaryId": 3, "title": "감염내과", "category": "내과", "incorrectTermCount": 0 },
  { "glossaryId": 4, "title": "소화기내과", "category": "내과", "incorrectTermCount": 12 },
  { "glossaryId": 5, "title": "신장내과", "category": "내과", "incorrectTermCount": 0 },
  { "glossaryId": 6, "title": "심장내과", "category": "내과", "incorrectTermCount": 5 },
  { "glossaryId": 7, "title": "신경과", "category": "내과", "incorrectTermCount": 0 },
  { "glossaryId": 8, "title": "호흡기내과", "category": "내과", "incorrectTermCount": 0 },
  { "glossaryId": 9, "title": "혈액종양내과", "category": "내과", "incorrectTermCount": 0 },
  { "glossaryId": 10, "title": "간담체외과", "category": "외과", "incorrectTermCount": 0 },
  { "glossaryId": 11, "title": "내분비외과", "category": "외과", "incorrectTermCount": 0 },
  { "glossaryId": 12, "title": "대장항문외과", "category": "외과", "incorrectTermCount": 0 },
  { "glossaryId": 13, "title": "성형외과", "category": "외과", "incorrectTermCount": 0 },
  { "glossaryId": 14, "title": "신경외과", "category": "외과", "incorrectTermCount": 0 },
  { "glossaryId": 15, "title": "심장외과", "category": "외과", "incorrectTermCount": 0 },
  { "glossaryId": 16, "title": "흉부외과", "category": "외과", "incorrectTermCount": 0 },
  { "glossaryId": 17, "title": "위장관외과", "category": "외과", "incorrectTermCount": 0 },
  { "glossaryId": 18, "title": "정형외과", "category": "외과", "incorrectTermCount": 7 },
  { "glossaryId": 19, "title": "정신건강의학과", "category": "기타", "incorrectTermCount": 0 },
  { "glossaryId": 20, "title": "응급의학과", "category": "기타", "incorrectTermCount": 0 },
  { "glossaryId": 21, "title": "중환자실", "category": "기타", "incorrectTermCount": 2 },
  { "glossaryId": 22, "title": "수술실", "category": "기타", "incorrectTermCount": 0 },
  { "glossaryId": 23, "title": "신생아실", "category": "기타", "incorrectTermCount": 0 },
  { "glossaryId": 24, "title": "산부인과", "category": "기타", "incorrectTermCount": 0 },
  { "glossaryId": 25, "title": "이비인후과", "category": "기타", "incorrectTermCount": 0 },
  { "glossaryId": 26, "title": "재활의학과", "category": "기타", "incorrectTermCount": 0 },
  { "glossaryId": 27, "title": "비뇨기과", "category": "기타", "incorrectTermCount": 0 },
  { "glossaryId": 28, "title": "소아청소년과", "category": "기타", "incorrectTermCount": 0 },
  { "glossaryId": 29, "title": "노인", "category": "기타", "incorrectTermCount": 0 }
]"""
    const val WRONG_ANSWER_TERM_ITEM_TERM_ID = "101"
    const val WRONG_ANSWER_TERM_ITEM_GLOSSARY_ID = "12"
    const val WRONG_ANSWER_TERM_ITEM_TOTAL_INCORRECT_COUNT = "18"
    const val DELETE_TERMS_FROM_WRONG_ANSWER_NOTE_REQUEST_TERM_IDS = "[123, 456]"
}