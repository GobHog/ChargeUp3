package com.example.chargeup3

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.chargeup3.ui.theme.ChargeUp3Theme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Создаем экземпляр DatabaseHelper
        val dbHelper = DatabaseHelper(this)
        // Получаем базу данных (если база данных не существует, она будет создана)
        val db = dbHelper.writableDatabase

        setContent {
            Main()
        }
    }
}
@Composable
fun Main() {
    val navController = rememberNavController()
    val navBarViewModel: NavBarViewModel = viewModel()
    val dbHelper = DatabaseHelper(LocalContext.current)
    val db = dbHelper.readableDatabase
    val workouts = remember { getAllWorkouts(db) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                // Show/hide bottom navigation based on ViewModel state
                if (navBarViewModel.showNavBar.value) {
                    BottomNavigationBar(navController = navController)
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues) // Apply padding from Scaffold
            ) {
                NavHost(
                    navController = navController,
                    startDestination = NavRoutes.Home.route,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(NavRoutes.Home.route) {
                        Home(navController = navController, workouts = workouts)
                    }
                    composable(NavRoutes.Workout.route) { Workout() }
                    composable(NavRoutes.Account.route) { Account() }
                    composable(NavRoutes.Shop.route) { Shop() }
                    composable(NavRoutes.Settings.route) { Settings() }
                    composable(
                        route = "exercises/{workoutId}",
                        arguments = listOf(navArgument("workoutId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val workoutId = backStackEntry.arguments?.getInt("workoutId") ?: 0
                        // Hide bottom navigation when on ExercisesScreen
                        navBarViewModel.setShowNavBar(false)
                        ExercisesScreen(workoutId = workoutId, db = db, navController = navController)
                    }
                    composable(
                        route = "workout_session/{workoutId}",
                        arguments = listOf(navArgument("workoutId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val workoutId = backStackEntry.arguments?.getInt("workoutId") ?: return@composable
                        val workout = getWorkoutById(db, workoutId)
                        val exercises = getExercisesByWorkoutId(db, workoutId)
                        // Show bottom navigation during workout session if desired
                        navBarViewModel.setShowNavBar(false)
                        WorkoutSessionScreen(workout, exercises, navController)
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController, modifier: Modifier = Modifier) {
    NavigationBar(
        containerColor = Color(0xFF282828), // Цвет фона панели навигации
        modifier = modifier // Применяем переданный modifier для выравнивания панели по низу
            .fillMaxWidth() // Занимаем всю ширину экрана
            .height(72.dp) // Увеличиваем высоту панели
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) // Закругляем только верхние углы
            .padding(0.dp) // Убираем все отступы
    ) {
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        NavBarItems.BarItems.forEach { navItem -> // Создаем элементы панели навигации
            val isSelected = currentRoute == navItem.route

            NavigationBarItem(
                selected = false, // Отключаем стандартное выделение
                onClick = {
                    navController.navigate(navItem.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    // Меняем изображение иконки в зависимости от состояния (выбрана/не выбрана)
                    val imageRes = if (isSelected) navItem.selectedImage else navItem.unselectedImage
                    val imagePainter = painterResource(id = imageRes)

                    Box(
                        contentAlignment = Alignment.Center, // Центрируем изображение внутри Box
                        modifier = Modifier
                            .size(48.dp) // Фиксируем размер фона (можно настроить в зависимости от нужд)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) // Кастомный фон для выбранной иконки
                                else Color.Transparent, // Прозрачный фон для остальных
                                shape = MaterialTheme.shapes.small // Закругление углов
                            )
                            .padding(8.dp) // Внутренний отступ для иконки
                    ) {
                        Image(
                            painter = imagePainter,
                            contentDescription = navItem.title,
                            modifier = Modifier.fillMaxSize() // Иконка заполняет Box
                        )
                    }
                }
            )
        }
    }
}
object NavBarItems {
    val BarItems = listOf(
        BarItem(
            title = "Home",
            unselectedImage = R.drawable.ic_white_home,
            selectedImage = R.drawable.ic_blue_home,
            route = "home"
        ),
        BarItem(
            title = "Workout",
            unselectedImage = R.drawable.ic_white_workout,
            selectedImage = R.drawable.ic_blue_workout,
            route = "workout"
        ),
        BarItem(
            title = "Account",
            unselectedImage = R.drawable.ic_white_account,
            selectedImage = R.drawable.ic_blue_account,
            route = "account"
        ),
        BarItem(
            title = "Shop",
            unselectedImage = R.drawable.ic_white_shop,
            selectedImage = R.drawable.ic_blue_shop,
            route = "shop"
        ),
        BarItem(
            title = "Settings",
            unselectedImage = R.drawable.ic_white_settings,
            selectedImage = R.drawable.ic_blue_settings,
            route = "settings"
        )
    )
}

data class BarItem(
    val title: String,
    val unselectedImage: Int,
    val selectedImage: Int,
    val route: String
)

@Composable
fun Home(navController: NavController, workouts: List<Workout>) {
    LazyColumn {
        items(workouts) { workout ->
            WorkoutItem(workout = workout) {
                    workoutId -> navController.navigate("exercises/$workoutId")
            }
        }
    }
}
@Composable
fun WorkoutItem(workout: Workout, onClick: (Int) -> Unit) {
    val imageRes = workout.imagePath?.let {
        LocalContext.current.resources.getIdentifier(it, "drawable", LocalContext.current.packageName)
            .takeIf { resId -> resId != 0 } ?: R.drawable.img_power_simple
    } ?: R.drawable.img_power_simple

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp // Ширина экрана в dp
    val buttonWidth = screenWidth - 40.dp // Ширина кнопки (на 40dp меньше экрана)

    Box(
        modifier = Modifier
            .padding(8.dp)
            .width(buttonWidth) // Устанавливаем ширину
            .height(150.dp) // Устанавливаем фиксированную высоту
            .clip(RoundedCornerShape(16.dp)) // Скругляем углы
            .background(Color.Gray) // Фон кнопки (можно заменить на другой цвет)
            .clickable { onClick(workout.id) } // Обработчик клика
        //.align(Alignment.CenterHorizontally) // Центрируем по ширине
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "Workout Image",
            contentScale = ContentScale.Crop, // Кадрируем изображение, чтобы заполнить всю область
            modifier = Modifier.fillMaxSize()
        )
    }
}





@SuppressLint("Range")
fun getAllWorkouts(db: SQLiteDatabase): List<Workout> {
    val workouts = mutableListOf<Workout>()
    val cursor = db.rawQuery("SELECT * FROM workout", null)

    if (cursor != null && cursor.moveToFirst()) {
        do {
            val id = cursor.getInt(cursor.getColumnIndex("id"))
            val name = cursor.getString(cursor.getColumnIndex("name"))
            val timeWork = cursor.getInt(cursor.getColumnIndex("time_work"))
            val timeRelax = cursor.getInt(cursor.getColumnIndex("time_relax"))
            val imagePath = cursor.getString(cursor.getColumnIndex("image_path")) // Получаем путь к изображению

            // Проверяем, чтобы имя и другие данные не были пустыми или null
            if (name != null && timeWork >= 0 && timeRelax >= 0) {
                workouts.add(Workout(id, name,imagePath, timeWork, timeRelax))
            }
        } while (cursor.moveToNext())
    }

    cursor?.close()
    return workouts
}
fun insertWorkout(db: SQLiteDatabase, name: String, timeWork: Int, timeRelax: Int, imagePath: String) {
    val contentValues = ContentValues()
    contentValues.put("name", name)
    contentValues.put("time_work", timeWork)
    contentValues.put("time_relax", timeRelax)
    contentValues.put("image_path", imagePath) // Путь к изображению

    db.insert("workout", null, contentValues)
}
fun insertExercise(db: SQLiteDatabase, name: String, workoutId: Int, imagePath: String) {
    val contentValues = ContentValues()
    contentValues.put("name", name)
    contentValues.put("image_path", imagePath)
    contentValues.put("workout_id", workoutId)

    db.insert("exercise", null, contentValues)
}
@Composable
fun Workout() {
    Text("Contact Page", fontSize = 30.sp)
}

@Composable
fun Account() {
    Text("About Page", fontSize = 30.sp)
}

@Composable
fun Shop() {
    Text("Shop Page", fontSize = 30.sp)
}

@Composable
fun Settings() {
    Text("Settings Page", fontSize = 30.sp)
}

sealed class NavRoutes(val route: String) {
    object Home : NavRoutes("home")
    object Workout : NavRoutes("workout")
    object Account : NavRoutes("account")
    object Shop : NavRoutes("shop")
    object Settings : NavRoutes("settings")
}
@Composable
fun ExercisesScreen(workoutId: Int, db: SQLiteDatabase, navController: NavController) {
    // Retrieve the Workout data
    val workout = remember { getWorkoutById(db, workoutId) }
    val exercises = remember { getExercisesByWorkoutId(db, workoutId) }

    // Retrieve workout times
    val workoutTime = remember { getWorkoutTime(db, workoutId) }

    // Main UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp) // Overall padding for the screen
    ) {
        // Workout Image with Back Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(244.dp) // Set height to 244dp
                .padding( top = 35.dp) // Adjusted left and top padding
        ) {
            // Workout Image with Rounded Corners
            Image(
                painter = painterResource(id = getImageResId(workout.imagePath)),
                contentDescription = "Workout Image",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(25.dp)) // Apply rounded corners
                    .align(Alignment.CenterStart), // Align to the start
                contentScale = ContentScale.Crop
            )

            // Back button overlay (unchanged)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.TopStart)
                     // Semi-transparent background
                    .clickable {
                        navController.popBackStack()
                        // Optionally, show the navigation bar again if needed
                        // navBarViewModel.setShowNavBar(true)
                    }
                    .padding(8.dp) // Padding inside the back button box
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Back",
                    modifier = Modifier.fillMaxSize() // Tint the back icon to white
                )
            }
        }

        // Spacer between image and exercises list
        Spacer(modifier = Modifier.height(16.dp))

        // Exercises Container with "Упражнения" and "Перерыв - [time_relax] сек"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)) // Rounded corners for the container
                .background(Color(0xFF656566)) // Background color
                .padding(16.dp) // Padding inside the container
                .weight(1f) // Occupies remaining space
        ) {
            Column {
                // Title "Упражнения"
                Text(
                    text = "Упражнения",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OswaldFontFamily,
                    modifier = Modifier.padding(bottom = 8.dp) // Spacing below the title
                )

                // Subtitle "Перерыв - [time_relax] сек"
                Text(
                    text = "Перерыв - ${workout.timeRelax} сек",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = OswaldFontFamily,
                    modifier = Modifier.padding(bottom = 16.dp) // Spacing below the subtitle
                )

                // Exercises List
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(exercises) { exercise ->
                        ExerciseItem(exercise = exercise, workoutTime = workoutTime)
                    }
                }
            }
        }

        // Start Workout Button
        Button(
            onClick = {
                navController.navigate("workout_session/$workoutId") // Navigate to workout session
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp) // Padding around the button
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp)), // Rounded corners for the button
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0264F6)) // Button color
        ) {
            Text(
                text = "Начать зарядку",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OswaldFontFamily
            )
        }
    }

    // Composable for each Exercise Item
    @Composable
    fun ExerciseItem(exercise: Exercise, workoutTime: String) {
        val context = LocalContext.current
        val imageRes = getImageResId(exercise.imagePath)

        Row(
            modifier = Modifier
                .padding(vertical = 10.dp)
                .fillMaxWidth()
        ) {
            // Exercise Image
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 99.dp)
                    .clip(RoundedCornerShape(8.dp)) // Rounded corners for exercise image
                    .background(Color.Gray) // Background color
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = "Exercise Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Exercise Name and Time
            Column(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .align(Alignment.CenterVertically) // Center vertically
            ) {
                // Exercise Name
                Text(
                    text = exercise.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OswaldFontFamily
                )

                // Exercise Time
                Text(
                    text = "Время: $workoutTime сек",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontFamily = OswaldFontFamily
                )
            }
        }
    }

    // Helper function to get image resource ID from imagePath
    @Composable
    fun getImageResId(imagePath: String?): Int {
        val context = LocalContext.current
        return imagePath?.let {
            context.resources.getIdentifier(it, "drawable", context.packageName)
                .takeIf { resId -> resId != 0 } ?: R.drawable.img_power_simple
        } ?: R.drawable.img_power_simple
    }
}


