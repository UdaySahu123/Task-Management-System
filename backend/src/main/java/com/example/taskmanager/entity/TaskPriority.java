package com.example.taskmanager.entity;

/**
 * Priority of a task. Stored as text in the database for the same
 * safety reasons explained in {@link TaskStatus}.
 */
public enum TaskPriority {
    LOW,
    MEDIUM,
    HIGH
}
