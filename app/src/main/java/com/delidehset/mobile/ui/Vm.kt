package com.delidehset.mobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.delidehset.mobile.AppContainer
import com.delidehset.mobile.DeliApp

/** The app container, for composables that need a dependency directly (WebView cookie hand-off). */
@Composable
fun deliContainer(): AppContainer =
    (LocalContext.current.applicationContext as DeliApp).container

/** Builds a ViewModel straight from the app container, no DI framework needed. */
@Composable
inline fun <reified VM : ViewModel> deliViewModel(
    key: String? = null,
    crossinline build: (AppContainer) -> VM,
): VM {
    val container = (LocalContext.current.applicationContext as DeliApp).container
    return viewModel(
        key = key,
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T = build(container) as T
        },
    )
}
