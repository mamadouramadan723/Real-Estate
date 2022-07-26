package com.rmd.realstate.ui.home.detail_view

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.rmd.realstate.R
import com.rmd.realstate.activity.Activity_Login_or_Register
import com.rmd.realstate.databinding.FragmentApartmentViewBinding
import com.rmd.realstate.databinding.RawApartmentBannerPriceScoreBinding
import com.rmd.realstate.model.Property
import com.rmd.realstate.model.User
import com.rmd.realstate.ui.home.recycler_adapter.Recycler_Adapter_Slide_View_Pager
import com.rmd.realstate.view_model.SharedViewModel_Property
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.URLEncoder


class Fragment_Property_View : Fragment() {

    //declaration
    private lateinit var viewPager: ViewPager2
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private lateinit var binding: FragmentApartmentViewBinding
    private lateinit var sharedViewModel: SharedViewModel_Property
    private lateinit var bindingRaw: RawApartmentBannerPriceScoreBinding
    private lateinit var recyclerAdapterSlideViewPager: Recycler_Adapter_Slide_View_Pager

    private var propertyId: String = ""
    private var ownerPhoneNumber: String = ""
    private var favoriteClicked: Boolean = false
    private var imageList: ArrayList<String> = arrayListOf()

    private val propertyRef = FirebaseFirestore.getInstance()
        .collection("property")
    private val favoriteClickedRef = FirebaseFirestore.getInstance()
        .collection("favorite_clicked")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        firebaseAuth = FirebaseAuth.getInstance()
        return inflater.inflate(R.layout.fragment_apartment_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkUserConnection()

        binding = FragmentApartmentViewBinding.bind(view)
        progressDialog = ProgressDialog(requireContext())
        bindingRaw = RawApartmentBannerPriceScoreBinding.bind(view)
        sharedViewModel =
            ViewModelProvider(requireActivity())[SharedViewModel_Property::class.java]

        //viewpager
        viewPager = binding.apartImageViewPager
        recyclerAdapterSlideViewPager = Recycler_Adapter_Slide_View_Pager(imageList)
        viewPager.adapter = recyclerAdapterSlideViewPager
        viewPager.registerOnPageChangeCallback(onImageChangeCallback)


        //setOnClickListener
        binding.likeApartIbtn.setOnClickListener {
            favoriteClicked = true
            updatePropertyLikes()
        }
        binding.contactByCallBtn.setOnClickListener {
            val dialIntent = Intent(Intent.ACTION_DIAL)
            dialIntent.data = Uri.parse("tel:$ownerPhoneNumber")
            startActivity(dialIntent)
        }
        binding.contactByMessageBtn.setOnClickListener {
            val packageManager: PackageManager = requireActivity().packageManager
            val intent = Intent(Intent.ACTION_VIEW)
            val text = "Bonjour.\n" +
                    "S'il vous plaît, je suis interessé par votre appartment que vous avez publié sur" +
                    " ${resources.getString(R.string.app_name)}"
            val url =
                "https://api.whatsapp.com/send?phone=+212$ownerPhoneNumber&text=" + URLEncoder.encode(
                    text, "UTF-8"
                )
            //val url = "https://wa.me/send?phone=+212680523387" //+ "&text=" + URLEncoder.encode("Hello","UTF-8")
            intent.setPackage("com.whatsapp")
            intent.data = Uri.parse(url)
            if (intent.resolveActivity(packageManager) == null) {
                Toast.makeText(context, "Make Sure you've installed Whatsapp", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            startActivity(intent)
        }

        bindingRaw.moreBtn.setOnClickListener {
            show_more_options()
        }

        //shared viewModels
        sharedViewModel.my_property.observe(viewLifecycleOwner) {
            propertyId = it
        }

        //functions
        getPropertyInfos()
        setApartLikeBtn()
    }

    private fun show_more_options() {
        val popupMenu = PopupMenu(requireContext(), bindingRaw.moreBtn, Gravity.END)

        popupMenu.menu.add(Menu.NONE, 0, 0, "Modify")
        popupMenu.menu.add(Menu.NONE, 1, 0, "Delete")
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> {
                    //set the id of the clicked post
                    sharedViewModel.set_property_id(propertyId)

                    NavHostFragment.findNavController(this)
                        .navigate(R.id.action_navigation_view_apart_to_navigation_post_modify)
                }
                1 -> {
                    //popup for confirmation
                    val alertDialog = AlertDialog.Builder(requireContext())
                    alertDialog.setTitle("Delete Your Publication")
                    alertDialog.setMessage("Are you sure ?")
                        .setIcon(R.drawable.ic_warning)
                    alertDialog.setPositiveButton("Yes") { _, _ ->
                        progressDialog.setMessage("Deleting...")
                        progressDialog.show()
                        deleteThisPublication()
                    }
                    alertDialog.setNeutralButton("Cancel") { _, _ ->
                    }
                    alertDialog.create().show()
                }
            }
            false
        }
        popupMenu.show()
    }

