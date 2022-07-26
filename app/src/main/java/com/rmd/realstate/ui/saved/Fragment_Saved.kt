package com.rmd.realstate.ui.saved

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rmd.realstate.R
import com.rmd.realstate.activity.Activity_Login_or_Register
import com.rmd.realstate.databinding.FragmentSavedBinding
import com.rmd.realstate.model.Property
import com.rmd.realstate.ui.home.recycler_adapter.Recycler_Adapter_Property
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class Fragment_Saved : Fragment() {

    //declarations
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: FragmentSavedBinding
    private lateinit var mLayoutManager: LinearLayoutManager

    private var propertyList = ArrayList<Property>()

    private val propertyRef = FirebaseFirestore.getInstance()
        .collection("property")
    private val favoriteClickedRef = FirebaseFirestore.getInstance()
        .collection("favorite_clicked")


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        firebaseAuth = FirebaseAuth.getInstance()
        return inflater.inflate(R.layout.fragment_saved, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //1st of all check if user is connected
        checkUserConnection()

        //affectation
        binding = FragmentSavedBinding.bind(view)

        //LayoutManager for recyclerview
        mLayoutManager = LinearLayoutManager(context)
        mLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        binding.likedApartmentsListRecyclerview.layoutManager = mLayoutManager

        //functions
        getAllLikedPost()
    }

    /*checkUserConnection*/
    private fun checkUserConnection() {
        val user = firebaseAuth.currentUser
        if (user == null) {
            val intent = Intent(context, Activity_Login_or_Register::class.java)
            startActivity(intent)
            activity?.finish()
        }
    }

    /*Get All Post Liked By The Current User*/
    private fun getAllLikedPost() = CoroutineScope(Dispatchers.IO).launch {

        propertyList.clear()
        val user = firebaseAuth.currentUser

        try {
            user?.let {

                val myQuerySnapshot = favoriteClickedRef.get().await()

                myQuerySnapshot.documents.mapNotNull { documentSnapshot ->

                    val map: MutableMap<String, Any>? = documentSnapshot.data

                    map?.let {

                        if (map.containsKey(user.uid)) {

                            val propertyId = map.getValue("post_id").toString()
                            val myDocumentSnapshot =
                                propertyRef.document(propertyId).get().await()
                            val apartment = myDocumentSnapshot.toObject(Property::class.java)

                            propertyList.add(apartment!!)

                            //As we can't directly access to UI within a coroutine, we use withContext
                            withContext(Dispatchers.Main) {
                                val adapter_apartment = Recycler_Adapter_Property(
                                    this@Fragment_Saved,
                                    requireActivity(),
                                    R.id.action_navigation_saved_to_navigation_view_apart,
                                    propertyList
                                )
                                binding.likedApartmentsListRecyclerview.adapter = adapter_apartment
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {

            //As we can't directly access to UI within a coroutine, we use withContext
            withContext(Dispatchers.Main) {
                //Log.d
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

}