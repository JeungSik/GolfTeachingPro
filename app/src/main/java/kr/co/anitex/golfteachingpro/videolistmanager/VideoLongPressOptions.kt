@file:Suppress("DEPRECATION")

package kr.co.anitex.golfteachingpro.videolistmanager

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kr.co.anitex.golfteachingpro.R
import java.io.File


/**
 * Created by nitinagarwal on 3/21/17.
 */
object VideoLongPressOptions {
    fun deleteFile(
            context: Context, selectedVideoDelete: String, id: Int,
            videoListUpdateManager: VideoListManagerImpl
    ) {
        val alertDialog: AlertDialog.Builder = AlertDialog.Builder(context)
        alertDialog.setTitle("Confirm Delete...")
        alertDialog.setMessage("Are you sure you want to Delete:\n\n$selectedVideoDelete")
        alertDialog.setNegativeButton("NO") { dialog, _ ->
            dialog.cancel()
        }
        alertDialog.setPositiveButton("YES") { _, _ ->
            val fileToDelete = File(selectedVideoDelete)
            val deletedSuccessfully = fileToDelete.delete()
            if (deletedSuccessfully) {
                MediaScannerConnection.scanFile(
                        context,
                        arrayOf(selectedVideoDelete),
                        null,
                        null
                )
                videoListUpdateManager.updateForDeleteVideo(id)
            }
        }
        alertDialog.show()
    }

    fun renameFile(
            context: Context, selectedVideoTitleForRename: String?, selectedVideoRenamePath: String,
            extensionValue: String, id: Int, videoListUpdateManager: VideoListUpdateManager
    ) {
        val alert: AlertDialog.Builder = AlertDialog.Builder(context)
        val li = LayoutInflater.from(context)
        val renameVideoView: View = li.inflate(R.layout.rename_video, null)
        val input = renameVideoView.findViewById<View>(R.id.rename_edit_text) as EditText
        input.setText(selectedVideoTitleForRename)
        alert.setView(renameVideoView)
        alert.setNegativeButton("CANCEL") { dialog, _ -> dialog.cancel() }
        alert.setPositiveButton("YES") { _, _ ->
            val fileToRename = File(selectedVideoRenamePath)
            val fileNameNew = File(
                    selectedVideoRenamePath.replace(
                            (selectedVideoTitleForRename)!!, input.text.toString()
                    )
            )
            if (fileNameNew.exists()) {
                Toast.makeText(
                        context,
                        context.resources.getString(R.string.same_title_exists), Toast.LENGTH_LONG
                ).show()
            } else {
                val updatedTitle = input.text.toString() + extensionValue
                fileToRename.renameTo(fileNameNew)
                val newFilePath = fileNameNew.toString()
                videoListUpdateManager.updateForRenameVideo(id, newFilePath, updatedTitle)
            }
        }
        alert.show()
    }

    fun shareFile(context: Context, selectedVideoShare: String) {
        MediaScannerConnection.scanFile(
            context, arrayOf(selectedVideoShare),
            null
        ) { _, uri ->
            val shareIntent = Intent(
                Intent.ACTION_SEND
            )
            shareIntent.type = "video/*"
            shareIntent.putExtra(
                "VSMP",
                "https://play.google.com/store/apps/details?id=" + context.packageName
            )
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            context.startActivity(
                Intent.createChooser(
                    shareIntent,
                    context.getString(R.string.share_text)
                )
            )
        }
    }
}