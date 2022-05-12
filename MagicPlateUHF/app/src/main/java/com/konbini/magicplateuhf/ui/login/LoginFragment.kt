package com.konbini.magicplateuhf.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.enum.MachineType
import com.konbini.magicplateuhf.data.enum.PaymentState
import com.konbini.magicplateuhf.databinding.FragmentLoginBinding
import com.konbini.magicplateuhf.ui.MainActivity
import com.konbini.magicplateuhf.utils.AlertDialogUtil
import com.konbini.magicplateuhf.utils.Resource
import com.konbini.magicplateuhf.utils.SafeClickListener
import com.konbini.magicplateuhf.utils.autoCleared
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    companion object {
        const val TAG = "LoginFragment"
    }

    private var binding: FragmentLoginBinding by autoCleared()
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupActions()
        AppContainer.CurrentTransaction.resetTemporaryInfo()
        AppContainer.CurrentTransaction.paymentState = PaymentState.Init
    }

    private fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
        val safeClickListener = SafeClickListener {
            onSafeClick(it)
        }
        setOnClickListener(safeClickListener)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect() { _state ->
                when (_state.status) {
                    Resource.Status.SUCCESS -> {
                        val intent = Intent(activity, MainActivity::class.java)
                        startActivity(intent)
                    }
                    Resource.Status.ERROR -> {
                        handleClearClicked()
                        AlertDialogUtil.showError(_state.message, requireContext())
                    }
                    Resource.Status.LOADING -> {
                        var asterisks = ""
                        for (i in 1.._state.message.length) {
                            asterisks += "◉"
                        }
                        binding.pinCodeValue.text = asterisks
                    }
                }
            }
        }
    }

    private fun setupActions() {
        binding.btnNumber0.setOnClickListener {
            handleNumberClicked("0")
        }
        binding.btnNumber1.setOnClickListener {
            handleNumberClicked("1")
        }
        binding.btnNumber2.setOnClickListener {
            handleNumberClicked("2")
        }
        binding.btnNumber3.setOnClickListener {
            handleNumberClicked("3")
        }
        binding.btnNumber4.setOnClickListener {
            handleNumberClicked("4")
        }
        binding.btnNumber5.setOnClickListener {
            handleNumberClicked("5")
        }
        binding.btnNumber6.setOnClickListener {
            handleNumberClicked("6")
        }
        binding.btnNumber7.setOnClickListener {
            handleNumberClicked("7")
        }
        binding.btnNumber8.setOnClickListener {
            handleNumberClicked("8")
        }
        binding.btnNumber9.setOnClickListener {
            handleNumberClicked("9")
        }
        binding.btnDeleteAll.setOnClickListener {
            handleClearClicked()
        }
        binding.btnEnter.setOnClickListener {
            handleEnterClicked()
        }
        binding.cancelButton.setSafeOnClickListener {
            cancelHandle()
        }
    }

    private fun handleNumberClicked(number: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.numberClicked(number)
        }
    }

    private fun handleClearClicked() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.clearClicked()
        }
    }

    private fun handleEnterClicked() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.enterClicked()
        }
    }

    private fun cancelHandle() {
        when (AppSettings.Options.MachineTypeActivated) {
            MachineType.MAGIC_PLATE_MODE.value,
            MachineType.DISCOUNT_MODE.value -> {
                view?.post {
                    findNavController().navigate(
                        R.id.action_loginFragment_to_magicPlateFragment
                    )
                }
            }
            MachineType.SELF_KIOSK_MODE.value -> {

            }
            MachineType.POS_MODE.value -> {

            }
        }
    }
}