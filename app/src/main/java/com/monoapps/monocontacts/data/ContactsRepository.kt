package com.monoapps.monocontacts.data

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts

data class ContactRow(
    val id: Long,
    val lookupKey: String,
    val displayName: String,
    val starred: Boolean,
)

data class PhoneEntry(val number: String = "", val type: Int = Phone.TYPE_MOBILE)

data class EmailEntry(val address: String = "", val type: Int = Email.TYPE_HOME)

data class ContactDetails(
    val id: Long = 0,
    val lookupKey: String = "",
    val rawContactId: Long = 0,
    val givenName: String = "",
    val familyName: String = "",
    val prefix: String = "",
    val middleName: String = "",
    val suffix: String = "",
    val phones: List<PhoneEntry> = emptyList(),
    val email: EmailEntry = EmailEntry(),
    val nickname: String = "",
    val company: String = "",
    val department: String = "",
    val note: String = "",
    val starred: Boolean = false,
) {
    val displayName: String
        get() = listOf(prefix, givenName, middleName, familyName, suffix)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { company.ifBlank { phones.firstOrNull()?.number.orEmpty() } }
}

fun phoneTypeLabel(type: Int): String = when (type) {
    Phone.TYPE_MOBILE -> "Mobile"
    Phone.TYPE_HOME -> "Home"
    Phone.TYPE_WORK -> "Work"
    Phone.TYPE_MAIN -> "Main"
    else -> "Other"
}

val PHONE_TYPES = listOf(Phone.TYPE_MOBILE, Phone.TYPE_HOME, Phone.TYPE_WORK, Phone.TYPE_MAIN, Phone.TYPE_OTHER)

fun emailTypeLabel(type: Int): String = when (type) {
    Email.TYPE_HOME -> "Home"
    Email.TYPE_WORK -> "Work"
    Email.TYPE_MOBILE -> "Mobile"
    else -> "Other"
}

val EMAIL_TYPES = listOf(Email.TYPE_HOME, Email.TYPE_WORK, Email.TYPE_MOBILE, Email.TYPE_OTHER)

/** Data mimetypes this app edits; update() replaces exactly these rows. */
private val MANAGED_MIMETYPES = arrayOf(
    StructuredName.CONTENT_ITEM_TYPE,
    Phone.CONTENT_ITEM_TYPE,
    Email.CONTENT_ITEM_TYPE,
    Nickname.CONTENT_ITEM_TYPE,
    Organization.CONTENT_ITEM_TYPE,
    Note.CONTENT_ITEM_TYPE,
)

class ContactsRepository(private val resolver: ContentResolver) {

