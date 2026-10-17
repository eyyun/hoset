package com.delidehset.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import com.delidehset.mobile.ui.Text
import com.delidehset.mobile.ui.t
import com.delidehset.mobile.ui.tf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delidehset.mobile.data.KeyValue
import com.delidehset.mobile.ui.components.DeliAvatar
import com.delidehset.mobile.ui.components.DeliCard
import com.delidehset.mobile.ui.components.ErrorBox
import com.delidehset.mobile.ui.components.LoadingBox
import com.delidehset.mobile.ui.components.MutedText
import com.delidehset.mobile.ui.components.pullUp
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.vm.ProfileViewModel
import com.delidehset.mobile.ui.theme.DeliShapes

/**
 * A native `@user` page, laid out the way profile tabs usually are: the picture, the
 * name, when they joined, the bio and the counters — all plain, in one block. The only
 * action a profile offers is editing it, and that button appears on your own profile.
 */
@Composable
fun ProfileScreen(
    vm: ProfileViewModel,
    name: String,
    contentPadding: PaddingValues,
    onMessage: (String) -> Unit,
    isSelf: Boolean = false,
    onEditProfile: () -> Unit = {},
) {
    val profiles by vm.state.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    LaunchedEffect(name) { vm.load(name) }
    val profile = profiles[name]

    when {
        loading && profile == null -> LoadingBox(Modifier.fillMaxSize())

        profile == null -> ErrorBox(
            error ?: "Profil yüklenemedi.",
            Modifier.fillMaxSize(),
            onRetry = { vm.load(name, force = true) },
        )

        else -> LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DeliColors.Background),
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = 10.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(DeliShapes.card)
                        .background(DeliColors.Surface)
                        .border(1.dp, DeliColors.Border, DeliShapes.card)
                ) {
                    Column {
                        // The avatar rides over this banner (see `pullUp` below).
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            DeliColors.Primary.copy(alpha = 0.5f),
                                            DeliColors.PrimaryContainer.copy(alpha = 0.3f),
                                            DeliColors.Surface,
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pullUp(46.dp),
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                DeliAvatar(
                                    url = profile.avatarUrl,
                                    size = 86.dp,
                                    modifier = Modifier.border(3.dp, DeliColors.Surface, CircleShape)
                                )
                                Spacer(Modifier.weight(1f))
                                if (!isSelf && profile.isFollowing) {
                                    FollowingChip()
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            Text(
                                "@${profile.name}",
                                color = DeliColors.OnBackground,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            profile.joinedAgo?.let {
                                Spacer(Modifier.height(5.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.CalendarMonth,
                                        contentDescription = null,
                                        tint = DeliColors.OnSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    MutedText(tf("Katılım: {0}", it))
                                }
                            }

                            // The counters sit in the header, the way every profile does it.
                            val counters = profile.stats.take(3)
                            if (counters.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    counters.forEachIndexed { index, kv: KeyValue ->
                                        if (index > 0) MutedText("·")
                                        // The label is translated first and the count is put
                                        // in afterwards: an interpolated line is already one
                                        // string, and a whole string cannot be translated.
                                        Text(
                                            text = tf("{0} {1}", kv.value, t(kv.key).lowercase()),
                                            color = DeliColors.OnSurfaceVariant,
                                            fontSize = 13.sp,
                                        )
                                    }
                                }
                            }

                            profile.bio?.takeIf { it.isNotBlank() }?.let { bioText ->
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = bioText,
                                    color = DeliColors.OnBackground,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                )
                            }

                            if (isSelf) {
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = onEditProfile,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = DeliShapes.control,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DeliColors.Primary,
                                        contentColor = Color.White,
                                    ),
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Profili Düzenle", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            if (profile.postDays.isNotEmpty()) {
                item(key = "activity") {
                    DeliCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.BarChart,
                                contentDescription = null,
                                tint = DeliColors.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Gönderi aktivitesi",
                                color = DeliColors.OnBackground,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.height(10.dp))

                        val max = profile.postDays.maxOf { it.count }.coerceAtLeast(1)
                        for (day in profile.postDays) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MutedText(day.label, Modifier.width(92.dp))

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(9.dp)
                                        .clip(DeliShapes.small)
                                        .background(DeliColors.SurfaceVariant)
                                ) {
                                    val fraction = (day.count.toFloat() / max).coerceIn(0.04f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction)
                                            .height(9.dp)
                                            .clip(DeliShapes.small)
                                            .background(
                                                Brush.horizontalGradient(listOf(DeliColors.Primary, DeliColors.Blue))
                                            )
                                    )
                                }

                                Spacer(Modifier.width(10.dp))
                                Text(
                                    day.count.toString(),
                                    color = DeliColors.OnBackground,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.width(28.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (profile.stats.isEmpty() && profile.postDays.isEmpty() && profile.bio.isNullOrBlank()) {
                item(key = "empty") {
                    DeliCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                tint = DeliColors.OnSurfaceVariant,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Bu profil henüz ek bir ayrıntı paylaşmamış.",
                                color = DeliColors.OnSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Only shown on somebody else's profile, when you already follow them. */
@Composable
private fun FollowingChip() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(DeliColors.Primary.copy(alpha = 0.18f))
            .border(1.dp, DeliColors.Primary.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = DeliColors.Primary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text("Takip Ediliyor", color = DeliColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
