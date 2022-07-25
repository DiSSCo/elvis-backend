package org.synthesis.calls.request.ta.export

import java.io.OutputStream
import org.synthesis.calls.CallException
import org.synthesis.calls.request.CallRequestId
import org.synthesis.calls.request.ta.export.task.ExportTask
import org.synthesis.calls.request.ta.store.TaCallRequestFinder

class TaCallRequestExporter(
    private val taCallRequestFinder: TaCallRequestFinder,
    private var taskCollection: Map<String, ExportTask>,
    private val htmlToPdfWriter: suspend (html: String, to: OutputStream) -> Unit
) {
    /**
     * @throws [CallException.CallRequestNotFound]
     * @throws [Exception] Incorrect tasks configuration
     */
    suspend fun handle(command: TaCallRequestExport, to: OutputStream) = htmlToPdfWriter(toHtml(command), to)

    private suspend fun toHtml(command: TaCallRequestExport): String {
        val callRequest = taCallRequestFinder.find(command.id)
            ?: throw CallException.CallRequestNotFound(command.id)

        return when (command) {
            is TaCallRequestExport.Overview -> obtainTask("overview").generate(callRequest)
            is TaCallRequestExport.Comments -> obtainTask("comments").generate(callRequest)
        }
    }

    /**
     * @throws [Exception] Incorrect tasks configuration
     */
    private fun obtainTask(name: String) = taskCollection[name] ?: throw Exception("Incorrect tasks configuration")
}

sealed class TaCallRequestExport {
    abstract val id: CallRequestId

    data class Overview(
        override val id: CallRequestId
    ) : TaCallRequestExport()

    data class Comments(
        override val id: CallRequestId
    ) : TaCallRequestExport()
}
