@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.monoapps.monocontacts.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monoapps.monocontacts.data.ContactRow
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD

@Composable
fun ListScreen(
    contacts: List<ContactRow>,
    onOpen: (Long) -> Unit,
    onSearch: () -> Unit,
    onAdd: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBarMMD(
                title = { TextMMD("Contacts", fontSize = 26.sp, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSearch) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = "Search contacts",
                            modifier = Modifier.size(32.dp),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (contacts.isEmpty()) {
                TextMMD(
                    "No contacts yet…",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                val favorites = contacts.filter { it.starred }
                val others = if (favorites.isEmpty()) contacts else contacts.filterNot { it.starred }
                LazyColumnMMD(modifier = Modifier.fillMaxSize()) {
                    if (favorites.isNotEmpty()) {
                        item { SectionHeader("Favorites") }
                        items(favorites, key = { it.id }) { ContactListRow(it, onOpen) }
                        item { SectionHeader("All contacts") }
                    }
                    items(others, key = { it.id }) { ContactListRow(it, onOpen) }
                }
            }
            OutlinedFab(
                onClick = onAdd,
                contentDescription = "New contact",
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 36.dp, bottom = 28.dp),
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    TextMMD(
        title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun ContactListRow(contact: ContactRow, onOpen: (Long) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .calmClickable { onOpen(contact.id) },
    ) {
        TextMMD(
            text = styledName(contact.displayName),
            fontSize = 22.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
        )
        DashedDivider(modifier = Modifier.padding(horizontal = 20.dp))
    }
}
