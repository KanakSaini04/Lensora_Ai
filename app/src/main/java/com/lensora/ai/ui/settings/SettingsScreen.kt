package com.lensora.ai.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lensora.ai.ui.theme.CharcoalSurface
import com.lensora.ai.ui.theme.ColorMutedGray
import com.lensora.ai.ui.theme.ColorWhite
import com.lensora.ai.ui.theme.PitchBlackBackground
import com.lensora.ai.ui.theme.RoyalBlue

@Composable
fun SettingsScreen(
    onOpenProbeLab: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlackBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            color = ColorWhite,
            fontWeight = FontWeight.Bold
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Hardware Diagnostics",
                    style = MaterialTheme.typography.titleLarge,
                    color = ColorWhite,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Audit Camera2 sensor capabilities (P1: 200MP, P2: RAW, P3: 24fps) safely.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ColorMutedGray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onOpenProbeLab,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Science,
                        contentDescription = null,
                        tint = ColorWhite
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Launch Probe Lab",
                        color = ColorWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
