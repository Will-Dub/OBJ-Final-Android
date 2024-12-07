package com.williamd.objetconnecteapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.williamd.objetconnecteapplication.databinding.FragmentHoraireAjouterBinding

class HoraireAjouterFragment : Fragment() {
    private lateinit var binding: FragmentHoraireAjouterBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHoraireAjouterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCancelHoraire.setOnClickListener{
            val horaireFragment = HoraireFragment()

            // Remplace configuration horaire par la liste horaire
            val transaction = parentFragmentManager.beginTransaction()
            transaction.replace(R.id.flFragment, horaireFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        binding.btnSauvegarderHoraire.setOnClickListener{
            val horaireFragment = HoraireFragment()

            // Remplace configuration horaire par la liste horaire
            val transaction = parentFragmentManager.beginTransaction()
            transaction.replace(R.id.flFragment, horaireFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }

}