import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.core.FileSearch.SearchResult
import org.example.core.FileSearch.api.FastSearch
import org.example.core.utils.FileUtils
import java.io.File

class MainViewModel: ViewModel() {
    private val _textQuery = MutableStateFlow("")
    val textQuery = _textQuery.asStateFlow()

    private val _textPath = MutableStateFlow("")
    val textPath = _textPath.asStateFlow()

    private val _textMask = MutableStateFlow("")
    val textMask = _textMask.asStateFlow()

    private val _checkedIncludeDirs = MutableStateFlow(false)
    val checkedIncludeDirs = _checkedIncludeDirs.asStateFlow()

    private val _checkedIncludeFiles = MutableStateFlow(false)
    val checkedIncludeFiles = _checkedIncludeFiles.asStateFlow()

    private val _resultSearch = MutableStateFlow<MutableList<SearchResult>>(mutableListOf())
    val resultSearch = _resultSearch.asStateFlow()

    private val _searchDurationMs = MutableStateFlow(0L)
    val searchDurationMs = _searchDurationMs.asStateFlow()

    private val _precision = MutableStateFlow(0.0)
    val precision = _precision.asStateFlow()

    private val _recall = MutableStateFlow(0.0)
    val recall = _recall.asStateFlow()

    fun search() {
        viewModelScope.launch(Dispatchers.IO) {
            val query = _textQuery.value.lowercase()
            val isQueryBlank = query.isBlank()
            val results = mutableListOf<SearchResult>()
            var relevantFound = 0
            var totalRelevant = 0

            val path = _textPath.value.ifEmpty { "C:/" }
            val rootFile = File(path)

            // Логирование параметров
            println("""
            SEARCH STARTED
            Path: $path
            Query: ${if (isQueryBlank) "none" else query}
            Mask: ${_textMask.value}
            Include dirs: ${_checkedIncludeDirs.value}
            Include files: ${_checkedIncludeFiles.value}
        """.trimIndent())

            // Проверка доступности пути
            if (!rootFile.exists() || !rootFile.canRead()) {
                println("Invalid path or access denied: $path")
                _resultSearch.update { mutableListOf() }
                return@launch
            }

            // Проверка включения файлов/директорий
            if (!_checkedIncludeDirs.value && !_checkedIncludeFiles.value) {
                println("Both include options are disabled")
                _resultSearch.update { mutableListOf() }
                return@launch
            }

            val searchResults = try {
                FastSearch {
                    this.path = path
                    content = if (isQueryBlank) null else query
                    mask = _textMask.value.takeIf { it.isNotEmpty() }
                    includeDirs = _checkedIncludeDirs.value
                    includeFiles = _checkedIncludeFiles.value
                }.run()
            } catch (e: Exception) {
                println("FastSearch init failed: ${e.message}")
                null
            }

            if (searchResults == null) {
                _resultSearch.update { mutableListOf() }
                return@launch
            }

            val startTime = System.currentTimeMillis()

            try {
                searchResults
                    .catch { e -> println("Search error: ${e.message}") }
                    .collect { result ->
                        results.add(result)
                        println("Found: ${result.path}") // Логирование найденных файлов
                        val file = File(result.path)
                        val isRelevant = when {
                            !isQueryBlank && file.isFile -> {
                                try {
                                    file.readText().contains(query, ignoreCase = true)
                                } catch (e: Exception) {
                                    false
                                }
                            }
                            else -> true
                        }
                        if (isRelevant) relevantFound++
                    }
            } catch (e: Exception) {
                println("Search crashed: ${e.message}")
            }

            val duration = System.currentTimeMillis() - startTime

            val allFiles = mutableListOf<File>()
            fun scanDir(dir: File) {
                dir.listFiles()?.forEach { file ->
                    try {
                        // Проверка маски
                        val matchesMask = _textMask.value.isEmpty() ||
                                file.name.matches(_textMask.value.toRegexFromWildcard())

                        // Проверка типа (файл/директория)
                        val include = when {
                            file.isDirectory -> _checkedIncludeDirs.value
                            else -> _checkedIncludeFiles.value
                        }

                        // Проверка содержимого (если запрос не пуст)
                        val contentCondition = if (!isQueryBlank && file.isFile) {
                            try {
                                file.readText().contains(query, ignoreCase = true)
                            } catch (e: Exception) {
                                false
                            }
                        } else true

                        if (include || matchesMask || contentCondition) {
                            allFiles.add(file)
                            println("✅ Added: ${file.name}")
                            if (file.isDirectory) scanDir(file)
                        } else {
                            println("❌ Skipped: ${file.name}")
                        }
                    } catch (e: Exception) {
                        println("Error: ${file.absolutePath} - ${e.message}")
                    }
                }
            }

            scanDir(rootFile)
            totalRelevant = allFiles.size

            // Расчёт метрик с защитой от NaN
            val precisionVal = when {
                results.isEmpty() -> 0.0
                else -> (relevantFound.toDouble() / results.size).coerceIn(0.0..1.0)
            }

            val recallVal = when {
                totalRelevant == 0 -> if (results.isEmpty()) 1.0 else 0.0
                else -> (relevantFound.toDouble() / totalRelevant).coerceIn(0.0..1.0)
            }

            // Обновление состояний
            _searchDurationMs.update { duration }
            _precision.update { precisionVal }
            _recall.update { recallVal }
            _resultSearch.update { results }

            // Отладочный вывод
            println("Final results: ${results.size}")
            println("Total relevant: $totalRelevant")
            println("Precision: $precisionVal")
            println("Recall: $recallVal")
            println("SEARCH FINISHED: ${results.size} results")
        }
    }

    fun String.toRegexFromWildcard(): Regex {
        return Regex(
            "^${replace(".", "\\.")  // Экранируем точку
                .replace("*", ".*")  // Заменяем * на .*
                .replace("?", ".")}$",  // Заменяем ? на .
            RegexOption.IGNORE_CASE
        )
    }

    fun updateTextQuery(value:String) {
        _textQuery.update { value }
    }

    fun updateTextPath(value: String) {
        _textPath.update { value }
    }

    fun updateTextMask(value:String){
        _textMask.update { value }
    }

    fun updateCheckedIncludeDirs(value: Boolean){
        _checkedIncludeDirs.update { value }
    }

    fun updateCheckedIncludeFile(value: Boolean){
        _checkedIncludeFiles.update { value }
    }
}