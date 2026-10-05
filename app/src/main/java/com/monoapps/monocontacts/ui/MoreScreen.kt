@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.monoapps.monocontacts.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monoapps.monocontacts.ContactsViewModel
import com.monoapps.monocontacts.data.ContactDetails
import com.monoapps.monocontacts.data.emailTypeLabel
import com.monoapps.monocontacts.data.phoneTypeLabel
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD

@Composable
fun MoreScreen(
    viewModel: ContactsViewModel,
    contactId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    var details by remember { mutableStateOf<ContactDetails?>(null) }
    LaunchedEffect(contactId, viewModel.contacts) {
        details = viewModel.details(contactId) ?: run { onBack(); null }
    }
    val d = details ?: return

    Scaffold(
        topBar = {
            TopAppBarMMD(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", modifier = Modifier.size(32.dp))
                    }
                },
                title = { TextMMD("More", fontSize = 26.sp, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit contact", modifier = Modifier.size(32.dp))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumnMMD(modifier = Modifier.padding(padding)) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    d.phones.forEach { phone ->
                        Spacer(modifier = Modifier.height(20.dp))
                        TextMMD(
                            "${phoneTypeLabel(phone.type)}  ·  ${phone.number}",
                            fontSize = 24.sp,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        DashedDivider()
                    }
                    FieldRow("First name", d.givenName)
                    FieldRow("Middle name(s)", d.middleName)
                    FieldRow("Last name", d.familyName)
                    FieldRow("Name prefix", d.prefix)
                    FieldRow("Name suffix", d.suffix)
                    FieldRow("Nickname", d.nickname)
                    if (d.email.address.isNotBlank()) {
                        FieldRow("Email (${emailTypeLabel(d.email.type)})", d.email.address)
                    }
                    FieldRow("Company", d.company)
                    FieldRow("Department", d.department)
                    FieldRow("Notes", d.note)
                }
            }
        }
    }
}

@Composable
private fun FieldRow(label: String, value: String) {
    if (value.isBlank()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(16.dp))
        TextMMD(label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        TextMMD(value, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(16.dp))
        DashedDivider()
    }
}
