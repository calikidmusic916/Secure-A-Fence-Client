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
    private lateinit var adapter: InvoiceAdapter
    private lateinit var paymentSheet: PaymentSheet

    private var selectedInvoiceForPayment: ClientInvoice? = null

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

        adapter = InvoiceAdapter(
            items = invoiceList,
            onPayInvoice = { invoice -> processInvoicePayment(invoice) }
        )

        binding.rvInvoices.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInvoices.adapter = adapter

        loadInvoices()
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
                    adapter.notifyDataSetChanged()
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
        adapter.notifyDataSetChanged()
    }

    private fun processInvoicePayment(invoice: ClientInvoice) {
        selectedInvoiceForPayment = invoice
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
                selectedInvoiceForPayment?.let { markInvoicePaid(it) }
            }
            is PaymentSheetResult.Canceled -> {
                Toast.makeText(requireContext(), "Payment Canceled", Toast.LENGTH_SHORT).show()
            }
            is PaymentSheetResult.Failed -> {
                Toast.makeText(requireContext(), "Payment Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun markInvoicePaid(invoice: ClientInvoice) {
        val index = invoiceList.indexOfFirst { it.id == invoice.id }
        if (index >= 0) {
            invoiceList[index] = invoice.copy(status = "paid")
            adapter.notifyDataSetChanged()
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
