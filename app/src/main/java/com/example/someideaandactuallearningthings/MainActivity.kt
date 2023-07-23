package com.example.someideaandactuallearningthings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.someideaandactuallearningthings.ui.theme.SomeIdeaAndActualLearningThingsTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
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



                }
            }
        }
    }
}
sealed class UiEvent{
    object OopBackOnStack:UiEvent()
    data class Navigate(val route:String):UiEvent()
    data class ShowSnackBar(val willShowMessage:String,val willDoOperation:String?=null):UiEvent()


}
@Entity
class TodoModel(
    val todoTitle:String,
    val todoDescription:String,
    val isDone:Boolean,
    @PrimaryKey val id :Int ?=null,
)
interface TodoRepository{
    suspend fun addNewTodo(todo:TodoModel)
    suspend fun deleteToTodo(todo:TodoModel)
    suspend fun getTodoById(id:Int):TodoModel?
    fun getAllTodos(): Flow<List<TodoModel>>
}
sealed class TodoListEvent{
    object AddingNewTodo:TodoListEvent()
    data class DeleteExistingTodo(val wantsToDeleteTodo:TodoModel):TodoListEvent()
    object UndoDeletedTodo:TodoListEvent()

    data class GoDetailOfTodo(val wantsToDetailTodo:TodoModel):TodoListEvent()

    data class TodoStateHasChanged(val willChangeTodo:TodoModel,val willState:Boolean):TodoListEvent()

}
object Routes{
    const val TODO_LIST_SCREEN_ROUTE ="todo_screen_list_route"
    const val TODO_EDIT_SCREEN_ROUTE="todo_edit_screen_route"
}
@HiltViewModel
class TodoListViewModel @Inject constructor(private val todoRepositoryInterfaceInstance:TodoRepository):ViewModel(){
    val allTodos = todoRepositoryInterfaceInstance.getAllTodos()

    private val _uiEvent = Channel< UiEvent>()

    val uiEvent = _uiEvent.receiveAsFlow()

    private var deletedTodoForTakeBack :TodoModel?=null

    fun todoListOperations(events:TodoListEvent){
        when(events){
            is TodoListEvent.DeleteExistingTodo -> {
                viewModelScope.launch {
                    deletedTodoForTakeBack =events.wantsToDeleteTodo
                    todoRepositoryInterfaceInstance.deleteToTodo(events.wantsToDeleteTodo)
                    showActionOnScreen(UiEvent.ShowSnackBar(willShowMessage = "do you want to do that ? ", willDoOperation = "undo"))
                }
            }
            is TodoListEvent.UndoDeletedTodo -> {
                deletedTodoForTakeBack?.let {todoModelInstance->
                    viewModelScope.launch {
                        todoRepositoryInterfaceInstance.addNewTodo(todoModelInstance)
                    }
                }
            }
            is TodoListEvent.TodoStateHasChanged -> {
                viewModelScope.launch {
                    todoRepositoryInterfaceInstance.addNewTodo(events.willChangeTodo)
                }
            }
            is TodoListEvent.GoDetailOfTodo->{
                showActionOnScreen(UiEvent.Navigate(Routes.TODO_EDIT_SCREEN_ROUTE+"todoId=${events.wantsToDetailTodo.id}"))
            }
            is TodoListEvent.AddingNewTodo -> {
                showActionOnScreen(UiEvent.Navigate(Routes.TODO_EDIT_SCREEN_ROUTE))
            }
        }
    }
    private fun showActionOnScreen(events: UiEvent){
        viewModelScope.launch {
            _uiEvent.send(events)
        }
    }

}
@Composable
fun SingleTodoLook(todoModelInstance : TodoModel,eventOnItem:(TodoListEvent)->Unit,modifier: Modifier=Modifier){
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {  
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = todoModelInstance.todoTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,

            )
            Spacer(modifier = Modifier.height(8.dp))

            IconButton(onClick = {eventOnItem(TodoListEvent.DeleteExistingTodo(todoModelInstance))}) {
                Icon(imageVector = Icons.Default.Delete, contentDescription ="Delete todo")
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            todoModelInstance.todoDescription?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it)
            }

        }


    }
    androidx.compose.material3.Checkbox(
        checked = todoModelInstance.isDone,
        onCheckedChange = { newStateForDone->
            eventOnItem(TodoListEvent.TodoStateHasChanged(todoModelInstance,newStateForDone))
        })

}

@Composable
fun TodoListScreen(onNavigate:(UiEvent.Navigate) -> Unit,viewModel:TodoListViewModel= hiltViewModel()){
    val todos =viewModel.allTodos.collectAsState(initial = emptyList())
    val scaffoldState= rememberScaffoldState()

    LaunchedEffect(key1 = true){
        viewModel.uiEvent.collect{event ->
            when(event){
                is UiEvent.ShowSnackBar ->{
                    val result = scaffoldState.snackbarHostState.showSnackbar(
                        message =event.willShowMessage,
                        actionLabel = event.willDoOperation

                    )
                    if (result == SnackbarResult.ActionPerformed){
                        viewModel.todoListOperations(TodoListEvent.UndoDeletedTodo)
                    }
                }
                is UiEvent.Navigate -> onNavigate(event)
                else -> Unit
            }
        }
    }
    Scaffold(scaffoldState = scaffoldState, floatingActionButton = { FloatingActionButton(onClick = { viewModel.todoListOperations(TodoListEvent.AddingNewTodo) }) {
        Icon(imageVector = Icons.Default.Add, contentDescription = "Add new todo")
    }}) {
        LazyColumn(modifier = Modifier.fillMaxSize()){
            items(todos.value.size){todoLengthInLazyColumn->
                SingleTodoLook(
                    todoModelInstance = todos.value[todoLengthInLazyColumn],
                    eventOnItem = viewModel::todoListOperations,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.todoListOperations(TodoListEvent.GoDetailOfTodo(todos.value[todoLengthInLazyColumn])) }
                        .padding(16.dp)
                )

            }
        }
    }
}}

