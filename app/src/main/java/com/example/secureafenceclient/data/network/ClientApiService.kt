package com.example.secureafenceclient.data.network

import com.example.secureafenceclient.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ClientApiService {

    @POST("api/client/login")
    suspend fun loginCustomer(@Body credentials: Map<String, String>): Response<Map<String, Any>>

    @POST("api/client/register")
    suspend fun registerCustomer(@Body customer: CustomerProfile): Response<CustomerProfile>

    @GET("api/client/profile")
    suspend fun getCustomerProfile(@Query("email") email: String): Response<CustomerProfile>

    @PUT("api/client/profile/{id}")
    suspend fun updateCustomerProfile(@Path("id") id: String, @Body profile: CustomerProfile): Response<CustomerProfile>

    @GET("api/jobsites")
    suspend fun getJobsites(@Query("customer_id") customerId: String? = null): Response<List<Jobsite>>

    @POST("api/jobsites")
    suspend fun createJobsite(@Body jobsite: Jobsite): Response<Jobsite>

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

    @POST("api/stripe/create-payment-sheet")
    suspend fun createPaymentSheet(@Body request: Map<String, Any>): Response<Map<String, String>>
}
