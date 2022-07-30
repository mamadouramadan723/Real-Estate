package com.rmd.realstate.ui.home.filter

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.rmd.realstate.R
import com.rmd.realstate.model.Property
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class Fragment_Explorer : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var map: GoogleMap

    private val propertyList: ArrayList<Property> = arrayListOf()
    private val propertyRef = FirebaseFirestore.getInstance()
        .collection("property")
    private var locationUpdateState = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_explorer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)


        getAllAddress()
    }

    private fun getAllAddress() = CoroutineScope(Dispatchers.IO).launch {
        //propertyList.clear()

        try {
            val myQuerySnapshot = propertyRef.get().await()

            myQuerySnapshot.documents.mapNotNull { documentSnapshot ->
                val property = documentSnapshot.toObject(Property::class.java)
                propertyList.add(property!!)
                //property?.propertyPlace?.placeLng!!
            }
        } catch (e: Exception) {
            //As we can't directly access to UI within a coroutine, we use withContext
            withContext(Dispatchers.Main) {
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                Log.e(" +++++", "" + e.message)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {

        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        map.setOnMarkerClickListener(this)

        for (i in 0 until (propertyList.size)) {
            val latLng = LatLng(
                propertyList[i].propertyPlace?.placeLng?.placeLat!!,
                propertyList[i].propertyPlace?.placeLng?.placeLng!!
            )
            val markerOptions = MarkerOptions()
                .position(latLng)
                .title(propertyList[i].propertyPlace?.placeName)

            val stringBuilder = StringBuilder()
                stringBuilder.append("$markerOptions")
                Log.d("+++--${propertyList.size - 1}", propertyList[i].propertyPlace?.placeName!!)

            map.addMarker(markerOptions)
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12f))
        }
        setUpMap()
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        return false
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
        // 1
        map.isMyLocationEnabled = true
        //MAP_TYPE_NORMAL, MAP_TYPE_SATELLITE, MAP_TYPE_TERRAIN, MAP_TYPE_HYBRID
        map.mapType = GoogleMap.MAP_TYPE_HYBRID
    }
}