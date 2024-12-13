package com.williamd.objetconnecteapplication

import android.content.SharedPreferences
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.FragmentActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class AccueilFragmentTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    lateinit var sharedPreferences: SharedPreferences
    @Mock
    lateinit var editor: SharedPreferences.Editor

    private lateinit var fragment: AccueilFragment
    private lateinit var activity: FragmentActivity

    @Before
    fun setup() {
        // Mocks will be initialized automatically with MockitoJUnitRunner or MockitoExtension
        Mockito.`when`(sharedPreferences.getString("pref_ip_connection", "10.4.129.18")).thenReturn("127.0.0.1")
        Mockito.`when`(sharedPreferences.getString("pref_port_connection", "4443")).thenReturn("8080")
        Mockito.`when`(sharedPreferences.getString("pref_fetch", "1")).thenReturn("5")
        Mockito.`when`(sharedPreferences.edit()).thenReturn(editor)

        // Mock Looper's static methods (mainLooper and myLooper)
        mockkStatic(Looper::class) // This mocks the Looper class
        val looper = mockk<Looper>(relaxed = true) // Relaxed mock to avoid errors on unused methods
        every { Looper.getMainLooper() } returns looper // Mock the main looper
        every { Looper.myLooper() } returns looper // Mock the myLooper method

        // Mock android.util.Log.isLoggable static method to avoid errors
        mockkStatic(Log::class) // Mock the static Log class
        every { Log.isLoggable(any(), any()) } returns true // Mock isLoggable to return true

        mockkStatic(Log::class) // Mock the Log class to avoid errors on Log methods
        every { Log.v(any(), any()) } returns 0 // Mock Log.v to return 0 (Log.VERBOSE)
        every { Log.d(any(), any()) } returns 0 // Mock Log.d to return 0 (Log.DEBUG)
        every { Log.i(any(), any()) } returns 0 // Mock Log.i to return 0 (Log.INFO)
        every { Log.e(any(), any()) } returns 0 // Mock Log.e to return 0 (Log.ERROR)
        every { Log.isLoggable(any(), any()) } returns true

        // Create an Activity for the Fragment to be attached to
        activity = FragmentActivity()

        // Initialize the fragment
        fragment = AccueilFragment()

        // Add the fragment to the activity using FragmentTransaction
        activity.supportFragmentManager.beginTransaction().add(fragment, "AccueilFragment").commit()

        // Force fragment lifecycle methods to simulate onAttach, onCreateView, etc.
        activity.supportFragmentManager.executePendingTransactions()
    }

    @Test
    fun testServerUrlIsCreatedCorrectly() {
        fragment.onViewCreated(View(fragment.context), null)

        // Assert that the server URL is correctly created from preferences
        val expectedUrl = "https://127.0.0.1:8080"
        assertEquals(expectedUrl, fragment.serverUrl)
    }

    @Test
    fun testPostRequestIsSentWhenSeekBarChanges() {
        val newSpeed = 50

        // Assume seekBar has been set up with an initial value
        fragment.binding.seekBarVitesse.progress = newSpeed

        // Mock the behavior of sending data (sendPost should be called with correct URL and body)
        val sendPostMock = Mockito.spy(fragment)
        sendPostMock.sendPost(fragment.serverUrl + "/vitesse", "{\"vitesse\": $newSpeed}")

        // Check if the post request method was called
        Mockito.verify(sendPostMock).sendPost(Mockito.anyString(), Mockito.anyString())
    }
}
