package com.snurdy.backend.domain.inquiry.docs

object InquirySwaggerDescription {

    const val INQUIRY_API_TAG_NAME = "Inquiry"
    const val INQUIRY_API_TAG_DESCRIPTION = "1대1 문의 API"
    const val CREATE_INQUIRY_SUMMARY = "문의 등록"
    const val LIST_INQUIRIES_SUMMARY = "내 문의 목록"
    const val GET_INQUIRY_SUMMARY = "문의 상세"
    const val UPDATE_INQUIRY_SUMMARY = "문의 수정. 답변 전까지만 가능"
    const val DELETE_INQUIRY_SUMMARY = "문의 삭제. 답변 전까지만 가능"

    const val CREATE_INQUIRY_REQUEST = "문의 등록 요청"
    const val UPDATE_INQUIRY_REQUEST = "문의 수정 요청. 유형과 내용을 모두 교체"
    const val INQUIRY_CATEGORY = "문의 유형 코드. BUG, CONTENT_ERROR, FEATURE_REQUEST, ACCOUNT, ETC 중 하나"
    const val INQUIRY_REQUEST_CONTENT = "문의 내용. 공백만으로는 불가하며 최대 1000자"

    const val INQUIRY_SUMMARY = "문의 목록 1건"
    const val INQUIRY_ID = "문의 ID"
    const val INQUIRY_CONTENT_PREVIEW = "문의 내용 미리보기. 줄바꿈을 공백으로 바꾼 앞 20자이며 잘렸으면 끝에 말줄임표가 붙음"
    const val INQUIRY_STATUS = "답변 상태. WAITING 은 답변 대기, ANSWERED 는 답변 완료"
    const val INQUIRY_CREATED_AT = "문의 작성 시각"
    const val INQUIRY_LIST_RESPONSE = "내 문의 목록. 최신순"

    const val INQUIRY_DETAIL = "문의 상세"
    const val INQUIRY_CONTENT = "문의 내용 전체"
    const val INQUIRY_EDITABLE = "수정과 삭제 가능 여부. 답변 전이면 true"
    const val INQUIRY_ANSWER = "답변. 답변 전이면 null"
    const val INQUIRY_ANSWER_CONTENT = "답변 내용"
    const val INQUIRY_ANSWERED_AT = "답변 등록 시각"

    const val ADMIN_INQUIRY_API_TAG_NAME = "Admin Inquiry"
    const val ADMIN_INQUIRY_API_TAG_DESCRIPTION = "1대1 문의 관리자 API. ADMIN 권한 필요"
    const val CREATE_INQUIRY_ANSWER_SUMMARY = "답변 등록"
    const val CREATE_INQUIRY_ANSWER_REQUEST = "답변 등록 요청"
    const val INQUIRY_ANSWER_REQUEST_CONTENT = "답변 내용. 공백만으로는 불가하며 최대 2000자"
}