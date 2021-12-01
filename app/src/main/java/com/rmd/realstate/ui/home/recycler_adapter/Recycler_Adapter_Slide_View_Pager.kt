package com.rmd.realstate.ui.home.recycler_adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rmd.realstate.R
import com.rmd.realstate.databinding.RawImageSliderBinding
import com.squareup.picasso.Picasso

class Recycler_Adapter_Slide_View_Pager(private var pager_image_list: ArrayList<String>) :
    RecyclerView.Adapter<Recycler_Adapter_Slide_View_Pager.View_Pager>() {

    inner class View_Pager(itemView: View) : RecyclerView.ViewHolder(itemView)

    private lateinit var binding: RawImageSliderBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): View_Pager {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.raw_image_slider, parent, false)
        return View_Pager(view)
    }

    override fun onBindViewHolder(holder: View_Pager, position: Int) {

        binding = RawImageSliderBinding.bind(holder.itemView)

        holder.itemView.apply {
            Picasso.get().load(pager_image_list[position]).fit().into(binding.imageSlider)
        }
    }

    override fun getItemCount(): Int {
        return pager_image_list.size
    }
}
