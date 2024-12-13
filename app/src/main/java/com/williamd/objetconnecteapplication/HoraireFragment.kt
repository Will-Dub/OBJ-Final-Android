package com.williamd.objetconnecteapplication

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.williamd.objetconnecteapplication.databinding.DialogHoraireAjouterBinding
import com.williamd.objetconnecteapplication.databinding.FragmentHoraireBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.internal.concurrent.Task
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.concurrent.TimeUnit


class HoraireFragment : Fragment(), HoraireAdapter.OnDeleteClickListener {
    private lateinit var binding: FragmentHoraireBinding
    private var horaireList: MutableList<Horaire> = ArrayList()
    private lateinit var adapterHoraire: HoraireAdapter
    private var addDialogShowing = false
    private lateinit var scheduler: TaskScheduler

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHoraireBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Charge la liste d'horaire
        horaireList = loadHoraireList()

        //Trie par heure
        horaireList.sortByDescending { it.debut }

        // Cree une instance pour le scheduler
        scheduler = TaskScheduler(requireContext())

        adapterHoraire = HoraireAdapter(requireContext(), horaireList)
        adapterHoraire.setDeleteClickListener(this)
        binding.lvProgrammation.adapter = adapterHoraire

        binding.btnAjouterProgrammation.setOnClickListener{
            showAddHoraireDialog()
        }
    }

    /**
     * Implémente onDeleteClick pour l'adapter
     */
    override fun onDeleteClick(currentItem: Horaire) {
        showDeleteConfirmationDialog(currentItem)
    }

    /**
     * Affiche le dialog de suppression
     */
    private fun showDeleteConfirmationDialog(currentItem: Horaire) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(R.string.horaire_delete_title)
        builder.setMessage(R.string.horaire_delete_message)
        builder.setPositiveButton(R.string.yes) { _, _ ->
            removeHoraire(currentItem)
        }
        builder.setNegativeButton(R.string.no) { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    /**
     * Affiche le dialog pour ajouter un horaire
     */
    private fun showAddHoraireDialog() {
        if (addDialogShowing) return
        addDialogShowing = true

        val dialogBinding = DialogHoraireAjouterBinding.inflate(layoutInflater)
        val dialogView = dialogBinding.root
        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle(R.string.horaire_add_dialog_title)
        val alertDialog = dialogBuilder.show()

        val spinner: Spinner = dialogBinding.spinnerHoraireType
        // Cree un adapter d'array avec la liste de string
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.type_horaire_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Spécifie le layout à utiliser pour le spinner
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            // Applique l'adapter au spinner
            spinner.adapter = adapter
        }

        // Customize le time picker pour être 24 heure au lieu de am-pm
        val simpleTimePicker = dialogBinding.timePickerHoraire
        simpleTimePicker.setIs24HourView(true)

        // Bouton ajout
        dialogBinding.btnSauvegarderHoraire.setOnClickListener {
            val selectedPosition = dialogBinding.spinnerHoraireType.selectedItemPosition

            val typeAction = when (selectedPosition) {
                0 -> {
                    HoraireTypeEnum.ALLUME
                }

                1 -> {
                    HoraireTypeEnum.ETEINT
                }

                else -> {
                    // Choix invalide
                    Toast.makeText(requireContext(), R.string.err_horaire_add_type, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Temps
            val heure = dialogBinding.timePickerHoraire.hour
            val minute = dialogBinding.timePickerHoraire.minute

            // Formatte les minute(ex 05 au lieu de 5)
            val formattedMinute = String.format("%02d", minute)

            // Combine heure et minute
            val temps = "$heure:$formattedMinute"

            // Crée une nouvelle horaire et refresh l'adapter
            val newHoraire = Horaire(temps, typeAction)

            // Ajoute l'horaire à la liste
            addHoraire(newHoraire)

            addDialogShowing = false
            alertDialog.dismiss()
        }

        dialogBinding.btnCancelHoraire.setOnClickListener{
            addDialogShowing = false
            alertDialog.dismiss()
        }

        alertDialog.setOnDismissListener {
            addDialogShowing = false
        }
    }

    /**
     * Fonction pour ajouter un horaire à la liste
     */
    private fun addHoraire(horaire: Horaire){
        // Charge la liste de la mémoire
        val storedHoraireList = loadHoraireList()

        // Ajoute l'horaire
        storedHoraireList.add(horaire)
        horaireList.add(horaire)

        // Sauvegarde la liste
        saveHoraireList(storedHoraireList)

        // Crée la tache
        scheduler.scheduleTask(horaire)

        // Affiche le changement
        horaireList.sortByDescending { it.debut }
        adapterHoraire.notifyDataSetChanged()
    }

    /**
     * Fonction pour enlever un horaire à la liste
     */
    private fun removeHoraire(horaire: Horaire) {
        // Charge la liste de la mémoire
        val newHoraireList = loadHoraireList()

        // Enleve l'horaire
        newHoraireList.removeIf { it.id == horaire.id }
        horaireList.removeIf { it.id == horaire.id }

        // Sauvegarde la liste
        saveHoraireList(newHoraireList)

        // Annule la tache
        scheduler.cancelTask(horaire)

        // Affiche le changement
        horaireList.sortByDescending { it.debut }
        adapterHoraire.notifyDataSetChanged()
    }

    /**
     * Charge la liste d'horaire
     */
    private fun loadHoraireList(): MutableList<Horaire> {
        return try {
            requireContext().openFileInput("horaireList.data").use { fileInputStream ->
                ObjectInputStream(fileInputStream).use { objectInputStream ->
                    objectInputStream.readObject() as MutableList<Horaire>
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            mutableListOf()
        }
    }

    /**
     * Sauvegarder la liste d'horaire
     */
    private fun saveHoraireList(horaireList: MutableList<Horaire>) {
        try {
            requireContext().openFileOutput("horaireList.data", Context.MODE_PRIVATE).use { fileOutputStream ->
                ObjectOutputStream(fileOutputStream).use { objectOutputStream ->
                    objectOutputStream.writeObject(horaireList)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

class TaskScheduler(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun scheduleTask(horaire: Horaire) {
        // Parse le temps de string a time
        val timeComponents = horaire.debut.split(":")
        val hour = timeComponents[0].toInt()
        val minute = timeComponents[1].toInt()

        // Calcul le delaie
        val currentTime = Calendar.getInstance()
        val scheduledTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        // Si le temps est dans le passé ajoute un jour
        if (scheduledTime.before(currentTime)) {
            scheduledTime.add(Calendar.DAY_OF_MONTH, 1)
        }

        val delay = scheduledTime.timeInMillis - currentTime.timeInMillis

        // Cree les donnes de l'entre du work
        val inputData = workDataOf(
            "type" to horaire.type.toString(),
            "debut" to horaire.debut,
            "id" to horaire.id
        )

        // Crée les contraintes
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Crée la requête de work
        val workRequest = OneTimeWorkRequestBuilder<ScheduledWorker>()
            .setInputData(inputData)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag(horaire.id)
            .build()

        // Envoie la requête
        workManager.enqueueUniqueWork(
            horaire.id,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelTask(horaire: Horaire) {
        // Annule une requete avec un id
        workManager.cancelAllWorkByTag(horaire.id)
    }
}

class ScheduledWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    override fun doWork(): Result {
        try {
            // Accède au donnée de work
            val type = inputData.getString("type")
            val debut = inputData.getString("debut")
            val horaireId = inputData.getString("id")

            // Accède au préférences
            val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val ip = sharedPreferences.getString("pref_ip_connection", "10.4.129.18")
            val port = sharedPreferences.getString("pref_port_connection", "8080")

            // Crée l'url du serveur
            val serverUrl = "https://$ip:$port"

            // Choisie le type(allumé ou éteindre)
            var estAllume = true
            if(type == "ETEINT"){
                estAllume = false
            }

            // Envoie la requête
            val result = sendPost("$serverUrl/status", "{\"estAllume\": $estAllume}")

            // Enleve l'horaire de la liste
            if(horaireId != null){
                removeHoraire(horaireId)
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e("ScheduledWorker", "Error executing work: ${e.message}", e)
            return Result.failure()
        }
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
                Log.e("ScheduledSendPost", "Request failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                // Recois la réponse
                try {
                    if (response.isSuccessful) {
                        // Success
                        val responseBody = response.body?.string() ?: ""
                        Log.d("ScheduledSendPost", "Response Code: ${response.code}")
                        Log.d("ScheduledSendPost", "Response Body: $responseBody")
                    } else {
                        // Erreur
                        Log.e("ScheduledSendPost", "Failed with response code: ${response.code}")
                    }
                } catch (e: Exception) {
                    Log.e("ScheduledSendPost", "Exception: ${e.message}")
                }
            }
        })
    }

    private fun removeHoraire(horaireId: String) {
        try {
            // Charge la liste
            val horaireList = loadHoraireList()

            // Enleve un l'horaire
            horaireList.removeIf { it.id == horaireId }

            // Sauvegarde la liste
            saveHoraireList(horaireList)
        } catch (e: IOException) {
            Log.e("ScheduledWorker", "Error removing horaire: ${e.message}")
        }
    }

    // Fonction pour charger la liste d'horaire
    private fun loadHoraireList(): MutableList<Horaire> {
        return try {
            applicationContext.openFileInput("horaireList.data").use { fileInputStream ->
                ObjectInputStream(fileInputStream).use { objectInputStream ->
                    objectInputStream.readObject() as MutableList<Horaire>
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            mutableListOf()
        }
    }

    // Fonction pour sauvegarder la liste d'horaire
    private fun saveHoraireList(horaireList: MutableList<Horaire>) {
        try {
            applicationContext.openFileOutput("horaireList.data", Context.MODE_PRIVATE).use { fileOutputStream ->
                ObjectOutputStream(fileOutputStream).use { objectOutputStream ->
                    objectOutputStream.writeObject(horaireList)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}