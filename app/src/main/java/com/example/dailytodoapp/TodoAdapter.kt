package com.example.dailytodoapp

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TodoAdapter(
    private val todos: List<TodoItem>,
    private val onToggle: (TodoItem) -> Unit,
    private val onLongClick: (TodoItem) -> Unit
) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    inner class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTask: TextView = itemView.findViewById(R.id.tvTask)
        private val cbDone: CheckBox = itemView.findViewById(R.id.cbDone)

        fun bind(todo: TodoItem) {
            tvTask.text = todo.title

            // Устанавливаем состояние чекбокса
            cbDone.isChecked = todo.isCompleted
            cbDone.visibility = View.VISIBLE
            cbDone.isClickable = false
            cbDone.isFocusable = false

            // === СТИЛИЗАЦИЯ ТЕКСТА ===
            if (todo.isCompleted) {
                // Выполненная: серый + зачёркивание
                tvTask.setTextColor(tvTask.context.getColor(android.R.color.darker_gray))
                tvTask.paintFlags = tvTask.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                // Обычная: тёмный
                tvTask.setTextColor(tvTask.context.getColor(android.R.color.black))
                tvTask.paintFlags = tvTask.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            // Клик по строке = переключение
            itemView.setOnClickListener {
                onToggle(todo)
            }

            // Долгое нажатие = редактировать/удалить
            itemView.setOnLongClickListener {
                onLongClick(todo)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return TodoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(todos[position])
    }

    override fun getItemCount(): Int = todos.size
}