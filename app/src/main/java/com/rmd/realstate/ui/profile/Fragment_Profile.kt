package com.rmd.realstate.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.rmd.realstate.R
import com.rmd.realstate.activity.Activity_Login_or_Register
import com.rmd.realstate.databinding.FragmentProfileBinding
import com.rmd.realstate.model.User
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class Fragment_Profile : Fragment() {

    //variable declaration
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: FragmentProfileBinding

    private val profileRef = FirebaseFirestore.getInstance().collection("profile")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        firebaseAuth = FirebaseAuth.getInstance()
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkUserConnection()
        binding = FragmentProfileBinding.bind(view)

        binding.updateProfileBtn.setOnClickListener {
            NavHostFragment.findNavController(this)
                .navigate(R.id.action_navigation_profile_to_navigation_update_profile)
        }
    }

    private fun checkUserConnection() {
        val user = firebaseAuth.currentUser
        if (user == null) {
            val intent = Intent(context, Activity_Login_or_Register::class.java)
            startActivity(intent)
            activity?.finish()
        } else {
            getUserInfos()
        }
    }

    private fun getUserInfos() = CoroutineScope(Dispatchers.IO).launch {
        val user = firebaseAuth.currentUser
        try {
            user?.let {
                val myDocumentSnapshot = profileRef.document(user.uid).get().await()
                val myUser = myDocumentSnapshot.toObject<User>()

                /*val stringBuilder = StringBuilder()
                stringBuilder.append("$myUser")
                Log.d("+++--", "$stringBuilder")*/

                //As we can't directly access to UI within a coroutine, we use withContext
                withContext(Dispatchers.Main) {
                    myUser?.let {
                        binding.mailTv.text = myUser.userMail
                        binding.usernameTv.text = myUser.userName
                        binding.phoneNumberTv.text = myUser.userPhoneNumber
                        Picasso.get().load(myUser.userImageUrl).into(binding.profileImgv)

                        if (myUser.userMail == "null") binding.mailTv.isVisible = false
                        if (myUser.userName == "null") binding.usernameTv.isVisible = false
                        if (myUser.userPhoneNumber == "null") binding.phoneNumberTv.isVisible = false
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
}