import androidx.compose.foundation.layout.Column
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable


@Composable
fun UICard(path: String, isDirectory: Boolean) {
    Card {
        Column {
            Text(text = path)
            Text(text = if (isDirectory) "Directory" else "File")
        }
    }
}