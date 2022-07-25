package org.synthesis.calls.request.flow

import java.io.OutputStream
import org.synthesis.calls.Call
import org.synthesis.calls.CallException
import org.synthesis.calls.CallId
import org.synthesis.calls.CallType
import org.synthesis.calls.request.ta.export.TaCallRequestExport
import org.synthesis.calls.request.ta.export.TaCallRequestExporter
import org.synthesis.calls.request.ta.flow.TaCallRequestFlow
import org.synthesis.calls.request.ta.presenter.TaCallRequestPresenter
import org.synthesis.calls.request.va.export.VaCallRequestExport
import org.synthesis.calls.request.va.export.VaCallRequestExporter
import org.synthesis.calls.request.va.flow.VaCallRequestFlow
import org.synthesis.calls.request.va.presenter.VaCallRequestPresenter
import org.synthesis.calls.store.CallFinder

class CallRequestFlowProxy(
    private val callFinder: CallFinder,
    private val vaCallRequestFlow: VaCallRequestFlow,
    private val taCallRequestFlow: TaCallRequestFlow,
    private val vaCallRequestPresenter: VaCallRequestPresenter,
    private val taCallRequestPresenter: TaCallRequestPresenter,
    private val taCallRequestExporter: TaCallRequestExporter,
    private val vaCallRequestExporter: VaCallRequestExporter,
) {
    /**
     * @throws [CallException.CallNotFound]
     * @throws [CallException.FlowNotImplemented]
     */
    suspend fun handle(command: CallRequestCommand): Any? {
        val call = loadCall(command)

        return when (call.type()) {
            CallType.VA -> vaCallRequestFlow.handle(call, command)
            CallType.TA -> taCallRequestFlow.handle(call, command)
        }
    }

    /**
     * @throws [CallException.CallNotFound]
     * @throws [CallException.FlowNotImplemented]
     */
    suspend fun overviewExportHandle(command: CallRequestCommand, to: OutputStream): Any? {
        val call = loadCall(command)

        return command.callRequestId?.let {
            when (call.type()) {
                CallType.VA -> vaCallRequestExporter.handle(
                    VaCallRequestExport.Overview(id = it),
                    to
                )
                CallType.TA -> taCallRequestExporter.handle(
                    TaCallRequestExport.Overview(id = it),
                    to
                )
            }
        }
    }

    /**
     * @throws [CallException.CallNotFound]
     * @throws [CallException.FlowNotImplemented]
     */
    suspend fun commentExportHandle(command: CallRequestCommand, to: OutputStream): Any? {
        val call = loadCall(command)

        return command.callRequestId?.let {
            when (call.type()) {
                CallType.VA -> vaCallRequestExporter.handle(
                    VaCallRequestExport.Comments(id = it),
                    to
                )
                CallType.TA -> taCallRequestExporter.handle(
                    TaCallRequestExport.Comments(id = it),
                    to
                )
            }
        }
    }

    suspend fun find(command: CallRequestCommand) {
        val call = loadCall(command)

        when (call.type()) {
            CallType.VA -> command.callRequestId?.let { vaCallRequestPresenter.find(it) }
            CallType.TA -> command.callRequestId?.let { taCallRequestPresenter.find(it) }
        }
    }

    private suspend fun loadCall(command: CallRequestCommand): Call {
        val callId = command.callId
        val callRequestId = command.callRequestId

        return (callId?.let { callFinder.find(it) } ?: callRequestId?.let { callFinder.find(it) })
            ?: throw CallException.CallNotFound(
                callId?.let {
                    "Call with id `${it.uuid}` was not found"
                } ?: "Call for request `${callRequestId?.uuid}` was not found"
            )
    }

    private suspend fun loadCall(callId: CallId): Call {
        return (callId.let { callFinder.find(it) }
            ?: throw CallException.CallNotFound(
                "Call with id `${callId.uuid}` was not found"
            ))
    }
}
