package codes.drinky.testapp

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import codes.drinky.testapp.databinding.ActivityMainBinding
import codes.drinky.testapp.helpers.ImageConversion
import codes.drinky.testapp.manager.UploadsFileManager
import codes.drinky.testapp.model.Photo
import codes.drinky.testapp.model.Upload
import codes.drinky.testapp.model.Uploads
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.*
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class MainActivity : AppCompatActivity() {

    private lateinit var uploads: Uploads

    private lateinit var openCamera: Button
    private lateinit var openGallery: Button

    private val fileManager = UploadsFileManager(this)

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        uploads = fileManager.getUploads()
        setContentView(binding.root)

        openCamera = findViewById(R.id.openCamera)
        openGallery = findViewById(R.id.openGallery)
        parseJson()

        openCamera.setOnClickListener {
            takeImage()
        }
        openGallery.setOnClickListener { loadImageFromGallery.launch("image/*") }

    }

    private val CLIENT_ID = BuildConfig.IMGUR_CLIENT_ID.toString()

    private fun uploadImageToImgur(image: Bitmap) {
        var shareLink: String
        ImageConversion().getBase64Image(image, complete = { base64Image ->
            GlobalScope.launch(Dispatchers.Default) {
                val url = URL("https://api.imgur.com/3/image")
                val boundary = "Boundary-${System.currentTimeMillis()}"

                val httpsURLConnection =
                    withContext(Dispatchers.IO) { url.openConnection() as HttpsURLConnection }
                httpsURLConnection.setRequestProperty("Authorization", "Client-ID $CLIENT_ID")
                httpsURLConnection.setRequestProperty(
                    "Content-Type",
                    "multipart/form-data; boundary=$boundary"
                )

                httpsURLConnection.requestMethod = "POST"
                httpsURLConnection.doInput = true
                httpsURLConnection.doOutput = true

                var body = ""
                body += "--$boundary\r\n"
                body += "Content-Disposition:form-data; name=\"image\""
                body += "\r\n\r\n$base64Image\r\n"
                body += "--$boundary--\r\n"


                val outputStreamWriter = OutputStreamWriter(httpsURLConnection.outputStream)
                withContext(Dispatchers.IO) {
                    outputStreamWriter.write(body)
                    outputStreamWriter.flush()
                }

                val response = httpsURLConnection.inputStream.bufferedReader()
                    .use { it.readText() }  // defaults to UTF-8
                val jsonObject = JSONTokener(response).nextValue() as JSONObject
                shareLink = jsonObject.getJSONObject("data").getString("link")
                pushToHistoryAfterUpload(shareLink)

            }
        })
    }

    private suspend fun pushToHistoryAfterUpload(url: String) {
        withContext(Dispatchers.Main) {
            val capturedUpload = Upload(System.currentTimeMillis(), url)
            uploads.uploads.add(0, capturedUpload)
            parseJson()
            fileManager.writeToFile(Json.encodeToString(uploads))
            copyToClipboard(url)
        }

    }

    private var latestTmpUri: Uri? = null

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            uploadImageToImgur(ImageConversion().uriToBitmap(latestTmpUri!!, this))
            Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Did not upload", Toast.LENGTH_SHORT).show()
        }

    }

    private fun takeImage() {
        lifecycleScope.launchWhenStarted {
            getTmpFileUri().let { uri ->
                latestTmpUri = uri
                takePhoto.launch(uri)
            }
        }
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".jpg", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        return FileProvider.getUriForFile(applicationContext, "${BuildConfig.APPLICATION_ID}.fileprovider", tmpFile)
    }

    private fun copyToClipboard(text: String) {
        val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("text", text)
        println("Clip data: $text")
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(this, "Success! Link copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private val loadImageFromGallery = registerForActivityResult(ActivityResultContracts.GetContent()) {
        println(it)
    }

    private fun parseJson() {
        try {
            val uploadsView: RecyclerView = findViewById(R.id.uploadList)
            uploadsView.layoutManager = LinearLayoutManager(this)
            val itemAdapter = UploadAdapter(this, uploads.uploads)
            uploadsView.adapter = itemAdapter
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun remove(date: Long, uploads: Uploads): Uploads {
        uploads.uploads.removeIf { it.uploadDate == date }
        return uploads
    }



    private fun savePhoto(fileName: String, bmp: Bitmap): Boolean {
        return try {
            openFileOutput("$fileName.jpg", MODE_PRIVATE).use { stream ->
                if (!bmp.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                    throw IOException("Couldn't save bitmap")
                }
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    private fun loadPhotosToRecyclerView() {
        lifecycleScope.launch {
            val photos = loadPhotos()
            println(photos)
        }
    }

    private suspend fun loadPhotos(): List<Photo> {
        return withContext(Dispatchers.IO) {
            val files = filesDir.listFiles()
            files?.filter { it.canRead() && it.isFile && it.name.endsWith(".jpg") }?.map {
                val bytes = it.readBytes()
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                Photo(it.name, bmp)
            } ?: listOf()
        }
    }

    private fun deletePhoto(fileName: String): Boolean {
        return try {
            deleteFile(fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

}