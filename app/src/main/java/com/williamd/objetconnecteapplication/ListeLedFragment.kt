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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
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
        val port = sharedPreferences.getString("pref_port_connection", "8080")
        val minuteIntervalString = sharedPreferences.getString("pref_fetch", "1")
        val minuteInterval = minuteIntervalString?.toLong()

        // Crée l'url du serveur
        serverUrl = "http://$ip:$port"

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
        val scheduler = Executors.newSingleThreadScheduledExecutor()

        // Convertie les minutes en milliseconde
        val intervalInMillis = intervalInMinutes * 60L * 1000L

        // Schedule la task chaque intervalInMinutes minute
        scheduler.scheduleAtFixedRate({
            refreshCouleurs()
        }, 0, intervalInMillis, TimeUnit.MILLISECONDS)
    }

    private fun refreshCouleurs(){
        val thread = Thread {
            val couleursJson = getData("$serverUrl/couleur")
            if(couleursJson != null){
                try {
                    val couleurs = Gson().fromJson(couleursJson, Couleur::class.java)

                    handler.post {
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
        val client = OkHttpClient()

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

    private fun sendPost(stUrl: String, jsonMsg: String){
        var conn: HttpURLConnection? = null
        var outputStream: DataOutputStream? = null
        var inputStream: BufferedReader? = null
        try {
            // Établie la connection
            val url = URL(stUrl)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.doInput = true
            conn.doOutput = true

            // Envoie la requête
            outputStream = DataOutputStream(conn.outputStream)
            outputStream.writeBytes(jsonMsg)
            outputStream.flush()

            // Reçois la réponse
            val responseCode = conn.responseCode
            val responseMessage = conn.responseMessage
            Log.d("Response", "Response Code: $responseCode")
            Log.d("Response", "Response Message: $responseMessage")

            // Lis la réponse
            inputStream = BufferedReader(InputStreamReader(conn.inputStream))
            val response = StringBuilder()
            var line: String?
            while (inputStream.readLine().also { line = it } != null) {
                response.append(line)
            }
            Log.d("Response", "Response Body: $response")

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ERREUR", "Exception: ${e.message}")
        } finally {
            // Ferme les streams et la connection
            try {
                outputStream?.close()
                inputStream?.close()
                conn?.disconnect()
            } catch (e: Exception) {
                Log.e("ERREUR", "Failed to close resources: ${e.message}")
            }
        }
    }
}