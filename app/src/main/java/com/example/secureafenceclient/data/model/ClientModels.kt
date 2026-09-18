package com.example.secureafenceclient.data.model

import com.google.gson.annotations.SerializedName

data class CustomerProfile(
    @SerializedName("id", alternate = ["_id", "customerId"]) val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("company") val company: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("role") val role: String? = "customer",
    @SerializedName("is_taxable", alternate = ["isTaxable"]) val isTaxable: Boolean? = true,
    @SerializedName("business_address", alternate = ["businessAddress"]) val businessAddress: String? = null
)

data class Jobsite(
    @SerializedName("id", alternate = ["_id", "jobsiteId"]) val id: String? = null,
    @SerializedName("customer_id", alternate = ["customerId"]) val customerId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("contact_name", alternate = ["contactName"]) val contactName: String? = null,
    @SerializedName("contact_phone", alternate = ["contactPhone"]) val contactPhone: String? = null,
    @SerializedName("special_instructions", alternate = ["specialInstructions"]) val specialInstructions: String? = null,
    @SerializedName("delivery_distance_miles", alternate = ["deliveryDistanceMiles"]) val deliveryDistanceMiles: Double? = 0.0
)

data class ClientProduct(
    @SerializedName("id", alternate = ["_id", "productId"]) val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("category") val category: String? = "sales",
    @SerializedName("type") val type: String? = "panel",
    @SerializedName("sale_price", alternate = ["salePrice"]) val salePrice: Double? = 0.0,
    @SerializedName("rental_price_monthly", alternate = ["rentalPriceMonthly"]) val rentalPriceMonthly: Double? = 0.0,
    @SerializedName("in_stock", alternate = ["inStock"]) val inStock: Int? = 0,
    @SerializedName("rented_count", alternate = ["rentedCount"]) val rentedCount: Int? = 0,
    @SerializedName("description") val description: String? = "",
    @SerializedName("image") val image: String? = "",
    @SerializedName("specs") val specs: String? = "",
    @SerializedName("suspended") val suspended: Boolean? = false,
    @SerializedName("is_rental", alternate = ["isRental"]) val isRental: Boolean? = true,
    @SerializedName("is_purchase", alternate = ["isPurchase"]) val isPurchase: Boolean? = true
)

data class ClientOrderItem(
    @SerializedName("product_id", alternate = ["productId", "id", "_id"]) val productId: String? = null,
    @SerializedName("product_name", alternate = ["productName", "name"]) val productName: String? = null,
    @SerializedName("quantity") val quantity: Int = 1,
    @SerializedName("unit_price", alternate = ["unitPrice", "price"]) val unitPrice: Double = 0.0,
    @SerializedName("item_type", alternate = ["itemType"]) val itemType: String = "purchase"
)

data class ClientOrder(
    @SerializedName("id", alternate = ["_id", "orderId"]) val id: String? = null,
    @SerializedName("customer_id", alternate = ["customerId"]) val customerId: String? = null,
    @SerializedName("customer_name", alternate = ["customerName"]) val customerName: String? = null,
    @SerializedName("customer_company", alternate = ["customerCompany"]) val customerCompany: String? = null,
    @SerializedName("customer_email", alternate = ["customerEmail"]) val customerEmail: String? = null,
    @SerializedName("customer_phone", alternate = ["customerPhone"]) val customerPhone: String? = null,
    @SerializedName("order_type", alternate = ["orderType"]) val orderType: String? = "sale",
    @SerializedName("items") val items: List<ClientOrderItem>? = emptyList(),
    @SerializedName("subtotal") val subtotal: Double? = 0.0,
    @SerializedName("delivery_fee", alternate = ["deliveryFee"]) val deliveryFee: Double? = 0.0,
    @SerializedName("tax") val tax: Double? = 0.0,
    @SerializedName("total_amount", alternate = ["totalAmount"]) val totalAmount: Double? = 0.0,
    @SerializedName("status") val status: String? = "Processing",
    @SerializedName("delivery_address", alternate = ["deliveryAddress"]) val deliveryAddress: String? = null,
    @SerializedName("jobsite_contact", alternate = ["jobsiteContact"]) val jobsiteContact: String? = null,
    @SerializedName("delivery_date", alternate = ["deliveryDate"]) val deliveryDate: String? = null,
    @SerializedName("payment_status", alternate = ["paymentStatus"]) val paymentStatus: String? = "Unpaid",
    @SerializedName("payment_method", alternate = ["paymentMethod"]) val paymentMethod: String? = "Card",
    @SerializedName("is_taxable", alternate = ["isTaxable"]) val isTaxable: Boolean? = true,
    @SerializedName("discount_amount", alternate = ["discountAmount"]) val discountAmount: Double? = 0.0,
    @SerializedName("created_at", alternate = ["createdAt"]) val createdAt: String? = null
)

