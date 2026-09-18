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

class ProductCatalogFragment : Fragment() {

    private var _binding: FragmentProductCatalogBinding? = null
    private val binding get() = _binding!!

    private val allProductsList = mutableListOf<ClientProduct>()
    private val displayedProductsList = mutableListOf<ClientProduct>()
    private val cartItems = mutableListOf<ClientOrderItem>()

    private lateinit var adapter: ProductAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProductCatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ProductAdapter(
            items = displayedProductsList,
            onAddRental = { product -> addToCart(product, "rental") },
            onAddPurchase = { product -> addToCart(product, "purchase") }
        )

        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = adapter

        binding.toggleGroupType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnFilterAll -> filterProducts("all")
                    R.id.btnFilterRentals -> filterProducts("rentals")
                    R.id.btnFilterPurchases -> filterProducts("purchases")
                }
            }
        }

        binding.btnViewCart.setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(requireContext(), "Your cart is empty. Add products to order!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val checkoutDialog = CheckoutDialogFragment.newInstance(cartItems) {
                cartItems.clear()
                updateCartBadge()
                loadProducts()
            }
            checkoutDialog.show(parentFragmentManager, "CheckoutDialog")
        }

        loadProducts()
    }

    override fun onResume() {
        super.onResume()
        loadProducts()
    }

    private fun addToCart(product: ClientProduct, itemType: String) {
        val stock = product.inStock ?: 0
        if (stock <= 0) {
            Toast.makeText(requireContext(), "${product.name} is currently out of stock.", Toast.LENGTH_SHORT).show()
            return
        }

        if (itemType == "rental" && product.isRental == false) {
            Toast.makeText(requireContext(), "${product.name} is not available for rental.", Toast.LENGTH_SHORT).show()
            return
        }

        if (itemType == "purchase" && product.isPurchase == false) {
            Toast.makeText(requireContext(), "${product.name} is not available for purchase.", Toast.LENGTH_SHORT).show()
            return
        }

        val unitPrice = if (itemType == "rental") (product.rentalPriceMonthly ?: 15.0) else (product.salePrice ?: 85.0)
        val existingIndex = cartItems.indexOfFirst { it.productId == product.id && it.itemType == itemType }

        if (existingIndex >= 0) {
            val item = cartItems[existingIndex]
            if (item.quantity >= stock) {
                Toast.makeText(requireContext(), "Cannot add more. Reached max available stock ($stock).", Toast.LENGTH_SHORT).show()
                return
            }
            cartItems[existingIndex] = item.copy(quantity = item.quantity + 1)
        } else {
            cartItems.add(
                ClientOrderItem(
                    productId = product.id ?: "prod-1",
                    productName = product.name ?: "Fence Product",
                    quantity = 1,
                    unitPrice = unitPrice,
                    itemType = itemType
                )
            )
        }

        updateCartBadge()
        val typeLabel = if (itemType == "rental") "Rental" else "Purchase"
        Toast.makeText(requireContext(), "Added $typeLabel: ${product.name}", Toast.LENGTH_SHORT).show()
    }

    private fun updateCartBadge() {
        val totalQty = cartItems.sumOf { it.quantity }
        binding.btnViewCart.text = "Cart ($totalQty)"
    }

    private fun filterProducts(filterMode: String) {
        displayedProductsList.clear()
        when (filterMode) {
            "rentals" -> displayedProductsList.addAll(allProductsList.filter { it.isRental == true && it.suspended != true })
            "purchases" -> displayedProductsList.addAll(allProductsList.filter { it.isPurchase == true && it.suspended != true })
            else -> displayedProductsList.addAll(allProductsList.filter { it.suspended != true })
        }
        adapter.notifyDataSetChanged()
    }

    private fun loadProducts() {
        binding.pbLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = ClientApiClient.instance.getProducts()
                allProductsList.clear()
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    allProductsList.addAll(response.body()!!)
                    filterProducts("all")
                }
            } catch (e: Exception) {
                // Keep empty or existing
            } finally {
                binding.pbLoading.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class ProductAdapter(
        private val items: List<ClientProduct>,
        private val onAddRental: (ClientProduct) -> Unit,
        private val onAddPurchase: (ClientProduct) -> Unit
    ) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

        class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product_card, parent, false)
            return ProductViewHolder(view)
        }

        override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
            val item = items[position]
            holder.ivImage.load(item.image.orEmpty().ifEmpty { null }) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_gallery)
            }

            holder.tvName.text = item.name ?: "Fence Product"
            val stock = item.inStock ?: 0
            holder.tvStock.text = if (stock > 0) "In Stock ($stock)" else "Out of Stock"
            holder.tvStock.setTextColor(if (stock > 0) android.graphics.Color.parseColor("#2E7D32") else android.graphics.Color.parseColor("#C62828"))

            holder.tvDesc.text = item.description ?: ""
            
            val rentalPrice = item.rentalPriceMonthly ?: 0.0
            val salePrice = item.salePrice ?: 0.0

            holder.tvRentalPrice.text = "Rental: $${String.format("%.2f", rentalPrice)}/mo"
            holder.tvSalePrice.text = "Buy: $${String.format("%.2f", salePrice)}"

            val isRentalAllowed = item.isRental == true
            val isPurchaseAllowed = item.isPurchase == true

            if (isRentalAllowed) {
                holder.tvRentalAvail.visibility = View.VISIBLE
                holder.tvRentalAvail.text = "✓ Available for Monthly Rental"
            } else {
                holder.tvRentalAvail.visibility = View.GONE
            }

            // Rental Button
            if (stock <= 0 || !isRentalAllowed) {
                holder.btnAddRental.isEnabled = false
                holder.btnAddRental.alpha = 0.4f
            } else {
                holder.btnAddRental.isEnabled = true
                holder.btnAddRental.alpha = 1.0f
                holder.btnAddRental.setOnClickListener { onAddRental(item) }
            }

            // Purchase Button
            if (stock <= 0 || !isPurchaseAllowed) {
                holder.btnAddPurchase.isEnabled = false
                holder.btnAddPurchase.alpha = 0.4f
            } else {
                holder.btnAddPurchase.isEnabled = true
                holder.btnAddPurchase.alpha = 1.0f
                holder.btnAddPurchase.setOnClickListener { onAddPurchase(item) }
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
