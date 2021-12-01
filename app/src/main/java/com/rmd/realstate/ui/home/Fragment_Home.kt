package com.rmd.realstate.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.rmd.realstate.R
import com.rmd.realstate.databinding.FragmentHomeBinding
import com.rmd.realstate.model.Filter
import com.rmd.realstate.model.Property
import com.rmd.realstate.ui.home.recycler_adapter.Recycler_Adapter_Property
import com.rmd.realstate.view_model.SharedViewModel_Filter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class Fragment_Home : Fragment() {

    //declarations

    private lateinit var filtered_property: Filter
    private lateinit var binding: FragmentHomeBinding
    private lateinit var layout_manager: LinearLayoutManager
    private lateinit var filter_shared_viewModel: SharedViewModel_Filter

    private var property_type: String = ""
    private var property_list = ArrayList<Property>()

    private val property_ref = FirebaseFirestore.getInstance()
        .collection("property")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //initializations
        binding = FragmentHomeBinding.bind(view)
        filter_shared_viewModel =
            ViewModelProvider(requireActivity())[SharedViewModel_Filter::class.java]

        //LayoutManager for recyclerview
        layout_manager = LinearLayoutManager(context)
        layout_manager.orientation = LinearLayoutManager.HORIZONTAL
        binding.apartmentsListRecyclerview.layoutManager = layout_manager

        //select which property type to show/hide
        //setOnCheckedChangeListener
        binding.propertyTypeRg.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.apartment_rb -> {
                    property_type = "apartment"
                    binding.apartmentLayout.visibility = View.VISIBLE
                    binding.homeLayout.visibility = View.GONE
                    binding.officeLayout.visibility = View.GONE
                    binding.commerceLayout.visibility = View.GONE
                }
                R.id.home_rb -> {
                    property_type = "home"
                    binding.apartmentLayout.visibility = View.GONE
                    binding.homeLayout.visibility = View.VISIBLE
                    binding.officeLayout.visibility = View.GONE
                    binding.commerceLayout.visibility = View.GONE
                }
                R.id.office_rb -> {
                    property_type = "office"
                    binding.apartmentLayout.visibility = View.GONE
                    binding.homeLayout.visibility = View.GONE
                    binding.officeLayout.visibility = View.VISIBLE
                    binding.commerceLayout.visibility = View.GONE
                }
                R.id.commerce_rb -> {
                    property_type = "commerce"
                    binding.apartmentLayout.visibility = View.GONE
                    binding.homeLayout.visibility = View.GONE
                    binding.officeLayout.visibility = View.GONE
                    binding.commerceLayout.visibility = View.VISIBLE
                }
            }
        }

        //setOnClickListener
        binding.filterImgbtn.setOnClickListener {
            NavHostFragment.findNavController(this)
                .navigate(R.id.action_navigation_home_to_navigation_filter)
        }

        //shared viewModels
        filter_shared_viewModel.my_filter.observe(viewLifecycleOwner, Observer {
            filtered_property = it
        })

        //functions
        get_all_posts()
    }

    private fun get_all_posts() = CoroutineScope(Dispatchers.IO).launch {

        property_list.clear()

        try {

            val myQuerySnapshot = property_ref.get().await()

            myQuerySnapshot.documents.mapNotNull { documentSnapshot ->

                val apartment = documentSnapshot.toObject(Property::class.java)
                property_list.add(apartment!!)

                //As we can't directly access to UI within a coroutine, we use withContext
                withContext(Dispatchers.Main) {

                    val adapter_property_list = Recycler_Adapter_Property(
                        this@Fragment_Home,
                        requireActivity(),
                        R.id.action_navigation_home_to_navigation_view_apart,
                        property_list.reversed()
                    )

                    binding.apartmentsListRecyclerview.adapter = adapter_property_list
                }
            }
        } catch (e: Exception) {

            //As we can't directly access to UI within a coroutine, we use withContext
            withContext(Dispatchers.Main) {
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}