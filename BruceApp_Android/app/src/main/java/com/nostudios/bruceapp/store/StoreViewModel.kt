package com.nostudios.bruceapp.store

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nostudios.bruceapp.ble.BruceBLEManager
import com.nostudios.bruceapp.ble.ConnectionState
import com.nostudios.bruceapp.data.model.AppScript
import com.nostudios.bruceapp.data.model.StoreCategory
import com.nostudios.bruceapp.data.remote.AppStoreService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URL

class StoreViewModel : ViewModel() {

    private val api = AppStoreService.create()

    private val _categories = MutableStateFlow<List<StoreCategory>>(emptyList())
    val categories: StateFlow<List<StoreCategory>> = _categories.asStateFlow()

    private val _availableScripts = MutableStateFlow<List<AppScript>>(emptyList())
    val availableScripts: StateFlow<List<AppScript>> = _availableScripts.asStateFlow()

    private val _isLoadingCategories = MutableStateFlow(false)
    val isLoadingCategories: StateFlow<Boolean> = _isLoadingCategories.asStateFlow()

    private val _isLoadingScripts = MutableStateFlow(false)
    val isLoadingScripts: StateFlow<Boolean> = _isLoadingScripts.asStateFlow()

    private val _popupMessage = MutableStateFlow<String?>(null)
    val popupMessage: StateFlow<String?> = _popupMessage.asStateFlow()

    private val _installationProgress = MutableStateFlow(0.0)
    val installationProgress: StateFlow<Double> = _installationProgress.asStateFlow()

    private val _isInstalling = MutableStateFlow(false)
    val isInstalling: StateFlow<Boolean> = _isInstalling.asStateFlow()

    private val baseURL = "http://ghp.iceis.co.uk"

    fun fetchCategories() {
        if (_categories.value.isNotEmpty()) return
        viewModelScope.launch {
            _isLoadingCategories.value = true
            try {
                val response = api.getCategories()
                _categories.value = response.categories.filter { it.slug != "updates" }
            } catch (e: Exception) {
                Log.e("StoreVM", "Error loading categories: $e")
                _popupMessage.value = "Connection error"
                clearPopupAfterDelay()
            } finally {
                _isLoadingCategories.value = false
            }
        }
    }

    fun fetchAppsAndScanStorage(category: StoreCategory, bleManager: BruceBLEManager) {
        viewModelScope.launch {
            _isLoadingScripts.value = true
            _availableScripts.value = emptyList()

            val slug = category.slug ?: return@launch
            try {
                val response = api.getCategoryApps(slug)
                val fetchedApps = response.apps.toMutableList()

                if (bleManager.connectionState.value == ConnectionState.Paired) {
                    bleManager.setFileFolderLoading(true)
                    val targetCategoryName = response.category ?: category.displayName
                    bleManager.sendCommand("FS_LIST /BruceJS/$targetCategoryName")

                    val startTime = System.currentTimeMillis()
                    while (bleManager.isFileFolderLoading.value && System.currentTimeMillis() - startTime < 3000) {
                        delay(100)
                    }

                    val localFolderNames = bleManager.remoteFiles.value
                        .filter { it.isDirectory }
                        .map { it.name }

                    for (i in fetchedApps.indices) {
                        if (localFolderNames.contains(fetchedApps[i].name)) {
                            fetchedApps[i].installedVersion = fetchedApps[i].version
                        }
                    }
                }

                _availableScripts.value = fetchedApps
            } catch (e: Exception) {
                Log.e("StoreVM", "Error: $e")
                _popupMessage.value = "Error loading"
                clearPopupAfterDelay()
            } finally {
                _isLoadingScripts.value = false
            }
        }
    }

