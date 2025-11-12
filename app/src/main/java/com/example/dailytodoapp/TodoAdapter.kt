package com.example.dailytodoapp

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TodoAdapter(
    private val todos: List<TodoItem>,
    private val onItemClick: (TodoItem, Int) -> Unit,
    private val onLongClick: (TodoItem, Int) -> Unit
) : RecyclerView.Adapter<TodoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTask: TextView = view.findViewById(R.id.tvTask)
        val cbDone: CheckBox = view.findViewById(R.id.cbDone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_todo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val todo = todos[position]
        holder.tvTask.text = todo.title

        // Зачёркивание
        holder.tvTask.paintFlags = if (todo.isCompleted)
            holder.tvTask.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        else
            holder.tvTask.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

        // Галочка: видимая и чекнутая для выполненных
        holder.cbDone.visibility = if (todo.isCompleted) View.VISIBLE else View.GONE
        if (todo.isCompleted) {
            holder.cbDone.isChecked = true
        }

        // Стили текста
        if (todo.isCompleted) {
            holder.tvTask.setTypeface(null, Typeface.NORMAL)
            holder.tvTask.setTextColor(Color.GRAY)
        } else {
            holder.tvTask.setTypeface(null, Typeface.BOLD)
            holder.tvTask.setTextColor(Color.BLACK)
        }

        // Клик на весь item
        holder.itemView.setOnClickListener { onItemClick(todo, position) }
        // Долгий клик для удаления
        holder.itemView.setOnLongClickListener {
            onLongClick(todo, position)
            true
        }
    }

    override fun getItemCount() = todos.size
}