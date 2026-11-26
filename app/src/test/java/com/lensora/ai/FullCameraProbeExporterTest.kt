package com.lensora.ai

import com.lensora.ai.domain.probe.FullCameraProbeExporter
import org.junit.Assert.assertNotNull
import org.junit.Test

class FullCameraProbeExporterTest {

    @Test
    fun probeExporterClass_existsAndInstantiable() {
        val exporter = FullCameraProbeExporter()
        assertNotNull(exporter)
    }
}