    fun installApp(script: AppScript, categorySlug: String, bleManager: BruceBLEManager) {
        viewModelScope.launch {
            if (bleManager.connectionState.value != com.nostudios.bruceapp.ble.ConnectionState.Paired) {
                _popupMessage.value = "Hardware not connected!"
                clearPopupAfterDelay()
                return@launch
            }

            _isInstalling.value = true
                _popupMessage.value = "Loading App metadata..."

            val encodedSlug = java.net.URLEncoder.encode(script.repoSlug, "UTF-8")
            val metadataUrlString = "$baseURL/service/main/repositories/$encodedSlug/metadata.json"

            try {
                val metadataJson = withContext(Dispatchers.IO) {
                    URL(metadataUrlString).readText()
                }

                val json = org.json.JSONObject(metadataJson)
                val files = json.getJSONArray("files")
                val repoPath = json.getString("path")
                val owner = json.getString("owner")
                val repo = json.getString("repo")
                val commit = json.getString("commit")
                val categoryName = json.getString("category")

                val baseDir = "/BruceJS/$categoryName"
                val appDir = "$baseDir/${script.name}"

                bleManager.sendCommand("FS_MKDIR /BruceJS")
                delay(40)
                bleManager.sendCommand("FS_MKDIR $baseDir")
                delay(40)
                bleManager.sendCommand("FS_MKDIR $appDir")
                delay(40)

                var currentFileIndex = 1
                val totalFilesCount = files.length()

                for (i in 0 until files.length()) {
                    val fileElement = files.get(i)
                    var fileName = ""
                    var sourceName = ""

                    if (fileElement is org.json.JSONObject) {
                        fileName = fileElement.optString("destination", "")
                        sourceName = fileElement.optString("source", "")
                    } else {
                        fileName = fileElement.toString()
                        sourceName = fileElement.toString()
                    }

                    fileName = fileName.replace(Regex("^/+"), "")
                    sourceName = sourceName.replace(Regex("^/+"), "")

                    _popupMessage.value = "Loading file $currentFileIndex/$totalFilesCount..."

                    val fullRepoPath = sourceName.replace(Regex("^/+"), "")
                    val fileUrlString = "$baseURL/service/manual/$owner/$repo/$commit/$fullRepoPath"
                        .replace(" ", "%20")

                    val fileData = withContext(Dispatchers.IO) {
                        val url = URL(fileUrlString)
                        val connection = url.openConnection()
                        val inputStream = connection.getInputStream()
                        val buffer = ByteArrayOutputStream()
                        val data = ByteArray(4096)
                        var n: Int
                        while (inputStream.read(data).also { n = it } != -1) {
                            buffer.write(data, 0, n)
                        }
                        buffer.toByteArray()
                    }

                    val destinationFilePath = "$appDir/$fileName"
                    val hexString = fileData.joinToString("") { "%02x".format(it) }

                    bleManager.sendCommand("FS_UPLOAD_START $destinationFilePath ${fileData.size}")
                    delay(60)

                    val chunkSize = 400
                    var currentIndex = 0
                    while (currentIndex < hexString.length) {
                        val end = minOf(currentIndex + chunkSize, hexString.length)
                        val chunk = hexString.substring(currentIndex, end)
                        bleManager.sendCommand("FS_UPLOAD_CHUNK $chunk")
                        currentIndex += chunkSize
                        delay(12)
                    }

                    bleManager.sendCommand("FS_UPLOAD_END")
                    delay(40)

                    currentFileIndex++
                    _installationProgress.value = (currentFileIndex - 1).toDouble() / totalFilesCount
                }

                val currentScripts = _availableScripts.value.toMutableList()
                val idx = currentScripts.indexOfFirst { it.repoSlug == script.repoSlug }
                if (idx >= 0) {
                    currentScripts[idx] = currentScripts[idx].also { it.installedVersion = script.version }
                    _availableScripts.value = currentScripts
                }

                _popupMessage.value = "${script.name} ready!"
            } catch (e: Exception) {
                Log.e("StoreVM", "Error: $e")
                _popupMessage.value = "Installation error"
            } finally {
                _isInstalling.value = false
                clearPopupAfterDelay()
            }
        }
    }

    fun deleteApp(script: AppScript, categorySlug: String, bleManager: BruceBLEManager) {
        val targetPath = "/BruceJS/$categorySlug/${script.name}"
        bleManager.sendCommand("FS_REMOVE $targetPath")

        val currentScripts = _availableScripts.value.toMutableList()
        val idx = currentScripts.indexOfFirst { it.repoSlug == script.repoSlug }
        if (idx >= 0) {
            currentScripts[idx] = currentScripts[idx].also { it.installedVersion = null }
            _availableScripts.value = currentScripts
        }

        _popupMessage.value = "App deleted"
        clearPopupAfterDelay()
    }

    private fun clearPopupAfterDelay() {
        viewModelScope.launch {
            delay(3000)
            _popupMessage.value = null
            _installationProgress.value = 0.0
        }
    }
}
