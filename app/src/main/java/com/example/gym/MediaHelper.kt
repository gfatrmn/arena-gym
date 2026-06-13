package com.example.gym

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.widget.ImageView
import com.bumptech.glide.Glide
import java.io.ByteArrayOutputStream

class MediaHelper(private val context: Context) {

    fun getRcGallery(): Int {
        return REQ_CODE_GALLERY
    }

    private val rcCamera = 100
    private val rcGallery = 101

    fun getRcCamera(): Int { return rcCamera }

    fun bitmapToString(bmp: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Mengompres kualitas gambar menjadi 60% agar hemat penyimpanan server
        bmp.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    fun getBitmapToString(uri: Uri, imv: ImageView): String {
        @SuppressWarnings("deprecation")
        var bmp = MediaStore.Images.Media.getBitmap(this.context.contentResolver, uri)
        val dim = 480 // Resolusi dimensi standar gambar profil bulat

        bmp = if (bmp.height > bmp.width) {
            Bitmap.createScaledBitmap(bmp, (bmp.width * dim).div(bmp.height), dim, true)
        } else {
            Bitmap.createScaledBitmap(bmp, dim, (bmp.height * dim).div(bmp.width), true)
        }

        // Tampilkan gambar ke form menggunakan Glide melingkar
        Glide.with(context).load(bmp).circleCrop().into(imv)
        return bitmapToString(bmp)
    }

    companion object {
        const val REQ_CODE_GALLERY = 100
    }
}