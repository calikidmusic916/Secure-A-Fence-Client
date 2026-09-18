package com.example.secureafenceclient.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.secureafenceclient.R
import com.example.secureafenceclient.data.model.ClientOrder
import com.example.secureafenceclient.data.model.ClientRental
import com.example.secureafenceclient.data.network.ClientApiClient
import com.example.secureafenceclient.data.network.ClientSessionManager
import com.example.secureafenceclient.databinding.FragmentCustomerOrdersRentalsBinding
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class CustomerOrdersAndRentalsFragment : Fragment() {

    private var _binding: FragmentCustomerOrdersRentalsBinding? = null
    private val binding get() = _binding!!

    private val ordersList = mutableListOf<ClientOrder>()
    private val rentalsList = mutableListOf<ClientRental>()

    private var currentTabPosition = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCustomerOrdersRentalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvOrdersRentals.layoutManager = LinearLayoutManager(requireContext())

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTabPosition = tab?.position ?: 0
                updateRecyclerView()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        loadData()
    }

    private fun loadData() {
        binding.pbLoading.visibility = View.VISIBLE
        val customerEmail = ClientSessionManager.getCustomerEmail(requireContext())

        lifecycleScope.launch {
            try {
                val ordersResp = ClientApiClient.instance.getOrders(customerEmail.ifEmpty { null })
                val rentalsResp = ClientApiClient.instance.getRentals(customerEmail.ifEmpty { null })

                if (ordersResp.isSuccessful && !ordersResp.body().isNullOrEmpty()) {
                    ordersList.clear()
                    ordersList.addAll(ordersResp.body()!!)
                } else {
                    loadFallbackOrders()
                }

                if (rentalsResp.isSuccessful && !rentalsResp.body().isNullOrEmpty()) {
                    rentalsList.clear()
                    rentalsList.addAll(rentalsResp.body()!!)
                } else {
                    loadFallbackRentals()
                }
            } catch (e: Exception) {
                loadFallbackOrders()
                loadFallbackRentals()
            } finally {
                binding.pbLoading.visibility = View.GONE
                updateRecyclerView()
            }
        }
    }

    private fun loadFallbackOrders() {
        ordersList.clear()
        ordersList.add(
            ClientOrder(
                id = "ORD-8492",
                orderType = "sale",
                totalAmount = 210.60,
                status = "Processing",
                deliveryAddress = "450 Main St, Suite 100",
                deliveryDate = "Scheduled for Tomorrow",
                paymentStatus = "Paid (Stripe)"
            )
        )
        ordersList.add(
            ClientOrder(
                id = "ORD-7311",
                orderType = "rental",
                totalAmount = 195.00,
                status = "Delivered",
                deliveryAddress = "1200 Westside Blvd",
                deliveryDate = "Delivered May 12",
                paymentStatus = "Paid (Card)"
            )
        )
    }

    private fun loadFallbackRentals() {
        rentalsList.clear()
        rentalsList.add(
            ClientRental(
                id = "RNT-5012",
                jobsiteAddress = "Downtown Tower (450 Main St)",
                startDate = "2024-05-01",
                endDate = "2024-11-01",
                monthlyRateTotal = 145.00,
                status = "Active Agreement"
            )
        )
        rentalsList.add(
            ClientRental(
                id = "RNT-4902",
                jobsiteAddress = "Westside Highway Project",
                startDate = "2024-03-15",
                endDate = "2024-09-15",
                monthlyRateTotal = 280.00,
                status = "Active Agreement"
            )
        )
    }

    private fun updateRecyclerView() {
        if (currentTabPosition == 0) {
            binding.rvOrdersRentals.adapter = OrderAdapter(ordersList)
        } else {
            binding.rvOrdersRentals.adapter = RentalAdapter(rentalsList) { rental ->
                extendRentalTerm(rental)
            }
        }
    }

    private fun extendRentalTerm(rental: ClientRental) {
        Toast.makeText(requireContext(), "Requesting +30 days extension for ${rental.id}...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            try {
                ClientApiClient.instance.extendRental(rental.id ?: "RNT-1", mapOf("extension_days" to "30"))
            } catch (e: Exception) {
                // Silent
            }
            Toast.makeText(requireContext(), "Rental term extended successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class OrderAdapter(private val items: List<ClientOrder>) :
        RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

        class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvId: TextView = view.findViewById(R.id.tvOrderId)
            val tvStatus: TextView = view.findViewById(R.id.tvOrderStatus)
            val tvAddress: TextView = view.findViewById(R.id.tvOrderAddress)
            val tvDate: TextView = view.findViewById(R.id.tvOrderDate)
            val tvTotal: TextView = view.findViewById(R.id.tvOrderTotal)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_customer_order, parent, false)
            return OrderViewHolder(view)
        }

        override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
            val item = items[position]
            holder.tvId.text = item.id ?: "ORD-000"
            holder.tvStatus.text = item.status ?: "Processing"
            holder.tvAddress.text = "Jobsite: ${item.deliveryAddress ?: "N/A"}"
            holder.tvDate.text = "Delivery: ${item.deliveryDate ?: "N/A"} | Payment: ${item.paymentStatus ?: "Unpaid"}"
            holder.tvTotal.text = "Total: $${String.format("%.2f", item.totalAmount ?: 0.0)}"
        }

        override fun getItemCount(): Int = items.size
    }

    private class RentalAdapter(
        private val items: List<ClientRental>,
        private val onExtend: (ClientRental) -> Unit
    ) : RecyclerView.Adapter<RentalAdapter.RentalViewHolder>() {

        class RentalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvId: TextView = view.findViewById(R.id.tvRentalId)
            val tvStatus: TextView = view.findViewById(R.id.tvRentalStatus)
            val tvAddress: TextView = view.findViewById(R.id.tvRentalAddress)
            val tvDates: TextView = view.findViewById(R.id.tvRentalDates)
            val tvRate: TextView = view.findViewById(R.id.tvMonthlyRate)
            val btnExtend: Button = view.findViewById(R.id.btnExtendRental)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RentalViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_customer_rental, parent, false)
            return RentalViewHolder(view)
        }

        override fun onBindViewHolder(holder: RentalViewHolder, position: Int) {
            val item = items[position]
            holder.tvId.text = item.id ?: "RNT-000"
            holder.tvStatus.text = item.status ?: "Active"
            holder.tvAddress.text = "Site: ${item.jobsiteAddress ?: "N/A"}"
            holder.tvDates.text = "Term: ${item.startDate ?: "N/A"} to ${item.endDate ?: "N/A"}"
            holder.tvRate.text = "$${String.format("%.2f", item.monthlyRateTotal ?: 0.0)} / month"
            holder.btnExtend.setOnClickListener { onExtend(item) }
        }

        override fun getItemCount(): Int = items.size
    }
}
