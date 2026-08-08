package ru.pokolenie.app.presentation.components

import android.media.MediaPlayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier as ComposeModifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.pokolenie.app.R

/**
 * Mortal Kombat-style Toasty: лицо снизу-справа + звук при подключении VPN.
 */
@Composable
fun ToastyOverlay(
    visible: Boolean,
    onFinished: () -> Unit
) {
    if (!visible) return
    val context = LocalContext.current
    val offsetX = remember { Animatable(220f) }
    val alpha = remember { Animatable(0f) }

    DisposableEffect(visible) {
        var player: MediaPlayer? = null
        runCatching {
            player = MediaPlayer.create(context, R.raw.toasty)?.also {
                it.setOnCompletionListener { mp -> mp.release() }
                it.start()
            }
        }
        onDispose {
            runCatching {
                player?.stop()
                player?.release()
            }
        }
    }

    LaunchedEffect(visible) {
        offsetX.snapTo(220f)
        alpha.snapTo(0f)
        offsetX.animateTo(0f, tween(180))
        alpha.animateTo(1f, tween(120))
        delay(1100)
        alpha.animateTo(0f, tween(220))
        offsetX.animateTo(220f, tween(220))
        onFinished()
    }

    Box(modifier = ComposeModifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.toasty_face),
            contentDescription = "Toasty",
            modifier = ComposeModifier
                .align(Alignment.BottomEnd)
                .offset(x = offsetX.value.dp, y = (-48).dp)
                .size(160.dp)
        )
    }
}
