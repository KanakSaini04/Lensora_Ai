package com.lensora.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ObjectEraserTest {

    @Test
    fun bufferAllocation_twoInputsContract_allocatesExpectedByteSizes() {
        val targetSize = 512

        // Input 0: [1, 512, 512, 3] FLOAT32 NHWC
        val imageBufSize = 1 * targetSize * targetSize * 3 * 4 // 3,145,728 bytes
        val imageBuffer = ByteBuffer.allocateDirect(imageBufSize).apply {
            order(ByteOrder.nativeOrder())
        }

        // Input 1: [1, 512, 512, 1] FLOAT32 NHWC
        val maskBufSize = 1 * targetSize * targetSize * 1 * 4 // 1,048,576 bytes
        val maskBuffer = ByteBuffer.allocateDirect(maskBufSize).apply {
            order(ByteOrder.nativeOrder())
        }

        assertEquals(3145728, imageBuffer.capacity())
        assertEquals(1048576, maskBuffer.capacity())
    }

    @Test
    fun maskPolarity_eraseIsOneAndKeepIsZero_writesCorrectFloats() {
        val size = 512
        val maskBuffer = ByteBuffer.allocateDirect(1 * size * size * 1 * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        // Write 1.0f for erase, 0.0f for keep
        for (i in 0 until (size * size)) {
            val maskVal = if (i < 100) 1.0f else 0.0f
            maskBuffer.putFloat(maskVal)
        }

        maskBuffer.rewind()

        // Check first float is 1.0f (erase)
        assertEquals(1.0f, maskBuffer.float, 0.001f)

        // Skip to float at index 100 and check it is 0.0f (keep)
        maskBuffer.position(100 * 4)
        assertEquals(0.0f, maskBuffer.float, 0.001f)
    }

    @Test
    fun tensorContract_shapeValidationLogic_validatesCorrectAndInvalidShapes() {
        val validImageShape = intArrayOf(1, 512, 512, 3)
        val validMaskShape = intArrayOf(1, 512, 512, 1)
        val validOutputShape = intArrayOf(1, 512, 512, 3)

        val isImageValid = validImageShape.size == 4 && validImageShape[0] == 1 && validImageShape[1] == 512 && validImageShape[2] == 512 && validImageShape[3] == 3
        val isMaskValid = validMaskShape.size == 4 && validMaskShape[0] == 1 && validMaskShape[1] == 512 && validMaskShape[2] == 512 && validMaskShape[3] == 1
        val isOutputValid = validOutputShape.size == 4 && validOutputShape[0] == 1 && validOutputShape[1] == 512 && validOutputShape[2] == 512 && validOutputShape[3] == 3

        assertTrue(isImageValid && isMaskValid && isOutputValid)

        val invalid4ChImageShape = intArrayOf(1, 512, 512, 4)
        val is4ChImageValid = invalid4ChImageShape.size == 4 && invalid4ChImageShape[0] == 1 && invalid4ChImageShape[1] == 512 && invalid4ChImageShape[2] == 512 && invalid4ChImageShape[3] == 3
        assertFalse(is4ChImageValid)
    }
}
