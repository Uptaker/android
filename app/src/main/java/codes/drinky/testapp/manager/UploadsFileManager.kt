package codes.drinky.testapp.manager

import android.content.Context
import codes.drinky.testapp.model.Uploads
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

class UploadsFileManager(val context: Context) {

    private fun readFile(): String? {
        return try {
            val file = File(context.filesDir, "uploads.json")
            val contents = file.readText()
            contents
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun writeToFile(content: String) {
        val path = context.filesDir
        val file = File(path, "uploads.json")
        file.createNewFile()
        FileOutputStream(file).use {
            it.write(content.toByteArray())
        }
    }

    fun getUploads(): Uploads {
        val uploadsJson = readFile()
        println(uploadsJson)
        return if (uploadsJson != null) {
            Json.decodeFromString(uploadsJson)
        } else {
            Uploads(arrayListOf())
        }
    }
}