package com.lensora.ai.ui.studio

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

object LutShader {

    const val REAL_3D_LUT_AGSL = """
        uniform shader composable;
        uniform shader lutTexture;
        uniform float lutSize;

        half4 main(float2 fragCoord) {
            half4 color = composable.eval(fragCoord);

            float maxColor = lutSize - 1.0;
            float r = clamp(color.r, 0.0, 1.0) * maxColor;
            float g = clamp(color.g, 0.0, 1.0) * maxColor;
            float b = clamp(color.b, 0.0, 1.0) * maxColor;

            float blueSlice1 = floor(b);
            float blueSlice2 = min(blueSlice1 + 1.0, maxColor);

            float x1 = (blueSlice1 * lutSize + r + 0.5);
            float y1 = (g + 0.5);

            float x2 = (blueSlice2 * lutSize + r + 0.5);
            float y2 = (g + 0.5);

            half4 col1 = lutTexture.eval(float2(x1, y1));
            half4 col2 = lutTexture.eval(float2(x2, y2));

            float blueFrac = b - blueSlice1;
            half4 lutColor = mix(col1, col2, blueFrac);
            return half4(lutColor.rgb, color.a);
        }
    """

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createLutRenderEffect(lutStripBitmap: Bitmap, lutSize: Float = 17f): RenderEffect? {
        return try {
            val bitmapShader = BitmapShader(lutStripBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val shader = RuntimeShader(REAL_3D_LUT_AGSL)
            shader.setInputShader("lutTexture", bitmapShader)
            shader.setFloatUniform("lutSize", lutSize)
            RenderEffect.createRuntimeShaderEffect(shader, "composable")
        } catch (t: Throwable) {
            Log.w("LutShader", "Failed to compile AGSL LUT shader: ${t.localizedMessage}")
            null
        }
    }
}
