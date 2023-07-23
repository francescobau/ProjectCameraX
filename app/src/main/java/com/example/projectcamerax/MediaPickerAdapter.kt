package com.example.projectcamerax

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat

class MediaPickerAdapter {
    // TODO: Adapter for the MediaPicker RecyclerView.
}

class FlowerAdapter(private val flowerList: Array<String>) :
    RecyclerView.Adapter<FlowerAdapter.FlowerViewHolder>() {

    // Conteggio onCreateViewHolder
    var i = 0

    // Conteggio  bind
    var j = 0

    // Espressione del listener, che verra' utilizzata da tutti i FlowerViewHolder.
    // La lambda v rappresentera' l'oggetto che chiama l'onClickListener (?)
    private val onClickListener = View.OnClickListener { v ->
        // Estraggo il valore del fiore. "v" e' la lambda che comunichera' con la classe sovrastante
        val flowerName = v.findViewById<TextView>(R.id.flower_text).text

        val detailIntent = Intent(v.context, DetailActivity::class.java)
        // Passaggio del parametro nell'Intent
//                detailIntent.extras?.putString("flower",word)
        detailIntent.putExtra(FLOWER_TAG, flowerName)
        printLog("String = ${detailIntent.extras?.getString(FLOWER_TAG)}")
        // Avvio della nuova Activity, dato l'Intent.
        v.context.startActivity(detailIntent)
    }

    // Describes an item view and its place within the RecyclerView
    class FlowerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val flowerTextView: TextView = itemView.findViewById(R.id.flower_text)

        // Aggiungo risorsa flowerIndexView
        private val flowerIndexView: TextView = itemView.findViewById(R.id.flower_index)

        // Flag per capire se elemento nuovo o riciclato
        private var isNew = true

        // Function chiamata per applicare i parametri in arrivo alla FlowerViewHolder attuale.
        fun bind(number: Int, word: String) {
            flowerTextView.text = word

            // Applico un formato numerico decimale con almeno 2 cifre
            val f = DecimalFormat("00")
            // Inserisco la posizione nella TextView, applicando il formato
            flowerIndexView.text = f.format(number)

            var str = "Elemento ${number - 1}"
            // Controllo se elemento nuovo o riciclato
            if (isNew) {
                str = "$str NUOVO"
                // Una volta effettuata la bind, tale ViewHolder non e' piu' nuovo.
                isNew = false
            } else str = "$str RICICLATO"
            // Stampo la stringa finale
            printLog(str)
        }
    }

    // Returns a new ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlowerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.flower_item, parent, false)

        // Messaggio di log quando un elemento nella lista necessita di un nuovo FlowerViewHolder
        i++
        printLog("onCreateViewHolder #$i : Creato un nuovo elemento")

        // Imposto il listener per ogni ViewHolder
        view.setOnClickListener(onClickListener)

        return FlowerViewHolder(view)
    }

    // Returns size of data list
    override fun getItemCount(): Int {
        return flowerList.size
    }

    // Displays data at a certain position
    override fun onBindViewHolder(holder: FlowerViewHolder, position: Int) {

        // Messaggio di log che si attiva quando un elemento della lista vuole
        // unirsi a un FlowerViewHolder
        j++
//        printLog("Bind #$j")
        Log.d(TAG, "Bind #$j")

        // Modificata la chiamata a bind: passa anche la position+1 (perche' position parte da 0).
        holder.bind(position + 1, flowerList[position])
    }

    companion object {
        // Nome della classe. Serve per il Log.
        private val TAG = FlowerAdapter::class.simpleName
        // Chiave passata nell'Intent
        const val FLOWER_TAG = "flower"

        // Function che stampa un messaggio di log.
        private fun printLog(message: String) {
            Log.d(TAG, message)
        }
    }
}
