package org.synthesis.institution

import org.synthesis.formbuilder.*
import org.synthesis.infrastructure.persistence.StorageException
import org.synthesis.institution.import.GridImporter
import org.synthesis.institution.import.InstitutionImportException
import org.synthesis.institution.import.InstitutionImporter

class InstitutionProvider(
    private val store: InstitutionStore,
    private val importer: InstitutionImporter = GridImporter
) {

    /**
     * Add a new institution
     *
     * @todo:
     *
     *  - validation GRID
     *  - import wiki data by grid_id
     *  - import cetaf data
     *
     * @throws [InstituteException.IncorrectGRID]
     * @throws [InstituteException.InstitutionAlreadyAdded]
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun handle(command: InstitutionCommand.Add) {
        val id = InstitutionId.fromString(command.id)

        try {
            val institute = Institution(
                id = id,
                form = DynamicForm.create(InstitutionAddressFactory.general()),
                name = command.name,
                cetaf = CETAF(command.cetaf)
            )

            store.add(institute)
        } catch (e: StorageException.UniqueConstraintViolationCheckFailed) {
            throw InstituteException.InstitutionAlreadyAdded(id)
        }
    }

    /**
     * Update information about institution (using GRID database)
     *
     * @throws [InstituteException.IncorrectGRID]
     * @throws [InstituteException.InstitutionNotFound]
     * @throws [StorageException.InteractingFailed]
     * @throws [InstitutionImportException.InteractionFailed]
     * @throws [InstitutionImportException.NotFound]
     */
    suspend fun handle(command: InstitutionCommand.Sync) {
        val id = InstitutionId.fromString(command.id)
        val institution = store.findById(id) ?: throw InstituteException.InstitutionNotFound(id)

        institution.setFieldValue(
            FieldId.fromString("title"),
            FieldValue.Text(importer.obtain(id.grid).title)
        ).also {
            store.save(institution)
        }
    }

    /**
     * Update institution details form.
     *
     * @throws [InstituteException.IncorrectGRID]
     * @throws [InstituteException.InstitutionNotFound]
     * @throws [StorageException.InteractingFailed]
     * @throws [InstitutionImportException.InteractionFailed]
     * @throws [InstitutionImportException.NotFound]
     */
    suspend fun handle(command: InstitutionCommand.Update, id: InstitutionId) {
        val institution = store.findById(id) ?: throw InstituteException.InstitutionNotFound(id)
        if (command.fieldId == "name") {
            when (command.fieldValue) {
                is FieldValue.Text -> {
                    institution.rename(command.fieldValue.value)
                }
                else -> {
                    throw Exception("Wrong type of fieldValue")
                }
            }
        }
        institution.setFieldValue(
            FieldId.fromString(command.fieldId),
            command.fieldValue
        ).also {
            store.save(institution)
        }
    }

    /**
     * Update institution base details.
     *
     * @throws [InstituteException.IncorrectGRID]
     * @throws [InstituteException.InstitutionNotFound]
     * @throws [StorageException.InteractingFailed]
     * @throws [InstitutionImportException.InteractionFailed]
     * @throws [InstitutionImportException.NotFound]
     */
    suspend fun handle(command: InstitutionCommand.ChangeName, id: InstitutionId) {
        val institution = store.findById(id) ?: throw InstituteException.InstitutionNotFound(id)

        institution.apply {
            rename(command.name)
            updateCetaf(CETAF(command.cetaf))
        }.also {
            store.save(institution)
        }
    }

    /**
     * Remove institution details form
     *
     * @throws [InstituteException.IncorrectGRID]
     * @throws [InstituteException.InstitutionNotFound]
     * @throws [StorageException.InteractingFailed]
     * @throws [InstitutionImportException.InteractionFailed]
     * @throws [InstitutionImportException.NotFound]
     */
    suspend fun handle(command: InstitutionCommand.RemoveGroup, id: InstitutionId) {
        val institution = store.findById(id) ?: throw InstituteException.InstitutionNotFound(id)
        val group = GroupId.fromString(command.groupId)

        institution.removeGroup(group)

        store.save(institution)
    }
}
