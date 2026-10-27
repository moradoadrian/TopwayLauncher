package com.morado.topwaylauncher.data.vehicle

import java.util.Locale

/**
 * Marca y modelo que el usuario escribe en Ajustes. Solo es texto: no se incluyen logotipos de
 * fabricantes. Se guarda tal cual se escribe y se normaliza al mostrarlo.
 */
object VehicleIdentity {

    /** Suficiente para "VOLKSWAGEN" o "ALTIMA 2.5 SL 2007" sin desbordar la cabecera. */
    const val MAX_LENGTH = 24

    /** Lo que se acepta al escribir: sin saltos de línea ni caracteres de control. */
    fun sanitize(input: String): String = input.filterNot { it.isISOControl() }.take(MAX_LENGTH)

    /** "nissan" + " altima  2007 " → "NISSAN ALTIMA 2007"; null si no hay nada que mostrar. */
    fun title(brand: String, model: String): String? =
        listOf(brand, model)
            .map { it.trim().replace(WHITESPACE, " ") }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .uppercase(Locale.ROOT)
            .ifEmpty { null }

    private val WHITESPACE = Regex("\\s+")
}
