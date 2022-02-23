package com.konbini.magicplateuhf.ui.transaction

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.databinding.FragmentTransactionBinding
import com.konbini.magicplateuhf.utils.AlertDialogUtil
import com.konbini.magicplateuhf.utils.CommonUtil
import com.konbini.magicplateuhf.utils.SafeClickListener
import com.konbini.magicplateuhf.utils.autoCleared
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

@AndroidEntryPoint
class TransactionFragment : Fragment(), SearchView.OnQueryTextListener,
    TransactionsAdapter.TransactionItemListener {

    companion object {
        const val TAG = "TransactionFragment"
        const val FROM = "FROM"
        const val TO = "TO"
    }

    private var typeDate = FROM
    private val dateSetListener =
        DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthOfYear)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            val myFormat = "dd/M/yyyy" // mention the format you need
            val sdf = SimpleDateFormat(myFormat, Locale.US)
            if (typeDate == FROM)
                binding.dateFrom.setText(sdf.format(calendar.time))
            else
                binding.dateTo.setText(sdf.format(calendar.time))
        }
    private lateinit var adapter: TransactionsAdapter

    private var binding: FragmentTransactionBinding by autoCleared()
    private val viewModel: TransactionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        setupActions()
        binding.searchTransactions.setOnQueryTextListener(this)
    }

    private fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
        val safeClickListener = SafeClickListener {
            onSafeClick(it)
        }
        setOnClickListener(safeClickListener)
    }

    private fun setupRecyclerView() {
        adapter = TransactionsAdapter(this)
        val manager = LinearLayoutManager(requireContext())
        binding.rvTransactions.layoutManager = manager
        binding.rvTransactions.adapter = adapter
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            val startToday = CommonUtil.atStartOfDay()
            val endToday = CommonUtil.atEndOfDay()
            val transactions = viewModel.getAllToday(startToday, endToday)
            if (!transactions.isNullOrEmpty()) {
                val sortList = transactions.sortedByDescending { _transactionEntity ->
                    _transactionEntity.paymentTime
                }
                adapter.setItems(ArrayList(sortList))
            }
        }
    }

    private fun setupActions() {
        binding.dateFrom.setSafeOnClickListener {
            typeDate = FROM
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(), dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.dateTo.setSafeOnClickListener {
            typeDate = TO
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(), dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.btnSearchByDate.setSafeOnClickListener {
            val fromDateString = binding.dateFrom.text.toString().trim()
            val toDateString = binding.dateTo.text.toString().trim()

            if (fromDateString.isEmpty() || toDateString.isEmpty()) {
                AlertDialogUtil.showWarning(
                    getString(R.string.message_warning_date_is_required),
                    requireContext()
                )
                return@setSafeOnClickListener
            }

            if (!isValidDate(fromDateString) || !isValidDate((toDateString))) {
                AlertDialogUtil.showWarning(
                    getString(R.string.message_warning_date_wrong_format),
                    requireContext()
                )
                return@setSafeOnClickListener
            }

            lifecycleScope.launch {
                val fromDate =
                    CommonUtil.atStartOfDay(CommonUtil.convertStringToMillis(fromDateString))
                val toDate = CommonUtil.atEndOfDay(CommonUtil.convertStringToMillis(toDateString))
                val transactions = viewModel.getAllByRangeDate(fromDate, toDate)
                if (!transactions.isNullOrEmpty()) {
                    val sortList = transactions.sortedByDescending { _transactionEntity ->
                        _transactionEntity.paymentTime
                    }
                    adapter.setItems(ArrayList(sortList))
                } else {
                    adapter.setItems(ArrayList(transactions))
                }
            }
        }
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        lifecycleScope.launch {
            val startToday = CommonUtil.atStartOfDay()
            val endToday = CommonUtil.atEndOfDay()
            val transactions = viewModel.getAllToday(startToday, endToday)
            if (!transactions.isNullOrEmpty()) {
                adapter.customFilter(newText, ArrayList(transactions))
            }
        }
        return false
    }

    override fun onClickedTransaction(transactionId: Long) {
        // TODO:
    }

    private fun isValidDate(inDate: String): Boolean {
        val dateFormat = SimpleDateFormat("dd/M/yyyy", Locale.getDefault())
        dateFormat.isLenient = false
        try {
            dateFormat.parse(inDate.trim { it <= ' ' })
        } catch (pe: ParseException) {
            return false
        }
        return true
    }
}