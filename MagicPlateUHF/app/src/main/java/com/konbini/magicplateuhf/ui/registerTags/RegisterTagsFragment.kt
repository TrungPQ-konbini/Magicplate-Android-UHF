package com.konbini.magicplateuhf.ui.registerTags

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
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.MainApplication
import com.konbini.magicplateuhf.MainApplication.Companion.mReaderUHF
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
    private var lastSerialNumber = ""

    private var isFinishWrite = false
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
                "REFRESH_READER_TAGS" -> {
                    // Refresh tags
                    val listTagEntity =
                        AppContainer.GlobalVariable.getListTagEntity(AppContainer.GlobalVariable.listEPC)
                    dataTags = ArrayList(listTagEntity)
                    dataTags.sortBy { tagEntity -> tagEntity.strEPC }
                    setTitleButtonRegister()
                    if (isFinishWrite) {
                        // Check write tags false
                        val findTagsFalse = dataTags.find { tagEntity -> "%02d".format(tagEntity.plateModel.toString().toInt()) != "%02d".format(selectedPlateModel.plateModelCode.toInt()) }
                        if (findTagsFalse != null) {
                            AlertDialogUtil.showError(
                                getString(R.string.message_error_write_tags_failed),
                                requireContext()
                            )
                            dataTags.forEach { tagEntity ->
                                if ("%02d".format(tagEntity.plateModel.toString().toInt()) != "%02d".format(selectedPlateModel.plateModelCode.toInt())) {
                                    tagEntity.isWriteFalse = true
                                }
                            }
                        }
                        isFinishWrite = false
                    }
                    adapter.setItems(dataTags)
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
        //viewModel.syncPlateModels()
        setupSpinner()
        setupRecyclerView()
        setupObservers()
        setupActions()
        setTitleButtonRegister()
    }

    override fun onStart() {
        super.onStart()
        AppContainer.GlobalVariable.allowReadTags = false
        val filterIntent = IntentFilter()
        filterIntent.addAction("REFRESH_READER_TAGS")
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(changeTagReceiver, IntentFilter(filterIntent))
    }

    override fun onStop() {
        try {
            AppContainer.GlobalVariable.allowReadTags = true
            AppContainer.GlobalVariable.listEPC.clear()
            //Thread.sleep(AppSettings.Hardware.Comport.DelayTimeReadTags.toLong())
            mReaderUHF.realTimeInventory(0xff.toByte(), 0x01.toByte())
        } catch (ex: Exception) {
            LogUtils.logError(ex)
        }
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
                        //writeTags()
                        //AlertDialogUtil.showSuccess(_state.message, requireContext())
                        LogUtils.logInfo("Sync Plate Models success")

                        val listPlatesModel = AppContainer.GlobalVariable.listPlatesModel.toList()
                        if (listPlatesModel.isNotEmpty()) {
                            val sel =
                                binding.spinnerPlateModelCode.getSpinner().selectedItem

                            listPlatesModel.forEach { m->
                                val item = "${m.plateModelCode} - ${m.plateModelTitle}"
                                if(sel == item) {
                                    selectedPlateModel = m
                                    val text = "Last Serial Number: <b>${selectedPlateModel.lastPlateSerial}</b>"
                                    lastSerialNumber = selectedPlateModel.lastPlateSerial
                                    binding.txtLastSerialNumber.text = Html.fromHtml(text)
                                    binding.txtCurrentLastSerial.text = Html.fromHtml(text)
                                }
                            }

                        }


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
        binding.searchTags.setOnQueryTextListener(this)

        binding.btnGetLastNumber.setSafeOnClickListener {
            viewModel.syncPlateModels()
        }

        binding.btnEndSession.setSafeOnClickListener {
            viewModel.setPlateModelData(ArrayList(listSetPlateModelDataRequest))
        }

        binding.btnStartWriting.setSafeOnClickListener {
            writeTags()
        }

        binding.btnStartScan.setSafeOnClickListener {
            AppContainer.GlobalVariable.allowReadTags = false

            try {
                AppContainer.GlobalVariable.listEPC.clear()
                //Thread.sleep(AppSettings.Hardware.Comport.DelayTimeReadTags.toLong())
                mReaderUHF.realTimeInventory(0xff.toByte(), 0x01.toByte())
            } catch (ex: Exception) {
                LogUtils.logError(ex)
            }
            Log.e(
                MainApplication.TAG,
                "==========Start command reading UHF=========="
            )
        }

//        binding.txtLastSerialNumber.setSafeOnClickListener {
//            writeTags2()
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
                showHideLoading(true)
                listSetPlateModelDataRequest.clear()
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

    private fun writeTags2() {
        if (!::selectedPlateModel.isInitialized) {
            AlertDialogUtil.showWarning(
                getString(R.string.message_warning_new_plate_code_required),
                requireContext()
            )
            return
        }
        if (dataTags.isNotEmpty()) {
            try {
                showHideLoading(true)
                listSetPlateModelDataRequest.clear()
                AppContainer.GlobalVariable.allowWriteTags = true
                writeTags2(0)
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
        val listPlatesModel = AppContainer.GlobalVariable.listPlatesModel.toList()
        if (listPlatesModel.isNotEmpty()) {
            // p0?.getItemIdAtPosition(position)

            val sel = p0?.selectedItem
            val selId = p0?.selectedItemId

            listPlatesModel.forEach { m->
                val item = "${m.plateModelCode} - ${m.plateModelTitle}"
                if(sel == item) {
                    selectedPlateModel = m
                    val text = "Last Serial Number: <b>${selectedPlateModel.lastPlateSerial}</b>"
                    lastSerialNumber = selectedPlateModel.lastPlateSerial
                    binding.txtLastSerialNumber.text = Html.fromHtml(text)
                    binding.txtCurrentLastSerial.text = Html.fromHtml(text)
                }
            }
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {}

    private fun setNewEPC(oldEPC: String): String {
        if (serialNumber == 0) {
            val lastPlateSerial = selectedPlateModel.lastPlateSerial.toInt(16)
            serialNumber = lastPlateSerial + 1
        } else {
            serialNumber += 1
        }

        val newPlateModel = "%02X".format(selectedPlateModel.plateModelCode.toInt())
        val newSerialNumber = "%06X".format(serialNumber)

        val replaceNewPlateModel = oldEPC.replaceRange(0, 2, newPlateModel)

        return replaceNewPlateModel.replaceRange(4, 10, newSerialNumber)
    }

    private fun writeTags(position: Int) {
        lifecycleScope.launch {
            try {
                if (position < dataTags.size) {
                    val tag = dataTags[position]
                    val epcValue = tag.strEPC ?: ""
                    if (epcValue.isEmpty()) return@launch

                    // Select tag
                    delay(500)
                    val epcMatch = UhfUtil.setAccessEpcMatch(
                        epcValue,
                        requireContext(),
                        getString(R.string.message_error_param_unknown_error)
                    )
                    LogUtils.logInfo("[epcMatch] | $epcMatch")
                    if (epcMatch != -1) {
                        val newEPC = setNewEPC(epcValue)
                        LogUtils.logInfo(
                            "Old EPC: $epcValue | New EPC: $newEPC | ${
                                newEPC.substring(0, 2).toInt(16)
                            } | ${newEPC.substring(4, 10).toInt(16)}"
                        )
                        Log.e("NEW_EPC", "New EPC: $newEPC | ${newEPC.substring(0, 2).toInt(16)} | ${newEPC.substring(4, 10).toInt(16)}")
                        LogUtils.logInfo("[New EPC] | $newEPC")
                        //delay(200)
                        val writeTag = UhfUtil.writeTag(
                            newEPC,
                            requireContext(),
                            getString(R.string.message_error_write_data_format)
                        )
                        LogUtils.logInfo("[writeTag] | $writeTag")
                        if (writeTag != -1) {
                            // Add serials for submit to server
                            val data = Data(
                                plateModelId = selectedPlateModel.plateModelId.toInt(),
                                lastPlateSerial = newEPC.substring(4, 10)
                            )
                            listSetPlateModelDataRequest.add(data)
                        }
                    }
                    LogUtils.logInfo("=============================================")
                    //delay(200)
                    writeTags(position + 1)
                } else {
                    showHideLoading(false)

                    val t1 = "Tag Written:<b> ${listSetPlateModelDataRequest.count()}</b>"
                    binding.txtTagWritten.text = Html.fromHtml(t1)

                    val t2 =
                        "Current Last Serial:<b> ${listSetPlateModelDataRequest.last().lastPlateSerial}</b> "
                    binding.txtCurrentLastSerial.text = Html.fromHtml(t2)

                    delay(300)
                    serialNumber = 0

                    isFinishWrite = true
                    AppContainer.GlobalVariable.allowWriteTags = false
                    adapter.setItems(ArrayList<TagEntity>())
                    AppContainer.GlobalVariable.listEPC.clear()

                    // Start reading UHF
                    delay(AppSettings.Hardware.Comport.DelayTimeReadTags.toLong())
                    mReaderUHF.realTimeInventory(0xff.toByte(), 0x01.toByte())

                }
            } catch (ex: Exception) {
                LogUtils.logError(ex)
            }
        }
    }

    private fun setNewEPC2(oldEPC: String): String {
        if (serialNumber == 0) {
            val lastPlateSerial = selectedPlateModel.lastPlateSerial.toInt(16)
            serialNumber = lastPlateSerial + 1
        } else {
            serialNumber += 1
        }


       // serialNumber =  (1..5000).random()
        serialNumber =  0

        val newPlateModel = "%02X".format(selectedPlateModel.plateModelCode.toInt())
        val newSerialNumber = "%06X".format(serialNumber)

        return oldEPC.replace(oldEPC.substring(0, 2), newPlateModel)
            .replace(oldEPC.substring(4, 10), newSerialNumber)
    }

    private fun writeTags2(position: Int) {
        lifecycleScope.launch {
            try {
                if (position < dataTags.size) {
                    val tag = dataTags[position]
                    val epcValue = tag.strEPC ?: ""
                    if (epcValue.isEmpty()) return@launch

                    // Select tag
                    val epcMatch = UhfUtil.setAccessEpcMatch(
                        epcValue,
                        requireContext(),
                        getString(R.string.message_error_param_unknown_error)
                    )
                    LogUtils.logInfo("[epcMatch] | $epcMatch")
                    if (epcMatch != -1) {
                        val newEPC = setNewEPC2(epcValue)
                        LogUtils.logInfo(
                            "Old EPC: $epcValue | New EPC: $newEPC | ${
                                newEPC.substring(0, 2).toInt(16)
                            } | ${newEPC.substring(4, 10).toInt(16)}"
                        )
                        //Log.e("NEW_EPC", "New EPC: $newEPC | ${newEPC.substring(0, 2).toInt(16)} | ${newEPC.substring(4, 10).toInt(16)}")
                        delay(200)
                        val writeTag = UhfUtil.writeTag(
                            newEPC,
                            requireContext(),
                            getString(R.string.message_error_write_data_format)
                        )
                        LogUtils.logInfo("[writeTag] | $writeTag")
                        if (writeTag != -1) {
                            // Add serials for submit to server
                            val data = Data(
                                plateModelId = selectedPlateModel.plateModelId.toInt(),
                                lastPlateSerial = newEPC.substring(4, 10)
                            )
                            listSetPlateModelDataRequest.add(data)
                        }
                    }
                    delay(200)
                    writeTags2(position + 1)
                } else {
                    showHideLoading(false)

                    val t1 = "Tag Written:<b> ${listSetPlateModelDataRequest.count()}</b>"
                    binding.txtTagWritten.text = Html.fromHtml(t1)

                    val t2 =
                        "Current Last Serial:<b> ${listSetPlateModelDataRequest.last().lastPlateSerial}</b> "
                    binding.txtCurrentLastSerial.text = Html.fromHtml(t2)

                    delay(300)
                    serialNumber = 0

                    // Start reading UHF
                    try {
                        AppContainer.GlobalVariable.listEPC.clear()
                        //delay(AppSettings.Hardware.Comport.DelayTimeReadTags.toLong())
                        mReaderUHF.realTimeInventory(0xff.toByte(), 0x01.toByte())
                    } catch (ex: Exception) {
                        LogUtils.logError(ex)
                    }
                }

            } catch (ex: Exception) {
                LogUtils.logError(ex)
            }
        }
    }
}