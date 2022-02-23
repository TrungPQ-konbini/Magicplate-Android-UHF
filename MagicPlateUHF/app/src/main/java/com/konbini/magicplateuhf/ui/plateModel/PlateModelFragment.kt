package com.konbini.magicplateuhf.ui.plateModel

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
import com.konbini.magicplateuhf.data.entities.PlateModelEntity
import com.konbini.magicplateuhf.databinding.FragmentPlateModelBinding
import com.konbini.magicplateuhf.utils.SafeClickListener
import com.konbini.magicplateuhf.utils.autoCleared
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlateModelFragment : Fragment(), SearchView.OnQueryTextListener {

    companion object {
        const val TAG = "PlateModelFragment"
    }

    private var processing = false
    private lateinit var adapter: PlateModelAdapter
    private var listPlatesModel: MutableList<PlateModelEntity> = mutableListOf()

    private var binding: FragmentPlateModelBinding by autoCleared()
    private val viewModel: PlateModelViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlateModelBinding.inflate(inflater, container, false)
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
        adapter = PlateModelAdapter()
        val manager = LinearLayoutManager(requireContext())
        binding.recyclerViewPlatesModel.layoutManager = manager
        binding.recyclerViewPlatesModel.adapter = adapter

        val mDividerItemDecoration = DividerItemDecoration(
            binding.recyclerViewPlatesModel.context,
            LinearLayoutManager.VERTICAL
        )
        binding.recyclerViewPlatesModel.addItemDecoration(mDividerItemDecoration)
    }

    private fun setupObservers() {

    }

    private fun setupActions() {
        getAll()
        binding.searchPlatesModel.setOnQueryTextListener(this)
    }

    private fun getAll() {
        viewLifecycleOwner.lifecycleScope.launch {
            showHideLoading(true)
            listPlatesModel = viewModel.getAll().toMutableList()
            adapter.setItems(ArrayList(listPlatesModel))
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
            adapter.customFilter(searchText, ArrayList(listPlatesModel))
        }
        return false
    }
}