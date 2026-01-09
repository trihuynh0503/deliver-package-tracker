package com.map524.a4_minh_tri

import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.map524.a4_minh_tri.databinding.ActivityAddNewpackageBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AddNewpackage : AppCompatActivity() {
    lateinit var binding: ActivityAddNewpackageBinding
    lateinit var geocoder: Geocoder
    private lateinit var packageDao: PackageDao


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddNewpackageBinding.inflate(layoutInflater)
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
        Geocoder(this, Locale.getDefault())
        val deliveryStatus = arrayOf("In Transit")
        binding.spinnerStatus.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, deliveryStatus)

        binding.edtDeliveryDate.setOnClickListener {
            val constraints = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
                .build()

            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Delivery Date")
                .setCalendarConstraints(constraints)
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

        binding.btnAddPackage.setOnClickListener {
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
                packageDao.insert(Package(
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
                binding.txtAddressWarning.text = "No coordinatess found"
                binding.txtAddressWarning.visibility = View.VISIBLE
                return null
            }
            return searchResults[0]
//            val foundAddress: Address = searchResults[0]
//            binding.txtAddressWarning.visibility = View.VISIBLE
//            binding.txtAddressWarning.text = "Latitude: ${foundAddress.latitude}\nLongitude: ${foundAddress.longitude}"


        } catch(ex: Exception) {
            Log.d("Get Coordinate Error", "Error: ${ex}")
            return null
        }
    }
}