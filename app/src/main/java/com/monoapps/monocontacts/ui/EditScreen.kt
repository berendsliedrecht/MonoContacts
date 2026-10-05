@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.monoapps.monocontacts.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monoapps.monocontacts.ContactsViewModel
import com.monoapps.monocontacts.data.ContactDetails
import com.monoapps.monocontacts.data.EMAIL_TYPES
import com.monoapps.monocontacts.data.EmailEntry
import com.monoapps.monocontacts.data.PHONE_TYPES
import com.monoapps.monocontacts.data.PhoneEntry
import com.monoapps.monocontacts.data.emailTypeLabel
import com.monoapps.monocontacts.data.phoneTypeLabel
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.menus.DropdownMenuItemMMD
import com.mudita.mmd.components.menus.DropdownMenuMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD

/** Editor for both New Contact (isNew, optionally prefilled) and Edit. */
@Composable
fun EditScreen(
    viewModel: ContactsViewModel,
    initial: ContactDetails?,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    isNew: Boolean = initial == null,
) {
    var draft by remember { mutableStateOf(initial ?: ContactDetails()) }
    // New Contact starts minimal; "More details" reveals the full field set.
    var showAll by remember { mutableStateOf(!isNew) }
    var confirmDelete by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val valid = draft.phones.any { it.number.isNotBlank() } ||
        draft.givenName.isNotBlank() || draft.familyName.isNotBlank() || draft.company.isNotBlank()
    // For a new contact anything non-empty counts as a change, so an
    // intent-prefilled draft can be saved immediately.
    val dirty = draft != (if (isNew) ContactDetails() else initial ?: ContactDetails())

    fun save() {
        if (saving) return
        saving = true
        val cleaned = draft.copy(phones = draft.phones.filter { it.number.isNotBlank() })
        if (isNew) viewModel.create(cleaned) { onSaved() }
        else viewModel.update(cleaned) { onSaved() }
    }

    Scaffold(
        topBar = {
            TopAppBarMMD(
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.Clear, contentDescription = "Cancel", modifier = Modifier.size(32.dp))
                    }
                },
                title = {
                    TextMMD(
                        if (isNew) "New Contact" else "Edit",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    if (isNew) {
                        OutlinedButtonMMD(
                            onClick = ::save,
                            enabled = valid && dirty && !saving,
                            modifier = Modifier.padding(end = 16.dp),
                        ) {
                            TextMMD("Create", fontSize = 18.sp)
                        }
                    } else {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete contact", modifier = Modifier.size(32.dp))
                        }
                        IconButton(onClick = ::save, enabled = valid && dirty && !saving) {
                            Icon(Icons.Outlined.Check, contentDescription = "Save", modifier = Modifier.size(32.dp))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (confirmDelete) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(padding).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(60.dp))
                TextMMD("Delete this contact?", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                TextMMD("This cannot be undone.", fontSize = 20.sp)
                Spacer(modifier = Modifier.height(40.dp))
                OutlinedButtonMMD(onClick = { viewModel.delete(draft.lookupKey) { onDeleted() } }) {
                    TextMMD("Delete", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButtonMMD(onClick = { confirmDelete = false }) {
                    TextMMD("Cancel", fontSize = 20.sp)
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // Existing phones plus one empty row to add another.
            val phoneRows = draft.phones + PhoneEntry()
            phoneRows.forEachIndexed { index, phone ->
                PhoneField(
                    phone = phone,
                    showLabel = showAll || index < phoneRows.lastIndex,
                    onChange = { changed ->
                        val rows = phoneRows.toMutableList()
                        rows[index] = changed
                        draft = draft.copy(phones = rows.filterIndexed { i, p ->
                            i < phoneRows.lastIndex || p.number.isNotBlank()
                        })
                    },
                )
            }

            EditField("First name", draft.givenName, "John") { draft = draft.copy(givenName = it) }
            EditField("Last name", draft.familyName, "Smith") { draft = draft.copy(familyName = it) }

            if (!showAll) {
                Spacer(modifier = Modifier.height(32.dp))
                OutlinedButtonMMD(
                    onClick = { showAll = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextMMD("More details", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                EditField("Name prefix", draft.prefix, "Mr") { draft = draft.copy(prefix = it) }
                EditField("Middle name(s)", draft.middleName, "Jones") { draft = draft.copy(middleName = it) }
                EditField("Name suffix", draft.suffix, "Jr") { draft = draft.copy(suffix = it) }

                Spacer(modifier = Modifier.height(16.dp))
                TextMMD("Email address type", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                TypeDropdown(
                    label = emailTypeLabel(draft.email.type),
                    options = EMAIL_TYPES.map { it to emailTypeLabel(it) },
                    onSelect = { draft = draft.copy(email = draft.email.copy(type = it)) },
                )
                Spacer(modifier = Modifier.height(16.dp))
                DashedDivider()

                EditField("Email", draft.email.address, "example@mail.com", KeyboardType.Email) {
                    draft = draft.copy(email = EmailEntry(it, draft.email.type))
                }
                EditField("Nickname", draft.nickname, "John Smith") { draft = draft.copy(nickname = it) }
                EditField("Company", draft.company, "Company Name") { draft = draft.copy(company = it) }
                EditField("Department", draft.department, "HR") { draft = draft.copy(department = it) }
                EditField("Notes", draft.note, "Anything worth remembering") { draft = draft.copy(note = it) }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun PhoneField(phone: PhoneEntry, showLabel: Boolean, onChange: (PhoneEntry) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showLabel) {
                TypeDropdown(
                    label = phoneTypeLabel(phone.type),
                    options = PHONE_TYPES.map { it to phoneTypeLabel(it) },
                    onSelect = { onChange(phone.copy(type = it)) },
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            PlainTextField(
                value = phone.number,
                placeholder = "Phone number",
                keyboardType = KeyboardType.Phone,
                onChange = { onChange(phone.copy(number = it)) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        DashedDivider()
    }
}

@Composable
private fun TypeDropdown(
    label: String,
    options: List<Pair<Int, String>>,
    onSelect: (Int) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.calmClickable { open = true },
        ) {
            TextMMD(label, fontSize = 22.sp)
            Icon(
                Icons.Outlined.KeyboardArrowDown,
                contentDescription = "Change type",
                tint = Color.Black,
            )
        }
        DropdownMenuMMD(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (value, name) ->
                DropdownMenuItemMMD(
                    text = { TextMMD(name, fontSize = 20.sp) },
                    onClick = {
                        open = false
                        onSelect(value)
                    },
                )
            }
        }
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(16.dp))
        TextMMD(label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        PlainTextField(value, placeholder, keyboardType, onChange, Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        DashedDivider()
    }
}

@Composable
private fun PlainTextField(
    value: String,
    placeholder: String,
    keyboardType: KeyboardType,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (value.isEmpty()) {
            TextMMD(placeholder, fontSize = 22.sp, color = Color.Gray)
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = LocalTextStyle.current.copy(fontSize = 22.sp, color = Color.Black),
            cursorBrush = SolidColor(Color.Black),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
