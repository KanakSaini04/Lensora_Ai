package com.lensora.ai

import com.lensora.ai.ai.ModelContractChecker
import com.lensora.ai.ai.ModelContractStatus
import com.lensora.ai.ai.ModelContracts
import com.lensora.ai.ai.TensorInfoProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ModelContractCheckerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class FakeTensorInfoProvider(
        private val inShape: IntArray? = intArrayOf(1, 512, 512, 4),
        private val inDtype: String? = "FLOAT32",
        private val outShape: IntArray? = intArrayOf(1, 512, 512, 3),
        private val outDtype: String? = "FLOAT32",
    ) : TensorInfoProvider {
        override fun getInputShape(): IntArray? = inShape
        override fun getInputDtype(): String? = inDtype
        override fun getOutputShape(): IntArray? = outShape
        override fun getOutputDtype(): String? = outDtype
    }

    @Test
    fun checkContract_validLamaModel_returnsValidStatus() {
        val modelFile = tempFolder.newFile("lama_dilated.tflite").apply { writeText("fake_tflite") }
        val provider = FakeTensorInfoProvider(
            inShape = intArrayOf(1, 512, 512, 4),
            inDtype = "FLOAT32",
            outShape = intArrayOf(1, 512, 512, 3),
            outDtype = "FLOAT32",
        )

        val result = ModelContractChecker.checkContract(modelFile, ModelContracts.LAMA_DILATED, provider)

        assertEquals(ModelContractStatus.Valid, result)
    }

    @Test
    fun checkContract_validRealEsrganModel_returnsValidStatus() {
        val modelFile = tempFolder.newFile("real_esrgan_v2.tflite").apply { writeText("fake_tflite") }
        val provider = FakeTensorInfoProvider(
            inShape = intArrayOf(1, 128, 128, 3),
            inDtype = "FLOAT32",
            outShape = intArrayOf(1, 512, 512, 3), // 4x output scale
            outDtype = "FLOAT32",
        )

        val result = ModelContractChecker.checkContract(modelFile, ModelContracts.REAL_ESRGAN_V2, provider)

        assertEquals(ModelContractStatus.Valid, result)
    }

    @Test
    fun checkContract_validSceneClassifierModel_returnsValidStatus() {
        val modelFile = tempFolder.newFile("scene_classifier.tflite").apply { writeText("fake_tflite") }
        val provider = FakeTensorInfoProvider(
            inShape = intArrayOf(1, 3, 224, 224),
            inDtype = "FLOAT32",
            outShape = intArrayOf(1, 365),
            outDtype = "FLOAT32",
        )

        val result = ModelContractChecker.checkContract(modelFile, ModelContracts.SCENE_CLASSIFIER, provider)

        assertEquals(ModelContractStatus.Valid, result)
    }

    @Test
    fun checkContract_inputShapeMismatch_returnsIncompatibleStatus() {
        val modelFile = tempFolder.newFile("wrong_input.tflite").apply { writeText("fake_tflite") }
        val provider = FakeTensorInfoProvider(
            inShape = intArrayOf(1, 256, 256, 3),
            inDtype = "FLOAT32",
            outShape = intArrayOf(1, 512, 512, 3),
            outDtype = "FLOAT32",
        )

        val result = ModelContractChecker.checkContract(modelFile, ModelContracts.DEHAZE, provider)

        assertTrue(result is ModelContractStatus.Incompatible)
        val reason = (result as ModelContractStatus.Incompatible).reason
        assertTrue(reason.contains("Input shape mismatch"))
    }

    @Test
    fun checkContract_outputDtypeMismatch_returnsIncompatibleStatus() {
        val modelFile = tempFolder.newFile("wrong_dtype.tflite").apply { writeText("fake_tflite") }
        val provider = FakeTensorInfoProvider(
            inShape = intArrayOf(1, 3, 224, 224),
            inDtype = "FLOAT32",
            outShape = intArrayOf(1, 365),
            outDtype = "UINT8",
        )

        val result = ModelContractChecker.checkContract(modelFile, ModelContracts.SCENE_CLASSIFIER, provider)

        assertTrue(result is ModelContractStatus.Incompatible)
        val reason = (result as ModelContractStatus.Incompatible).reason
        assertTrue(reason.contains("Output dtype mismatch"))
    }

    @Test
    fun checkContract_missingFile_returnsMissingStatus() {
        val missingFile = File(tempFolder.root, "non_existent.tflite")
        val provider = FakeTensorInfoProvider()

        val result = ModelContractChecker.checkContract(missingFile, ModelContracts.LAMA_DILATED, provider)

        assertEquals(ModelContractStatus.Missing, result)
    }
}
