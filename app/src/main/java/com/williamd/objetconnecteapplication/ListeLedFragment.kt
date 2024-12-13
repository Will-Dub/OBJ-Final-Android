package com.williamd.objetconnecteapplication

import android.app.Dialog
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.williamd.objetconnecteapplication.databinding.FragmentListeLedBinding
import com.williamd.objetconnecteapplication.databinding.RgbLayoutDialogBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class ListeLedFragment : Fragment() {
    private lateinit var binding: FragmentListeLedBinding
    private var selectedLedState = ""
    val handler = Handler(Looper.getMainLooper())
    private lateinit var serverUrl: String
    private var currentCouleurLedAllume = "#000000"
    private var currentCouleurLedEteint = "#000000"
    private val rgbLayoutDialogBinding : RgbLayoutDialogBinding by lazy {
        RgbLayoutDialogBinding.inflate(layoutInflater)
    }
    private var scheduler: ScheduledExecutorService? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentListeLedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Accède au preferences
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val ip = sharedPreferences.getString("pref_ip_connection", "10.4.129.18")
        val port = sharedPreferences.getString("pref_port_connection", "4443")
        val minuteIntervalString = sharedPreferences.getString("pref_fetch", "1")
        val minuteInterval = minuteIntervalString?.toLong()

        // Crée l'url du serveur
        serverUrl = "https://$ip:$port"

        refreshCouleurs()

        // Lance le refresh automatic
        if (minuteInterval != null) {
            startFetchingTask(minuteInterval)
        }

        // Crée le dialog pour changer la couleur
        val rgbDialog = Dialog(requireContext()).apply {
            setContentView(rgbLayoutDialogBinding.root)
            window!!.setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setCancelable(false)
        }

        setOnSeekBar(
            "R",
            rgbLayoutDialogBinding.redLayout.typeText,
            rgbLayoutDialogBinding.redLayout.seekBar,
            rgbLayoutDialogBinding.redLayout.colorValueText,
            )
        setOnSeekBar(
            "G",
            rgbLayoutDialogBinding.greenLayout.typeText,
            rgbLayoutDialogBinding.greenLayout.seekBar,
            rgbLayoutDialogBinding.greenLayout.colorValueText,
        )
        setOnSeekBar(
            "B",
            rgbLayoutDialogBinding.blueLayout.typeText,
            rgbLayoutDialogBinding.blueLayout.seekBar,
            rgbLayoutDialogBinding.blueLayout.colorValueText,
        )
        rgbLayoutDialogBinding.cancelBtn.setOnClickListener{
            rgbDialog.dismiss()
        }

        rgbLayoutDialogBinding.pickBtn.setOnClickListener{
            val newHex = getHexColor()
            val thread = Thread {
                if(selectedLedState == "allume"){
                    currentCouleurLedAllume = newHex
                    setRGBColor(binding.viewConfigurationLedAllume, newHex)
                    sendPost("$serverUrl/couleur", "{\"allume\": \"$newHex\"}")
                }else if(selectedLedState == "eteint"){
                    currentCouleurLedEteint = newHex
                    setRGBColor(binding.viewConfigurationLedEteint, newHex)
                    sendPost("$serverUrl/couleur", "{\"eteint\": \"$newHex\"}")
                }
            }
            thread.start()
            rgbDialog.dismiss()
        }

        binding.btnModifierConfigurationLedAllume.setOnClickListener{
            selectedLedState = "allume"
            val (red, green, blue) = hexToRgb(currentCouleurLedAllume)

            // Change les seekbars
            rgbLayoutDialogBinding.redLayout.seekBar.progress = red
            rgbLayoutDialogBinding.greenLayout.seekBar.progress = green
            rgbLayoutDialogBinding.blueLayout.seekBar.progress = blue

            rgbDialog.show()
        }

        binding.btnModifierConfigurationLedEteint.setOnClickListener{
            selectedLedState = "eteint"

            val (red, green, blue) = hexToRgb(currentCouleurLedEteint)

            // Change les seekbars
            rgbLayoutDialogBinding.redLayout.seekBar.progress = red
            rgbLayoutDialogBinding.greenLayout.seekBar.progress = green
            rgbLayoutDialogBinding.blueLayout.seekBar.progress = blue

            rgbDialog.show()
        }
    }

    private fun setOnSeekBar(type: String, typeText: TextView, seekBar: SeekBar, colorText: TextView){
        typeText.text = type
        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                colorText.text = seekBar.progress.toString()
                setRGBColor(rgbLayoutDialogBinding.colorView, getHexColor())
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}

            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
    }

    private fun getHexColor(): String{
        return String.format(
            "#%02x%02x%02x",
            rgbLayoutDialogBinding.redLayout.seekBar.progress,
            rgbLayoutDialogBinding.greenLayout.seekBar.progress,
            rgbLayoutDialogBinding.blueLayout.seekBar.progress
        )
    }

    private fun hexToRgb(hex: String): Triple<Int, Int, Int> {
        // Remplace le #
        val cleanedHex = if (hex.startsWith("#")) hex.substring(1) else hex

        // Accède au r, g et b
        val red = Integer.parseInt(cleanedHex.substring(0, 2), 16)
        val green = Integer.parseInt(cleanedHex.substring(2, 4), 16)
        val blue = Integer.parseInt(cleanedHex.substring(4, 6), 16)

        return Triple(red, green, blue)
    }

    private fun setRGBColor(view: View, hex: String){
        view.setBackgroundColor(Color.parseColor(hex))
    }

    fun startFetchingTask(intervalInMinutes: Long) {
        scheduler = Executors.newSingleThreadScheduledExecutor()

        // Convertie les minutes en milliseconde
        val intervalInMillis = intervalInMinutes * 60L * 1000L

        // Schedule la task chaque intervalInMinutes minute
        scheduler?.scheduleAtFixedRate({
            refreshCouleurs()
        }, 0, intervalInMillis, TimeUnit.MILLISECONDS)
    }

    private fun stopFetchingTask() {
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

    private fun refreshCouleurs(){
        val thread = Thread {
            val couleursJson = getData("$serverUrl/couleur")
            if(couleursJson != null){
                try {
                    val couleurs = Gson().fromJson(couleursJson, Couleur::class.java)

                    handler.post {
                        if(couleurs == null){
                            return@post
                        }

                        // Change les couleurs
                        currentCouleurLedAllume = couleurs.allume
                        binding.viewConfigurationLedAllume.setBackgroundColor(Color.parseColor(couleurs.allume))

                        currentCouleurLedEteint = couleurs.eteint
                        binding.viewConfigurationLedEteint.setBackgroundColor(Color.parseColor(couleurs.eteint))
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

    private fun getData(stUrl: String): String?{
        val client = OkHttpClient.Builder()
            .hostnameVerifier(HostnameVerifier())
            .build()

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

    private fun sendPost(stUrl: String, jsonMsg: String) {
        val client: OkHttpClient = OkHttpClient.Builder()
            .hostnameVerifier(HostnameVerifier())
            .build()

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
                Log.e("LedSendPost", "Request failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                // Recois la réponse
                try {
                    if (response.isSuccessful) {
                        // Success
                        val responseBody = response.body?.string() ?: ""
                        Log.d("LedSendPost", "Response Code: ${response.code}")
                        Log.d("LedSendPost", "Response Body: $responseBody")
                    } else {
                        // Erreur
                        Log.e("LedSendPost", "Failed with response code: ${response.code}")
                    }
                } catch (e: Exception) {
                    Log.e("LedSendPost", "Exception: ${e.message}")
                }
            }
        })
    }

    override fun onDestroyView() {
        // Arrête toutes les tâches
        stopFetchingTask()
        super.onDestroyView()
    }
}