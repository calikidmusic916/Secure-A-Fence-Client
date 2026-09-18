package com.example.secureafenceclient.ui.catalog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.secureafenceclient.R
import com.example.secureafenceclient.data.model.ClientOrderItem
import com.example.secureafenceclient.data.model.ClientProduct
import com.example.secureafenceclient.data.network.ClientApiClient
import com.example.secureafenceclient.databinding.FragmentProductCatalogBinding
import com.example.secureafenceclient.ui.checkout.CheckoutDialogFragment
import kotlinx.coroutines.launch

class PurchaseCatalogFragment : Fragment() {

    private var _binding: FragmentProductCatalogBinding? = null
    private val binding get() = _binding!!

    private val purchaseProductsList = mutableListOf<ClientProduct>()
    private val cartItems = mutableListOf<ClientOrderItem>()
    private lateinit var adapter: PurchaseProductAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProductCatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvCatalogTitle.text = "Outright Purchase Catalog"
        binding.toggleGroupType.visibility = View.GONE

        adapter = PurchaseProductAdapter(purchaseProductsList) { product ->
            addToPurchaseCart(product)
        }

        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = adapter

        binding.btnViewCart.setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(requireContext(), "Your purchase cart is empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val checkoutDialog = CheckoutDialogFragment.newInstance(cartItems) {
                cartItems.clear()
                updateCartBadge()
                loadPurchaseProducts()
            }
            checkoutDialog.show(parentFragmentManager, "CheckoutDialog")
        }

        loadPurchaseProducts()
    }

    override fun onResume() {
        super.onResume()
        loadPurchaseProducts()
    }

    private fun loadPurchaseProducts() {
        binding.pbLoading.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = ClientApiClient.instance.getProducts()
                purchaseProductsList.clear()
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    purchaseProductsList.addAll(response.body()!!.filter { it.isPurchase == true && it.suspended != true })
                }
            } catch (e: Exception) {
                // Silent
            } finally {
                adapter.notifyDataSetChanged()
                binding.pbLoading.visibility = View.GONE
            }
        }
    }

    private fun addToPurchaseCart(product: ClientProduct) {
        val stock = product.inStock ?: 0
        if (stock <= 0) {
            Toast.makeText(requireContext(), "${product.name} is currently out of stock for purchase.", Toast.LENGTH_SHORT).show()
            return
        }

        val unitPrice = product.salePrice ?: 85.0
        val existingIndex = cartItems.indexOfFirst { it.productId == product.id && it.itemType == "purchase" }

        if (existingIndex >= 0) {
            val item = cartItems[existingIndex]
            cartItems[existingIndex] = item.copy(quantity = item.quantity + 1)
        } else {
            cartItems.add(
                ClientOrderItem(
                    productId = product.id ?: "prod-1",
                    productName = product.name ?: "Purchase Fencing",
                    quantity = 1,
                    unitPrice = unitPrice,
                    itemType = "purchase"
                )
            )
        }

        updateCartBadge()
        Toast.makeText(requireContext(), "Added Purchase: ${product.name}", Toast.LENGTH_SHORT).show()
    }

    private fun updateCartBadge() {
        val totalQty = cartItems.sumOf { it.quantity }
        binding.btnViewCart.text = "Purchase Cart ($totalQty)"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class PurchaseProductAdapter(
        private val items: List<ClientProduct>,
        private val onBuy: (ClientProduct) -> Unit
    ) : RecyclerView.Adapter<PurchaseProductAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivImage: ImageView = view.findViewById(R.id.ivProductImage)
            val tvName: TextView = view.findViewById(R.id.tvProductName)
            val tvStock: TextView = view.findViewById(R.id.tvStockBadge)
            val tvDesc: TextView = view.findViewById(R.id.tvProductDescription)
            val tvRentalPrice: TextView = view.findViewById(R.id.tvRentalPrice)
            val tvSalePrice: TextView = view.findViewById(R.id.tvSalePrice)
            val tvRentalAvail: TextView = view.findViewById(R.id.tvRentalAvailability)
            val btnAddRental: Button = view.findViewById(R.id.btnAddRental)
            val btnAddPurchase: Button = view.findViewById(R.id.btnAddPurchase)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.ivImage.load(item.image.orEmpty().ifEmpty { null }) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_gallery)
            }

            holder.tvName.text = item.name ?: "Purchase Product"
            val stock = item.inStock ?: 0
            holder.tvStock.text = if (stock > 0) "In Stock ($stock)" else "Out of Stock"
            holder.tvDesc.text = item.description ?: ""
            holder.tvSalePrice.text = "Sale Price: $${String.format("%.2f", item.salePrice ?: 0.0)}"
            holder.tvRentalPrice.visibility = View.GONE
            holder.tvRentalAvail.text = "✓ Available for Outright Purchase"

            holder.btnAddRental.visibility = View.GONE
            holder.btnAddPurchase.text = "+ Buy Outright"

            if (stock <= 0) {
                holder.btnAddPurchase.isEnabled = false
                holder.btnAddPurchase.alpha = 0.4f
            } else {
                holder.btnAddPurchase.isEnabled = true
                holder.btnAddPurchase.alpha = 1.0f
                holder.btnAddPurchase.setOnClickListener { onBuy(item) }
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
