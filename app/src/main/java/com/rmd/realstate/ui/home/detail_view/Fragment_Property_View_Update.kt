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
import com.rmd.realstate.model.PropertyPlace
import com.rmd.realstate.ui.home.recycler_adapter.Recycler_Adapter_Loaded_Image_Url
import com.rmd.realstate.ui.post.recycler_adapter.Recycler_Adapter_Loaded_Image_Uri
import com.rmd.realstate.view_model.SharedViewModel_Property
import com.rtchagas.pingplacepicker.PingPlacePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class Fragment_Property_View_Update : Fragment() {

    private lateinit var propertyViewmodel: SharedViewModel_Property
    private lateinit var applied_published: Property
    private lateinit var auth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private lateinit var binding: FragmentPostBinding
    private var mPropertyId: String = ""
    private val post_apartment_ref = FirebaseFirestore.getInstance()
        .collection("property")

    private val PICK_IMAGE_REQUEST = 1
    private var imageList = ArrayList<Uri?>()
    private var imageNameList = ArrayList<String>()
    private var propertyImagesUrl = ArrayList<String>()
    private var propertyBedroomsNumber = 1
    private var propertyBathroomsNumber = 1
    private var propertySize = 0
    private var propertyPrice = 0
    private var propertyType = "apartment"
    private var propertyPlace : PropertyPlace? = null
    private var propertyDescription = ""
    private var propertyId = ""
    private var propertyHasBalcony = false
    private var propertyHasGarage = false
    private var propertyHasBathPlace = false
    private var propertyHasDiningRoom = false
    private var propertyHasBabyBedroom = false
    private var propertyHasTVRoom = false
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
                    propertyType = "apartment"
                }
                R.id.home_rb -> {
                    propertyType = "home"
                }
            }
        }
        binding.bedroomRg.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.studio_rb -> {
                    propertyBedroomsNumber = 0
                }
                R.id.one_bed_rb -> {
                    propertyBedroomsNumber = 1
                }
                R.id.two_bed_rb -> {
                    propertyBedroomsNumber = 2
                }
                R.id.three_bed_rb -> {
                    propertyBedroomsNumber = 3
                }
                R.id.four_bed_rb -> {
                    propertyBedroomsNumber = 4
                }
            }
        }
        binding.bathroomRg.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.one_bath_rb -> {
                    propertyBathroomsNumber = 1
                }
                R.id.two_bath_rb -> {
                    propertyBathroomsNumber = 2
                }
                R.id.three_bath_rb -> {
                    propertyBathroomsNumber = 3
                }
                R.id.four_bath_rb -> {
                    propertyBathroomsNumber = 4
                }
            }
        }

        //setOnClickListener
        binding.balconyRb.setOnClickListener {
            propertyHasBalcony = !propertyHasBalcony
            binding.balconyRb.isChecked = propertyHasBalcony
        }
        binding.garageRb.setOnClickListener {
            propertyHasGarage = !propertyHasGarage
            binding.garageRb.isChecked = propertyHasGarage
        }
        binding.bathRb.setOnClickListener {
            propertyHasBathPlace = !propertyHasBathPlace
            binding.bathRb.isChecked = propertyHasBathPlace
        }
        binding.diningRoomRb.setOnClickListener {
            propertyHasDiningRoom = !propertyHasDiningRoom
            binding.diningRoomRb.isChecked = propertyHasDiningRoom
        }
        binding.bedroomBabyRb.setOnClickListener {
            propertyHasBabyBedroom = !propertyHasBabyBedroom
            binding.bedroomBabyRb.isChecked = propertyHasBabyBedroom
        }
        binding.tvRoomRb.setOnClickListener {
            propertyHasTVRoom = !propertyHasTVRoom
            binding.tvRoomRb.isChecked = propertyHasTVRoom
        }
        binding.addImagesImgv.setOnClickListener {
            handleImageClick()
        }
        binding.chooseLocationBtn.setOnClickListener {
            setUpMap()
            showPlacePicker()
        }

        binding.deleteLoadedImagesBtn.setOnClickListener {
            imageList.clear()
            imageNameList.clear()
            if (propertyImagesUrl.isNotEmpty()) {
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
                propertyPrice = binding.propertyPriceEdt.text.toString().toInt()
            } else {
                Toast.makeText(context, "Price must not be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (binding.propertySizeEdt.text.toString().isNotEmpty()) {
                propertySize = binding.propertySizeEdt.text.toString().toInt()
            } else {
                Toast.makeText(context, "Size must not be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            propertyDescription = binding.addDescriptionEdt.text.toString()

            progressDialog.setMessage("uploading...")
            progressDialog.show()
            uploadImages()
        }
        //
        propertyViewmodel =
            ViewModelProvider(requireActivity())[SharedViewModel_Property::class.java]

        //shared viewModels
        propertyViewmodel.my_property.observe(viewLifecycleOwner, Observer {
            mPropertyId = it
        })

        get_all_data()
    }

    private fun empty_image_from_firestorage() {
        //propertyImagesUrl.clear()
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
                val documentSnapshot = post_apartment_ref.document(mPropertyId).get().await()
                val property = documentSnapshot.toObject(Property::class.java)
                property?.let {
                    //get values
                    propertyBedroomsNumber = property.propertyBedroomsNumber
                    propertyBathroomsNumber = property.propertyBathroomsNumber
                    propertySize = property.propertySize
                    propertyPrice = property.propertyPrice
                    propertyType = property.propertyType
                    propertyPlace = property.propertyPlace!!
                    propertyDescription = property.propertyDescription
                    propertyId = property.propertyId
                    propertyHasBalcony = property.propertyHasBalcony
                    propertyHasGarage = property.propertyHasGarage
                    propertyHasBathPlace = property.propertyHasBathPlace
                    propertyHasDiningRoom = property.propertyHasDiningRoom
                    propertyHasBabyBedroom = property.propertyHasBabyBedroom
                    propertyHasTVRoom = property.propertyHasTVRoom
                    propertyImagesUrl = property.propertyImagesUrl

                    //set values
                    binding.propertyPriceEdt.setText(propertyPrice.toString())
                    binding.propertySizeEdt.setText(propertySize.toString())
                    binding.addDescriptionEdt.setText(propertyDescription)
                    //propertyType
                    var checkedPropertyTypeId = 0
                    when (propertyType) {
                        "apartment" -> checkedPropertyTypeId = R.id.apartment_rb
                        "home" -> checkedPropertyTypeId = R.id.home_rb
                    }
                    binding.propertyTypeRg.check(checkedPropertyTypeId)

                    //bed numbers
                    var checkedPropertyBedroomsNumberId = 0
                    when (propertyBedroomsNumber) {
                        0 -> checkedPropertyBedroomsNumberId = R.id.studio_rb
                        1 -> checkedPropertyBedroomsNumberId = R.id.one_bed_rb
                        2 -> checkedPropertyBedroomsNumberId = R.id.two_bed_rb
                        3 -> checkedPropertyBedroomsNumberId = R.id.three_bed_rb
                        4 -> checkedPropertyBedroomsNumberId = R.id.four_bed_rb
                    }
                    binding.bedroomRg.check(checkedPropertyBedroomsNumberId)

                    //bath numbers
                    var checkedPropertyBathroomsNumberId = 0
                    when (propertyBathroomsNumber) {
                        1 -> checkedPropertyBathroomsNumberId = R.id.one_bath_rb
                        2 -> checkedPropertyBathroomsNumberId = R.id.two_bath_rb
                        3 -> checkedPropertyBathroomsNumberId = R.id.three_bath_rb
                        4 -> checkedPropertyBathroomsNumberId = R.id.four_bath_rb
                    }
                    binding.bathroomRg.check(checkedPropertyBathroomsNumberId)

                    //facilities
                    binding.bathRb.isChecked = propertyHasBathPlace
                    binding.diningRoomRb.isChecked = propertyHasDiningRoom
                    binding.garageRb.isChecked = propertyHasGarage
                    binding.balconyRb.isChecked = propertyHasBalcony
                    binding.bedroomBabyRb.isChecked = propertyHasBabyBedroom
                    binding.tvRoomRb.isChecked = propertyHasTVRoom

                    //images
                    val recyclerAdapterLoadedImageUrl = Recycler_Adapter_Loaded_Image_Url(propertyImagesUrl)
                    binding.imagesAddedListRecyclerview.adapter = recyclerAdapterLoadedImageUrl
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
            if (imageList.size > 0) {

                for (i in 0..(imageList.size - 1)) {
                    val imageUri: Uri? = imageList[i]

                    val storageReference = FirebaseStorage.getInstance()
                        .getReference("Post Images/" + user?.uid + "/images for " + propertyId)
                        .child(imageNameList[i])

                    val uploadTask = imageUri?.let { storageReference.putFile(it).await() }
                    val myUrl = uploadTask?.storage?.downloadUrl?.await()

                    propertyImagesUrl.add(myUrl.toString())
                }
                //without this, we will get error for the progressDialog:
                //Can't create handler inside thread that has not called Looper.prepare()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Update with New Images", Toast.LENGTH_SHORT).show()
                    val update: MutableMap<String, Any> = HashMap()
                    update["propertyType"] = propertyType
                    update["propertySize"] = propertySize
                    update["propertyPrice"] = propertyPrice
                    update["propertyBedroomsNumber"] = propertyBedroomsNumber
                    update["propertyBathroomsNumber"] = propertyBathroomsNumber
                    update["propertyHasBathPlace"] = propertyHasBathPlace
                    update["propertyHasDiningRoom"] = propertyHasDiningRoom
                    update["propertyHasGarage"] = propertyHasGarage
                    update["propertyHasBalcony"] = propertyHasBalcony
                    update["propertyHasBabyBedroom"] = propertyHasBabyBedroom
                    update["propertyHasTVRoom"] = propertyHasTVRoom
                    update[propertyPlace!!.placeAddress] = propertyPlace?.placeAddress.toString()
                    update["propertyDescription"] = propertyDescription
                    update["propertyImagesUrl"] = propertyImagesUrl
                    applyAndUpdate(update)
                }
            } else {

                withContext(Dispatchers.Main) {
                    if (propertyImagesUrl.isEmpty()) {
                        progressDialog.dismiss()
                        Toast.makeText(context, "You've to set new Images", Toast.LENGTH_SHORT)
                            .show()
                        return@withContext
                    }
                    Toast.makeText(context, "Update without New Images", Toast.LENGTH_SHORT).show()
                    val update: MutableMap<String, Any> = HashMap()
                    update["propertyType"] = propertyType
                    update["propertySize"] = propertySize
                    update["propertyPrice"] = propertyPrice
                    update["propertyBedroomsNumber"] = propertyBedroomsNumber
                    update["propertyBathroomsNumber"] = propertyBathroomsNumber
                    update["propertyHasBathPlace"] = propertyHasBathPlace
                    update["propertyHasDiningRoom"] = propertyHasDiningRoom
                    update["propertyHasGarage"] = propertyHasGarage
                    update["propertyHasBalcony"] = propertyHasBalcony
                    update["propertyHasBabyBedroom"] = propertyHasBabyBedroom
                    update["propertyHasTVRoom"] = propertyHasTVRoom
                    update[propertyPlace!!.placeAddress] = propertyPlace?.placeAddress.toString()
                    update["propertyDescription"] = propertyDescription
                    applyAndUpdate(update)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "fatal error : " + e.message, Toast.LENGTH_LONG).show()
                Log.d("*****", "" + e.message)
            }
        }
    }

    private fun applyAndUpdate(update: MutableMap<String, Any>) =
        CoroutineScope(Dispatchers.IO).launch {

            try {
                post_apartment_ref.document(propertyId).update(update).await()
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
                    val imageUri: Uri = clipdata.getItemAt(currentImageSelect).uri
                    val imageName = "image_$currentImageSelect"
                    imageList.add(imageUri)
                    imageNameList.add(imageName)
                    currentImageSelect += 1
                }
                val adapterLoadedImages = Recycler_Adapter_Loaded_Image_Uri(imageList)
                binding.imagesAddedListRecyclerview.adapter = adapterLoadedImages
                binding.selectedImagesTv.visibility = View.VISIBLE
                binding.addOrChangeTv.text = "Change Image(s)"

                Toast.makeText(context, "You have selected $count image(s)", Toast.LENGTH_LONG)
                    .show()
            }
        }

        if ((requestCode == PLACE_PICKER_REQUEST) && (resultCode == Activity.RESULT_OK)) {
            val place: Place? = PingPlacePicker.getPlace(data!!)
            if (place != null) {
                propertyPlace = PropertyPlace(
                    place.id!!.toString(),
                    place.name!!.toString(),
                    place.address!!.toString(),
                    place.latLng!!.latitude,
                    place.latLng!!.longitude
                )
            }
            binding.locationSelectedTv.text = "You selected: ${propertyPlace?.placeName}"
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