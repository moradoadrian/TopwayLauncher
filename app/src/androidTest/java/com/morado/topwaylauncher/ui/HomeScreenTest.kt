package com.morado.topwaylauncher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.morado.topwaylauncher.data.vehicle.GpsStatus
import com.morado.topwaylauncher.data.vehicle.ObdError
import com.morado.topwaylauncher.data.vehicle.ObdStatus
import com.morado.topwaylauncher.data.vehicle.SpeedSource
import com.morado.topwaylauncher.data.vehicle.VehicleState
import com.morado.topwaylauncher.ui.components.HomeSection
import com.morado.topwaylauncher.ui.home.AppsUiState
import com.morado.topwaylauncher.ui.home.HomeScreen
import com.morado.topwaylauncher.ui.home.HomeUiState
import com.morado.topwaylauncher.ui.home.MediaActions
import com.morado.topwaylauncher.ui.theme.TopwayLauncherTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas de la pantalla con estados fijos: no usan Bluetooth, GPS ni apps instaladas, así
 * que el resultado no depende del emulador. Tamaño de referencia: pantalla de radio panorámica.
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun show(
        vehicle: VehicleState = VehicleState(),
        state: HomeUiState = HomeUiState(apps = AppsUiState.Empty),
        width: Dp = 900.dp,
        height: Dp = 500.dp,
    ) {
        rule.setContent {
            var section by remember { mutableStateOf(HomeSection.Dashboard) }
            TopwayLauncherTheme {
                Box(Modifier.requiredSize(width, height)) {
                    HomeScreen(
                        state = state,
                        vehicle = remember { mutableStateOf(vehicle) },
                        section = section,
                        snackbarHostState = remember { SnackbarHostState() },
                        onSection = { section = it },
                        onNavigation = {},
                        onMusic = {},
                        media = MediaActions({}, {}, {}, {}, {}),
                        onRetry = {},
                        onAppClick = {},
                        settingsContent = { Text(SETTINGS_MARKER, it) },
                    )
                }
            }
        }
    }

    @Test
    fun velocidadDesconocida_muestraGuionesYNoCero() {
        show()
        rule.onNodeWithContentDescription("Velocidad no disponible").assertIsDisplayed()
        rule.onNodeWithText("Configura OBD-II o GPS en Ajustes").assertIsDisplayed()
        rule.onAllNodesWithText("0").assertCountEquals(0)
    }

    @Test
    fun velocidadObd_muestraValorFuenteYEstado() {
        show(VehicleState(speedKmh = 87, source = SpeedSource.Obd, obd = ObdStatus.Connected("ISO 15765-4 (CAN 11/500)")))
        rule.onNodeWithContentDescription("87 kilómetros por hora").assertIsDisplayed()
        rule.onNodeWithText("Lectura en vivo").assertIsDisplayed()
        rule.onNodeWithText("Conectado").assertIsDisplayed()
        // Insignia de la tarjeta + indicador de la cabecera.
        rule.onAllNodesWithText("OBD-II").assertCountEquals(2)
    }

    @Test
    fun datosDemo_seIdentificanComoSimulados() {
        show(VehicleState(speedKmh = 40, source = SpeedSource.Demo))
        rule.onAllNodesWithText("DEMO · simulado").onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("Lectura en vivo").assertCountEquals(0)
    }

    @Test
    fun datosAntiguos_noSeMuestranComoActuales() {
        show(VehicleState(stale = true, obd = ObdStatus.Error(ObdError.ConnectionLost)))
        rule.onNodeWithContentDescription("Velocidad no disponible").assertIsDisplayed()
        rule.onNodeWithText("Sin datos recientes").assertIsDisplayed()
        rule.onNodeWithText("Conexión perdida").assertIsDisplayed()
    }

    @Test
    fun gps_soloSeIndicaCuandoEstaActivo() {
        show(VehicleState(gps = GpsStatus.Disabled))
        rule.onAllNodesWithText("GPS").assertCountEquals(0)
    }

    @Test
    fun gpsActivo_indicaSiTieneSenal() {
        show(VehicleState(speedKmh = 52, source = SpeedSource.Gps, gps = GpsStatus.On, gpsHasFix = true))
        rule.onNodeWithText("Con señal").assertIsDisplayed()
        rule.onNodeWithContentDescription("52 kilómetros por hora").assertIsDisplayed()
    }

    @Test
    fun mapasAusentes_loIndicaYOfreceLaListaDeApps() {
        // 900 × 500: navegación y música quedan lado a lado; la tarjeta usa su versión en columna.
        show(state = HomeUiState(apps = AppsUiState.Empty, navigation = null))
        rule.onNodeWithText("Google Maps no está instalado").assertIsDisplayed()
        rule.onNodeWithText("No instalado").assertIsDisplayed()
        rule.onNodeWithText("Ver aplicaciones").assertIsDisplayed()
    }

    @Test
    fun musicaSinDatos_estadoVacioSinInventarCanciones() {
        show(state = HomeUiState(apps = AppsUiState.Empty, music = null))
        rule.onNodeWithText("Sin datos de música").assertIsDisplayed()
        rule.onNodeWithText("Elegir app de música").assertIsDisplayed()
    }

    @Test
    fun navegacionEntreSecciones() {
        show()
        rule.onNodeWithText("Conducción").performClick()
        rule.onNodeWithText("Informativo: no sustituye al tablero").assertIsDisplayed()

        rule.onNodeWithText("Apps").performClick()
        rule.onNodeWithText("Aplicaciones").assertIsDisplayed()

        rule.onNodeWithText("Ajustes").performClick()
        rule.onNodeWithText(SETTINGS_MARKER).assertIsDisplayed()

        rule.onNodeWithText("Inicio").performClick()
        rule.onNodeWithText("VELOCIDAD").assertIsDisplayed()
    }

    @Test
    fun barraLateral_conPocaAlturaAjustesSigueEntero() {
        show(width = 800.dp, height = 330.dp)
        // Sin espacio para etiquetas se ocultan, pero los seis botones siguen visibles y accesibles.
        listOf("Inicio", "Conducción", "Mapas", "Música", "Apps", "Ajustes").forEach {
            rule.onNodeWithContentDescription(it).assertIsDisplayed()
        }
    }

    @Test
    fun pantallaPanoramica_todoVisible() {
        show(width = 1000.dp, height = 560.dp)
        rule.onNodeWithText("VELOCIDAD").assertIsDisplayed()
        rule.onNodeWithText("Navegación").assertIsDisplayed()
        // Barra lateral y título de la tarjeta.
        rule.onAllNodesWithText("Música").assertCountEquals(2)
        rule.onNodeWithText("Todas").assertIsDisplayed()
        rule.onNodeWithText("Ajustes").assertIsDisplayed()
    }

    private companion object {
        const val SETTINGS_MARKER = "contenido de ajustes"
    }
}
