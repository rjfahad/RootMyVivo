package com.rootmyvivo.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.rootmyvivo.R
import com.rootmyvivo.data.AppUpdate
import com.rootmyvivo.vm.UpdateDownloadState

/**
 * Диалог обновления: версия, размер, список изменений, кнопки
 * «Обновить» / «Позже». Во время скачивания — прогресс-бар с мегабайтами;
 * после скачивания кнопка превращается в «Установить» (переоткрыть
 * системный установщик).
 *
 * Изменение размера при нажатии «Обновить» — плавное и симметричное:
 * высота ОКНА ведётся вручную (Animatable + setLayout каждый кадр), окно
 * центрировано, поэтому верхняя и нижняя кромки движутся одновременно.
 * Кнопки привязаны к низу (едут вместе с нижней кромкой), а текст меняется
 * кроссфейдом с той же длительностью, что и анимация размера. Естественные
 * высоты обоих состояний меряются скрытыми копиями без обратной связи
 * (unbounded-замер) — иначе окно ограничивает контент и цель съедает сама
 * себя. Обычный animateContentSize здесь принципиально не работает: окно
 * пересчитывалось в финальный размер одним кадром.
 */
private const val BODY_ANIM_MS = 280

@Composable
fun UpdateDialog(
    update: AppUpdate,
    download: UpdateDownloadState?,
    onUpdate: () -> Unit,
    onCancel: () -> Unit,
) {
    val density = LocalDensity.current
    // Естественные высоты двух состояний (px): с чейнджлогом и с прогрессом
    var changelogPx by remember { mutableStateOf(0) }
    var progressPx by remember { mutableStateOf(0) }
    val showProgress = download != null
    val targetPx = when {
        showProgress && progressPx > 0 -> progressPx
        !showProgress && changelogPx > 0 -> changelogPx
        else -> 0
    }

    val animH = remember { Animatable(0f) }
    LaunchedEffect(targetPx) {
        if (targetPx <= 0) return@LaunchedEffect
        if (animH.value <= 0f) {
            animH.snapTo(targetPx.toFloat())
        } else if (animH.targetValue != targetPx.toFloat()) {
            animH.animateTo(targetPx.toFloat(), tween(BODY_ANIM_MS, easing = FastOutSlowInEasing))
        }
    }

    // Высоту окна ведём ВРУЧНУЮ каждый кадр анимации: окно центрируется
    // WindowManager'ом, поэтому верх и низ движутся одновременно
    val view = LocalView.current
    LaunchedEffect(animH.value) {
        val h = animH.value.toInt()
        if (h <= 0) return@LaunchedEffect
        (view.parent as? DialogWindowProvider)?.window?.let { w ->
            if (w.attributes.height != h) {
                w.attributes = w.attributes.also { it.height = h }
            }
        }
    }

    Dialog(
        onDismissRequest = onCancel,
        // usePlatformDefaultWidth — как у AlertDialog: та же ширина
        properties = DialogProperties(usePlatformDefaultWidth = true),
    ) {
        Box {
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
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                ) {
                    UpdateDialogTitle()
                    UpdateDialogVersion(update)
                    // Тело: кроссфейд чейнджлог ↔ прогресс, одна и та же
                    // длительность, что и у анимации высоты; занимает всё
                    // место между версией и кнопками
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clipToBounds(),
                    ) {
                        Crossfade(
                            targetState = showProgress,
                            animationSpec = tween(BODY_ANIM_MS, easing = FastOutSlowInEasing),
                            label = "updateBody",
                        ) { progress ->
                            if (progress) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    UpdateProgress(download!!)
                                }
                            } else if (update.body.isNotBlank()) {
                                // список изменений из релиза: режем до разумного объёма
                                val body = update.body.trim().take(600)
                                Column(
                                    Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                ) {
                                    Text(body, style = MaterialTheme.typography.bodySmall)
                                }
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

            // ── Скрытые измерители естественных высот (unbounded) ──
            Box(Modifier.height(0.dp).clipToBounds()) {
                Box(Modifier.wrapContentHeight(align = Alignment.Top, unbounded = true)) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .onSizeChanged { changelogPx = it.height },
                    ) {
                        UpdateDialogTitle()
                        UpdateDialogVersion(update)
                        if (update.body.isNotBlank()) {
                            Column(Modifier.height(160.dp)) {
                                Text(
                                    update.body.trim().take(600),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            TextButton(onClick = {}, enabled = true) {
                                Text(stringResource(R.string.update_cancel))
                            }
                            Button(onClick = {}, enabled = true) {
                                Text(stringResource(R.string.update_button))
                            }
                        }
                    }
                }
            }
            Box(Modifier.height(0.dp).clipToBounds()) {
                Box(Modifier.wrapContentHeight(align = Alignment.Top, unbounded = true)) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .onSizeChanged { progressPx = it.height },
                    ) {
                        UpdateDialogTitle()
                        UpdateDialogVersion(update)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            UpdateProgress(UpdateDownloadState("", 0, 1))
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            TextButton(onClick = {}, enabled = true) {
                                Text(stringResource(R.string.update_cancel))
                            }
                            Button(onClick = {}, enabled = true) {
                                Text(stringResource(R.string.update_button))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Заголовок с иконкой — общий для видимого диалога и измерителей. */
@Composable
private fun UpdateDialogTitle() {
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
}

/** Строка версии и размера — общий для видимого диалога и измерителей. */
@Composable
private fun UpdateDialogVersion(update: AppUpdate) {
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
