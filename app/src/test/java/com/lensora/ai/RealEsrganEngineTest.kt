package com.lensora.ai

import com.lensora.ai.ai.RealEsrganEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class RealEsrganEngineTest {

    @Test
    fun constants_verifyTileSizesPaddingAndScale() {
        assertEquals(128, RealEsrganEngine.TILE_IN_SIZE)
        assertEquals(512, RealEsrganEngine.TILE_OUT_SIZE)
        assertEquals(4, RealEsrganEngine.SCALE_FACTOR)
        assertEquals(8, RealEsrganEngine.TILE_PAD)
        assertEquals(112, RealEsrganEngine.TILE_CORE)
        assertEquals(32, RealEsrganEngine.CROP_OFFSET)
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
        // Standard 1024x768 -> 10 cols, 7 rows, 70 tiles
        val grid1 = RealEsrganEngine.calculateTileGrid(1024, 768)
        assertEquals(10, grid1.numCols)
        assertEquals(7, grid1.numRows)
        assertEquals(70, grid1.totalTiles)

        // 1530x2040 (halved 3060x4080 source) -> 14 cols, 19 rows, 266 tiles
        val grid2 = RealEsrganEngine.calculateTileGrid(1530, 2040)
        assertEquals(14, grid2.numCols)
        assertEquals(19, grid2.numRows)
        assertEquals(266, grid2.totalTiles)
    }

    @Test
    fun calculatePaddedSize_verifyPaddedSourceDimensions() {
        val grid = RealEsrganEngine.calculateTileGrid(1530, 2040)
        val paddedSize = RealEsrganEngine.calculatePaddedSize(grid)
        assertEquals(1584, paddedSize.width)  // 14 * 112 + 16 = 1584
        assertEquals(2144, paddedSize.height) // 19 * 112 + 16 = 2144
    }

    @Test
    fun calculatePartialEdgeTileBoundsAndCrop_verifyExactPartialTileAt1530x2040() {
        // For a 1530x2040 image at col=13, row=18:
        // Col 13 (left = 13 * 112 = 1456): right = min(1456+112, 1530) = 1530 -> core width = 74
        // Row 18 (top = 18 * 112 = 2016): bottom = min(2016+112, 2040) = 2040 -> core height = 24
        val bounds = RealEsrganEngine.calculateTileBounds(col = 13, row = 18, scaledInWidth = 1530, scaledInHeight = 2040)
        assertEquals(1456, bounds.left)
        assertEquals(2016, bounds.top)
        assertEquals(1530, bounds.right)
        assertEquals(2040, bounds.bottom)
        assertEquals(74, bounds.width)
        assertEquals(24, bounds.height)

        // Crop dimensions for 74x24 core region -> 4x scale = 296x96
        val cropDims = RealEsrganEngine.calculateOutputCropDimensions(bounds.width, bounds.height)
        assertEquals(296, cropDims.cropWidth)
        assertEquals(96, cropDims.cropHeight)

        // Canvas destination coordinates at col=13, row=18
        val destCoords = RealEsrganEngine.calculateDestCoordinates(col = 13, row = 18)
        assertEquals(5824f, destCoords.destLeft) // 1456 * 4
        assertEquals(8064f, destCoords.destTop)  // 2016 * 4
    }
}
