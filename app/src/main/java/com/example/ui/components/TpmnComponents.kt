package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.TransactionEntity
import com.example.data.local.UserEntity
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.EmeraldShadow
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.NavyObsidian
import com.example.ui.theme.NavySurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningContainer
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Modifier.tpmnVaultBackground(): Modifier = this.drawBehind {
    drawRect(color = NavyObsidian)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(EmeraldShadow.copy(alpha = 0.28f), Color.Transparent),
            center = Offset(size.width * 0.88f, size.height * 0.14f),
            radius = size.maxDimension * 0.55f
        )
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(GoldPrimary.copy(alpha = 0.12f), Color.Transparent),
            center = Offset(size.width * 0.12f, size.height * 0.86f),
            radius = size.maxDimension * 0.48f
        )
    )
}

fun formatUsd(amount: Double): String {
    val fmt = NumberFormat.getCurrencyInstance(Locale.US)
    return fmt.format(amount)
}

fun formatShortTime(millis: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}

@Composable
fun TpmnBrandMark(
    modifier: Modifier = Modifier,
    sizeDp: Int = 44
) {
    Surface(
        modifier = modifier.size(sizeDp.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.65f)),
        color = NavyObsidian
    ) {
        Image(
            painter = painterResource(id = R.drawable.tpmn_icon_art),
            contentDescription = stringResource(R.string.cd_brand_logo),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun SampleAiNoticeBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = WarningContainer.copy(alpha = 0.9f),
        border = BorderStroke(0.8.dp, WarningAmber.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = WarningAmber,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.ai_firebase_notice),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = GoldLight
            )
        }
    }
}

@Composable
fun TpmnPlatinumVaultCard(
    user: UserEntity,
    isBalanceVisible: Boolean,
    onToggleBalance: () -> Unit,
    onToggleFreeze: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(212.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    GoldPrimary.copy(alpha = 0.75f),
                    EmeraldGlow.copy(alpha = 0.45f),
                    GoldPrimary.copy(alpha = 0.35f)
                )
            )
        ),
        shadowElevation = 10.dp,
        color = NavySurface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img_tpmn_card_texture),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                NavyObsidian.copy(alpha = 0.35f),
                                NavyObsidian.copy(alpha = 0.72f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TPMN BANK • ${user.kycTier}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldLight
                        )
                        Text(
                            text = user.accountNumber,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                color = TextSecondary
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            onClick = onToggleFreeze,
                            shape = RoundedCornerShape(50),
                            color = if (user.isFrozen) DangerCrimson.copy(alpha = 0.22f) else EmeraldDeep.copy(alpha = 0.85f),
                            border = BorderStroke(
                                0.8.dp,
                                if (user.isFrozen) DangerCrimson else EmeraldGlow.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("card_freeze_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (user.isFrozen) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = DangerCrimson,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = if (user.isFrozen) "FROZEN" else "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (user.isFrozen) DangerCrimson else EmeraldGlow
                                )
                            }
                        }

                        IconButton(
                            onClick = onToggleBalance,
                            modifier = Modifier.testTag("toggle_balance_button")
                        ) {
                            Icon(
                                imageVector = if (isBalanceVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = stringResource(R.string.cd_toggle_balance),
                                tint = GoldLight
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = "Checking Balance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = if (isBalanceVisible) formatUsd(user.checkingBalance) else "••••••••",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 30.sp
                        ),
                        color = TextPrimary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = EmeraldGlow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Prosperity Vault: ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = if (isBalanceVisible) formatUsd(user.savingsBalance) else "••••••",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = EmeraldGlow
                        )
                    }
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.labelLarge,
                        color = GoldLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionRowCard(tx: TransactionEntity, modifier: Modifier = Modifier) {
    val isCredit = tx.amount >= 0
    val icon = when (tx.type) {
        "BILL_PAY" -> Icons.Default.ReceiptLong
        "VAULT_MOVE" -> Icons.Default.Savings
        else -> if (isCredit) Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade
    }
    val iconTint = when {
        isCredit -> EmeraldGlow
        tx.type == "VAULT_MOVE" -> GoldPrimary
        else -> GoldLight
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = NavySurface.copy(alpha = 0.92f),
        border = BorderStroke(0.75.dp, NavyBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(NavyElevated)
                    .border(0.8.dp, iconTint.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = tx.type,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (tx.initiatedBy.startsWith("AI")) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GoldContainer,
                            border = BorderStroke(0.5.dp, GoldPrimary.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = tx.initiatedBy.replace("_", " "),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = GoldPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = formatShortTime(tx.timestamp),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = (if (isCredit) "+" else "-") + formatUsd(kotlin.math.abs(tx.amount)),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = if (isCredit) EmeraldGlow else TextPrimary
            )
        }
    }
}
