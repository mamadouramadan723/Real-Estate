package com.rmd.realstate.ui.profile

import android.app.Activity.RESULT_OK
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import com.rmd.realstate.R
import com.rmd.realstate.activity.Activity_Login_or_Register
import com.rmd.realstate.databinding.FragmentProfileUpdateBinding
import com.rmd.realstate.model.User
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class Fragment_Profile_Update : Fragment() {


    //variable declaration
    private lateinit var binding: FragmentProfileUpdateBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    private val profile_ref = FirebaseFirestore.getInstance().collection("profile")
    private val PICK_IMAGE_REQUEST = 1234
    private var image_uri: Uri? = null
    private var image_url: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        auth = FirebaseAuth.getInstance()
        return inflater.inflate(R.layout.fragment_profile_update, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkUserConnection()

        binding = FragmentProfileUpdateBinding.bind(view)

        progressDialog = ProgressDialog(requireContext())

        binding.profileUpdateImg.setOnClickListener {
            handleImageClick()
        }
        binding.validateUpdateProfileBtn.setOnClickListener {
            progressDialog.setMessage("updating...")
            progressDialog.show()
            updateUserInfos()
        }
    }


    private fun checkUserConnection() {
        val user = auth.currentUser
        if(user == null){
            val intent = Intent(context, Activity_Login_or_Register::class.java)
            startActivity(intent)
            activity?.finish()
        }
        else{
            getUserInfos_and_fillChamp()
        }
    }

    private fun getUserInfos_and_fillChamp() = CoroutineScope(Dispatchers.IO).launch{
        val user = auth.currentUser
        try {
            user?.let {
                val myDocumentSnapshot = profile_ref.document(user.uid).get().await()
                val myUser = myDocumentSnapshot.toObject<User>()
                /*val stringBuilder = StringBuilder()
                stringBuilder.append("$myUser")
                Log.d("+++--", "$stringBuilder")*/

                //As we can't directly access to UI within a coroutine, we use withContext
                withContext(Dispatchers.Main){
                    myUser?.let {
                        binding.mailEdt.setText(myUser.mail)
                        binding.usernameEdt.setText(myUser.username)
                        binding.phoneNumberEdt.setText(myUser.phonenumber)
                        Picasso.get().load(myUser.image_url).into(binding.profileUpdateImg)

                        if(myUser.mail == "null") binding.mailEdt.setText("")
                        if(myUser.username == "null") binding.usernameEdt.setText("")
                        if(myUser.phonenumber == "null") binding.phoneNumberEdt.setText("")

                    }
                }
            }
        }
        catch (e: Exception){
            //As we can't directly access to UI within a coroutine, we use withContext
            withContext(Dispatchers.Main){
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun updateUserInfos() {
        uploadImage()
    }

    private fun uploadImage() {

        val user = auth.currentUser
        try {
            if(image_uri != null){
                val storageReference = FirebaseStorage.getInstance().getReference("Profile Image/"+user?.uid+"/profile for "+user?.uid)
                val uploadTask =  storageReference.putFile(image_uri!!)

                uploadTask.continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let {
                            throw it
                        }
                    }
                    storageReference.downloadUrl
                }.addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        image_url = task.result.toString()
                        getNewUserInfos_mapThem_then_Update()

                    } else {
                        Toast.makeText(context, "fail to retrieve uri from firestorage", Toast.LENGTH_LONG).show()
                    }
                }
            }
            else{

                getNewUserInfos_mapThem_then_Update()
            }

        }
        catch (e: Exception){
            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun getNewUserInfos_mapThem_then_Update() {

        //get new user infos

        val myUsername = binding.usernameEdt.text.toString()
        val myMail = binding.mailEdt.text.toString()
        val myPhoneNumber = binding.phoneNumberEdt.text.toString()

        //then map
        val map = mutableMapOf<String, Any>()

        if(myUsername.isNotEmpty())
            map["username"] = myUsername
        else{
            Toast.makeText(context, "Username must not be empty", Toast.LENGTH_LONG).show()
            progressDialog.dismiss()
            return
        }

        if(myMail.isNotEmpty())
            map["mail"] = myMail
        else{
            Toast.makeText(context, "mail must not be empty", Toast.LENGTH_LONG).show()
            progressDialog.dismiss()
            return
        }

        if(myPhoneNumber.isNotEmpty())
            map["phonenumber"] = myPhoneNumber
        else{
            Toast.makeText(context, "phone number must not be empty", Toast.LENGTH_LONG).show()
            progressDialog.dismiss()
            return
        }

        map["image_url"] = image_url

        //then update
        updateProfile(map)
    }

    private fun updateProfile(newUserMap: Map<String, Any>) = CoroutineScope(Dispatchers.IO).launch {
        val user = auth.currentUser

        try {
            user?.let {
                profile_ref.document(user.uid)
                    .set(newUserMap, SetOptions.merge())
                    .await()

                //As we can't directly access to UI within a coroutine, we use withContext
                withContext(Dispatchers.Main){
                    Toast.makeText(context, "Profile Updated successfully", Toast.LENGTH_LONG).show()
                    progressDialog.dismiss()
                    NavHostFragment.findNavController(this@Fragment_Profile_Update)
                        .navigate(R.id.action_navigation_update_profile_to_navigation_profile)
                }
            }
        }
        catch (e: Exception){
            //As we can't directly access to UI within a coroutine, we use withContext
            withContext(Dispatchers.Main){
                progressDialog.dismiss()
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleImageClick() {
        Intent(Intent.ACTION_GET_CONTENT).also { intent ->
            intent.type = "image/*"
            //intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK){
            data?.data?.let { uri ->
                image_uri = uri
                Picasso.get().load(image_uri).into(binding.profileUpdateImg)
            }
        }
    }
}