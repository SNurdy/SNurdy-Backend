package com.snurdy.backend.global.exception

import com.snurdy.backend.global.common.constants.ErrorType

class CustomException(
    val errorType: ErrorType
) : RuntimeException(errorType.message)