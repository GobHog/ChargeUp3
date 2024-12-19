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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.chargeup3.ui.theme.ChargeUp3Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Создаем экземпляр DatabaseHelper
        val dbHelper = DatabaseHelper(this)
        // Получаем базу данных (если база данных не существует, она будет создана)
        val db = dbHelper.writableDatabase
//        val contentValues = ContentValues()
//        contentValues.put("name", "Силовая тренировка")
//        contentValues.put("time_work", 30)
//        contentValues.put("time_relax", 15)
//        db.insert("workout", null, contentValues)


        setContent {
            Main()
        }
    }
}
@Composable
fun Main() {
    val navController = rememberNavController()

    // Получаем список тренировок из базы данных
    val dbHelper = DatabaseHelper(LocalContext.current)
    val db = dbHelper.readableDatabase
    val workouts = remember { getAllWorkouts(db) }

    Box(modifier = Modifier.fillMaxSize()) { // Растягиваем Box на весь экран
        // Основной контент (NavHost) занимает все пространство, кроме нижней панели
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)) {
            NavHost(navController, startDestination = NavRoutes.Home.route, modifier = Modifier.weight(1f)) {
                composable(NavRoutes.Home.route) { Home(workouts = workouts) } // Передаем workouts в Home
                composable(NavRoutes.Workout.route) { Workout() }
                composable(NavRoutes.Account.route) { Account() }
                composable(NavRoutes.Shop.route) { Shop() }
                composable(NavRoutes.Settings.route) { Settings() }
            }
        }

        // Панель навигации будет автоматически располагаться внизу благодаря align(Alignment.BottomCenter)
        BottomNavigationBar(navController = navController, modifier = Modifier.align(Alignment.BottomCenter))
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
fun Home(workouts: List<Workout>) {
    LazyColumn {
        items(workouts) { workout ->
            WorkoutItem(workout)
        }
    }
}


@Composable
fun WorkoutItem(workout: Workout) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text("Workout: ${workout.name}", fontSize = 20.sp)
        Text("Work time: ${workout.timeWork} minutes", fontSize = 16.sp)
        Text("Relax time: ${workout.timeRelax} minutes", fontSize = 16.sp)

        // Загружаем изображение из ресурсов с использованием имени файла
        val imageRes = workout.imagePath?.let {
            // Получаем ID ресурса по имени изображения
            val resourceId = LocalContext.current.resources.getIdentifier(it, "drawable", LocalContext.current.packageName)
            resourceId.takeIf { it != 0 } ?: R.drawable.img_power_simple // Используем дефолтное изображение, если не нашли
        } ?: R.drawable.img_power_simple // Используем дефолтное изображение, если нет пути

        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "Workout Image",
            modifier = Modifier.size(100.dp).padding(8.dp)
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

// Добавляем предпросмотр для Main
@Preview(showBackground = true)
@Composable
fun PreviewMain() {
    Main()
}
