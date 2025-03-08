package com.example.miradioapp.ui.social

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.miradioapp.RadioApplication
import com.example.miradioapp.databinding.FragmentSocialBinding
import kotlinx.coroutines.launch

class SocialFragment : Fragment() {

    private var _binding: FragmentSocialBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SocialViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSocialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[SocialViewModel::class.java]

        // Cargar configuración de redes sociales
        lifecycleScope.launch {
            val config = (requireActivity().application as RadioApplication)
                .configRepository.getAppConfig()

            // Configurar botones de redes sociales
            binding.btnFacebook.setOnClickListener {
                openLink(config.social_links.facebook)
            }

            binding.btnInstagram.setOnClickListener {
                openLink(config.social_links.instagram)
            }

            binding.btnTwitter.apply {
                visibility = if (config.social_links.twitter.isNotEmpty()) View.VISIBLE else View.GONE
                setOnClickListener {
                    openLink(config.social_links.twitter)
                }
            }

            binding.btnWebsite.apply {
                visibility = if (config.social_links.website.isNotEmpty()) View.VISIBLE else View.GONE
                setOnClickListener {
                    openLink(config.social_links.website)
                }
            }
        }
    }

    private fun openLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}