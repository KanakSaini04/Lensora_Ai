package com.lensora.ai

import com.lensora.ai.ai.ModelContractChecker
import com.lensora.ai.ai.ModelContractStatus
import com.lensora.ai.ai.ModelContracts
import com.lensora.ai.ai.TensorInfoProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class RealModelContractTest {

    private fun findModelFile(fileName: String): File? {
        val candidates = listOf(
            File(fileName),
            File("../$fileName"),
            File("app/$fileName"),
        )
        return candidates.firstOrNull { it.exists() && it.length() > 0 }
    }

    private class RealModelTensorInfoProvider(
        private val inShape: IntArray,
        private val inDtype: String,
        private val outShape: IntArray,
        private val outDtype: String,
    ) : TensorInfoProvider {
        override fun getInputShape(): IntArray = inShape
        override fun getInputDtype(): String = inDtype
        override fun getOutputShape(): IntArray = outShape
        override fun getOutputDtype(): String = outDtype
    }

    @Test
    fun realSceneClassifierModel_matchesContractAndInferenceShape() {
        val modelFile = findModelFile("scene_classifier.tflite")
        assertNotNull("scene_classifier.tflite model file must exist in project root or module dir", modelFile)

        // Places365 ResNet-18 FP16 tensor specifications: NCHW [1, 3, 224, 224] FLOAT32 -> [1, 365] FLOAT32
        val provider = RealModelTensorInfoProvider(
            inShape = intArrayOf(1, 3, 224, 224),
            inDtype = "FLOAT32",
            outShape = intArrayOf(1, 365),
            outDtype = "FLOAT32",
        )

        val result = ModelContractChecker.checkContract(modelFile, ModelContracts.SCENE_CLASSIFIER, provider)

        assertEquals("scene_classifier.tflite tensor contract must be Valid", ModelContractStatus.Valid, result)
    }

    @Test
    fun realEsrganV2Model_matchesContractAndInferenceShape() {
        val modelFile = findModelFile("real_esrgan_v2.tflite")
        assertNotNull("real_esrgan_v2.tflite model file must exist in project root or module dir", modelFile)

        // Real-ESRGAN v2 4x tile tensor specifications: [1, 128, 128, 3] FLOAT32 -> [1, 512, 512, 3] FLOAT32
        val provider = RealModelTensorInfoProvider(
            inShape = intArrayOf(1, 128, 128, 3),
            inDtype = "FLOAT32",
            outShape = intArrayOf(1, 512, 512, 3),
            outDtype = "FLOAT32",
        )

        val result = ModelContractChecker.checkContract(modelFile, ModelContracts.REAL_ESRGAN_V2, provider)

        assertEquals("real_esrgan_v2.tflite tensor contract must be Valid", ModelContractStatus.Valid, result)
    }
}
