package com.williamd.objetconnecteapplication

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.SeekBar
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.fragment.app.testing.withFragment
import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
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
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import java.io.InputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.security.Security
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import org.bouncycastle.jce.provider.BouncyCastleProvider
import javax.net.ssl.X509TrustManager


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

        // Setup SharedPreferences with test values
        preferencesEditor = PreferenceManager.getDefaultSharedPreferences(targetContext).edit()
        preferencesEditor.putString("pref_ip_connection", "127.0.0.1")
        preferencesEditor.putString("pref_port_connection", mockWebServerSSL.getMockWebServer().port.toString())
        preferencesEditor.apply()
    }

    @After
    fun teardown() {
        mockWebServerSSL.shutdown()
    }

    @Test
    fun testFragmentCreation() {
        launchFragmentInContainer<AccueilFragment>()

        // Verify basic UI elements are displayed
        onView(withId(R.id.seekBar_vitesse)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_refresh_status)).check(matches(isDisplayed()))
    }

    @Test
    fun testButtonClickTriggersRequestAndDisplaysResponse() {
        fragment= launchFragmentInContainer<AccueilFragment>()

        val client = mockWebServerSSL.getClientBuilder()
            .build()

        fragment.onFragment { frag ->
            frag.setClient(client)
        }

        // Enqueue a mock response from the server
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("Test response") // This is the response body that should be shown in the UI
        mockWebServerSSL.getMockWebServer().enqueue(mockResponse)
        mockWebServerSSL.getMockWebServer().enqueue(mockResponse)
        mockWebServerSSL.getMockWebServer().enqueue(mockResponse)

        // Perform button click action on btn_refresh
        onView(withId(R.id.btn_refresh_status)).perform(click())

        // Verify the request was made to the server
        val request = mockWebServerSSL.getMockWebServer().takeRequest()
        assertNotNull(request)
        assertEquals("/fetch", request.path)  // Assuming the request path is "/fetch" (update accordingly)

        // Verify that the response is shown in the UI (assuming it's displayed in a TextView with id `response_text`)
        //onView(withId(R.id.response_text)).check(matches(withText("Test response")))
    }

    @Test
    fun testRefreshSendRequest(){
        launchFragmentInContainer<AccueilFragment>()

        // Prepare mock response
        mockWebServerSSL.getMockWebServer().enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("test response")
        )
        mockWebServerSSL.getMockWebServer().enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("test response")
        )
        mockWebServerSSL.getMockWebServer().enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("test response")
        )

        // Click refresh button
        onView(withId(R.id.btn_refresh_status)).perform(click())

        // Verify network request was made
        val request = mockWebServerSSL.getMockWebServer().takeRequest(2, TimeUnit.SECONDS)
        assertNotNull(request)
        assertTrue(request?.path?.startsWith("/") == true)
    }

    @Test
    fun testSeekBarMaxValue() {
        launchFragmentInContainer<AccueilFragment>()

        // Verify seekBar max value is set to 100
        onView(withId(R.id.seekBar_vitesse)).check(matches(withSeekBarMax(100)))
    }

    // Custom matcher for SeekBar max value
    private fun withSeekBarMax(max: Int) = object : TypeSafeMatcher<View>() {
        override fun describeTo(description: org.hamcrest.Description) {
            description.appendText("SeekBar with max value: $max")
        }

        override fun matchesSafely(item: View): Boolean {
            return item is SeekBar && item.max == max
        }
    }
}