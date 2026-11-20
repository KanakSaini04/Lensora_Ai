package com.lensora.ai

import com.lensora.ai.ai.RealEsrganEngine
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class RealEsrganEngineTest {

    @Test
    fun constants_verifyTileSizesAndScale() {
        assertEquals(128, RealEsrganEngine.TILE_IN_SIZE)
        assertEquals(512, RealEsrganEngine.TILE_OUT_SIZE)
        assertEquals(4, RealEsrganEngine.SCALE_FACTOR)
    }

    @Test
    fun bufferToBitmap_validBuffer_createsCorrectBitmapDimensions() {
        val width = 512
        val height = 512
        val numPixels = width * height
        val buffer = ByteBuffer.allocateDirect(1 * width * height * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        for (i in 0 until numPixels * 3) {
            buffer.putFloat(0.5f) // 50% gray
        }
        buffer.rewind()

        val bitmap = RealEsrganEngine.bufferToBitmap(buffer, width, height)
        assertEquals(512, bitmap.width)
        assertEquals(512, bitmap.height)
        bitmap.recycle()
    }
}
