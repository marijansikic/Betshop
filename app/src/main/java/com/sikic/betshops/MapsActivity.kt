package com.sikic.betshops

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.clustering.ClusterManager
import com.readystatesoftware.chuck.ChuckInterceptor
import com.sikic.betshops.databinding.ActivityMapsBinding
import com.sikic.betshops.extensions.isNotNull
import com.sikic.betshops.models.BetShopData
import com.sikic.betshops.models.CustomMarker
import com.sikic.betshops.utils.ClusterRenderer
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnCameraIdleListener, ClusterManager.OnClusterItemClickListener<CustomMarker> {

    private lateinit var mMap: GoogleMap
    private lateinit var clusterManager: ClusterManager<CustomMarker>

    private lateinit var binding: ActivityMapsBinding

    private val MUNICH_LAT = 48.137154
    private val MUNICH_LON = 11.576124
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    private var selectedMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.apply {
            setOnCameraIdleListener(this@MapsActivity)
            isMyLocationEnabled = true
        }
        mMap.uiSettings.isMyLocationButtonEnabled = false
        mMap.uiSettings.isCompassEnabled = false
        mMap.setMapStyle(
            MapStyleOptions.loadRawResourceStyle(this, R.raw.dark_mode_google_maps))

        setUpClusterer()

        //TODO permissions
        // Add a marker in Munich and move the map
        val default = LatLng(MUNICH_LAT, MUNICH_LON)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(default))
    }

    override fun onCameraIdle() {
        selectedMarker = null
        clusterManager.clearItems()

        //TODO DI and MVVM
        val client = OkHttpClient.Builder()
            .addInterceptor(ChuckInterceptor(this))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://interview.superology.dev")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        val service: Api = retrofit.create(Api::class.java)

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

        val call = service.getBetShopData(stringBuilder.toString())

        call.enqueue(object :
            Callback<BetShopData> {
            override fun onFailure(call: Call<BetShopData>, t: Throwable) {
                Snackbar.make(
                    binding.map,
                    getString(R.string.error_network_request),
                    Snackbar.LENGTH_LONG
                ).show()
            }

            override fun onResponse(call: Call<BetShopData>, response: Response<BetShopData>) {
                if (response.isSuccessful && response.body().isNotNull()) {
                    response.body()?.betShopList?.forEach { betshop ->
                        clusterManager.addItem(CustomMarker(betshop))
                    }
                    clusterManager.cluster()
                }
            }
        })
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

    @SuppressLint("PotentialBehaviorOverride")
    private fun setUpClusterer() {
        clusterManager = ClusterManager(this, mMap)
        clusterManager.setOnClusterItemClickListener(this)
        clusterManager.renderer = ClusterRenderer(
            this,
            mMap,
            clusterManager
        )
        mMap.setOnMarkerClickListener(clusterManager.markerManager)
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
}