    fun contacts(): List<ContactRow> {
        val rows = mutableListOf<ContactRow>()
        resolver.query(
            Contacts.CONTENT_URI,
            arrayOf(Contacts._ID, Contacts.LOOKUP_KEY, Contacts.DISPLAY_NAME_PRIMARY, Contacts.STARRED),
            null,
            null,
            Contacts.SORT_KEY_PRIMARY,
        )?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(2) ?: continue
                rows += ContactRow(c.getLong(0), c.getString(1), name, c.getInt(3) == 1)
            }
        }
        return rows
    }

    fun details(contactId: Long): ContactDetails? {
        var d = ContactDetails(id = contactId)
        var found = false
        resolver.query(
            Contacts.CONTENT_URI,
            arrayOf(Contacts.LOOKUP_KEY, Contacts.STARRED),
            "${Contacts._ID} = ?",
            arrayOf(contactId.toString()),
            null,
        )?.use { c ->
            if (c.moveToFirst()) {
                found = true
                d = d.copy(lookupKey = c.getString(0), starred = c.getInt(1) == 1)
            }
        }
        if (!found) return null

        val phones = mutableListOf<Pair<Long, PhoneEntry>>()
        resolver.query(
            Data.CONTENT_URI,
            arrayOf(Data.RAW_CONTACT_ID, Data.MIMETYPE, Data.DATA1, Data.DATA2, Data.DATA3,
                Data.DATA4, Data.DATA5, Data.DATA6),
            "${Data.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                if (d.rawContactId == 0L) d = d.copy(rawContactId = c.getLong(0))
                when (c.getString(1)) {
                    StructuredName.CONTENT_ITEM_TYPE -> d = d.copy(
                        rawContactId = c.getLong(0),
                        givenName = c.getString(3).orEmpty(),
                        familyName = c.getString(4).orEmpty(),
                        prefix = c.getString(5).orEmpty(),
                        middleName = c.getString(6).orEmpty(),
                        suffix = c.getString(7).orEmpty(),
                    )
                    Phone.CONTENT_ITEM_TYPE ->
                        phones += c.getLong(0) to PhoneEntry(c.getString(2).orEmpty(), c.getInt(3))
                    Email.CONTENT_ITEM_TYPE ->
                        if (d.email.address.isBlank())
                            d = d.copy(email = EmailEntry(c.getString(2).orEmpty(), c.getInt(3)))
                    Nickname.CONTENT_ITEM_TYPE -> d = d.copy(nickname = c.getString(2).orEmpty())
                    Organization.CONTENT_ITEM_TYPE -> d = d.copy(
                        company = c.getString(2).orEmpty(),
                        department = c.getString(6).orEmpty(),
                    )
                    Note.CONTENT_ITEM_TYPE -> d = d.copy(note = c.getString(2).orEmpty())
                }
            }
        }
        // Sync adapters (WhatsApp, Signal) mirror numbers into their own raw
        // contacts; show each number once, preferring the local row.
        val deduped = phones
            .sortedBy { (rawId, _) -> if (rawId == d.rawContactId) 0 else 1 }
            .distinctBy { (_, p) -> p.number.filter(Char::isDigit).takeLast(9) }
            .map { (_, p) -> p }
        return d.copy(phones = deduped)
    }

    fun create(d: ContactDetails) {
        val account = syncAccount()
        val ops = arrayListOf<ContentProviderOperation>()
        ops += ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
            .withValue(RawContacts.ACCOUNT_TYPE, account?.second)
            .withValue(RawContacts.ACCOUNT_NAME, account?.first)
            .build()
        ops += dataInserts(d) { builder ->
            builder.withValueBackReference(Data.RAW_CONTACT_ID, 0)
        }
        resolver.applyBatch(ContactsContract.AUTHORITY, ops)
    }

    fun update(d: ContactDetails) {
        val ops = arrayListOf<ContentProviderOperation>()
        val mimePlaceholders = MANAGED_MIMETYPES.joinToString(",") { "?" }
        ops += ContentProviderOperation.newDelete(Data.CONTENT_URI)
            .withSelection(
                "${Data.RAW_CONTACT_ID} = ? AND ${Data.MIMETYPE} IN ($mimePlaceholders)",
                arrayOf(d.rawContactId.toString(), *MANAGED_MIMETYPES),
            )
            .build()
        ops += dataInserts(d) { builder ->
            builder.withValue(Data.RAW_CONTACT_ID, d.rawContactId)
        }
        resolver.applyBatch(ContactsContract.AUTHORITY, ops)
    }

    private fun dataInserts(
        d: ContactDetails,
        bindRawContact: (ContentProviderOperation.Builder) -> ContentProviderOperation.Builder,
    ): List<ContentProviderOperation> {
        val ops = mutableListOf<ContentProviderOperation>()
        fun insert(mimetype: String, values: Map<String, String>) {
            var b = ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValue(Data.MIMETYPE, mimetype)
            b = bindRawContact(b)
            values.forEach { (k, v) -> b = b.withValue(k, v) }
            ops += b.build()
        }

        if (listOf(d.givenName, d.familyName, d.prefix, d.middleName, d.suffix).any { it.isNotBlank() }) {
            insert(
                StructuredName.CONTENT_ITEM_TYPE,
                buildMap {
                    if (d.givenName.isNotBlank()) put(StructuredName.GIVEN_NAME, d.givenName)
                    if (d.familyName.isNotBlank()) put(StructuredName.FAMILY_NAME, d.familyName)
                    if (d.prefix.isNotBlank()) put(StructuredName.PREFIX, d.prefix)
                    if (d.middleName.isNotBlank()) put(StructuredName.MIDDLE_NAME, d.middleName)
                    if (d.suffix.isNotBlank()) put(StructuredName.SUFFIX, d.suffix)
                },
            )
        }
        d.phones.filter { it.number.isNotBlank() }.forEach { p ->
            insert(
                Phone.CONTENT_ITEM_TYPE,
                mapOf(Phone.NUMBER to p.number, Phone.TYPE to p.type.toString()),
            )
        }
        if (d.email.address.isNotBlank()) {
            insert(
                Email.CONTENT_ITEM_TYPE,
                mapOf(Email.ADDRESS to d.email.address, Email.TYPE to d.email.type.toString()),
            )
        }
        if (d.nickname.isNotBlank()) {
            insert(Nickname.CONTENT_ITEM_TYPE, mapOf(Nickname.NAME to d.nickname))
        }
        if (d.company.isNotBlank() || d.department.isNotBlank()) {
            insert(
                Organization.CONTENT_ITEM_TYPE,
                buildMap {
                    if (d.company.isNotBlank()) put(Organization.COMPANY, d.company)
                    if (d.department.isNotBlank()) put(Organization.DEPARTMENT, d.department)
                },
            )
        }
        if (d.note.isNotBlank()) {
            insert(Note.CONTENT_ITEM_TYPE, mapOf(Note.NOTE to d.note))
        }
        return ops
    }

    /**
     * (name, type) of the first account whose contacts sync adapter uploads
     * (e.g. DAVx5), so new contacts sync instead of staying phone-local.
     * Null means no such account; the contact is created device-only.
     */
    private fun syncAccount(): Pair<String, String>? {
        val uploading = ContentResolver.getSyncAdapterTypes()
            .filter { it.authority == ContactsContract.AUTHORITY && it.supportsUploading() }
            .map { it.accountType }
            .toSet()
        if (uploading.isEmpty()) return null
        resolver.query(
            ContactsContract.Settings.CONTENT_URI,
            arrayOf(ContactsContract.Settings.ACCOUNT_NAME, ContactsContract.Settings.ACCOUNT_TYPE),
            null,
            null,
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(0) ?: continue
                val type = c.getString(1) ?: continue
                if (type in uploading) return name to type
            }
        }
        return null
    }

    fun delete(lookupKey: String) {
        resolver.delete(Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, lookupKey), null, null)
    }

    fun setStarred(contactId: Long, starred: Boolean) {
        resolver.update(
            Contacts.CONTENT_URI,
            ContentValues().apply { put(Contacts.STARRED, if (starred) 1 else 0) },
            "${Contacts._ID} = ?",
            arrayOf(contactId.toString()),
        )
    }
}
