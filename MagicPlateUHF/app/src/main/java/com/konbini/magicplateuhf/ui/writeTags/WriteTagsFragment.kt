package com.konbini.magicplateuhf.ui.writeTags

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.MainApplication
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.entities.TagEntity
import com.konbini.magicplateuhf.databinding.FragmentWriteTagsBinding
import com.konbini.magicplateuhf.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WriteTagsFragment : Fragment(), SearchView.OnQueryTextListener,
    AdapterView.OnItemSelectedListener {

    companion object {
        const val TAG = "WriteTagsFragment"
    }

    private var serialNumber = 1000
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
                    if (!AppContainer.GlobalVariable.allowWriteTags) {
                        // Refresh tags
                        dataTags = ArrayList(AppContainer.CurrentTransaction.listTagEntity)
                        dataTags.sortBy { tagEntity -> tagEntity.strEPC }
                        adapter.setItems(dataTags)
                        setTitleButtonWrite()
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
        AppContainer.GlobalVariable.allowWriteTags = false
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
        val listPlatesModel = AppContainer.GlobalVariable.listPlatesModel
        val listPlatesCode: MutableList<String> = mutableListOf()
        listPlatesModel.forEach { _plateModelEntity ->
            listPlatesCode.add(_plateModelEntity.plateModelCode)
        }
        if (listPlatesCode.isNotEmpty()) {
            selectedPlateModel = listPlatesCode[position]
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {}

    private fun setNewEPC(oldEPC: String): String {
        serialNumber += 1
        val newPlateModel = "%04X".format(selectedPlateModel.toInt())
        val newSerialNumber = "%06X".format(serialNumber)
        val newPaidDate = "%02X".format(0)
        val newPaidSession = "%02X".format(0)
        var newCustomPrice = "%06X".format(0)
        if (binding.newCustomPrice.text.toString().isNotEmpty()) {
            val price = (binding.newCustomPrice.text.toString().toDouble() * 100).toInt()
            newCustomPrice =
                "%06X".format(price)
        }

        return oldEPC.replace(oldEPC.substring(0, 4), newPlateModel)
            .replace(oldEPC.substring(4, 10), newSerialNumber)
            .replace(oldEPC.substring(14, 16), newPaidDate)
            .replace(oldEPC.substring(16, 18), newPaidSession)
            .replace(oldEPC.substring(18), newCustomPrice)
    }

    private fun writeTags(position: Int) {
        lifecycleScope.launch {
            try {
                if (position < dataTags.size) {
                    val tag = dataTags[position]
                    val epcValue = tag.strEPC ?: ""
                    if (epcValue.isEmpty()) return@launch

                    // Select tag
                    UhfUtil.setAccessEpcMatch(epcValue, requireContext(), getString(R.string.message_error_param_unknown_error))

                    val newEPC = setNewEPC(epcValue)
                    delay(100)
                    UhfUtil.writeTag(newEPC, requireContext(), getString(R.string.message_error_write_data_format))

                    delay(100)
                    writeTags(position + 1)
                } else {
                    showHideLoading(false)
                    AlertDialogUtil.showSuccess(
                        getString(R.string.message_success_write_tags),
                        requireContext()
                    )

                    delay(1000)
                    AppContainer.GlobalVariable.allowWriteTags = false
                    // Start reading UHF
                    MainApplication.startRealTimeInventory()
                }

            } catch (ex: Exception) {
                LogUtils.logError(ex)
            }
        }
    }

//    private fun setAccessEpcMatch(tag: String) {
//        var btAryEpc: ByteArray? = null
//        btAryEpc = try {
//            val result = StringTool.stringToStringArray(tag.uppercase(), 2)
//            StringTool.stringArrayToByteArray(result, result.size)
//        } catch (ex: Exception) {
//            AlertDialogUtil.showError(
//                getString(R.string.message_error_param_unknown_error),
//                requireContext()
//            )
//            LogUtils.logError(ex)
//            return
//        }
//        if (btAryEpc == null) {
//            AlertDialogUtil.showError(
//                getString(R.string.message_error_param_unknown_error),
//                requireContext()
//            )
//            return
//        }
//        MainApplication.mReaderUHF.setAccessEpcMatch(
//            0x01,
//            (btAryEpc.size and 0xFF).toByte(), btAryEpc
//        )
//    }

//    private fun writeTag(tag: String) {
//        /*
//         * 0x00: area password
//         * 0x01: area epc
//         * 0x02: area tid
//         * 0x03: area user
//         */
//        val btMemBank: Byte = 0x01 // Fix access area EPC
//        val btWordAdd: Byte = 0x02
//        var btWordCnt: Byte = 0x00
//        val btAryPassWord: ByteArray =
//            byteArrayOf(0x00, 0x00, 0x00, 0x00) // Fix password is 00000000
//
//        var btAryData: ByteArray? = null
//        var result: Array<String>? = null
//        try {
//            result = StringTool.stringToStringArray(tag.uppercase(), 2)
//            btAryData = StringTool.stringArrayToByteArray(result, result.size)
//            btWordCnt = (result.size / 2 + result.size % 2 and 0xFF).toByte()
//        } catch (ex: Exception) {
//            AlertDialogUtil.showError(
//                getString(R.string.message_error_write_data_format),
//                requireContext()
//            )
//            LogUtils.logError(ex)
//            return
//        }
//
//        if (btAryData == null || btAryData.isEmpty()) {
//            AlertDialogUtil.showError(
//                getString(R.string.message_error_write_data_format),
//                requireContext()
//            )
//            return
//        }
//        MainApplication.mReaderUHF.writeTag(
//            0x01,
//            btAryPassWord,
//            btMemBank,
//            btWordAdd,
//            btWordCnt,
//            btAryData
//        )
//    }
}