package com.gustavozreis.dogvacionalapp

import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.gustavozreis.dogvacionalapp.databinding.ActivityMainBinding
import com.gustavozreis.dogvacionalapp.viewmodels.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var binding: ActivityMainBinding? = null

    private var ivDogImage: ImageView? = null
    private var tvDogPhrase: TextView? = null
    private var cvImageContainer: CardView? = null

    private var btnNewDog: Button? = null
    private var btnShareButton: Button? = null

    private var loadingRotatingLogo: AnimationDrawable? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        setUpViewBindingVariables()

        // These 2 functions will start the loading animation and retrieve the first imagem and phrase
        showRotatingLogoLoading()
        getNewDogFromAPI()

        // Placeholder variable for the image phrase
        var imagePhrase = ""

        // Livedata observer that changes the image phrase
        viewModel.imagePhrase.observe(this) { newPhrase ->
            imagePhrase = newPhrase
        }

        // Livedata observer that changes the dog image
        viewModel.dogObject.observe(this) { newDogObject ->
            Glide.with(this)
                .load(newDogObject?.imgUrl)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Show error if the image loading fails
                        Toast.makeText(
                            this@MainActivity,
                            "Erro ao carregar o Coachorro, verifique se sua conexão está funcionando e tente novamente.",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("TAG", "Image loading error!")
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Change the image textview once the image is loaded
                        tvDogPhrase?.text = imagePhrase
                        return false
                    }

                })
                .into(ivDogImage!!)
        }

        getNewDogFromAPI()

        setupButtonsClickListeners()
    }

    private fun setupButtonsClickListeners() {
        btnNewDog?.setOnClickListener {
            showRotatingLogoLoading()
            getNewDogFromAPI()
            tvDogPhrase?.text = ""
        }

        btnShareButton?.setOnClickListener {
            val currentImageBitmap: Bitmap = createBitmapFromView(ivDogImage, tvDogPhrase)
            var currentImageUri: Uri?
            lifecycleScope.launch {
                currentImageUri = saveImage(currentImageBitmap)
                shareImageUri(currentImageUri)
            }
        }
    }

    private fun setUpViewBindingVariables() {
        ivDogImage = binding?.ivDogImage
        tvDogPhrase = binding?.tvMessage
        btnNewDog = binding?.btnNewdog
        btnShareButton = binding?.btnShare
        cvImageContainer = binding?.cvMain
    }


    /*
    This function invokes the update coachorro of the viewModel
     */
    private fun getNewDogFromAPI() {
        viewModel.getNewCoachorro()
    }

    /*
    This function shows the rotating logo on the imageview when retrieving the image
     */
    private fun showRotatingLogoLoading() {
        ivDogImage?.apply {
            setBackgroundResource(R.drawable.loading_animation)
            loadingRotatingLogo = background as AnimationDrawable
        }
        loadingRotatingLogo?.start()
    }

    /*
    This function creates a bitmap out of the imageview and the phrase textview
     */
    private fun createBitmapFromView(dogPhoto: ImageView?, phrase: TextView?): Bitmap {
        val returnedBitmap =
            Bitmap.createBitmap(dogPhoto!!.width, dogPhoto.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        dogPhoto.draw(canvas)
        phrase?.draw(canvas)
        return returnedBitmap
    }

    /*
  This function saves the image on the cacheDir and returns it URI
   */
    private suspend fun saveImage(image: Bitmap): Uri? {
        var uri: Uri? = null
        withContext(Dispatchers.IO) {
            try {
                val imagesFolder = File(cacheDir, "images")
                imagesFolder.mkdirs() // creates new dir in cachedir

                val file = File( // creates file object with path of created dir
                    imagesFolder, "shared_image.png"
                )

                val fileOutputStream = FileOutputStream(file)

                image.compress( // compress bitmap
                    Bitmap.CompressFormat.PNG,
                    100,
                    fileOutputStream
                )

                fileOutputStream.flush() // send bitmap data to created file
                fileOutputStream.close() // close stream

                uri = FileProvider.getUriForFile( // retrieves the file uri
                    this@MainActivity,
                    "com.gustavozreis.fileprovider",
                    file
                )

            } catch (e: IOException) {
                Toast.makeText(this@MainActivity, "Error while saving the file", Toast.LENGTH_LONG)
                    .show()
                Log.d(TAG, "IOException while saving the file")
            }
        }
        return uri
    }

    /*
    This function shares the image
     */
    private fun shareImageUri(uri: Uri?) {
        val intent = Intent().apply {
            this.action = Intent.ACTION_SEND
            this.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            this.putExtra(Intent.EXTRA_STREAM, uri)
            this.type = "image/png"
        }
        startActivity(Intent.createChooser(intent, "Share"))
    }
}




