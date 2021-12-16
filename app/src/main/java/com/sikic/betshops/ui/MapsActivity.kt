package com.sikic.betshops.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.clustering.ClusterManager
import com.sikic.betshops.R
import com.sikic.betshops.databinding.ActivityMapsBinding
import com.sikic.betshops.extensions.isNotNull
import com.sikic.betshops.models.CustomMarker
import com.sikic.betshops.utils.ClusterRenderer
import dagger.hilt.android.AndroidEntryPoint
import java.util.*


@AndroidEntryPoint
@SuppressLint("MissingPermission")
class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnCameraIdleListener, ClusterManager.OnClusterItemClickListener<CustomMarker> {

    private val munichCoordinates = LatLng(48.137154, 11.576124)
    private val europeCoordinates = LatLng(53.0, 9.0)
    private val locationPermissionCode = 1

    private lateinit var mMap: GoogleMap
    private lateinit var clusterManager: ClusterManager<CustomMarker>

    private lateinit var binding: ActivityMapsBinding

    private var selectedMarker: Marker? = null
    private var isLocationGranted = false

    private lateinit var viewModel: MapsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[MapsViewModel::class.java]
        subscribeObservers()

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prepareMap()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.apply {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(europeCoordinates, 5.9f))
            setOnCameraIdleListener(this@MapsActivity)
            if (isLocationGranted) {
                isMyLocationEnabled = true
            }
            uiSettings.isMyLocationButtonEnabled = false
            uiSettings.isCompassEnabled = false
            setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this@MapsActivity, R.raw.dark_mode_google_maps)
            )
        }
        setUpClusterer()
    }

    override fun onCameraIdle() {
        selectedMarker = null
        clusterManager.clearItems()

        //top-right latitude (lat1),
        //top-right longitude (lon1),
        //bottom-left latitude (lat2),
        //bottom-left longitude (lon2)
        val visibleRegion = mMap.projection.visibleRegion.latLngBounds

        //TODO refactor
        val stringBuilder = StringBuilder(visibleRegion.northeast.latitude.toString())
            .append(",")
            .append(visibleRegion.northeast.longitude.toString())
            .append(",")
            .append(visibleRegion.southwest.latitude.toString())
            .append(",")
            .append(visibleRegion.southwest.longitude.toString())

        viewModel.getBetShopsData(stringBuilder.toString())
    }

    override fun onClusterItemClick(marker: CustomMarker): Boolean {
        if (selectedMarker.isNotNull()) {
            selectedMarker?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_normal))
            selectedMarker = null
        }
        selectedMarker = (clusterManager.renderer as ClusterRenderer).getMarker(marker)
        (clusterManager.renderer as ClusterRenderer).getMarker(marker)
            .setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_active))
        initBottomSheetDialog(marker)

        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isLocationGranted = true
                mMap.isMyLocationEnabled = true
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(europeCoordinates, 5.9f))
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    showRationaleDialog()
                }
            }
        }
    }

    private fun subscribeObservers() {
        viewModel.dataState.observe(this) { event ->
            when (event) {
                is MapStateEvent.Loading -> Toast.makeText(
                    this,
                    "loading",
                    Toast.LENGTH_SHORT
                ).show()
                is MapStateEvent.Error -> Snackbar.make(
                    binding.map,
                    getString(R.string.error_network_request),
                    Snackbar.LENGTH_LONG
                ).show()
                is MapStateEvent.GetBetShopsEvent -> {
                    event.betShopData.betShopList.forEach { clusterManager.addItem(CustomMarker(it)) }
                    clusterManager.cluster()
                }
            }
        }
    }

    private fun setUpClusterer() {
        clusterManager = ClusterManager(this, mMap)
        clusterManager.setOnClusterItemClickListener(this)
        clusterManager.renderer = ClusterRenderer(
            this,
            mMap,
            clusterManager
        )
    }

    private fun initBottomSheetDialog(item: CustomMarker) {
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(R.layout.dialog_bottom_sheet)

        with(item.betShop) {
            bottomSheetDialog.findViewById<TextView>(R.id.txtLocation)?.text =
                StringBuilder(this.name.trimStart())
                    .append("\n")
                    .append(this.address)
                    .append("\n")
                    .append(this.city)
                    .append(" - ")
                    .append(this.county)

            val currentTime = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

            bottomSheetDialog.findViewById<TextView>(R.id.txtStatus)?.text =
                convertTextFormat(currentTime)

            bottomSheetDialog.findViewById<TextView>(R.id.txtRoute)?.setOnClickListener {
                handleRouteClick(item)
            }
        }

        bottomSheetDialog.findViewById<ImageView>(R.id.btnClose)
            ?.setOnClickListener { bottomSheetDialog.dismiss() }

        bottomSheetDialog.show()
    }

    private fun convertTextFormat(currentTime: Int): SpannableString {
        val spannableString: SpannableString?
        if (currentTime in 8..16) {
            spannableString = SpannableString(getString(R.string.open_until))
            spannableString.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(
                        this@MapsActivity,
                        R.color.primary
                    )
                ), 0, getString(R.string.open_until).length, 0
            )
        } else {
            spannableString = SpannableString(getString(R.string.closed_until))
            spannableString.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(
                        this@MapsActivity,
                        R.color.red
                    )
                ), 0, getString(R.string.closed_until).length, 0
            )
        }
        return spannableString
    }

    private fun handleRouteClick(item: CustomMarker) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("google.navigation:q=${item.betShop.location.latitude},${item.betShop.location.longitude}")
        ).setClassName(
            "com.google.android.apps.maps",
            "com.google.android.maps.MapsActivity"
        )

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Snackbar.make(
                binding.map,
                getString(R.string.error_no_implicit_intent_handler),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun prepareMap() {
        checkPermissions()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionCode
            )
        } else {
            isLocationGranted = true
        }
    }

    private fun showRationaleDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_info))
            .setMessage(getString(R.string.dialog_message))
            .setNegativeButton(getString(R.string.dialog_negative)) { dialog, _ ->
                dialog.dismiss()
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(munichCoordinates, 16f))
            }
            .setPositiveButton(getString(R.string.dialog_positive)) { _, _ ->
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    locationPermissionCode
                )
            }
            .setCancelable(false)
            .show()
    }
}