package com.konbini.magicplateuhf.ui.category

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
import com.konbini.magicplateuhf.databinding.FragmentCategoryBinding
import com.konbini.magicplateuhf.utils.SafeClickListener
import com.konbini.magicplateuhf.utils.autoCleared
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CategoryFragment : Fragment(), SearchView.OnQueryTextListener,
    CategoriesAdapter.ItemListener {

    companion object {
        const val TAG = "CategoryFragment"
    }

    private var processing = false
    private lateinit var adapter: CategoriesAdapter
    private var listCategories: MutableList<CategoryEntity> = mutableListOf()

    private var binding: FragmentCategoryBinding by autoCleared()
    private val viewModel: CategoryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCategoryBinding.inflate(inflater, container, false)
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
        adapter = CategoriesAdapter(this)
        val manager = LinearLayoutManager(requireContext())
        binding.recyclerViewCategories.layoutManager = manager
        binding.recyclerViewCategories.adapter = adapter

        val mDividerItemDecoration = DividerItemDecoration(
            binding.recyclerViewCategories.context,
            LinearLayoutManager.VERTICAL
        )
        binding.recyclerViewCategories.addItemDecoration(mDividerItemDecoration)
    }

    private fun setupObservers() {

    }

    private fun setupActions() {
        getAll()
        binding.searchCategories.setOnQueryTextListener(this)
    }

    private fun getAll() {
        viewLifecycleOwner.lifecycleScope.launch {
            showHideLoading(true)
            listCategories = viewModel.getAll().toMutableList()
            adapter.setItems(ArrayList(listCategories))
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
            adapter.customFilter(searchText, ArrayList(listCategories))
        }
        return false
    }

    override fun onClickedCategoryItem(categoryEntity: CategoryEntity) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.update(categoryEntity)
        }
    }
}