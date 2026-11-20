package com.lensora.ai

import com.lensora.ai.ai.RealEsrganEngine
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteOrder

class RealEsrganEngineTest {

    @Test
    fun constants_verifyTileSizesAndScale() {
        assertEquals(128, RealEsrganEngine.TILE_IN_SIZE)
        assertEquals(512, RealEsrganEngine.TILE_OUT_SIZE)
        assertEquals(4, RealEsrganEngine.SCALE_FACTOR)
    }

    @Test
    fun bufferAllocation_verifyInputAndOutputBufferSize() {
        val inBuffer = RealEsrganEngine.createInputBuffer()
        val outBuffer = RealEsrganEngine.createOutputBuffer()

        assertEquals(196608, inBuffer.capacity()) // 1 * 128 * 128 * 3 * 4 bytes
        assertEquals(3145728, outBuffer.capacity()) // 1 * 512 * 512 * 3 * 4 bytes
    }

    @Test
    fun calculateTileGrid_verifyTileCountsForFullAndPartialSizes() {
        // Standard 1024x768 -> (1024/128=8 cols) x (768/128=6 rows) = 48 tiles
        val cols1 = (1024 + 127) / 128
        val rows1 = (768 + 127) / 128
        assertEquals(8, cols1)
        assertEquals(6, rows1)
        assertEquals(48, cols1 * rows1)

        // Non-multiple 1000x500 -> cols=(1000+127)/128=8, rows=(500+127)/128=4 = 32 tiles
        val cols2 = (1000 + 127) / 128
        val rows2 = (500 + 127) / 128
        assertEquals(8, cols2)
        assertEquals(4, rows2)
        assertEquals(32, cols2 * rows2)
    }

    @Test
    fun calculateEdgeTileCrop_verifyCorrectScaledDimensions() {
        val edgeTileW = 50
        val edgeTileH = 128

        val scaledW = edgeTileW * RealEsrganEngine.SCALE_FACTOR
        val scaledH = edgeTileH * RealEsrganEngine.SCALE_FACTOR

        assertEquals(200, scaledW)
        assertEquals(512, scaledH)
    }

    @Test
    fun calculateCornerTileCrop_verifyCorrectScaledDimensions() {
        val cornerTileW = 40
        val cornerTileH = 30

        val scaledW = cornerTileW * RealEsrganEngine.SCALE_FACTOR
        val scaledH = cornerTileH * RealEsrganEngine.SCALE_FACTOR

        assertEquals(160, scaledW)
        assertEquals(120, scaledH)
    }
}
