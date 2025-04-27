import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.core.FileSearch.SearchResult
import org.example.core.FileSearch.api.FastSearch

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

    fun search() {
        viewModelScope.launch(Dispatchers.IO) {
            val results = mutableListOf<SearchResult>()
            FastSearch {
                path = _textPath.value.ifEmpty { "C:/" }
                content = _textQuery.value.ifEmpty { null }
                mask = _textMask.value.ifEmpty { null }
                includeDirs = _checkedIncludeDirs.value
                includeFiles = _checkedIncludeFiles.value
            }.run().collect { result ->
                println(result)
                results.add(result)
            }
            _resultSearch.update {
                results
            }
        }
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