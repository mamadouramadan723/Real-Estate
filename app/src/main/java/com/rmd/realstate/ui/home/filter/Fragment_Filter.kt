package com.rmd.realstate.ui.home.filter

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.rmd.realstate.R
import com.rmd.realstate.databinding.FragmentFilterBinding
import com.rmd.realstate.model.Filter
import com.rmd.realstate.view_model.SharedViewModel_Filter


class Fragment_Filter : Fragment(), AdapterView.OnItemSelectedListener {

    //variable declaration
    private lateinit var binding: FragmentFilterBinding
    private lateinit var filter_shared_viewModel: SharedViewModel_Filter
    private lateinit var applied_property: Filter
    private var min_property_size: Int = 40
    private var max_property_size: Int = 100
    private var min_property_price: Int = 3500
    private var max_property_price: Int = 10000
    private var number_bedrooms: Int = 1
    private var property_type: String = ""
    private var property_region: String = ""
    private var property_city: String = ""
    private var check_balcony = false
    private var check_garage = false
    private var check_bath = false
    private var check_dinning = false
    private var check_baby = false
    private var check_tv = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_filter, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //affectations
        binding = FragmentFilterBinding.bind(view)
        filter_shared_viewModel =
            ViewModelProvider(requireActivity())[SharedViewModel_Filter::class.java]

        //spinners
        binding.spinnerRegion.onItemSelectedListener = this
        binding.spinnerCity.onItemSelectedListener = this

        // Setup the new range seek bar
        binding.rangePriceSeekBar.setRangeValues(1000, 20000)
        binding.rangeSizeSeekBar.setRangeValues(20, 400)
        binding.rangePriceSeekBar.selectedMinValue = min_property_price
        binding.rangePriceSeekBar.selectedMaxValue = max_property_price
        binding.rangeSizeSeekBar.selectedMinValue = min_property_size
        binding.rangeSizeSeekBar.selectedMaxValue = max_property_size

        binding.rangePriceTv.text = "$min_property_price DAM - $max_property_price DAM"
        binding.rangeSizeTv.text = "$min_property_size m² - $max_property_size m²"

        binding.rangePriceSeekBar.setOnRangeSeekBarChangeListener { _, minValue, maxValue ->
            binding.rangePriceTv.text = "$minValue DAM - $maxValue DAM"
        }
        binding.rangeSizeSeekBar.setOnRangeSeekBarChangeListener { _, minValue, maxValue ->
            binding.rangeSizeTv.text = "$minValue m² - $maxValue m²"
        }

        //setOnCheckedChangeListener
        binding.propertyTypeRg.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.apartment_rb -> {
                    property_type = "apartment"
                }
                R.id.home_rb -> {
                    property_type = "home"
                }
            }
        }
        binding.bedroomRg.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.studio_rb -> {
                    number_bedrooms = 0
                }
                R.id.one_bed_rb -> {
                    number_bedrooms = 1
                }
                R.id.two_bed_rb -> {
                    number_bedrooms = 2
                }
                R.id.three_bed_rb -> {
                    number_bedrooms = 3
                }
                R.id.four_bed_rb -> {
                    number_bedrooms = 4
                }
            }
        }

        //setOnClickListener
        binding.balconyRb.setOnClickListener {
            check_balcony = !check_balcony
            binding.balconyRb.isChecked = check_balcony
        }
        binding.garageRb.setOnClickListener {
            check_garage = !check_garage
            binding.garageRb.isChecked = check_garage
        }
        binding.bathRb.setOnClickListener {
            check_bath = !check_bath
            binding.bathRb.isChecked = check_bath
        }
        binding.diningRoomRb.setOnClickListener {
            check_dinning = !check_dinning
            binding.diningRoomRb.isChecked = check_dinning
        }
        binding.bedroomBabyRb.setOnClickListener {
            check_baby = !check_baby
            binding.bedroomBabyRb.isChecked = check_baby
        }
        binding.tvRoomRb.setOnClickListener {
            check_tv = !check_tv
            binding.tvRoomRb.isChecked = check_tv
        }

        binding.applyAndSearchBtn.setOnClickListener {
            applied_property = Filter(
                property_type, min_property_size, max_property_size,
                min_property_price, max_property_price, number_bedrooms, check_bath, check_dinning,
                check_garage, check_balcony, check_baby, check_tv, property_region, property_city
            )

            filter_shared_viewModel.apply_and_send(applied_property)

            NavHostFragment.findNavController(this)
                .navigate(R.id.action_navigation_filter_to_navigation_home)
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        parent as Spinner
        when (parent.id) {
            R.id.spinner_region -> {
                property_region = parent.getItemAtPosition(position).toString()
                when (position) {
                    0 -> {
                        val list_default_city =
                            resources.getStringArray(R.array.ville_BeniMellalKhénifra)
                        val adapter_city = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            list_default_city
                        )
                        binding.spinnerCity.adapter = adapter_city
                    }
                    1 -> {
                        val list_default_city =
                            resources.getStringArray(R.array.ville_CasablancaSettat)
                        val adapter_city = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            list_default_city
                        )
                        binding.spinnerCity.adapter = adapter_city
                    }
                    2 -> {
                        val list_default_city =
                            resources.getStringArray(R.array.ville_DraaTafilalet)
                        val adapter_city = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            list_default_city
                        )
                        binding.spinnerCity.adapter = adapter_city
                    }
                    3 -> {
                        val list_default_city =
                            resources.getStringArray(R.array.ville_EdDakhlaOuededDahab)
                        val adapter_city = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            list_default_city
                        )
                        binding.spinnerCity.adapter = adapter_city
                    }
                    4 -> {
                        val list_default_city = resources.getStringArray(R.array.ville_FesMeknes)
                        val adapter_city = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            list_default_city
                        )
                        binding.spinnerCity.adapter = adapter_city
                    }
                    5 -> {
                        val list_default_city =
                            resources.getStringArray(R.array.ville_GuelmimOuedNoun)
                        val adapter_city = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            list_default_city
                        )
                        binding.spinnerCity.adapter = adapter_city
                    }
                    6 -> {
                        val list_default_city =
                            resources.getStringArray(R.array.ville_LaayouneSaguiaalHamra)
                        val adapter_city = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            list_default_city
                        )
                        binding.spinnerCity.adapter = adapter_city
                    }
                    7 -> {
                        val list_default_city =
                            resources.getStringArray(R.array.ville_MarrakechSafi)
                        val adapter_city = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            list_default_city
                        )
                        binding.spinnerCity.adapter = adapter_city
                    }
                    8 -> {
                        val list_default_city = resources.getStringArray(R.array.ville_Oriental)
                        val adapter_city = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            list_default_city
                        )
                        binding.spinnerCity.adapter = adapter_city
                    }
                    9 -> {
                        val list_default_city =
                            resources.getStringArray(R.array.ville_RabatSaleKenitra)
                        val adapter_city = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            list_default_city
                        )
                        binding.spinnerCity.adapter = adapter_city
                    }
                    10 -> {
                        val list_default_city = resources.getStringArray(R.array.ville_SousMassa)
                        val adapter_city = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            list_default_city
                        )
                        binding.spinnerCity.adapter = adapter_city
                    }
                    11 -> {
                        val list_default_city =
                            resources.getStringArray(R.array.ville_TangerTetouanAlHoceima)
                        val adapter_city = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            list_default_city
                        )
                        binding.spinnerCity.adapter = adapter_city
                    }
                }
            }
            R.id.spinner_city -> {
                property_city = parent.getItemAtPosition(position).toString()
            }
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
    }

}