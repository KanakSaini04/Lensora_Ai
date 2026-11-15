package com.lensora.ai.ui.camera

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi

object FilmGrainShader {
    private const val FILM_GRAIN_AGSL = """
        uniform shader inputShader;
        uniform float2 in_size;
        uniform float in_time;

        float pseudoNoise(float2 co) {
            return fract(sin(dot(co, float2(12.9898, 78.233))) * 43758.5453);
        }

        half4 main(float2 fragCoord) {
            half4 color = inputShader.eval(fragCoord);
            float noise = pseudoNoise(fragCoord + float2(in_time, in_time * 1.5)) - 0.5;
            float grainStrength = 0.08;
            color.rgb += half3(noise * grainStrength);
            return color;
        }
    """

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createFilmGrainEffect(width: Float, height: Float, timeSec: Float = 0f): RenderEffect {
        val shader = RuntimeShader(FILM_GRAIN_AGSL)
        shader.setFloatUniform("in_size", width, height)
        shader.setFloatUniform("in_time", timeSec)
        return RenderEffect.createRuntimeShaderEffect(shader, "inputShader")
    }
}
