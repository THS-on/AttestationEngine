package com.example.mobileattester.ui.pages

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mobileattester.R
import com.example.mobileattester.data.model.Element
import com.example.mobileattester.data.model.Policy
import com.example.mobileattester.data.model.Rule
import com.example.mobileattester.data.util.AttestationStatus
import com.example.mobileattester.ui.components.anim.FadeInWithDelay
import com.example.mobileattester.ui.components.common.*
import com.example.mobileattester.ui.theme.FONTSIZE_XXL
import com.example.mobileattester.ui.util.Screen
import com.example.mobileattester.ui.viewmodel.AttestationViewModel

sealed class AttestationType(@StringRes val resId: Int) {
    companion object {
        fun getStringList(context: Context): List<String> {
            return AttestationType::class.sealedSubclasses.map { it.objectInstance as AttestationType }
                .map {
                    context.getString(it.resId)
                }
        }

        fun getFromString(context: Context, str: String): AttestationType? {
            return AttestationType::class.sealedSubclasses.map { it.objectInstance as AttestationType }
                .find { context.getString(it.resId) == str }
        }
    }

    object AttestAndVerify : AttestationType(resId = R.string.attest_verify)
    object Attest : AttestationType(resId = R.string.attest)
}

/**
 * Provides attestation screen
 *
 * TODO Clean up.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Attest(navController: NavController, viewModel: AttestationViewModel) {
    val clickedElementId =
        navController.currentBackStackEntry?.arguments?.get(ARG_ELEMENT_ID).toString()
    val element = viewModel.getElementFromCache(clickedElementId) ?: run {
        FadeInWithDelay(1500) {
            Text(text = "Error getting element data for id: $clickedElementId")
        }
        return
    }

    val u = viewModel.attestationUtil
    val context = LocalContext.current

    // Reset attestation util state when navigating out of this screen
    DisposableEffect(navController) {
        val onNavigateOutListener = NavController.OnDestinationChangedListener { _, _, _ ->
            u.reset()
        }

        navController.addOnDestinationChangedListener(onNavigateOutListener)

        onDispose {
            navController.removeOnDestinationChangedListener(onNavigateOutListener)
        }
    }

    val attestationStatus = u.attestationStatus.collectAsState().value

    // Attestation types are expected to have unique strings.
    val attestationTypes = AttestationType.getStringList(context)
    val selectedType = remember {
        mutableStateOf(context.getString(AttestationType.AttestAndVerify.resId))
    }

    // Current implementation expects names to be unique
    val policies = u.policyFlow.collectAsState().value.data ?: listOf()
    val selectedPolicy = remember {
        mutableStateOf(policies.getOrNull(0)?.name ?: "")
    }

    // Current implementation expects names to be unique
    val rules = u.ruleFlow.collectAsState().value.data ?: listOf()
    val selectedRule = remember {
        mutableStateOf(rules.getOrNull(0)?.name ?: "")
    }

    fun submit() {
        u.reset()

        val policyId = policies.find { it?.name == selectedPolicy.value }?.itemid ?: run {
            println("PolicyId not found")
            return
        }

        val rule = if (selectedType.value == context.getString(AttestationType.Attest.resId)) {
            null
        } else selectedRule

        u.attest(element.itemid, policyId, rule?.value)
    }

    // Decide what to render
    when (attestationStatus) {
        AttestationStatus.LOADING -> LoadingFullScreen()
        AttestationStatus.ERROR -> AttestationErrorScreen(
            onReset = { u.reset() },
            onRetry = { submit() },
        )
        AttestationStatus.SUCCESS -> {
            if (selectedType.value == context.getString(AttestationType.Attest.resId)) {
                navController.navigate(Screen.Claim.route)
            } else navController.navigate(Screen.Result.route)
        }
        AttestationStatus.IDLE -> AttestationConfig(
            element,
            attestationTypes,
            selectedType,
            policies,
            selectedPolicy,
            rules,
            selectedRule,
        ) {
            submit()
        }
    }
}

@Composable
private fun AttestationConfig(
    element: Element,
    attestTypes: List<String>,
    selectedAttestType: MutableState<String>,
    policies: List<Policy?>,
    selectedPolicy: MutableState<String>,
    rules: List<Rule>,
    selectedRule: MutableState<String>,
    onSubmit: () -> Unit,
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {

        // Element data
        HeaderRoundedBottom {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)) {
                Text(
                    text = element.name,
                    fontSize = FONTSIZE_XXL,
                    fontWeight = FontWeight.Bold,
                )
                Text(text = element.endpoint,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp))
            }
        }

        // Config for attest
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.size(16.dp))

            // Attestation type
            SimpleRadioGroup(selections = attestTypes,
                selected = selectedAttestType,
                onSelectionChanged = { selectedAttestType.value = it },
                vertical = false)

            // Policy
            Spacer(modifier = Modifier.size(24.dp))
            Text(modifier = Modifier.padding(bottom = 4.dp), text = "Select policy")
            DropDown(items = policies.map { it?.name },
                selectedValue = policies.find { it?.name == selectedPolicy.value }?.name,
                onSelectionChanged = {
                    println("it $it")
                    selectedPolicy.value = it.toString()
                })

            Spacer(modifier = Modifier.size(24.dp))

            // Rule
            if (selectedAttestType.value == stringResource(id = AttestationType.AttestAndVerify.resId)) {
                Text(modifier = Modifier.padding(bottom = 4.dp), text = "Select rule")
                DropDown(items = rules.map { it.name },
                    selectedValue = selectedRule.value,
                    onSelectionChanged = {
                        selectedRule.value = it
                    })
            }

            // Submit
            Spacer(modifier = Modifier.size(24.dp))
            Button(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(8.dp),
                onClick = { onSubmit() },
            ) {
                Text(text = "Submit", color = Color.White)
            }
        }
    }
}

@Composable
private fun AttestationErrorScreen(
    onReset: () -> Unit,
    onRetry: () -> Unit,
) {
    FadeInWithDelay(50) {
        Column(Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp)) {
            ErrorIndicator(msg = "Something went wrong")
            Row(Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically) {
                Button(onClick = { onReset() }) {
                    Text(text = "Reset", color = Color.White)
                }
                Spacer(Modifier.size(16.dp))
                Button(onClick = { onRetry() }) {
                    Text(text = "Submit again", color = Color.White)
                }
            }
        }
    }
}