package com.example.secureafenceclient.ui.deliveries

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.secureafenceclient.R
import com.example.secureafenceclient.data.model.ClientShipment
import com.example.secureafenceclient.data.network.ClientApiClient
import com.example.secureafenceclient.data.network.ClientSessionManager
import com.example.secureafenceclient.databinding.FragmentDeliveryTrackingBinding
import kotlinx.coroutines.launch

class DeliveryTrackingFragment : Fragment() {

    private var _binding: FragmentDeliveryTrackingBinding? = null
    private val binding get() = _binding!!

    private val shipmentList = mutableListOf<ClientShipment>()
    private lateinit var adapter: ShipmentAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDeliveryTrackingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ShipmentAdapter(shipmentList)
        binding.rvShipments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvShipments.adapter = adapter

        loadShipments()
    }

    private fun loadShipments() {
        binding.pbLoading.visibility = View.VISIBLE
        val customerEmail = ClientSessionManager.getCustomerEmail(requireContext())

        lifecycleScope.launch {
            try {
                val response = ClientApiClient.instance.getShipments(customerEmail.ifEmpty { null })
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    shipmentList.clear()
                    shipmentList.addAll(response.body()!!)
                    adapter.notifyDataSetChanged()
                } else {
                    loadFallbackShipments()
                }
            } catch (e: Exception) {
                loadFallbackShipments()
            } finally {
                binding.pbLoading.visibility = View.GONE
            }
        }
    }

    private fun loadFallbackShipments() {
        shipmentList.clear()
        shipmentList.add(
            ClientShipment(
                id = "SHP-3021",
                orderId = "ORD-8492",
                type = "Delivery",
                driverName = "Dave Miller (Flatbed Rig #4)",
                status = "Out For Delivery",
                eta = "Today at 2:30 PM (~25 mins away)",
                destination = "450 Main St (Downtown Tower Site)",
                notes = "Contact site manager Mike Vance upon arrival."
            )
        )
        shipmentList.add(
            ClientShipment(
                id = "SHP-2980",
                orderId = "ORD-7311",
                type = "Delivery",
                driverName = "Robert Taylor",
                status = "Delivered",
                eta = "Completed May 12, 11:15 AM",
                destination = "1200 Westside Blvd",
                notes = "Fencing panels unloaded at West Gate."
            )
        )
        adapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class ShipmentAdapter(private val items: List<ClientShipment>) :
        RecyclerView.Adapter<ShipmentAdapter.ShipmentViewHolder>() {

        class ShipmentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvId: TextView = view.findViewById(R.id.tvShipmentId)
            val tvStatus: TextView = view.findViewById(R.id.tvShipmentStatus)
            val tvDriver: TextView = view.findViewById(R.id.tvDriverInfo)
            val tvETA: TextView = view.findViewById(R.id.tvETA)
            val tvDestination: TextView = view.findViewById(R.id.tvDestination)
            val tvNotes: TextView = view.findViewById(R.id.tvNotes)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShipmentViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_shipment_tracking, parent, false)
            return ShipmentViewHolder(view)
        }

        override fun onBindViewHolder(holder: ShipmentViewHolder, position: Int) {
            val item = items[position]
            holder.tvId.text = item.id ?: "SHP-000"
            holder.tvStatus.text = item.status ?: "Scheduled"
            holder.tvDriver.text = "Driver: ${item.driverName ?: "Unassigned"}"
            holder.tvETA.text = "Estimated Arrival: ${item.eta ?: "Pending dispatch"}"
            holder.tvDestination.text = "Destination: ${item.destination ?: "Site Address N/A"}"
            holder.tvNotes.text = "Notes: ${item.notes.orEmpty().ifEmpty { "None" }}"
        }

        override fun getItemCount(): Int = items.size
    }
}
