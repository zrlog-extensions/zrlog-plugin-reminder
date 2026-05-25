package com.zrlog.plugin.reminder.model;

import java.util.ArrayList;
import java.util.List;

public class ReminderStore {

    private List<ReminderTask> tasks = new ArrayList<>();

    public List<ReminderTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<ReminderTask> tasks) {
        this.tasks = tasks;
    }
}
