package com.example.mobileattester.ui.pages

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import com.example.mobileattester.data.network.Status
import com.example.mobileattester.ui.components.common.ErrorIndicator
import com.example.mobileattester.ui.components.common.HeaderRoundedBottom
import com.example.mobileattester.ui.components.common.LoadingIndicator
import com.example.mobileattester.ui.theme.*
import com.example.mobileattester.ui.util.*
import com.example.mobileattester.ui.viewmodel.AttestationViewModel
import compose.icons.TablerIcons
import compose.icons.tablericons.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.sql.Time

@Composable
fun Home(navController: NavController? = null, viewModel: AttestationViewModel) {
    val compose = currentRecomposeScope
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = Preferences(LocalContext.current)

    /** DATA */
    val currentUrl = viewModel.currentUrl.collectAsState()

    // History of engines
    val enginesList = preferences.engines.collectAsState(initial = sortedSetOf<String>())

    // Check preferences for the last used engine
    // Launch only once when the page has been created
    LaunchedEffect(scope) {
        preferences.engine.collect {
            // Switch engine from preference if not the same
            if (currentUrl.value != "http://${it}/") viewModel.switchBaseUrl("http://${it}/")
        }
    }

    /** UI */
    Column(modifier = Modifier.verticalScroll(ScrollState(0))) {
        var showAllConfigurations by remember { mutableStateOf(false) }
        Column(modifier = Modifier
            .fillMaxSize()
            .background(Primary)
            .border(0.dp, Color.Transparent)) {

            // Top Bar
            Text(text = "Current Configuration",
                modifier = Modifier.padding(10.dp, 15.dp, 10.dp, 5.dp),
                fontSize = FONTSIZE_XXL,
                color = Color.White)

            // Current Engine
            ConfigurationButton(text = parseBaseUrl(currentUrl.value)!!,
                name = "Engine",
                icon = TablerIcons.AdjustmentsHorizontal,
                onClick = {
                    showAllConfigurations = !showAllConfigurations
                })

            if (showAllConfigurations) {
                (enginesList.value.filter { "http://${it}/" != currentUrl.value }).forEach { engineAddress ->
                    ConfigurationButton(
                        text = engineAddress,
                        onClick = {
                            scope.launch {
                                preferences.saveEngine(it)
                            }

                            viewModel.switchBaseUrl("http://${it}/")

                            // Close window on select
                            showAllConfigurations = false
                        },
                        onIconClick = {
                            enginesList.value.remove(it)

                            scope.launch {
                                preferences.saveEngines(enginesList.value.toSortedSet())

                                // Refresh
                                compose.invalidate()
                            }
                        },
                    )
                }


                ConfigurationButton(text = "Ipaddress:port",
                    name = "",
                    icon = TablerIcons.Plus,
                    editable = true,
                    onIconClick = { str ->
                        val config = parseBaseUrl(str)

                        if (config != null) {
                            if (enginesList.value.contains(config)) {
                                Toast.makeText(context, "Config already exists", Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                enginesList.value.add(config)

                                scope.launch {
                                    preferences.saveEngines(enginesList.value)

                                    // Refresh
                                    compose.invalidate()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Input is invalid", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            HeaderRoundedBottom()
        }

        // Content
        Column(modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(5, 5, 0, 0))
            .background(Color.White)) {
            Column(Modifier.padding(4.dp)) {
                Content(navController, viewModel)
            }
        }

    }
}

@Composable
fun ConfigurationButton(
    text: String,
    icon: ImageVector = TablerIcons.X,
    name: String = "History",
    editable: Boolean = false,
    onClick: (String) -> Unit = {},
    onTextChange: (String) -> Unit = {},
    onIconClick: (String) -> Unit = {},
) {
    Button(modifier = Modifier
        .fillMaxWidth()
        .padding(0.dp, 5.dp),
        onClick = { onClick(text) },
        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent,
            contentColor = Color.White),
        elevation = null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            var input by remember { mutableStateOf("") }
            Column() {
                if (!editable) {
                    Text(name)
                    Text(text)
                } else OutlinedTextField(value = input,
                    label = { Text(text) },
                    onValueChange = { input = it; onTextChange(input) },
                    singleLine = true,
                    keyboardActions = KeyboardActions(onDone = { onIconClick(input) }),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        unfocusedLabelColor = Color.White, // TODO: MaterialTheme.colors.primary
                        focusedLabelColor = Color.White,
                        unfocusedBorderColor = Color.White,
                        focusedBorderColor = Color.White,
                    ))
            }

            IconButton(onClick = { if (!editable) onIconClick(text) else onIconClick(input) }) {
                Icon(imageVector = icon, null)
            }
        }
    }
}

@Composable
fun Content(navController: NavController? = null, viewModel: AttestationViewModel) {
    val elementCount = viewModel.elementCount.collectAsState()
    val isRefreshing = viewModel.isRefreshing.collectAsState()

    when (elementCount.value.status) {
        Status.ERROR -> {
            ErrorIndicator(msg = elementCount.value.message.toString())
            return
        }
        Status.LOADING -> {
            LoadingIndicator()
            return
        }
        else -> {
        }
    }

    Row(modifier = Modifier
        .padding(15.dp)
        .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Row() {
            Icon(TablerIcons.DeviceDesktop,
                contentDescription = null,
                modifier = Modifier
                    .padding(5.dp, 0.dp)
                    .align(Alignment.CenterVertically)
                    .size(25.dp))
            Text("System Devices",
                modifier = Modifier
                    .padding(5.dp, 0.dp)
                    .align(Alignment.CenterVertically),
                fontSize = 18.sp)
        }

        if (isRefreshing.value) {
            LoadingIndicator()
        } else {
            Text(AnnotatedString(elementCount.value.data.toString()),
                modifier = Modifier
                    .padding(5.dp, 0.dp)
                    .align(Alignment.CenterVertically)
                    .fillMaxWidth(),
                textAlign = TextAlign.End,
                fontSize = 24.sp)
        }
    }

    Divider(
        Modifier
            .fillMaxWidth()
            .padding(8.dp, 8.dp), color = DividerColor)

    Text(text = "Attestation Overview",
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 12.dp),
        textAlign = TextAlign.Center,
        fontSize = FONTSIZE_XL)

    val resultsLatest = viewModel.getLatestResults().collectAsState()
    val resultsLatestsFails = resultsLatest.value.filter { it.result != 0 }

    val results24h = viewModel.getLatestResults(hoursSince = 24).collectAsState()
    val results24hByElement = results24h.value.groupBy { it.elementID }
    val results24hFails = results24hByElement.mapNotNull { it.value.firstOrNull() { r -> r.result != 0 } }

    Column(Modifier.padding(horizontal = 2.dp))
    {
        Spacer(modifier = Modifier.size(10.dp))
        Alert("Active", attestations = resultsLatest.value.size, fail = resultsLatestsFails.size) {
            navController!!.navigate(Screen.Elements.route, bundleOf(Pair(ARG_BASE_FILTERS, resultsLatestsFails.joinToString(separator = " ") { it.elementID })))
        }
        Spacer(modifier = Modifier.size(20.dp))
        Alert("24H", attestations = results24hByElement.size, fail = results24hFails.size) {
            navController!!.navigate(Screen.Elements.route, bundleOf(Pair(ARG_BASE_FILTERS, results24hFails.joinToString(separator = " ") { it.elementID })))
        }
    }
}

@Composable
fun Alert(
    alertDurationInfo: String = "",
    attestations: Int = 0,
    fail: Int = 0,
    onClick: () -> Unit = {},
) {
    Row(Modifier.padding(start = 10.dp)) {

        Text(text = alertDurationInfo, color = PrimaryDark)
    }
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() },
        Arrangement.SpaceBetween,
        Alignment.CenterVertically) {
        Column(modifier = Modifier.padding(10.dp, 0.dp)) {
            Text(text = "Attested Systems", color = Primary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    TablerIcons.ListSearch,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(28.dp),
                )
                Text(attestations.toString(),
                    color = Primary,
                    modifier = Modifier.padding(5.dp, 0.dp),
                    fontSize = FONTSIZE_LG,
                    fontWeight = FontWeight.SemiBold)
            }
        }
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = "Accepted", color = Ok)
            Row {
                Icon(TablerIcons.SquareCheck, contentDescription = null, tint = Ok)
                Text((attestations - fail).toString(),
                    color = Ok,
                    modifier = Modifier.padding(5.dp, 0.dp),
                    fontSize = FONTSIZE_LG,
                    fontWeight = FontWeight.SemiBold)
            }
        }
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = "Failed", color = Error)
            Row {
                Icon(TablerIcons.SquareX, contentDescription = null, tint = Error)
                Text(fail.toString(),
                    color = Error,
                    modifier = Modifier.padding(5.dp, 0.dp),
                    fontSize = FONTSIZE_LG,
                    fontWeight = FontWeight.SemiBold)
            }
        }
        Column(modifier = Modifier
            .padding(10.dp)
            .align(Alignment.CenterVertically)) {
            Icon(TablerIcons.ChevronRight, contentDescription = null)
        }
    }
}