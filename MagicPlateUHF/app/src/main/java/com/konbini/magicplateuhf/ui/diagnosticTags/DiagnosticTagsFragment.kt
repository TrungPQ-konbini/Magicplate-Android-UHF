package com.konbini.magicplateuhf.ui.diagnosticTags

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Html
import android.util.Log
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
import com.konbini.magicplateuhf.MainApplication
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.entities.PlateModelEntity
import com.konbini.magicplateuhf.data.entities.TagEntity
import com.konbini.magicplateuhf.data.remote.plateModel.request.Data
import com.konbini.magicplateuhf.databinding.FragmentDiagnosticTagsBinding
import com.konbini.magicplateuhf.databinding.FragmentRegisterTagsBinding
import com.konbini.magicplateuhf.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DiagnosticTagsFragment : Fragment() {

    companion object {
        const val TAG = "DiagnosticTagsFragment"
    }

    private var serialNumber = 0
    private var lastSerialNumber = ""

    private var processing = false
    private lateinit var selectedPlateModel: PlateModelEntity
    private lateinit var adapter: DiagnosticTagsAdapter

    private var listSetPlateModelDataRequest: MutableList<Data> = mutableListOf()
    private var listPLateModelsSync: MutableList<PlateModelEntity> = mutableListOf()
    private var dataTags: ArrayList<String> = ArrayList()

    private var binding: FragmentDiagnosticTagsBinding by autoCleared()
    private val viewModel: DiagnosticTagsViewModel by viewModels()

    private val changeTagReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "REFRESH_READER_TAGS" -> {
                    // Refresh tags
                    //AppContainer.GlobalVariable.listEPC
//                    dataTags = ArrayList(AppContainer.CurrentTransaction.listTagEntity)
//                    dataTags.sortBy { tagEntity -> tagEntity.strEPC }
//                    adapter.setItems(dataTags)
//                    setTitleButtonRegister()

                    dataTags = ArrayList(AppContainer.GlobalVariable.listEPC)
                    dataTags.sortBy { tagEntity -> tagEntity }
                    adapter.setItems(dataTags)
                    setTitleButtonRegister()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDiagnosticTagsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //viewModel.syncPlateModels()
        setupSpinner()
        setupRecyclerView()
        setupObservers()
        setupActions()
        setTitleButtonRegister()
    }

    override fun onStart() {
        super.onStart()

        val filterIntent = IntentFilter()
        filterIntent.addAction("REFRESH_READER_TAGS")
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(changeTagReceiver, IntentFilter(filterIntent))
    }

    override fun onStop() {
        MainApplication.startRealTimeInventory()
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
        val listPlatesModel = AppContainer.GlobalVariable.listPlatesModel
        val listPlatesCode: MutableList<String> = mutableListOf()
        listPlatesModel.forEach { _plateModelEntity ->
            listPlatesCode.add("${_plateModelEntity.plateModelCode} - ${_plateModelEntity.plateModelTitle}")
        }

        val adapterSpinner =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listPlatesCode)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

    }

    private fun setupRecyclerView() {
        adapter = DiagnosticTagsAdapter()
        val manager = LinearLayoutManager(requireContext())
        binding.recyclerViewTags.layoutManager = manager
        binding.recyclerViewTags.adapter = adapter

        val mDividerItemDecoration = DividerItemDecoration(
            binding.recyclerViewTags.context,
            LinearLayoutManager.VERTICAL
        )
        binding.recyclerViewTags.addItemDecoration(mDividerItemDecoration)
    }

    @Suppress("UNCHECKED_CAST")
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.state.collect() { _state ->
                when (_state.status) {
                    Resource.Status.LOADING -> {
                        showHideLoading(true)
                    }
                    Resource.Status.SUCCESS -> {
                        listPLateModelsSync = _state.data as MutableList<PlateModelEntity>
                        AppContainer.GlobalVariable.listPlatesModel = listPLateModelsSync
                        //writeTags()
                        //AlertDialogUtil.showSuccess(_state.message, requireContext())
                        LogUtils.logInfo("Sync Plate Models success")

                        val listPlatesModel = AppContainer.GlobalVariable.listPlatesModel.toList()



                        showHideLoading(false)
                    }
                    Resource.Status.ERROR -> {
                        showHideLoading(false)
                        AlertDialogUtil.showError(_state.message, requireContext())
                        LogUtils.logInfo("Sync Plate Models error")
                    }
                    else -> {
                    }
                }
            }

        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {

            viewModel.stateEndSession.collect() { _state ->
                when (_state.status) {
                    Resource.Status.LOADING -> {
                        showHideLoading(true)
                    }
                    Resource.Status.SUCCESS -> {
                        LogUtils.logInfo("End Session success")
                        showHideLoading(false)
                        viewModel.syncPlateModels()
                    }
                    Resource.Status.ERROR -> {
                        showHideLoading(false)
                        AlertDialogUtil.showError(_state.message, requireContext())
                        LogUtils.logInfo("End Session error")
                    }
                    else -> {
                    }
                }
            }
        }
    }

    private fun setupActions() {

    }




    private fun setTitleButtonRegister() {
        if (dataTags.isEmpty()) {
            val titleButton = getString(R.string.title_register_tags).replace(" %s", "")
            // binding.buttonRegisterTags.text = titleButton
        } else {
            val titleButton = String.format(getString(R.string.title_register_tags), dataTags.size)
            // binding.buttonRegisterTags.text = titleButton
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





}