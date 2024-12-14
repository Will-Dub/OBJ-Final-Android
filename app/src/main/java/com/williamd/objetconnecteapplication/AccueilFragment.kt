package com.williamd.objetconnecteapplication

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.williamd.objetconnecteapplication.databinding.FragmentAccueilBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


class AccueilFragment : Fragment() {
    lateinit var binding: FragmentAccueilBinding
    val handler = Handler(Looper.getMainLooper())
    lateinit var serverUrl: String
    private var isUpdatingData = false
    var scheduler: ScheduledExecutorService? = null
    private var client: OkHttpClient = OkHttpClient.Builder()
        .hostnameVerifier(HostnameVerifier())
        .build()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = context
        if (context != null) {
            val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val ip = sharedPreferences.getString("pref_ip_connection", "10.4.129.18")
            val port = sharedPreferences.getString("pref_port_connection", "4443")
            val minuteIntervalString = sharedPreferences.getString("pref_fetch", "1")
            val minuteInterval = minuteIntervalString?.toLong()

            // Crée l'url du serveur
            serverUrl = "https://$ip:$port"

            // Définie le maximum de la bar de vitesse
            binding.seekBarVitesse.max = 100

            // Lance le refresh automatique
            if (minuteInterval != null) {
                startFetchingTask(minuteInterval)
            }
        }

        binding.btnRefreshStatus.setOnClickListener{
            refreshStatus()

            // Fait une animation
            val rotate = RotateAnimation(
                0f,
                360f,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
            )
            rotate.duration = 250
            rotate.interpolator = LinearInterpolator()

            val image = binding.btnRefreshStatus as ImageView

            image.startAnimation(rotate)
        }

        binding.seekBarVitesse.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (isUpdatingData) return

                // Appelé lors du changement dans le progress de la bar
                val newVitesse = binding.seekBarVitesse.progress

                // Crée un nouveau thread pour envoyer la vitesse
                val thread = Thread {
                    sendPost("$serverUrl/vitesse", "{\"vitesse\": $newVitesse}")
                }
                thread.start()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Appelé lorsque l'utilisateur commence à intéragir avec la bar
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Appelé lorsque l'utilisateur fini d'intéragir avec la bar
            }
        })

        binding.switchVentilation.setOnCheckedChangeListener{ buttonView, isChecked ->
            if (isUpdatingData) return@setOnCheckedChangeListener

            // Crée un nouveau thread pour envoyer le status
            val thread = Thread {
                sendPost("$serverUrl/status", "{\"estAllume\": $isChecked}")
            }
            thread.start()
        }

        binding.switchTemperatureType.setOnCheckedChangeListener{ buttonView, isChecked ->
            if (isUpdatingData) return@setOnCheckedChangeListener

            // Convertie le boolean en string
            val typeTemperature = if (isChecked) "C" else "F"

            // Crée un nouveau thread pour envoyer le status
            val thread = Thread {
                sendPost("$serverUrl/typedegree", "{\"type\": \"$typeTemperature\"}")
            }
            thread.start()
        }

        binding.btnModifierCouleur.setOnClickListener{
            val configurationFragment = ListeLedFragment()

            // Remplace le fragment par la liste led
            val transaction = parentFragmentManager.beginTransaction()
            transaction.replace(R.id.flFragment, configurationFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAccueilBinding.inflate(inflater, container, false)
        return binding.root
    }

    fun setClient(testClient: OkHttpClient) {
        client = testClient
    }

    fun startFetchingTask(intervalInMinutes: Long) {
        scheduler = Executors.newSingleThreadScheduledExecutor()

        // Convertie les minutes en milliseconde
        val intervalInMillis = intervalInMinutes * 60L * 1000L

        // Schedule la task chaque intervalInMinutes minute
        scheduler?.scheduleAtFixedRate({
            refreshStatus()
        }, 0, intervalInMillis, TimeUnit.MILLISECONDS)
    }

    fun stopFetchingTask() {
        scheduler?.apply {
            // Arrête toutes les tâches en cours
            shutdown()
            try {
                // Attends que les taches finissent 2 seconde
                if (!awaitTermination(2, TimeUnit.SECONDS)) {
                    shutdownNow() // Force la fin de la tâche
                }
            } catch (e: InterruptedException) {
                shutdownNow()
            }
        }
        scheduler = null
    }

    fun getData(stUrl: String): String?{
        try{
            val request = Request.Builder()
                .url(stUrl)
                .build()

            client.newCall(request).execute().use{ response: Response ->
                if(!response.isSuccessful){
                    Log.e("ERREUR", "Erreur de connection`${response.code}")
                }else{
                    val responseBody = response.body?.string()
                    Log.d("ResponseBody", "Received response: $responseBody")
                    return responseBody
                }
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            Log.e("ERREUR", e.toString())
        }
        return null
    }

    fun sendPost(stUrl: String, jsonMsg: String) {
        // Prépare la requête
        val body = jsonMsg.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        // Crée la requête
        val request = Request.Builder()
            .url(stUrl)
            .post(body)
            .addHeader("Content-Type", "application/json;charset=UTF-8")
            .addHeader("Accept", "application/json")
            .build()

        // Envoie la requête
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("AccueilSendPost", "Request failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                // Recois la réponse
                try {
                    if (response.isSuccessful) {
                        // Success
                        val responseBody = response.body?.string() ?: ""
                        Log.d("AccueilSendPost", "Response Code: ${response.code}")
                        Log.d("AccueilSendPost", "Response Body: $responseBody")
                    } else {
                        // Erreur
                        Log.e("AccueilSendPost", "Failed with response code: ${response.code}")
                    }
                } catch (e: Exception) {
                    Log.e("AccueilSendPost", "Exception: ${e.message}")
                }
            }
        })
    }

    fun refreshStatus(){
        val thread = Thread {
            val statusJson = getData("$serverUrl/status")
            if(statusJson != null){
                try {
                    val status = Gson().fromJson(statusJson, Status::class.java)

                    handler.post {
                        if(status == null){
                            return@post
                        }

                        isUpdatingData = true
                        val formattedTemperature = getString(R.string.temperature, status.temperature)
                        binding.tvTemperature.text = formattedTemperature

                        val formattedHumidite = getString(R.string.humidite, status.humidite)
                        binding.tvHumidite.text = formattedHumidite

                        binding.switchVentilation.isChecked = status.estAllume
                        binding.seekBarVitesse.progress = status.vitesse.toInt()

                        // Degree C
                        if (status.typeDegree == "C") {
                            binding.switchTemperatureType.isChecked = true
                        }
                        // Degree F
                        else if (status.typeDegree == "F") {
                            binding.switchTemperatureType.isChecked = false
                        }

                        isUpdatingData = false
                    }
                } catch (e: JsonSyntaxException) {
                    Log.e("THREAD_ACCUEIL", "Error parsing JSON: ${e.message}")
                } catch (e: JsonParseException) {
                    Log.e("THREAD_ACCUEIL", "Error parsing JSON structure: ${e.message}")
                }
            }
        }
        thread.start()
    }

    override fun onDestroyView() {
        // Arrête toutes les tâches
        stopFetchingTask()
        super.onDestroyView()
    }

}