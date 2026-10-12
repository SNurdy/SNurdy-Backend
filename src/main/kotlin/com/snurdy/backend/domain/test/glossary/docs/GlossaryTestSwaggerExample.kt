package com.snurdy.backend.domain.test.glossary.docs

object GlossaryTestSwaggerExample {

    const val GLOSSARY_TEST_CARDS_REQUEST_TERM_IDS = "[395, 432, 441, 440]"
    const val GLOSSARY_TEST_CARDS_REQUEST_TEST_TYPES = """[
  "WRITE_MEANING_FROM_TERM",
  "MC_MEANING_FROM_TERM",
  "MC_TERM_FROM_ABBREVIATION",
  "MC_TERM_FROM_MEANING"
]"""
    const val GLOSSARY_TEST_CARDS_RESPONSE_AVAILABLE_COUNTS = """{
  "WRITE_MEANING_FROM_TERM": 2,
  "MC_MEANING_FROM_TERM": 1,
  "MC_TERM_FROM_ABBREVIATION": 1
}"""
    const val GLOSSARY_TEST_CARDS_RESPONSE_CARDS = """[
  {
    "termId": 441,
    "testType": "MC_TERM_FROM_ABBREVIATION",
    "question": "ACL",
    "answer": "anterior cruciate ligament",
    "answers": ["anterior cruciate ligament"],
    "distractors": ["intervertebral disk", "physical therapy", "multiple myeloma"],
    "spelling": "anterior cruciate ligament",
    "meaning": "전방십자인대",
    "abbreviation": "ACL",
    "explanation": "무릎 관절 중앙에서 정강뼈가 넙다리뼈보다 앞으로 밀리는 것을 막는 중요한 인대입니다.",
    "bookmarked": false,
    "morphemes": [
      { "id": 67, "spelling": "ante-", "meaning": "~앞에; 이전에", "order": 0, "highlight": { "start": 0, "length": 4 } },
      { "id": 376, "spelling": "ligament(o)-", "meaning": "인대", "order": 1, "highlight": { "start": 18, "length": 8 } }
    ]
  },
  {
    "termId": 395,
    "testType": "WRITE_MEANING_FROM_TERM",
    "question": "intervertebral disk",
    "answer": "추간판, 디스크",
    "answers": ["추간판", "디스크"],
    "distractors": null,
    "spelling": "intervertebral disk",
    "meaning": "추간판, 디스크",
    "abbreviation": "IVD",
    "explanation": "척추뼈 사이에 위치하여 충격을 흡수하는 연골 조직입니다.",
    "bookmarked": false,
    "morphemes": [
      { "id": 139, "spelling": "inter-", "meaning": "사이, 간, 상호-", "order": 0, "highlight": { "start": 0, "length": 5 } },
      { "id": 481, "spelling": "vertebr(o)-", "meaning": "척추뼈", "order": 1, "highlight": { "start": 5, "length": 7 } }
    ]
  },
  {
    "termId": 432,
    "testType": "MC_MEANING_FROM_TERM",
    "question": "osteoarthritis",
    "answer": "골관절염",
    "answers": ["골관절염"],
    "distractors": ["강직척추염", "물리치료", "다발성 골수종"],
    "spelling": "osteoarthritis",
    "meaning": "골관절염",
    "abbreviation": "OA",
    "explanation": "관절 연골이 마모되어 통증과 변형을 유발하는 퇴행성 관절 질환입니다.",
    "bookmarked": true,
    "morphemes": [
      { "id": 405, "spelling": "oste(o)-; osturm-", "meaning": "뼈", "order": 0, "highlight": { "start": 0, "length": 5 } },
      { "id": 293, "spelling": "arthr(o)-; articul(o)-", "meaning": "관절", "order": 1, "highlight": { "start": 5, "length": 5 } },
      { "id": 20, "spelling": "-itis", "meaning": "염증", "order": 2, "highlight": { "start": 10, "length": 4 } }
    ]
  },
  {
    "termId": 440,
    "testType": "WRITE_MEANING_FROM_TERM",
    "question": "ankylosing spondylitis",
    "answer": "강직척추염",
    "answers": ["강직척추염"],
    "distractors": null,
    "spelling": "ankylosing spondylitis",
    "meaning": "강직척추염",
    "abbreviation": "AS",
    "explanation": "척추에 염증이 생기고 점차 굳어 움직임이 둔해지는 류마티스성 질환입니다.",
    "bookmarked": false,
    "morphemes": [
      { "id": 65, "spelling": "ankyl(o)-", "meaning": "경직된", "order": 0, "highlight": { "start": 0, "length": 6 } },
      { "id": 452, "spelling": "spondyl(o)-", "meaning": "척추뼈", "order": 1, "highlight": { "start": 11, "length": 7 } },
      { "id": 20, "spelling": "-itis", "meaning": "염증", "order": 2, "highlight": { "start": 18, "length": 4 } }
    ]
  }
]"""
    const val GLOSSARY_TEST_CARD_TERM_ID = "395"
    const val GLOSSARY_TEST_CARD_TEST_TYPE = "WRITE_MEANING_FROM_TERM"
    const val GLOSSARY_TEST_CARD_QUESTION = "intervertebral disk"
    const val GLOSSARY_TEST_CARD_ANSWER = "추간판, 디스크"
    const val GLOSSARY_TEST_CARD_ANSWERS = "[\"추간판\", \"디스크\"]"
    const val GLOSSARY_TEST_CARD_DISTRACTORS = "[\"골관절염\", \"물리치료\", \"다발성 골수종\"]"
    const val GLOSSARY_TEST_CARD_SPELLING = "intervertebral disk"
    const val GLOSSARY_TEST_CARD_MEANING = "추간판, 디스크"
    const val GLOSSARY_TEST_CARD_ABBREVIATION = "IVD"
    const val GLOSSARY_TEST_CARD_EXPLANATION = "척추뼈 사이에 위치하여 충격을 흡수하는 연골 조직입니다."
    const val GLOSSARY_TEST_CARD_BOOKMARKED = "false"
    const val GLOSSARY_TEST_CARD_MORPHEMES = """[
  { "id": 139, "spelling": "inter-", "meaning": "사이, 간, 상호-", "order": 0, "highlight": { "start": 0, "length": 5 } },
  { "id": 481, "spelling": "vertebr(o)-", "meaning": "척추뼈", "order": 1, "highlight": { "start": 5, "length": 7 } }
]"""
    const val GLOSSARY_TEST_SUBMIT_REQUEST_RESULTS = """[
  { "termId": 441, "testType": "MC_TERM_FROM_ABBREVIATION", "correct": false, "userAnswer": "intervertebral disk" },
  { "termId": 395, "testType": "WRITE_MEANING_FROM_TERM", "correct": true, "userAnswer": "추간판" },
  { "termId": 432, "testType": "MC_MEANING_FROM_TERM", "correct": true, "userAnswer": "골관절염" },
  { "termId": 440, "testType": "WRITE_MEANING_FROM_TERM", "correct": true, "userAnswer": "강직척추염" }
]"""
    const val GLOSSARY_TEST_SUBMIT_REQUEST_STUDIED_TERM_COUNT = "4"
    const val GLOSSARY_TEST_RESULT_ITEM_TERM_ID = "395"
    const val GLOSSARY_TEST_RESULT_ITEM_TEST_TYPE = "WRITE_MEANING_FROM_TERM"
    const val GLOSSARY_TEST_RESULT_ITEM_CORRECT = "true"
    const val GLOSSARY_TEST_RESULT_ITEM_USER_ANSWER = "추간판"
    const val GLOSSARY_TEST_SUBMIT_RESPONSE_CORRECT_COUNT = "3"
    const val GLOSSARY_TEST_SUBMIT_RESPONSE_INCORRECT_COUNT = "1"
    const val GLOSSARY_TEST_SUBMIT_RESPONSE_SESSION_STUDIED_COUNT = "4"
    const val GLOSSARY_TEST_SUBMIT_RESPONSE_TODAY_STUDIED_COUNT = "4"
    const val GLOSSARY_TEST_SUBMIT_RESPONSE_TOTAL_STUDIED_COUNT = "124"
    const val GLOSSARY_TEST_SUBMIT_RESPONSE_TODAY_GOAL_COMPLETED = "true"
    const val GLOSSARY_TEST_SNAPSHOT_GLOSSARY_ID = "18"
    const val GLOSSARY_TEST_SNAPSHOT_PROGRESS_RATE_BEFORE = "80.3"
    const val GLOSSARY_TEST_SNAPSHOT_PROGRESS_RATE_AFTER = "84.8"
    const val GLOSSARY_TEST_SNAPSHOT_LEVEL_BEFORE = "4"
    const val GLOSSARY_LEVEL_UP_EVENT_FROM = "4"
    const val GLOSSARY_LEVEL_UP_EVENT_TO = "5"
}