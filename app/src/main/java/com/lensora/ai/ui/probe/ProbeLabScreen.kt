package com.lensora.ai.ui.probe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lensora.ai.domain.probe.CameraProbeP1
import com.lensora.ai.domain.probe.CameraProbeP2
import com.lensora.ai.domain.probe.CameraProbeP3
import com.lensora.ai.domain.probe.ProbeResult
import com.lensora.ai.domain.probe.ProbeStatus
import com.lensora.ai.ui.theme.CharcoalSurface
import com.lensora.ai.ui.theme.ColorMutedGray
import com.lensora.ai.ui.theme.ColorWhite
import com.lensora.ai.ui.theme.PitchBlackBackground
import com.lensora.ai.ui.theme.RoyalBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProbeLabScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var probeResults by remember { mutableStateOf<List<ProbeResult>>(emptyList()) }
    var isRunning by remember { mutableStateOf(false) }

    fun runProbes() {
        isRunning = true
        val p1 = CameraProbeP1().execute(context)
        val p2 = CameraProbeP2().execute(context)
        val p3 = CameraProbeP3().execute(context)
        probeResults = listOf(p1, p2, p3)
        isRunning = false
    }

    LaunchedEffect(Unit) {
        runProbes()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Hardware Probe Lab",
                        color = ColorWhite,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ColorWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CharcoalSurface
                )
            )
        },
        containerColor = PitchBlackBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Camera2 Hardware Audit",
                            style = MaterialTheme.typography.titleLarge,
                            color = ColorWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Audits device HAL capabilities (200MP, RAW, 24fps) without opening camera devices.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = ColorMutedGray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { runProbes() },
                            enabled = !isRunning,
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = ColorWhite
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRunning) "Running Probes..." else "Run All Probes",
                                color = ColorWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            items(probeResults) { result ->
                ProbeResultCard(result = result)
            }
        }
    }
}

@Composable
private fun ProbeResultCard(result: ProbeResult) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = ColorWhite,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(status = result.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = result.details,
                style = MaterialTheme.typography.bodyLarge,
                color = ColorMutedGray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Execution time: ${result.executionTimeMs} ms",
                fontSize = 11.sp,
                color = ColorMutedGray
            )
        }
    }
}

@Composable
private fun StatusBadge(status: ProbeStatus) {
    val (bgColor, textColor, text) = when (status) {
        ProbeStatus.PASS -> Triple(Color(0xFF10B981), Color.White, "PASS")
        ProbeStatus.FAIL -> Triple(Color(0xFFEF4444), Color.White, "FAIL")
        ProbeStatus.UNSUPPORTED -> Triple(Color(0xFFF59E0B), Color.Black, "UNSUPPORTED")
        ProbeStatus.ERROR -> Triple(Color(0xFFDC2626), Color.White, "ERROR")
    }

    Box(
        modifier = Modifier
            .background(color = bgColor, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ProbeLabScreenPreview() {
    com.lensora.ai.ui.theme.Lensora_AiTheme {
        ProbeLabScreen()
    }
}
