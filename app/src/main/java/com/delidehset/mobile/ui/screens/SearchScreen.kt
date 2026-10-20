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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
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
import com.delidehset.mobile.ui.theme.DeliShapes

/** The site's search tabs, in its own order. */
private val SearchTabs = listOf(
    "posts" to "Gönderiler",
    "comments" to "Yorumlar",
    "users" to "Kullanıcılar",
    "holes" to "Topluluklar",
)

@Composable
fun SearchScreen(
    vm: SearchViewModel,
    contentPadding: PaddingValues,
    onOpenPost: (String) -> Unit,
    onOpenUser: (String) -> Unit = {},
    onOpenHole: (String) -> Unit = {},
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
            shape = DeliShapes.control,
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.query.isNotBlank()) {
                        IconButton(onClick = { vm.clearQuery() }) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = com.delidehset.mobile.ui.t("Kapat"),
                                tint = DeliColors.OnSurfaceVariant,
                            )
                        }
                    }
                    IconButton(onClick = { vm.search() }) {
                        Icon(Icons.Filled.Search, contentDescription = com.delidehset.mobile.ui.t("Ara"), tint = DeliColors.Primary)
                    }
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

        // The site's own tabs.
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(DeliColors.Surface, shape = DeliShapes.card)
                    .padding(vertical = 4.dp),
            ) {
                items(state.suggestions) { s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!s.url.isNullOrBlank()) {
                                    val u = s.url
                                    when {
                                        u.startsWith("/@") -> onOpenUser(u.removePrefix("/@"))
                                        u.startsWith("/h/") -> onOpenHole(u.removePrefix("/h/").substringBefore("/"))
                                        else -> onOpenPost(u)
                                    }
                                } else {
                                    vm.search(s.text)
                                }
                            }
                            .padding(vertical = 9.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val icon = when (s.kind) {
                            "user" -> Icons.Filled.Person
                            "hole" -> Icons.Filled.Groups
                            else -> Icons.Filled.Article
                        }
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = DeliColors.Primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = s.text,
                                color = DeliColors.OnBackground,
                                fontSize = 14.sp,
                            )
                            if (!s.detail.isNullOrBlank()) {
                                Text(
                                    text = s.detail,
                                    color = DeliColors.OnSurfaceVariant,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
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
                                // User results link to /@name, hole results to /h/name, everything else to a post.
                                when {
                                    href.startsWith("/@") -> onOpenUser(href.removePrefix("/@"))
                                    href.startsWith("/h/") -> onOpenHole(href.removePrefix("/h/").substringBefore("/"))
                                    else -> onOpenPost(href)
                                }
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
