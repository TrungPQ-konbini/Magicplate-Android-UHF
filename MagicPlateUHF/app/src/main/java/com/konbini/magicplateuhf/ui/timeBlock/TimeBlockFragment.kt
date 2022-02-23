package com.konbini.magicplateuhf.ui.timeBlock

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
import com.konbini.magicplateuhf.data.entities.TimeBlockEntity
import com.konbini.magicplateuhf.databinding.FragmentTimeBlockBinding
import com.konbini.magicplateuhf.utils.SafeClickListener
import com.konbini.magicplateuhf.utils.autoCleared
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TimeBlockFragment : Fragment(), SearchView.OnQueryTextListener,
    TimeBlockAdapter.ItemListener {

    companion object {
        const val TAG = "TimeBlockFragment"
    }

    private var processing = false
    private lateinit var adapter: TimeBlockAdapter
    private var listTimeBlocks: MutableList<TimeBlockEntity> = mutableListOf()

    private var binding: FragmentTimeBlockBinding by autoCleared()
    private val viewModel: TimeBlockViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTimeBlockBinding.inflate(inflater, container, false)
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
        adapter = TimeBlockAdapter(this)
        val manager = LinearLayoutManager(requireContext())
        binding.recyclerViewTimeBlocks.layoutManager = manager
        binding.recyclerViewTimeBlocks.adapter = adapter

        val mDividerItemDecoration = DividerItemDecoration(
            binding.recyclerViewTimeBlocks.context,
            LinearLayoutManager.VERTICAL
        )
        binding.recyclerViewTimeBlocks.addItemDecoration(mDividerItemDecoration)
    }

    private fun setupObservers() {

    }

    private fun setupActions() {
        getAll()
        binding.searchTimeBlocks.setOnQueryTextListener(this)
    }

    private fun getAll() {
        viewLifecycleOwner.lifecycleScope.launch {
            showHideLoading(true)
            listTimeBlocks = viewModel.getAll()
                .sortedBy { timeBlockEntity -> timeBlockEntity.fromHour }
                .toMutableList()
            adapter.setItems(ArrayList(listTimeBlocks))
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
            adapter.customFilter(searchText, ArrayList(listTimeBlocks))
        }
        return false
    }

    override fun onClickedTimeBlockItem(timeBlockEntity: TimeBlockEntity) {
        viewModel.update(timeBlockEntity)
    }

}