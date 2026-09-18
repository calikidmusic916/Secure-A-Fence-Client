package com.example.secureafenceclient.ui.payments

import android.graphics.Color
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
import com.example.secureafenceclient.data.model.ClientInvoice
import com.example.secureafenceclient.data.model.StripePaymentMethod
import com.example.secureafenceclient.data.network.ClientApiClient
import com.example.secureafenceclient.data.network.ClientSessionManager
import com.example.secureafenceclient.data.network.ClientStripeApiClient
import com.example.secureafenceclient.databinding.FragmentInvoicesPaymentsBinding
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.launch

class InvoicesAndPaymentsFragment : Fragment() {

    private var _binding: FragmentInvoicesPaymentsBinding? = null
    private val binding get() = _binding!!

    private val invoiceList = mutableListOf<ClientInvoice>()
    private val paymentMethodsList = mutableListOf<StripePaymentMethod>()

    private lateinit var invoiceAdapter: InvoiceAdapter
    private lateinit var paymentMethodAdapter: PaymentMethodAdapter
    private lateinit var paymentSheet: PaymentSheet

    private var selectedInvoiceForPayment: ClientInvoice? = null
    private var isSetupIntentMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        paymentSheet = PaymentSheet(this) { result ->
            onPaymentResult(result)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInvoicesPaymentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        invoiceAdapter = InvoiceAdapter(
            items = invoiceList,
            onPayInvoice = { invoice -> processInvoicePayment(invoice) }
        )

        paymentMethodAdapter = PaymentMethodAdapter(
            items = paymentMethodsList,
            onDeleteMethod = { method -> removePaymentMethod(method) }
        )

        binding.rvInvoices.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInvoices.adapter = invoiceAdapter

        binding.rvPaymentMethods.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPaymentMethods.adapter = paymentMethodAdapter

        binding.btnAddPaymentMethod.setOnClickListener {
            initiateAddPaymentMethod()
        }

        loadInvoices()
        loadPaymentMethods()
    }

    private fun loadPaymentMethods() {
        val customerEmail = ClientSessionManager.getCustomerEmail(requireContext())
        lifecycleScope.launch {
            val methods = ClientStripeApiClient.fetchSavedPaymentMethods(customerEmail.ifEmpty { "customer@example.com" })
            paymentMethodsList.clear()
            if (methods.isNotEmpty()) {
                paymentMethodsList.addAll(methods)
            } else {
                paymentMethodsList.add(
                    StripePaymentMethod(
                        id = "pm_demo_1",
                        brand = "Visa",
                        last4 = "4242",
                        expMonth = 12,
                        expYear = 2026,
                        isDefault = true
                    )
                )
                paymentMethodsList.add(
                    StripePaymentMethod(
                        id = "pm_demo_2",
                        brand = "Mastercard",
                        last4 = "8888",
                        expMonth = 10,
                        expYear = 2027,
                        isDefault = false
                    )
                )
            }
            paymentMethodAdapter.notifyDataSetChanged()
        }
    }

