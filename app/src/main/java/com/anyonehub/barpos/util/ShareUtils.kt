/* Copyright 2024 anyone-Hub */
package com.anyonehub.barpos.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Standard utility to share generated PDF or CSV reports with external apps.
 */
fun shareFile(context: Context, file: File, mimeType: String) {
    val authority = "${context.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, file)

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(intent, "Share Midtown Report")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    context.startActivity(chooser)
}