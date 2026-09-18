package com.example.secureafenceclient.data.network

import com.example.secureafenceclient.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ClientApiService {

    // --- Direct Supabase PostgREST Table Endpoints ---

    @POST("rest/v1/customers")
    suspend fun registerCustomer(@Body customer: CustomerProfile): Response<List<CustomerProfile>>

    @POST("rest/v1/customers")
    suspend fun createCustomerAdmin(@Body customer: CustomerProfile): Response<List<CustomerProfile>>

    @GET("rest/v1/customers")
    suspend fun getCustomerProfile(@Query("email") emailFilter: String): Response<List<CustomerProfile>>

    @PATCH("rest/v1/customers")
    suspend fun updateCustomerProfile(@Query("id") idFilter: String, @Body profile: CustomerProfile): Response<List<CustomerProfile>>

    @GET("rest/v1/jobsites")
    suspend fun getJobsites(
        @Query("customer_id") customerId: String? = null,
        @Query("customer_email") customerEmail: String? = null
    ): Response<List<Jobsite>>

    @POST("rest/v1/jobsites")
    suspend fun createJobsite(@Body jobsite: Jobsite): Response<List<Jobsite>>

    @POST("rest/v1/jobsites")
    suspend fun createJobsiteAdmin(
        @Query("customer_id") customerId: String,
        @Body jobsite: Jobsite
    ): Response<List<Jobsite>>

    @GET("rest/v1/products")
    suspend fun getProducts(): Response<List<ClientProduct>>

    @GET("rest/v1/orders")
    suspend fun getOrders(@Query("customer_email") customerEmail: String? = null): Response<List<ClientOrder>>

    @POST("rest/v1/orders")
    suspend fun createOrder(@Body order: ClientOrder): Response<List<ClientOrder>>

    @GET("rest/v1/rentals")
    suspend fun getRentals(@Query("customer_email") customerEmail: String? = null): Response<List<ClientRental>>

    @PATCH("rest/v1/rentals")
    suspend fun extendRental(@Query("id") rentalIdFilter: String, @Body body: Map<String, String>): Response<List<ClientRental>>

    @GET("rest/v1/shipments")
    suspend fun getShipments(@Query("customer_email") customerEmail: String? = null): Response<List<ClientShipment>>

    @GET("rest/v1/invoices")
    suspend fun getInvoices(@Query("customer_email") customerEmail: String? = null): Response<List<ClientInvoice>>

    @PATCH("rest/v1/invoices")
    suspend fun payInvoice(@Query("id") invoiceIdFilter: String, @Body body: Map<String, String>): Response<List<Map<String, Any>>>

    // --- Stripe API Endpoints (via backend or direct) ---
    @POST("api/stripe/create-payment-sheet")
    suspend fun createPaymentSheet(@Body request: Map<String, Any>): Response<Map<String, String>>

    @GET("api/stripe/payment-methods")
    suspend fun getPaymentMethods(@Query("email") customerEmail: String): Response<List<StripePaymentMethod>>

    @DELETE("api/stripe/payment-methods/{id}")
    suspend fun detachPaymentMethod(@Path("id") methodId: String): Response<Map<String, Any>>

    @POST("api/stripe/create-setup-intent")
    suspend fun createSetupIntent(@Body request: Map<String, Any>): Response<Map<String, String>>
}
