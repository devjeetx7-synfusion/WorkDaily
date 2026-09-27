package com.synfusion.attendance.utils.pdf

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

data class PdfShareAppTarget(
    val label: String,
    val packageName: String,
    val icon: Drawable?
)

object PdfShareManager {

    private const val AUTHORITY = "com.synfusion.attendance.fileprovider"

    fun getSecureContentUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, AUTHORITY, file)
    }

    fun getAvailablePdfShareTargets(context: Context): List<PdfShareAppTarget> {
        return try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
            }
            val pm = context.packageManager
            val resolveInfos = pm.queryIntentActivities(sendIntent, PackageManager.MATCH_DEFAULT_ONLY)
            val targets = mutableListOf<PdfShareAppTarget>()
            val seenPackages = mutableSetOf<String>()

            // Priority packages order if present
            val priorityPackages = listOf(
                "com.whatsapp",
                "org.telegram.messenger",
                "com.google.android.gm",
                "com.google.android.apps.docs"
            )

            // First add priority packages in order if found
            for (priorityPkg in priorityPackages) {
                val match = resolveInfos.firstOrNull { it.activityInfo.packageName == priorityPkg }
                if (match != null && seenPackages.add(priorityPkg)) {
                    val label = match.loadLabel(pm).toString()
                    val icon = match.loadIcon(pm)
                    targets.add(PdfShareAppTarget(label = label, packageName = priorityPkg, icon = icon))
                }
            }

            // Then add remaining resolved apps up to a maximum of 8
            for (info in resolveInfos) {
                val pkg = info.activityInfo.packageName
                if (pkg == context.packageName) continue // Skip self
                if (seenPackages.add(pkg)) {
                    val label = info.loadLabel(pm).toString()
                    val icon = info.loadIcon(pm)
                    targets.add(PdfShareAppTarget(label = label, packageName = pkg, icon = icon))
                }
                if (targets.size >= 8) break
            }

            targets
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun shareToApp(context: Context, uri: Uri, packageName: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                setPackage(packageName)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun shareToSystemChooser(context: Context, uri: Uri, title: String = "Share PDF"): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            val chooser = Intent.createChooser(intent, title).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
