package com.icm.taller2movil

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.icm.taller2movil.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var bindingMain: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindingMain = ActivityMainBinding.inflate(layoutInflater)
        val view = bindingMain.root
        setContentView(view)

        bindingMain.imageButtonContactos.setOnClickListener{
            val pasarContactos = Intent(this,ContactosActivity::class.java)
            startActivity(pasarContactos)
        }

        bindingMain.imageButtonCamara.setOnClickListener{
            val pasarcamara = Intent(this,CamaraActivity::class.java)
            startActivity(pasarcamara)
        }


    }
}