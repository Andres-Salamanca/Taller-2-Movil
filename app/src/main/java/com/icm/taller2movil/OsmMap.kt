package com.icm.taller2movil

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import com.icm.taller2movil.databinding.ActivityOsmMapBinding
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.LocationManager
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import java.io.IOException
import kotlin.math.log


class OsmMap : AppCompatActivity() {


    private lateinit var binding:ActivityOsmMapBinding

    private lateinit var sensorManager: SensorManager
    private lateinit var lightSensor: Sensor
    private lateinit var sensorEventListener: SensorEventListener
    private  var longPressedMarker:Marker? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_osm_map)

        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        binding = ActivityOsmMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.map.setTileSource(TileSourceFactory.MAPNIK)
        binding.map.setMultiTouchControls(true)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // Obtiene una referencia al sensor de luminosidad
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (lightSensor == null) {
            Toast.makeText(this, "No se encuentra el sensor de luminosidad", Toast.LENGTH_SHORT).show()
        }
        cambioMapa()


        binding.map.overlays.add(createOverlayEvent())

        binding.editTextText.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (keyEvent?.action == KeyEvent.ACTION_DOWN && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) {
                // Realiza la acción que deseas aquí
                Log.i("lugar", textView.text.toString())
                val locationName = textView.text.toString()
                searchLocationAndMoveMap(locationName)
                return@setOnEditorActionListener true
            }
            false
        }


    }

    fun cambioMapa(){
        sensorEventListener = object : SensorEventListener{
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_LIGHT) {
                    val luminosityValue = event.values[0]

                    if (luminosityValue < 15157.9) {
                        // Cambia el modo del mapa a oscuro
                        binding.map.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
                    } else {
                        // Restaura el modo del mapa a normal
                        binding.map.overlayManager.tilesOverlay.setColorFilter(null)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Puedes manejar cambios en la precisión del sensor aquí si es necesario
            }
        }
    }


    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            binding.map.onResume()
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (location != null){
                val currentLocation = GeoPoint(location.latitude, location.longitude)
                val mapcontroller:IMapController = binding.map.controller

                mapcontroller.setZoom(18.0)
                mapcontroller.setCenter(currentLocation)

                longPressOnMap(currentLocation)
                sensorManager.registerListener(
                    sensorEventListener,
                    lightSensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
                cambioMapa()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.map.onPause()
        sensorManager.unregisterListener(sensorEventListener)
    }

    // Esta funcion sirve para crear el overlay para recibir los eventos en el mapa
    private fun createOverlayEvent():MapEventsOverlay{
        val receiver = object : MapEventsReceiver {
            //override para que no haga nada con un single tap
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                return false
            }

            //override para que cree un marcador cuando hay un longpreess
            override fun longPressHelper(p: GeoPoint): Boolean {
                longPressOnMap(p)
                return true
            }
        }
        return MapEventsOverlay(receiver)
    }

    //funcion para que se ponga el marker en el mapa, quite la parte que quita los otros markers
    private fun longPressOnMap(p: GeoPoint) {

        //longPressedMarker?.let { binding.map.overlays.remove(it) }

        longPressedMarker = createMarker(p, null, R.drawable.baseline_push_pin_24)
        longPressedMarker?.let { binding.map.overlays.add(it) }
        binding.map.invalidate()
    }



    // Es la funcion encargada de crear los markers
    private fun createMarker(p: GeoPoint, desc: String?, iconID: Int): Marker? {
        val geocoder = Geocoder(this)
        var NombreLugar= " "
        val direccioes = geocoder.getFromLocation(p.latitude,p.longitude,1)
        if (direccioes != null) {
            if(direccioes.isNotEmpty()){
                val direccion = direccioes[0]
                NombreLugar = direccion.getAddressLine(0)
            }
            else{
                NombreLugar = "No se encontro el nombre";
            }
        }
        var marker: Marker? = null
        if (binding.map != null) {
            marker = Marker(binding.map)
            marker.title = NombreLugar
            marker.snippet = "Lat: ${p.latitude}, Lon: ${p.longitude}"
            if (iconID != 0) {
                val myIcon = resources.getDrawable(iconID, theme)
                marker.icon = myIcon
            }
            marker.position = p
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.showInfoWindow()
        }
        return marker
    }

    private fun searchLocationAndMoveMap(locationName: String) {

        val geocoder = Geocoder(this)
        try {
            val addressList = geocoder.getFromLocationName(locationName, 1)
            if (addressList != null) {
                if (addressList.isNotEmpty()) {
                    val address = addressList[0]
                    val foundLocation = GeoPoint(address.latitude, address.longitude)

                    // Move the map to the found location
                    val mapController: IMapController = binding.map.controller
                    mapController.setZoom(18.0)
                    mapController.setCenter(foundLocation)

                    // Create a marker at the found location
                    longPressOnMap(foundLocation)
                } else {
                    Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error searching for location", Toast.LENGTH_SHORT).show()
        }
    }



}