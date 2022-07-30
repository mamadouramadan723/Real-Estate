package com.rmd.realstate.ui.home

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private lateinit var filteredProperty: Filter
    private lateinit var binding: FragmentHomeBinding
    private lateinit var layoutManagerHome: LinearLayoutManager
    private lateinit var layoutManagerApartment: LinearLayoutManager
    private lateinit var filterSharedViewmodel: SharedViewModel_Filter
    private lateinit var recyclerAdapterHomeList: Recycler_Adapter_Property
    private lateinit var recyclerAdapterApartmentList: Recycler_Adapter_Property

    private var propertyType: String = ""
    private var horizontalHomeView = false
    private var horizontalApartmentView = false
    private var homeList = ArrayList<Property>()
    private var apartmentList = ArrayList<Property>()

    private val propertyRef = FirebaseFirestore.getInstance()
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
        filterSharedViewmodel =
            ViewModelProvider(requireActivity())[SharedViewModel_Filter::class.java]
        recyclerAdapterHomeList = Recycler_Adapter_Property(
            this@Fragment_Home,
            requireActivity(),
            R.id.action_navigation_home_to_navigation_view_apart,
            homeList
        )
        recyclerAdapterApartmentList = Recycler_Adapter_Property(
            this@Fragment_Home,
            requireActivity(),
            R.id.action_navigation_home_to_navigation_view_apart,
            homeList
        )
        binding.swipeLayout.setOnRefreshListener {
            Toast.makeText(context, "Refreshing", Toast.LENGTH_SHORT).show()
            refresh()
        }

        //LayoutManager for recyclerview
        layoutManagerApartment = LinearLayoutManager(context)
        layoutManagerHome = LinearLayoutManager(context)

        layoutManagerApartment.orientation = LinearLayoutManager.HORIZONTAL
        layoutManagerHome.orientation = LinearLayoutManager.HORIZONTAL

        binding.apartmentsListRecyclerview.layoutManager = layoutManagerApartment
        binding.homesListRecyclerview.layoutManager = layoutManagerHome

        //select which property type to show/hide
        //setOnCheckedChangeListener
        binding.propertyTypeRg.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.apartment_rb -> {
                    propertyType = "apartment"
                    binding.apartmentLayout.visibility = View.VISIBLE
                    binding.homeLayout.visibility = View.GONE
                }
                R.id.home_rb -> {
                    propertyType = "home"
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
            if (horizontalApartmentView.equals(true)) {
                layoutManagerApartment.orientation = LinearLayoutManager.HORIZONTAL
                binding.apartmentsListRecyclerview.layoutManager = layoutManagerApartment
                horizontalApartmentView = false
            } else {
                layoutManagerApartment.orientation = LinearLayoutManager.VERTICAL
                binding.apartmentsListRecyclerview.layoutManager = layoutManagerApartment
                horizontalApartmentView = true
            }
        }
        binding.seeMoreHomesBtn.setOnClickListener {
            if (horizontalHomeView.equals(true)) {
                binding.seeMoreApartmentsBtn.setText("See Less")
                layoutManagerHome.orientation = LinearLayoutManager.HORIZONTAL
                binding.homesListRecyclerview.layoutManager = layoutManagerHome
                horizontalHomeView = false
            } else {
                binding.seeMoreApartmentsBtn.setText("See More")
                layoutManagerHome.orientation = LinearLayoutManager.VERTICAL
                binding.homesListRecyclerview.layoutManager = layoutManagerHome
                horizontalHomeView = false
            }
        }

        binding.searchSvw.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {

                recyclerAdapterApartmentList.filter.filter(newText)
                recyclerAdapterHomeList.filter.filter(newText)
                return false
            }
        })
        //shared viewModels
        filterSharedViewmodel.my_filter.observe(viewLifecycleOwner, Observer {
            filteredProperty = it
        })

        //functions
        getAllPosts()
    }

    private fun refresh() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            getParentFragmentManager().beginTransaction().detach(this).commitNow()
            getParentFragmentManager().beginTransaction().attach(this).commitNow()
            binding.swipeLayout.isRefreshing = false
        } else {
            getParentFragmentManager().beginTransaction().detach(this).attach(this).commit()
            binding.swipeLayout.isRefreshing = false
        }
    }

    private fun getAllPosts() = CoroutineScope(Dispatchers.IO).launch {
        apartmentList.clear()
        homeList.clear()

        try {
            val myQuerySnapshot = propertyRef.get().await()

            myQuerySnapshot.documents.mapNotNull { documentSnapshot ->
                val property = documentSnapshot.toObject(Property::class.java)

                property?.let {
                    when (property.propertyType) {
                        "apartment" -> {
                            apartmentList.add(property)
                        }
                        "home" -> {
                            homeList.add(property)
                        }
                        else -> {}
                    }
                }

                //As we can't directly access to UI within a coroutine, we use withContext
                withContext(Dispatchers.Main) {

                    recyclerAdapterApartmentList = Recycler_Adapter_Property(
                        this@Fragment_Home,
                        requireActivity(),
                        R.id.action_navigation_home_to_navigation_view_apart,
                        apartmentList
                    )
                    recyclerAdapterHomeList = Recycler_Adapter_Property(
                        this@Fragment_Home,
                        requireActivity(),
                        R.id.action_navigation_home_to_navigation_view_apart,
                        homeList
                    )

                    binding.apartmentsListRecyclerview.adapter = recyclerAdapterApartmentList
                    binding.homesListRecyclerview.adapter = recyclerAdapterHomeList
                }
            }
        } catch (e: Exception) {
            //As we can't directly access to UI within a coroutine, we use withContext
            withContext(Dispatchers.Main) {
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                Log.e(" +++++", "" + e.message)
            }
        }
    }
}