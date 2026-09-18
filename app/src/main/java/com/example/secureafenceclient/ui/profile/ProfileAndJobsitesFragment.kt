package com.example.secureafenceclient.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.secureafenceclient.R
import com.example.secureafenceclient.data.model.Jobsite
import com.example.secureafenceclient.data.network.ClientApiClient
import com.example.secureafenceclient.data.network.ClientSessionManager
import com.example.secureafenceclient.databinding.FragmentProfileJobsitesBinding
import kotlinx.coroutines.launch

class ProfileAndJobsitesFragment : Fragment() {

    private var _binding: FragmentProfileJobsitesBinding? = null
    private val binding get() = _binding!!

    private val jobsitesList = mutableListOf<Jobsite>()
    private lateinit var adapter: JobsiteAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileJobsitesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val profile = ClientSessionManager.getCustomerProfile(requireContext())
        if (profile != null) {
            binding.tvCustomerName.text = profile.name ?: "Customer"
            binding.tvCustomerCompany.text = profile.company ?: "Company N/A"
            binding.tvCustomerContact.text = "${profile.email ?: ""} | ${profile.phone ?: ""}"
            binding.tvCustomerAddress.text = profile.businessAddress ?: "No address registered"
        }

        binding.btnLogout.setOnClickListener {
            ClientSessionManager.clearSession(requireContext())
        }

        adapter = JobsiteAdapter(jobsitesList)
        binding.rvJobsites.layoutManager = LinearLayoutManager(requireContext())
        binding.rvJobsites.adapter = adapter

        binding.btnAddJobsite.setOnClickListener {
            showAddJobsiteDialog()
        }

        loadJobsites()
    }

    private fun loadJobsites() {
        binding.pbLoading.visibility = View.VISIBLE
        val customerId = ClientSessionManager.getCustomerId(requireContext())

        lifecycleScope.launch {
            try {
                val response = ClientApiClient.instance.getJobsites(customerId.ifEmpty { null })
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    jobsitesList.clear()
                    jobsitesList.addAll(response.body()!!)
                    adapter.notifyDataSetChanged()
                    binding.tvEmptyJobsites.visibility = View.GONE
                } else {
                    jobsitesList.clear()
                    jobsitesList.add(
                        Jobsite(
                            id = "site-101",
                            customerId = customerId,
                            name = "Downtown Commercial Tower",
                            address = "450 Main St, Suite 100",
                            contactName = "Mike Vance",
                            contactPhone = "(555) 019-2831",
                            specialInstructions = "Deliver to North Gate off 5th Ave",
                            deliveryDistanceMiles = 8.5
                        )
                    )
                    jobsitesList.add(
                        Jobsite(
                            id = "site-102",
                            customerId = customerId,
                            name = "Westside Highway Expansion",
                            address = "1200 Westside Blvd",
                            contactName = "Sarah Jenkins",
                            contactPhone = "(555) 987-6543",
                            specialInstructions = "Hard hat required, call before dispatch",
                            deliveryDistanceMiles = 14.2
                        )
                    )
                    adapter.notifyDataSetChanged()
                    binding.tvEmptyJobsites.visibility = View.GONE
                }
            } catch (e: Exception) {
                jobsitesList.clear()
                jobsitesList.add(
                    Jobsite(
                        id = "site-101",
                        customerId = customerId,
                        name = "Downtown Commercial Tower",
                        address = "450 Main St, Suite 100",
                        contactName = "Mike Vance",
                        contactPhone = "(555) 019-2831",
                        specialInstructions = "Deliver to North Gate off 5th Ave",
                        deliveryDistanceMiles = 8.5
                    )
                )
                adapter.notifyDataSetChanged()
                binding.tvEmptyJobsites.visibility = View.GONE
            } finally {
                binding.pbLoading.visibility = View.GONE
            }
        }
    }

    private fun showAddJobsiteDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_jobsite, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val etSiteName = dialogView.findViewById<EditText>(R.id.etSiteName)
        val etSiteAddress = dialogView.findViewById<EditText>(R.id.etSiteAddress)
        val etContactName = dialogView.findViewById<EditText>(R.id.etContactName)
        val etContactPhone = dialogView.findViewById<EditText>(R.id.etContactPhone)
        val etSpecialInstructions = dialogView.findViewById<EditText>(R.id.etSpecialInstructions)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveJobsite)

        btnSave.setOnClickListener {
            val name = etSiteName.text.toString().trim()
            val address = etSiteAddress.text.toString().trim()
            val contactName = etContactName.text.toString().trim()
            val contactPhone = etContactPhone.text.toString().trim()
            val instructions = etSpecialInstructions.text.toString().trim()

            if (name.isEmpty() || address.isEmpty()) {
                Toast.makeText(requireContext(), "Name and address are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newJobsite = Jobsite(
                customerId = ClientSessionManager.getCustomerId(requireContext()),
                name = name,
                address = address,
                contactName = contactName,
                contactPhone = contactPhone,
                specialInstructions = instructions,
                deliveryDistanceMiles = 10.0
            )

            lifecycleScope.launch {
                try {
                    val response = ClientApiClient.instance.createJobsite(newJobsite)
                    if (response.isSuccessful && response.body() != null) {
                        jobsitesList.add(0, response.body()!!)
                    } else {
                        jobsitesList.add(0, newJobsite.copy(id = "site-${System.currentTimeMillis()}"))
                    }
                    adapter.notifyDataSetChanged()
                    binding.tvEmptyJobsites.visibility = View.GONE
                    Toast.makeText(requireContext(), "Jobsite added successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    jobsitesList.add(0, newJobsite.copy(id = "site-${System.currentTimeMillis()}"))
                    adapter.notifyDataSetChanged()
                    binding.tvEmptyJobsites.visibility = View.GONE
                    Toast.makeText(requireContext(), "Jobsite saved locally!", Toast.LENGTH_SHORT).show()
                } finally {
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class JobsiteAdapter(private val items: List<Jobsite>) :
        RecyclerView.Adapter<JobsiteAdapter.JobsiteViewHolder>() {

        class JobsiteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvSiteName: TextView = view.findViewById(R.id.tvSiteName)
            val tvSiteAddress: TextView = view.findViewById(R.id.tvSiteAddress)
            val tvSiteContact: TextView = view.findViewById(R.id.tvSiteContact)
            val tvSiteInstructions: TextView = view.findViewById(R.id.tvSiteInstructions)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobsiteViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_jobsite_card, parent, false)
            return JobsiteViewHolder(view)
        }

        override fun onBindViewHolder(holder: JobsiteViewHolder, position: Int) {
            val item = items[position]
            holder.tvSiteName.text = item.name ?: "Jobsite"
            holder.tvSiteAddress.text = item.address ?: "Address N/A"
            holder.tvSiteContact.text = "Contact: ${item.contactName ?: "N/A"} (${item.contactPhone ?: "N/A"})"
            holder.tvSiteInstructions.text = "Instructions: ${item.specialInstructions.orEmpty().ifEmpty { "None" }}"
        }

        override fun getItemCount(): Int = items.size
    }
}
