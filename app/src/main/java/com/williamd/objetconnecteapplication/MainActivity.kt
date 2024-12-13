package com.williamd.objetconnecteapplication

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.PreferenceManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit
import kotlin.math.abs

val CHANNEL_ID = "Channel1"

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    private lateinit var bottomNavigationView: BottomNavigationView

    private fun getFragment(id: Int) = when (id) {
        R.id.accueil -> AccueilFragment()
        R.id.horaire -> HoraireFragment()
        R.id.reglages -> ReglagesFragment()
        else -> AccueilFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialise les notifications
        demanderPermissionNotification()

        // Crée la requête
        val requete = PeriodicWorkRequestBuilder<NotificationWorker>(
            15, TimeUnit.MINUTES
        ).build()

        // Envoie la tâche
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "SensorMonitoringWork",
                ExistingPeriodicWorkPolicy.KEEP,
                requete
            )
        // Lancer la commande au Worker pour qu'il l'exécute
        WorkManager.getInstance(this).enqueue(requete)

        // Acède à la vue de navigation
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener(this)

        // Affiche l'accueil
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.flFragment, getFragment(R.id.accueil))
                .commit()
            bottomNavigationView.selectedItemId = R.id.accueil
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.flFragment, getFragment(item.itemId))
            .addToBackStack(null)
            .commit()
        return true
    }

    /**
     * Méthode pour demander la permission d'envoyer des notification,
     * nécessaire à partir de l'API 33
     */
    private fun demanderPermissionNotification() {
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            // Lanceur pour demander la permission pour les notifications
            val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                if(!result){
                    Toast.makeText(this, "La permission n'a pas été accordée", Toast.LENGTH_SHORT).show()
                }
            }

            if(ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PermissionChecker.PERMISSION_GRANTED){
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

class NotificationWorker(context: Context, workerParams: WorkerParameters): Worker(context, workerParams){
    private val channelId = "sensor_monitoring_channel"
    private val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

    companion object {
        private const val PREF_LAST_TEMPERATURE = "last_temperature"
        private const val PREF_LAST_HUMIDITY = "last_humidity"
        private const val TEMPERATURE_THRESHOLD = 1.0f  // Threshold de temperature
        private const val HUMIDITY_THRESHOLD = 5.0f     // Threshold d'humidité
    }

    override fun doWork(): Result {
        // Accède au préférences
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val ip = sharedPreferences.getString("pref_ip_connection", "10.4.129.18")
        val port = sharedPreferences.getString("pref_port_connection", "4443")
        val alerteTemperature = sharedPreferences.getBoolean("pref_alerte_temperature", true)
        val alerteHumidite = sharedPreferences.getBoolean("pref_alerte_humidite", true)

        if(!alerteHumidite && !alerteTemperature){
            return Result.success()
        }

        // Crée l'url du serveur
        val serverUrl = "https://$ip:$port"

        // Fait la requête get pour accéder au status
        val statusJson = getData("$serverUrl/status") ?: return Result.failure()

        val status = Gson().fromJson(statusJson, Status::class.java) ?: return Result.failure()

        val (hasChanged, message) = hasDataChanged(status, alerteTemperature, alerteHumidite)

        if (hasChanged) {
            creerChannel()
            val notificationId = 1
            afficherNotification(notificationId, "Alerte", message)

            // Sauvegarde les nouvelles valeurs
            sharedPreferences.edit().apply {
                putFloat(PREF_LAST_TEMPERATURE, status.temperature)
                putFloat(PREF_LAST_HUMIDITY, status.humidite)
                apply()
            }
        }

        return Result.success()
    }

    private fun hasDataChanged(
        newStatus: Status,
        checkTemperature: Boolean,
        checkHumidity: Boolean
    ): Pair<Boolean, String> {
        val lastTemperature = sharedPreferences.getFloat(PREF_LAST_TEMPERATURE, newStatus.temperature)
        val lastHumidity = sharedPreferences.getFloat(PREF_LAST_HUMIDITY, newStatus.humidite)

        val temperatureChange = abs(newStatus.temperature - lastTemperature)
        val humidityChange = abs(newStatus.humidite - lastHumidity)

        val temperatureChanged = checkTemperature && temperatureChange >= TEMPERATURE_THRESHOLD
        val humidityChanged = checkHumidity && humidityChange >= HUMIDITY_THRESHOLD

        val messageBuilder = StringBuilder()

        if (temperatureChanged) {
            messageBuilder.append("La température à changé de ${String.format("%.1f", temperatureChange)}°C")
            if (humidityChanged) messageBuilder.append(" et ")
        }

        if (humidityChanged) {
            messageBuilder.append("L'humidité à changé de ${String.format("%.1f", humidityChange)}%")
        }

        return if (temperatureChanged || humidityChanged) {
            Pair(true, messageBuilder.toString())
        } else {
            Pair(false, "")
        }
    }

    private fun getData(stUrl: String): String?{
        val client: OkHttpClient = OkHttpClient.Builder()
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

    /**
     * Méthode pour afficher une notificatioin
     * @param id Identifiant de la notification
     * @param titre Titre de la notification
     * @param texte Texte de la notification
     */
    private fun afficherNotification(id:Int, titre: String, texte: String){
        // Prépare la notification, choisie ce qui y sera affiché et son niveau de priorité
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentText(titre)
            .setContentText(texte)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Affiche la notification
        with(NotificationManagerCompat.from(applicationContext)) {
            // Vérification de la permission à ce moment
            if(ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED){
                notify(id, builder.build())
            }
        }
    }

    /**
     * Méthode pour créer un canal pour les notifications
     * C'est nécessaire pour les versions 26 et + d'android
     */
    private fun creerChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Sensor Monitoring",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for sensor monitoring notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}

