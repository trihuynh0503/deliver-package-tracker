package com.map524.a4_minh_tri

import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.map524.a4_minh_tri.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class DeliveryStatus {
    IN_TRANSIT,
    DELIVERED
}
class MainActivity : AppCompatActivity(), ClickListenerInterface {

    lateinit var binding: ActivityMainBinding
    private lateinit var packageDao: PackageDao
    private val packageList = mutableListOf<Package>()
    lateinit var packageAdapter: PackageAdapter

    lateinit var geocoder: Geocoder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //set up Room Database
        val db = PackageDatabase.getInstance(this)
        packageDao = db.packageDao()

        //set up Sort by option
        val sortByOptions = arrayOf(
            "ALL", "IN_TRANSIT", "DELIVERED"
        )
        binding.spinnerSortBy.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            sortByOptions
        )
        binding.spinnerSortBy.onItemSelectedListener  = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> loadPackages("ALL")
                    1 -> loadPackages("IN_TRANSIT")
                    2 -> loadPackages("DELIVERED")
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // setup Adapter
        packageAdapter = PackageAdapter(packageList, this)
        binding.rvPackageList.adapter = packageAdapter
        binding.rvPackageList.layoutManager = LinearLayoutManager(this)

        //      Add divider for RecyclerView
        val divider = DividerItemDecoration(this, LinearLayoutManager.VERTICAL)
        binding.rvPackageList.addItemDecoration(divider)
        GlobalScope.launch(Dispatchers.IO) {
            var packages = packageDao.getAllPackage()
            if (packages.isEmpty()) {
                // Insert
                packageDao.insert(Package(0, "Johnathan Lee",
                    "1750 Finch Ave E, North York, ON M2J 2X5",
                    43.7955,
                    -79.3496,
                    DeliveryStatus.IN_TRANSIT,
                    "2025-12-08"
                ))
                packageDao.insert(Package(
                    customerName = "Catherine Davids",
                    address = "Finch & Don Mills Plaza, 1555 Finch Ave E, North York",
                    latitude = 43.7991,
                    longitude = -79.3470,
                    deliveryStatus = DeliveryStatus.IN_TRANSIT,
                    deliveryDate = "2025-12-07"
                ))
                packageDao.insert(Package(
                    customerName = "Emily Tran",
                    address = "Fairview Mall, 1800 Sheppard Ave E, North York",
                    latitude = 43.7775,
                    longitude = -79.3450,
                    deliveryStatus = DeliveryStatus.DELIVERED,
                    deliveryDate = "2025-11-27"
                ))
            }
            loadPackages("ALL")
        }
        binding.btnAddPackage.setOnClickListener {
            val addNewPackageIntent = Intent(this@MainActivity, AddNewpackage::class.java)
            startActivity(addNewPackageIntent)
        }

    }
    override fun onResume() {
        super.onResume()
        binding.spinnerSortBy.setSelection(0, true)
        loadPackages("ALL")
    }
    private fun loadPackages(filter: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val packages = when (filter) {
                "ALL" -> packageDao.getAllPackage()
                "IN_TRANSIT" -> packageDao.searchPackagesByStatus(DeliveryStatus.IN_TRANSIT)
                "DELIVERED" -> packageDao.searchPackagesByStatus(DeliveryStatus.DELIVERED)
                else -> emptyList()
            }

            // Now update UI and RecyclerView
            withContext(Dispatchers.Main) {
                packageList.clear()
                packageList.addAll(packages)
                packageAdapter.notifyDataSetChanged()
            }
        }
    }
    override fun displayPackage(position: Int) {
        val packageToDisplay = packageList.getOrNull(position) ?: return
        Intent(this, PackageDetails::class.java)
        Intent()
        // Navigate to PackageDetails screen with the package ID
        val intent = Intent(this, PackageDetails::class.java)
        intent.putExtra("PACKAGE_ID", packageToDisplay.id)
        startActivity(intent)
    }
    override fun deletePackage(position: Int) {
        //safety check
        val packageToDelete = packageList.getOrNull(position) ?: return
        GlobalScope.launch(Dispatchers.IO) {
            packageDao.delete(packageToDelete)
            val newList = packageDao.getAllPackage()
            withContext(Dispatchers.Main) {
                // Refresh package list
                packageList.clear()
                packageList.addAll(newList)
                packageAdapter.notifyDataSetChanged()
            }
        }

    }
}