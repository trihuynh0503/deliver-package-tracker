package com.map524.a4_minh_tri

import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.map524.a4_minh_tri.databinding.ActivityPackageDetailsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PackageDetails : AppCompatActivity(), OnMapReadyCallback {
    lateinit var binding: ActivityPackageDetailsBinding
    lateinit var geocoder: Geocoder
    private lateinit var packageDao: PackageDao
    private lateinit var selectedPackage  : Package
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var gMap: GoogleMap
    private val APP_PERMISSION_LIST = arrayOf(
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPackageDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //set yp Room Database
        val db = PackageDatabase.getInstance(this)
        packageDao = db.packageDao()
        // set up Geocoder
        geocoder = Geocoder(applicationContext, Locale.getDefault())

        val deliveryStatus = arrayOf("In Transit", "Delivered")
        binding.spinnerStatus.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, deliveryStatus)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment  = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // load selected package
        val packageId = intent.getIntExtra("PACKAGE_ID", -1)
        if (packageId == -1) {
            finish()
            return
        }
        GlobalScope.launch(Dispatchers.IO) {
            selectedPackage = packageDao.getPackageById(packageId)
            withContext(Dispatchers.Main) {
                loadPackageIntoUI(selectedPackage)
            }
        }
        binding.edtDeliveryDate.setOnClickListener {

            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Delivery Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { timestamp ->
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                formatter.timeZone = TimeZone.getTimeZone("UTC")
                val date = formatter.format(Date(timestamp))
                binding.edtDeliveryDate.setText(date)
            }

            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }
        binding.btnCheckAddress.setOnClickListener {
            val address = getCoordinates()
            if(address != null) {
                val latLng = LatLng(address.latitude, address.longitude)

                // Update map marker
                gMap.clear()
                gMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title("Valid Address")
                )
                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            }
        }
        binding.btnUpdatePackage.setOnClickListener {
                val name = binding.edtCustomerName.text.toString().trim()
                val address = binding.edtAddress.text.toString().trim()
                val deliveryDate = binding.edtDeliveryDate.text.toString().trim()
                val statusString = binding.spinnerStatus.selectedItem.toString()

                if (name.isEmpty()) return@setOnClickListener
                if (address.isEmpty()) return@setOnClickListener
                if (deliveryDate.isEmpty()) return@setOnClickListener
                lateinit var deliveryStatus: DeliveryStatus
                when (statusString){
                    "In Transit" -> deliveryStatus = DeliveryStatus.IN_TRANSIT
                    "Delivered" -> deliveryStatus = DeliveryStatus.DELIVERED
                }

                val addressObj = getCoordinates()
                if (addressObj == null) {
                    return@setOnClickListener
                }
                val latitude = addressObj.latitude
                val longitude = addressObj.longitude

                GlobalScope.launch(Dispatchers.IO) {
                    packageDao.update(Package(
                        id = packageId,
                        customerName = name,
                        address = address,
                        latitude = latitude,
                        longitude = longitude,
                        deliveryStatus = deliveryStatus,
                        deliveryDate = deliveryDate))
                }
                finish()
        }
        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun loadPackageIntoUI(pkg: Package) {
        binding.edtCustomerName.setText(pkg.customerName)
        binding.edtDeliveryDate.setText(pkg.deliveryDate)
        binding.edtAddress.setText(pkg.address)
        binding.edtLatitude.setText(pkg.latitude.toString())
        binding.edtLongitude.setText(pkg.longitude.toString())

        // Set Spinner value
        val index = when (pkg.deliveryStatus) {
            DeliveryStatus.IN_TRANSIT -> 0
            DeliveryStatus.DELIVERED -> 1
        }
        binding.spinnerStatus.setSelection(index)

        // Update map
        val location = LatLng(pkg.latitude, pkg.longitude)

        gMap.clear()
        gMap.addMarker(MarkerOptions().position(location).title(pkg.customerName))
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
    }
    private val multiplePermissionResultLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            resultList ->

        var allPermissionGranted = true

        for(item in resultList.entries) {
            if(item.key in APP_PERMISSION_LIST && !item.value)
                allPermissionGranted = false
        }

        if(allPermissionGranted) {
            Snackbar.make(binding.root, "All Permissions Granted!", Snackbar.LENGTH_SHORT).show()

            // To Do: get device location
            getDeviceLocation()
        } else {
            Snackbar.make(binding.root, "Some Permissions Not Granted!", Snackbar.LENGTH_SHORT).show()

            // To Do: tell user why we need these permissions
            binding.btnUpdatePackage.isEnabled = false

        }
    }

    private fun getCoordinates() : Address? {
        val addressFromUI = binding.edtAddress.text.toString()

        if (addressFromUI.isEmpty()) {
            binding.txtAddressWarning.text = "Address is required"
            binding.txtAddressWarning.visibility = View.VISIBLE
            return null
        }
        try {
            val searchResults:MutableList<Address>? = geocoder.getFromLocationName(addressFromUI,1)

            if (searchResults == null) {
                binding.txtAddressWarning.text = "Could not connect to Geocoder service"
                binding.txtAddressWarning.visibility = View.VISIBLE
                return null
            }
            if (searchResults!!.isEmpty()) {
                binding.txtAddressWarning.text = "No coordinate found"
                binding.txtAddressWarning.visibility = View.VISIBLE
                return null
            }
            binding.edtLatitude.setText(searchResults[0].latitude.toString())
            binding.edtLongitude.setText(searchResults[0].longitude.toString())
            return searchResults[0]
//            val foundAddress: Address = searchResults[0]
//            binding.txtAddressWarning.visibility = View.VISIBLE
//            binding.txtAddressWarning.text = "Latitude: ${foundAddress.latitude}\nLongitude: ${foundAddress.longitude}"


        } catch(ex: Exception) {
            Log.d("Get Coordinate Error", "Error: ${ex}")
            return null
        }
    }
    private fun getDeviceLocation() {
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) !== PackageManager.PERMISSION_GRANTED
            &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !== PackageManager.PERMISSION_GRANTED) {
            multiplePermissionResultLauncher.launch(APP_PERMISSION_LIST)
            return
        }

        fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY,
            object: CancellationToken() {
                override fun onCanceledRequested(p0: OnTokenCanceledListener) = CancellationTokenSource().token
                override fun isCancellationRequested() = false
            })
            .addOnSuccessListener { location: Location? ->
                if(location == null) {
                    return@addOnSuccessListener
                }

                //val message = "Lat: ${location.latitude}\nLong: ${location.longitude}"
            }
    }
    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        gMap.uiSettings.isZoomControlsEnabled = true

        if (::selectedPackage.isInitialized) {
            loadPackageIntoUI(selectedPackage)
        }
    }
}