package com.williamd.objetconnecteapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.williamd.objetconnecteapplication.databinding.FragmentListeLedBinding
import com.williamd.objetconnecteapplication.databinding.FragmentModificationLedBinding

class ModificationLedFragment : Fragment() {
    private lateinit var binding: FragmentModificationLedBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentModificationLedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCancel.setOnClickListener{
            val listeFragment = ListeLedFragment()

            // Remplace modification led par liste
            val transaction = parentFragmentManager.beginTransaction()
            transaction.replace(R.id.flFragment, listeFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        binding.btnSauvegarder.setOnClickListener{
            val listeFragment = ListeLedFragment()

            // Remplace modification led par liste
            val transaction = parentFragmentManager.beginTransaction()
            transaction.replace(R.id.flFragment, listeFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }
}