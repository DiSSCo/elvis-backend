package org.synthesis.calls

import org.synthesis.calls.contract.CallCommand
import org.synthesis.calls.store.CallFinder
import org.synthesis.calls.store.CallStore
import org.synthesis.infrastructure.persistence.StorageException

class CallProvider(
    private val store: CallStore,
    private val finder: CallFinder
) {
    /**
     * Create a new Call.
     * Returns created Call id.
     *
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun handle(command: CallCommand.Create): CallId {

        var call = Call(
            id = CallId.next(),
            info = CallInfo(
                name = command.name,
                description = command.description,
                additionalInformation = command.additionalInfo,
                scoring = ScoringData(
                    endDate = null,
                    weight = ScoringWeight(
                        methodology = "1",
                        researchExcellence = "1",
                        supportStatement = "1",
                        justification = "1",
                        gainsOutputs = "1",
                        societalChallenge = "1",
                        merit = "1",
                    )
                )
            ),
            lifetime = CallLifeTime.create(
                from = command.startDate,
                to = command.endDate
            ),
            type = CallType.valueOf(command.type)
        )

        if (command.type == "TA") {
            call.apply {
                command.scoringEndDate?.let { addScoringEndDate(it) }
                command.scoringWeight?.let { addScoringWeight(it) }
            }
        }

        store.save(call)

        return call.id()
    }

    /**
     * Update Call information.
     *
     * @throws [CallException.CallNotFound]
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun handle(id: CallId, command: CallCommand.Update) {
        val call = finder.find(id) ?: throw CallException.CallNotFound(id.toString())

        call.apply {
            rename(command.name)
            updateDescription(command.description)
            updateAdditionalInfo(command.additionalInfo)
            changeLifetime(command.startDate, command.endDate)
            command.scoringEndDate?.let { addScoringEndDate(it) }
            command.scoringWeight?.let { addScoringWeight(it) }
        }

        store.save(call)
    }

    /**
     * Mark Call as deleted
     *
     * @throws [CallException.CallNotFound]
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun handle(command: CallCommand.Delete) {
        val call = finder.find(command.id) ?: throw CallException.CallNotFound(command.id.uuid.toString())

        call.delete()

        store.save(call)
    }
}
