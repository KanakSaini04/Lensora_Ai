package com.lensora.ai.ui.camera

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lensora.ai.ui.theme.CharcoalSurface
import com.lensora.ai.ui.theme.ColorMutedGray
import com.lensora.ai.ui.theme.ColorWhite
import com.lensora.ai.ui.theme.PitchBlackBackground
import com.lensora.ai.ui.theme.RoyalBlue
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ProModeControls(
    isoValue: Int,
    shutterSpeedNs: Long,
    isRawEnabled: Boolean,
    onIsoChanged: (Int) -> Unit,
    onShutterSpeedChanged: (Long) -> Unit,
    onRawToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface.copy(alpha = 0.90f)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(width = 1.dp, color = RoyalBlue.copy(alpha = 0.4f), shape = RoundedCornerShape(16.dp)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "\u2699 PRO MODE EXPOSURE",
                    color = RoyalBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1. ISO Slider (100 to 3200)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "ISO",
                    color = ColorWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(0.25f),
                )
                Text(
                    text = "$isoValue",
                    color = RoyalBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(0.25f),
                )
            }

            Slider(
                value = isoValue.toFloat(),
                onValueChange = { onIsoChanged(it.roundToInt()) },
                valueRange = 100f..3200f,
                steps = 30,
                colors = SliderDefaults.colors(
                    thumbColor = ColorWhite,
                    activeTrackColor = RoyalBlue,
                    inactiveTrackColor = PitchBlackBackground,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Shutter Speed Slider (1/1000s to 1/30s -> 1,000,000 ns to 33,333,333 ns)
            val minShutterNs = 1_000_000L // 1/1000s
            val maxShutterNs = 33_333_333L // 1/30s
            val shutterFraction = (shutterSpeedNs - minShutterNs).toFloat() / (maxShutterNs - minShutterNs).toFloat()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "SHUTTER",
                    color = ColorWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(0.35f),
                )
                Text(
                    text = formatShutterSpeedText(shutterSpeedNs),
                    color = RoyalBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(0.65f),
                )
            }

            Slider(
                value = shutterFraction.coerceIn(0f, 1f),
                onValueChange = { fraction ->
                    val newShutterNs = (minShutterNs + fraction * (maxShutterNs - minShutterNs)).toLong()
                    onShutterSpeedChanged(newShutterNs)
                },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = ColorWhite,
                    activeTrackColor = RoyalBlue,
                    inactiveTrackColor = PitchBlackBackground,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Save RAW (DNG) Switch Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "SAVE RAW (DNG)",
                    color = ColorWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
                Switch(
                    checked = isRawEnabled,
                    onCheckedChange = onRawToggled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ColorWhite,
                        checkedTrackColor = RoyalBlue,
                        uncheckedThumbColor = ColorMutedGray,
                        uncheckedTrackColor = PitchBlackBackground,
                    ),
                )
            }
        }
    }
}

private fun formatShutterSpeedText(shutterNs: Long): String {
    val seconds = shutterNs / 1_000_000_000.0
    return if (seconds < 1.0) {
        val denominator = (1.0 / seconds).roundToInt()
        "1/${denominator}s (${(shutterNs / 1_000_000)}ms)"
    } else {
        String.format(Locale.US, "%.1fs", seconds)
    }
}
