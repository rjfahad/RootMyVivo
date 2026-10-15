package com.rootmyvivo.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rootmyvivo.R
import com.rootmyvivo.data.AppUpdate
import com.rootmyvivo.vm.UpdateDownloadState

/**
 * Диалог обновления: версия, размер, список изменений, кнопки
 * «Обновить» / «Позже». Во время скачивания — прогресс-бар с мегабайтами;
 * после скачивания кнопка превращается в «Установить» (переоткрыть
 * системный установщик).
 */
@Composable
fun UpdateDialog(
    update: AppUpdate,
    download: UpdateDownloadState?,
    onUpdate: () -> Unit,
    onCancel: () -> Unit,
) {
    Dialog(
        onDismissRequest = onCancel,
        // usePlatformDefaultWidth — как у AlertDialog: та же ширина
        properties = DialogProperties(usePlatformDefaultWidth = true),
    ) {
        // animateContentSize на ВСЕЙ поверхности диалога: окно центрируется
        // и следует за каждым кадром анимации, поэтому размер меняется
        // симметрично с обеих сторон. Внутри AlertDialog верх «подпрыгивал»
        // (слоты пересчитывались мгновенно), а низ доезжал анимацией.
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.CloudDownload, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        stringResource(R.string.update_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.update_version, update.versionName) +
                            if (update.apkSize > 0) {
                                "  ·  " + stringResource(
                                    R.string.update_size,
                                    "%.1f".format(update.apkSize / 1048576.0),
                                )
                            } else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (download != null) {
                        UpdateProgress(download)
                    } else if (update.body.isNotBlank()) {
                        // список изменений из релиза: режем до разумного объёма
                        val body = update.body.trim().take(600)
                        Column(
                            Modifier
                                .height(160.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            Text(body, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.align(Alignment.End),
                ) {
                    TextButton(onClick = onCancel, enabled = download == null || download.done) {
                        Text(stringResource(R.string.update_cancel))
                    }
                    Button(onClick = onUpdate, enabled = download == null || download.done) {
                        Text(
                            if (download != null && download.done) {
                                stringResource(R.string.update_retry_install)
                            } else {
                                stringResource(R.string.update_button)
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Прогресс скачивания: полоса + счётчик мегабайт. */
@Composable
fun UpdateProgress(dl: UpdateDownloadState) {
    val mb = "%.1f / %.1f МБ".format(dl.read / 1048576.0, dl.total / 1048576.0)
    when (val f = dl.fraction) {
        null -> {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(mb, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> {
            LinearProgressIndicator(
                progress = { f.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(mb, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
