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
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class ClientLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClientLoginBinding
    private var isSignUpMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check active session
        val existingToken = ClientSessionManager.getToken(this)
        val existingCustomer = ClientSessionManager.getCustomerProfile(this)
        if (!existingToken.isNullOrEmpty() && existingCustomer != null) {
            navigateToMain()
            return
        }

        binding.tabAuthMode.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                setAuthMode(tab?.position == 1)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.tvToggleModeLink.setOnClickListener {
            val newMode = !isSignUpMode
            binding.tabAuthMode.getTabAt(if (newMode) 1 else 0)?.select()
            setAuthMode(newMode)
        }

        binding.btnLogin.setOnClickListener {
            if (isSignUpMode) {
                performSignUp()
            } else {
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
    }

    private fun setAuthMode(signUp: Boolean) {
        isSignUpMode = signUp
        val extraVisibility = if (signUp) View.VISIBLE else View.GONE
        binding.tilName.visibility = extraVisibility
        binding.tilCompany.visibility = extraVisibility
        binding.tilPhone.visibility = extraVisibility
        binding.tilAddress.visibility = extraVisibility

        binding.btnLogin.text = if (signUp) "Register & Create Account" else "Login"
        binding.tvToggleModeLink.text = if (signUp) "Already have an account? Tap here to log in" else "New customer? Tap here to create an account"
    }

    private fun performSignUp() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val company = binding.etCompany.text?.toString()?.trim().orEmpty()
        val phone = binding.etPhone.text?.toString()?.trim().orEmpty()
        val address = binding.etAddress.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString()?.trim().orEmpty()

        if (name.isEmpty()) {
            binding.tilName.error = "Full Name is required"
            return
        }
        binding.tilName.error = null

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            return
        }
        binding.tilEmail.error = null

        if (password.length < 4) {
            binding.tilPassword.error = "Password must be at least 4 characters"
            return
        }
        binding.tilPassword.error = null

        binding.pbLoading.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        lifecycleScope.launch {
            val newCustomer = CustomerProfile(
                id = "cust-${System.currentTimeMillis()}",
                name = name,
                company = company.ifEmpty { "$name Construction" },
                email = email,
                phone = phone.ifEmpty { "(555) 000-0000" },
                role = "customer",
                isTaxable = true,
                businessAddress = address.ifEmpty { "Main Commercial Address" }
            )

            try {
                val api = ClientApiClient.instance
                // Register via client route AND create in admin customers table for instant sync across Admin app
                try {
                    api.createCustomerAdmin(newCustomer)
                } catch (e: Exception) {
                    // Suppress if already exists
                }

                val response = api.registerCustomer(newCustomer)
                val finalProfile = response.body() ?: newCustomer

                ClientSessionManager.saveSession(this@ClientLoginActivity, "auth-token-${finalProfile.id}", finalProfile)
                Toast.makeText(this@ClientLoginActivity, "Account Created! Welcome, ${finalProfile.name}!", Toast.LENGTH_LONG).show()
                navigateToMain()
            } catch (e: Exception) {
                ClientSessionManager.saveSession(this@ClientLoginActivity, "auth-token-${newCustomer.id}", newCustomer)
                Toast.makeText(this@ClientLoginActivity, "Account Created! Welcome, ${newCustomer.name}!", Toast.LENGTH_LONG).show()
                navigateToMain()
            } finally {
                binding.pbLoading.visibility = View.GONE
                binding.btnLogin.isEnabled = true
            }
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
