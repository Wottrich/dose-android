package com.waseefakhtar.doseapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.waseefakhtar.doseapp.analytics.AnalyticsEvents
import com.waseefakhtar.doseapp.analytics.AnalyticsHelper
import com.waseefakhtar.doseapp.feature.addmedication.navigation.AddMedicationDestination
import com.waseefakhtar.doseapp.feature.history.HistoryDestination
import com.waseefakhtar.doseapp.feature.home.navigation.HomeDestination
import com.waseefakhtar.doseapp.navigation.DoseNavHost
import com.waseefakhtar.doseapp.navigation.DoseTopLevelNavigation
import com.waseefakhtar.doseapp.navigation.TOP_LEVEL_DESTINATIONS
import com.waseefakhtar.doseapp.navigation.TopLevelDestination
import com.waseefakhtar.doseapp.ui.theme.DoseAppTheme
import com.waseefakhtar.doseapp.util.SnackBarUtil

@Composable
fun DoseApp() {
    DoseAppTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // For the snackbar
            var snackbarVisibility by remember { mutableStateOf(false) }
            var snackbarMessage by remember { mutableStateOf("") }

            val navController = rememberNavController()
            val doseTopLevelNavigation = remember(navController) {
                DoseTopLevelNavigation(navController)
            }

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val bottomBarVisibility = rememberSaveable { (mutableStateOf(true)) }
            val fabVisibility = rememberSaveable { (mutableStateOf(true)) }

            val context = LocalContext.current
            val analyticsHelper = AnalyticsHelper.getInstance(context)

            Scaffold(
                modifier = Modifier.padding(16.dp, 0.dp),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                floatingActionButton = {

                    AnimatedVisibility(
                        visible = fabVisibility.value,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it }),
                        content = {
                            DoseFAB(navController, analyticsHelper)
                        }
                    )
                },
                bottomBar = {
                    Box {
                        SnackBarUtil.SnackbarWithoutScaffold(
                            snackBarMessage,
                            openMySnackbar,
                            { openMySnackbar = it },
                        )
                        AnimatedVisibility(
                            visible = bottomBarVisibility.value,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it }),
                            content = {
                                DoseBottomBar(
                                    onNavigateToTopLevelDestination = doseTopLevelNavigation::navigateTo,
                                    currentDestination = currentDestination,
                                    analyticsHelper = analyticsHelper
                                )
                            }
                        )

                    }
                }
            ) { padding ->
                Row(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Horizontal
                            )
                        )
                ) {

                    DoseNavHost(
                        bottomBarVisibility = bottomBarVisibility,
                        fabVisibility = fabVisibility,
                        navController = navController,
                        modifier = Modifier
                            .padding(padding)
                            .consumeWindowInsets(padding)
                            .zIndex(1f),
                        showSnackbar = {
                            openMySnackbar = true
                            snackBarMessage = it

                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DoseBottomBar(
    onNavigateToTopLevelDestination: (TopLevelDestination) -> Unit,
    currentDestination: NavDestination?,
    analyticsHelper: AnalyticsHelper
) {
    // Wrap the navigation bar in a surface so the color behind the system
    // navigation is equal to the container color of the navigation bar.
    Surface(color = MaterialTheme.colorScheme.surface) {
        NavigationBar(
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            ),
            tonalElevation = 0.dp
        ) {

            TOP_LEVEL_DESTINATIONS.forEach { destination ->
                val selected =
                    currentDestination?.hierarchy?.any { it.route == destination.route } == true
                NavigationBarItem(
                    selected = selected,
                    onClick =
                    {
                        trackTabClicked(analyticsHelper, destination.route)
                        onNavigateToTopLevelDestination(destination)
                    },
                    icon = {
                        Icon(
                            if (selected) {
                                destination.selectedIcon
                            } else {
                                destination.unselectedIcon
                            },
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(destination.iconTextId)) }
                )
            }
        }
    }
}

private fun trackTabClicked(analyticsHelper: AnalyticsHelper, route: String) {
    if (route == HomeDestination.route) {
        analyticsHelper.logEvent(AnalyticsEvents.HOME_TAB_CLICKED)
    }

    if (route == HistoryDestination.route) {
        analyticsHelper.logEvent(AnalyticsEvents.HISTORY_TAB_CLICKED)
    }
}

@Composable
fun DoseFAB(navController: NavController, analyticsHelper: AnalyticsHelper) {
    ExtendedFloatingActionButton(
        text = { Text(text = stringResource(id = R.string.add_medication)) },
        icon = { Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.add)) },
        onClick = {
            analyticsHelper.logEvent(AnalyticsEvents.ADD_MEDICATION_CLICKED_FAB)
            navController.navigate(AddMedicationDestination.route)
        },
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
        containerColor = MaterialTheme.colorScheme.tertiary
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DoseAppTheme {
        DoseApp()
    }
}
