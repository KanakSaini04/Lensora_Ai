package com.lensora.ai.ai

import java.io.File

object ModelFileGuard {
    fun isUsable(file: File?): Boolean {
        return file != null && file.exists() && file.length() > 0 && file.canRead()
    }
}
