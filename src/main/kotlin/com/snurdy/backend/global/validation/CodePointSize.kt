package com.snurdy.backend.global.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

// 유니코드 코드포인트 단위 길이 검증
// 표준 @Size 가 String.length (UTF-16 code unit) 기준이라 이모지 1개를 2 카운트하는 한계를 해결
// 한글/영문/숫자/이모지 모두 1자 = 1 카운트
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Constraint(validatedBy = [CodePointSizeValidator::class])
annotation class CodePointSize(
    val min: Int = 0,
    val max: Int = Int.MAX_VALUE,
    val message: String = "글자 수가 허용 범위를 벗어났습니다",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class CodePointSizeValidator : ConstraintValidator<CodePointSize, String> {

    private var min: Int = 0
    private var max: Int = Int.MAX_VALUE

    override fun initialize(constraint: CodePointSize) {
        this.min = constraint.min
        this.max = constraint.max
    }

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return true
        val count = value.codePointCount(0, value.length)
        return count in min..max
    }
}
