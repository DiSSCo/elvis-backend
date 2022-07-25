@file:Suppress("BlockingMethodInNonBlockingContext")

package org.synthesis.institution.import

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.synthesis.institution.GRID

data class ImportedInstitutionData(
    val title: String
)

sealed class InstitutionImportException(message: String?) : Exception(message) {
    class InteractionFailed(message: String?) : InstitutionImportException(message)
    class NotFound(grid: GRID) :
        InstitutionImportException("Institute with identifier `$grid` not found in GRID database")
}

interface InstitutionImporter {

    /**
     * Getting information about the institute
     *
     * @throws [InstitutionImportException.InteractionFailed]
     * @throws [InstitutionImportException.NotFound]
     */
    suspend fun obtain(grid: GRID): ImportedInstitutionData
}

object GridImporter : InstitutionImporter {
    private const val endpointURL = "https://www.grid.ac/institutes/"

    override suspend fun obtain(grid: GRID) = withContext(Dispatchers.IO) {

        val url = "$endpointURL/${grid.value}"

        try {
            val responseElement = Jsoup.connect(url).get().allElements.first()
            val institutionData = responseElement.hydrate()

            if (institutionData.isValid()) {
                return@withContext institutionData
            }

            throw InstitutionImportException.NotFound(grid)
        } catch (e: Exception) {
            throw InstitutionImportException.InteractionFailed(e.message)
        }
    }
}

private fun Element.hydrate() = ImportedInstitutionData(
    title = select("body > div.wrapper > div.content > div > header > div > div > h1 > span").text()
)

private fun ImportedInstitutionData.isValid() = title.isNotEmpty()
