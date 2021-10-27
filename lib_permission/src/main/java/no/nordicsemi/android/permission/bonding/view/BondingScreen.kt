package no.nordicsemi.android.permission.bonding.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import no.nordicsemi.android.permission.bonding.viewmodel.BondingViewModel
import no.nordicsemi.android.utils.BondingState
import no.nordicsemi.android.utils.exhaustive
import org.koin.androidx.compose.getViewModel

@Composable
fun BondingScreen(finishAction: () -> Unit) {
    val viewModel: BondingViewModel = getViewModel()
    val state = viewModel.state.collectAsState().value

    LaunchedEffect("start") {
        viewModel.bondDevice()
    }

    when (state) {
        BondingState.BONDING -> BondingInProgressView()
        BondingState.BONDED -> finishAction()
        BondingState.NONE -> BondingErrorView()
    }.exhaustive
}
