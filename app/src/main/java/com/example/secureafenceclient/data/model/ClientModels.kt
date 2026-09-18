package com.example.secureafenceclient.data.model

import com.google.gson.annotations.SerializedName

data class CustomerProfile(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("company") val company: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("role") val role: String? = "customer",
    @SerializedName("is_taxable") val isTaxable: Boolean? = true,
    @SerializedName("business_address") val businessAddress: String? = null
)

data class Jobsite(
    @SerializedName("id") val id: String? = null,
    @SerializedName("customer_id") val customerId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("contact_name") val contactName: String? = null,
    @SerializedName("contact_phone") val contactPhone: String? = null,
    @SerializedName("special_instructions") val specialInstructions: String? = null,
    @SerializedName("delivery_distance_miles") val deliveryDistanceMiles: Double? = 0.0
)

data class ClientProduct(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("category") val category: String? = "sales",
    @SerializedName("type") val type: String? = "panel",
    @SerializedName("sale_price") val salePrice: Double? = 0.0,
    @SerializedName("rental_price_monthly") val rentalPriceMonthly: Double? = 0.0,
    @SerializedName("in_stock") val inStock: Int? = 0,
    @SerializedName("rented_count") val rentedCount: Int? = 0,
    @SerializedName("description") val description: String? = "",
    @SerializedName("image") val image: String? = "",
    @SerializedName("specs") val specs: String? = "",
    @SerializedName("suspended") val suspended: Boolean? = false,
    @SerializedName("is_rental") val isRental: Boolean? = true,
    @SerializedName("is_purchase") val isPurchase: Boolean? = true
)

data class ClientOrderItem(
    @SerializedName("product_id") val productId: String? = null,
    @SerializedName("product_name") val productName: String? = null,
    @SerializedName("quantity") val quantity: Int = 1,
    @SerializedName("unit_price") val unitPrice: Double = 0.0,
    @SerializedName("item_type") val itemType: String = "purchase"
)

data class ClientOrder(
    @SerializedName("id") val id: String? = null,
    @SerializedName("customer_id") val customerId: String? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("customer_company") val customerCompany: String? = null,
    @SerializedName("customer_email") val customerEmail: String? = null,
    @SerializedName("customer_phone") val customerPhone: String? = null,
    @SerializedName("order_type") val orderType: String? = "sale",
    @SerializedName("items") val items: List<ClientOrderItem>? = emptyList(),
    @SerializedName("subtotal") val subtotal: Double? = 0.0,
    @SerializedName("delivery_fee") val deliveryFee: Double? = 0.0,
    @SerializedName("tax") val tax: Double? = 0.0,
    @SerializedName("total_amount") val totalAmount: Double? = 0.0,
    @SerializedName("status") val status: String? = "Processing",
    @SerializedName("delivery_address") val deliveryAddress: String? = null,
    @SerializedName("jobsite_contact") val jobsiteContact: String? = null,
    @SerializedName("delivery_date") val deliveryDate: String? = null,
    @SerializedName("payment_status") val paymentStatus: String? = "Unpaid",
    @SerializedName("payment_method") val paymentMethod: String? = "Card",
    @SerializedName("is_taxable") val isTaxable: Boolean? = true,
    @SerializedName("discount_amount") val discountAmount: Double? = 0.0,
    @SerializedName("created_at") val createdAt: String? = null
)

data class ClientRental(
    @SerializedName("id") val id: String? = null,
    @SerializedName("order_id") val orderId: String? = null,
    @SerializedName("customer_id") val customerId: String? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("customer_company") val customerCompany: String? = null,
    @SerializedName("customer_email") val customerEmail: String? = null,
    @SerializedName("customer_phone") val customerPhone: String? = null,
    @SerializedName("jobsite_address") val jobsiteAddress: String? = null,
    @SerializedName("jobsite_contact") val jobsiteContact: String? = null,
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null,
    @SerializedName("monthly_rate_total") val monthlyRateTotal: Double? = 0.0,
    @SerializedName("status") val status: String? = "Active",
    @SerializedName("items") val items: List<ClientOrderItem>? = emptyList(),
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class ClientShipment(
    @SerializedName("id") val id: String? = null,
    @SerializedName("order_id") val orderId: String? = null,
    @SerializedName("type") val type: String? = "Delivery",
    @SerializedName("driver_name") val driverName: String? = "Assigned Dispatcher",
    @SerializedName("dispatch_date") val dispatchDate: String? = null,
    @SerializedName("status") val status: String? = "Scheduled",
    @SerializedName("destination") val destination: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("eta") val eta: String? = null,
    @SerializedName("delivery_photos") val deliveryPhotos: List<String>? = emptyList(),
    @SerializedName("delivered_items") val deliveredItems: List<ClientOrderItem>? = emptyList(),
    @SerializedName("created_at") val createdAt: String? = null
)

data class ClientInvoice(
    @SerializedName("id") val id: String? = null,
    @SerializedName("order_id") val orderId: String? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("amount") val amount: Double? = 0.0,
    @SerializedName("status") val status: String? = "unpaid",
    @SerializedName("created_at") val createdAt: String? = null
)

data class StripePaymentSheetParams(
    val clientSecret: String,
    val ephemeralKey: String,
    val customerId: String,
    val publishableKey: String
)
