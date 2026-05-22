package app.aegis.data.repository

import app.aegis.data.local.dao.TrustedContactDao
import app.aegis.data.local.entities.toDomain
import app.aegis.data.local.entities.toEntity
import app.aegis.domain.model.TrustedContact
import app.aegis.domain.repository.TrustedContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TrustedContactRepositoryImpl(
    private val dao: TrustedContactDao
) : TrustedContactRepository {
    override fun getAllContacts(): Flow<List<TrustedContact>> {
        return dao.getAllContacts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addContact(contact: TrustedContact) {
        dao.insertContact(contact.toEntity())
    }

    override suspend fun deleteContact(contact: TrustedContact) {
        dao.deleteContact(contact.toEntity())
    }

    override suspend fun getContactById(id: String): TrustedContact? {
        return dao.getContactById(id)?.toDomain()
    }

    override suspend fun isTrusted(identifier: String): Boolean {
        return dao.checkIsTrusted(identifier) > 0
    }
}
