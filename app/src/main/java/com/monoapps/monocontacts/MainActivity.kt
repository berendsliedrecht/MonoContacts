package com.monoapps.monocontacts

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.viewmodel.compose.viewModel
import android.provider.ContactsContract
import com.monoapps.monocontacts.data.ContactDetails
import com.monoapps.monocontacts.data.PhoneEntry
import com.monoapps.monocontacts.ui.DetailsScreen
import com.monoapps.monocontacts.ui.EditScreen
import com.monoapps.monocontacts.ui.ListScreen
import com.monoapps.monocontacts.ui.MoreScreen
import com.monoapps.monocontacts.ui.PermissionScreen
import com.monoapps.monocontacts.ui.SearchScreen
import com.mudita.mmd.ThemeMMD

sealed interface Screen {
    data object Contacts : Screen
    data object Search : Screen
    data class Details(val contactId: Long) : Screen
    data class More(val contactId: Long) : Screen
    data class Edit(val contactId: Long) : Screen
    data class New(val prefillPhone: String = "", val prefillName: String = "") : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initial = initialScreen()
        setContent { ThemeMMD { App(initial) } }
    }

    /** Screen requested by a VIEW or INSERT intent from another app. */
    private fun initialScreen(): Screen? = when (intent?.action) {
        Intent.ACTION_VIEW -> intent.data?.let { uri ->
            runCatching {
                contentResolver.query(
                    uri,
                    arrayOf(ContactsContract.Contacts._ID),
                    null,
                    null,
                    null,
                )?.use { if (it.moveToFirst()) Screen.Details(it.getLong(0)) else null }
            }.getOrNull()
        }

        Intent.ACTION_INSERT -> Screen.New(
            prefillPhone = intent.getStringExtra(ContactsContract.Intents.Insert.PHONE).orEmpty(),
            prefillName = intent.getStringExtra(ContactsContract.Intents.Insert.NAME).orEmpty(),
        )

        else -> null
    }
}

@Composable
private fun App(initial: Screen? = null) {
    val viewModel: ContactsViewModel = viewModel()
    if (!viewModel.hasPermission) {
        PermissionScreen(onGranted = { viewModel.onPermissionGranted() })
        return
    }

    val stack = remember {
        mutableStateListOf<Screen>(Screen.Contacts).also { if (initial != null) it.add(initial) }
    }
    fun push(s: Screen) = stack.add(s)
    fun pop() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    fun popTo(predicate: (Screen) -> Boolean) {
        while (stack.size > 1 && !predicate(stack.last())) stack.removeAt(stack.lastIndex)
    }
    BackHandler(enabled = stack.size > 1) { pop() }

    when (val screen = stack.last()) {
        Screen.Contacts -> ListScreen(
            contacts = viewModel.contacts,
            onOpen = { push(Screen.Details(it)) },
            onSearch = { push(Screen.Search) },
            onAdd = { push(Screen.New()) },
        )

        Screen.Search -> SearchScreen(
            contacts = viewModel.contacts,
            onBack = ::pop,
            onOpen = { push(Screen.Details(it)) },
        )

        is Screen.Details -> DetailsScreen(
            viewModel = viewModel,
            contactId = screen.contactId,
            onBack = ::pop,
            onEdit = { push(Screen.Edit(screen.contactId)) },
            onMore = { push(Screen.More(screen.contactId)) },
        )

        is Screen.More -> MoreScreen(
            viewModel = viewModel,
            contactId = screen.contactId,
            onBack = ::pop,
            onEdit = { push(Screen.Edit(screen.contactId)) },
        )

        is Screen.Edit -> {
            var initial by remember(screen.contactId) { mutableStateOf<ContactDetails?>(null) }
            LaunchedEffect(screen.contactId) { initial = viewModel.details(screen.contactId) }
            initial?.let { details ->
                EditScreen(
                    viewModel = viewModel,
                    initial = details,
                    onClose = ::pop,
                    onSaved = ::pop,
                    onDeleted = { popTo { it == Screen.Contacts } },
                )
            }
        }

        is Screen.New -> EditScreen(
            viewModel = viewModel,
            initial = if (screen.prefillPhone.isBlank() && screen.prefillName.isBlank()) null
            else ContactDetails(
                givenName = screen.prefillName,
                phones = listOf(PhoneEntry(number = screen.prefillPhone)).filter { it.number.isNotBlank() },
            ),
            isNew = true,
            onClose = ::pop,
            onSaved = ::pop,
            onDeleted = ::pop,
        )
    }
}
