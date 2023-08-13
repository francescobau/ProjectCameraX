package com.example.projectcamerax

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class MediaPickerAdapter(private val mediaList: MutableList<MediaInfo>) :
    RecyclerView.Adapter<MediaPickerAdapter.MediaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.media_item, parent, false)
        return MediaViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return mediaList.size
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val mediaInfo = mediaList[position]
        holder.bind(mediaInfo)
    }

    inner class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val mediaTitle: TextView = itemView.findViewById(R.id.media_title)
        private val mediaType: TextView = itemView.findViewById(R.id.media_type)
        private val mediaPath: TextView = itemView.findViewById(R.id.media_path)
        private val deleteButton: Button = itemView.findViewById(R.id.delete_button)

        private fun deleteMedia(mediaInfo: MediaInfo) {
            val fileToDelete = File(mediaInfo.fullPath)
            if (fileToDelete.exists()) {
                val deleted = fileToDelete.delete()
                if (deleted) {
                    mediaList.remove(mediaInfo)
                    notifyDataSetChanged()
                    deleteButton.visibility = View.GONE
                    Toast.makeText(itemView.context, "File deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(itemView.context, "Error deleting file", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        fun bind(mediaInfo: MediaInfo) {
            mediaTitle.text = mediaInfo.title
            mediaType.text = mediaInfo.mimeType
            mediaPath.text = mediaInfo.fullPath

            deleteButton.visibility = if (deleteButton.isVisible) View.VISIBLE else View.GONE

            itemView.setOnClickListener { v ->
                // Extracts the path from the TextView.
                val mediaPathText = v.findViewById<TextView>(R.id.media_path).text.toString()

                // Gets the file from the given path.
                val mediaFile = File(mediaPathText)

                if (mediaFile.exists()) {
                    val mediaUri = FileProvider.getUriForFile(
                        v.context,
                        "${v.context.packageName}.fileprovider",
                        mediaFile
                    )
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(mediaUri, mediaInfo.mimeType)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    if (intent.resolveActivity(v.context.packageManager) != null) {
                        v.context.startActivity(intent)
                    } else {
                        Toast.makeText(
                            v.context,
                            "No app available to open the content",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            }

            deleteButton.setOnClickListener { deleteMedia(mediaInfo) }
            itemView.setOnLongClickListener { v ->
                // Inverte lo stato della visibilità del pulsante di cancellazione
                if (deleteButton.isVisible) deleteButton.visibility = View.GONE
                else deleteButton.visibility = View.VISIBLE
                notifyItemChanged(adapterPosition)
                true
            }
        }
    }
}