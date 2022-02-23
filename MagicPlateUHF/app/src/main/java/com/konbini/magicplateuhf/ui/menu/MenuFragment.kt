package com.konbini.magicplateuhf.ui.menu

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
import com.konbini.magicplateuhf.data.entities.MenuEntity
import com.konbini.magicplateuhf.databinding.FragmentMenuBinding
import com.konbini.magicplateuhf.utils.SafeClickListener
import com.konbini.magicplateuhf.utils.autoCleared
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MenuFragment : Fragment(), SearchView.OnQueryTextListener {

    companion object {
        const val TAG = "MenuFragment"
    }

    private var processing = false
    private lateinit var adapter: MenuAdapter
    private var listMenus: MutableList<MenuEntity> = mutableListOf()

    private var binding: FragmentMenuBinding by autoCleared()
    private val viewModel: MenuViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMenuBinding.inflate(inflater, container, false)
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
        adapter = MenuAdapter()
        val manager = LinearLayoutManager(requireContext())
        binding.recyclerViewMenus.layoutManager = manager
        binding.recyclerViewMenus.adapter = adapter

        val mDividerItemDecoration = DividerItemDecoration(
            binding.recyclerViewMenus.context,
            LinearLayoutManager.VERTICAL
        )
        binding.recyclerViewMenus.addItemDecoration(mDividerItemDecoration)
    }

    private fun setupObservers() {

    }

    private fun setupActions() {
        getAll()
        binding.searchMenu.setOnQueryTextListener(this)
    }

    private fun getAll() {
        viewLifecycleOwner.lifecycleScope.launch {
            showHideLoading(true)
            listMenus = viewModel.getAll()
                .sortedWith(
                    compareBy(
                        MenuEntity::menuDate,
                        MenuEntity::timeBlockTitle,
                        MenuEntity::plateModelCode,
                        MenuEntity::plateModelName,
                        MenuEntity::productName,
                        MenuEntity::price
                    )
                ).toMutableList()
            adapter.setItems(ArrayList(listMenus))
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
            adapter.customFilter(searchText, ArrayList(listMenus))
        }
        return false
    }

}