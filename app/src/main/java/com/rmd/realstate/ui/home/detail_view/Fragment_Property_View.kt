package com.rmd.realstate.ui.home.detail_view

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.rmd.realstate.R
import com.rmd.realstate.activity.Activity_Login_or_Register
import com.rmd.realstate.databinding.FragmentApartmentViewBinding
import com.rmd.realstate.databinding.RawApartmentBannerPriceScoreBinding
import com.rmd.realstate.model.Property
import com.rmd.realstate.ui.home.recycler_adapter.Recycler_Adapter_Slide_View_Pager
import com.rmd.realstate.view_model.SharedViewModel_Property
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class Fragment_Property_View : Fragment() {

    //declaration
    private lateinit var auth: FirebaseAuth
    private lateinit var my_view_pager: ViewPager2
    private lateinit var binding: FragmentApartmentViewBinding
    private lateinit var binding_raw: RawApartmentBannerPriceScoreBinding
    private lateinit var my_property_viewModel: SharedViewModel_Property
    private lateinit var view_pager_adapterSlideViewPager: Recycler_Adapter_Slide_View_Pager

    private var property_id: String = ""
    private var phone_number = ""
    private var favorite_clicked: Boolean = false
    private var image_list: ArrayList<String> = arrayListOf()

    private val property_ref = FirebaseFirestore.getInstance()
        .collection("property")
    private val favorite_clicked_ref = FirebaseFirestore.getInstance()
        .collection("favorite_clicked")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        auth = FirebaseAuth.getInstance()
        return inflater.inflate(R.layout.fragment_apartment_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkUserConnection()

        binding = FragmentApartmentViewBinding.bind(view)
        binding_raw = RawApartmentBannerPriceScoreBinding.bind(view)
        my_property_viewModel =
            ViewModelProvider(requireActivity())[SharedViewModel_Property::class.java]

        //viewpager
        my_view_pager = binding.apartImageViewPager
        view_pager_adapterSlideViewPager = Recycler_Adapter_Slide_View_Pager(image_list)
        my_view_pager.adapter = view_pager_adapterSlideViewPager
        my_view_pager.registerOnPageChangeCallback(onImageChangeCallback)


        //setOnClickListener
        binding.likeApartIbtn.setOnClickListener {
            favorite_clicked = true
            update_property_like()
        }
        binding.contactByCallBtn.setOnClickListener {

            val dialIntent = Intent(Intent.ACTION_DIAL)
            dialIntent.data = Uri.parse("tel:" + "0680523387")
            startActivity(dialIntent)
        }
        binding.contactByMessageBtn.setOnClickListener {
            val packageManager: PackageManager = requireActivity().packageManager
            val intent = Intent(Intent.ACTION_VIEW)
            val text = "Bonjour.\n" +
                    "S'il vous plaît, je suis interessé par votre appartment que vous avez publié sur" +
                    " ${resources.getString(R.string.app_name)}"
            //val url = "https://api.whatsapp.com/send?phone=+212680523387" //+ "&text=" + URLEncoder.encode("Hello","UTF-8")
            val url = "https://wa.me/send?phone=+212680523387" //+ "&text=" + URLEncoder.encode("Hello","UTF-8")
            intent.setPackage("com.whatsapp")
            intent.data = Uri.parse(url)
            if(intent.resolveActivity(packageManager) == null){
                Toast.makeText(context, "Make Sure you've installed Whatsapp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(intent)

        }


        binding.modifyApartBtn.setOnClickListener {
            //set the id of the clicked post
            my_property_viewModel.set_property_id(property_id)

            NavHostFragment.findNavController(this)
                .navigate(R.id.action_navigation_view_apart_to_navigation_post_modify)
        }

        //shared viewModels
        my_property_viewModel.my_property.observe(viewLifecycleOwner, {
            property_id = it
        })

        //functions
        get_apart_info()
        set_apart_like_btn()
    }

    private fun checkUserConnection() {
        val user = auth.currentUser
        if (user == null) {
            val intent = Intent(context, Activity_Login_or_Register::class.java)
            startActivity(intent)
            activity?.finish()
        }
    }

    private fun update_property_like() = CoroutineScope(Dispatchers.IO).launch {
        val user = auth.currentUser
        favorite_clicked = true

        try {
            user?.let {
                withContext(Dispatchers.Main) {
                    val documentSnapshot =
                        favorite_clicked_ref.document(property_id).get().await()

                    if (favorite_clicked) {

                        val map: MutableMap<String, Any>? = documentSnapshot.data

                        map?.let {
                            if (map.containsKey(user.uid)) {
                                //if I've already liked ==> then unlike ==>
                                // Remove my_uid  field from the document

                                val updates: MutableMap<String, Any> = HashMap()
                                updates[user.uid] = FieldValue.delete()

                                favorite_clicked_ref.document(property_id).update(updates)
                                    .await()

                                //unlike successful ==> so we can click the button again
                                favorite_clicked = false
                                set_apart_like_btn()
                            } else {
                                val add: MutableMap<String, Any> = HashMap()
                                add[user.uid] = "OK"
                                favorite_clicked_ref.document(property_id).update(add).await()

                                //like successful ==> so we can click the button again
                                favorite_clicked = false
                                set_apart_like_btn()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            //vu qu'on ne peu acceder au UI dans un coroutine on use withContext
            withContext(Dispatchers.Main) {
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun set_apart_like_btn() = CoroutineScope(Dispatchers.IO).launch {
        val user = auth.currentUser
        try {
            user?.let {
                withContext(Dispatchers.Main) {

                    val documentSnapshot =
                        favorite_clicked_ref.document(property_id).get().await()
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
            //vu qu'on ne peu acceder au UI dans un coroutine on use withContext
            withContext(Dispatchers.Main) {
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun get_apart_info() = CoroutineScope(Dispatchers.IO).launch {
        val user = auth.currentUser
        image_list.clear()
        try {
            withContext(Dispatchers.Main) {
                val documentSnapshot = property_ref.document(property_id).get().await()
                val apartment = documentSnapshot.toObject(Property::class.java)
                apartment?.let {
                    binding_raw.thisApartmentPriceTv.text =
                        apartment.property_price.toString() + " DH/Month"

                    image_list = apartment.image_url
                    view_pager_adapterSlideViewPager = Recycler_Adapter_Slide_View_Pager(image_list)
                    my_view_pager.adapter = view_pager_adapterSlideViewPager

                    //
                    if (apartment.property_user_id == user?.uid) {
                        binding.modifyApartBtn.isVisible = true
                        //binding.layoutContact.isVisible = false
                    }
                }
            }
        } catch (e: Exception) {
            //vu qu'on ne peu acceder au UI dans un coroutine on use withContext
            withContext(Dispatchers.Main) {
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private var onImageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            add_dots(position)
        }
    }

    private fun add_dots(position: Int) {
        binding.dotsImageLayout.removeAllViews()

        val dots: Array<TextView?> = arrayOfNulls(image_list.size)

        for (i in 0 until image_list.size) {

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
        my_view_pager.unregisterOnPageChangeCallback(onImageChangeCallback)
        super.onDestroy()
    }
}


