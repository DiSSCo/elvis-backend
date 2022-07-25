package org.synthesis.calls.request.ta.export.task

import org.synthesis.calls.request.ta.TaCallRequest

interface ExportTask {
    suspend fun generate(callRequest: TaCallRequest): String
}
