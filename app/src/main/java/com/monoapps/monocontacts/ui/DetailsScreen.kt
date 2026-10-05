@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.monoapps.monocontacts.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.monoapps.monocontacts.ContactsViewModel
import com.monoapps.monocontacts.R
import com.monoapps.monocontacts.data.ContactDetails
import com.monoapps.monocontacts.data.phoneTypeLabel
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD

@Composable
fun DetailsScreen(
    viewModel: ContactsViewModel,
    contactId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onMore: () -> Unit,
) {
    var details by remember { mutableStateOf<ContactDetails?>(null) }
    // Reload when the contact list changes (e.g. after an edit).
    LaunchedEffect(contactId, viewModel.contacts) {
        details = viewModel.details(contactId) ?: run { onBack(); null }
    }
    val d = details ?: return

    val context = LocalContext.current
    val number = d.phones.firstOrNull()?.number
    fun dial() {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    }
    val callPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")))
        } else {
            dial()
        }
    }

    Scaffold(
        topBar = {
            TopAppBarMMD(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", modifier = Modifier.size(32.dp))
                    }
                },
                title = { TextMMD("Details", fontSize = 26.sp, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit contact", modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = {
                        details = d.copy(starred = !d.starred)
                        viewModel.setStarred(d.id, !d.starred)
                    }) {
                        Icon(
                            if (d.starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (d.starred) "Remove favorite" else "Add favorite",
                            modifier = Modifier.size(32.dp),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(100.dp))
            TextMMD(
                d.displayName,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            d.phones.forEach { phone ->
                TextMMD(
                    buildAnnotatedString {
                        append(phoneTypeLabel(phone.type).lowercase() + "  ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(phone.number) }
                    },
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
            Spacer(modifier = Modifier.height(110.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(44.dp)) {
                if (number != null) {
                    ActionButton(painterResource(R.drawable.ic_call), "Call", outlined = true) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")))
                        } else {
                            callPermission.launch(Manifest.permission.CALL_PHONE)
                        }
                    }
                    ActionButton(painterResource(R.drawable.ic_sms), "SMS", outlined = true) {
                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")))
                    }
                }
                ActionButton(rememberVectorPainter(Icons.Outlined.GridView), "More", outlined = false, onClick = onMore)
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: Painter,
    label: String,
    outlined: Boolean,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .then(
                    if (outlined) Modifier.border(2.dp, Color.Black, RoundedCornerShape(22.dp))
                    else Modifier
                )
                .calmClickable(onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextMMD(label, fontSize = 20.sp)
    }
}
