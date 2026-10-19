package com.example.taskmanager.entity;

/**
 * Lifecycle state of a task.
 * Stored in the database as text (e.g. "IN_PROGRESS"), never as an ordinal number,
 * so that re-ordering the enum can never corrupt existing data.
 */
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    COMPLETED
}
