package com.snurdy.backend.domain.admin.workflow.service

object WorkflowStatus {
    const val REQUESTED = "REQUESTED"
    const val VALIDATED_WAITING = "VALIDATED_WAITING"
    const val APPLIED_DEV = "APPLIED_DEV"
    const val FAILED = "FAILED"
    const val CANCELED = "CANCELED"
}

object WorkflowItemStatus {
    const val PENDING = "PENDING"
    const val VALIDATED = "VALIDATED"
    const val APPLIED = "APPLIED"
    const val FAILED = "FAILED"
    const val SKIPPED = "SKIPPED"
}

object WorkflowOperation {
    const val CREATE = "CREATE"
    const val UPDATE = "UPDATE"
    const val DELETE = "DELETE"
}

object WorkflowEntityType {
    const val TERM = "TERM"
    const val MORPHEME = "MORPHEME"
}
