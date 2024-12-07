package com.williamd.objetconnecteapplication

import android.os.Bundle
import android.preference.PreferenceFragment
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat

class ReglagesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        //Charge les préférences
        setPreferencesFromResource(R.xml.preferences, rootKey)

        //Initialise les préférences
        PreferenceManager.setDefaultValues(requireContext(), R.xml.preferences, false)
    }
}