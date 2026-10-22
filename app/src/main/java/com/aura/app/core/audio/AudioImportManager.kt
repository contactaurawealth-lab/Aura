package com.aura.app.core.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.aura.app.core.database.AuraDatabase
import com.aura.app.core.database.AudioTrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class AudioImportManager(
    private val context: Context,
    private val database: AuraDatabase
) {

    suspend fun importAudioUris(uris: List<Uri>): List<AudioTrackEntity> = withContext(Dispatchers.IO) {
        val importedList = mutableListOf<AudioTrackEntity>()
        val musicDir = File(context.filesDir, "imported_music").apply { if (!exists()) mkdirs() }
        val artDir = File(context.filesDir, "album_art").apply { if (!exists()) mkdirs() }

        for (uri in uris) {
            try {
                val displayName = queryDisplayName(uri) ?: "Audio_${System.currentTimeMillis()}"
                val safeExtension = getExtension(displayName)
                val trackId = "imported_${UUID.randomUUID()}"
                val destFile = File(musicDir, "${trackId}.${safeExtension}")

                // Copy stream to private files
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: continue

                // Retrieve audio metadata
                val mmr = MediaMetadataRetriever()
                var title = displayName.substringBeforeLast(".")
                var artist = "Personal Import"
                var durationMs = 0L
                var artUri: String? = null

                try {
                    mmr.setDataSource(destFile.absolutePath)
                    val metaTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    val metaArtist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    val metaDuration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

                    if (!metaTitle.isNullOrBlank()) title = metaTitle
                    if (!metaArtist.isNullOrBlank()) artist = metaArtist
                    if (!metaDuration.isNullOrBlank()) durationMs = metaDuration.toLongOrNull() ?: 0L

                    val artBytes = mmr.embeddedPicture
                    if (artBytes != null && artBytes.isNotEmpty()) {
                        val artFile = File(artDir, "art_${trackId}.png")
                        FileOutputStream(artFile).use { it.write(artBytes) }
                        artUri = artFile.absolutePath
                    }
                } catch (e: Exception) {
                    // Fallback to default name if retriever fails on this format
                } finally {
                    try {
                        mmr.release()
                    } catch (ignored: Exception) {}
                }

                val entity = AudioTrackEntity(
                    id = trackId,
                    title = title,
                    artist = artist,
                    category = "Imported",
                    durationMs = durationMs,
                    filePath = destFile.absolutePath,
                    isGuided = false,
                    isFavorite = false,
                    description = "Imported from personal device storage ($displayName)",
                    isImported = true,
                    albumArtUri = artUri,
                    dateAdded = System.currentTimeMillis()
                )

                database.audioDao().insertTrack(entity)
                importedList.add(entity)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        importedList
    }

    suspend fun deleteTrack(track: AudioTrackEntity) = withContext(Dispatchers.IO) {
        if (track.isImported) {
            val file = File(track.filePath)
            if (file.exists()) file.delete()
            track.albumArtUri?.let {
                val artFile = File(it)
                if (artFile.exists()) artFile.delete()
            }
        }
        database.audioDao().deleteTrack(track.id)
    }

    private fun queryDisplayName(uri: Uri): String? {
        var name: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return name ?: uri.lastPathSegment
    }

    private fun getExtension(fileName: String): String {
        return if (fileName.contains(".")) {
            fileName.substringAfterLast(".").lowercase()
        } else {
            "m4a"
        }
    }
}
