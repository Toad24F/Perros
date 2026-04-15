package com.example.huellasseguras.Nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable

/**
 * Maneja la escritura y lectura de etiquetas NFC.
 * El deep link escrito tiene el formato: huellaliza://mascota/{petId}
 * Este URI es interceptado por MainActivity gracias al intent-filter configurado.
 */
object NfcManager {

    private const val DEEP_LINK_SCHEME = "huellaliza://mascota/"

    /**
     * Escribe el deep link de la mascota en la etiqueta NFC.
     * Funciona con etiquetas ya formateadas (Ndef) y sin formatear (NdefFormatable).
     */
    fun writeToTag(tag: Tag, petId: String): Result<Unit> {
        val uri = "$DEEP_LINK_SCHEME$petId"
        return try {
            val record = NdefRecord.createUri(uri)
            val message = NdefMessage(arrayOf(record))

            // Intentar primero con Ndef (etiqueta ya formateada)
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    ndef.close()
                    return Result.failure(Exception("La etiqueta NFC es de solo lectura"))
                }
                if (ndef.maxSize < message.toByteArray().size) {
                    ndef.close()
                    return Result.failure(Exception("La etiqueta NFC no tiene suficiente espacio"))
                }
                ndef.writeNdefMessage(message)
                ndef.close()
                Result.success(Unit)
            } else {
                // Si no está formateada, usar NdefFormatable
                val formatable = NdefFormatable.get(tag)
                    ?: return Result.failure(Exception("Esta etiqueta no es compatible con NDEF. Prueba con una etiqueta NTAG213 o similar."))
                formatable.connect()
                formatable.format(message)
                formatable.close()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Lee el petId almacenado en una etiqueta NFC.
     * Devuelve null si la etiqueta no tiene el formato esperado.
     */
    fun readPetIdFromTag(tag: Tag): String? {
        return try {
            val ndef = Ndef.get(tag) ?: return null
            ndef.connect()
            val message = ndef.cachedNdefMessage ?: ndef.ndefMessage
            ndef.close()

            val record = message?.records?.firstOrNull() ?: return null
            val uri = record.toUri()?.toString() ?: return null

            if (uri.startsWith(DEEP_LINK_SCHEME)) {
                uri.removePrefix(DEEP_LINK_SCHEME).takeIf { it.isNotBlank() }
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}