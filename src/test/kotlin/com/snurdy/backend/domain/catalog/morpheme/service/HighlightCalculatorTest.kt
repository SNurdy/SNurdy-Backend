package com.snurdy.backend.domain.catalog.morpheme.service

import com.snurdy.backend.domain.catalog.morpheme.repository.MorphemeVariantProjection
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class HighlightCalculatorTest {

    private val calculator = HighlightCalculator()

    private fun variant(
        morphemeId: Long = 1,
        variant: String,
        base: String
    ): MorphemeVariantProjection = object : MorphemeVariantProjection {
        override val id: Long = 0
        override val morphemeId: Long = morphemeId
        override val variant: String = variant
        override val base: String = base
    }

    @Nested
    @DisplayName("hepat(o)- 어근어미")
    inner class HepatTests {

        private val variants = listOf(variant(morphemeId = 361, variant = "hepato", base = "hepat"))

        @Test
        @DisplayName("hepatocyte → variant 'hepato' 매칭, start=0, length=6")
        fun hepatocyte() {
            val result = calculator.calculate("hepatocyte", variants)
            assertThat(result).isNotNull
            assertThat(result!!.start).isEqualTo(0)
            assertThat(result.length).isEqualTo(6)
        }

        @Test
        @DisplayName("hepatitis → variant 실패, base 'hepat' 폴백, start=0, length=5")
        fun hepatitis() {
            val result = calculator.calculate("hepatitis", variants)
            assertThat(result).isNotNull
            assertThat(result!!.start).isEqualTo(0)
            assertThat(result.length).isEqualTo(5)
        }

        @Test
        @DisplayName("hepatoma → variant 'hepato' 매칭, start=0, length=6")
        fun hepatoma() {
            val result = calculator.calculate("hepatoma", variants)
            assertThat(result).isNotNull
            assertThat(result!!.start).isEqualTo(0)
            assertThat(result.length).isEqualTo(6)
        }
    }

    @Nested
    @DisplayName("electr(o)- 어근어미")
    inner class ElectrTests {

        private val variants = listOf(
            variant(variant = "electro", base = "electr"),
            variant(variant = "electric", base = "electric")
        )

        @Test
        @DisplayName("electrocardiogram → 'electro' 매칭, start=0, length=7")
        fun electrocardiogram() {
            val result = calculator.calculate("electrocardiogram", variants)
            assertThat(result).isNotNull
            assertThat(result!!.start).isEqualTo(0)
            assertThat(result.length).isEqualTo(7)
        }

        @Test
        @DisplayName("electric → 'electric' 매칭 (더 긴 variant 우선), start=0, length=8")
        fun electric() {
            val result = calculator.calculate("electric", variants)
            assertThat(result).isNotNull
            assertThat(result!!.start).isEqualTo(0)
            assertThat(result.length).isEqualTo(8)
        }
    }

    @Nested
    @DisplayName("다중 variant — aque(o)- / hydr(o)- / hydration / hidr(o)-")
    inner class MultiVariantTests {

        private val variants = listOf(
            variant(variant = "aqueo", base = "aque"),
            variant(variant = "hydro", base = "hydr"),
            variant(variant = "hydration", base = "hydration"),
            variant(variant = "hidro", base = "hidr")
        )

        @Test
        @DisplayName("dehydration → 'hydration' 매칭 (가장 긴 variant), start=2, length=9")
        fun dehydration() {
            val result = calculator.calculate("dehydration", variants)
            assertThat(result).isNotNull
            assertThat(result!!.start).isEqualTo(2)
            assertThat(result.length).isEqualTo(9)
        }

        @Test
        @DisplayName("aqueous → variant 'aqueo' 매칭, start=0, length=5")
        fun aqueous() {
            val result = calculator.calculate("aqueous", variants)
            assertThat(result).isNotNull
            assertThat(result!!.start).isEqualTo(0)
            assertThat(result.length).isEqualTo(5)
        }
    }

    @Nested
    @DisplayName("접미사 — -cyte / -cyto")
    inner class SuffixTests {

        private val variants = listOf(
            variant(variant = "cyte", base = "cyte"),
            variant(variant = "cyto", base = "cyto")
        )

        @Test
        @DisplayName("hepatocyte → 'cyte' 매칭, start=6, length=4")
        fun hepatocyte_suffix() {
            val result = calculator.calculate("hepatocyte", variants)
            assertThat(result).isNotNull
            assertThat(result!!.start).isEqualTo(6)
            assertThat(result.length).isEqualTo(4)
        }

        @Test
        @DisplayName("cytoplasm → 'cyto' 매칭, start=0, length=4")
        fun cytoplasm() {
            val result = calculator.calculate("cytoplasm", variants)
            assertThat(result).isNotNull
            assertThat(result!!.start).isEqualTo(0)
            assertThat(result.length).isEqualTo(4)
        }
    }

    @Nested
    @DisplayName("매칭 실패 케이스")
    inner class NoMatchTests {

        @Test
        @DisplayName("관련 없는 용어 → null 반환")
        fun noMatch() {
            val variants = listOf(variant(variant = "hepato", base = "hepat"))
            val result = calculator.calculate("cardiology", variants)
            assertThat(result).isNull()
        }

        @Test
        @DisplayName("빈 variants → null 반환")
        fun emptyVariants() {
            val result = calculator.calculate("hepatocyte", emptyList())
            assertThat(result).isNull()
        }
    }
}