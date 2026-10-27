package com.synfusion.attendance.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.synfusion.attendance.R

object SafeExternalLinkHandler {
    fun openLink(context: Context, url: String, errorMessageResId: Int = R.string.error_opening_link) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(errorMessageResId), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.error_generic_link), Toast.LENGTH_SHORT).show()
        }
    }

    fun openEmail(context: Context, emailAddress: String, errorMessageResId: Int = R.string.error_no_email_app) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$emailAddress"))
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(errorMessageResId), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.error_generic_email), Toast.LENGTH_SHORT).show()
        }
    }

    fun openExternalUrl(context: Context, url: String) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return

        val uri = try {
            Uri.parse(trimmed)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.error_generic_link), Toast.LENGTH_SHORT).show()
            return
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme == "mailto") {
            val emailAddress = trimmed.substringAfter("mailto:").substringBefore("?")
            openEmail(context, emailAddress)
        } else if (scheme == "http" || scheme == "https") {
            openLink(context, trimmed)
        } else {
            // For any other scheme or no scheme provided, default to openLink with http/https fallback if needed
            val targetUrl = if (scheme == null) "https://$trimmed" else trimmed
            openLink(context, targetUrl)
        }
    }
}
