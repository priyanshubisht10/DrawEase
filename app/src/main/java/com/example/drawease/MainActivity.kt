package com.example.drawease

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yuku.ambilwarna.AmbilWarnaDialog
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var drawingView: DrawingView
    private lateinit var brushButton: ImageButton
    private lateinit var redColor: ImageButton
    private lateinit var blueColor: ImageButton
    private lateinit var yellowColor: ImageButton
    private lateinit var greenColor: ImageButton
    private lateinit var color_pallette: ImageButton
    private lateinit var galleryButton: ImageButton
    private lateinit var colorPickerButton: ImageButton
    private lateinit var undoButton: ImageButton
    private var currentBrushSize: Int = 10
    private lateinit var saveButton: ImageButton


    private val opengalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            findViewById<ImageView>(R.id.gallery_image).setImageURI(result.data?.data)
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)
        drawingView.changeBrushSize(10.toFloat())

        redColor = findViewById(R.id.red_button)
        yellowColor = findViewById(R.id.yellow_button)
        blueColor = findViewById(R.id.blue_button)
        greenColor = findViewById(R.id.green_button)
        color_pallette = findViewById(R.id.color_pallette)
        undoButton = findViewById(R.id.undo_button)
        colorPickerButton = findViewById(R.id.color_picker_button)
        galleryButton = findViewById(R.id.gallery_button)
        saveButton = findViewById(R.id.save_button)
        brushButton = findViewById(R.id.brush_button)


        redColor.setOnClickListener(this)
        blueColor.setOnClickListener(this)
        color_pallette.setOnClickListener(this)
        yellowColor.setOnClickListener(this)
        greenColor.setOnClickListener(this)
        undoButton.setOnClickListener(this)
        colorPickerButton.setOnClickListener(this)
        galleryButton.setOnClickListener(this)
        saveButton.setOnClickListener(this)



        brushButton.setOnClickListener {
            showBrushChooserDialog()
        }
    }


    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted && permissionName == android.Manifest.permission.READ_EXTERNAL_STORAGE) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                    val pickIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    opengalleryLauncher.launch(pickIntent)
                } else if (isGranted && (permissionName == Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "Permission $permissionName granted", Toast.LENGTH_SHORT)
                        .show()
                    CoroutineScope(IO).launch {
                        saveImage(getBitmapFromView(findViewById(R.id.constraint_l3)))
                    }
                } else {
                    if (permissionName == android.Manifest.permission.READ_EXTERNAL_STORAGE || permissionName == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


    private fun showBrushChooserDialog() {
        val brushDialog = Dialog(this@MainActivity)
        brushDialog.setContentView(R.layout.dialog_brush)
        val seekBarProgress = brushDialog.findViewById<SeekBar>(R.id.dialog_seek_bar)
        val showProgressTv = brushDialog.findViewById<TextView>(R.id.dialog_text_view_progress)
        seekBarProgress.progress = currentBrushSize
        showProgressTv.text = seekBarProgress.progress.toString()

        seekBarProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, p1: Int, p2: Boolean) {
                drawingView.changeBrushSize(seekBar.progress.toFloat())
                showProgressTv.text = seekBar.progress.toString()
                currentBrushSize = seekBar.progress
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }

        })

        brushDialog.show()
    }


    private fun showBgPickerDialog() {
        val dialog = AmbilWarnaDialog(this, Color.GREEN, object : OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog?) {
                TODO("Not yet implemented")
            }

            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                drawingView.setBackgroundColor(color)
            }

        })
        dialog.show()
    }


    private fun showColorPickerDialog() {
        val dialog = AmbilWarnaDialog(this, Color.GREEN, object : OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog?) {
                TODO("Not yet implemented")
            }

            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                drawingView.setColor(color)
            }

        })
        dialog.show()
    }


    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            showRationaleDialog()
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            showRationaleDialog()
        } else {
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }


    private fun showRationaleDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Storage Permission")
            .setMessage("Allow us to access your internal storage")
            .setPositiveButton(R.string.dialog_yes) { dialog, _ ->
                requestPermission.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
                dialog.dismiss()
            }
        builder.create().show()
    }


    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.red_button -> {
                drawingView.setColor(Color.RED)
            }

            R.id.yellow_button -> {
                drawingView.setColor(Color.YELLOW)
            }

            R.id.green_button -> {
                drawingView.setColor(Color.GREEN)
            }

            R.id.blue_button -> {
                drawingView.setColor(Color.BLUE)
            }

            R.id.color_pallette -> {
                showColorPickerDialog()
            }

            R.id.undo_button -> {
                Toast.makeText(this@MainActivity, "undo", Toast.LENGTH_SHORT).show()
                drawingView.undo()
            }

            R.id.color_picker_button -> {
                showBgPickerDialog()
            }

            R.id.gallery_button -> {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    requestStoragePermission()
                } else {
                    val pickIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    opengalleryLauncher.launch(pickIntent)
                }
            }

            R.id.save_button -> {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(this, "IF CALLED", Toast.LENGTH_SHORT).show()
                    requestStoragePermission()
                } else {
                    val layout = findViewById<ConstraintLayout>(R.id.constraint_l3)
                    val bitmap = getBitmapFromView(layout)
                    CoroutineScope(IO).launch {
                        saveImage(bitmap)
                    }

                }
            }

        }
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private suspend fun saveImage(bitmap: Bitmap) {
        val root =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
        val myDir = File("$root/saved_images")
        myDir.mkdir()
        val generator = java.util.Random()
        var n = 10000
        n = generator.nextInt(n)
        val outputfile = File(myDir, "Images-$n.jpg")

        if (outputfile.exists()) {
            outputfile.delete()
        } else {
            try {
                val out = FileOutputStream(outputfile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
                out.close()
            } catch (e: Exception) {
                e.stackTrace
            }
            withContext(Main) {
                Toast.makeText(
                    this@MainActivity,
                    "${outputfile.absoluteFile} saved!",
                    Toast.LENGTH_SHORT
                ).show()

            }
        }

    }

}