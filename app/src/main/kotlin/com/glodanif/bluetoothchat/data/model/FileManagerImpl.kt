package com.glodanif.bluetoothchat.data.model

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.support.v4.content.FileProvider
import com.crashlytics.android.Crashlytics
import com.glodanif.bluetoothchat.BuildConfig
import com.glodanif.bluetoothchat.R
import io.fabric.sdk.android.Fabric
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FileManagerImpl(private val context: Context) : FileManager {

    override suspend fun extractApkFile(): Uri? {

        val application = context.packageManager
                .getPackageInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_SHARED_LIBRARY_FILES)
        val directory = if (context.externalCacheDir != null) context.externalCacheDir else
            File(Environment.getExternalStorageDirectory(), context.getString(R.string.app_name))

        if (application != null && directory != null) {

            val file = File(application.applicationInfo.publicSourceDir)

            try {
                val newFile = copyAndZip(file, directory, "BluetoothChat")

                return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    FileProvider.getUriForFile(context,
                            context.applicationContext.packageName + ".provider", newFile)
                } else {
                    Uri.fromFile(newFile)
                }

            } catch (e: IOException) {
                if (Fabric.isInitialized()) {
                    val ex = IOException("${e.message} (file: $file)")
                    Crashlytics.getInstance().core.logException(ex)
                }
                return null
            }
        } else {
            return null
        }
    }

    private fun copyAndZip(file: File, targetDirectory: File, newName: String): File {

        val bufferSize = 2048

        val copiedFile = File(targetDirectory, "$newName.apk")
        val zipFile = File(targetDirectory, "$newName.zip")

        copiedFile.deleteOnExit()
        copiedFile.createNewFile()
        zipFile.deleteOnExit()

        FileInputStream(file).use { fileInputStream ->

            FileOutputStream(copiedFile).use { fileOutputStream ->

                fileOutputStream.channel
                        .transferFrom(fileInputStream.channel, 0, fileInputStream.channel.size())
            }
        }

        FileInputStream(copiedFile).use { fileInputStream ->

            BufferedInputStream(fileInputStream, bufferSize).use { bufferedInputStream ->

                FileOutputStream(zipFile).use { fileOutputStream ->

                    BufferedOutputStream(fileOutputStream).use { bufferedOutputStream ->

                        ZipOutputStream(bufferedOutputStream).use { zipOutputStream ->

                            val data = ByteArray(bufferSize)
                            val entry = ZipEntry(copiedFile.name)
                            zipOutputStream.putNextEntry(entry)

                            var count = bufferedInputStream.read(data, 0, bufferSize)
                            while (count != -1) {
                                zipOutputStream.write(data, 0, count)
                                count = bufferedInputStream.read(data, 0, bufferSize)
                            }
                        }
                    }
                }
            }
        }

        return zipFile
    }
}
