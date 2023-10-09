package com.icm.taller2movil

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.icm.taller2movil.databinding.ActivityContactosBinding
import com.icm.taller2movil.databinding.ActivityMainBinding
import android.Manifest
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log
import android.widget.ListView
import androidx.core.app.ActivityCompat

class ContactosActivity : AppCompatActivity() {

    lateinit var bindingContactos: ActivityContactosBinding
    private val READ_CONTACTS_PERMISSION_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contactos)

        bindingContactos = ActivityContactosBinding.inflate(layoutInflater)
        val view = bindingContactos.root
        setContentView(view)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Si no está concedido, solicita el permiso
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                READ_CONTACTS_PERMISSION_REQUEST
            )
        } else {
            // El permiso ya está concedido, puedes realizar la operación que requiere el permiso
            retrieveContacts()
        }


    }
    private fun retrieveContacts() {
        val contactsList = mutableListOf<String>()
        val projection = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)

        val cursor: Cursor? = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val contactName =
                    it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                Log.i("contacto",contactName)
                contactsList.add(contactName)
            }
        }

        val adapter = ContactsAdapter(this, contactsList)
        bindingContactos.listview.adapter = adapter
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            READ_CONTACTS_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permiso concedido, realiza la operación que requiere el permiso
                    retrieveContacts()
                } else {
                    // Permiso denegado, muestra un mensaje o realiza alguna acción
                    Toast.makeText(
                        this,
                        "El permiso de contactos fue denegado",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}