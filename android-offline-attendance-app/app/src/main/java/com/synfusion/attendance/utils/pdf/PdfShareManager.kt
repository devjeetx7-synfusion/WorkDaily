package com.synfusion.attendance.utils.pdf

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object PdfShareManager {

    private const val AUTHORITY = "com.synfusion.attendance.fileprovider"
    private const val PACKAGE_WHATSAPP = "com.whatsapp"
    private const val PACKAGE_TELEGRAM = "org.telegram.messenger"
    private const val PACKAGE_GMAIL = "com.google.android.gm"

    fun getSecureContentUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, AUTHORITY, file)
    }

    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isWhatsAppInstalled(context: Context): Boolean = isPackageInstalled(context, PACKAGE_WHATSAPP)
    fun isTelegramInstalled(context: Context): Boolean = isPackageInstalled(context, PACKAGE_TELEGRAM)
    fun isGmailInstalled(context: Context): Boolean = isPackageInstalled(context, PACKAGE_GMAIL)

    fun shareToApp(context: Context, uri: Uri, packageName: String?): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                if (!packageName.isNullOrEmpty()) {
                    setPackage(packageName)
                }
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
