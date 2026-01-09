package com.map524.a4_minh_tri

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.map524.a4_minh_tri.databinding.PackageItemBinding

class PackageAdapter(val packageList: MutableList<Package>, val clickInterface: ClickListenerInterface): RecyclerView.Adapter<PackageAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PackageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pkg = packageList[position]

        // Bind data to views
        holder.binding.txtCustomerName.text = pkg.customerName
        holder.binding.txtAddress.text = pkg.address
        holder.binding.txtDeliveryStatus.text = pkg.deliveryStatus.name.replace("_", " ")

        holder.binding.packageInfo.setOnClickListener {
            clickInterface.displayPackage(position)
        }
        holder.binding.imgDelete.setOnClickListener {
            clickInterface.deletePackage(position)
        }
    }

    override fun getItemCount() = packageList.size
    class ViewHolder(val binding: PackageItemBinding) : RecyclerView.ViewHolder(binding.root)

}

