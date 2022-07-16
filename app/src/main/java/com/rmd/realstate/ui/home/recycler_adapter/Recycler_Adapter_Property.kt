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
    private var my_context: Fragment,
    private var my_activity: FragmentActivity,
    private var action_id: Int,
    private var property_list: ArrayList<Property>

) : RecyclerView.Adapter<Recycler_Adapter_Property.Property_ViewHolder>(), Filterable {

    inner class Property_ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private lateinit var my_property_viewModel: SharedViewModel_Property
    private var all_properties = ArrayList<Property>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Property_ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.row_property, parent, false)

        my_property_viewModel =
            ViewModelProvider(my_activity)[SharedViewModel_Property::class.java]
        all_properties = property_list
        return Property_ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: Property_ViewHolder, position: Int) {

        val binding = RowPropertyBinding.bind(holder.itemView)

        holder.itemView.apply {

            binding.apartmentLocationTv.text = property_list[position].property_city
            binding.apartmentSurface.text = property_list[position].property_size.toString()
            binding.apartmentBedNumber.text = property_list[position].number_bedrooms.toString()
            binding.apartmentPriceTv.text =
                property_list[position].property_price.toString() + " DH"
            binding.apartmentBathroomNumber.text =
                property_list[position].number_bathrooms.toString()

            //show image randomly
            val i = property_list[position].image_url.size
            Picasso.get()
                .load(property_list[position].image_url[(0 until i).random()])
                .resize(300, 300).into(binding.apartmentProfileImgv)
        }

        holder.itemView.setOnClickListener {
            val apart_id = property_list[position].property_id
            my_property_viewModel.set_property_id(apart_id)

            NavHostFragment.findNavController(my_context)
                .navigate(action_id)

        }
    }


    override fun getItemCount(): Int {
        return property_list.size
    }

    override fun getFilter(): Filter {
        return filter_property
    }


    private val filter_property: Filter = object : Filter() {
        override fun performFiltering(charSequence: CharSequence): FilterResults {
            val search_text = charSequence.toString().lowercase(Locale.getDefault())
            val temp_list: MutableList<Property> = ArrayList()
            if (search_text.isEmpty()) {
                temp_list.addAll(all_properties)
            } else {
                for (item in all_properties) {
                    if (item.property_city.lowercase(Locale.getDefault()).contains(search_text)
                        || item.property_description.lowercase(Locale.getDefault())
                            .contains(search_text)
                    ) {
                        temp_list.add(item)
                    }
                }
            }
            val filterResults = FilterResults()
            filterResults.values = temp_list
            return filterResults
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun publishResults(constraint: CharSequence, filterResults: FilterResults) {
            property_list.clear()
            property_list.addAll(filterResults.values as Collection<Property>)
            notifyDataSetChanged()
        }
    }

}