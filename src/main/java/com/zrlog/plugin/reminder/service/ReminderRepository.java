package com.zrlog.plugin.reminder.service;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.SessionKvRepository;
import com.zrlog.plugin.reminder.model.ReminderStore;
import com.zrlog.plugin.reminder.model.ReminderTask;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReminderRepository {

    private static final Logger LOGGER = LoggerUtil.getLogger(ReminderRepository.class);
    private static final ReminderRepository INSTANCE = new ReminderRepository();
    private static final String STORE_KEY = "reminderTasks";
    private final Gson gson = new Gson();

    public static ReminderRepository getInstance() {
        return INSTANCE;
    }

    public synchronized List<ReminderTask> list(IOSession session) {
        List<ReminderTask> tasks = new ArrayList<>(readStore(session).getTasks());
        Collections.sort(tasks, new Comparator<ReminderTask>() {
            @Override
            public int compare(ReminderTask left, ReminderTask right) {
                long leftTime = parseTime(left.getDueAt(), Long.MAX_VALUE);
                long rightTime = parseTime(right.getDueAt(), Long.MAX_VALUE);
                return Long.compare(leftTime, rightTime);
            }
        });
        return tasks;
    }

    public synchronized ReminderTask save(IOSession session, ReminderTask input) {
        ReminderStore store = readStore(session);
        List<ReminderTask> tasks = store.getTasks();
        ReminderTask task = null;
        if (notBlank(input.getId())) {
            for (ReminderTask item : tasks) {
                if (Objects.equals(item.getId(), input.getId())) {
                    task = item;
                    break;
                }
            }
        }
        String now = now();
        if (task == null) {
            task = new ReminderTask();
            task.setId(UUID.randomUUID().toString().replace("-", ""));
            task.setCreatedAt(now);
            tasks.add(task);
        }
        task.setTitle(input.getTitle());
        task.setNote(input.getNote());
        task.setDueAt(normalizeDueAt(input.getDueAt()));
        task.setPriority(notBlank(input.getPriority()) ? input.getPriority() : "normal");
        task.setStatus(notBlank(input.getStatus()) ? input.getStatus() : "todo");
        task.setEmailNotify(input.isEmailNotify());
        task.setUpdatedAt(now);
        if (!Objects.equals(task.getStatus(), "done")) {
            task.setCompletedAt(null);
        }
        writeStore(session, store);
        return task;
    }

    public synchronized ReminderTask complete(IOSession session, String id, boolean done) {
        ReminderStore store = readStore(session);
        ReminderTask task = find(store.getTasks(), id);
        if (task == null) {
            return null;
        }
        String now = now();
        task.setStatus(done ? "done" : "todo");
        task.setCompletedAt(done ? now : null);
        task.setUpdatedAt(now);
        writeStore(session, store);
        return task;
    }

    public synchronized boolean delete(IOSession session, String id) {
        ReminderStore store = readStore(session);
        ReminderTask task = find(store.getTasks(), id);
        if (task == null) {
            return false;
        }
        store.getTasks().remove(task);
        writeStore(session, store);
        return true;
    }

    public synchronized void markReminded(IOSession session, String id) {
        ReminderStore store = readStore(session);
        ReminderTask task = find(store.getTasks(), id);
        if (task != null) {
            task.setRemindedAt(now());
            task.setUpdatedAt(task.getRemindedAt());
            writeStore(session, store);
        }
    }

    public synchronized List<ReminderTask> dueTasks(IOSession session, long now) {
        List<ReminderTask> dueTasks = new ArrayList<>();
        for (ReminderTask task : readStore(session).getTasks()) {
            if (!task.isEmailNotify() || Objects.equals(task.getStatus(), "done") || notBlank(task.getRemindedAt())) {
                continue;
            }
            long dueAt = parseTime(task.getDueAt(), Long.MAX_VALUE);
            if (dueAt <= now) {
                dueTasks.add(task);
            }
        }
        return dueTasks;
    }

    public synchronized ReminderTask get(IOSession session, String id) {
        return find(readStore(session).getTasks(), id);
    }

    public static boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static long parseTime(String value, long defaultValue) {
        if (!notBlank(value)) {
            return defaultValue;
        }
        String normalized = value.trim().replace("T", " ");
        if (normalized.length() == 16) {
            normalized = normalized + ":00";
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(normalized).getTime();
        } catch (ParseException e) {
            return defaultValue;
        }
    }

    public static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    private ReminderTask find(List<ReminderTask> tasks, String id) {
        if (!notBlank(id)) {
            return null;
        }
        for (ReminderTask task : tasks) {
            if (Objects.equals(task.getId(), id)) {
                return task;
            }
        }
        return null;
    }

    private ReminderStore readStore(IOSession session) {
        try {
            String json = SessionKvRepository.of(session).get(STORE_KEY).orElse("");
            if (!notBlank(json)) {
                return new ReminderStore();
            }
            ReminderStore store = gson.fromJson(json, ReminderStore.class);
            if (store == null || store.getTasks() == null) {
                return new ReminderStore();
            }
            return store;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "read reminder tasks from website config error", e);
            return new ReminderStore();
        }
    }

    private void writeStore(IOSession session, ReminderStore store) {
        SessionKvRepository.of(session).put(STORE_KEY, gson.toJson(store));
    }

    private String normalizeDueAt(String value) {
        if (!notBlank(value)) {
            return "";
        }
        String normalized = value.trim().replace("T", " ");
        if (normalized.length() == 16) {
            normalized = normalized + ":00";
        }
        return normalized;
    }
}
