package app.aegis.domain.repository

import app.aegis.domain.model.TrustedContact
import kotlinx.coroutines.flow.Flow

interface TrustedContactRepository {
    fun getAllContacts(): Flow<List<TrustedContact>>
    suspend fun addContact(contact: TrustedContact)
    suspend fun deleteContact(contact: TrustedContact)
    suspend fun getContactById(id: String): TrustedContact?
}
