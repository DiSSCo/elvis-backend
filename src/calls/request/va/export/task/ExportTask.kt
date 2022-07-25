package org.synthesis.calls.request.va.export.task

import org.synthesis.calls.request.va.VaCallRequest

interface ExportTask {
    suspend fun generate(callRequest: VaCallRequest): String
}
