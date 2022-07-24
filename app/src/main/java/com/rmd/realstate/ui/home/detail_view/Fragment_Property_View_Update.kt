package com.rmd.realstate.ui.home.detail_view

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.libraries.places.api.model.Place
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.rmd.realstate.R
import com.rmd.realstate.activity.Activity_Login_or_Register
import com.rmd.realstate.databinding.FragmentPostBinding
import com.rmd.realstate.model.Property
import com.rmd.realstate.ui.home.recycler_adapter.Recycler_Adapter_Loaded_Image_Url
import com.rmd.realstate.ui.post.Fragment_Post
import com.rmd.realstate.ui.post.recycler_adapter.Recycler_Adapter_Loaded_Image_Uri
import com.rmd.realstate.view_model.SharedViewModel_Property
import com.rtchagas.pingplacepicker.PingPlacePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class Fragment_Property_View_Update : Fragment() {

    private lateinit var my_property_viewModel: SharedViewModel_Property
    private lateinit var applied_published: Property
    private lateinit var auth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private lateinit var binding: FragmentPostBinding
    private var property__id: String = ""
    private val post_apartment_ref = FirebaseFirestore.getInstance()
        .collection("property")

    private val PICK_IMAGE_REQUEST = 1234
    private var image_list = ArrayList<Uri?>()
    private var image_name_list = ArrayList<String>()
    private var image_url = ArrayList<String>()
    private var number_bedrooms = 1
    private var number_bathrooms = 1
    private var property_size = 0
    private var property_price = 0
    private var property_type = "apartment"
    private lateinit var property_place : Place
    private var property_description = ""
    private var property_id = ""
    private var check_balcony = false
    private var check_garage = false
    private var check_bath = false
    private var check_dinning = false
    private var check_baby = false
    private var check_tv = false
    //

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        auth = FirebaseAuth.getInstance()
        return inflater.inflate(R.layout.fragment_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkUserConnection()

        binding = FragmentPostBinding.bind(view)
        progressDialog = ProgressDialog(requireContext())
        binding.deleteLoadedImagesBtn.visibility = View.VISIBLE
        binding.deleteLoadedImagesBtn.setText("Delete all Images")

        //adapter
        //val adapter_loaded_images = Recycler_Adapter_Loaded_Image_Uri(image_list)
        val mLayoutManager = LinearLayoutManager(context)
        mLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        binding.imagesAddedListRecyclerview.layoutManager = mLayoutManager


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
        binding.bathroomRg.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.one_bath_rb -> {
                    number_bathrooms = 1
                }
                R.id.two_bath_rb -> {
                    number_bathrooms = 2
                }
                R.id.three_bath_rb -> {
                    number_bathrooms = 3
                }
                R.id.four_bath_rb -> {
                    number_bathrooms = 4
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
        binding.addImagesImgv.setOnClickListener {
            handleImageClick()
        }
        binding.chooseLocationBtn.setOnClickListener {
            setUpMap()
            showPlacePicker()
        }

        binding.deleteLoadedImagesBtn.setOnClickListener {
            image_list.clear()
            image_name_list.clear()
            if (image_url.isNotEmpty()) {
                //popup for confirmation
                val alertDialog = AlertDialog.Builder(requireContext())
                alertDialog.setTitle("Delete Images from The Cloud")
                alertDialog.setMessage("Are you sure ?")
                    .setIcon(R.drawable.ic_warning)
                alertDialog.setPositiveButton("Yes") { _, _ ->
                    empty_image_from_firestorage()
                }
                alertDialog.setNeutralButton("Cancel") { _, _ ->
                }
                alertDialog.create().show()

            }
        }

        binding.applyAndPublishBtn.setOnClickListener {

            if (binding.propertyPriceEdt.text.toString().isNotEmpty()) {
                property_price = binding.propertyPriceEdt.text.toString().toInt()
            } else {
                Toast.makeText(context, "Price must not be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (binding.propertySizeEdt.text.toString().isNotEmpty()) {
                property_size = binding.propertySizeEdt.text.toString().toInt()
            } else {
                Toast.makeText(context, "Size must not be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            property_description = binding.addDescriptionEdt.text.toString()

            progressDialog.setMessage("uploading...")
            progressDialog.show()
            uploadImages()
        }
        //
        my_property_viewModel =
            ViewModelProvider(requireActivity())[SharedViewModel_Property::class.java]

        //shared viewModels
        my_property_viewModel.my_property.observe(viewLifecycleOwner, Observer {
            property__id = it
        })

        get_all_data()
    }

    private fun empty_image_from_firestorage() {
        //image_url.clear()
        Toast.makeText(
            context,
            "image not deleted yet from cloud, see you in the next version",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun checkUserConnection() {
        val user = auth.currentUser
        if (user == null) {
            val intent = Intent(context, Activity_Login_or_Register::class.java)
            startActivity(intent)
            activity?.finish()
        }
    }

    private fun get_all_data() = CoroutineScope(Dispatchers.IO).launch {

        try {
            withContext(Dispatchers.Main) {
                val documentSnapshot = post_apartment_ref.document(property__id).get().await()
                val apartment = documentSnapshot.toObject(Property::class.java)
                apartment?.let {
                    //get values
                    number_bedrooms = apartment.number_bedrooms
                    number_bathrooms = apartment.number_bathrooms
                    property_size = apartment.property_size
                    property_price = apartment.property_price
                    property_type = apartment.property_type
                    property_place = apartment.property_place
                    property_description = apartment.property_description
                    property_id = apartment.property_id
                    check_balcony = apartment.has_balcony
                    check_garage = apartment.has_garage
                    check_bath = apartment.has_bath_place
                    check_dinning = apartment.has_dinner_room
                    check_baby = apartment.has_bedroom_baby
                    check_tv = apartment.has_tv_room
                    image_url = apartment.image_url

                    //set values
                    binding.propertyPriceEdt.setText(property_price.toString())
                    binding.propertySizeEdt.setText(property_size.toString())
                    binding.addDescriptionEdt.setText(property_description)
                    //property_type
                    var checked_property_type_id = 0
                    when (property_type) {
                        "apartment" -> checked_property_type_id = R.id.apartment_rb
                        "home" -> checked_property_type_id = R.id.home_rb
                    }
                    binding.propertyTypeRg.check(checked_property_type_id)

                    //bed numbers
                    var checked_number_bedrooms_id = 0
                    when (number_bedrooms) {
                        0 -> checked_number_bedrooms_id = R.id.studio_rb
                        1 -> checked_number_bedrooms_id = R.id.one_bed_rb
                        2 -> checked_number_bedrooms_id = R.id.two_bed_rb
                        3 -> checked_number_bedrooms_id = R.id.three_bed_rb
                        4 -> checked_number_bedrooms_id = R.id.four_bed_rb
                    }
                    binding.bedroomRg.check(checked_number_bedrooms_id)

                    //bath numbers
                    var checked_number_bathrooms_id = 0
                    when (number_bathrooms) {
                        1 -> checked_number_bathrooms_id = R.id.one_bath_rb
                        2 -> checked_number_bathrooms_id = R.id.two_bath_rb
                        3 -> checked_number_bathrooms_id = R.id.three_bath_rb
                        4 -> checked_number_bathrooms_id = R.id.four_bath_rb
                    }
                    binding.bathroomRg.check(checked_number_bathrooms_id)

                    //facilities
                    binding.bathRb.isChecked = check_bath
                    binding.diningRoomRb.isChecked = check_dinning
                    binding.garageRb.isChecked = check_garage
                    binding.balconyRb.isChecked = check_balcony
                    binding.bedroomBabyRb.isChecked = check_baby
                    binding.tvRoomRb.isChecked = check_tv

                    //images
                    val adapter_loaded_images = Recycler_Adapter_Loaded_Image_Url(image_url)
                    binding.imagesAddedListRecyclerview.adapter = adapter_loaded_images
                }
            }
        } catch (e: Exception) {
            //vu qu'on ne peu acceder au UI dans un coroutine on use withContext
            withContext(Dispatchers.Main) {
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun uploadImages() = CoroutineScope(Dispatchers.IO).launch {
        val user = auth.currentUser

        try {
            if (image_list.size > 0) {

                for (i in 0..(image_list.size - 1)) {
                    val imageuri: Uri? = image_list[i]

                    val storageReference = FirebaseStorage.getInstance()
                        .getReference("Post Images/" + user?.uid + "/images for " + property_id)
                        .child(image_name_list[i])

                    val uploadTask = imageuri?.let { storageReference.putFile(it).await() }
                    val my_url = uploadTask?.storage?.downloadUrl?.await()

                    image_url.add(my_url.toString())
                }
                //without this, we will get error for the progressDialog:
                //Can't create handler inside thread that has not called Looper.prepare()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Update with New Images", Toast.LENGTH_SHORT).show()
                    val update: MutableMap<String, Any> = HashMap()
                    update["property_type"] = property_type
                    update["property_size"] = property_size
                    update["property_price"] = property_price
                    update["number_bedrooms"] = number_bedrooms
                    update["number_bathrooms"] = number_bathrooms
                    update["check_bath"] = check_bath
                    update["check_dinning"] = check_dinning
                    update["check_garage"] = check_garage
                    update["check_balcony"] = check_balcony
                    update["check_baby"] = check_baby
                    update["check_tv"] = check_tv
                    update["property_place"] = property_place
                    update["property_description"] = property_description
                    update["image_url"] = image_url
                    apply_and_update(update)
                }
            } else {

                withContext(Dispatchers.Main) {
                    if (image_url.isEmpty()) {
                        progressDialog.dismiss()
                        Toast.makeText(context, "You've to set new Images", Toast.LENGTH_SHORT)
                            .show()
                        return@withContext
                    }
                    Toast.makeText(context, "Update without New Images", Toast.LENGTH_SHORT).show()
                    val update: MutableMap<String, Any> = HashMap()
                    update["property_type"] = property_type
                    update["property_size"] = property_size
                    update["property_price"] = property_price
                    update["number_bedrooms"] = number_bedrooms
                    update["number_bathrooms"] = number_bathrooms
                    update["check_bath"] = check_bath
                    update["check_dinning"] = check_dinning
                    update["check_garage"] = check_garage
                    update["check_balcony"] = check_balcony
                    update["check_baby"] = check_baby
                    update["check_tv"] = check_tv
                    update["property_place"] = property_place
                    update["property_description"] = property_description
                    apply_and_update(update)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "fatal error : " + e.message, Toast.LENGTH_LONG).show()
                Log.d("*****", "" + e.message)
            }
        }
    }

    private fun apply_and_update(update: MutableMap<String, Any>) =
        CoroutineScope(Dispatchers.IO).launch {
            val user = auth.currentUser

            try {
                post_apartment_ref.document(property_id).update(update).await()
                //vu qu'on ne peu acceder au UI dans un coroutine on use withContext
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), "Post Updated successfully", Toast.LENGTH_LONG)
                        .show()
                    NavHostFragment.findNavController(this@Fragment_Property_View_Update)
                        .navigate(R.id.action_navigation_post_to_navigation_home)
                    progressDialog.dismiss()
                }
            } catch (e: Exception) {
                //vu qu'on ne peu acceder au UI dans un coroutine on use withContext
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                }
            }
        }


    private fun handleImageClick() {
        Intent(Intent.ACTION_GET_CONTENT).also { intent ->
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.clipData?.let { clipdata ->
                val count = clipdata.itemCount
                var currentImageSelect = 0
                while (currentImageSelect < count) {
                    val imageuri: Uri = clipdata.getItemAt(currentImageSelect).uri
                    val imagename = "image_$currentImageSelect"
                    image_list.add(imageuri)
                    image_name_list.add(imagename)
                    currentImageSelect += 1
                }
                val adapter_loaded_images = Recycler_Adapter_Loaded_Image_Uri(image_list)
                binding.imagesAddedListRecyclerview.adapter = adapter_loaded_images
                binding.selectedImagesTv.visibility = View.VISIBLE
                binding.addOrChangeTv.text = "Change Image(s)"

                Toast.makeText(context, "You have selected $count image(s)", Toast.LENGTH_LONG)
                    .show()
            }
        }

        if ((requestCode == PLACE_PICKER_REQUEST) && (resultCode == Activity.RESULT_OK)) {
            val place: Place? = PingPlacePicker.getPlace(data!!)
            property_place = place!!
            binding.locationSelectedTv.text = "You selected: ${place?.name}"
            Log.d("++++++ : ", "address : ${place?.address}, latlng : ${place?.latLng}, photo : ${place?.name}")
            //Toast.makeText(context, "You selected: ${place?.name}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
    }

    private fun showPlacePicker() {
        val builder = PingPlacePicker.IntentBuilder()
        builder
            .setAndroidApiKey(getString(R.string.PLACES_API_KEY))
            .setMapsApiKey(getString(R.string.PLACES_API_KEY))

        try {
            val placeIntent = builder.build(requireActivity())
            startActivityForResult(placeIntent, PLACE_PICKER_REQUEST)
        } catch (e: GooglePlayServicesRepairableException) {
            e.printStackTrace()
        } catch (e: GooglePlayServicesNotAvailableException) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val PLACE_PICKER_REQUEST = 1
        private const val LOCATION_PERMISSION_REQUEST_CODE = 2
    }
}