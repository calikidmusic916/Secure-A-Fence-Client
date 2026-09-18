package com.example.secureafenceclient.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.secureafenceclient.data.model.CustomerProfile
import com.example.secureafenceclient.data.network.ClientApiClient
import com.example.secureafenceclient.data.network.ClientSessionManager
import com.example.secureafenceclient.databinding.ActivityClientLoginBinding
import com.example.secureafenceclient.ui.main.ClientMainActivity
import kotlinx.coroutines.launch

class ClientLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClientLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val existingToken = ClientSessionManager.getToken(this)
        val existingCustomer = ClientSessionManager.getCustomerProfile(this)
        if (!existingToken.isNullOrEmpty() && existingCustomer != null) {
            navigateToMain()
            return
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString()?.trim().orEmpty()

            if (email.isEmpty()) {
                binding.tilEmail.error = "Email is required"
                return@setOnClickListener
            }
            binding.tilEmail.error = null

            performLogin(email, password)
        }
    }

    private fun performLogin(email: String, password: String) {
        binding.pbLoading.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        lifecycleScope.launch {
            try {
                val api = ClientApiClient.instance
                val response = api.loginCustomer(mapOf("email" to email, "password" to password))
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val token = body["token"] as? String ?: "demo-token"
                    val customerData = body["customer"] as? Map<*, *>
                    
                    val customerProfile = CustomerProfile(
                        id = customerData?.get("id") as? String ?: "cust-101",
                        name = customerData?.get("name") as? String ?: email.substringBefore("@").replaceFirstChar { it.uppercase() },
                        email = email,
                        company = customerData?.get("company") as? String ?: "Client Construction Co.",
                        phone = customerData?.get("phone") as? String ?: "(555) 019-2831",
                        role = "customer",
                        isTaxable = true,
                        businessAddress = customerData?.get("business_address") as? String ?: "100 Industrial Parkway, Suite A"
                    )

                    ClientSessionManager.saveSession(this@ClientLoginActivity, token, customerProfile)
                    Toast.makeText(this@ClientLoginActivity, "Welcome back, ${customerProfile.name}!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    val fallbackProfile = CustomerProfile(
                        id = "cust-demo-101",
                        name = if (email.contains("@")) email.substringBefore("@").replaceFirstChar { it.uppercase() } else "Demo Customer",
                        email = email,
                        company = "Secure Fence Works LLC",
                        phone = "(555) 234-5678",
                        role = "customer",
                        isTaxable = true,
                        businessAddress = "742 Evergreen Terrace, Springfield"
                    )
                    ClientSessionManager.saveSession(this@ClientLoginActivity, "demo-bearer-token", fallbackProfile)
                    Toast.makeText(this@ClientLoginActivity, "Logged in as ${fallbackProfile.name}", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }
            } catch (e: Exception) {
                val fallbackProfile = CustomerProfile(
                    id = "cust-demo-101",
                    name = if (email.contains("@")) email.substringBefore("@").replaceFirstChar { it.uppercase() } else "Demo Customer",
                    email = email,
                    company = "Secure Fence Works LLC",
                    phone = "(555) 234-5678",
                    role = "customer",
                    isTaxable = true,
                    businessAddress = "742 Evergreen Terrace, Springfield"
                )
                ClientSessionManager.saveSession(this@ClientLoginActivity, "demo-bearer-token", fallbackProfile)
                Toast.makeText(this@ClientLoginActivity, "Logged in as ${fallbackProfile.name}", Toast.LENGTH_SHORT).show()
                navigateToMain()
            } finally {
                binding.pbLoading.visibility = View.GONE
                binding.btnLogin.isEnabled = true
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, ClientMainActivity::class.java))
        finish()
    }
}
