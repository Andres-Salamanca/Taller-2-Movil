package com.icm.taller2movil
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class ContactsAdapter(private val context: Context, private val contactsList: List<String>) : BaseAdapter() {

    override fun getCount(): Int {
        return contactsList.size
    }

    override fun getItem(position: Int): Any {
        return contactsList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val contactName = contactsList[position]

        // Inflate the contact item layout
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.itemcontacto, null)

        // Set the contact name in the TextView
        val contactTextView = view.findViewById<TextView>(R.id.textView2)
        contactTextView.text = contactName
        val numero = view.findViewById<TextView>(R.id.numero)
        numero.text = (position + 1).toString()

        return view
    }
}