// Composable for each Exercise Item
@Composable
fun ExerciseItem(exercise: Exercise, workoutTime: String) {
    val context = LocalContext.current
    val imageRes = exercise.imagePath?.let {
        context.resources.getIdentifier(it, "drawable", context.packageName)
            .takeIf { resId -> resId != 0 } ?: R.drawable.img_power_simple
    } ?: R.drawable.img_power_simple

    Row(
        modifier = Modifier
            .padding(vertical = 10.dp)
            .fillMaxWidth()
    ) {
        // Exercise Image
        Box(
            modifier = Modifier
                .size(width = 120.dp, height = 99.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Gray)
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "Exercise Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Exercise Name and Time
        Column(
            modifier = Modifier
                .padding(start = 10.dp)
                .align(Alignment.CenterVertically) // Center vertically
        ) {
            // Exercise Name
            Text(
                text = exercise.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OswaldFontFamily
            )

            // Exercise Time
            Text(
                text = "Время: $workoutTime сек",
                color = Color.Gray,
                fontSize = 14.sp,
                fontFamily = OswaldFontFamily
            )
        }
    }
}



// Helper function to get image resource ID from imagePath
@Composable
fun getImageResId(imagePath: String?): Int {
    val context = LocalContext.current
    return imagePath?.let {
        context.resources.getIdentifier(it, "drawable", context.packageName)
            .takeIf { resId -> resId != 0 } ?: R.drawable.img_power_simple
    } ?: R.drawable.img_power_simple
}


@SuppressLint("Range")
fun getWorkoutTime(db: SQLiteDatabase, workoutId: Int): String {
    // Предположим, что время хранится в столбце "time" в таблице "workouts"
    val cursor = db.rawQuery("SELECT time_work FROM workout WHERE id = ?", arrayOf(workoutId.toString()))
    return if (cursor.moveToFirst()) {
        cursor.getString(cursor.getColumnIndex("time_work")) ?: "0"
    } else {
        "0"
    }
}
@SuppressLint("Range")
fun getExercisesByWorkoutId(db: SQLiteDatabase, workoutId: Int): List<Exercise> {
    val exercises = mutableListOf<Exercise>()
    val cursor = db.rawQuery("SELECT * FROM exercise WHERE workout_id = ?", arrayOf(workoutId.toString()))

    if (cursor != null && cursor.moveToFirst()) {
        do {
            val id = cursor.getInt(cursor.getColumnIndex("id"))
            val name = cursor.getString(cursor.getColumnIndex("name"))
            val workoutId = cursor.getInt(cursor.getColumnIndex("workout_id"))
            val imagePath = cursor.getString(cursor.getColumnIndex("image_path"))  // Получаем путь к изображению
            exercises.add(Exercise(id, name, imagePath, workoutId))
        } while (cursor.moveToNext())
    }

    cursor?.close()
    return exercises
}

// Функция для получения времени отдыха
@SuppressLint("Range")
fun getWorkoutRelaxTime(db: SQLiteDatabase, workoutId: Int): Int {
    val cursor = db.rawQuery("SELECT time_relax FROM workout WHERE id = ?", arrayOf(workoutId.toString()))
    return if (cursor.moveToFirst()) {
        cursor.getInt(cursor.getColumnIndex("time_relax"))
    } else {
        0
    }.also { cursor.close() }
}
// Где-нибудь в вашем коде, например в том же файле, вне @Composable
val OswaldFontFamily = FontFamily(Font(R.font.oswald))

@Composable
fun WorkoutSessionScreen(workout: Workout, exercises: List<Exercise>, navController: NavController) {
    // State variables for workout session
    val currentExerciseIndex = remember { mutableStateOf(0) }
    val isResting = remember { mutableStateOf(false) }
    val currentTime = remember { mutableStateOf(0) }
    val totalTime = remember { mutableStateOf(0) }
    val isPaused = remember { mutableStateOf(false) }
    val trainingEnded = remember { mutableStateOf(false) }

    val isPreparation = remember { mutableStateOf(true) }
    val preStartTime = remember { mutableStateOf(10) }

    val maxTime = remember { exercises.size * workout.timeWork }

    // Elapsed time counter
    val elapsedTime = remember { mutableStateOf(0) }

    // Start counting elapsed time after preparation
    LaunchedEffect(isPreparation.value) {
        if (!isPreparation.value && !trainingEnded.value) {
            while (!trainingEnded.value) {
                delay(1000L)
                elapsedTime.value += 1
            }
        }
    }

    val context = LocalContext.current
    val topImageRes = if (isResting.value) {
        R.drawable.rest_image
    } else {
        val exercise = exercises.getOrNull(currentExerciseIndex.value)
        val imageName = exercise?.imagePath ?: "default_exercise_image"
        val drawableId = context.resources.getIdentifier(imageName, "drawable", context.packageName)
        if (drawableId != 0) drawableId else R.drawable.default_exercise_image
    }

    // Preparation countdown
    LaunchedEffect(Unit) {
        while (preStartTime.value > 0 && isPreparation.value) {
            delay(1000L)
            preStartTime.value -= 1
        }
        if (preStartTime.value == 0) {
            isPreparation.value = false
        }
    }

    // Workout logic
    LaunchedEffect(currentExerciseIndex.value, isResting.value, isPreparation.value) {
        if (!isPreparation.value && !trainingEnded.value) {
            val duration = if (isResting.value) workout.timeRelax else workout.timeWork

            currentTime.value = duration
            while (currentTime.value > 0) {
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < 1000) {
                    delay(100L)
                    while (isPaused.value) {
                        delay(100L)
                    }
                }

                currentTime.value -= 1
                if (!isResting.value) {
                    totalTime.value += 1
                }
            }

            if (!isResting.value) {
                // Ended exercise, start rest or finish
                if (currentExerciseIndex.value < exercises.size - 1) {
                    isResting.value = true
                } else {
                    trainingEnded.value = true
                }
            } else {
                // Ended rest, move to next exercise or finish
                isResting.value = false
                if (currentExerciseIndex.value < exercises.size - 1) {
                    currentExerciseIndex.value += 1
                } else {
                    trainingEnded.value = true
                }
            }
        }
    }

    val progress = if (maxTime > 0) totalTime.value.toFloat() / maxTime.toFloat() else 0f

    // Format elapsed time
    val minutes = elapsedTime.value / 60
    val seconds = elapsedTime.value % 60
    val formattedTime = String.format("%d:%02d", minutes, seconds)

    // Dialog for training completion
    if (trainingEnded.value) {
        AlertDialog(
            onDismissRequest = { },
            title = null,
            text = {
                // Custom layout inside the dialog
                Box(
                    modifier = Modifier
                        .size(width = 336.dp, height = 525.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_completed),
                            contentDescription = null,
                            modifier = Modifier.size(100.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Image(
                            painter = painterResource(id = R.drawable.txt_congratulation),
                            contentDescription = null,
                            modifier = Modifier.size(200.dp) // Adjust size as needed
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Вы выполнили тренировку: ${workout.name}",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = OswaldFontFamily
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Затраченное время: $formattedTime",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = OswaldFontFamily
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        // Close button
                        TextButton(onClick = { navController.popBackStack() }) {
                            Text(
                                text = "Закрыть",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontFamily = OswaldFontFamily
                            )
                        }
                    }
                }
            },
            confirmButton = {}, // No standard buttons
            dismissButton = {},
            shape = RoundedCornerShape(25.dp),
            containerColor = Color(0xFF282828)
        )
    }

    if (isPreparation.value && !trainingEnded.value) {
        // Preparation screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Приготовьтесь",
                color = Color(0xFF0264F6),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OswaldFontFamily
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "${preStartTime.value}",
                color = Color(0xFF0264F6),
                fontSize = 96.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OswaldFontFamily
            )
        }
    } else if (!trainingEnded.value) {
        // Main workout screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Top Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(524.dp)
                    .clip(RoundedCornerShape(bottomStart = 25.dp, bottomEnd = 25.dp))
            ) {
                Image(
                    painter = painterResource(id = topImageRes),
                    contentDescription = "Top Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Close Workout Button
                Image(
                    painter = painterResource(id = R.drawable.ic_close_workout),
                    contentDescription = "Завершить тренировку",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(48.dp)
                        .clickable {
                            navController.popBackStack()
                        }
                )

                // Exercise Name or "Rest"
                Text(
                    text = if (isResting.value) "Отдых" else exercises.getOrNull(
                        currentExerciseIndex.value
                    )?.name
                        ?: "Завершено",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OswaldFontFamily,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                )
            }

            // Spacer between image and bottom container
            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(365.dp)
                    .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                    .background(Color(0xFF202020))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Progress Bar
                    LinearProgressIndicator(
                        progress = progress.coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF0264F6),
                        trackColor = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Total Time
                    Text(
                        text = "Общее время: ${totalTime.value} сек",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OswaldFontFamily
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Current Time
                    Text(
                        text = "${currentTime.value} сек",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OswaldFontFamily
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Pause Button
                    val pauseIcon = if (isPaused.value) R.drawable.ic_play else R.drawable.ic_pause
                    Image(
                        painter = painterResource(id = pauseIcon),
                        contentDescription = if (isPaused.value) "Продолжить" else "Пауза",
                        modifier = Modifier
                            .size(64.dp)
                            .clickable {
                                isPaused.value = !isPaused.value
                            }
                    )
                }
            }
        }
    }
}

@SuppressLint("Range")
fun getWorkoutById(db: SQLiteDatabase, workoutId: Int): Workout {
    val cursor = db.rawQuery("SELECT * FROM workout WHERE id = ?", arrayOf(workoutId.toString()))
    return if (cursor != null && cursor.moveToFirst()) {
        val id = cursor.getInt(cursor.getColumnIndex("id"))
        val name = cursor.getString(cursor.getColumnIndex("name"))
        val timeWork = cursor.getInt(cursor.getColumnIndex("time_work"))
        val timeRelax = cursor.getInt(cursor.getColumnIndex("time_relax"))
        val imagePath = cursor.getString(cursor.getColumnIndex("image_path"))

        Workout(id, name, imagePath, timeWork, timeRelax)
    } else {
        Workout(0, "Unknown", "", 0, 0) // Возвращаем заглушку, если не найдено
    }
}



// Добавляем предпросмотр для Main
@Preview(showBackground = true)
@Composable
fun PreviewMain() {
    Main()
}