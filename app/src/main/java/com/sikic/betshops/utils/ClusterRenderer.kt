package com.sikic.betshops.utils


import android.content.Context
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.sikic.betshops.R
import com.sikic.betshops.models.CustomMarker


class ClusterRenderer(
    context: Context,
    map: GoogleMap,
    clusterManager: ClusterManager<CustomMarker>
) :
    DefaultClusterRenderer<CustomMarker>(context, map, clusterManager) {
    override fun onBeforeClusterItemRendered(item: CustomMarker, markerOptions: MarkerOptions) {
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_normal))
    }

    override fun shouldRenderAsCluster(cluster: Cluster<CustomMarker>): Boolean = cluster.size >= 5

    override fun getClusterText(bucket: Int): String = bucket.toString()

    override fun getBucket(cluster: Cluster<CustomMarker>): Int = cluster.size
}