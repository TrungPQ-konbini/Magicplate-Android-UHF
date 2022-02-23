package com.konbini.magicplateuhf.ui.product

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.konbini.magicplateuhf.data.entities.CategoryEntity
import com.konbini.magicplateuhf.data.entities.ProductEntity
import com.konbini.magicplateuhf.databinding.FragmentProductBinding
import com.konbini.magicplateuhf.utils.SafeClickListener
import com.konbini.magicplateuhf.utils.autoCleared
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProductFragment : Fragment(), SearchView.OnQueryTextListener {

    companion object {
        const val TAG = "ProductFragment"
    }

    private var processing = false
    private lateinit var adapter: ProductAdapter
    private var listCategories: MutableList<CategoryEntity> = mutableListOf()
    private var listProducts: MutableList<ProductEntity> = mutableListOf()

    private var binding: FragmentProductBinding by autoCleared()
    private val viewModel: ProductViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        adapter = ProductAdapter()
        val manager = LinearLayoutManager(requireContext())
        binding.recyclerViewProducts.layoutManager = manager
        binding.recyclerViewProducts.adapter = adapter

        val mDividerItemDecoration = DividerItemDecoration(
            binding.recyclerViewProducts.context,
            LinearLayoutManager.VERTICAL
        )
        binding.recyclerViewProducts.addItemDecoration(mDividerItemDecoration)
    }

    private fun setupObservers() {

    }

    private fun setupActions() {
        getAll()
        binding.searchProduct.setOnQueryTextListener(this)
    }

    private fun getAll() {
        viewLifecycleOwner.lifecycleScope.launch {
            showHideLoading(true)
            listProducts = viewModel.getAll().toMutableList()
            listCategories = viewModel.getAllCategories().toMutableList()
            adapter.setItems(ArrayList(listProducts), ArrayList(listCategories))
            delay(1000L)
            showHideLoading(false)
        }
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

    override fun onQueryTextSubmit(p0: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(searchText: String): Boolean {
        viewLifecycleOwner.lifecycleScope.launch {
            adapter.customFilter(searchText, ArrayList(listProducts))
        }
        return false
    }
}