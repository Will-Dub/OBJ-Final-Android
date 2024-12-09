package com.williamd.objetconnecteapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

class HoraireAdapter(private val context: Context, private val dataList: MutableList<Horaire>): BaseAdapter() {
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

        if(currentItem.type == HoraireTypeEnum.ALLUME){
            tvType.text = "Allumé"
        }else if(currentItem.type == HoraireTypeEnum.ETEINT){
            tvType.text = "Éteindre"
        }

        tvTitre.text = currentItem.debut

        //Events
        //Supprimer
        btnDelete.setOnClickListener {
            showDeleteConfirmationDialog(position)
        }

        return itemView
    }

    private fun showDeleteConfirmationDialog(position: Int){
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.horaire_delete_title)
        builder.setMessage(R.string.horaire_delete_message)
        builder.setPositiveButton(R.string.yes) { _, _ ->
            dataList.removeAt(position)
            notifyDataSetChanged()
        }
        builder.setNegativeButton(R.string.no) { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()

    }

}