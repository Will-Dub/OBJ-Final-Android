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
        // Initialize Mockito annotations (if not using a custom rule like `MockitoJUnit.rule()`)
        MockitoAnnotations.initMocks(this)

        // Initialize the fragment and mock objects
        fragment = AccueilFragment()

        // Mock SharedPreferences
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.getString(eq("pref_ip_connection"), anyString())).thenReturn("10.4.129.18")
        `when`(mockSharedPreferences.getString(eq("pref_port_connection"), anyString())).thenReturn("4443")
        `when`(mockSharedPreferences.getString(eq("pref_fetch"), anyString())).thenReturn("1")

        // Mock Fragment Binding
        val mockBinding = mock(FragmentAccueilBinding::class.java)

        // Mock UI components
        `when`(mockBinding.seekBarVitesse).thenReturn(mockSeekBar)
        `when`(mockBinding.btnRefreshStatus).thenReturn(mockButtonRefresh)
        `when`(mockBinding.switchVentilation).thenReturn(mockSwitchVentilation)
        `when`(mockBinding.switchTemperatureType).thenReturn(mockSwitchTemperatureType)

        // Initialize the fragment and the mock context
        fragment.onAttach(InstrumentationRegistry.getInstrumentation().targetContext)
        fragment.onCreateView(LayoutInflater.from(ApplicationProvider.getApplicationContext()), null, null)
    }

    @Test
    fun testOnViewCreated() {
        // Simulate onViewCreated
        fragment.onViewCreated(mock(View::class.java), null)

        // Check that SharedPreferences values are being used to set up server URL
        assert(fragment.serverUrl == "https://10.4.129.18:4443")

        // Verify SeekBar max value
        verify(mockBinding.seekBarVitesse).setMax(100)

        // Verify that the refresh button animation is set up
        val rotateCaptor = argumentCaptor<RotateAnimation>()
        verify(mockButtonRefresh).startAnimation(rotateCaptor.capture())
        assert(rotateCaptor.firstValue.duration == 250L)

        // Test the "Start Fetching Task" behavior based on SharedPreferences
        verify(mockHandler).post(any())
    }

    @Test
    fun `test getData successfully parses response`() {
        // Prepare a mock response
        val mockJson = """{"temperature": 25.5, "humidite": 60.0, "estAllume": true, "vitesse": 75, "typeDegree": "C"}"""
        /**
        `when`(mockResponseBody.string()).thenReturn(mockJson)
        `when`(mockCall.execute()).thenReturn(mockResponse)

        fragment.setClient(mockClient)

        // Execute the test method
        val result = fragment.getData("https://test.com/status")

        // Verify and assert the result
        assertNotNull(result)
        assertEquals(mockJson, result)
        verify(mockClient).newCall(any())  // Verify that the mock client was called**/
    }
}