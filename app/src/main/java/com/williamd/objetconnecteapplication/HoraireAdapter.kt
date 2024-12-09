package com.williamd.objetconnecteapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class HoraireAdapter(private val context: Context, private val dataList: List<Horaire>): BaseAdapter() {
    override fun getCount(): Int {
        return dataList.size
    }

    override fun getItem(position: Int): Any {
        return dataList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, converView: View?, parent: ViewGroup?): View {
        val currentItem =getItem(position) as Horaire
        val itemView = converView ?: LayoutInflater.from(context).inflate(R.layout.item_liste_horaire, parent, false)

        // Stocke les éléments de la vue
        val tvDebut = itemView.findViewById<TextView>(R.id.tv_horaire_nom)

        tvDebut.text = currentItem.debut

        return itemView
    }

}