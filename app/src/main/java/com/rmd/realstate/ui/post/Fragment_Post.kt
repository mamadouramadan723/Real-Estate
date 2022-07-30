package com.rmd.realstate.ui.post

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
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
import com.rmd.realstate.model.LatLong
import com.rmd.realstate.model.Property
import com.rmd.realstate.model.PropertyPlace
import com.rmd.realstate.ui.post.recycler_adapter.Recycler_Adapter_Loaded_Image_Uri
import com.rtchagas.pingplacepicker.PingPlacePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@SuppressLint("SetTextI18n")
class Fragment_Post : Fragment() {

    //variable declaration
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var propertyToPublish: Property
    private lateinit var binding: FragmentPostBinding
    private lateinit var progressDialog: ProgressDialog

    private var imageList = ArrayList<Uri?>()
    private var imageNameList = ArrayList<String>()
    private var propertyId: String = ""
    private var propertyType: String = "apartment"
    private var propertyDescription: String = ""
    private var propertyOwnerUserId: String = ""
    private var propertyOwnerPhoneNumber: String = ""
    private var propertyPlace: PropertyPlace? = null
    private var propertySize: Int = 0
    private var propertyPrice: Int = 0
    private var propertyScore: Int = 0
    private var propertyVotersNumber: Int = 0
    private var propertyBedroomsNumber: Int = 0
    private var propertyBathroomsNumber: Int = 0
    private var propertyHasGarage: Boolean = false
    private var propertyHasTVRoom: Boolean = false
    private var propertyHasBalcony: Boolean = false
    private var propertyHasBathPlace: Boolean = false
    private var propertyHasDiningRoom: Boolean = false
    private var propertyHasBedroomBaby: Boolean = false
    private var propertyImagesUrl: ArrayList<String> = arrayListOf()

