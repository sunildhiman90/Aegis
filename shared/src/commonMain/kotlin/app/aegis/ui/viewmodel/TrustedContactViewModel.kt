package app.aegis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.aegis.domain.model.TrustedContact
import app.aegis.domain.repository.TrustedContactRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for Trusted Contacts screen
 */
class TrustedContactViewModel(
    private val repository: TrustedContactRepository
) : ViewModel() {

    val contacts: StateFlow<List<TrustedContact>> = repository.getAllContacts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addContact(contact: TrustedContact) {
        viewModelScope.launch {
            repository.addContact(contact)
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            // Get contact by ID first, then delete
            val contact = repository.getContactById(contactId)
            contact?.let {
                repository.deleteContact(it)
            }
        }
    }
}
