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
                // Показываем/скрываем панель навигации в зависимости от состояния
                if (navBarViewModel.showNavBar.value) {
                    BottomNavigationBar(navController = navController)
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues) // Используем paddingValues, переданные в Scaffold
            ) {
                NavHost(
                    navController,
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
                        // Скрываем панель навигации при переходе на экран упражнений
                        navBarViewModel.setShowNavBar(false)
                        ExercisesScreen(workoutId = workoutId, db = db, navController=navController)
                    }
                    composable(
                        route = "workout_session/{workoutId}",
                        arguments = listOf(navArgument("workoutId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val workoutId = backStackEntry.arguments?.getInt("workoutId") ?: return@composable
                        val workout = getWorkoutById(db, workoutId)
                        val exercises = getExercisesByWorkoutId(db, workoutId)
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
    // Получаем упражнения, связанные с workoutId
    val exercises = remember { getExercisesByWorkoutId(db, workoutId) }

    // Получаем время упражнения из таблицы workout
    val workoutTime = remember { getWorkoutTime(db, workoutId) }

    // Контейнер для всего контента с черным фоном
    Column(
        modifier = Modifier
            .fillMaxSize() // Занимает весь экран
            .background(Color.Black) // Черный фон для всей страницы
            .padding(16.dp) // Отступы вокруг всего контента
    ) {
        // Оборачиваем LazyColumn в Box с закругленными углами
        Box(
            modifier = Modifier
                .fillMaxWidth() // Занимает всю ширину экрана
                .clip(RoundedCornerShape(16.dp)) // Закругленные углы
                .background(Color(0xFF656566)) // Цвет фона LazyColumn в формате HEX
                .padding(8.dp) // Отступы внутри контейнера Box
                .weight(1f) // Отнимаем пространство, чтобы кнопка не зашла за LazyColumn
        ) {
            // Лист упражнений
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth() // Занимает всю ширину
            ) {
                items(exercises) { exercise ->
                    val imageRes = exercise.imagePath?.let {
                        LocalContext.current.resources.getIdentifier(it, "drawable", LocalContext.current.packageName)
                            .takeIf { resId -> resId != 0 } ?: R.drawable.img_power_simple
                    } ?: R.drawable.img_power_simple

                    // Каждый элемент списка с изображением
                    Row(
                        modifier = Modifier
                            .padding(vertical = 10.dp) // Вертикальные отступы между элементами
                            .fillMaxWidth()
                    ) {
                        // Изображение упражнения
                        Box(
                            modifier = Modifier
                                .padding(start = 37.dp) // Отступ слева от LazyColumn
                                .size(120.dp, 99.dp) // Устанавливаем размер изображения
                                .clip(RoundedCornerShape(8.dp)) // Закругленные углы
                                .background(Color.Gray) // Фон для изображения (можно настроить)
                        ) {
                            Image(
                                painter = painterResource(id = imageRes),
                                contentDescription = "Exercise Image",
                                contentScale = ContentScale.Crop, // Кадрируем изображение, чтобы оно заполнило Box
                                modifier = Modifier.fillMaxSize() // Изображение заполняет весь контейнер
                            )
                        }

                        // Текст упражнения и время
                        Column(
                            modifier = Modifier
                                .padding(start = 10.dp) // Отступ слева от изображения
                                .align(Alignment.Top) // Выравнивание по верхнему краю
                        ) {
                            // Название упражнения
                            Text(
                                text = exercise.name ?: "Название упражнения",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Время упражнения
                            Text(
                                text = "Время: $workoutTime сек",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Кнопка "Начать зарядку"
        Button(
            onClick = {
                navController.navigate("workout_session/${workoutId}") // Переход с передачей workoutId
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0264F6))
        ) {
            Text(
                text = "Начать зарядку",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

    }
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
    val currentExerciseIndex = remember { mutableStateOf(0) }
    val isResting = remember { mutableStateOf(false) }
    val currentTime = remember { mutableStateOf(0) }
    val totalTime = remember { mutableStateOf(0) }
    val isPaused = remember { mutableStateOf(false) }
    val trainingEnded = remember { mutableStateOf(false) }

    val isPreparation = remember { mutableStateOf(true) }
    val preStartTime = remember { mutableStateOf(10) }

    val maxTime = remember { exercises.size * workout.timeWork }

    // Счётчик общего прошедшего времени
    val elapsedTime = remember { mutableStateOf(0) }

    // Когда подготовка заканчивается, начинаем считать общее время
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

    // Обратный отсчёт перед началом
    LaunchedEffect(Unit) {
        while (preStartTime.value > 0 && isPreparation.value) {
            delay(1000L)
            preStartTime.value -= 1
        }
        if (preStartTime.value == 0) {
            isPreparation.value = false
        }
    }

    // Логика тренировки
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
                // Закончили упражнение
                if (currentExerciseIndex.value < exercises.size - 1) {
                    isResting.value = true
                } else {
                    trainingEnded.value = true
                }
            } else {
                // Закончили отдых
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

    // Форматирование затраченного времени (elapsedTime) в мм:сс
    val minutes = elapsedTime.value / 60
    val seconds = elapsedTime.value % 60
    val formattedTime = String.format("%d:%02d", minutes, seconds)

    // Диалог завершения тренировки
    if (trainingEnded.value) {
        AlertDialog(
            onDismissRequest = { },
            title = null,
            text = {
                // Вся кастомная разметка внутри text
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
                            modifier = Modifier.size(200.dp) // подберите нужный размер
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
                        // Кнопка "Закрыть"
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
            confirmButton = {}, // Не используем стандартные кнопки
            dismissButton = {},
            shape = RoundedCornerShape(25.dp),
            containerColor = Color(0xFF282828)
        )
    }

    if (isPreparation.value && !trainingEnded.value) {
        // Экран подготовки
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
        // Основной экран тренировки
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Верхняя картинка (524dp)
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

                // Кнопка завершения тренировки (верхняя)
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

                // Название упражнения или "Отдых"
                Text(
                    text = if (isResting.value) "Отдых" else exercises.getOrNull(currentExerciseIndex.value)?.name ?: "Завершено",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OswaldFontFamily,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                )
            }

            // Отступ в 20 dp между картинкой и нижним контейнером
            Spacer(modifier = Modifier.height(20.dp))

            // Контейнер снизу
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
                    // Прогрессбар
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

                    // Общее время
                    Text(
                        text = "Общее время: ${totalTime.value} сек",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OswaldFontFamily
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Текущее время
                    Text(
                        text = "${currentTime.value} сек",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OswaldFontFamily
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Кнопка паузы
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


















@Composable
fun NavigationGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "workoutSession"
    ) {
        composable("workoutSession") {
            val workout = Workout(1, "Full Body", "path1.jpg", 20, 10)
            val exercises = listOf(
                Exercise(1, "Push-Ups", "path2.jpg", 1),
                Exercise(2, "Squats", "path3.jpg", 1),
                Exercise(3, "Plank", "path4.jpg", 1)
            )

            WorkoutSessionScreen(workout, exercises, navController)
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
