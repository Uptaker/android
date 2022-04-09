package codes.drinky.testapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    private lateinit var openHistory: Button
    private lateinit var openCamera: Button
    private lateinit var openGallery: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        openHistory = findViewById<Button>(R.id.openHistory)
        openCamera = findViewById<Button>(R.id.openCamera)
        openGallery = findViewById<Button>(R.id.openGallery)
        setListeners()
    }

    private fun setListeners() {
        openHistory.setOnClickListener { openHistoryActivity() }
        openCamera.setOnClickListener { openCameraActivity() }
        openGallery.setOnClickListener { openGalleryActivity() }
    }

    private fun openHistoryActivity() {
        val intent = Intent(this, HistoryActivity::class.java)
        startActivity(intent)
    }

    private fun openGalleryActivity() {
        val intent = Intent(this, ShareGalleryActivity::class.java)
        startActivity(intent)
    }

    private fun openCameraActivity() {
        val intent = Intent(this, ShareCameraActivity::class.java)
        startActivity(intent)
    }

}