package com.icm.taller2movil

import android.Manifest
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
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import java.io.File
import java.io.FileWriter
import java.io.IOException




data class LocationData(val latitud: Double, val longitud: Double);
class OsmMap : AppCompatActivity() {

    val LOCATION_PERMISSION_REQUEST_CODE=2
    private lateinit var binding:ActivityOsmMapBinding

    private lateinit var sensorManager: SensorManager
    private lateinit var lightSensor: Sensor
    private lateinit var sensorEventListener: SensorEventListener
    private  var longPressedMarker:Marker? = null
    private  var marcadorPosicionInicial:Marker? = null
    private var ultimaUbicacion: Location? = null
    private var locationManager: LocationManager? = null

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (ultimaUbicacion == null || location.distanceTo(ultimaUbicacion!!) >= 30) {
                val locationData = LocationData(location.latitude, location.longitude)
                savedLocation(locationData)
                ultimaUbicacion = location
                val newLocation = GeoPoint(location.latitude, location.longitude)
                updateMarkerAndMap(newLocation)
            }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {}
    }


    override fun onCreate(savedInstanceState: Bundle?) {

        //Solicitar actualizaciones de ubicacion
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager


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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Si los permisos de ubicación no se han otorgado, solicítalos al usuario.
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }


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
        cambioMapa()
        // Rastear los cambios de ubicación y registrarla cuando se detecta movimiento a más de 30m
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000,
                10f,
                locationListener
            )
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                binding.map.onResume()
                val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (location != null) {
                    val currentLocation = GeoPoint(location.latitude, location.longitude)
                    val mapController: IMapController = binding.map.controller
                    mapController.setZoom(18.0)
                    mapController.setCenter(currentLocation)

                    if (marcadorPosicionInicial == null) {
                        marcadorPosicionInicial = createMarker(currentLocation, "Mi ubicación", R.drawable.baseline_push_pin_24)
                        binding.map.overlays.add(marcadorPosicionInicial)
                    }

                    sensorManager.registerListener(
                        sensorEventListener,
                        lightSensor,
                        SensorManager.SENSOR_DELAY_NORMAL
                    )

                }
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

        // Elimina el marcador anterior (si lo hubiera)
        if (longPressedMarker != marcadorPosicionInicial) {
            longPressedMarker?.let { binding.map.overlays.remove(it) }
        }

        // Crea un nuevo marcador en la ubicación actual
        longPressedMarker = createMarker(p, null, R.drawable.baseline_push_pin_24)
        longPressedMarker?.let { binding.map.overlays.add(it) }
        binding.map.invalidate()

        // Llamamos a la función para guardar el registro
        val newLocation = Location("newLocation")
        newLocation.latitude = p.latitude
        newLocation.longitude = p.longitude

        Calculardistancia(ultimaUbicacion, newLocation)
    }

    private fun Calculardistancia(location1: Location?,location2: Location){
        if (location1 != null) {
            val distanceInMeters = location1.distanceTo(location2)
            val distanceInKilometers = distanceInMeters?.div(1000.0)
            val distanceMessage = "Distancia a marcador: ${"%.2f".format(distanceInKilometers)} km"
            Toast.makeText(this, distanceMessage, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Ubicación actual no disponible", Toast.LENGTH_SHORT).show()
        }


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


    //Función para crear el archivo json y guardar la información en el
    private fun savedLocation(locationData: LocationData) {
        try {
            // Cambiar el directorio de almacenamiento a externo
            val filename = "location_data.json"
            val jsonFile = File(getExternalFilesDir(null), filename)
            val jsonArray: JSONArray

            if (jsonFile.exists()) {
                val jsonString = jsonFile.readText()
                jsonArray = JSONArray(jsonString)
            } else {
                jsonArray = JSONArray()
            }

            val locationObject = JSONObject()
            locationObject.put("Latitud", locationData.latitud)
            locationObject.put("Longitud", locationData.longitud)
            jsonArray.put(locationObject)

            FileWriter(jsonFile).use { fileWriter -> fileWriter.write(jsonArray.toString()) }
        } catch (e: JSONException) {
            e.printStackTrace()
            Toast.makeText(this, "Error saving location data", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error saving location data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateMarkerAndMap(newLocation: GeoPoint) {
        // Elimina el marcador anterior (si lo hubiera)
        longPressedMarker?.let { binding.map.overlays.remove(it) }
        // Crea un nuevo marcador en la nueva ubicación
        longPressedMarker = createMarker(newLocation, null, R.drawable.baseline_push_pin_24)
        // Agrega el nuevo marcador al mapa
        longPressedMarker?.let { binding.map.overlays.add(it) }
        // Centra el mapa en la nueva ubicación
        val mapController: IMapController = binding.map.controller
        mapController.setZoom(18.0)
        mapController.setCenter(newLocation)
        // Llama a la función para guardar el registro
        val newLocationData = LocationData(newLocation.latitude, newLocation.longitude)
        savedLocation(newLocationData)
        // Actualiza la vista del mapa
        binding.map.invalidate()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // El usuario otorgó permisos de ubicación, puedes realizar acciones relacionadas con la ubicación aquí.
                // Por ejemplo, iniciar la solicitud de actualizaciones de ubicación.
            } else {
                // El usuario denegó los permisos de ubicación, puedes manejar esta situación aquí.
                // Por ejemplo, mostrar un mensaje al usuario.
            }
        }
    }



}