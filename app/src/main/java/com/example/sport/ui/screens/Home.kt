package com.example.sport.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.sport.R
import com.example.sport.data.models.sport.SportItem
import com.example.sport.databinding.FragmentHomeBinding
import com.example.sport.ui.adapters.SportCardAdapter
import com.example.sport.ui.uistate.HomeScreenUiState
import com.example.sport.ui.viewmodels.HomeViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.launch

class Home : Fragment(R.layout.fragment__home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = checkNotNull(_binding)
    private val homeViewModel: HomeViewModel by activityViewModels { HomeViewModel.Factory }

    private val fusedLocationProviderClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireContext())
    }

    @SuppressLint("MissingPermission")
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                fusedLocationProviderClient.lastLocation.addOnSuccessListener {
                    if (it != null)
                        homeViewModel.refreshWeather(it)
                    else {
                        Toast.makeText(requireContext(), "Введите город", Toast.LENGTH_LONG).show()
//                        findNavController().navigate(R.id.action_homeScreen_to_settings)
                        homeViewModel.hideSplash()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Введите город", Toast.LENGTH_LONG).show()
                homeViewModel.hideSplash()
//                findNavController().navigate(R.id.action_homeScreen_to_settings)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.uiState.collect { uiState ->
                    when (uiState) {
                        is HomeScreenUiState.Content -> {
                            showUiContent(
                                currentTemperature = uiState.temperature,
                                maxTemp = uiState.temperatureMax,
                                minTemp = uiState.temperatureMin,
                                currentPrecipitation = uiState.precipitation,
                                weatherIcon = uiState.weatherIcon,
                                city = uiState.city,
//                                stories = uiState.storiesCards,
                                sportCards = uiState.sportCards
                            )
                        }

                        HomeScreenUiState.NoLocation -> noLocation()
                        HomeScreenUiState.Error -> {}
                        HomeScreenUiState.Loading -> showLoading()
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            toolbar.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_item__home_settings -> {
                        findNavController().navigate(R.id.action_homeScreen_to_settings)
                        true
                    }

                    else -> false
                }
            }
            swipeRefreshLayout.setOnRefreshListener {
                swipeRefreshLayout.isRefreshing = false
                if (checkLocationPermission())
                    fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null)
                            homeViewModel.refreshWeather(location)
                        else {
                            Toast.makeText(requireContext(), "Введите город", Toast.LENGTH_LONG)
                                .show()
                            homeViewModel.hideSplash()
                        }
                    }
                else
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
    }

    private fun showUiContent(
        currentTemperature: Int,
        maxTemp: Int,
        minTemp: Int,
        currentPrecipitation: String,
        weatherIcon: String,
        city: String,
//        stories: List<Story>,
        sportCards: List<SportItem>
    ) {
        hideLoading()
        binding.apply {
            val imageLoader = ImageLoader.Builder(requireContext())
                .components {
                    if (SDK_INT >= 28)
                        add(ImageDecoderDecoder.Factory())
                    else
                        add(GifDecoder.Factory())
                }.build()
//            imageMascot.load(R.drawable.all_football, imageLoader) { crossfade(500) }
            composeWeatherInfo.setContent {
                MaterialTheme {
                    TopHeader(
                        currentTemperature = currentTemperature,
                        city = city,
                        currentPrecipitation = currentPrecipitation,
                        icon = getWeatherIcon(weatherIcon)
                    )
                }
            }
//            textViewCurrentPrecipitation.text =
//                currentPrecipitation.replaceFirstChar { it.uppercase() }
//            textViewCurrentTemperature.text =
//                if (currentTemperature > 0)
//                    "+${getString(R.string.temperature_mask, currentTemperature)}"
//                else
//                    getString(R.string.temperature_mask, currentTemperature)
//            textViewTempMaxMin.text = getString(R.string.temp_mask_max_min, maxTemp, minTemp)
//            textViewCity.text = city
//            imageWeatherIcon.load(getWeatherIcon(weatherIcon)) { crossfade(500) }
////            recyclerViewStories.adapter = StoryCardAdapter(stories)
            recyclerViewSports.adapter = SportCardAdapter(sportCards, imageLoader) {
                val bundle = bundleOf("sportId" to it)
                findNavController().navigate(R.id.action_homeScreen_to_sportDetail, bundle)
            }
//            nestedScrollView.setOnScrollChangeListener(
//                NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
//                    if (scrollY > textViewCurrentPrecipitation.bottom)
//                        toolbar.title =
//                            if (currentTemperature > 0)
//                                "+${getString(R.string.temperature_mask, currentTemperature)}, $currentPrecipitation"
//                            else
//                                "${getString(R.string.temperature_mask, currentTemperature)}, $currentPrecipitation"
//                    else
//                        toolbar.title = getString(R.string.app_name)
//                })
        }
    }

    @Composable
    private fun TopHeader(
        currentTemperature: Int,
        city: String,
        currentPrecipitation: String,
        icon: Int
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CardInfo(
                currentTemperature = currentTemperature,
                city = city,
                currentPrecipitation = currentPrecipitation,
                icon = icon
            )
            Image(
                painter = painterResource(id = R.drawable.mascot),
                contentDescription = stringResource(id = R.string.app_name),
                modifier = Modifier.padding(8.dp)
            )
        }
    }

    @Composable
    private fun CardInfo(
        currentTemperature: Int,
        city: String,
        currentPrecipitation: String,
        icon: Int,
    ) {
        Card {
            Surface {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Image(
                        painterResource(id = icon),
                        contentDescription = currentPrecipitation,
                        modifier = Modifier
                            .padding(top = 16.dp, bottom = 8.dp)
                            .height(100.dp)
                    )
                    Text(
                        text =
                        if (currentTemperature > 0)
                            "+${getString(R.string.temperature_mask, currentTemperature)}"
                        else
                            getString(R.string.temperature_mask, currentTemperature),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                    )
//            Text(
//                text = city,
//                style = MaterialTheme.typography.headlineLarge,
//                textAlign = TextAlign.Center,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(vertical = 8.dp)
//            )
                    Text(
                        text = currentPrecipitation.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 16.dp)
                    )
                }
            }
        }
    }

    private fun noLocation() {
        hideLoading()
        if (checkLocationPermission())
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                if (location != null)
                    homeViewModel.refreshWeather(location)
//                else
                // todo: Найти город
            }
        else
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun checkLocationPermission(): Boolean {
        return when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> true

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) -> {
                showLocationRequiredDialog()
                false
            }

            else -> false
        }
    }

    private fun showLocationRequiredDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.location_required)
            .setMessage("Для определения погоды приложению необходим доступ к вашему местоположению")
            .setPositiveButton(R.string.allow) { dialog, _ ->
                dialog.cancel()
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            .setNegativeButton(R.string.not_allow) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun getWeatherIcon(icon: String): Int {
        return when (icon) {
            "01d" -> R.drawable._01d
            "01n" -> R.drawable._01n
            "02d" -> R.drawable._02d
            "02n" -> R.drawable._02n
            "03n" -> R.drawable._02n // fixme: Change weather icon
            "03d" -> R.drawable._02n // fixme: Change weather icon
            "04d" -> R.drawable._02n // fixme: Change weather icon
            "04n" -> R.drawable._02n // fixme: Change weather icon
            "09d" -> R.drawable._09d
            "09n" -> R.drawable._09n
            "10d" -> R.drawable._10d
            "10n" -> R.drawable._10n
            "11d" -> R.drawable._11d
            "11n" -> R.drawable._11n
            "13d" -> R.drawable._13d
            "13n" -> R.drawable._13n
            "50d" -> R.drawable._50d
            "50n" -> R.drawable._50n
            else -> R.drawable._01d
        }
    }

    private fun showLoading() {
        binding.apply {
            linearProgressIndicator.isVisible = true
        }
    }

    private fun hideLoading() {
        binding.apply {
            linearProgressIndicator.isVisible = false
            swipeRefreshLayout.isRefreshing = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
