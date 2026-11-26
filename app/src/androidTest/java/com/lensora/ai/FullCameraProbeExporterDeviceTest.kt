package com.lensora.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lensora.ai.domain.probe.FullCameraProbeExporter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FullCameraProbeExporterDeviceTest {

    @Test
    fun exportFullCameraProbeReport_onDevice() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val exporter = FullCameraProbeExporter()
        val result = exporter.export(context)

        println("PROBE EXPORT RESULT: success=${result.success}, path=${result.jsonFilePath}, length=${result.jsonString.length}")
        assertTrue("Probe export should succeed on device. Error: ${result.errorMessage}", result.success)
        assertTrue("JSON string should not be empty", result.jsonString.isNotEmpty())
    }
}
