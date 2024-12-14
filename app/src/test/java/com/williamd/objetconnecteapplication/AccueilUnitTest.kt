package com.williamd.objetconnecteapplication
/*
import android.view.View
import android.widget.SeekBar
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Description
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.internal.matchers.TypeSafeMatcher
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
/*
@RunWith(AndroidJUnit4::class)
class AccueilUnitTest {
    @Rule
    @JvmField
    val activity = FragmentScenario.launchInContainer(AccueilFragment::class.java)

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setup() {
        // Setup MockWebServer
        mockWebServer = MockWebServer()
        mockWebServer.start()

        // Setup SharedPreferences with test values
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.edit().apply {
            putString("pref_ip_connection", "127.0.0.1")
            putString("pref_port_connection", mockWebServer.port.toString())
            putString("pref_fetch", "1")
            apply()
        }
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testFragmentCreation() {
        // Launch fragment
        val scenario = launchFragmentInContainer<AccueilFragment>()

        // Verify basic UI elements are displayed
        onView(withId(R.id.seekBar_vitesse)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_refresh_status)).check(matches(isDisplayed()))
    }

    @Test
    fun testRefreshButtonClick() {
        // Prepare mock response
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("{\"status\": \"success\"}")
        mockWebServer.enqueue(mockResponse)

        // Launch fragment
        val scenario = launchFragmentInContainer<AccueilFragment>()

        // Click refresh button
        onView(withId(R.id.btn_refresh_status)).perform(click())

        // Verify network request was made
        val request = mockWebServer.takeRequest(2, TimeUnit.SECONDS)
        assertNotNull(request)
        assertTrue(request?.path?.startsWith("/") == true)
    }

    @Test
    fun testSeekBarMaxValue() {
        // Launch fragment
        val scenario = launchFragmentInContainer<AccueilFragment>()

        // Verify seekBar max value is set to 100
        onView(withId(R.id.seekBar_vitesse)).check(matches(withSeekBarMax(100)))
    }

    @Test
    fun testAutomaticRefresh() {
        // Prepare multiple mock responses
        repeat(2) {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("{\"status\": \"success\"}")
            )
        }

        // Launch fragment
        val scenario = launchFragmentInContainer<AccueilFragment>()

        // Wait for automatic refresh (slightly longer than the 1-minute interval)
        Thread.sleep(70_000)

        // Verify at least one request was made
        val request = mockWebServer.takeRequest(0, TimeUnit.SECONDS)
        assertNotNull(request)
    }

    // Custom matcher for SeekBar max value
    private fun withSeekBarMax(max: Int) = object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("SeekBar with max value: $max")
        }

        override fun matchesSafely(item: View): Boolean {
            return item is SeekBar && item.max == max
        }
    }
}*/