package com.example.dailytodoapp

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: affectedTodoAdapter
    private lateinit var sharedPref: SharedPreferences
    private lateinit var fab: FloatingActionButton

    // Базовые задачи
    private val baseTodos = listOf(
        TodoItem(1, "Выпить кофе"),
        TodoItem(2, "Прогулка на свежем воздухе"),
        TodoItem(3, "Прочитать 10 страниц книги"),
        TodoItem(4, "Заняться спортом 30 мин"),
        TodoItem(5, "Позвонить другу")
    )

    // Специальные
    private val SPECIAL_RUN_ID = 6
    private val SPECIAL_WORKOUT_ID = 7
    private val SPECIAL_BASE_RUN = 5
    private val SPECIAL_BASE_WORKOUT = 10

    // Прогрессирующие задачи (ID >= 1000)
    private val PROGRESSIVE_ID_START = 1000

    private var currentTodos: MutableList<TodoItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPref = getSharedPreferences("todo_prefs", Context.MODE_PRIVATE)
        recyclerView = findViewById(R.id.recyclerView)
        fab = findViewById(R.id.fab)
        recyclerView.layoutManager = LinearLayoutManager(this)

        fab.setOnClickListener { showAddTaskDialog() }

        // Адаптер
        adapter = TodoAdapter(
            currentTodos,
            { todo, pos -> toggleTodo(todo, pos) },
            { todo, pos -> showEditOrDeleteDialog(todo, pos) }
        )
        recyclerView.adapter = adapter

        // loadTodos() перенесён в onResume()
    }

    override fun onResume() {
        super.onResume()
        loadTodos() // КАЖДЫЙ РАЗ ПРИ ОТКРЫТИИ — ГАРАНТИРОВАННАЯ ИНКРЕМЕНТАЦИЯ
    }

    private fun loadTodos() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastResetDate: String = sharedPref.getString("last_reset_date", "") ?: ""
        var startDate: String = sharedPref.getString("start_date", "") ?: ""

        if (startDate.isEmpty()) {
            startDate = today
            sharedPref.edit().putString("start_date", today).apply()
        }

        val taskIdsSet: Set<String> = sharedPref.getStringSet(
            "task_ids",
            setOf("1", "2", "3", "4", "5", SPECIAL_RUN_ID.toString(), SPECIAL_WORKOUT_ID.toString())
        ) ?: setOf("1", "2", "3", "4", "5", SPECIAL_RUN_ID.toString(), SPECIAL_WORKOUT_ID.toString())

        currentTodos.clear()

        // === БАЗОВЫЕ ===
        baseTodos.forEach { base ->
            if (taskIdsSet.contains(base.id.toString())) {
                val savedTitle = sharedPref.getString("task_${base.id}_title", base.title) ?: base.title
                currentTodos.add(TodoItem(base.id, savedTitle, false))
            }
        }

        // === ОБЫЧНЫЕ ПОЛЬЗОВАТЕЛЬСКИЕ (ID > 7 и < 1000) ===
        taskIdsSet.filter { it.toIntOrNull() != null && it.toInt() > 7 && it.toInt() < PROGRESSIVE_ID_START }
            .forEach { idStr ->
                val id = idStr.toInt()
                val savedTitle = sharedPref.getString("task_${id}_title", "") ?: ""
                if (savedTitle.isNotEmpty()) {
                    currentTodos.add(TodoItem(id, savedTitle, false))
                }
            }

        // === СПЕЦИАЛЬНЫЕ ===
        val deltaDays = calculateDeltaDaysSafe(startDate, today)
        currentTodos.add(TodoItem(SPECIAL_RUN_ID, "Пробежка ${SPECIAL_BASE_RUN + deltaDays} минут", false))
        currentTodos.add(TodoItem(SPECIAL_WORKOUT_ID, "Тренировка ${SPECIAL_BASE_WORKOUT + deltaDays} минут", false))

        // === ПРОГРЕССИРУЮЩИЕ (ID >= 1000) ===
        taskIdsSet.mapNotNull { it.toIntOrNull() }.filter { it >= PROGRESSIVE_ID_START }.forEach { id ->
            val baseValue = sharedPref.getInt("prog_${id}_base", 0)
            val increment = sharedPref.getInt("prog_${id}_inc", 1)
            val template = sharedPref.getString("prog_${id}_template", "") ?: ""
            val numberPos = sharedPref.getInt("prog_${id}_pos", 0)

            if (template.isNotEmpty() && baseValue > 0) {
                val currentValue = baseValue + deltaDays * increment
                val originalNumberStr = baseValue.toString()
                val updatedTitle = template.substring(0, numberPos) +
                        currentValue +
                        template.substring(numberPos + originalNumberStr.length)
                currentTodos.add(
                    TodoItem(
                        id = id,
                        title = updatedTitle,
                        isCompleted = false,
                        isProgressive = true,
                        baseValue = baseValue,
                        increment = increment,
                        numberPosition = numberPos
                    )
                )
            }
        }

        currentTodos.sortBy { it.id }

        if (lastResetDate != today) {
            resetCompleted()
            sharedPref.edit().putString("last_reset_date", today).apply()
            Toast.makeText(this, "Новый день! Задачи обновлены!", Toast.LENGTH_LONG).show()
        } else {
            currentTodos.forEach { todo ->
                todo.isCompleted = sharedPref.getBoolean("todo_${todo.id}_completed", false)
            }
        }

        adapter.notifyDataSetChanged()
    }

    private fun calculateDeltaDaysSafe(start: String, end: String): Int {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val diff = sdf.parse(end)!!.time - sdf.parse(start)!!.time
            (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
        } catch (e: Exception) {
            0
        }
    }

    private fun resetCompleted() {
        currentTodos.forEach { todo ->
            todo.isCompleted = false
            sharedPref.edit().putBoolean("todo_${todo.id}_completed", false).apply()
        }
    }

    private fun toggleTodo(todo: TodoItem, position: Int) {
        currentTodos[position].isCompleted = !currentTodos[position].isCompleted
        adapter.notifyItemChanged(position)
        saveTodos()
        val message = if (currentTodos[position].isCompleted) "Задача выполнена!" else "Снято!"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showAddTaskDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val editText = dialogView.findViewById<EditText>(R.id.etTaskTitle)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroupTaskType)
        val rbSimple = dialogView.findViewById<RadioButton>(R.id.rbSimple)
        val rbProgressive = dialogView.findViewById<RadioButton>(R.id.rbProgressive)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Добавить задачу")
            .setView(dialogView)
            .setPositiveButton("Добавить", null)
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val title = editText.text.toString().trim()
            if (title.isEmpty()) return@setOnClickListener

            if (rbSimple.isChecked) {
                val newId = getNextId()
                currentTodos.add(TodoItem(newId, title))
                currentTodos.sortBy { it.id }
                adapter.notifyDataSetChanged()
                saveTodos()
                dialog.dismiss()
            } else {
                val match = "\\d+".toRegex().find(title) ?: run {
                    Toast.makeText(this, "Число обязательно!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val baseValue = match.value.toInt()
                val pos = match.range.first

                showIncrementDialog(title, baseValue, pos) { inc ->
                    val newId = getNextProgressiveId()
                    currentTodos.add(TodoItem(newId, title, isProgressive = true, baseValue = baseValue, increment = inc, numberPosition = pos))
                    currentTodos.sortBy { it.id }
                    adapter.notifyDataSetChanged()
                    saveTodos()
                    dialog.dismiss()
                }
            }
        }
    }

    private fun showIncrementDialog(template: String, baseValue: Int, pos: Int, onConfirm: (Int) -> Unit) {
        val et = EditText(this).apply { setText("1"); inputType = 2 }
        AlertDialog.Builder(this)
            .setTitle("Прогрессия")
            .setMessage("Число: $baseValue\nШаблон: $template\n\nШаг роста:")
            .setView(et)
            .setPositiveButton("OK") { _, _ ->
                val inc = et.text.toString().toIntOrNull() ?: 1
                if (inc > 0) onConfirm(inc) else Toast.makeText(this, "Шаг > 0", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditOrDeleteDialog(todo: TodoItem, position: Int) {
        AlertDialog.Builder(this)
            .setTitle(todo.title)
            .setItems(arrayOf("Редактировать", "Удалить")) { _, which ->
                if (which == 0) showEditTaskDialog(todo, position)
                else confirmAndDelete(todo, position)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditTaskDialog(todo: TodoItem, position: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val editText = dialogView.findViewById<EditText>(R.id.etTaskTitle)
        val rbSimple = dialogView.findViewById<RadioButton>(R.id.rbSimple)
        val rbProgressive = dialogView.findViewById<RadioButton>(R.id.rbProgressive)

        editText.setText(todo.title)
        if (todo.isProgressive) rbProgressive.isChecked = true else rbSimple.isChecked = true

        val dialog = AlertDialog.Builder(this)
            .setTitle("Редактировать")
            .setView(dialogView)
            .setPositiveButton("Сохранить", null)
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val newTitle = editText.text.toString().trim()
            if (newTitle.isEmpty()) return@setOnClickListener

            if (rbSimple.isChecked) {
                currentTodos[position] = TodoItem(todo.id, newTitle, todo.isCompleted)
                clearProgressiveData(todo.id)
                adapter.notifyItemChanged(position)
                saveTodos()
                dialog.dismiss()
            } else {
                val match = "\\d+".toRegex().find(newTitle) ?: run {
                    Toast.makeText(this, "Число обязательно!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val baseValue = match.value.toInt()
                val pos = match.range.first
                val oldInc = todo.increment ?: 1

                showIncrementDialogEdit(newTitle, baseValue, pos, oldInc) { inc ->
                    currentTodos[position] = TodoItem(
                        id = todo.id,
                        title = newTitle,
                        isCompleted = todo.isCompleted,
                        isProgressive = true,
                        baseValue = baseValue,
                        increment = inc,
                        numberPosition = pos
                    )
                    adapter.notifyItemChanged(position)
                    saveTodos()
                    dialog.dismiss()
                }
            }
        }
    }

    private fun showIncrementDialogEdit(template: String, baseValue: Int, pos: Int, oldInc: Int, onConfirm: (Int) -> Unit) {
        val et = EditText(this).apply { setText(oldInc.toString()); inputType = 2 }
        AlertDialog.Builder(this)
            .setTitle("Редактировать шаг")
            .setMessage("Число: $baseValue\nТекущий шаг: $oldInc")
            .setView(et)
            .setPositiveButton("OK") { _, _ ->
                val inc = et.text.toString().toIntOrNull() ?: 1
                if (inc > 0) onConfirm(inc) else Toast.makeText(this, "Шаг > 0", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun confirmAndDelete(todo: TodoItem, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Удалить?")
            .setMessage(todo.title)
            .setPositiveButton("Да") { _, _ ->
                currentTodos.removeAt(position)
                adapter.notifyItemRemoved(position)
                clearTaskData(todo.id, todo.isProgressive)
                saveTodos()
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    private fun clearTaskData(id: Int, isProgressive: Boolean) {
        val editor = sharedPref.edit()
        editor.remove("todo_${id}_completed")
        if (id > 7 && id < PROGRESSIVE_ID_START) editor.remove("task_${id}_title")
        if (isProgressive) {
            editor.remove("prog_${id}_template")
            editor.remove("prog_${id}_base")
            editor.remove("prog_${id}_inc")
            editor.remove("prog_${id}_pos")
        }
        editor.apply()
    }

    private fun clearProgressiveData(id: Int) {
        sharedPref.edit()
            .remove("prog_${id}_template")
            .remove("prog_${id}_base")
            .remove("prog_${id}_inc")
            .remove("prog_${id}_pos")
            .apply()
    }

    private fun getNextId(): Int {
        val ids = sharedPref.getStringSet("task_ids", setOf())?.mapNotNull { it.toIntOrNull() }?.filter { it > 7 && it < PROGRESSIVE_ID_START } ?: emptyList()
        return (ids.maxOrNull() ?: 7) + 1
    }

    private fun getNextProgressiveId(): Int {
        val ids = sharedPref.getStringSet("task_ids", setOf())?.mapNotNull { it.toIntOrNull() }?.filter { it >= PROGRESSIVE_ID_START } ?: emptyList()
        return (ids.maxOrNull() ?: PROGRESSIVE_ID_START - 1) + 1
    }

    private fun saveTodos() {
        val taskIds = mutableSetOf<String>()
        val editor = sharedPref.edit()

        currentTodos.forEach { todo ->
            taskIds.add(todo.id.toString())
            editor.putBoolean("todo_${todo.id}_completed", todo.isCompleted)
            if (todo.id > 7 && todo.id < PROGRESSIVE_ID_START) {
                editor.putString("task_${todo.id}_title", todo.title)
            }
            if (todo.isProgressive && todo.baseValue != null && todo.increment != null && todo.numberPosition != null) {
                editor.putString("prog_${todo.id}_template", todo.title)
                editor.putInt("prog_${todo.id}_base", todo.baseValue)
                editor.putInt("prog_${todo.id}_inc", todo.increment)
                editor.putInt("prog_${todo.id}_pos", todo.numberPosition)
            }
        }
        editor.putStringSet("task_ids", taskIds).apply()
    }
}