package com.snurdy.backend.global.exception

import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.dto.response.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ApiErrorController : ErrorController {

    @RequestMapping("/error")
    fun handleError(request: HttpServletRequest): ResponseEntity<ApiResponse<Unit>> {
        val status = request.getAttribute("jakarta.servlet.error.status_code") as? Int ?: 500

        val error = when (status) {
            401 -> CommonError.UNAUTHORIZED
            403 -> CommonError.FORBIDDEN
            404 -> CommonError.NOT_FOUND
            else -> CommonError.INTERNAL_ERROR
        }

        return ResponseEntity.status(status).body(ApiResponse.fail(error))
    }
}