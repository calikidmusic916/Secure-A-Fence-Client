package com.example.secureafenceclient.ui.catalog

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
            }
            checkoutDialog.show(parentFragmentManager, "CheckoutDialog")
        }

        loadProducts()
    }

    private fun addToCart(product: ClientProduct, itemType: String) {
        val unitPrice = if (itemType == "rental") (product.rentalPriceMonthly ?: 15.0) else (product.salePrice ?: 85.0)
        val existingIndex = cartItems.indexOfFirst { it.productId == product.id && it.itemType == itemType }

        if (existingIndex >= 0) {
            val item = cartItems[existingIndex]
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
            "rentals" -> displayedProductsList.addAll(allProductsList.filter { it.isRental == true })
            "purchases" -> displayedProductsList.addAll(allProductsList.filter { it.isPurchase == true })
            else -> displayedProductsList.addAll(allProductsList)
        }
        adapter.notifyDataSetChanged()
    }

    private fun loadProducts() {
        binding.pbLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = ClientApiClient.instance.getProducts()
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    allProductsList.clear()
                    allProductsList.addAll(response.body()!!)
                    filterProducts("all")
                } else {
                    loadFallbackProducts()
                }
            } catch (e: Exception) {
                loadFallbackProducts()
            } finally {
                binding.pbLoading.visibility = View.GONE
            }
        }
    }

    private fun loadFallbackProducts() {
        allProductsList.clear()
        allProductsList.add(
            ClientProduct(
                id = "prod-101",
                name = "6ft x 10ft Chain Link Fence Panel",
                type = "panel",
                salePrice = 85.00,
                rentalPriceMonthly = 14.50,
                inStock = 250,
                description = "Galvanized steel construction panel for perimeter containment and site safety.",
                isRental = true,
                isPurchase = true
            )
        )
        allProductsList.add(
            ClientProduct(
                id = "prod-102",
                name = "Heavy Duty Concrete Fence Feet Base",
                type = "base",
                salePrice = 32.00,
                rentalPriceMonthly = 5.00,
                inStock = 500,
                description = "High density concrete base for securing temporary fence panels in wind conditions.",
                isRental = true,
                isPurchase = true
            )
        )
        allProductsList.add(
            ClientProduct(
                id = "prod-103",
                name = "Swing Pedestrian Access Gate (4ft Wide)",
                type = "gate",
                salePrice = 145.00,
                rentalPriceMonthly = 25.00,
                inStock = 45,
                description = "Latchable swing gate for site personnel access.",
                isRental = true,
                isPurchase = true
            )
        )
        allProductsList.add(
            ClientProduct(
                id = "prod-104",
                name = "Privacy Windscreen Mesh Roll (50ft)",
                type = "accessory",
                salePrice = 65.00,
                rentalPriceMonthly = 12.00,
                inStock = 80,
                description = "High opacity green privacy netting with reinforced grommets.",
                isRental = true,
                isPurchase = true
            )
        )
        filterProducts("all")
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
            val tvName: TextView = view.findViewById(R.id.tvProductName)
            val tvStock: TextView = view.findViewById(R.id.tvStockBadge)
            val tvDesc: TextView = view.findViewById(R.id.tvProductDescription)
            val tvRentalPrice: TextView = view.findViewById(R.id.tvRentalPrice)
            val tvSalePrice: TextView = view.findViewById(R.id.tvSalePrice)
            val btnAddRental: Button = view.findViewById(R.id.btnAddRental)
            val btnAddPurchase: Button = view.findViewById(R.id.btnAddPurchase)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product_card, parent, false)
            return ProductViewHolder(view)
        }

        override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = item.name ?: "Fence Product"
            holder.tvStock.text = "In Stock (${item.inStock ?: 0})"
            holder.tvDesc.text = item.description ?: ""
            holder.tvRentalPrice.text = "Rental: $${String.format("%.2f", item.rentalPriceMonthly ?: 0.0)}/mo"
            holder.tvSalePrice.text = "Buy: $${String.format("%.2f", item.salePrice ?: 0.0)}"

            holder.btnAddRental.setOnClickListener { onAddRental(item) }
            holder.btnAddPurchase.setOnClickListener { onAddPurchase(item) }
        }

        override fun getItemCount(): Int = items.size
    }
}
