package com.poolguard

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var btnCapture: Button
    private lateinit var resultLayout: LinearLayout
    private lateinit var tvAnalysis: TextView
    private lateinit var btnOk: Button
    private lateinit var btnRetake: Button
    private var imageCapture: ImageCapture? = null
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        previewView = findViewById(R.id.viewFinder)
        btnCapture = findViewById(R.id.btnCapture)
        resultLayout = findViewById(R.id.resultLayout)
        tvAnalysis = findViewById(R.id.tvAnalysis)
        btnOk = findViewById(R.id.btnOk)
        btnRetake = findViewById(R.id.btnRetake)

        if (allPermissionsGranted()) startCamera()
        else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 0)

        btnCapture.setOnClickListener { takePhoto() }
        btnOk.setOnClickListener { Toast.makeText(this, "Prova registrada!", Toast.LENGTH_LONG).show(); finish() }
        btnRetake.setOnClickListener {
            resultLayout.visibility = LinearLayout.GONE
            previewView.visibility = PreviewView.VISIBLE
            btnCapture.visibility = Button.VISIBLE
        }
    }

    private fun startCamera() {
        ProcessCameraProvider.getInstance(this).also { provider ->
            provider.addListener({
                val cp = provider.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
                try { cp.unbindAll(); cp.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture) }
                catch (e: Exception) { Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_LONG).show() }
            }, ContextCompat.getMainExecutor(this))
        }
    }

    private fun takePhoto() {
        val ic = imageCapture ?: return
        val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "POOL_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg")
        ic.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), executor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(r: ImageCapture.OutputFileResults) = analyze(file.absolutePath)
            override fun onError(e: ImageCaptureException) = runOnUiThread { Toast.makeText(this@CameraActivity, "Erro: ${e.message}", Toast.LENGTH_LONG).show() }
        })
    }

    private fun analyze(path: String) {
        try {
            val exif = ExifInterface(path)
            val ll = FloatArray(2)
            val gps = exif.getLatLong(ll)
            val dt = exif.getAttribute(ExifInterface.TAG_DATETIME)
            val bmp = BitmapFactory.decodeFile(path)
            val img = InputImage.fromBitmap(bmp, 0)
            ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS).process(img)
                .addOnSuccessListener { labels ->
                    val det = labels.map { "${it.text} (${(it.confidence*100).toInt()}%)" }
                    val isPool = labels.any { it.text.contains("pool",true) || it.text.contains("water",true) }
                    val isClean = labels.any { it.text.contains("clean",true) || it.text.contains("clear",true) || it.text.contains("blue",true) }
                    val ok = isPool && isClean && gps && dt != null
                    val txt = buildString {
                        appendLine("══════ IA ANALISOU ══════")
                        det.take(5).forEach { appendLine("• $it") }
                        appendLine()
                        appendLine("GPS: ${if(gps) "OK" else "NAO"} | Data: ${dt?: "NAO"}")
                        appendLine(if(ok) "✅ APROVADA!" else "⚠️ REPROVADA")
                    }
                    bmp.recycle()
                    runOnUiThread { showResult(txt, ok) }
                }
                .addOnFailureListener { bmp.recycle(); runOnUiThread { showResult("Erro na IA", false) } }
        } catch (e: Exception) {
            runOnUiThread { showResult("Erro: ${e.message}", false) }
        }
    }

    private fun showResult(txt: String, ok: Boolean) {
        resultLayout.visibility = LinearLayout.VISIBLE
        previewView.visibility = PreviewView.GONE
        btnCapture.visibility = Button.GONE
        tvAnalysis.text = txt
        btnOk.visibility = if (ok) Button.VISIBLE else Button.GONE
        btnRetake.visibility = if (ok) Button.GONE else Button.VISIBLE
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    override fun onDestroy() { super.onDestroy(); executor.shutdown() }
}
