package com.lensora.ai

import com.lensora.ai.ai.TiledPipelineMath
import org.junit.Assert.assertEquals
import org.junit.Test

class TiledPipelineEngineTest {

    @Test
    fun calculateOutputDimensions_1xScale_returnsSameDimensions() {
        val (w, h) = TiledPipelineMath.calculateOutputDimensions(1024, 1024, 1)
        assertEquals(1024, w)
        assertEquals(1024, h)
    }

    @Test
    fun calculateOutputDimensions_2xScaleRealEsrgan_returnsDoubleDimensions() {
        val (w, h) = TiledPipelineMath.calculateOutputDimensions(1024, 1024, 2)
        assertEquals(2048, w)
        assertEquals(2048, h)
    }

    @Test
    fun calculateTileGrid_12MBImage512TileSize_calculatesCorrectTileCount() {
        // 12.5 MP image (4080x3060) with 512x512 tiles
        val (cols, rows) = TiledPipelineMath.calculateTileGrid(4080, 3060, 512)
        assertEquals(8, cols) // (4080 + 511) / 512 = 8
        assertEquals(6, rows) // (3060 + 511) / 512 = 6
        assertEquals(48, cols * rows)
    }
}
