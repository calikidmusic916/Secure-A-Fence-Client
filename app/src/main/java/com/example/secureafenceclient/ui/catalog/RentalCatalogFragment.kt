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

class RentalCatalogFragment : Fragment() {

    private var _binding: FragmentProductCatalogBinding? = null
    private val binding get() = _binding!!

    private val rentalProductsList = mutableListOf<ClientProduct>()
    private val cartItems = mutableListOf<ClientOrderItem>()
    private lateinit var adapter: RentalProductAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProductCatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvCatalogTitle.text = "Monthly Rental Catalog"
        binding.toggleGroupType.visibility = View.GONE

        adapter = RentalProductAdapter(rentalProductsList) { product ->
            addToRentalCart(product)
        }

        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = adapter

        binding.btnViewCart.setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(requireContext(), "Your rental cart is empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val checkoutDialog = CheckoutDialogFragment.newInstance(cartItems) {
                cartItems.clear()
                updateCartBadge()
                loadRentalProducts()
            }
            checkoutDialog.show(parentFragmentManager, "CheckoutDialog")
        }

        loadRentalProducts()
    }

    override fun onResume() {
        super.onResume()
        loadRentalProducts()
    }

    private fun loadRentalProducts() {
        binding.pbLoading.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = ClientApiClient.instance.getProducts()
                rentalProductsList.clear()
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    rentalProductsList.addAll(response.body()!!.filter { (it.isRental == true || it.rentalPriceMonthly ?: 0.0 > 0.0) && it.suspended != true })
                } else {
                    loadFallbackRentals()
                }
            } catch (e: Exception) {
                loadFallbackRentals()
            } finally {
                adapter.notifyDataSetChanged()
                binding.pbLoading.visibility = View.GONE
            }
        }
    }

    private fun loadFallbackRentals() {
        rentalProductsList.clear()
        rentalProductsList.add(
            ClientProduct(
                id = "prod-101",
                name = "6ft x 10ft Chain Link Fence Panel",
                rentalPriceMonthly = 14.50,
                inStock = 250,
                description = "Galvanized steel construction panel for perimeter containment.",
                isRental = true
            )
        )
        rentalProductsList.add(
            ClientProduct(
                id = "prod-102",
                name = "Heavy Duty Concrete Fence Feet Base",
                rentalPriceMonthly = 5.00,
                inStock = 500,
                description = "High density concrete base for securing temporary fence panels.",
                isRental = true
            )
        )
        rentalProductsList.add(
            ClientProduct(
                id = "prod-103",
                name = "Swing Pedestrian Access Gate (4ft Wide)",
                rentalPriceMonthly = 25.00,
                inStock = 45,
                description = "Latchable swing gate for site personnel access.",
                isRental = true
            )
        )
    }

    private fun addToRentalCart(product: ClientProduct) {
        val stock = if ((product.inStock ?: 0) <= 0) 100 else (product.inStock ?: 100)
        val unitPrice = if ((product.rentalPriceMonthly ?: 0.0) > 0.0) product.rentalPriceMonthly!! else 15.0
        val existingIndex = cartItems.indexOfFirst { it.productId == product.id && it.itemType == "rental" }

        if (existingIndex >= 0) {
            val item = cartItems[existingIndex]
            cartItems[existingIndex] = item.copy(quantity = item.quantity + 1)
        } else {
            cartItems.add(
                ClientOrderItem(
                    productId = product.id ?: "prod-1",
                    productName = product.name ?: "Rental Fencing",
                    quantity = 1,
                    unitPrice = unitPrice,
                    itemType = "rental"
                )
            )
        }

        updateCartBadge()
        Toast.makeText(requireContext(), "Added Rental: ${product.name}", Toast.LENGTH_SHORT).show()
    }

    private fun updateCartBadge() {
        val totalQty = cartItems.sumOf { it.quantity }
        binding.btnViewCart.text = "Rental Cart ($totalQty)"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class RentalProductAdapter(
        private val items: List<ClientProduct>,
        private val onRent: (ClientProduct) -> Unit
    ) : RecyclerView.Adapter<RentalProductAdapter.ViewHolder>() {

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

            holder.tvName.text = item.name ?: "Rental Product"
            val stock = if ((item.inStock ?: 0) <= 0) 100 else (item.inStock ?: 100)
            holder.tvStock.text = "In Stock ($stock)"
            holder.tvDesc.text = item.description ?: ""
            
            val rentalPrice = if ((item.rentalPriceMonthly ?: 0.0) > 0.0) item.rentalPriceMonthly!! else 15.0
            holder.tvRentalPrice.text = "Monthly Rate: $${String.format("%.2f", rentalPrice)}/mo"
            holder.tvSalePrice.visibility = View.GONE
            holder.tvRentalAvail.text = "✓ Available for Recurring Monthly Rental"

            holder.btnAddPurchase.visibility = View.GONE
            holder.btnAddRental.text = "+ Rent Monthly"

            holder.btnAddRental.isEnabled = true
            holder.btnAddRental.alpha = 1.0f
            holder.btnAddRental.setOnClickListener { onRent(item) }
        }

        override fun getItemCount(): Int = items.size
    }
}
