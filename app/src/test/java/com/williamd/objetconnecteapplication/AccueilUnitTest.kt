package com.williamd.objetconnecteapplication



import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Assert.*
import org.junit.Rule
import org.junit.runner.RunWith
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText

@RunWith(AndroidJUnit4::class)
class AccueilUnitTest {

    @get: Rule
    val activity = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun main_activity_test() {
        onView(withId(R.id.someBtn)).check(matches(isDisplayed()))
    }

    @Test
    fun btn_test(){
        onView(withId(R.id.someBtn)).perform(click())
    }
}