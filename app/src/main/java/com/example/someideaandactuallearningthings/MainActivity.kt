package com.example.someideaandactuallearningthings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.someideaandactuallearningthings.ui.theme.SomeIdeaAndActualLearningThingsTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SomeIdeaAndActualLearningThingsTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SomeIdeaAndActualLearningThingsTheme {
        Greeting("Android")
    }
}

@HiltViewModel
class TodoListViewModel @Inject constructor(
    private val repository: TodoRepository
):ViewModel(){

}

interface TodoRepository {
    suspend fun insertTodo(todoInstance: Todo)
    suspend fun deleteTodo(todoInstance: Todo)
    suspend fun getSingleTodoById(todoIdForSearch: Int): Todo
    fun getAllTodos(): Flow<List<Todo>>
}

@Entity
 class Todo(//convert to data class after work
    @PrimaryKey val todoId: Int,
    val todoTitle: String,
    val todoDescription: String,//delete val and try to use like that
    val todoDoneBoolean: Boolean,

    )


