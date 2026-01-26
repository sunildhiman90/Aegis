package app.aegis.ui.components

import aegis.composeapp.generated.resources.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.aegis.domain.model.AppLanguageItem
import app.aegis.ui.theme.AegisTheme
import app.aegis.ui.theme.AegisTypography
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Bottom Sheet for selecting app language
 * Matches the design with globe icon, subtitle, language list with radio buttons,
 * and Cancel/Apply buttons at the bottom
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionBottomSheet(
    languages: List<AppLanguageItem>,
    currentLanguageCode: String,
    onDismissRequest: () -> Unit,
    onApply: (AppLanguageItem) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val colors = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = colors.surface,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Globe icon and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = colors.onSurface,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(Res.string.select_language),
                    style = AegisTypography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle
            Text(
                text = stringResource(Res.string.settings_language_desc),
                style = AegisTypography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Language list - applies immediately on selection
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(languages) { language ->
                    LanguageItemRow(
                        language = language,
                        isSelected = language.code == currentLanguageCode,
                        onClick = {
                            // Apply immediately and close
                            onApply(language)
                            onDismissRequest()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageItemRow(
    language: AppLanguageItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = colors.surface,
        shape = RoundedCornerShape(10.dp),
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Flag icon
            Icon(
                painter = painterResource(language.flagDrawable),
                contentDescription = language.name,
                modifier = Modifier.size(20.dp),
                tint = Color.Unspecified,
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Language name
            Text(
                text = language.name,
                style = AegisTypography.bodyMedium,
                color = colors.onSurface,
                modifier = Modifier.weight(1f),
            )

            // Radio button
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                modifier = Modifier.size(24.dp),
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFF2563EB),
                    unselectedColor = Color(0xFFBDBDBD),
                ),
            )
        }
    }
}
