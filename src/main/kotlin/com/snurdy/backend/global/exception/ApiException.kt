package com.snurdy.backend.global.exception

import com.snurdy.backend.global.common.constants.ErrorCode

class ApiException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message
) : RuntimeException(message)