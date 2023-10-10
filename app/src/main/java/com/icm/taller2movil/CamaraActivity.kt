package com.icm.taller2movil

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.icm.taller2movil.databinding.ActivityCamaraBinding
import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.icu.text.SimpleDateFormat
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.util.Date

class CamaraActivity : AppCompatActivity() {

    private lateinit var bindingCamara : ActivityCamaraBinding
    private val CAMERA_STORAGE_PERMISSION_REQUEST = 102
    private lateinit var camerapath: Uri
    /* private val cameraRequest = registerForActivityResult(
         ActivityResultContracts.TakePicture()
     ) { loadImage(camerapath)
         galleryAddPic(camerapath)
     }*/
    private val cameraRequest = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // Load the captured image into ImageView
            loadImage(camerapath)

            // Decode the Bitmap from the Uri
            val imageStream = contentResolver.openInputStream(camerapath)
            val bitmap = BitmapFactory.decodeStream(imageStream)

            // Save the Bitmap to the gallery
            saveBitmapToGallery(bitmap, "ImageFileName")
        } else {
            // Handle the case where image capture was not successful
            Toast.makeText(this, "Image capture canceled or failed", Toast.LENGTH_SHORT).show()
        }
    }





    /*private val GalleryRequest = registerForActivityResult(ActivityResultContracts.GetContent()
    ) { loadImage(camerapath) }*/

    private val GalleryRequest = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            loadImage(uri)
            // You can perform additional operations with the selected image URI if needed
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camara)

        bindingCamara = ActivityCamaraBinding.inflate(layoutInflater)
        val view = bindingCamara.root
        setContentView(view)

        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)

        // Verifica si el permiso de almacenamiento ya está concedido
        val storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        // Lista de permisos que se solicitarán
        val permissionsToRequest = mutableListOf<String>()

        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (storagePermission != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissionsToRequest.isNotEmpty()) { ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                CAMERA_STORAGE_PERMISSION_REQUEST
            )
        } else {
            // Ambos permisos ya están concedidos, puedes realizar la operación que requiere los permisos

            initializeFile()
        }

        bindingCamara.buttonEscoger.setOnClickListener{
            if (permissionsToRequest.isNotEmpty()) { ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                CAMERA_STORAGE_PERMISSION_REQUEST
            )
            } else {
                GalleryRequest.launch("image/*")
            }

        }
        bindingCamara.buttonCamara.setOnClickListener{
            if (permissionsToRequest.isNotEmpty()) { ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                CAMERA_STORAGE_PERMISSION_REQUEST
            )
            } else {
                cameraRequest.launch(camerapath)
            }

        }

    }



    fun initializeFile() {
        val imageFileName: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        try {
            val imageFile = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
            )

            // Save the file path for use with ACTION_VIEW intents
            camerapath = FileProvider.getUriForFile(
                this,
                applicationContext.packageName + ".fileprovider",
                imageFile
            )
        } catch (e: IOException) {
            e.printStackTrace()
            // Handle the error, show a toast, or log the exception
        }
    }

    private fun galleryAddPic(imagepath:Uri?) {
        if (imagepath != null) {
            MediaScannerConnection.scanFile(
                this,
                arrayOf(imagepath.path),
                null
            ) { path, uri ->
                Log.i("ExternalStorage", "Scanned $path:")
                Log.i("ExternalStorage", "-> uri=$uri")
            }
        }else{
            Log.i("no funciono", "-> uri= que mal")
        }
    }



    fun loadImage(imageUri: Uri?) {
        try {
            val imageStream = contentResolver.openInputStream(imageUri!!)
            val originalBitmap = BitmapFactory.decodeStream(imageStream)

            // Rotate the original bitmap by 90 degrees
            val matrix = Matrix()
            matrix.postRotate(90f)

            // Create a new rotated bitmap
            val rotatedBitmap = Bitmap.createBitmap(
                originalBitmap,
                0,
                0,
                originalBitmap.width,
                originalBitmap.height,
                matrix,
                true
            )

            // Set the rotated bitmap to the ImageView
            bindingCamara.imageView2.setImageBitmap(rotatedBitmap)
        } catch (e: IOException) {
            e.printStackTrace()
            // Handle the error, show a toast, or log the exception
        }
    }



    private fun setPic(imagepath: Uri?) {
        val currentPhotoPath = imagepath?.path
        // Get the dimensions of the View
        val targetW: Int = bindingCamara.imageView2.width
        val targetH: Int = bindingCamara.imageView2.height

        val bmOptions = BitmapFactory.Options().apply {
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true

            BitmapFactory.decodeFile(currentPhotoPath, this)

            val photoW: Int = outWidth
            val photoH: Int = outHeight

            // Determine how much to scale down the image
            val scaleFactor: Int = 2 // or any other power of 2 depending on your requirements

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
            inPurgeable = true
        }

        BitmapFactory.decodeFile(currentPhotoPath, bmOptions)?.also { bitmap ->
            bindingCamara.imageView2.setImageBitmap(bitmap)
        }
    }

    private fun rotateImage(bitmap: Bitmap, imagePath: String?): Bitmap {
        val exifInterface = imagePath?.let { ExifInterface(it) }
        val orientation = exifInterface?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90F)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180F)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270F)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }



    fun saveBitmapToGallery(bitmap: Bitmap, displayName: String) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
        }

        val resolver = contentResolver
        var imageUri: Uri? = null

        try {
            // Insert the image into the MediaStore
            imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            // Open an output stream to write the bitmap data to the content provider
            imageUri?.let { uri ->
                resolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
            }

            // Notify the system that a new image has been added
            imageUri?.let { uri ->
                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
            }

            Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_STORAGE_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    // Todos los permisos solicitados fueron concedidos, puedes realizar la operación que requiere los permisos

                    initializeFile()
                } else {
                    // Al menos un permiso fue denegado, muestra un mensaje o realiza alguna acción
                    Toast.makeText(
                        this,
                        "Los permisos de cámara y almacenamiento fueron denegados",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}