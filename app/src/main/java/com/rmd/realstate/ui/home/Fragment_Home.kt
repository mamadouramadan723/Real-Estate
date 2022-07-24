package com.rmd.realstate.ui.home

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.SearchView
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
    private lateinit var layout_manager_apartment: LinearLayoutManager
    private lateinit var layout_manager_home: LinearLayoutManager
    private lateinit var filter_shared_viewModel: SharedViewModel_Filter
    private lateinit var adapter_apartment_list: Recycler_Adapter_Property
    private lateinit var adapter_home_list: Recycler_Adapter_Property
    private var property_type: String = ""
    private var horizontal_apartment_view = false
    private var horizontal_home_view = false
    private var property_list = ArrayList<Property>()
    private var apartment_list = ArrayList<Property>()
    private var home_list = ArrayList<Property>()
    private var list_city = ArrayList<String>()


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
        adapter_home_list = Recycler_Adapter_Property(
            this@Fragment_Home,
            requireActivity(),
            R.id.action_navigation_home_to_navigation_view_apart,
            home_list
        )
        adapter_apartment_list = Recycler_Adapter_Property(
            this@Fragment_Home,
            requireActivity(),
            R.id.action_navigation_home_to_navigation_view_apart,
            home_list
        )
        binding.swipeLayout.setOnRefreshListener {
            Toast.makeText(context, "Refreshing", Toast.LENGTH_SHORT).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                getParentFragmentManager().beginTransaction().detach(this).commitNow()
                getParentFragmentManager().beginTransaction().attach(this).commitNow()
                binding.swipeLayout.isRefreshing = false
            } else {
                getParentFragmentManager().beginTransaction().detach(this).attach(this).commit()
                binding.swipeLayout.isRefreshing = false
            }
        }


        //LayoutManager for recyclerview
        layout_manager_apartment = LinearLayoutManager(context)
        layout_manager_home = LinearLayoutManager(context)

        layout_manager_apartment.orientation = LinearLayoutManager.HORIZONTAL
        layout_manager_home.orientation = LinearLayoutManager.HORIZONTAL

        binding.apartmentsListRecyclerview.layoutManager = layout_manager_apartment
        binding.homesListRecyclerview.layoutManager = layout_manager_home

        //select which property type to show/hide
        //setOnCheckedChangeListener
        binding.propertyTypeRg.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.apartment_rb -> {
                    property_type = "apartment"
                    binding.apartmentLayout.visibility = View.VISIBLE
                    binding.homeLayout.visibility = View.GONE
                }
                R.id.home_rb -> {
                    property_type = "home"
                    binding.apartmentLayout.visibility = View.GONE
                    binding.homeLayout.visibility = View.VISIBLE
                }
            }
        }

        //setOnClickListener
        binding.filterImgbtn.setOnClickListener {
            NavHostFragment.findNavController(this)
                .navigate(R.id.action_navigation_home_to_navigation_filter)
        }
        binding.seeMoreApartmentsBtn.setOnClickListener {
            if (horizontal_apartment_view.equals(true)) {
                layout_manager_apartment.orientation = LinearLayoutManager.HORIZONTAL
                binding.apartmentsListRecyclerview.layoutManager = layout_manager_apartment
                horizontal_apartment_view = false
            } else {
                layout_manager_apartment.orientation = LinearLayoutManager.VERTICAL
                binding.apartmentsListRecyclerview.layoutManager = layout_manager_apartment
                horizontal_apartment_view = true
            }
        }
        binding.seeMoreHomesBtn.setOnClickListener {
            if (horizontal_home_view.equals(true)) {
                binding.seeMoreApartmentsBtn.setText("See Less")
                layout_manager_home.orientation = LinearLayoutManager.HORIZONTAL
                binding.homesListRecyclerview.layoutManager = layout_manager_home
                horizontal_home_view = false
            } else {
                binding.seeMoreApartmentsBtn.setText("See More")
                layout_manager_home.orientation = LinearLayoutManager.VERTICAL
                binding.homesListRecyclerview.layoutManager = layout_manager_home
                horizontal_home_view = false
            }
        }



        binding.searchSvw.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {

                adapter_apartment_list.filter.filter(newText)
                adapter_home_list.filter.filter(newText)
                return false
            }
        })
        //shared viewModels
        filter_shared_viewModel.my_filter.observe(viewLifecycleOwner, Observer {
            filtered_property = it
        })

        //functions
        get_all_posts()
    }

    private fun get_all_posts() = CoroutineScope(Dispatchers.IO).launch {

        property_list.clear()
        apartment_list.clear()
        home_list.clear()

        try {

            val myQuerySnapshot = property_ref.get().await()

            myQuerySnapshot.documents.mapNotNull { documentSnapshot ->

                val apartment = documentSnapshot.toObject(Property::class.java)
                apartment?.let {
                    property_list.add(apartment)

                    when (apartment.property_type) {
                        "apartment" -> {
                            apartment_list.add(apartment)
                        }
                        "home" -> {
                            home_list.add(apartment)
                        }
                        else -> {}
                    }
                }

                //As we can't directly access to UI within a coroutine, we use withContext
                withContext(Dispatchers.Main) {
                    adapter_apartment_list = Recycler_Adapter_Property(
                        this@Fragment_Home,
                        requireActivity(),
                        R.id.action_navigation_home_to_navigation_view_apart,
                        apartment_list
                    )
                    adapter_home_list = Recycler_Adapter_Property(
                        this@Fragment_Home,
                        requireActivity(),
                        R.id.action_navigation_home_to_navigation_view_apart,
                        home_list
                    )


                    binding.apartmentsListRecyclerview.adapter = adapter_apartment_list
                    binding.homesListRecyclerview.adapter = adapter_home_list
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