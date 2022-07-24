package com.rmd.realstate.ui.post

import android.annotation.SuppressLint
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
import com.rmd.realstate.model.Property
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
    private lateinit var binding: FragmentPostBinding
    private lateinit var applied_published: Property
    private lateinit var auth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private val PICK_IMAGE_REQUEST = 1234
    private val property_ref = FirebaseFirestore.getInstance()
        .collection("property")
    private val like_apartment_ref = FirebaseFirestore.getInstance()
        .collection("favorite_clicked")
    private var image_list = ArrayList<Uri?>()
    private var image_name_list = ArrayList<String>()
    private var image_url = ArrayList<String>()
    private var number_bedrooms: Int = 1
    private var number_bathrooms: Int = 1
    private var property_size: Int = 0
    private var property_price: Int = 0
    private var property_type: String = "apartment"
    private lateinit var property_place: Place
    private var property_description: String = ""
    private var property_id: String = ""
    private var property_user_id: String = ""
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
        auth = FirebaseAuth.getInstance()
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
                    property_type = "apartment"
                }// Toast.makeText(context, "appa", Toast.LENGTH_SHORT).show()
                R.id.home_rb -> {
                    property_type = "home"
                }
            }
        }
        binding.bedroomRg.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.studio_rb -> {
                    number_bedrooms = 0
                }// Toast.makeText(context, "Studio", Toast.LENGTH_SHORT).show()
                R.id.one_bed_rb -> {
                    number_bedrooms = 1
                }//  Toast.makeText(context, "1", Toast.LENGTH_SHORT).show()
                R.id.two_bed_rb -> {
                    number_bedrooms = 2
                }//  Toast.makeText(context, "2", Toast.LENGTH_SHORT).show()
                R.id.three_bed_rb -> {
                    number_bedrooms = 3
                }//  Toast.makeText(context, "3", Toast.LENGTH_SHORT).show()
                R.id.four_bed_rb -> {
                    number_bedrooms = 4
                }//  Toast.makeText(context, "4", Toast.LENGTH_SHORT).show()
            }
        }
        binding.bathroomRg.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.one_bath_rb -> {
                    number_bathrooms = 1
                }//  Toast.makeText(context, "1", Toast.LENGTH_SHORT).show()
                R.id.two_bath_rb -> {
                    number_bathrooms = 2
                }//  Toast.makeText(context, "2", Toast.LENGTH_SHORT).show()
                R.id.three_bath_rb -> {
                    number_bathrooms = 3
                }//  Toast.makeText(context, "3", Toast.LENGTH_SHORT).show()
                R.id.four_bath_rb -> {
                    number_bathrooms = 4
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
            binding.deleteLoadedImagesBtn.visibility = View.GONE
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
            if (image_list.size < 1 || image_name_list.size < 1) {
                Toast.makeText(context, "You have to add at least 2 image", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            property_description = binding.addDescriptionEdt.text.toString()

            property_id = System.currentTimeMillis().toString() + "__" + property_user_id

            progressDialog.setMessage("uploading...")
            progressDialog.show()

            uploadImages()
        }
    }

    private fun uploadImages() = CoroutineScope(Dispatchers.IO).launch {
        val user = auth.currentUser
        image_url.clear()
        try {


            for (i in 0 until image_list.size) {
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

                //after uploading images(s) we upload post
                applied_published = Property(
                    property_type,
                    property_size,
                    property_price,
                    number_bedrooms,
                    number_bathrooms,
                    check_bath,
                    check_dinning,
                    check_garage,
                    check_balcony,
                    check_baby,
                    check_tv,
                    property_place,
                    property_description,
                    property_id,
                    property_user_id,
                    image_url
                )

                apply_and_publish(applied_published)
            }


        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "fatal error : " + e.message, Toast.LENGTH_LONG).show()
                Log.d("*****", "" + e.message)
            }

        }
    }

    private fun apply_and_publish(post: Property) =
        CoroutineScope(Dispatchers.IO).launch {
            val user = auth.currentUser
            val hashMap: HashMap<Any, String> = HashMap()
            hashMap["post_id"] = property_id

            try {
                property_ref.document(post.property_id).set(post).await()
                like_apartment_ref.document(post.property_id).set(hashMap).await()

                //vu qu'on ne peu acceder au UI dans un coroutine on use withContext
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
                //vu qu'on ne peu acceder au UI dans un coroutine on use withContext
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                }
            }
        }

    private fun checkUserConnection() {
        val user = auth.currentUser
        if (user == null) {
            val intent = Intent(context, Activity_Login_or_Register::class.java)
            startActivity(intent)
            activity?.finish()
        } else {
            property_user_id = user.uid
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
                image_list.clear()
                image_name_list.clear()

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
                binding.deleteLoadedImagesBtn.visibility = View.VISIBLE
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