package com.example.spoilalert

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.spoilalert.adapters.ProductAdapter
import com.example.spoilalert.databinding.ActivityMainBinding
import com.example.spoilalert.enginebuilder.OpenFoodFactsKtorClient
import com.example.spoilalert.utils.AlarmReceiver
import com.example.spoilalert.utils.JsonConverter
import com.example.spoilalert.utils.UpdateAndSaveImageTask
import com.example.spoilalert.utils.rotateImageProxy
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdSize.AUTO_HEIGHT
import com.google.android.gms.ads.AdSize.FULL_WIDTH
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class MainActivity : ComponentActivity(){ //, OnTouchListener, GestureDetector.OnGestureListener {
    private var adView: AdView? = null
    val ktorclient = OpenFoodFactsKtorClient()
    private var scanBarcode: ActivityResultLauncher<String>? = null
    private var cameraLauncher: ActivityResultLauncher<String>? = null
    private lateinit var binding: ActivityMainBinding
    val driver = AndroidSqliteDriver(Database.Schema, this, "launch.db")
    private val database = Database(driver)
    private val itemQueries = database.itemQueries
    private val dbinfoQueries = database.dBInfoQueries
    private var mRecyclerView: RecyclerView? = null
    private val productQueries = database.productQueries
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var imageCapture: ImageCapture

    private var rewardedAd: RewardedAd? = null
    private final var TAG = "MainActivity"

    lateinit var alarmManager: AlarmManager

    @SuppressLint("QueryPermissionsNeeded")
    override fun onCreate(savedInstanceState: Bundle?) {
//        itemQueries.deleteAll()
//        productQueries.deleteAll()
        logItemsandProducts()
        dbUpdateManager()
        var scans: Int

//        scheduleNotifications(this)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
//        enableEdgeToEdge()
//
        val backgroundScope = CoroutineScope(Dispatchers.IO)
        backgroundScope.launch {
            MobileAds.initialize(this@MainActivity) {}
        }

        val adView = AdView(this)
        adView.adUnitId = "ca-app-pub-3940256099942544/9214589741"
        adView.setAdSize(AdSize(FULL_WIDTH, AUTO_HEIGHT))
        this.adView = adView
        binding.adViewContainer.addView(adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
        loadAdd(adRequest)

        setContentView(binding.root)

        binding.mainAddSlidingDrawer.animateOpen()
        binding.mainMenuSlidingDrawer.animateOpen()
//        gestureDetector = GestureDetector(this, this)

        scanBarcode = registerForActivityResult(
            ActivityResultContracts
                .RequestPermission(),
        ) {
            if (it) {
                val intent = Intent(this, BarcodeScan::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Permission Not Granted", Toast.LENGTH_SHORT).show()
            }
        }

        cameraLauncher = registerForActivityResult(
            ActivityResultContracts
                .RequestPermission(),
        ) {
            if (it) {
                val viewFlipper = binding.myViewFlipper
                viewFlipper.displayedChild = viewFlipper.indexOfChild(binding.flipperMediaCamera.cameraImagePreviewLayout)
                startCamera()
            } else {
                Toast.makeText(this, "Permission Not Granted", Toast.LENGTH_SHORT).show()
            }
        }

        rewardedAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
            override fun onAdClicked() {
                // Called when a click is recorded for an ad.
                Log.d(TAG, "Ad was clicked.")
            }

            override fun onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                // Set the ad reference to null so you don't show the ad a second time.
                Log.d(TAG, "Ad dismissed fullscreen content.")
                rewardedAd = null
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                // Called when ad fails to show.
                Log.e(TAG, "Ad failed to show fullscreen content.")
                rewardedAd = null
            }

            override fun onAdImpression() {
                // Called when an impression is recorded for an ad.
                Log.d(TAG, "Ad recorded an impression.")
            }

            override fun onAdShowedFullScreenContent() {
                // Called when ad is shown.
                Log.d(TAG, "Ad showed fullscreen content.")
            }
        }

        binding.mainMenuStartScanButton.setOnClickListener {
            scanBarcode?.launch(android.Manifest.permission.CAMERA)
        }

        binding.flipperMedia.editImageButton.setOnClickListener{
            cameraLauncher?.launch(android.Manifest.permission.CAMERA)
        }

        binding.flipperMediaCamera.AddImageSaveButton.setOnClickListener{
            val fileName = productQueries.getRecordKey(binding.flipperMedia.tvbarCode.text.toString()).executeAsList()[0]
            imageCapture.takePicture(
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val rotatedImage = rotateImageProxy(image)
                        UpdateAndSaveImageTask(this@MainActivity, fileName, database, rotatedImage).saveImage()

                        binding.flipperMedia.imageView.setImageBitmap(rotatedImage)
                        binding.myViewFlipper.displayedChild = binding.myViewFlipper.indexOfChild(binding.flipperMedia.productView)
                        mStopCamera()
                        Toast.makeText(this@MainActivity, "Image Overwritten", Toast.LENGTH_SHORT).show()
                        binding.flipperMedia.editImageButton.setBackgroundResource(R.drawable.circle_background)
                        productQueries.set_nullcheck(binding.flipperMedia.tvbarCode.text.toString())
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("SpoilAlert", "Error capturing image: ${exception.message}")
                    }
                }
            )
        }

        binding.flipperMedia.tvProductName.setOnClickListener{
            updateProductInfoDialog("ProductName",
                binding.flipperMedia.tvProductName.text.toString(),
                binding.flipperMedia.tvbarCode.text.toString())
        }

        binding.flipperMedia.tvProductBrand.setOnClickListener{

            updateProductInfoDialog("ProductBrand",
                binding.flipperMedia.tvProductBrand.text.toString(),
                binding.flipperMedia.tvbarCode.text.toString())
        }

        binding.mainWatchAdd.setOnClickListener {
            rewardedAd?.let { ad ->
                ad.show(this) {
                    dbinfoQueries.watched_add()
                    scans = dbinfoQueries.get_tokens().executeAsOne().toInt()
                    binding.mainTokenCounter.text = "$scans\ntokens\nremaining"
                    Toast.makeText(applicationContext, "Watched add! 10 tokens Added. $scans tokens remaining", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "User earned the reward.")
                    rewardedAd = null
                    loadAdd(adRequest)
                    val adRequest = AdRequest.Builder().build()
                    adView.loadAd(adRequest)
                }
            } ?: run {
                Log.d(TAG, "The rewarded ad wasn't ready yet.")
            }
        }

        binding.mainMenuSettingsButton.setOnClickListener {
            Toast.makeText(applicationContext, "Placeholder for settings button", Toast.LENGTH_SHORT).show()
        }

        binding.mainMenuInfoButton.setOnClickListener {
            Toast.makeText(applicationContext, "Placeholder for info button", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setAlarm(){
        val myFormat = "yyyyMMdd"
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        val dateCrit = sdf.format(Calendar.getInstance().time).toInt() + 5
        val scans = itemQueries.selectalarm(dateCrit.toDouble()).executeAsList()
//        val timesArray = arrayOf("8:41", "8:42", "8:03", "8:04")
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for ( i in 0 until 5) {
            try{
                Log.e("scanstest_date", scans[i].spoildate)
                Log.e("scanstest_item", scans[i].Item!!)
                val notificationTime = scans[i].spoildate
                val date: Date = sdf.parse(notificationTime)!!
                val timeInMillis: Long = date.time
//
//                val notification = timesArray[i]
//                Log.e("notification", notification)
//                val hour = notification.split(":")[0].toInt()
//                val minutes = notification.split(":")[1].toInt()
//                val calendar = Calendar.getInstance()
//                calendar.set(Calendar.HOUR_OF_DAY, hour)
//                calendar.set(Calendar.MINUTE, minutes)
//                calendar.set(Calendar.SECOND, 0)

                val intent = Intent(this, AlarmReceiver::class.java)
                intent.putExtra("description",scans[i].Item!!)
                intent.putExtra("channel", "Orange Lead-Time")
                val pendingIntent =
                    PendingIntent.getBroadcast(this, i, intent, PendingIntent.FLAG_MUTABLE)
                alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
//                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
//                Toast.makeText(this, "Alarm set Successfully!", Toast.LENGTH_SHORT).show()
                }
            catch (_: IndexOutOfBoundsException) {
                val pendingIntent2 =
                    PendingIntent.getBroadcast(this, i, intent, PendingIntent.FLAG_MUTABLE)
                alarmManager.cancel(pendingIntent2)
            }
        }
    }

    private fun loadAdd (adRequest: AdRequest) {
        RewardedAd.load(this,"ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
        override fun onAdFailedToLoad(adError: LoadAdError) {
            Log.d(TAG, adError.toString())
            rewardedAd = null
        }

        override fun onAdLoaded(ad: RewardedAd) {
            Log.d(TAG, "Ad was loaded.")
            rewardedAd = ad
        }
    })}

    private fun logItemsandProducts() {
        val allitems = itemQueries.selectjson().executeAsList()
        val sb: String = allitems.toString()
        if (sb.length > 4000) {
            Log.v("All Items json query", "sb.length = " + sb.length)
            val chunkCount: Int = sb.length / 4000 // integer division
            for (i in 0..chunkCount) {
                val max = 4000 * (i + 1)
                if (max >= sb.length) {
                    Log.v(
                        "All Items json query",
                        "chunk " + i + " of " + chunkCount + ":" + sb.substring(4000 * i)
                    )
                } else {
                    Log.v(
                        "All Items json query",
                        "chunk " + i + " of " + chunkCount + ":" + sb.substring(4000 * i, max)
                    )
                }
            }
        }
        else {Log.v("All items query", sb)}


        val allproducts = productQueries.selectAll().executeAsList()
        val bs: String = allproducts.toString()
        if (bs.length > 4000) {
            Log.v("All products query", "sb.length = " + bs.length)
            val chunkCount: Int = bs.length / 4000 // integer division
            for (i in 0..chunkCount) {
                val max = 4000 * (i + 1)
                if (max >= bs.length) {
                    Log.v("All products query", "chunk " + i + " of " + chunkCount + ":" + bs.substring(4000 * i))
                } else {
                    Log.v(
                        "All products query",
                        "chunk " + i + " of " + chunkCount + ":" + bs.substring(4000 * i, max)
                    )
                }
            }
        }
    else {Log.v("All products query", bs)}
    Log.d("GetAllDBInfo", dbinfoQueries.selectAll().executeAsList().toString())
    }


    private fun iniBc(){
        val scans = dbinfoQueries.get_tokens().executeAsOne().toInt()
        binding.mainTokenCounter.text = "$scans\ntokens\nremaining"
        val allitems = itemQueries.selectjson().executeAsList()
        mRecyclerView = binding.recyclerView
        val adapter = ProductAdapter(this, JsonConverter(this, allitems).getItemData(), binding)
        mRecyclerView!!.adapter = adapter
        mRecyclerView!!.layoutManager = LinearLayoutManager(this)
        setAlarm()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.flipperMediaCamera.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageCapture, preview)

            } catch(exc: Exception) {
                Log.e("Camera", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun mStopCamera() {
        cameraProvider.unbindAll()
    }


    @SuppressLint("SetTextI18n")
    private fun updateProductInfoDialog(item: String, value: String, barCode: String) {
        // Please make sure to also update updateProductInfoDialog in BarcodeScan
        val columnName = item.replace(", ", "")
        lateinit var newItem : String
        var customlist = listOf("")
        if (columnName == "ProductName") {
            newItem = getString(R.string.updateProductName)
            customlist = productQueries.get_all_products().executeAsList()
        }
        if (columnName == "ProductBrand") {
            newItem = getString(R.string.updateProductBrand)
            customlist = productQueries.get_all_brands().executeAsList()
        }
        if (customlist.isEmpty()) {
            customlist = listOf("")
        }

        val newValue = value.replace(", ", "")
        Log.e("Product Info Update", barCode)
        Log.e("Product Info Update", newItem)
        Log.e("Product Info Update", newValue)

        // create an alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(newItem)
        // set the custom layout
        val adapter: ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_list_item_1, customlist)
        val customLayout: View = layoutInflater.inflate(R.layout.dialog_update_product_info, null)
        val autoCompleteTextView: AutoCompleteTextView = customLayout.findViewById(R.id.editText)
        autoCompleteTextView.setAdapter(adapter)
        builder.setView(customLayout)
        // add a button
        builder.setPositiveButton(
            "Update"
        ) { _, _ -> // do something with response
            val updatedValue = autoCompleteTextView.text.toString()
            Log.e("Product Info new Update", updatedValue)
            if (columnName == "ProductName") {
                productQueries.update_product(updatedValue, newValue, barCode)
                binding.flipperMedia.tvProductName.text = "$updatedValue, "
            }
            if (columnName == "ProductBrand") {
                productQueries.update_brand(updatedValue, newValue, barCode)
                binding.flipperMedia.tvProductBrand.text = updatedValue
            }
        }
        // create and show the alert dialog
        val dialog: AlertDialog = builder.create()
        dialog.setOnDismissListener {
            productQueries.set_nullcheck(barCode)
        }
        dialog.show()
    }


    private fun dbUpdateManager() {
        var versionNr = try {
            dbinfoQueries.get_latest().executeAsOne().toString()
        } catch (_: Exception) {"1"}
        Log.d("Software version:", "Current Software Version: $versionNr")
        if (versionNr == "1") {
            try {
                Database.Schema.migrate(driver, oldVersion = 1, newVersion = 2)
                Log.d("Software version", "Software updated to Version: $versionNr")
            } catch (_: RuntimeException) {}
            versionNr = "2"
        }
        if (versionNr == "2") {
            try {
                Database.Schema.migrate(driver, oldVersion = 2, newVersion = 3)
                Log.d("Software version", "Software updated to Version: $versionNr")
            } catch (_: RuntimeException) {
                Log.e("Software version", "Software failed to update to Version: $versionNr")
            }
            versionNr = "3"
        }
        if (versionNr == "3") {
            try {
                Database.Schema.migrate(driver, oldVersion = 3, newVersion = 4)
                versionNr = "4"
                Log.d("Software version", "Software updated to Version: $versionNr")
            } catch (_: RuntimeException) {
                Log.e("Software version", "Software failed to update to Version: $versionNr")
            }
        }
        if (versionNr == "4") {
            try {
                Database.Schema.migrate(driver, oldVersion = 4, newVersion = 5)
                versionNr = "5"
                Log.d("Software version", "Software updated to Version: $versionNr")
            } catch (e: RuntimeException) {
                if (e.toString() == "android.database.sqlite.SQLiteException: duplicate column name: barCode_original (code 1 SQLITE_ERROR): , while compiling: ALTER TABLE product_data ADD COLUMN barCode_original TEXT NOT NULL DEFAULT 0") {
                    dbinfoQueries.update(5)
                    Log.d("Software version", "Software updated to Version: 5")}
                else {
                    Log.e("Software version", "Software failed to update to Version: $versionNr")
                }
            }
        }
        if (versionNr == "5") {
            try {
                Database.Schema.migrate(driver, oldVersion = 5, newVersion = 6)
                versionNr = "6"
                Log.d("Software version", "Software updated to Version: $versionNr")
            } catch (_: RuntimeException) {
                Log.e("Software version", "Software failed to update to Version: $versionNr")
            }
        }

        if (versionNr == "6") {
            try {
                Database.Schema.migrate(driver, oldVersion = 6, newVersion = 7)
                versionNr = "7"
                Log.d("Software version", "Software updated to Version: $versionNr")
            } catch (_: RuntimeException) {
                Log.e("Software version", "Software failed to update to Version: $versionNr")
            }
            // DO NOT FORGET TO SET INITIAL TABLE GENERATION DB VERSION in DBInfo IF UPDATING. CURRENTLY SET TO VERSION 7
            // DO NOT FORGET TO UPDATE SOFTWARE VERSION IN MIGRATION FILE
        }
    }

    override fun onResume() {
        super.onResume()
        Log.e("resumed main?", "yess")
        iniBc()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val viewFlipper = binding.myViewFlipper
        if (viewFlipper.displayedChild == viewFlipper.indexOfChild(binding.flipperMedia.productView)){
            returnToMain()}
        else if (viewFlipper.displayedChild == viewFlipper.indexOfChild(binding.flipperMediaCamera.cameraImagePreviewLayout)){
            mStopCamera()
            viewFlipper.displayedChild = viewFlipper.indexOfChild(binding.flipperMedia.productView)}
        else if (binding.mainAddSlidingDrawer.isOpened) {binding.mainAddSlidingDrawer.animateClose()}
        else if (binding.mainMenuSlidingDrawer.isOpened) {binding.mainMenuSlidingDrawer.animateClose()}
        else {super.onBackPressed()
            Log.e("return to main?", "yessss")}
        //super.onBackPressed();
    }

    private fun returnToMain() {
        Log.e("return to main?", "yessss")
        iniBc()
        binding.myViewFlipper.displayedChild = binding.myViewFlipper.indexOfChild(binding.main)
        val scans = dbinfoQueries.get_tokens().executeAsOne().toInt()
        binding.mainTokenCounter.text = "$scans\ntokens\nremaining"
    }
}