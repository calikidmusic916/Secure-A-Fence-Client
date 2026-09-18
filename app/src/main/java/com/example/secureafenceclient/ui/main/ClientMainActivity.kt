package com.example.secureafenceclient.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.secureafenceclient.R
import com.example.secureafenceclient.databinding.ActivityClientMainBinding
import com.example.secureafenceclient.ui.catalog.ProductCatalogFragment
import com.example.secureafenceclient.ui.deliveries.DeliveryTrackingFragment
import com.example.secureafenceclient.ui.orders.CustomerOrdersAndRentalsFragment
import com.example.secureafenceclient.ui.payments.InvoicesAndPaymentsFragment
import com.example.secureafenceclient.ui.profile.ProfileAndJobsitesFragment

class ClientMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClientMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            loadFragment(ProfileAndJobsitesFragment())
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_profile -> ProfileAndJobsitesFragment()
                R.id.nav_catalog -> ProductCatalogFragment()
                R.id.nav_orders -> CustomerOrdersAndRentalsFragment()
                R.id.nav_deliveries -> DeliveryTrackingFragment()
                R.id.nav_invoices -> InvoicesAndPaymentsFragment()
                else -> ProfileAndJobsitesFragment()
            }
            loadFragment(fragment)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
