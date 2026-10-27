package com.morado.topwaylauncher

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Arranque real de la app con sus ViewModels. Se borran antes los ajustes del vehículo (el
 * emulador puede conservarlos de pruebas manuales): sin fuentes configuradas, la velocidad
 * debe ser desconocida.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    private val cleanSettings = object : ExternalResource() {
        override fun before() {
            InstrumentationRegistry.getInstrumentation().targetContext
                .getSharedPreferences("vehicle", Context.MODE_PRIVATE)
                .edit().clear().commit()
        }
    }

    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(cleanSettings).around(compose)

    @Test
    fun arranca_conVelocidadDesconocida() {
        compose.onNodeWithText("VELOCIDAD").assertIsDisplayed()
        compose.onNodeWithContentDescription("Velocidad no disponible").assertIsDisplayed()
        compose.onNodeWithText("Desactivado").assertIsDisplayed()
    }

    @Test
    fun recreacion_conservaLaSeccion() {
        compose.onNodeWithText("Conducción").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Informativo: no sustituye al tablero").assertIsDisplayed()
    }
}