data class ClientRental(
    @SerializedName("id", alternate = ["_id", "rentalId"]) val id: String? = null,
    @SerializedName("order_id", alternate = ["orderId"]) val orderId: String? = null,
    @SerializedName("customer_id", alternate = ["customerId"]) val customerId: String? = null,
    @SerializedName("customer_name", alternate = ["customerName"]) val customerName: String? = null,
    @SerializedName("customer_company", alternate = ["customerCompany"]) val customerCompany: String? = null,
    @SerializedName("customer_email", alternate = ["customerEmail"]) val customerEmail: String? = null,
    @SerializedName("customer_phone", alternate = ["customerPhone"]) val customerPhone: String? = null,
    @SerializedName("jobsite_address", alternate = ["jobsiteAddress"]) val jobsiteAddress: String? = null,
    @SerializedName("jobsite_contact", alternate = ["jobsiteContact"]) val jobsiteContact: String? = null,
    @SerializedName("start_date", alternate = ["startDate"]) val startDate: String? = null,
    @SerializedName("end_date", alternate = ["endDate"]) val endDate: String? = null,
    @SerializedName("monthly_rate_total", alternate = ["monthlyRateTotal"]) val monthlyRateTotal: Double? = 0.0,
    @SerializedName("status") val status: String? = "Active",
    @SerializedName("items") val items: List<ClientOrderItem>? = emptyList(),
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("created_at", alternate = ["createdAt"]) val createdAt: String? = null
)

data class ClientShipment(
    @SerializedName("id", alternate = ["_id", "shipmentId"]) val id: String? = null,
    @SerializedName("order_id", alternate = ["orderId"]) val orderId: String? = null,
    @SerializedName("type") val type: String? = "Delivery",
    @SerializedName("driver_name", alternate = ["driverName"]) val driverName: String? = "Assigned Dispatcher",
    @SerializedName("dispatch_date", alternate = ["dispatchDate"]) val dispatchDate: String? = null,
    @SerializedName("status") val status: String? = "Scheduled",
    @SerializedName("destination") val destination: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("eta") val eta: String? = null,
    @SerializedName("delivery_photos", alternate = ["deliveryPhotos"]) val deliveryPhotos: List<String>? = emptyList(),
    @SerializedName("delivered_items", alternate = ["deliveredItems"]) val deliveredItems: List<ClientOrderItem>? = emptyList(),
    @SerializedName("created_at", alternate = ["createdAt"]) val createdAt: String? = null
)

data class ClientInvoice(
    @SerializedName("id", alternate = ["_id", "invoiceId"]) val id: String? = null,
    @SerializedName("order_id", alternate = ["orderId"]) val orderId: String? = null,
    @SerializedName("customer_name", alternate = ["customerName"]) val customerName: String? = null,
    @SerializedName("amount") val amount: Double? = 0.0,
    @SerializedName("status") val status: String? = "unpaid",
    @SerializedName("created_at", alternate = ["createdAt"]) val createdAt: String? = null
)

data class StripePaymentSheetParams(
    val clientSecret: String,
    val ephemeralKey: String,
    val customerId: String,
    val publishableKey: String
)

data class StripePaymentMethod(
    @SerializedName("id") val id: String,
    @SerializedName("brand") val brand: String? = "Visa",
    @SerializedName("last4") val last4: String? = "4242",
    @SerializedName("exp_month") val expMonth: Int? = 12,
    @SerializedName("exp_year") val expYear: Int? = 2026,
    @SerializedName("is_default") val isDefault: Boolean = false
)

data class StripeSetupIntentParams(
    val clientSecret: String,
    val ephemeralKey: String,
    val customerId: String,
    val publishableKey: String
)
