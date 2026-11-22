package com.lensora.ai

import com.lensora.ai.ai.NchwConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NchwConverterTest {

    @Test
    fun softmax_validLogits_sumsToOneAndPreservesArgmax() {
        val logits = floatArrayOf(2.0f, 1.0f, 0.1f, 3.0f, 0.5f)
        val probabilities = NchwConverter.softmax(logits)

        assertEquals(5, probabilities.size)

        var sum = 0.0f
        var maxIndex = 0
        var maxProb = probabilities[0]

        for (i in probabilities.indices) {
            sum += probabilities[i]
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i]
                maxIndex = i
            }
        }

        assertEquals(1.0f, sum, 0.0001f)
        assertEquals(3, maxIndex) // Index 3 has logit 3.0f (highest)
    }

    @Test
    fun softmax_emptyLogits_returnsEmptyArray() {
        val probabilities = NchwConverter.softmax(floatArrayOf())
        assertTrue(probabilities.isEmpty())
    }
}
