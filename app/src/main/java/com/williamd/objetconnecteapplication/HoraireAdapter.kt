package com.williamd.objetconnecteapplication

import android.content.Context
import android.media.Image
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

class HoraireAdapter(private val context: Context, private val dataList: MutableList<Horaire>): BaseAdapter() {
    interface OnDeleteClickListener {
        fun onDeleteClick(currentItem: Horaire)
    }

    // Variable qui stocke la référence
    private var deleteClickListener: OnDeleteClickListener? = null

    // Permet au fragment de changer le listener
    fun setDeleteClickListener(listener: OnDeleteClickListener) {
        this.deleteClickListener = listener
    }

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
        val tvTitre = itemView.findViewById<TextView>(R.id.tv_adapter_horaire_titre)
        val tvType = itemView.findViewById<TextView>(R.id.tv_adapter_horaire_type)
        val btnDelete = itemView.findViewById<Button>(R.id.btn_adapter_horaire_supprimer)
        val imgQuotidiennement = itemView.findViewById<ImageView>(R.id.img_quotidiennement)

        // Affiche le type de changement
        if(currentItem.type == HoraireTypeEnum.ALLUME){
            tvType.text = "Allumé"
        }else if(currentItem.type == HoraireTypeEnum.ETEINT){
            tvType.text = "Éteindre"
        }

        // Affiche l'heure
        tvTitre.text = currentItem.debut

        // Affiche quotidiennement ou non
        if(currentItem.isQuotidiennement){
            imgQuotidiennement.visibility = View.VISIBLE
        }else{
            imgQuotidiennement.visibility = View.GONE
        }

        //Events
        //Supprimer
        btnDelete.setOnClickListener {
            deleteClickListener?.onDeleteClick(currentItem)
        }

        return itemView
    }
}