package app.aegis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.aegis.domain.model.TrustedContact
import app.aegis.ui.theme.AegisTheme
import app.aegis.ui.theme.AegisTypography
import app.aegis.ui.viewmodel.TrustedContactViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Trusted Contacts Screen - Manage emergency contacts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrustedContactsScreen(
    onBackClick: () -> Unit = {},
    viewModel: TrustedContactViewModel = koinViewModel()
) {
    val colors = AegisTheme.colors

    // Collect contacts from ViewModel
    val contacts by viewModel.contacts.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Trusted Contacts",
                            style = AegisTypography.headlineMedium,
                            color = colors.textPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = colors.textPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.background
                    )
                )
                HorizontalDivider(color = colors.divider, thickness = 1.dp)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = colors.primary,
                contentColor = Color(0xFF121212)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Contact"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp)
        ) {
            // Info Card - Hidden as requested
            /*
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.primaryContainer)
                    .padding(16.dp)
            ) {
                Text(
                    text = "About Trusted Contacts",
                    style = AegisTypography.titleMedium,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Trusted contacts can receive emergency alerts when a threat is detected. They will also be notified if you don't respond to safety checks.",
                    style = AegisTypography.bodySmall,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            */

            Text(
                text = "YOUR CONTACTS (${contacts.size})",
                style = AegisTypography.overline,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (contacts.isEmpty()) {
                EmptyContactsPlaceholder()
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(contacts, key = { it.id }) { contact ->
                        TrustedContactItem(
                            contact = TrustedContactDisplayItem(
                                id = contact.id,
                                name = contact.name,
                                phoneNumber = contact.phoneNumber,
                                relationship = contact.relationship
                            ),
                            onDelete = {
                                viewModel.deleteContact(contact.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Contact Dialog
    if (showAddDialog) {
        AddContactDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, phone, relationship ->
                val newContact = TrustedContact(
                    id = System.currentTimeMillis().toString(),
                    name = name,
                    phoneNumber = phone,
                    relationship = relationship,
                    addedAt = System.currentTimeMillis()
                )
                viewModel.addContact(newContact)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun TrustedContactItem(
    contact: TrustedContactDisplayItem,
    onDelete: () -> Unit
) {
    val colors = AegisTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                style = AegisTypography.titleMedium,
                color = colors.textPrimary
            )
            Text(
                text = contact.phoneNumber,
                style = AegisTypography.bodySmall,
                color = colors.textSecondary
            )
            Text(
                text = contact.relationship,
                style = AegisTypography.caption,
                color = colors.primary
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = colors.error
            )
        }
    }
}

@Composable
private fun EmptyContactsPlaceholder() {
    val colors = AegisTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No trusted contacts yet",
            style = AegisTypography.titleMedium,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap the + button to add your first contact",
            style = AegisTypography.bodySmall,
            color = colors.textTertiary
        )
    }
}

@Composable
private fun AddContactDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, phone: String, relationship: String) -> Unit
) {
    val colors = AegisTheme.colors
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                text = "Add Trusted Contact",
                style = AegisTypography.headlineSmall,
                color = colors.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("Relationship (e.g., Family, Friend)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onAdd(name, phone, relationship.ifBlank { "Contact" })
                    }
                },
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) {
                Text("Add", color = colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}

/**
 * Display model for trusted contacts
 */
data class TrustedContactDisplayItem(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val relationship: String
)
