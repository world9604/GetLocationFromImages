package com.taein.getlocationfromimages

import android.Manifest.permission.ACCESS_MEDIA_LOCATION
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE
import androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE
import com.taein.getlocationfromimages.MainActivity.Companion.TAG
import com.taein.getlocationfromimages.ui.theme.PermissionTestTheme
import java.io.IOException


class MainActivity : ComponentActivity() {

    companion object {
        const val TAG = "GetLocationFromImages"
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PermissionTestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isGranted by remember { mutableStateOf(false) }
                    val requestPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { permission ->
                        when {
                            permission -> isGranted = true
                            !permission -> isGranted = false
                        }
                    }

                    RequestPermission(requestPermissionLauncher)
                    if (isGranted) RequestImageFromGallery()
                    //if (isGranted) getLocation(LocalContext.current)
                }
            }
        }
    }

    @Composable
    private fun RequestPermission(requestPermissionLauncher: ActivityResultLauncher<String>) {
        when {
            ContextCompat.checkSelfPermission(
                LocalContext.current,
                ACCESS_MEDIA_LOCATION
            ) != PackageManager.PERMISSION_GRANTED -> {
                Log.d(TAG, "PERMISSION_TEST > !checkSelfPermission()")
                SideEffect {
                    requestPermissionLauncher.launch(ACCESS_MEDIA_LOCATION)
                }
            }

            shouldShowRequestPermissionRationale(ACCESS_MEDIA_LOCATION)
            -> {
                Log.d(
                    TAG,
                    "PERMISSION_TEST > shouldShowRequestPermissionRationale(ACCESS_MEDIA_LOCATION)"
                )
                Column(
                    modifier = Modifier
                        .height(100.dp)
                        .width(100.dp)
                ) {
                    Text(text = "ACCESS_MEDIA_LOCATION 권한이 필요합니다.")
                    Button(onClick = { /*TODO*/ }) {
                        Text(text = "cancel")
                    }
                }
            }

            else -> {
                Log.d(TAG, "PERMISSION_TEST > else")
                SideEffect {
                    requestPermissionLauncher.launch(ACCESS_MEDIA_LOCATION)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun RequestImageFromGallery() {
    val context = LocalContext.current
    var imageUris by remember {
        mutableStateOf<List<Uri>>(emptyList())
    }
    val launcher = rememberLauncherForActivityResult(contract =
    ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        imageUris = uris
    }

    Column {
        Button(onClick = {
            launcher.launch("image/*")
        }) {
            Text(text = "Pick image")
        }
        Spacer(modifier = Modifier.height(12.dp))
        imageUris.forEach { imageUri ->
            imageUri.let { photoUri ->
                Log.d(TAG, "imageUri : $photoUri")
                try {
                    context.contentResolver.openInputStream(photoUri).use { inputStream ->
                        val exif = ExifInterface(inputStream!!)
                        Log.d(TAG, "exif.TAG_GPS_LATITUDE: ${exif.getAttribute(TAG_GPS_LATITUDE)}")
                        Log.d(TAG, "exif.TAG_GPS_LONGITUDE: ${exif.getAttribute(TAG_GPS_LONGITUDE)}")
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                val bitmap = remember {
                    mutableStateOf<Bitmap?>(null)
                }
                if (Build.VERSION.SDK_INT < 28) {
                    bitmap.value = MediaStore.Images
                        .Media.getBitmap(context.contentResolver, photoUri)
                } else {
                    val source = ImageDecoder
                        .createSource(context.contentResolver, photoUri)
                    bitmap.value = ImageDecoder.decodeBitmap(source)
                }
                bitmap.value?.let { btm ->
                    Image(
                        modifier = Modifier.size(100.dp),
                        bitmap = btm.asImageBitmap(),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}