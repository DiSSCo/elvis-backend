package org.synthesis.calls.request.ta.scoring

class ScorePresenter(
    private val scoreFinder: ScoreFinder
) {
    suspend fun find(id: ScoreFormId): ScoreResponse? = scoreFinder.find(id)
}
