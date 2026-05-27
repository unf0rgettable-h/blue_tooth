package com.unforgettable.bluetoothcollector.ui.collector

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unforgettable.bluetoothcollector.domain.model.InstrumentBrand
import com.unforgettable.bluetoothcollector.domain.model.InstrumentModel

/**
 * 仪器品牌/型号选择区域。
 *
 * 该组件只负责选择 UI；型号对应的协议能力由 CollectorUiState/ImportProfileRegistry 决定。
 */
@Composable
internal fun InstrumentPanel(
    brands: List<InstrumentBrand>,
    models: List<InstrumentModel>,
    selectedBrandId: String?,
    selectedModelId: String?,
    selectionLocked: Boolean,
    onBrandSelected: (String) -> Unit,
    onModelSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PanelCard(
        modifier = modifier,
        title = "仪器选择",
        subtitle = "左侧先定品牌与型号，后续收数使用型号的默认分隔策略。",
    ) {
        DropdownSelector(
            label = "仪器品牌",
            options = brands,
            selectedId = selectedBrandId,
            enabled = !selectionLocked,
            displayName = { it.displayName },
            optionId = { it.id },
            onSelect = onBrandSelected,
        )
        Spacer(modifier = Modifier.height(12.dp))
        DropdownSelector(
            label = "仪器型号",
            options = models,
            selectedId = selectedModelId,
            enabled = !selectionLocked,
            displayName = { it.displayName },
            optionId = { it.modelId },
            onSelect = onModelSelected,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (selectedModelId == null) {
                "尚未选择型号。"
            } else {
                "当前型号：$selectedModelId"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
