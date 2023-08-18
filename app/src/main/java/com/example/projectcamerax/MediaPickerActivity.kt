package com.example.projectcamerax

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * This activity allows users to pick and view media files.
 */
class MediaPickerActivity : AppCompatActivity() {

    private lateinit var mediaAdapter: MediaPickerAdapter

    /**
     * Code executed when Activity is created.
     * @param savedInstanceState If a saved instance exists, this is the [Bundle] containing it, eventually restored by function [onRestoreInstanceState].
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_picker)

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val mediaList = fetchMediaList()

        mediaAdapter = MediaPickerAdapter(mediaList)
        recyclerView.adapter = mediaAdapter
    }

    /**
     * Fetches the list of media files to display.
     *
     * @return The list of [MediaInfo] objects representing media files.
     */
    private fun fetchMediaList(): MutableList<MediaInfo> {
        val datasource = Datasource().getMediaList()
        return if (datasource.isEmpty()) {
            val noMedia = TextView(this)
            noMedia.text = getString(R.string.no_media_text)
            noMedia.textSize = 50.0F
            setContentView(noMedia)
            emptyList<MediaInfo>().toMutableList()
        } else datasource
    }
}