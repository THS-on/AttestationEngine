package com.example.mobileattester.ui.pages

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import com.example.mobileattester.R
import com.example.mobileattester.data.model.Element
import com.example.mobileattester.data.model.ElementResult.Companion.CODE_RESULT_OK
import com.example.mobileattester.data.network.Status
import com.example.mobileattester.data.util.abs.MatchType
import com.example.mobileattester.ui.components.SearchBar
import com.example.mobileattester.ui.components.TagRow
import com.example.mobileattester.ui.components.anim.FadeInWithDelay
import com.example.mobileattester.ui.components.common.DecorText
import com.example.mobileattester.ui.components.common.ErrorIndicator
import com.example.mobileattester.ui.components.common.HeaderRoundedBottom
import com.example.mobileattester.ui.theme.*
import com.example.mobileattester.ui.util.FilterBuilder
import com.example.mobileattester.ui.util.Screen
import com.example.mobileattester.ui.util.latestResults
import com.example.mobileattester.ui.util.navigate
import com.example.mobileattester.ui.viewmodel.AttestationViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import compose.icons.TablerIcons
import compose.icons.tablericons.ChevronRight
import compose.icons.tablericons.Refresh

const val ARG_BASE_FILTERS = "base_filters"

private const val TAG = "Elements"

@Composable
fun Elements(navController: NavController, viewModel: AttestationViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val elementResponse = viewModel.elementFlowResponse.collectAsState()
    val lastIndex = elementResponse.value.data?.lastIndex
    val isLoading = viewModel.isLoading.collectAsState()
    val isRefreshing = viewModel.isRefreshing.collectAsState()

    val searchField = remember { mutableStateOf(TextFieldValue()) }
    val baseFilters = remember {
        navController.currentBackStackEntry?.arguments?.getString(ARG_BASE_FILTERS, "")
    }
    val dataFilter = FilterBuilder.buildWithBaseFilter(
        baseFilters,
        MatchType.MATCH_ANY,
        searchField.value.text,
        MatchType.MATCH_ALL,
    )

    DisposableEffect(lifecycleOwner) {
        viewModel.applyFilters(dataFilter)
        onDispose {
            viewModel.applyFilter()
            viewModel.stopElementFetchLoop()
        }
    }

    Log.d(TAG, "Elements: ${elementResponse.value.data?.size}")

    // Navigate to single element view, pass clicked id as argument
    fun onElementClicked(itemid: String) {
        navController.navigate(Screen.Element.route, bundleOf(Pair(ARG_ELEMENT_ID, itemid)))
    }

    fun onLoadElements() {
        if (elementResponse.value.data?.isNotEmpty() == true) {
            viewModel.getMoreElements()
        } else viewModel.refreshElements()
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing.value),
        onRefresh = { viewModel.refreshElements() },
    ) {
        LazyColumn() {
            // Header
            item {
                HeaderRoundedBottom {
                    SearchBar(
                        searchField,
                        stringResource(id = R.string.placeholder_search_elementlist),
                        onValueChange = {
                            viewModel.applyFilters(dataFilter)
                            viewModel.startElementFetchLoop()
                        },
                    )
                }
                Spacer(modifier = Modifier.size(5.dp))
            }

            // List of the elements
            itemsIndexed(elementResponse.value.data ?: listOf()) { index, element ->
                // Get more elements when we are getting close to the end of the list
                if (lastIndex != null && index >= lastIndex) {
                    viewModel.getMoreElements()
                }

                Column(Modifier.padding(horizontal = 12.dp)) {
                    ElementListItem(element, onElementClick = ::onElementClicked)
                    Divider(modifier = Modifier.fillMaxWidth(), color = DividerColor)
                }
            }

            item {
                if (elementResponse.value.status == Status.ERROR && !isLoading.value) {
                    FadeInWithDelay(200) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            ErrorIndicator(msg = elementResponse.value.message.toString())
                            IconButton(onClick = { onLoadElements() }) {
                                Icon(TablerIcons.Refresh, null, tint = Primary)
                            }
                        }
                    }
                }
            }

            // Footer
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (isLoading.value) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colors.primary,
                        )
                    } else if (!isRefreshing.value && elementResponse.value.status != Status.ERROR) {
                        Text(text = "All elements listed.", color = DarkGrey)
                    }
                }
            }
        }

    }
}

@Composable
private fun ElementListItem(
    element: Element,
    onElementClick: (id: String) -> Unit,
) {
    val results = element.results.latestResults(24)
    val attestations = results.size
    val passed = results.count { it.result == CODE_RESULT_OK }
    val failed = attestations - passed

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onElementClick(element.itemid)
            }
            .padding(top = 12.dp, bottom = 18.dp, start = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row {
            Column {
                Text(text = element.name, style = MaterialTheme.typography.h2)
                Spacer(modifier = Modifier.size(8.dp))
                Row {
                    TagRow(tags = element.types)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.padding(start = 4.dp)) {
                        DecorText("$attestations", Primary, true)
                        Spacer(modifier = Modifier.size(10.dp))
                        DecorText("$passed", Ok, true)
                        Spacer(modifier = Modifier.size(10.dp))
                        DecorText("$failed", Error, true)
                    }
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                        text = "(24h)",
                        color = LightGrey,
                        fontWeight = FontWeight.Normal,
                        fontSize = FONTSIZE_XS,
                    )
                }
            }
        }
        Icon(
            imageVector = TablerIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colors.secondary,
        )
    }
}

