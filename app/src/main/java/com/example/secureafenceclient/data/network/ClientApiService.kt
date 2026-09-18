package com.example.secureafenceclient.data.network

import com.example.secureafenceclient.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ClientApiService {

    // --- Backend REST Routes ---
    @POST("api/client/login")
    suspend fun loginCustomer(@Body credentials: Map<String, String>): Response<Map<String, Any>>

    @POST("api/client/register")
    suspend fun registerCustomer(@Body customer: CustomerProfile): Response<CustomerProfile>

    @POST("api/admin/customers")
    suspend fun createCustomerAdmin(@Body customer: CustomerProfile): Response<Map<String, Any>>

    @GET("api/client/profile")
    suspend fun getCustomerProfile(@Query("email") email: String): Response<CustomerProfile>

    @PUT("api/client/profile/{id}")
    suspend fun updateCustomerProfile(@Path("id") id: String, @Body profile: CustomerProfile): Response<CustomerProfile>

    @GET("api/jobsites")
    suspend fun getJobsites(@Query("customer_id") customerId: String? = null, @Query("customer_email") customerEmail: String? = null): Response<List<Jobsite>>

    @POST("api/jobsites")
    suspend fun createJobsite(@Body jobsite: Jobsite): Response<Jobsite>

    @POST("api/admin/customers/{customerId}/jobsites")
    suspend fun createJobsiteAdmin(
        @Path("customerId") customerId: String,
        @Body jobsite: Jobsite
    ): Response<Map<String, Any>>

    @GET("api/products")
    suspend fun getProducts(): Response<List<ClientProduct>>

    @GET("api/orders")
    suspend fun getOrders(@Query("customer_email") customerEmail: String? = null): Response<List<ClientOrder>>

    @POST("api/orders")
    suspend fun createOrder(@Body order: ClientOrder): Response<ClientOrder>

    @GET("api/rentals")
    suspend fun getRentals(@Query("customer_email") customerEmail: String? = null): Response<List<ClientRental>>

    @PUT("api/rentals/{id}/extend")
    suspend fun extendRental(@Path("id") rentalId: String, @Body body: Map<String, String>): Response<ClientRental>

    @GET("api/shipments")
    suspend fun getShipments(@Query("customer_email") customerEmail: String? = null): Response<List<ClientShipment>>

    @GET("api/invoices")
    suspend fun getInvoices(@Query("customer_email") customerEmail: String? = null): Response<List<ClientInvoice>>

    @POST("api/invoices/{id}/pay")
    suspend fun payInvoice(@Path("id") invoiceId: String, @Body body: Map<String, String>): Response<Map<String, Any>>

    // --- Stripe API Payment Sheet & Payment Methods ---
    @POST("api/stripe/create-payment-sheet")
    suspend fun createPaymentSheet(@Body request: Map<String, Any>): Response<Map<String, String>>

    @GET("api/stripe/payment-methods")
    suspend fun getPaymentMethods(@Query("email") customerEmail: String): Response<List<StripePaymentMethod>>

    @DELETE("api/stripe/payment-methods/{id}")
    suspend fun detachPaymentMethod(@Path("id") methodId: String): Response<Map<String, Any>>

    @POST("api/stripe/create-setup-intent")
    suspend fun createSetupIntent(@Body request: Map<String, Any>): Response<Map<String, String>>

    // --- Direct Supabase Table Access Routes (PostgREST) ---
    @POST("rest/v1/customers")
    suspend fun supabaseCreateCustomer(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") preferHeader: String = "return=representation",
        @Body customer: CustomerProfile
    ): Response<List<CustomerProfile>>

    @GET("rest/v1/customers")
    suspend fun supabaseGetCustomer(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("email") emailFilter: String
    ): Response<List<CustomerProfile>>

    @POST("rest/v1/jobsites")
    suspend fun supabaseCreateJobsite(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") preferHeader: String = "return=representation",
        @Body jobsite: Jobsite
    ): Response<List<Jobsite>>

    @GET("rest/v1/jobsites")
    suspend fun supabaseGetJobsites(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("customer_id") customerIdFilter: String? = null
    ): Response<List<Jobsite>>

    @GET("rest/v1/products")
    suspend fun supabaseGetProducts(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String
    ): Response<List<ClientProduct>>

    @POST("rest/v1/orders")
    suspend fun supabaseCreateOrder(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") preferHeader: String = "return=representation",
        @Body order: ClientOrder
    ): Response<List<ClientOrder>>

    @GET("rest/v1/orders")
    suspend fun supabaseGetOrders(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("customer_email") emailFilter: String? = null
    ): Response<List<ClientOrder>>

    @GET("rest/v1/rentals")
    suspend fun supabaseGetRentals(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("customer_email") emailFilter: String? = null
    ): Response<List<ClientRental>>

    @GET("rest/v1/shipments")
    suspend fun supabaseGetShipments(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String
    ): Response<List<ClientShipment>>

    @GET("rest/v1/invoices")
    suspend fun supabaseGetInvoices(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String
    ): Response<List<ClientInvoice>>
}
