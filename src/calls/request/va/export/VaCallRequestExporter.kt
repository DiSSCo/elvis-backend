package org.synthesis.calls.request.va.export

import java.io.OutputStream
import org.synthesis.calls.CallException
import org.synthesis.calls.request.CallRequestId
import org.synthesis.calls.request.va.export.task.ExportTask
import org.synthesis.calls.request.va.store.VaCallRequestFinder

class VaCallRequestExporter(
    private val vaCallRequestFinder: VaCallRequestFinder,
    private var taskCollection: Map<String, ExportTask>,
    private val htmlToPdfWriter: suspend (html: String, to: OutputStream) -> Unit
) {
    /**
     * @throws [CallException.CallRequestNotFound]
     * @throws [Exception] Incorrect tasks configuration
     */
    suspend fun handle(command: VaCallRequestExport, to: OutputStream) = htmlToPdfWriter(toHtml(command), to)

    private suspend fun toHtml(command: VaCallRequestExport): String {
        val callRequest = vaCallRequestFinder.find(command.id)
            ?: throw CallException.CallRequestNotFound(command.id)

        return when (command) {
            is VaCallRequestExport.Overview -> obtainTask("overview").generate(callRequest)
            is VaCallRequestExport.Comments -> obtainTask("comments").generate(callRequest)
        }
    }

    /**
     * @throws [Exception] Incorrect tasks configuration
     */
    private fun obtainTask(name: String) = taskCollection[name] ?: throw Exception("Incorrect tasks configuration")
}

sealed class VaCallRequestExport {
    abstract val id: CallRequestId

    data class Overview(
        override val id: CallRequestId
    ) : VaCallRequestExport()

    data class Comments(
        override val id: CallRequestId
    ) : VaCallRequestExport()
}
