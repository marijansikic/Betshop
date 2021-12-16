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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
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

    private lateinit var map: GoogleMap
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
        map = googleMap
        map.apply {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(europeCoordinates, 5.9f))
            setOnCameraIdleListener(this@MapsActivity)
            if (isLocationGranted) {
                isMyLocationEnabled = true
            }
        }
        setUpClusterer()
    }

    override fun onCameraIdle() {
        selectedMarker = null
        clusterManager.clearItems()
        viewModel.getBetShopsData(map.projection.visibleRegion.latLngBounds)
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
                map.isMyLocationEnabled = true
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(europeCoordinates, 5.9f))
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
                is MapStateEvent.Loading -> {
                    binding.progressBar.show()
                }
                is MapStateEvent.Error -> {
                    binding.progressBar.hide()
                    Snackbar.make(
                        binding.map,
                        getString(R.string.error_network_request),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                is MapStateEvent.GetBetShopsEvent -> {
                    binding.progressBar.hide()
                    event.betShopData.betShopList.forEach { clusterManager.addItem(CustomMarker(it)) }
                    clusterManager.cluster()
                }
            }
        }
    }

    private fun setUpClusterer() {
        clusterManager = ClusterManager(this, map)
        clusterManager.setOnClusterItemClickListener(this)
        clusterManager.renderer = ClusterRenderer(
            this,
            map,
            clusterManager
        )
    }

    private fun initBottomSheetDialog(item: CustomMarker) {
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(R.layout.dialog_bottom_sheet)
        bottomSheetDialog.findViewById<TextView>(R.id.txtLocation)?.text =
            viewModel.mapResponseToText(item.betShop)

        val currentTime = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        bottomSheetDialog.findViewById<TextView>(R.id.txtStatus)?.text =
            convertTextFormat(currentTime)

        bottomSheetDialog.findViewById<TextView>(R.id.txtRoute)?.setOnClickListener {
            handleRouteClick(item)
        }

        bottomSheetDialog.findViewById<ImageView>(R.id.btnClose)
            ?.setOnClickListener { bottomSheetDialog.dismiss() }
        bottomSheetDialog.setOnDismissListener {
            selectedMarker?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_normal))
            selectedMarker = null
        }
        bottomSheetDialog.window!!.setDimAmount(0f)
        bottomSheetDialog.show()
    }


    private fun convertTextFormat(currentTime: Int): SpannableString {
        val spannableString: SpannableString?
        if (currentTime in 8..16) {
            spannableString = SpannableString(getString(R.string.open_until))
            spannableString.setSpan(
                ForegroundColorSpan(
                    getColor(
                        R.color.primary
                    )
                ), 0, getString(R.string.open_until).length, 0
            )
        } else {
            spannableString = SpannableString(getString(R.string.closed_until))
            spannableString.setSpan(
                ForegroundColorSpan(
                    getColor(
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
        val rationaleDialog = MaterialAlertDialogBuilder(this, R.style.MyDialogTheme)
            .setTitle(getString(R.string.dialog_info))
            .setMessage(getString(R.string.dialog_message))
            .setNegativeButton(getString(R.string.dialog_negative)) { dialog, _ ->
                dialog.dismiss()
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(munichCoordinates, 16f))
            }
            .setPositiveButton(getString(R.string.dialog_positive)) { _, _ ->
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    locationPermissionCode
                )
            }
            .setCancelable(false)
            .show()
        rationaleDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.red))
        rationaleDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.primary))
    }
}