package com.example.test

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.test.databinding.ActivityMainBinding
import com.example.test.ml.Mobilenetv2Haricot
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import kotlin.math.truncate

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var imageView: ImageView
    private lateinit var button: Button
    private lateinit var tvOutput: TextView
    private lateinit var certaintyOutput: TextView
    private val GALLERY_REQUEST_CODE = 123
    private var class_list = listOf("Dor701 Tc=118 min",
        "Dor701 Tc=160 min",
        "Dor701 Tc=217 min",
        "Dor701 Tc=280 min",
        "Dor701 Tc=62 min",
        "Dor701 Tc=86 min",
        "Dor701 Tc=98 min",
        "Ecapan021 Tc=122 min",
        "Ecapan021 Tc=136 min",
        "Ecapan021 Tc=164 min",
        "Ecapan021 Tc=255 min",
        "Ecapan021 Tc=287 min",
        "Ecapan021 Tc=65 min",
        "Ecapan021 Tc=84 min",
        "GPL190C Tc=120 min",
        "GPL190C Tc=135 min",
        "GPL190C Tc=170 min",
        "GPL190C Tc=235 min",
        "GPL190C Tc=295 min",
        "GPL190C Tc=70 min",
        "GPL190C Tc=95 min",
        "GPL190S Tc=115 min",
        "GPL190S Tc=129 min",
        "GPL190S Tc=155 min",
        "GPL190S Tc=200 min",
        "GPL190S Tc=270 min",
        "GPL190S Tc=58 min",
        "GPL190S Tc=85 min",
        "Macc55 Tc=134 min",
        "Macc55 Tc=200 min",
        "Macc55 Tc=225 min",
        "Macc55 Tc=265  min",
        "Macc55 Tc=340 min",
        "Macc55 Tc=410 min",
        "Macc55 Tc=88 min",
        "NIT4G16187 Tc=110 min",
        "NIT4G16187 Tc=122 min",
        "NIT4G16187 Tc=151 min",
        "NIT4G16187 Tc=210 min",
        "NIT4G16187 Tc=68 min",
        "NIT4G16187 Tc=81 min",
        "NIT4G16187 Tc=90 min",
        "Senegalais Tc=100 min",
        "Senegalais Tc=120 min",
        "Senegalais Tc=180 min",
        "Senegalais Tc=230 min",
        "Senegalais Tc=300 min",
        "Senegalais Tc=70 min",
        "Senegalais Tc=90 min",
        "TY339612 Tc=105 min",
        "TY339612 Tc=120 min",
        "TY339612 Tc=163 min",
        "TY339612 Tc=230min",
        "TY339612 Tc=286min",
        "TY339612 Tc=51 min",
        "TY339612 Tc=82 min"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("TAG", "onCreate start")
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        imageView = binding.imageView
        button = binding.btnCaptureImage
        tvOutput = binding.tvOutput
        certaintyOutput = binding.certaintyOutput
        val buttonLoad = binding.btnLoadImage
//        Log.i("number classe", "${class_list.size}")
//        try {
//            val fileName = "labels.txt"
//            val inputString = application.assets.open(fileName).bufferedReader().use{it.readText()}
//            class_list = inputString.split("\n")
//        } catch(e:Exception){
//            Log.i("error ", "$e")
//        }

        button.setOnClickListener{
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ){
                takePicturePreview.launch(null)
            }
            else{
                requestPermission.launch(android.Manifest.permission.CAMERA)
            }
        }

        /*on cree un photo picker qui va nous permettre de choisir une image
        et d'utiliser son uri dans un callback*/
        val photoPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()){ uri ->

            if(uri != null){
                Log.d("PhotoPicker", "Une jolie photo")
                val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                imageView.setImageBitmap(bitmap)
                outputGenerator(bitmap)
            }else{
                Log.d("PhotoPicker", "L'utilisateur n'a rien choisi")
            }
        }

        buttonLoad.setOnClickListener{
            //on lance le PhotoPicker
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

            /*if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES)
                == PackageManager.PERMISSION_GRANTED){
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                intent.type = "image/*"
                val mimeTypes = arrayListOf("image/jpeg", "image/png", "image/jpg")
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                onresult.launch(intent)
            } else {
                requestPermission.launch(android.Manifest.permission.READ_MEDIA_IMAGES);
            }*/*/


        }

        Log.i("TAG", "onCreate end")
    }

    //request camera permission
    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()){granted->
        if (granted){
            takePicturePreview.launch(null)
        } else {
            Toast.makeText(this, "Permission Denied!! Try again", Toast.LENGTH_SHORT).show()
        }
    }

    //launch camera and take picture
    private  val takePicturePreview = registerForActivityResult(ActivityResultContracts.TakePicturePreview()){bitmap->
        if (bitmap != null){
            imageView.setImageBitmap(bitmap)
            outputGenerator(bitmap)
        }
    }

    //to get image from gallery
    private val onresult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        Log.i("TAG", "This is the result: ${result.data} ${result.resultCode}")
        onResultReceived(GALLERY_REQUEST_CODE, result)
    }

    private fun onResultReceived(requestCode: Int, result: ActivityResult?){
        Log.i("TAG", "onResultReceived start")
        when(requestCode){
            GALLERY_REQUEST_CODE -> {
                if (result?.resultCode == Activity.RESULT_OK){
                    result.data?.data?.let { uri->
                        Log.i("TAG", "onResultReceived: $uri")
                        val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                        imageView.setImageBitmap(bitmap)
                        outputGenerator(bitmap)
                    }
                } else {
                    Log.e("TAG", "onActivityResult: error is selecting image")
                }
            }
        }
        Log.i("TAG", "onResultReceived end")
    }

    private fun outputGenerator(bitmap: Bitmap){
        Log.i("TAG", "outputGenerator start")

        var resized: Bitmap = Bitmap.createScaledBitmap(bitmap, 160, 160, true)

        try{
            //declearing tensorflow lite model variable
            val model = Mobilenetv2Haricot.newInstance(this)

            var tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(resized)

//            var theBuffer = TensorImage.fromBitmap(resized)
            var byteBuffer = tensorImage.buffer

            // Creates inputs for reference.
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 160, 160, 3), DataType.FLOAT32)

            Log.i("shape bytebuffer", byteBuffer.toString())
            Log.i("shape inputFeature0", inputFeature0.buffer.toString())

            inputFeature0.loadBuffer(byteBuffer)

            Log.i("after", " inputFeature" )

            // Runs model inference and gets result.
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer
            Log.i("TAG", "outputFeature0: ${outputFeature0.floatArray.size}")

            var results = getMax(outputFeature0.floatArray)
            val max:Int = results[0] as Int
            val certainty = results[1] as Float * 100
            val foundLabel = class_list[max + 1]
            tvOutput.text = foundLabel
            certaintyOutput.text = String.format("%.2f",certainty) + " %"
            Log.i("TAG", "outputGenerator: ${foundLabel} with $max ")

            // Releases model resources if no longer used.
            model.close()

        } catch (e: Exception){

            Log.i("error", "$e")
        }

        Log.i("TAG", "outputGenerator end")

    }

    private fun getMax(arr:FloatArray) : List<Any>{
        Log.i("TAG", "getMax start" )

        var index = 0
        var min = 0.0f

        for(i in 0..arr.size - 1){
            Log.i("Turn", "$i arr[i] = ${arr[i]}" )
            if(arr[i]>min){
                index = i
                min = arr[i]
            }
        }
        Log.i("TAG", "getMax end" )
        return listOf(index, min)
    }
}