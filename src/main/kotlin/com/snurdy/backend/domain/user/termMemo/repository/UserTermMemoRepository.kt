package com.snurdy.backend.domain.user.termMemo.repository

import com.snurdy.backend.domain.user.termMemo.entity.UserTermMemoEntity
import com.snurdy.backend.domain.user.termMemo.entity.UserTermMemoId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserTermMemoRepository : JpaRepository<UserTermMemoEntity, UserTermMemoId> {

    fun findByIdUserIdAndIdTermId(userId: UUID, termId: Long): UserTermMemoEntity?

    fun existsByIdUserIdAndIdTermId(userId: UUID, termId: Long): Boolean

    fun deleteByIdUserIdAndIdTermId(userId: UUID, termId: Long)
}