    private val PICK_IMAGE_REQUEST = 1234
    private val propertyRef = FirebaseFirestore.getInstance()
        .collection("property")
    private val likedPropertyRef = FirebaseFirestore.getInstance()
        .collection("favorite_clicked")


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        firebaseAuth = FirebaseAuth.getInstance()
        return inflater.inflate(R.layout.fragment_post, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkUserConnection()

        binding = FragmentPostBinding.bind(view)
        progressDialog = ProgressDialog(requireContext())

        binding.selectedImagesTv.visibility = View.GONE

        //setOnCheckedChangeListener
        binding.propertyTypeRg.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.apartment_rb -> {
                    propertyType = "apartment"}
                R.id.home_rb -> {
                    propertyType = "home"
                }
            }
        }
        binding.bedroomRg.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.studio_rb -> {
                    propertyBedroomsNumber = 0
                }// Toast.makeText(context, "Studio", Toast.LENGTH_SHORT).show()
                R.id.one_bed_rb -> {
                    propertyBedroomsNumber = 1
                }//  Toast.makeText(context, "1", Toast.LENGTH_SHORT).show()
                R.id.two_bed_rb -> {
                    propertyBedroomsNumber = 2
                }//  Toast.makeText(context, "2", Toast.LENGTH_SHORT).show()
                R.id.three_bed_rb -> {
                    propertyBedroomsNumber = 3
                }//  Toast.makeText(context, "3", Toast.LENGTH_SHORT).show()
                R.id.four_bed_rb -> {
                    propertyBedroomsNumber = 4
                }//  Toast.makeText(context, "4", Toast.LENGTH_SHORT).show()
            }
        }
        binding.bathroomRg.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.one_bath_rb -> {
                    propertyBathroomsNumber = 1
                }//  Toast.makeText(context, "1", Toast.LENGTH_SHORT).show()
                R.id.two_bath_rb -> {
                    propertyBathroomsNumber = 2
                }//  Toast.makeText(context, "2", Toast.LENGTH_SHORT).show()
                R.id.three_bath_rb -> {
                    propertyBathroomsNumber = 3
                }//  Toast.makeText(context, "3", Toast.LENGTH_SHORT).show()
                R.id.four_bath_rb -> {
                    propertyBathroomsNumber = 4
                }//  Toast.makeText(context, "4", Toast.LENGTH_SHORT).show()
            }
        }

        //adapter
        //val adapter_loaded_images = Recycler_Adapter_Loaded_Image_Uri(image_list)
        val mLayoutManager = LinearLayoutManager(context)
        mLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        binding.imagesAddedListRecyclerview.layoutManager = mLayoutManager
        //binding.imagesAddedListRecyclerview.adapter = adapter_loaded_images

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
            propertyHasBedroomBaby = !propertyHasBedroomBaby
            binding.bedroomBabyRb.isChecked = propertyHasBedroomBaby
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
            binding.deleteLoadedImagesBtn.visibility = View.GONE
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
            if (imageList.size < 1 || imageNameList.size < 1) {
                Toast.makeText(context, "You have to add at least 2 image", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            propertyDescription = binding.addDescriptionEdt.text.toString()
            propertyId = "${System.currentTimeMillis()}__$propertyOwnerUserId"

            progressDialog.setMessage("uploading...")
            progressDialog.show()

            uploadImages()
        }
    }

     private fun uploadImages() = CoroutineScope(Dispatchers.IO).launch {
        val user = firebaseAuth.currentUser
        propertyImagesUrl.clear()
        try {
            for (i in 0 until imageList.size) {
                val imageUri: Uri? = imageList[i]

                val storageReference = FirebaseStorage.getInstance()
                    .getReference("Post Images/" + user?.uid + "/images for " + propertyId)
                    .child(imageNameList[i])

                val uploadTask = imageUri?.let { storageReference.putFile(it).await() }

                val my_url = uploadTask?.storage?.downloadUrl?.await()

                propertyImagesUrl.add(my_url.toString())

            }
            //without this, we will get error for the progressDialog:
            //Can't create handler inside thread that has not called Looper.prepare()
            withContext(Dispatchers.Main) {

                //after uploading images(s) we upload post
                propertyToPublish = Property(
                    propertyId,
                    propertyType,
                    propertyDescription,
                    propertyOwnerUserId,
                    propertyOwnerPhoneNumber,
                    propertyPlace,
                    propertySize,
                    propertyPrice,
                    propertyScore,
                    propertyVotersNumber,
                    propertyBedroomsNumber,
                    propertyBathroomsNumber,
                    propertyHasGarage,
                    propertyHasTVRoom,
                    propertyHasBalcony,
                    propertyHasBathPlace,
                    propertyHasDiningRoom,
                    propertyHasBedroomBaby,
                    propertyImagesUrl
                )
                apply_and_publish(propertyToPublish)
            }


        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "fatal error : " + e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun apply_and_publish(post: Property) =
        CoroutineScope(Dispatchers.IO).launch {

            val hashMap: HashMap<Any, String> = HashMap()
            hashMap["propertyId"] = propertyId

            try {
                propertyRef.document(post.propertyId).set(post).await()
                likedPropertyRef.document(post.propertyId).set(hashMap).await()

                //As we can't directly access to UI within a coroutine, we use withContext
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), "Post added successfully", Toast.LENGTH_LONG)
                        .show()
                    Toast.makeText(
                        requireContext(),
                        "Make Sure You've Added Your Phone Number To Be Reachable ",
                        Toast.LENGTH_LONG
                    )
                        .show()
                    NavHostFragment.findNavController(this@Fragment_Post)
                        .navigate(R.id.action_navigation_post_to_navigation_home)
                    progressDialog.dismiss()
                }
            } catch (e: Exception) {
                //As we can't directly access to UI within a coroutine, we use withContext
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                }
            }
        }

    private fun checkUserConnection() {
        val user = firebaseAuth.currentUser
        if (user == null) {
            val intent = Intent(context, Activity_Login_or_Register::class.java)
            startActivity(intent)
            activity?.finish()
        } else {
            propertyOwnerUserId = user.uid
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
            data?.clipData?.let { clipData ->
                val count = clipData.itemCount
                var currentImageSelect = 0
                imageList.clear()
                imageNameList.clear()

                while (currentImageSelect < count) {
                    val imageUri: Uri = clipData.getItemAt(currentImageSelect).uri
                    val imageName = "image_$currentImageSelect"
                    imageList.add(imageUri)
                    imageNameList.add(imageName)
                    currentImageSelect += 1
                }
                val adapterLoadedImages = Recycler_Adapter_Loaded_Image_Uri(imageList)
                binding.imagesAddedListRecyclerview.adapter = adapterLoadedImages
                binding.selectedImagesTv.visibility = View.VISIBLE
                binding.deleteLoadedImagesBtn.visibility = View.VISIBLE
                binding.addOrChangeTv.text = "Change Image(s)"

                Toast.makeText(context, "You have selected $count image(s)", Toast.LENGTH_LONG)
                    .show()
            }
        }
        if ((requestCode == PLACE_PICKER_REQUEST) && (resultCode == Activity.RESULT_OK)) {
            val place: Place? = PingPlacePicker.getPlace(data!!)

            if (place != null) {
                val latLng = LatLong(place.latLng!!.latitude, place.latLng!!.longitude )
                propertyPlace = PropertyPlace(
                    place.id!!.toString(),
                    place.name!!.toString(),
                    place.address!!.toString(),
                    latLng
                )
            }

            binding.locationSelectedTv.text = "You selected the location at : ${propertyPlace?.placeAddress}"
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