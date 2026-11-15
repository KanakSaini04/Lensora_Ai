package com.lensora.ai.ui.camera

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

object FilmGrainShader {
    private const val FILM_GRAIN_AGSL = """
        uniform shader inputShader;

        float pseudoNoise(float2 co) {
            return fract(sin(dot(co, float2(12.9898, 78.233))) * 43758.5453);
        }

        half4 main(float2 fragCoord) {
            half4 color = inputShader.eval(fragCoord);
            float noise = pseudoNoise(fragCoord) - 0.5;
            float grainStrength = 0.05;
            color.rgb += half3(noise * grainStrength);
            return color;
        }
    """

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createFilmGrainEffect(width: Float, height: Float, timeSec: Float = 0f): RenderEffect? {
        return try {
            val shader = RuntimeShader(FILM_GRAIN_AGSL)
            RenderEffect.createRuntimeShaderEffect(shader, "inputShader")
        } catch (t: Throwable) {
            Log.w("FilmGrainShader", "Failed to compile AGSL Film Grain shader: ${t.localizedMessage}")
            null
        }
    }
}
