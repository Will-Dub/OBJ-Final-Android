package com.williamd.objetconnecteapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.williamd.objetconnecteapplication.databinding.FragmentAccueilBinding
import com.williamd.objetconnecteapplication.databinding.FragmentListeLedBinding

class ListeLedFragment : Fragment() {
    private lateinit var binding: FragmentListeLedBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentListeLedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnModifierConfigurationLedAllume.setOnClickListener{
            val configurationFragment = ModificationLedFragment()

            // Remplace liste led par la configuration
            val transaction = parentFragmentManager.beginTransaction()
            transaction.replace(R.id.flFragment, configurationFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        binding.btnModifierConfigurationLedEteint.setOnClickListener{
            val configurationFragment = ModificationLedFragment()

            // Remplace liste led par la configuration
            val transaction = parentFragmentManager.beginTransaction()
            transaction.replace(R.id.flFragment, configurationFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }
}