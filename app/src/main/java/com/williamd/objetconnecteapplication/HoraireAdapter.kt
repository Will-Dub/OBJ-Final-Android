package com.williamd.objetconnecteapplication

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter

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
        TODO("Not yet implemented")
    }

}