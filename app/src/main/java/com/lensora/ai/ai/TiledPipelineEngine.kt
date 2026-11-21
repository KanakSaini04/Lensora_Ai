package com.lensora.ai.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log

interface TileProcessor {
    fun processTile(tileBitmap: Bitmap): Bitmap?
}

object TiledPipelineMath {
    fun calculateOutputDimensions(inWidth: Int, inHeight: Int, scaleFactor: Int): Pair<Int, Int> {
        return Pair(inWidth * scaleFactor, inHeight * scaleFactor)
    }

    fun calculateTileGrid(inWidth: Int, inHeight: Int, tileSize: Int): Pair<Int, Int> {
        val numCols = (inWidth + tileSize - 1) / tileSize
        val numRows = (inHeight + tileSize - 1) / tileSize
        return Pair(numCols, numRows)
    }
}

object TiledPipelineEngine {
    private const val TAG = "TiledPipelineEngine"

    fun processTiled(
        sourceBitmap: Bitmap,
        tileSize: Int = 512,
        scaleFactor: Int = 1,
        processor: TileProcessor,
        onTileRecycled: ((Bitmap) -> Unit)? = null,
    ): Bitmap {
        val inWidth = sourceBitmap.width
        val inHeight = sourceBitmap.height

        val outWidth = inWidth * scaleFactor
        val outHeight = inHeight * scaleFactor

        val outputBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val numCols = (inWidth + tileSize - 1) / tileSize
        val numRows = (inHeight + tileSize - 1) / tileSize

        for (row in 0 until numRows) {
            for (col in 0 until numCols) {
                val left = col * tileSize
                val top = row * tileSize
                val right = (left + tileSize).coerceAtMost(inWidth)
                val bottom = (top + tileSize).coerceAtMost(inHeight)

                val tileW = right - left
                val tileH = bottom - top

                val inTile = Bitmap.createBitmap(sourceBitmap, left, top, tileW, tileH)
                val outTile = processor.processTile(inTile)

                if (outTile != null) {
                    val destLeft = (left * scaleFactor).toFloat()
                    val destTop = (top * scaleFactor).toFloat()

                    canvas.drawBitmap(outTile, destLeft, destTop, paint)

                    if (outTile != inTile) {
                        outTile.recycle()
                        onTileRecycled?.invoke(outTile)
                    }
                }

                inTile.recycle()
                onTileRecycled?.invoke(inTile)
            }
        }

        Log.i(TAG, "Completed tiled pipeline pass: input ${inWidth}x${inHeight} -> output ${outWidth}x${outHeight} (${scaleFactor}x scale)")
        return outputBitmap
    }
}
