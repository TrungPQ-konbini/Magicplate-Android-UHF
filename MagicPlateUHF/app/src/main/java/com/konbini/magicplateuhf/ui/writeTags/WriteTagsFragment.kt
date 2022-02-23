package com.konbini.magicplateuhf.ui.writeTags

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.entities.TagEntity
import com.konbini.magicplateuhf.databinding.FragmentWriteTagsBinding
import com.konbini.magicplateuhf.utils.AlertDialogUtil
import com.konbini.magicplateuhf.utils.LogUtils
import com.konbini.magicplateuhf.utils.SafeClickListener
import com.konbini.magicplateuhf.utils.autoCleared
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WriteTagsFragment : Fragment(), SearchView.OnQueryTextListener, AdapterView.OnItemSelectedListener {

    companion object {
        const val TAG = "WriteTagsFragment"
    }

    private var processing = false
    private var selectedPlateModel = ""
    private lateinit var adapter: WriteTagsAdapter

    private var dataTags: ArrayList<TagEntity> = ArrayList()

    private var binding: FragmentWriteTagsBinding by autoCleared()
    private val viewModel: WriteTagsViewModel by viewModels()

    private val changeTagReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "REFRESH_TAGS" -> {
                    // Refresh tags
                    dataTags = ArrayList(AppContainer.CurrentTransaction.listTagEntity)
                    dataTags.sortBy { tagEntity -> tagEntity.strEPC }
                    adapter.setItems(ArrayList(dataTags))
                    setTitleButtonWrite()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWriteTagsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinner()
        setupRecyclerView()
        setupObservers()
        setupActions()
        setTitleButtonWrite()
    }

    override fun onStart() {
        super.onStart()
        val filterIntent = IntentFilter()
        filterIntent.addAction("REFRESH_TAGS")
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(changeTagReceiver, IntentFilter(filterIntent))
    }

    override fun onStop() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(changeTagReceiver)
        super.onStop()
    }

    private fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
        val safeClickListener = SafeClickListener {
            onSafeClick(it)
        }
        setOnClickListener(safeClickListener)
    }

    private fun setupSpinner() {
        val listPlatesModel = AppContainer.InitData.listPlatesModel
        val listPlatesCode: MutableList<String> = mutableListOf()
        listPlatesModel.forEach { _plateModelEntity ->
            listPlatesCode.add("${_plateModelEntity.plateModelCode} - ${_plateModelEntity.plateModelTitle}")
        }
        binding.spinnerPlateModelCode.setLabel(getString(R.string.title_plate_code))

        val adapterSpinner = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listPlatesCode)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPlateModelCode.setAdapter(adapterSpinner)
    }

    private fun setupRecyclerView() {
        adapter = WriteTagsAdapter()
        val manager = LinearLayoutManager(requireContext())
        binding.recyclerViewTags.layoutManager = manager
        binding.recyclerViewTags.adapter = adapter

        val mDividerItemDecoration = DividerItemDecoration(
            binding.recyclerViewTags.context,
            LinearLayoutManager.VERTICAL
        )
        binding.recyclerViewTags.addItemDecoration(mDividerItemDecoration)
    }

    private fun setupObservers() {

    }

    private fun setupActions() {
        binding.searchTags.setOnQueryTextListener(this)

        binding.buttonWriteTags.setSafeOnClickListener {
            if (selectedPlateModel.isEmpty()) {
                AlertDialogUtil.showWarning(
                    getString(R.string.message_warning_new_plate_code_required),
                    requireContext()
                )
                return@setSafeOnClickListener
            }
            if (dataTags.isNotEmpty()) {
                showHideLoading(true)
                try {

                } catch (ex: Exception) {
                    showHideLoading(false)
                    AlertDialogUtil.showError(
                        ex.message.toString(),
                        requireContext()
                    )
                    LogUtils.logError(ex)
                }
            }
        }
    }

    private fun setTitleButtonWrite() {
        if (dataTags.isEmpty()) {
            val titleButton = getString(R.string.title_write_tags).replace(" %s", "")
            binding.buttonWriteTags.text = titleButton
        } else {
            val titleButton = String.format(getString(R.string.title_write_tags), dataTags.size)
            binding.buttonWriteTags.text = titleButton
        }
    }

    override fun onQueryTextSubmit(p0: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(searchText: String): Boolean {
        viewLifecycleOwner.lifecycleScope.launch {
            adapter.customFilter(searchText, ArrayList(dataTags))
        }
        return false
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

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
        val listPlatesModel = AppContainer.InitData.listPlatesModel
        val listPlatesCode: MutableList<String> = mutableListOf()
        listPlatesModel.forEach { _plateModelEntity ->
            listPlatesCode.add(_plateModelEntity.plateModelCode)
        }
        if (listPlatesCode.isNotEmpty()) {
            selectedPlateModel = listPlatesCode[position]
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {}
}