package com.rootmyvivo.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
 *
 * Изменение размера — СИММЕТРИЧНОЕ и плавное: естественная высота контента
 * меряется без обратной связи (обёртка wrapContentHeight(unbounded)), высота
 * поверхности ведётся Animatable'ом, окно диалога следует за каждым кадром
 * и центрируется — верхняя и нижняя кромки движутся одновременно. Обычный
 * animateContentSize здесь не работал: окно пересчитывалось в финальный
 * размер одним кадром (сырой срез снизу + подтягивание верха).
 */
@Composable
fun UpdateDialog(
    update: AppUpdate,
    download: UpdateDownloadState?,
    onUpdate: () -> Unit,
    onCancel: () -> Unit,
) {
    val density = LocalDensity.current
    // Естественная высота контента (px), меряется независимо от окна
    var naturalPx by remember { mutableStateOf(0) }
    val animH = remember { Animatable(0f) }

    // Цель изменилась → ведём высоту плавно; первый замер — мгновенно
    LaunchedEffect(naturalPx) {
        if (naturalPx <= 0) return@LaunchedEffect
        if (animH.value <= 0f) {
            animH.snapTo(naturalPx.toFloat())
        } else if (animH.targetValue != naturalPx.toFloat()) {
            animH.animateTo(naturalPx.toFloat(), tween(280, easing = FastOutSlowInEasing))
        }
    }

    Dialog(
        onDismissRequest = onCancel,
        // usePlatformDefaultWidth — как у AlertDialog: та же ширина
        properties = DialogProperties(usePlatformDefaultWidth = true),
    ) {
        val hPx = animH.value.toInt()
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (hPx > 0) {
                        Modifier.height(with(density) { hPx.toDp() })
                    } else {
                        Modifier
                    },
                ),
        ) {
            // Контент меряется БЕЗ ограничения по высоте (unbounded): его
            // естественная высота — цель анимации. Поверхность клипит по
            // форме — при «росте» контент раскрывается, при «сжатии»
            // плавно уезжает
            Box(
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(align = Alignment.Top, unbounded = true),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .onSizeChanged { naturalPx = it.height },
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
