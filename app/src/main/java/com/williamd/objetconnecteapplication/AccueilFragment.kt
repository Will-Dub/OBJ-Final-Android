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
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
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
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit


class AccueilFragment : Fragment() {
    private lateinit var binding: FragmentAccueilBinding
    val handler = Handler(Looper.getMainLooper())
    private lateinit var serverUrl: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Accède au preferences
        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("ServerPrefs", Context.MODE_PRIVATE)
        val ip = sharedPreferences.getString("pref_ip_connection", "10.4.129.18")
        val port = sharedPreferences.getInt("pref_port_connection", 8080)

        // Crée l'url du serveur
        serverUrl = "http://$ip:$port"

        // Définie le maximum de la bar de vitesse
        binding.seekBarVitesse.max = 100

        refreshStatus()

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
        }

        binding.seekBarVitesse.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Appelé lors du changement dans le progress de la bar
                val newVitesse = binding.seekBarVitesse.progress

                // Crée un nouveau thread pour envoyer la vitesse
                val thread = Thread {
                    sendPost(serverUrl + "/vitesse", "{\"vitesse\": $newVitesse}")
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
            // Crée un nouveau thread pour envoyer le status
            val thread = Thread {
                sendPost(serverUrl + "/status", "{\"estAllume\": $isChecked}")
            }
            thread.start()
        }

        binding.switchTemperatureType.setOnCheckedChangeListener{ buttonView, isChecked ->
            // Convertie le boolean en string
            val typeTemperature = if (isChecked) "C" else "F"

            // Crée un nouveau thread pour envoyer le status
            val thread = Thread {
                sendPost(serverUrl + "/typedegree", "{\"type\": \"$typeTemperature\"}")
            }
            thread.start()
        }

        binding.btnModifierCouleur.setOnClickListener{
            // Crée un nouveau thread pour executer la requête POST
            val thread = Thread {
                sendPost(serverUrl, "{\"heat\": 1}")
            }
            thread.start()
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAccueilBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun getData(stUrl: String): String?{
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(stUrl)
            .build()
        return try{
            client.newCall(request).execute().use{ response: Response ->
                if(!response.isSuccessful){
                    Log.e("ERREUR", "Erreur de connection`${response.code}")
                    null
                }else{
                    val responseBody = response.body?.string()
                    Log.d("ResponseBody", "Received response: $responseBody")
                    responseBody
                }
            }
        }
        catch(e: Exception){
            e.printStackTrace()
            Log.e("ERREUR", e.toString())
            null
        }
    }

    private fun sendPost(stUrl: String, jsonMsg: String){
        try{
            // Établir la connexion à l'URL et envoyer notre commande json dans une requête post
            val url = URL(stUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.doInput = true
            conn.doOutput = true
            DataOutputStream(conn.outputStream).use { os ->
                os.writeBytes(jsonMsg)
                os.flush()
            }

            conn.disconnect()
        }catch(e: Exception){
            e.printStackTrace()
            Log.e("ERREUR", e.message.toString())
        }
    }

    private fun refreshStatus(){
        val thread = Thread {
            val statusJson = getData("$serverUrl/status")
            if(statusJson != null){
                try {
                    val status = Gson().fromJson(statusJson, Status::class.java)

                    handler.post {
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

}

class MyWorker(context: Context, workerParams: WorkerParameters): Worker(context, workerParams){
    override fun doWork(): Result{
        Log.i("WORKER_ACCUEIL", "Affichage d'un log en arrière-plan")

        return Result.success()
    }
}
