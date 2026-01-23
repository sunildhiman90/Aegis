package app.aegis.ui.components

import aegis.composeapp.generated.resources.Res
import aegis.composeapp.generated.resources.action_cancel
import aegis.composeapp.generated.resources.select_language
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.aegis.domain.model.AppLanguageItem
import app.aegis.ui.theme.AegisTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Dialog for selecting app language
 */
@Composable
fun LanguageSelectionDialog(
    languages: List<AppLanguageItem>,
    showDialog: Boolean,
    selectedLanguage: String,
    onDismissRequest: () -> Unit,
    onLanguageSelected: (AppLanguageItem) -> Unit,
) {
    if (!showDialog) return

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AegisTheme.colors.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(Res.string.select_language),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(languages) { language ->
                        LanguageItem(
                            language = language,
                            isSelected = language.name == selectedLanguage,
                            onLanguageClick = {
                                onLanguageSelected(language)
                                onDismissRequest()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = stringResource(Res.string.action_cancel),
                        color = AegisTheme.colors.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageItem(
    language: AppLanguageItem,
    isSelected: Boolean,
    onLanguageClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onLanguageClick)
            .background(
                if (isSelected) AegisTheme.colors.primaryContainer
                else AegisTheme.colors.surface
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Flag icon
        Icon(
            painter = painterResource(language.flagDrawable),
            contentDescription = language.name,
            modifier = Modifier.size(24.dp),
            tint = androidx.compose.ui.graphics.Color.Unspecified
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = language.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        if (isSelected) {
            RadioButton(
                selected = true,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = AegisTheme.colors.primary
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}