    private fun deleteThisPublication() = CoroutineScope(Dispatchers.IO).launch {
        try {
            propertyRef.document(propertyId).delete().await()
            favoriteClickedRef.document(propertyId).delete().await()

            withContext(Dispatchers.Main) {
                NavHostFragment.findNavController(this@Fragment_Property_View)
                    .navigate(R.id.action_navigation_view_apart_to_navigation_home)
                progressDialog.dismiss()
                Toast.makeText(context, "Post Deleted", Toast.LENGTH_SHORT).show()
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
        }
    }

    private fun updatePropertyLikes() = CoroutineScope(Dispatchers.IO).launch {
        val user = firebaseAuth.currentUser
        favoriteClicked = true

        try {
            user?.let {
                withContext(Dispatchers.Main) {
                    val documentSnapshot =
                        favoriteClickedRef.document(propertyId).get().await()

                    if (favoriteClicked) {

                        val map: MutableMap<String, Any>? = documentSnapshot.data

                        map?.let {
                            if (map.containsKey(user.uid)) {
                                //if I've already liked ==> then unlike ==>
                                // Remove my_uid  field from the document

                                val updates: MutableMap<String, Any> = HashMap()
                                updates[user.uid] = FieldValue.delete()

                                favoriteClickedRef.document(propertyId).update(updates)
                                    .await()

                                //unlike successful ==> so we can click the button again
                                favoriteClicked = false
                                setApartLikeBtn()
                            } else {
                                val add: MutableMap<String, Any> = HashMap()
                                add[user.uid] = "OK"
                                favoriteClickedRef.document(propertyId).update(add).await()

                                //like successful ==> so we can click the button again
                                favoriteClicked = false
                                setApartLikeBtn()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            //As we can't directly access to UI within a coroutine, we use withContext
            withContext(Dispatchers.Main) {
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setApartLikeBtn() = CoroutineScope(Dispatchers.IO).launch {
        val user = firebaseAuth.currentUser
        try {
            user?.let {
                withContext(Dispatchers.Main) {

                    val documentSnapshot =
                        favoriteClickedRef.document(propertyId).get().await()
                    val map: MutableMap<String, Any>? = documentSnapshot.data
                    map?.let {
                        if (map.containsKey(user.uid)) {
                            binding.likeApartIbtn.setImageResource(R.drawable.ic_liked)
                        } else {
                            binding.likeApartIbtn.setImageResource(R.drawable.ic_favorite_appart)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            //As we can't directly access to UI within a coroutine, we use withContext
            withContext(Dispatchers.Main) {
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getPropertyInfos() = CoroutineScope(Dispatchers.IO).launch {
        val user = firebaseAuth.currentUser
        imageList.clear()
        try {
            withContext(Dispatchers.Main) {
                val documentSnapshot = propertyRef.document(propertyId).get().await()
                val property = documentSnapshot.toObject(Property::class.java)
                property?.let {
                    bindingRaw.thisApartmentPriceTv.text =
                        "${property.propertyPrice} DH/Month"

                    imageList = property.propertyImagesUrl
                    recyclerAdapterSlideViewPager =
                        Recycler_Adapter_Slide_View_Pager(imageList)
                    viewPager.adapter = recyclerAdapterSlideViewPager

                    //
                    if (property.propertyOwnerUserId == user?.uid) {
                        bindingRaw.moreBtn.isVisible = true
                        //binding.layoutContact.isVisible = false
                    }
                    get_owner_phone_number(property.propertyOwnerUserId)
                }
            }
        } catch (e: Exception) {
            //As we can't directly access to UI within a coroutine, we use withContext
            withContext(Dispatchers.Main) {
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun get_owner_phone_number(propertyOwnerUserId: String) =
        CoroutineScope(Dispatchers.IO).launch {
            val profileRef = FirebaseFirestore.getInstance().collection("profile")
            val myDocumentSnapshot = profileRef.document(propertyOwnerUserId).get().await()
            val propertyOwner = myDocumentSnapshot.toObject<User>()

            propertyOwner?.let {
                ownerPhoneNumber = propertyOwner.userPhoneNumber
            }
        }

    private var onImageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            addDots(position)
        }
    }

    private fun addDots(position: Int) {
        binding.dotsImageLayout.removeAllViews()

        val dots: Array<TextView?> = arrayOfNulls(imageList.size)

        for (i in 0 until imageList.size) {

            dots[i] = TextView(requireContext())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                dots[i]?.text = Html.fromHtml("&#8226", Html.FROM_HTML_MODE_LEGACY)
            } else {
                dots[i]?.text = Html.fromHtml("")
            }

            dots[i]?.textSize = 38f
            dots[i]?.setTextColor(resources.getColor(R.color.white))
            binding.dotsImageLayout.addView(dots[i])

        }
        if (dots.isNotEmpty()) {
            dots[position]?.setTextColor(resources.getColor(R.color.teal_700))
        }
    }

    override fun onDestroy() {
        viewPager.unregisterOnPageChangeCallback(onImageChangeCallback)
        super.onDestroy()
    }
}