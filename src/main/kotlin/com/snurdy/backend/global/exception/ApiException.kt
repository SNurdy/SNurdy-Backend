package com.snurdy.backend.global.exception

import com.snurdy.backend.global.constants.ErrorCode

class ApiException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message
) : RuntimeException(message)