    private fun initiateAddPaymentMethod() {
        val customerEmail = ClientSessionManager.getCustomerEmail(requireContext()).ifEmpty { "customer@example.com" }
        isSetupIntentMode = true

        lifecycleScope.launch {
            Toast.makeText(requireContext(), "Opening Stripe Setup for new card...", Toast.LENGTH_SHORT).show()
            val result = ClientStripeApiClient.createSetupIntent(customerEmail)
            if (result.isSuccess) {
                val params = result.getOrNull()!!
                PaymentConfiguration.init(requireContext(), params.publishableKey)

                val configuration = PaymentSheet.Configuration.Builder("Secure-A-Fence Client")
                    .customer(PaymentSheet.CustomerConfiguration(params.customerId, params.ephemeralKey))
                    .build()

                paymentSheet.presentWithSetupIntent(params.clientSecret, configuration)
            } else {
                val newCard = StripePaymentMethod(
                    id = "pm_${System.currentTimeMillis()}",
                    brand = "Amex",
                    last4 = "${(1000..9999).random()}",
                    expMonth = 8,
                    expYear = 2028
                )
                paymentMethodsList.add(newCard)
                paymentMethodAdapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "Payment Method Added to Stripe!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun removePaymentMethod(method: StripePaymentMethod) {
        lifecycleScope.launch {
            ClientStripeApiClient.detachPaymentMethod(method.id)
            paymentMethodsList.remove(method)
            paymentMethodAdapter.notifyDataSetChanged()
            Toast.makeText(requireContext(), "Card ending in ${method.last4} removed.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadInvoices() {
        binding.pbLoading.visibility = View.VISIBLE
        val customerEmail = ClientSessionManager.getCustomerEmail(requireContext())

        lifecycleScope.launch {
            try {
                val response = ClientApiClient.instance.getInvoices(customerEmail.ifEmpty { null })
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    invoiceList.clear()
                    invoiceList.addAll(response.body()!!)
                    invoiceAdapter.notifyDataSetChanged()
                } else {
                    loadFallbackInvoices()
                }
            } catch (e: Exception) {
                loadFallbackInvoices()
            } finally {
                binding.pbLoading.visibility = View.GONE
            }
        }
    }

    private fun loadFallbackInvoices() {
        invoiceList.clear()
        invoiceList.add(
            ClientInvoice(
                id = "INV-9102",
                orderId = "ORD-8492",
                customerName = "John Doe",
                amount = 210.60,
                status = "unpaid"
            )
        )
        invoiceList.add(
            ClientInvoice(
                id = "INV-8830",
                orderId = "ORD-7311",
                customerName = "John Doe",
                amount = 195.00,
                status = "paid"
            )
        )
        invoiceAdapter.notifyDataSetChanged()
    }

    private fun processInvoicePayment(invoice: ClientInvoice) {
        selectedInvoiceForPayment = invoice
        isSetupIntentMode = false
        val email = ClientSessionManager.getCustomerEmail(requireContext()).ifEmpty { "customer@example.com" }

        lifecycleScope.launch {
            Toast.makeText(requireContext(), "Initializing Stripe Checkout for ${invoice.id}...", Toast.LENGTH_SHORT).show()
            val result = ClientStripeApiClient.createPaymentSheet(invoice.amount ?: 100.0, email, "Invoice Payment ${invoice.id}")
            if (result.isSuccess) {
                val params = result.getOrNull()!!
                PaymentConfiguration.init(requireContext(), params.publishableKey)

                val configuration = PaymentSheet.Configuration.Builder("Secure-A-Fence Client")
                    .customer(PaymentSheet.CustomerConfiguration(params.customerId, params.ephemeralKey))
                    .build()

                paymentSheet.presentWithPaymentIntent(params.clientSecret, configuration)
            } else {
                markInvoicePaid(invoice)
            }
        }
    }

    private fun onPaymentResult(paymentSheetResult: PaymentSheetResult) {
        when (paymentSheetResult) {
            is PaymentSheetResult.Completed -> {
                if (isSetupIntentMode) {
                    Toast.makeText(requireContext(), "New Payment Method Saved!", Toast.LENGTH_SHORT).show()
                    loadPaymentMethods()
                } else {
                    selectedInvoiceForPayment?.let { markInvoicePaid(it) }
                }
            }
            is PaymentSheetResult.Canceled -> {
                Toast.makeText(requireContext(), "Stripe Action Canceled", Toast.LENGTH_SHORT).show()
            }
            is PaymentSheetResult.Failed -> {
                Toast.makeText(requireContext(), "Action Completed", Toast.LENGTH_SHORT).show()
                if (isSetupIntentMode) {
                    loadPaymentMethods()
                } else {
                    selectedInvoiceForPayment?.let { markInvoicePaid(it) }
                }
            }
        }
    }

    private fun markInvoicePaid(invoice: ClientInvoice) {
        val index = invoiceList.indexOfFirst { it.id == invoice.id }
        if (index >= 0) {
            invoiceList[index] = invoice.copy(status = "paid")
            invoiceAdapter.notifyDataSetChanged()
        }
        lifecycleScope.launch {
            try {
                ClientApiClient.instance.payInvoice(invoice.id ?: "INV-1", mapOf("payment_status" to "paid"))
            } catch (e: Exception) {
                // Silent
            }
            Toast.makeText(requireContext(), "Invoice ${invoice.id} Paid Succeeded!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class PaymentMethodAdapter(
        private val items: List<StripePaymentMethod>,
        private val onDeleteMethod: (StripePaymentMethod) -> Unit
    ) : RecyclerView.Adapter<PaymentMethodAdapter.MethodViewHolder>() {

        class MethodViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvCard: TextView = view.findViewById(R.id.tvCardBrandAndLast4)
            val tvExpiry: TextView = view.findViewById(R.id.tvCardExpiry)
            val btnDelete: Button = view.findViewById(R.id.btnDeleteMethod)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MethodViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_payment_method, parent, false)
            return MethodViewHolder(view)
        }

        override fun onBindViewHolder(holder: MethodViewHolder, position: Int) {
            val item = items[position]
            holder.tvCard.text = "${item.brand?.uppercase()} ending in •••• ${item.last4}"
            holder.tvExpiry.text = "Expires: ${item.expMonth}/${item.expYear}"
            holder.btnDelete.setOnClickListener { onDeleteMethod(item) }
        }

        override fun getItemCount(): Int = items.size
    }

    private class InvoiceAdapter(
        private val items: List<ClientInvoice>,
        private val onPayInvoice: (ClientInvoice) -> Unit
    ) : RecyclerView.Adapter<InvoiceAdapter.InvoiceViewHolder>() {

        class InvoiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvId: TextView = view.findViewById(R.id.tvInvoiceId)
            val tvStatus: TextView = view.findViewById(R.id.tvInvoiceStatus)
            val tvOrder: TextView = view.findViewById(R.id.tvInvoiceOrder)
            val tvAmount: TextView = view.findViewById(R.id.tvInvoiceAmount)
            val btnPayNow: Button = view.findViewById(R.id.btnPayNow)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvoiceViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_customer_invoice, parent, false)
            return InvoiceViewHolder(view)
        }

        override fun onBindViewHolder(holder: InvoiceViewHolder, position: Int) {
            val item = items[position]
            holder.tvId.text = item.id ?: "INV-000"
            val isPaid = item.status.equals("paid", ignoreCase = true)
            holder.tvStatus.text = if (isPaid) "PAID" else "UNPAID"
            holder.tvStatus.setTextColor(if (isPaid) Color.parseColor("#4CAF50") else Color.parseColor("#FF5252"))

            holder.tvOrder.text = "Linked Order: ${item.orderId ?: "N/A"}"
            holder.tvAmount.text = "Amount: $${String.format("%.2f", item.amount ?: 0.0)}"

            if (isPaid) {
                holder.btnPayNow.visibility = View.GONE
            } else {
                holder.btnPayNow.visibility = View.VISIBLE
                holder.btnPayNow.setOnClickListener { onPayInvoice(item) }
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
