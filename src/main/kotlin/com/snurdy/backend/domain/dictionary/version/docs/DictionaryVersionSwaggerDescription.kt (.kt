package com.snurdy.backend.domain.dictionary.version.docs

object DictionaryVersionSwaggerDescription {

    const val DICTIONARY_VERSION_API_TAG_NAME = "Dictionary Version"
    const val DICTIONARY_VERSION_API_TAG_DESCRIPTION = "사전 데이터 버전 API"
    const val GET_DICTIONARY_VERSION_SUMMARY = "사전 버전 조회"
    const val GET_DICTIONARY_VERSION_DESCRIPTION = "사전 데이터가 바뀌면 버전 값이 바뀐다. 클라이언트는 저장된 버전과 다를 때만 전체 동기화한다"
    const val DICTIONARY_VERSION_RESPONSE = "사전 버전 조회 응답"
    const val DICTIONARY_VERSION_RESPONSE_VERSION = "사전 데이터 버전. 저장된 값과 다르면 전체 동기화가 필요. 클라이언트는 값을 해석하지 않고 동일 여부만 비교"
}