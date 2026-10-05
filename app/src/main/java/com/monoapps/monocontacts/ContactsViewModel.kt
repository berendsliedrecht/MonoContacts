package com.monoapps.monocontacts

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.provider.ContactsContract
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.monoapps.monocontacts.data.ContactDetails
import com.monoapps.monocontacts.data.ContactRow
import com.monoapps.monocontacts.data.ContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ContactsRepository(application.contentResolver)

    var hasPermission by mutableStateOf(
        ContextCompat.checkSelfPermission(application, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
    )
        private set

    var contacts by mutableStateOf<List<ContactRow>>(emptyList())
        private set

    private val observer = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) = refresh()
    }
    private var observing = false

    init {
        if (hasPermission) start()
    }

    fun onPermissionGranted() {
        hasPermission = true
        start()
    }

    private fun start() {
        if (!observing) {
            observing = true
            getApplication<Application>().contentResolver
                .registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, observer)
        }
        refresh()
    }

    fun refresh() {
        if (!hasPermission) return
        viewModelScope.launch(Dispatchers.IO) {
            val rows = repo.contacts()
            withContext(Dispatchers.Main) { contacts = rows }
        }
    }

    suspend fun details(contactId: Long): ContactDetails? =
        withContext(Dispatchers.IO) { repo.details(contactId) }

    fun create(d: ContactDetails, onDone: () -> Unit) = mutate(onDone) { repo.create(d) }

    fun update(d: ContactDetails, onDone: () -> Unit) = mutate(onDone) { repo.update(d) }

    fun delete(lookupKey: String, onDone: () -> Unit) = mutate(onDone) { repo.delete(lookupKey) }

    fun setStarred(contactId: Long, starred: Boolean) = mutate({}) { repo.setStarred(contactId, starred) }

    private fun mutate(onDone: () -> Unit, block: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            block()
            val rows = repo.contacts()
            withContext(Dispatchers.Main) {
                contacts = rows
                onDone()
            }
        }
    }

    override fun onCleared() {
        if (observing) getApplication<Application>().contentResolver.unregisterContentObserver(observer)
    }
}
