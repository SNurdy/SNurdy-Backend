package com.snurdy.backend.global.docs

import com.snurdy.backend.global.constants.AdminError
import com.snurdy.backend.global.constants.AdminSuccess
import com.snurdy.backend.global.constants.AuthError
import com.snurdy.backend.global.constants.AuthSuccess
import com.snurdy.backend.global.constants.BookmarkSuccess
import com.snurdy.backend.global.constants.CalendarSuccess
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.constants.CommonSuccess
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.constants.DictionarySuccess
import com.snurdy.backend.global.constants.ErrorCode
import com.snurdy.backend.global.constants.LearningError
import com.snurdy.backend.global.constants.LearningSuccess
import com.snurdy.backend.global.constants.MigrationError
import com.snurdy.backend.global.constants.MigrationSuccess
import com.snurdy.backend.global.constants.CustomNoteError
import com.snurdy.backend.global.constants.CustomNoteSuccess
import com.snurdy.backend.global.constants.SuccessCode
import com.snurdy.backend.global.constants.TermMemoError
import com.snurdy.backend.global.constants.TermMemoSuccess
import com.snurdy.backend.global.constants.UserError
import com.snurdy.backend.global.constants.UserSuccess
import com.snurdy.backend.global.constants.VersionError
import com.snurdy.backend.global.constants.VersionSuccess

// 코드 문자열로 ErrorCode/SuccessCode enum 항목을 빠르게 찾기 위한 인-메모리 인덱스
// 새 enum 을 정의하면 여기에 등록만 추가하면 Swagger 문서에 자동 노출된다
object ApiCodeRegistry {

    val ERRORS: Map<String, ErrorCode> = buildMap {
        listOf(
            CommonError.entries,
            AuthError.entries,
            UserError.entries,
            LearningError.entries,
            MigrationError.entries,
            DictionaryError.entries,
            CustomNoteError.entries,
            TermMemoError.entries,
            AdminError.entries,
            VersionError.entries
        ).flatten().forEach {
            val prev = put(it.code, it)
            require(prev == null) { "중복된 ErrorCode 문자열: ${it.code} (${prev!!::class.simpleName} vs ${it::class.simpleName})" }
        }
    }

    val SUCCESSES: Map<String, SuccessCode> = buildMap {
        listOf(
            CommonSuccess.entries,
            AuthSuccess.entries,
            UserSuccess.entries,
            BookmarkSuccess.entries,
            LearningSuccess.entries,
            CalendarSuccess.entries,
            DictionarySuccess.entries,
            CustomNoteSuccess.entries,
            TermMemoSuccess.entries,
            MigrationSuccess.entries,
            AdminSuccess.entries,
            VersionSuccess.entries
        ).flatten().forEach {
            val prev = put(it.code, it)
            require(prev == null) { "중복된 SuccessCode 문자열: ${it.code} (${prev!!::class.simpleName} vs ${it::class.simpleName})" }
        }
    }
}
