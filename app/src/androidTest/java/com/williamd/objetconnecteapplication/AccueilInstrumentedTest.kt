package com.williamd.objetconnecteapplication

import android.content.Context
import android.content.SharedPreferences
import android.media.Image
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Switch
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.fragment.app.testing.withFragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import kotlinx.coroutines.launch
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import io.mockk.verify
import io.mockk.spyk
import android.os.Build
import android.provider.Settings
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher

@RunWith(AndroidJUnit4::class)
class AccueilInstrumentedTest {
    private lateinit var preferencesEditor: SharedPreferences.Editor
    private lateinit var mockWebServerSSL: MockWebServerSSL
    private lateinit var fragment: FragmentScenario<AccueilFragment>

    @Before
    fun setup() {
        val targetContext: Context = getInstrumentation().targetContext

        mockWebServerSSL = MockWebServerSSL()
        mockWebServerSSL.setupMockWebServerWithSSL(targetContext)

        // Arrange les préférence pour pointer au mockServer
        preferencesEditor = PreferenceManager.getDefaultSharedPreferences(targetContext).edit()
        preferencesEditor.putString("pref_ip_connection", "127.0.0.1")
        preferencesEditor.putString("pref_port_connection", mockWebServerSSL.getMockWebServer().port.toString())
        preferencesEditor.apply()

        fragment = launchFragmentInContainer<AccueilFragment>()
    }

    @After
    fun teardown() {
        mockWebServerSSL.shutdown()
    }

    @Test
    fun testFragmentCreation() {
        // Assert
        onView(withId(R.id.seekBar_vitesse)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_refresh_status)).check(matches(isDisplayed()))
        onView(withId(R.id.tv_temperature)).check(matches(isDisplayed()))
        onView(withId(R.id.tv_humidite)).check(matches(isDisplayed()))
        onView(withId(R.id.switch_ventilation)).check(matches(isDisplayed()))
        onView(withId(R.id.switch_temperature_type)).check(matches(isDisplayed()))
    }

    @Test
    fun testSeekBarProgressUpdate() {
        // Arrange
        val client = mockWebServerSSL.getClientBuilder().build()
        fragment.onFragment { frag ->
            frag.setClient(client)
        }

        // Setup le retour de la requête
        mockWebServerSSL.getMockWebServer().enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{\"success\": true}")
        )

        // Act
        onView(withId(R.id.seekBar_vitesse))
            .perform(setProgress(100))
            .check(matches(withSeekBarValue(100)))

        // Assert qu'une requête à été envoyé avec la bonne vitesse et le bon chemin
        val request = mockWebServerSSL.getMockWebServer().takeRequest()
        assertEquals("/vitesse", request.path)
        assertEquals("{\"vitesse\": 100}", request.body.readUtf8())
    }

    @Test
    fun testRefreshRequestAndDisplaysResponse() {
        val context = getInstrumentation().targetContext

        // Valeur attendu
        val expectedTemperature = 22.5f
        val expectedHumidite = 100f
        val expectedVitesse = 23
        val formattedTemperature = context.getString(R.string.temperature, expectedTemperature)
        val formattedHumidite = context.getString(R.string.humidite, expectedHumidite)
        val expectedEstAllume = true

        // Setup le client okHttp
        val client = mockWebServerSSL.getClientBuilder().build()
        fragment.onFragment { frag ->
            frag.setClient(client)
        }

        // Met la réponse en queue
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("{ \"temperature\": $expectedTemperature, \"humidite\": $expectedHumidite, \"estAllume\": $expectedEstAllume, \"vitesse\": $expectedVitesse, \"typeDegree\": \"C\" }")
        mockWebServerSSL.getMockWebServer().enqueue(mockResponse)

        // Act
        onView(withId(R.id.btn_refresh_status)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_refresh_status)).check(matches(isEnabled()))

        fragment.onFragment { frag ->
            val myButton = frag.view?.findViewById<ImageButton>(R.id.btn_refresh_status)

            assertNotNull(myButton)

            myButton?.performClick()
        }

        // Assert
        // Attant la requête pour 3 seconde max
        val request = mockWebServerSSL.getMockWebServer().takeRequest(3, TimeUnit.SECONDS)
        assertNotNull(request)
        if (request != null) {
            assertEquals("/status", request.path)
        }

        // Vérifie que les donneés changé son bonne
        onView(withId(R.id.tv_temperature)).check(matches(withText(formattedTemperature)))
        onView(withId(R.id.tv_humidite)).check(matches(withText(formattedHumidite)))
        onView(withId(R.id.seekBar_vitesse)).check(matches(withSeekBarValue(expectedVitesse)))
        onView(withId(R.id.switch_ventilation)).check(matches(withSwitchValue(expectedEstAllume)))
    }

    @Test
    fun testSeekBarMaxValue() {
        launchFragmentInContainer<AccueilFragment>()

        // Assert
        // Vérifie que le maximum est 100 à la seekbar
        onView(withId(R.id.seekBar_vitesse)).check(matches(withSeekBarMax(100)))
    }

    private fun withSeekBarMax(max: Int) = object : TypeSafeMatcher<View>() {
        override fun describeTo(description: org.hamcrest.Description) {
            description.appendText("SeekBar with max value: $max")
        }

        override fun matchesSafely(item: View): Boolean {
            return item is SeekBar && item.max == max
        }
    }

    private fun withSeekBarValue(value: Int) = object : TypeSafeMatcher<View>() {
        override fun describeTo(description: org.hamcrest.Description) {
            description.appendText("SeekBar with value: $value")
        }

        override fun matchesSafely(item: View): Boolean {
            return item is SeekBar && item.progress == value
        }
    }

    private fun withSwitchValue(value: Boolean) = object : TypeSafeMatcher<View>() {
        override fun describeTo(description: org.hamcrest.Description) {
            description.appendText("Check with value: $value")
        }

        override fun matchesSafely(item: View): Boolean {
            return item is Switch && item.isEnabled == value
        }
    }

    private fun setProgress(progress: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return ViewMatchers.isAssignableFrom(SeekBar::class.java)
            }

            override fun getDescription(): String {
                return "Set progress to $progress"
            }

            override fun perform(uiController: UiController, view: View) {
                val seekBar = view as SeekBar
                seekBar.progress = progress
                uiController.loopMainThreadUntilIdle()
            }
        }
    }
}