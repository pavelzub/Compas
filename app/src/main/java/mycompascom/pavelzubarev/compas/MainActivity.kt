package mycompascom.pavelzubarev.compas

import android.content.pm.ActivityInfo
import android.graphics.drawable.AnimationDrawable
import android.hardware.Sensor
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.View
import android.widget.ImageView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.android.synthetic.main.activity_main.*
import mycompascom.pavelzubarev.compas.R.id.image
import java.lang.Math.abs
import android.view.ViewGroup.MarginLayoutParams




class MainActivity : AppCompatActivity(), SensorEventListener {
    private var currentDegree = 0f
    private val xDelta = 0
    private val yDelta = 0

    private lateinit var sensorManager: SensorManager
    private lateinit var accSensor: Sensor
    private lateinit var magnetSensor: Sensor
    private lateinit var ad: InterstitialAd

    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    private var start = 0L

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

        sensorManager = getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetSensor, SensorManager.SENSOR_DELAY_NORMAL);

        ad = InterstitialAd(this)
        ad.adUnitId = "ca-app-pub-3940256099942544/8691691433"
        ad.loadAd(AdRequest.Builder().addTestDevice("33293CAFEB24FBEA2DB6F6DF355BD150").build())

        dunamicImage.visibility = View.INVISIBLE
        staticImage.visibility = View.INVISIBLE

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        start = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        sensorManager!!.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_GAME)
        sensorManager!!.registerListener(this, magnetSensor, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this, accSensor)
        sensorManager.unregisterListener(this, magnetSensor)
    }


    fun btnClick(view: android.view.View) {
        if (ad.isLoaded()) {

            ad.show()
            dunamicImage.visibility = View.VISIBLE
            staticImage.visibility = View.VISIBLE
            button.visibility = View.INVISIBLE
        }
    }

    var gravity: FloatArray? = null
    var geoMagnetic: FloatArray? = null
    var azimut: Float = 0.toFloat()
    var pitch: Float = 0.toFloat()
    var roll: Float = 0.toFloat()
    var mtop = 0
    var mbottom= 0
    var mleft = 0
    var mright = 0

    override fun onSensorChanged(event: SensorEvent) {
        if (event!!.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            gravity = event!!.values.clone()

            if (dunamicImage.visibility == View.VISIBLE) {
                var mtop = 0
                var mbottom= 0
                var mleft = 0
                var mright = 0
                val marginParams = dunamicImage.layoutParams as MarginLayoutParams
                if (event!!.values[0] > 0)
                    mright = (event!!.values[0] * 10).toInt()
                else
                    mleft = -(event!!.values[0] * 10).toInt()
                if (event!!.values[1] > 0)
                    mtop = (event!!.values[1] * 10).toInt()
                else
                    mbottom= -(event!!.values[1] * 10).toInt()
                marginParams.setMargins(mleft, mtop, mright, mbottom)
                dunamicImage.layoutParams = marginParams

            }
            if (abs(event!!.values[0]) < 1.0f &&  abs(event!!.values[1]) < 1.0f
                    && abs(event!!.values[2]) > 3.0f) {
                textView.text = "ON_TABLE"
            } else {
                textView.text = "NOT_ON_TABLE"
            }
        }
        if (event!!.sensor.type == Sensor.TYPE_MAGNETIC_FIELD)
            geoMagnetic = event!!.values.clone()

        if (gravity != null && geoMagnetic != null) {

            val R = FloatArray(9)
            val I = FloatArray(9)
            val success = SensorManager.getRotationMatrix(R, I, gravity, geoMagnetic)
            if (success) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(R, orientation)
                azimut = 57.29578f * orientation[0]
                pitch = 57.29578f * orientation[1]
                roll = 57.29578f * orientation[2]

                val dist = abs((1.4f * Math.tan(pitch * Math.PI / 180)).toFloat())
                var ra = RotateAnimation(
                        currentDegree,
                        -azimut,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f)

                ra.setDuration(210)
                ra.setFillAfter(true)
                if (abs(abs(currentDegree) - abs(azimut)) > 0.2) {
                    compasImage.startAnimation(ra)
                    currentDegree = -azimut
                }

            }
        }

        fbData(-azimut)
    }

    fun fbData(azimut: Float) {
        var t = System.currentTimeMillis();

        if (abs(start - t) > 700) {
            start = t

            var log = ""
            if ((azimut > 0 && azimut <= 45) || (azimut <= 360 && azimut > 270)) {
                log = "SOUTH"
            } else if (azimut > 45 && azimut <= 135) {
                log = "EAST"
            } else if (azimut > 135 && azimut <= 225) {
                log = "NORTH"
            } else {
                log = "WEST"
            }

            val bundle = Bundle()
            bundle.putString("image_name", "DIR")
            bundle.putString("full_text", log)
            mFirebaseAnalytics!!.logEvent("share_image", bundle)

        }
    }
}
