package com.konbini.magicplateuhf.ui.registerTags

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
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
import com.konbini.magicplateuhf.databinding.FragmentRegisterTagsBinding
import com.konbini.magicplateuhf.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterTagsFragment : Fragment(), SearchView.OnQueryTextListener,
    AdapterView.OnItemSelectedListener {

    companion object {
        const val TAG = "RegisterTagsFragment"
    }

    private var serialNumber = 0
    private var processing = false
    private lateinit var selectedPlateModel: PlateModelEntity
    private lateinit var adapter: RegisterTagsAdapter

    private var listSetPlateModelDataRequest: MutableList<Data> = mutableListOf()
    private var listPLateModelsSync: MutableList<PlateModelEntity> = mutableListOf()
    private var dataTags: ArrayList<TagEntity> = ArrayList()

    private var binding: FragmentRegisterTagsBinding by autoCleared()
    private val viewModel: RegisterTagsViewModel by viewModels()

    private val changeTagReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "REFRESH_TAGS" -> {
                    if (!AppContainer.GlobalVariable.allowWriteTags) {
                        // Refresh tags
                        dataTags = ArrayList(AppContainer.CurrentTransaction.listTagEntity)
                        dataTags.sortBy { tagEntity -> tagEntity.strEPC }
                        adapter.setItems(dataTags)
                        setTitleButtonRegister()
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterTagsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinner()
        setupRecyclerView()
        setupObservers()
        setupActions()
        setTitleButtonRegister()
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
        val listPlatesModel = AppContainer.GlobalVariable.listPlatesModel
        val listPlatesCode: MutableList<String> = mutableListOf()
        listPlatesModel.forEach { _plateModelEntity ->
            listPlatesCode.add("${_plateModelEntity.plateModelCode} - ${_plateModelEntity.plateModelTitle}")
        }
        binding.spinnerPlateModelCode.setLabel(getString(R.string.title_plate_code))

        val adapterSpinner =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listPlatesCode)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPlateModelCode.setAdapter(adapterSpinner)

        binding.spinnerPlateModelCode.getSpinner().onItemSelectedListener = this
    }

    private fun setupRecyclerView() {
        adapter = RegisterTagsAdapter()
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
                        writeTags()
                        //AlertDialogUtil.showSuccess(_state.message, requireContext())
                        LogUtils.logInfo("Sync Plate Models success")
                    }
                    Resource.Status.ERROR -> {
                        showHideLoading(false)
                        AlertDialogUtil.showError(_state.message, requireContext())
                        LogUtils.logInfo("Sync Plate Models error")
                    }
                    else -> {}
                }
            }
        }
    }

    private fun setupActions() {
        binding.searchTags.setOnQueryTextListener(this)

//        binding.buttonRegisterTags.setSafeOnClickListener {
//            viewModel.syncPlateModels()
//        }
    }

    private fun writeTags() {
        if (!::selectedPlateModel.isInitialized) {
            AlertDialogUtil.showWarning(
                getString(R.string.message_warning_new_plate_code_required),
                requireContext()
            )
            return
        }
        if (dataTags.isNotEmpty()) {
            try {
                AppContainer.GlobalVariable.allowWriteTags = true
                writeTags(0)
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

    private fun setTitleButtonRegister() {
        if (dataTags.isEmpty()) {
            val titleButton = getString(R.string.title_register_tags).replace(" %s", "")
           // binding.buttonRegisterTags.text = titleButton
        } else {
            val titleButton = String.format(getString(R.string.title_register_tags), dataTags.size)
           // binding.buttonRegisterTags.text = titleButton
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
        val listPlatesModel = AppContainer.GlobalVariable.listPlatesModel
        if (listPlatesModel.isNotEmpty()) {
            selectedPlateModel = listPlatesModel[position]
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {}

    private fun setNewEPC(oldEPC: String): String {
        if (serialNumber == 0) {
            listPLateModelsSync.forEach { _plateModelEntity ->
                if (selectedPlateModel.plateModelCode == _plateModelEntity.plateModelCode) {
                    val lastPlateSerial = _plateModelEntity.lastPlateSerial.toInt(16)
                    serialNumber = lastPlateSerial + 1
                }
            }
        } else {
            serialNumber += 1
        }

        val newPlateModel = "%02X".format(selectedPlateModel.plateModelCode.toInt())
        val newSerialNumber = "%06X".format(serialNumber)

        return oldEPC.replace(oldEPC.substring(0, 2), newPlateModel)
            .replace(oldEPC.substring(4, 10), newSerialNumber)
    }

    private fun writeTags(position: Int) {
        lifecycleScope.launch {
            try {
                if (position < dataTags.size) {
                    val tag = dataTags[position]
                    val epcValue = tag.strEPC ?: ""
                    if (epcValue.isEmpty()) return@launch

                    // Select tag
                    val epcMatch = UhfUtil.setAccessEpcMatch(epcValue, requireContext(), getString(R.string.message_error_param_unknown_error))
                    Log.e(TAG, "[epcMatch] | $epcMatch")
                    if (epcMatch != -1) {
                        val newEPC = setNewEPC(epcValue)
                        LogUtils.logInfo("New EPC: $newEPC | ${newEPC.substring(0, 2).toInt(16)} | ${newEPC.substring(4, 10).toInt(16)}")
                        Log.e("NEW_EPC", "New EPC: $newEPC | ${newEPC.substring(0, 2).toInt(16)} | ${newEPC.substring(4, 10).toInt(16)}")
                        delay(150)
                        val writeTag = UhfUtil.writeTag(newEPC, requireContext(), getString(R.string.message_error_write_data_format))
                        Log.e(TAG, "[writeTag] | $writeTag")
                        if (writeTag != -1) {
                            // Add serials for submit to server
                            val data = Data(
                                plateModelId = selectedPlateModel.plateModelId.toInt(),
                                lastPlateSerial = newEPC.substring(4, 10)
                            )
                            listSetPlateModelDataRequest.add(data)
                        }
                    }
                    delay(150)
                    writeTags(position + 1)
                } else {
                    showHideLoading(false)
                    if (dataTags.size == listSetPlateModelDataRequest.size) {

                        AlertDialogUtil.showSuccess(
                            getString(R.string.message_success_register_tags),
                            requireContext()
                        )

                        // Sync last serial to server
                        viewModel.setPlateModelData(ArrayList(listSetPlateModelDataRequest))
                    } else {
                        AlertDialogUtil.showError(
                            getString(R.string.message_error_some_tag_write_error),
                            requireContext(),
                            getString(R.string.title_register_failed)
                        )
                        serialNumber = 0
                        listSetPlateModelDataRequest.clear()
                    }

                    AppContainer.GlobalVariable.allowWriteTags = false
                    delay(150)
                    //MainApplication.mReaderUHF.resetInventoryBuffer(0xff.toByte())
                    // Start reading UHF
                    MainApplication.mReaderUHF.realTimeInventory(0xff.toByte(), 0x01.toByte())
                }

            } catch (ex: Exception) {
                LogUtils.logError(ex)
            }
        }
    }
}