package com.williamd.objetconnecteapplication

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    private lateinit var bottomNavigationView: BottomNavigationView
    private val accueilFragment = AccueilFragment()
    private val horaireFragment = HoraireFragment()
    private val reglagesFragment = ReglagesFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
        bottomNavigationView.selectedItemId = R.id.accueil
    }

    override fun onNavigationItemSelected(@NonNull item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.accueil -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.flFragment, accueilFragment)
                    .commit()
                return true
            }
            R.id.horaire -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.flFragment, horaireFragment)
                    .commit()
                return true
            }
            R.id.reglages -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.flFragment, reglagesFragment)
                    .commit()
                return true
            }
        }
        return false
    }
}