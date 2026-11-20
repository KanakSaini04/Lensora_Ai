package com.lensora.ai.ai

import org.tensorflow.lite.Interpreter
import java.io.File

data class TensorContract(
    val expectedInputShape: IntArray,
    val expectedInputDtype: String,
    val expectedOutputShape: IntArray,
    val expectedOutputDtype: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TensorContract) return false
        if (!expectedInputShape.contentEquals(other.expectedInputShape)) return false
        if (expectedInputDtype != other.expectedInputDtype) return false
        if (!expectedOutputShape.contentEquals(other.expectedOutputShape)) return false
        if (expectedOutputDtype != other.expectedOutputDtype) return false
        return true
    }

    override fun hashCode(): Int {
        var result = expectedInputShape.contentHashCode()
        result = 31 * result + expectedInputDtype.hashCode()
        result = 31 * result + expectedOutputShape.contentHashCode()
        result = 31 * result + expectedOutputDtype.hashCode()
        return result
    }
}

sealed class ModelContractStatus {
    object Valid : ModelContractStatus()
    object Missing : ModelContractStatus()
    data class Incompatible(val reason: String) : ModelContractStatus()
}

interface TensorInfoProvider {
    fun getInputShape(): IntArray?
    fun getInputDtype(): String?
    fun getOutputShape(): IntArray?
    fun getOutputDtype(): String?
}

class InterpreterTensorInfoProvider(
    private val interpreter: Interpreter,
) : TensorInfoProvider {
    override fun getInputShape(): IntArray? = try { interpreter.getInputTensor(0).shape() } catch (_: Exception) { null }
    override fun getInputDtype(): String? = try { interpreter.getInputTensor(0).dataType().name } catch (_: Exception) { null }
    override fun getOutputShape(): IntArray? = try { interpreter.getOutputTensor(0).shape() } catch (_: Exception) { null }
    override fun getOutputDtype(): String? = try { interpreter.getOutputTensor(0).dataType().name } catch (_: Exception) { null }
}

object ModelContracts {
    val SCENE_CLASSIFIER = TensorContract(
        expectedInputShape = intArrayOf(1, 3, 224, 224), // NCHW Float32 Places365
        expectedInputDtype = "FLOAT32",
        expectedOutputShape = intArrayOf(1, 365),
        expectedOutputDtype = "FLOAT32",
    )

    val LAMA_DILATED = TensorContract(
        expectedInputShape = intArrayOf(1, 512, 512, 4),
        expectedInputDtype = "FLOAT32",
        expectedOutputShape = intArrayOf(1, 512, 512, 3),
        expectedOutputDtype = "FLOAT32",
    )

    val REAL_ESRGAN_V2 = TensorContract(
        expectedInputShape = intArrayOf(1, 128, 128, 3),
        expectedInputDtype = "FLOAT32",
        expectedOutputShape = intArrayOf(1, 512, 512, 3), // 4x output scale
        expectedOutputDtype = "FLOAT32",
    )

    val DEHAZE = TensorContract(
        expectedInputShape = intArrayOf(1, 512, 512, 3),
        expectedInputDtype = "FLOAT32",
        expectedOutputShape = intArrayOf(1, 512, 512, 3),
        expectedOutputDtype = "FLOAT32",
    )
}

object ModelContractChecker {

    fun checkContract(
        modelFile: File?,
        contract: TensorContract,
        tensorInfoProvider: TensorInfoProvider?,
    ): ModelContractStatus {
        if (modelFile == null || !modelFile.exists() || modelFile.length() == 0L || tensorInfoProvider == null) {
            return ModelContractStatus.Missing
        }

        val inShape = tensorInfoProvider.getInputShape()
        val inDtype = tensorInfoProvider.getInputDtype()
        val outShape = tensorInfoProvider.getOutputShape()
        val outDtype = tensorInfoProvider.getOutputDtype()

        if (inShape == null || outShape == null || inDtype == null || outDtype == null) {
            return ModelContractStatus.Incompatible("Could not read tensor shapes or dtypes from model file")
        }

        android.util.Log.i("ModelContractChecker", "Model ${modelFile.name} tensor contract: Input shape=${inShape.contentToString()}, dtype=$inDtype; Output shape=${outShape.contentToString()}, dtype=$outDtype")

        if (!inShape.contentEquals(contract.expectedInputShape)) {
            return ModelContractStatus.Incompatible(
                "Input shape mismatch: expected ${contract.expectedInputShape.contentToString()}, got ${inShape.contentToString()}",
            )
        }

        if (!inDtype.equals(contract.expectedInputDtype, ignoreCase = true)) {
            return ModelContractStatus.Incompatible(
                "Input dtype mismatch: expected ${contract.expectedInputDtype}, got $inDtype",
            )
        }

        if (!outShape.contentEquals(contract.expectedOutputShape)) {
            return ModelContractStatus.Incompatible(
                "Output shape mismatch: expected ${contract.expectedOutputShape.contentToString()}, got ${outShape.contentToString()}",
            )
        }

        if (!outDtype.equals(contract.expectedOutputDtype, ignoreCase = true)) {
            return ModelContractStatus.Incompatible(
                "Output dtype mismatch: expected ${contract.expectedOutputDtype}, got $outDtype",
            )
        }

        return ModelContractStatus.Valid
    }
}
