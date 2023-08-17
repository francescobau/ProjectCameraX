package com.example.projectcamerax

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MediaPickerActivity : AppCompatActivity() {

    private lateinit var mediaAdapter: MediaPickerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_picker)

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val mediaList = fetchMediaList()

        mediaAdapter = MediaPickerAdapter(mediaList)
        recyclerView.adapter = mediaAdapter
    }

    private fun fetchMediaList(): MutableList<MediaInfo> {
        val datasource = Datasource().getMediaList()
        return if (datasource.isEmpty()) {
            val noMedia = TextView(this)
            noMedia.text = "NO_MEDIA"
            noMedia.textSize = 50.0F
            setContentView(noMedia)
            emptyList<MediaInfo>().toMutableList()
        } else datasource
    }
}