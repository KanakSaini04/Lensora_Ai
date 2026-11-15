package com.lensora.ai.ui.camera

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi

object AgslShaders {

    // AGSL Shader for Zebras (Highlights overexposed pixels > 0.85 luminance in red stripes)
    const val ZEBRAS_AGSL = """
        uniform shader composable;
        
        half4 main(float2 fragCoord) {
            half4 color = composable.eval(fragCoord);
            float lum = dot(color.rgb, float3(0.299, 0.587, 0.114));
            if (lum > 0.85) {
                float stripe = mod(fragCoord.x + fragCoord.y, 20.0);
                if (stripe < 10.0) {
                    return half4(1.0, 0.0, 0.0, 1.0);
                }
            }
            return color;
        }
    """

    // AGSL Shader for Focus Peaking (Highlights sharp high-frequency edges in Neon Green)
    const val FOCUS_PEAKING_AGSL = """
        uniform shader composable;
        
        half4 main(float2 fragCoord) {
            half4 color = composable.eval(fragCoord);
            
            half4 left = composable.eval(fragCoord - float2(2.0, 0.0));
            half4 right = composable.eval(fragCoord + float2(2.0, 0.0));
            half4 top = composable.eval(fragCoord - float2(0.0, 2.0));
            half4 bottom = composable.eval(fragCoord + float2(0.0, 2.0));
            
            float lumCenter = dot(color.rgb, float3(0.299, 0.587, 0.114));
            float lumLeft = dot(left.rgb, float3(0.299, 0.587, 0.114));
            float lumRight = dot(right.rgb, float3(0.299, 0.587, 0.114));
            float lumTop = dot(top.rgb, float3(0.299, 0.587, 0.114));
            float lumBottom = dot(bottom.rgb, float3(0.299, 0.587, 0.114));
            
            float edge = abs(lumCenter - lumLeft) + abs(lumCenter - lumRight) + abs(lumCenter - lumTop) + abs(lumCenter - lumBottom);
            if (edge > 0.25) {
                return half4(0.0, 1.0, 0.0, 1.0);
            }
            return color;
        }
    """

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createZebrasEffect(): RenderEffect {
        val shader = RuntimeShader(ZEBRAS_AGSL)
        return RenderEffect.createRuntimeShaderEffect(shader, "composable")
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createFocusPeakingEffect(): RenderEffect {
        val shader = RuntimeShader(FOCUS_PEAKING_AGSL)
        return RenderEffect.createRuntimeShaderEffect(shader, "composable")
    }
}
