package com.example.secureafenceclient.data.network

import com.example.secureafenceclient.data.model.StripePaymentSheetParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ClientStripeApiClient {

    suspend fun createPaymentSheet(amount: Double, customerEmail: String, description: String): Result<StripePaymentSheetParams> {
        return withContext(Dispatchers.IO) {
            try {
                val api = ClientApiClient.instance
                val requestMap = mapOf(
                    "amount" to (amount * 100).toInt(),
                    "currency" to "usd",
                    "customer_email" to customerEmail,
                    "description" to description
                )
                val response = api.createPaymentSheet(requestMap)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val clientSecret = body["paymentIntent"] ?: body["clientSecret"] ?: ""
                    val ephemeralKey = body["ephemeralKey"] ?: ""
                    val customerId = body["customer"] ?: ""
                    val publishableKey = body["publishableKey"] ?: "pk_test_51UGs9qERCsfh1i1Di6n7HvRCVbVwdt3Rh6CSGln2eVjUGCwSdXmRY3Af88zHFm5KOPKY7Smi1ZRZD16vmjKZWvhZ00fL302Or4"

                    Result.success(
                        StripePaymentSheetParams(
                            clientSecret = clientSecret,
                            ephemeralKey = ephemeralKey,
                            customerId = customerId,
                            publishableKey = publishableKey
                        )
                    )
                } else {
                    Result.failure(Exception("Stripe creation failed: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
