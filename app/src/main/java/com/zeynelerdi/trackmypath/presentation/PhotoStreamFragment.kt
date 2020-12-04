package com.zeynelerdi.trackmypath.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.BroadcastReceiver
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.provider.Settings
import androidx.lifecycle.Observer
import androidx.fragment.app.Fragment
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager

import java.util.ArrayList

import com.google.android.material.snackbar.Snackbar

import org.koin.androidx.viewmodel.ext.android.viewModel

import com.zeynelerdi.trackmypath.R
import com.zeynelerdi.trackmypath.BuildConfig
import com.zeynelerdi.trackmypath.databinding.FragmentPhotoStreamBinding
import com.zeynelerdi.trackmypath.presentation.model.PhotoViewItem
import com.zeynelerdi.trackmypath.presentation.service.LocationService

import timber.log.Timber

class PhotoStreamFragment : Fragment() {

    companion object {
        fun newInstance() = PhotoStreamFragment()
    }

    private val viewModel: PhotoStreamViewModel by viewModel()

    // Used in checking for runtime permissions.
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 34
    // The BroadcastReceiver used to listen from broadcasts from the service.
    private lateinit var locationReceiver: LocationReceiver
    // A reference to the service used to get location updates.
    private lateinit var locationService: LocationService
    // used to store button state
    private lateinit var sharedPref: SharedPreferences
    // recycler view and adapter for retrieved photos
    private lateinit var photoAdapter: PhotoAdapter

    private lateinit var binding: FragmentPhotoStreamBinding

    // Monitors the state of the connection to the service.
    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Timber.d("ServiceConnection: onServiceConnected")
            val binder = service as LocationService.LocalBinder
            locationService = binder.service
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Timber.d("ServiceConnection: onServiceDisconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        locationReceiver = LocationReceiver()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPhotoStreamBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val photoList = ArrayList<PhotoViewItem>()
        photoAdapter = PhotoAdapter(photoList)
        binding.imageRecyclerView.adapter = photoAdapter
        binding.imageRecyclerView.isNestedScrollingEnabled = false

        viewModel.photosByLocation.observe(viewLifecycleOwner, Observer { photos ->
            photoAdapter.populate(photos)
            binding.imageRecyclerView.smoothScrollToPosition(0)
        })
    }

    override fun onStart() {
        super.onStart()

        photoAdapter.resetPhotoList()
        viewModel.retrievePhotos()

        binding.buttonStart.text = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(getString(R.string.service_state), "Start")
        binding.buttonStart.setOnClickListener {
            if (binding.buttonStart.text == getString(R.string.button_text_stop)) {
                locationService?.removeLocationUpdates()
                binding.buttonStart.text = getString(R.string.button_text_start)
            } else {
                if (!checkPermissions()) {
                    requestPermissions()
                } else {
                    locationService?.requestLocationUpdates()
                }
                photoAdapter.resetPhotoList()
                binding.buttonStart.text = getString(R.string.button_text_stop)
            }
        }

        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        requireActivity().bindService(
            Intent(context, LocationService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            locationReceiver,
            IntentFilter(LocationService.ACTION_BROADCAST)
        )
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(locationReceiver)
        super.onPause()
    }

    override fun onStop() {
        // Unbind from the service. This signals to the service that this activity is no longer in the foreground,
        // and the service can respond by promoting itself to a foreground service.
        requireActivity().unbindService(serviceConnection)
        super.onStop()
    }

    /**
     * Returns the current state of the permissions needed.
     */
    private fun checkPermissions(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            requireActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Snackbar.make(
                binding.fragmentMain,
                getString(R.string.location_permission_text),
                Snackbar.LENGTH_INDEFINITE
            ).setAction(getString(R.string.location_permission_action_ok_text)) {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSIONS_REQUEST_CODE
                )
            }.show()
        } else {
            // Request permission. It's possible this can be auto answered if device policy sets the permission
            // in a given state or the user denied the permission previously and checked "Never ask again".
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            when {
                grantResults.isEmpty() ->
                    // If user interaction was interrupted, the permission request is cancelled and you receive empty arrays.
                    Timber.i("=======> User interaction was cancelled.")
                grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                    locationService?.requestLocationUpdates()
                }
                else -> // Permission denied.
                    Snackbar.make(
                        binding.fragmentMain,
                        getString(R.string.location_permission_denied_text),
                        Snackbar.LENGTH_INDEFINITE
                    )
                        .setAction(getString(R.string.location_permission_action_settings_text)) {
                            // Build intent that displays the App settings screen.
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }.show()
            }
        }
    }

    /**
     * Receiver for broadcasts sent by [LocationService].
     */
    private inner class LocationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val photo = intent.getParcelableExtra<PhotoViewItem>(LocationService.EXTRA_PHOTO)
            if (photo != null) {
                photoAdapter.addPhoto(photo)
                binding.imageRecyclerView.smoothScrollToPosition(0)
            }
        }
    }
}
