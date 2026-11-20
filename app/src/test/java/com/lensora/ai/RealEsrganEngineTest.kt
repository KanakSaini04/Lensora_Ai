package com.lensora.ai

import com.lensora.ai.ai.RealEsrganEngine
import org.junit.Assert.assertEquals
import org.junit.Test

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
        // Standard 1024x768 -> (8 cols) x (6 rows) = 48 tiles
        val grid1 = RealEsrganEngine.calculateTileGrid(1024, 768)
        assertEquals(8, grid1.numCols)
        assertEquals(6, grid1.numRows)
        assertEquals(48, grid1.totalTiles)

        // 1530x2040 (halved 3060x4080 source) -> (12 cols) x (16 rows) = 192 tiles
        val grid2 = RealEsrganEngine.calculateTileGrid(1530, 2040)
        assertEquals(12, grid2.numCols)
        assertEquals(16, grid2.numRows)
        assertEquals(192, grid2.totalTiles)
    }

    @Test
    fun calculatePartialEdgeTileBoundsAndCrop_verifyExactPartialTileAt1530x2040() {
        // For a 1530x2040 image:
        // Col 11 (last col, left = 11 * 128 = 1408): right = min(1536, 1530) = 1530 -> tileW = 122
        // Row 15 (last row, top = 15 * 128 = 1920): bottom = min(2048, 2040) = 2040 -> tileH = 120
        val bounds = RealEsrganEngine.calculateTileBounds(col = 11, row = 15, scaledInWidth = 1530, scaledInHeight = 2040)
        assertEquals(1408, bounds.left)
        assertEquals(1920, bounds.top)
        assertEquals(1530, bounds.right)
        assertEquals(2040, bounds.bottom)
        assertEquals(122, bounds.width)
        assertEquals(120, bounds.height)

        // Crop dimensions for 122x120 input tile -> 4x scale = 488x480
        val cropDims = RealEsrganEngine.calculateOutputCropDimensions(bounds.width, bounds.height)
        assertEquals(488, cropDims.cropWidth)
        assertEquals(480, cropDims.cropHeight)

        // Canvas destination coordinates at col=11, row=15
        val destCoords = RealEsrganEngine.calculateDestCoordinates(col = 11, row = 15)
        assertEquals(5632f, destCoords.destLeft) // 1408 * 4
        assertEquals(7680f, destCoords.destTop)  // 1920 * 4
    }
}
