import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
@Preview
fun App() {

    val viewModel = viewModel { MainViewModel() }

    val textPath by viewModel.textPath.collectAsState()
    val textQuery by viewModel.textQuery.collectAsState()
    val textMask by viewModel.textMask.collectAsState()
    val checkedIncludeDirs by viewModel.checkedIncludeDirs.collectAsState()
    val checkedIncludeFiles by viewModel.checkedIncludeFiles.collectAsState()
    val resultSearch by viewModel.resultSearch.collectAsState()
    val durationMs by viewModel.searchDurationMs.collectAsState()
    val precision by viewModel.precision.collectAsState()
    val recall by viewModel.recall.collectAsState()

    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(state = rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Путь поиска")
                TextField(value = textPath, onValueChange = { it -> viewModel.updateTextPath(value = it) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Поиск внутри файлов")
                TextField(value = textQuery, onValueChange = { it -> viewModel.updateTextQuery(value = it) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Маска")
                TextField(value = textMask, onValueChange = { it -> viewModel.updateTextMask(value = it) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Искать внутри папок")
                Checkbox(
                    checked = checkedIncludeDirs,
                    onCheckedChange = { it -> viewModel.updateCheckedIncludeDirs(it) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Искать файлы")
                Checkbox(
                    checked = checkedIncludeFiles,
                    onCheckedChange = { it -> viewModel.updateCheckedIncludeFile(it) })
            }
            Button(onClick = {
                viewModel.search()
            }) {
                Text("Search")
            }

            Text(text = "Найдено ${resultSearch.size} результатов")
            Text(text = "Время выполнения: $durationMs мс")
            Text(text = "Precision: ${(precision * 100).format(2)}%")
            Text(text = "Recall: ${(recall * 100).format(2)}%")

            Column {
                resultSearch.forEach { result ->
                    println("result: $result")
                    UICard(path = result.path, isDirectory = result.isDirectory)
                }
            }
        }
    }
}


fun Double.format(digits: Int) = "%.${digits}f".format(this)


fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
