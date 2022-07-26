package com.rmd.realstate.ui.post.recycler_adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rmd.realstate.R
import com.rmd.realstate.databinding.RowLoadedImagesBinding
import com.squareup.picasso.Picasso

class Recycler_Adapter_Loaded_Image_Uri(var loadedImagesList: ArrayList<Uri?>) :
    RecyclerView.Adapter<Recycler_Adapter_Loaded_Image_Uri.Loaded_Image_ViewHolder>() {

    inner class Loaded_Image_ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Loaded_Image_ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.row_loaded_images, parent, false)
        return Loaded_Image_ViewHolder(view)
    }

    override fun onBindViewHolder(holder: Loaded_Image_ViewHolder, position: Int) {

        val binding = RowLoadedImagesBinding.bind(holder.itemView)

        holder.itemView.apply {
            Picasso.get().load(loadedImagesList[position]).resize(300, 300)
                .into(binding.loadedImageImgv)
        }
    }

    override fun getItemCount(): Int {
        return loadedImagesList.size
    }
}