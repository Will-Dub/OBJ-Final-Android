package com.williamd.objetconnecteapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.williamd.objetconnecteapplication.databinding.FragmentHoraireBinding

class HoraireFragment : Fragment() {
    private lateinit var binding: FragmentHoraireBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHoraireBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAjouterProgrammation.setOnClickListener{
            val horaireFormFragment = HoraireAjouterFragment()

            // Remplace la liste des horaire la ajouter une horaire
            val transaction = parentFragmentManager.beginTransaction()
            transaction.replace(R.id.flFragment, horaireFormFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }
}