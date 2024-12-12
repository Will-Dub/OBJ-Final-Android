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
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.williamd.objetconnecteapplication.databinding.FragmentAccueilBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


class AccueilFragment : Fragment() {
    private lateinit var binding: FragmentAccueilBinding
    val handler = Handler(Looper.getMainLooper())
    private lateinit var serverUrl: String
    private var isUpdatingData = false
    private var scheduler: ScheduledExecutorService? = null

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

        // Définie le maximum de la bar de vitesse
        binding.seekBarVitesse.max = 100

        refreshStatus()

        // Lance le refresh automatic
        if (minuteInterval != null) {
            startFetchingTask(minuteInterval)
        }

        // Créer une requête pour exécuter la tâche une seule fois
        val myWorkRequest = OneTimeWorkRequest.Builder(MyWorker::class.java).build()

        // Planifier la tâche avec WorkManager
        WorkManager.getInstance(requireContext()).enqueue(myWorkRequest)

        // Préparer la requête au Worker
        val requete = PeriodicWorkRequest.Builder(
            MyWorker::class.java
            , 15, TimeUnit.MINUTES
        ).build()

        // Lancer la commande au Worker pour qu'il l'exécute
        WorkManager.getInstance(requireContext()).enqueue(requete)

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

    fun startFetchingTask(intervalInMinutes: Long) {
        scheduler = Executors.newSingleThreadScheduledExecutor()

        // Convertie les minutes en milliseconde
        val intervalInMillis = intervalInMinutes * 60L * 1000L

        // Schedule la task chaque intervalInMinutes minute
        scheduler?.scheduleAtFixedRate({
            refreshStatus()
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

    private fun refreshStatus(){
        val thread = Thread {
            val statusJson = getData("$serverUrl/status")
            if(statusJson != null){
                try {
                    val status = Gson().fromJson(statusJson, Status::class.java)

                    handler.post {
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

class MyWorker(context: Context, workerParams: WorkerParameters): Worker(context, workerParams){
    override fun doWork(): Result{
        Log.i("WORKER_ACCUEIL", "Affichage d'un log en arrière-plan")

        return Result.success()
    }
}
