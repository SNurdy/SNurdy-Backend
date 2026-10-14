package com.snurdy.backend.domain.test.common.docs

object TestSubmissionSwaggerDescription {

    const val IDEMPOTENCY_KEY =
        "시험 1회마다 앱이 생성하는 UUID. 같은 키로 다시 제출하면 재처리 없이 첫 응답을 그대로 반환한다. 생략 시 매 요청을 새 제출로 처리"
}