package com.konbini.magicplateuhf.ui.discount

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.entities.DiscountEntity
import com.konbini.magicplateuhf.data.enum.DiscountType
import com.konbini.magicplateuhf.databinding.FragmentDiscountBinding
import com.konbini.magicplateuhf.utils.AlertDialogUtil
import com.konbini.magicplateuhf.utils.PrefUtil
import com.konbini.magicplateuhf.utils.SafeClickListener
import com.konbini.magicplateuhf.utils.autoCleared
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DiscountFragment : Fragment(), SearchView.OnQueryTextListener,
    AdapterView.OnItemSelectedListener, DiscountAdapter.ItemListener {

    companion object {
        const val TAG = "DiscountFragment"
    }

    private val gson = Gson()
    private var processing = false
    private var selectedDiscountType = ""
    private var selectedRole = ""
    private lateinit var adapter: DiscountAdapter
    private lateinit var listDiscounts: List<DiscountEntity>

    private var binding: FragmentDiscountBinding by autoCleared()
    private val viewModel: DiscountViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDiscountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinner()
        setupRecyclerView()
        setupObservers()
        setupActions()
    }

    private fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
        val safeClickListener = SafeClickListener {
            onSafeClick(it)
        }
        setOnClickListener(safeClickListener)
    }

    private fun setupRecyclerView() {
        adapter = DiscountAdapter(this)
        val manager = LinearLayoutManager(requireContext())
        binding.recyclerViewDiscount.layoutManager = manager
        binding.recyclerViewDiscount.adapter = adapter

        val mDividerItemDecoration = DividerItemDecoration(
            binding.recyclerViewDiscount.context,
            LinearLayoutManager.VERTICAL
        )
        binding.recyclerViewDiscount.addItemDecoration(mDividerItemDecoration)
    }

    private fun setupObservers() {

    }

    private fun setupActions() {
        getAll()
        binding.searchDiscounts.setOnQueryTextListener(this)

        binding.addNewDiscount.setSafeOnClickListener {
            if (selectedRole.isEmpty()) {
                AlertDialogUtil.showSuccess(getString(R.string.message_warning_role_is_required), requireContext())
                return@setSafeOnClickListener
            }
            if (selectedDiscountType.isEmpty()) {
                AlertDialogUtil.showSuccess(getString(R.string.message_warning_discount_type_is_required), requireContext())
                return@setSafeOnClickListener
            }
            var discounts: MutableList<DiscountEntity> = mutableListOf()
            if (AppSettings.Options.DiscountList.isNotEmpty()) {
                discounts = gson.fromJson(
                    AppSettings.Options.DiscountList,
                    Array<DiscountEntity>::class.java
                ).toMutableList()
            }

            val isExist = discounts.find { _discountEntity ->
                _discountEntity.roleName == selectedRole
            }
            if (isExist != null) {
                AlertDialogUtil.showError(getString(R.string.message_error_exists), requireContext())
                return@setSafeOnClickListener
            }

            val newDiscount = DiscountEntity(
                roleName = selectedRole,
                discountType = DiscountType.PERCENT.type,
                discountValue = "1" // TODO: TrungPQ hardcode to ignore this percent
            )
            discounts.add(newDiscount)

            adapter.setItems(ArrayList(discounts))

            PrefUtil.setString("AppSettings.Options.DiscountList", gson.toJson(discounts))

            // Refresh Configuration
            AppSettings.getAllSetting()

            AlertDialogUtil.showSuccess(getString(R.string.message_success_save), requireContext())
        }
    }

    private fun getAll() {
        if (AppSettings.Options.DiscountList.isEmpty()) return
        viewLifecycleOwner.lifecycleScope.launch {
            showHideLoading(true)
            listDiscounts =
                gson.fromJson(AppSettings.Options.DiscountList, Array<DiscountEntity>::class.java)
                    .asList()
            adapter.setItems(ArrayList(listDiscounts))
            delay(1000L)
            showHideLoading(false)
        }
    }

    private fun setupSpinner() {
        // Setup discount type
        val listDiscountType: MutableList<String> = mutableListOf()
        listDiscountType.add(DiscountType.PERCENT.type.lowercase())
        selectedDiscountType = listDiscountType[0]

        // Setup roles
        var roles: MutableList<String> = mutableListOf()
        if (AppSettings.Options.RolesList.isNotEmpty()) {
            roles = AppSettings.Options.RolesList.split(",").map { it -> it.trim() }.toMutableList()
            selectedRole = roles[0]
        }
        binding.spinnerRoles.setLabel(getString(R.string.title_roles))
        val adapterSpinnerRoles =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roles)
        adapterSpinnerRoles.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRoles.setAdapter(adapterSpinnerRoles)

        binding.spinnerRoles.getSpinner().onItemSelectedListener = this
    }

    private fun showHideLoading(show: Boolean) {
        if (show) {
            binding.loadingPanel.visibility = View.VISIBLE
            binding.contentPanel.visibility = View.GONE
        } else {
            binding.loadingPanel.visibility = View.GONE
            binding.contentPanel.visibility = View.VISIBLE
        }
        processing = show
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(searchText: String): Boolean {
        viewLifecycleOwner.lifecycleScope.launch {
            adapter.customFilter(searchText, ArrayList(listDiscounts))
        }
        return false
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        var roles: MutableList<String> = mutableListOf()
        if (AppSettings.Options.RolesList.isNotEmpty()) {
            roles = AppSettings.Options.RolesList.split(",").map { it -> it.trim() }.toMutableList()
            selectedRole = roles[position]
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    override fun onRemoveDiscountItem(discountEntity: DiscountEntity) {
        // Remove discount item
        listDiscounts.forEach listDiscount@{ _discountEntity ->
            if (_discountEntity.roleName == discountEntity.roleName
                && _discountEntity.discountType == discountEntity.discountType
                && _discountEntity.discountValue == discountEntity.discountValue
            ) {
                listDiscounts.toMutableList().remove(_discountEntity)
                var discounts: MutableList<DiscountEntity> = mutableListOf()
                if (AppSettings.Options.DiscountList.isNotEmpty()) {
                    discounts = gson.fromJson(
                        AppSettings.Options.DiscountList,
                        Array<DiscountEntity>::class.java
                    ).toMutableList()
                }

                discounts.remove(_discountEntity)
                PrefUtil.setString("AppSettings.Options.DiscountList", gson.toJson(discounts))

                // Refresh Configuration
                AppSettings.getAllSetting()

                AlertDialogUtil.showSuccess(getString(R.string.message_success_save), requireContext())

                getAll()
                return@listDiscount
            }
        }
    }
}