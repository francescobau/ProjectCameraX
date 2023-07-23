package com.example.projectcamerax

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView

class MediaPickerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_picker)
    }
}

class DetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val dt: TextView = findViewById(R.id.detail_text)
        //val str = "is a nice flower"
        // Estraggo il parametro passato con l'intent tramite putExtra(...)
        //dt.text = "${intent.extras?.getString(FlowerAdapter.FLOWER_TAG)} $str"
        dt.text = "${intent.extras?.getString(FlowerAdapter.FLOWER_TAG)} ${getString(R.string.flower_str)}"
        //dt.text = dt.text.("FFF".toRegex(),intent.extras?.getString(FlowerAdapter.FLOWER_TAG) as CharSequence)
        // Stampa di debug del parametro
        Log.d(
            DetailActivity::class.simpleName,
            "Intent's extra = ${intent.extras?.getString(FlowerAdapter.FLOWER_TAG)}"
        )
    }
}