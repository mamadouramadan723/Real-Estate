package com.rmd.realstate.ui.home.recycler_adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.RecyclerView
import com.rmd.realstate.R
import com.rmd.realstate.databinding.RowPropertyBinding
import com.rmd.realstate.model.Property
import com.rmd.realstate.view_model.SharedViewModel_Property
import com.squareup.picasso.Picasso
import java.util.*

class Recycler_Adapter_Property(
    private var myContext: Fragment,
    private var myActivity: FragmentActivity,
    private var actionId: Int,
    private var propertyList: ArrayList<Property>

) : RecyclerView.Adapter<Recycler_Adapter_Property.Property_ViewHolder>(), Filterable {

    inner class Property_ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private lateinit var sharedViewModelProperty: SharedViewModel_Property

    private var allProperties = ArrayList<Property>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Property_ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.row_property, parent, false)

        sharedViewModelProperty =
            ViewModelProvider(myActivity)[SharedViewModel_Property::class.java]
        allProperties = propertyList
        return Property_ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: Property_ViewHolder, position: Int) {

        val binding = RowPropertyBinding.bind(holder.itemView)

        holder.itemView.apply {

            binding.apartmentSurface.text = propertyList[position].propertySize.toString()
            binding.apartmentLocationTv.text = propertyList[position].propertyPlace?.placeAddress
            binding.apartmentBedNumber.text =
                propertyList[position].propertyBedroomsNumber.toString()
            binding.apartmentPriceTv.text =
                propertyList[position].propertyPrice.toString() + " DH"
            binding.apartmentBathroomNumber.text =
                propertyList[position].propertyBathroomsNumber.toString()

            //show image randomly
            val i = propertyList[position].propertyImagesUrl.size
            Picasso.get()
                .load(propertyList[position].propertyImagesUrl[(0 until i).random()])
                .resize(300, 300).into(binding.apartmentProfileImgv)
        }

        holder.itemView.setOnClickListener {
            val apart_id = propertyList[position].propertyId
            sharedViewModelProperty.set_property_id(apart_id)

            NavHostFragment.findNavController(myContext)
                .navigate(actionId)
        }
    }

    override fun getItemCount(): Int {
        return propertyList.size
    }

    override fun getFilter(): Filter {
        return filter_property
    }

    private val filter_property: Filter = object : Filter() {
        override fun performFiltering(charSequence: CharSequence): FilterResults {
            val search_text = charSequence.toString().lowercase(Locale.getDefault())
            val temp_list: MutableList<Property> = ArrayList()
            if (search_text.isEmpty()) {
                temp_list.addAll(allProperties)
            } else {
                for (item in allProperties) {
                    /*if (item.PropertyPlace?.address?.lowercase(Locale.getDefault())
                            .contains(search_text)
                        || item.property_description.lowercase(Locale.getDefault())
                            .contains(search_text)
                    ) {
                        temp_list.add(item)
                    }*/
                }
            }
            val filterResults = FilterResults()
            filterResults.values = temp_list
            return filterResults
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun publishResults(constraint: CharSequence, filterResults: FilterResults) {
            propertyList.clear()
            propertyList.addAll(filterResults.values as Collection<Property>)
            notifyDataSetChanged()
        }
    }

}