package com.williamd.objetconnecteapplication

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.animation.RotateAnimation
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Switch
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.williamd.objetconnecteapplication.databinding.FragmentAccueilBinding
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.argumentCaptor
import java.util.concurrent.ScheduledExecutorService

@RunWith(MockitoJUnitRunner::class)
class AccueilUnitTest {

    @Mock lateinit var mockSharedPreferences: SharedPreferences
    @Mock lateinit var mockEditor: SharedPreferences.Editor
    @Mock lateinit var mockContext: Context
    @Mock lateinit var mockBinding: FragmentAccueilBinding
    @Mock lateinit var mockHandler: Handler
    @Mock lateinit var mockScheduledExecutorService: ScheduledExecutorService
    @Mock lateinit var mockButtonRefresh: ImageButton
    @Mock lateinit var mockSeekBar: SeekBar
    @Mock lateinit var mockSwitchVentilation: Switch
    @Mock lateinit var mockSwitchTemperatureType: Switch
    @Mock lateinit var mockOkHttpClient: OkHttpClient
    lateinit var fragment: AccueilFragment

    @Before
    fun setup() {
        // Initialise mockito
        MockitoAnnotations.initMocks(this)

        // Initialise le fragment
        fragment = AccueilFragment()

        // Mock SharedPreferences
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.getString(eq("pref_ip_connection"), anyString())).thenReturn("10.4.129.18")
        `when`(mockSharedPreferences.getString(eq("pref_port_connection"), anyString())).thenReturn("4443")
        `when`(mockSharedPreferences.getString(eq("pref_fetch"), anyString())).thenReturn("1")

        // Mock les bindings
        val mockBinding = mock(FragmentAccueilBinding::class.java)

        // Mock les composants du UI
        `when`(mockBinding.seekBarVitesse).thenReturn(mockSeekBar)
        `when`(mockBinding.btnRefreshStatus).thenReturn(mockButtonRefresh)
        `when`(mockBinding.switchVentilation).thenReturn(mockSwitchVentilation)
        `when`(mockBinding.switchTemperatureType).thenReturn(mockSwitchTemperatureType)

        // Initialise le fragment
        fragment.onAttach(InstrumentationRegistry.getInstrumentation().targetContext)
        fragment.onCreateView(LayoutInflater.from(ApplicationProvider.getApplicationContext()), null, null)
    }

    @Test
    fun testOnViewCreated() {
        fragment.onViewCreated(mock(View::class.java), null)

        // Vérifie l'url crée
        assert(fragment.serverUrl == "https://10.4.129.18:4443")

        // Vérifie le maximum de vitesse
        verify(mockBinding.seekBarVitesse).setMax(100)

        // Vérifie si l'animation du bouton refresh fonctionne
        val rotateCaptor = argumentCaptor<RotateAnimation>()
        verify(mockButtonRefresh).startAnimation(rotateCaptor.capture())
        assert(rotateCaptor.firstValue.duration == 250L)

        // Vérifie si le fragment comment à fetch
        verify(mockHandler).post(any())
    }
}