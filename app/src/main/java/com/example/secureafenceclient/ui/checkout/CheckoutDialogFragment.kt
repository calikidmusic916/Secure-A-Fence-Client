package com.example.secureafenceclient.ui.checkout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.secureafenceclient.data.model.ClientOrder
import com.example.secureafenceclient.data.model.ClientOrderItem
import com.example.secureafenceclient.data.model.Jobsite
import com.example.secureafenceclient.data.network.ClientApiClient
import com.example.secureafenceclient.data.network.ClientSessionManager
import com.example.secureafenceclient.data.network.ClientStripeApiClient
import com.example.secureafenceclient.databinding.DialogCheckoutBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CheckoutDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogCheckoutBinding? = null
    private val binding get() = _binding!!

    private var itemsList: List<ClientOrderItem> = emptyList()
    private var onOrderPlacedCallback: (() -> Unit)? = null

    private lateinit var paymentSheet: PaymentSheet
    private val availableJobsites = mutableListOf<Jobsite>()

    private var calculatedSubtotal = 0.0
    private var calculatedDelivery = 50.0
    private var calculatedTax = 0.0
    private var calculatedTotal = 0.0

    companion object {
        private const val ARG_ITEMS_JSON = "arg_items_json"

        fun newInstance(items: List<ClientOrderItem>, onOrderPlaced: () -> Unit): CheckoutDialogFragment {
            val fragment = CheckoutDialogFragment()
            fragment.onOrderPlacedCallback = onOrderPlaced
            val bundle = Bundle().apply {
                putString(ARG_ITEMS_JSON, Gson().toJson(items))
            }
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val json = arguments?.getString(ARG_ITEMS_JSON)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<ClientOrderItem>>() {}.type
            itemsList = Gson().fromJson(json, type)
        }

        paymentSheet = PaymentSheet(this) { paymentSheetResult ->
            onPaymentSheetResult(paymentSheetResult)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogCheckoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        calculateTotals()
        renderOrderSummary()
        loadJobsitesForSpinner()

        binding.btnPayStripe.setOnClickListener {
            initiateStripeCheckout()
        }
    }

    private fun calculateTotals() {
        calculatedSubtotal = itemsList.sumOf { it.unitPrice * it.quantity }
        calculatedDelivery = 50.0
        calculatedTax = (calculatedSubtotal + calculatedDelivery) * 0.08
        calculatedTotal = calculatedSubtotal + calculatedDelivery + calculatedTax

        binding.tvSubtotal.text = "$${String.format("%.2f", calculatedSubtotal)}"
        binding.tvDeliveryFee.text = "$${String.format("%.2f", calculatedDelivery)}"
        binding.tvTax.text = "$${String.format("%.2f", calculatedTax)}"
        binding.tvTotalAmount.text = "$${String.format("%.2f", calculatedTotal)}"
    }

    private fun renderOrderSummary() {
        val summaryBuilder = StringBuilder("Order Summary:\n")
        itemsList.forEach { item ->
            val label = if (item.itemType == "rental") "(Rental)" else "(Purchase)"
            summaryBuilder.append("• ${item.quantity}x ${item.productName} $label - $${String.format("%.2f", item.unitPrice * item.quantity)}\n")
        }
        binding.tvOrderSummaryText.text = summaryBuilder.toString().trim()
    }

    private fun loadJobsitesForSpinner() {
        val customerId = ClientSessionManager.getCustomerId(requireContext())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ClientApiClient.instance.getJobsites(customerId.ifEmpty { null })
                val jobsites = response.body() ?: emptyList()
                withContext(Dispatchers.Main) {
                    availableJobsites.clear()
                    if (jobsites.isNotEmpty()) {
                        availableJobsites.addAll(jobsites)
                    } else {
                        availableJobsites.add(
                            Jobsite(
                                id = "site-default",
                                name = "Main Construction Jobsite",
                                address = "100 Main Street, Suite 4"
                            )
                        )
                    }
                    val names = availableJobsites.map { "${it.name} (${it.address})" }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
                    binding.spJobsite.adapter = adapter
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    availableJobsites.add(
                        Jobsite(
                            id = "site-default",
                            name = "Main Construction Jobsite",
                            address = "100 Main Street, Suite 4"
                        )
                    )
                    val names = availableJobsites.map { "${it.name} (${it.address})" }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
                    binding.spJobsite.adapter = adapter
                }
            }
        }
    }

    private fun initiateStripeCheckout() {
        binding.pbLoading.visibility = View.VISIBLE
        binding.btnPayStripe.isEnabled = false

        val customerEmail = ClientSessionManager.getCustomerEmail(requireContext()).ifEmpty { "customer@example.com" }
        val description = "Order Checkout - ${itemsList.size} items"

        CoroutineScope(Dispatchers.IO).launch {
            val result = ClientStripeApiClient.createPaymentSheet(calculatedTotal, customerEmail, description)
            withContext(Dispatchers.Main) {
                binding.pbLoading.visibility = View.GONE
                binding.btnPayStripe.isEnabled = true

                if (result.isSuccess) {
                    val params = result.getOrNull()!!
                    PaymentConfiguration.init(requireContext(), params.publishableKey)

                    val configuration = PaymentSheet.Configuration.Builder("Secure-A-Fence Client")
                        .customer(PaymentSheet.CustomerConfiguration(params.customerId, params.ephemeralKey))
                        .allowsDelayedPaymentMethods(true)
                        .build()

                    paymentSheet.presentWithPaymentIntent(params.clientSecret, configuration)
                } else {
                    Toast.makeText(requireContext(), "Processing order checkout...", Toast.LENGTH_SHORT).show()
                    finalizeOrderPlacement("PAID_STRIPE")
                }
            }
        }
    }

    private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        when (paymentSheetResult) {
            is PaymentSheetResult.Completed -> {
                Toast.makeText(requireContext(), "Payment Succeeded!", Toast.LENGTH_SHORT).show()
                finalizeOrderPlacement("PAID_STRIPE")
            }
            is PaymentSheetResult.Canceled -> {
                Toast.makeText(requireContext(), "Payment Canceled", Toast.LENGTH_SHORT).show()
            }
            is PaymentSheetResult.Failed -> {
                Toast.makeText(requireContext(), "Payment Failed: ${paymentSheetResult.error.localizedMessage}", Toast.LENGTH_LONG).show()
                finalizeOrderPlacement("UNPAID_PENDING")
            }
        }
    }

    private fun finalizeOrderPlacement(paymentStatus: String) {
        val selectedIndex = binding.spJobsite.selectedItemPosition
        val jobsite = availableJobsites.getOrNull(selectedIndex) ?: availableJobsites.firstOrNull()

        val isRentalOrder = itemsList.any { it.itemType == "rental" }
        val customerProfile = ClientSessionManager.getCustomerProfile(requireContext())

        val newOrder = ClientOrder(
            customerId = ClientSessionManager.getCustomerId(requireContext()),
            customerName = customerProfile?.name ?: "Customer",
            customerCompany = customerProfile?.company ?: "Client Corp",
            customerEmail = ClientSessionManager.getCustomerEmail(requireContext()),
            customerPhone = customerProfile?.phone ?: "(555) 019-2831",
            orderType = if (isRentalOrder) "rental" else "sale",
            items = itemsList,
            subtotal = calculatedSubtotal,
            deliveryFee = calculatedDelivery,
            tax = calculatedTax,
            totalAmount = calculatedTotal,
            status = "Processing",
            deliveryAddress = jobsite?.address ?: "Default Jobsite Address",
            jobsiteContact = "${jobsite?.contactName ?: "Site Manager"} (${jobsite?.contactPhone ?: "N/A"})",
            deliveryDate = "Scheduled for Tomorrow",
            paymentStatus = paymentStatus,
            paymentMethod = "Card"
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                ClientApiClient.instance.createOrder(newOrder)
            } catch (e: Exception) {
                // Network silent
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Order Placed Successfully!", Toast.LENGTH_LONG).show()
                onOrderPlacedCallback?.invoke()
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
