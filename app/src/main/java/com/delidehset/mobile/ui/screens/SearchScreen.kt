package com.delidehset.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import com.delidehset.mobile.ui.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delidehset.mobile.data.SimpleRow
import com.delidehset.mobile.ui.components.DeliCard
import com.delidehset.mobile.ui.components.EmptyBox
import com.delidehset.mobile.ui.components.ErrorBox
import com.delidehset.mobile.ui.components.LoadingBox
import com.delidehset.mobile.ui.components.MutedText
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.vm.SearchViewModel

/** The site's search tabs, in its own order. */
private val SearchTabs = listOf(
    "posts" to "Gönderiler",
    "comments" to "Yorumlar",
    "users" to "Kullanıcılar",
)

@Composable
fun SearchScreen(
    vm: SearchViewModel,
    contentPadding: PaddingValues,
    onOpenPost: (String) -> Unit,
    onOpenUser: (String) -> Unit = {},
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(DeliColors.Background)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            placeholder = {
                Text("Ara (ör. 'hole:general')", color = DeliColors.OnSurfaceVariant, fontSize = 14.sp)
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            trailingIcon = {
                IconButton(onClick = { vm.search() }) {
                    Icon(Icons.Filled.Search, contentDescription = "Ara", tint = DeliColors.Primary)
                }
            },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { vm.search() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DeliColors.Surface,
                unfocusedContainerColor = DeliColors.Surface,
                focusedTextColor = DeliColors.OnBackground,
                unfocusedTextColor = DeliColors.OnBackground,
                cursorColor = DeliColors.Primary,
                focusedIndicatorColor = DeliColors.Primary,
                unfocusedIndicatorColor = DeliColors.Border,
            ),
        )

        // The site's own three tabs. Without them a user search or a comment search was
        // simply unreachable, and the tab strip itself was what the row reader had been
        // mistaking for results.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SearchTabs.forEach { (kind, label) ->
                val active = state.kind == kind
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (active) DeliColors.PrimaryContainer else DeliColors.SurfaceVariant)
                        .clickable { vm.setKind(kind) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(
                        label,
                        color = if (active) DeliColors.OnBackground else DeliColors.OnSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        if (state.suggestions.isNotEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                items(state.suggestions) { s ->
                    Text(
                        text = s,
                        color = DeliColors.OnSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.search(s) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        when {
            state.loading -> LoadingBox(Modifier.fillMaxSize())
            state.error != null -> ErrorBox(state.error!!, Modifier.fillMaxSize(), onRetry = { vm.search() })
            !state.searched -> EmptyBox("Aramak için bir şey yaz.", Modifier.fillMaxSize())
            state.results.isEmpty() -> EmptyBox("Sonuç yok.", Modifier.fillMaxSize())
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = contentPadding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.results) { row: SimpleRow ->
                    DeliCard(
                        onClick = row.href?.let { href ->
                            {
                                // User results link to /@name, everything else to a post.
                                if (href.startsWith("/@")) onOpenUser(href.removePrefix("/@"))
                                else onOpenPost(href)
                            }
                        },
                    ) {
                        Text(
                            text = row.cells.firstOrNull().orEmpty(),
                            color = DeliColors.OnBackground,
                            fontSize = 15.sp,
                            maxLines = 3,
                        )
                        if (row.cells.size > 1) {
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MutedText(row.cells.drop(1).filter { it.isNotBlank() }.joinToString("  ·  "))
                            }
                        }
                    }
                }
            }
        }
    }
}
