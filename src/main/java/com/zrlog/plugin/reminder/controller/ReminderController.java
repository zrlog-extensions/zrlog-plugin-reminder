package com.zrlog.plugin.reminder.controller;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.HttpRequestInfo;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.reminder.model.ReminderTask;
import com.zrlog.plugin.reminder.service.ReminderRepository;
import com.zrlog.plugin.reminder.service.ReminderScheduler;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ReminderController {

    private final IOSession session;
    private final MsgPacket requestPacket;
    private final HttpRequestInfo requestInfo;
    private final ReminderRepository repository = ReminderRepository.getInstance();
    private final Gson gson = new Gson();

    public ReminderController(IOSession session, MsgPacket requestPacket, HttpRequestInfo requestInfo) {
        this.session = session;
        this.requestPacket = requestPacket;
        this.requestInfo = requestInfo;
    }

    public void index() {
        Map<String, Object> data = new HashMap<>();
        data.put("theme", isDarkMode() ? "dark" : "light");
        data.put("data", gson.toJson(pageData()));
        session.responseHtml("/templates/index", data, requestPacket.getMethodStr(), requestPacket.getMsgId());
    }

    public void json() {
        response(pageData());
    }

    public void surface() {
        response(successMap(surfaceData()));
    }

    public void surfaceAction() {
        Map<String, Object> params = params();
        String actionRef = stringValue(params.get("actionRef"));
        Map values = parseValues(params.get("values"));
        String message = "操作完成";
        if ("reminder:create".equals(actionRef)) {
            ReminderTask task = new ReminderTask();
            task.setTitle(stringValue(values.get("title")).trim());
            if (!ReminderRepository.notBlank(task.getTitle())) {
                response(errorMap("标题不能为空"));
                return;
            }
            task.setNote(stringValue(values.get("note")));
            task.setDueAt(stringValue(values.get("dueAt")));
            task.setPriority("normal");
            task.setStatus("todo");
            task.setEmailNotify(booleanValue(values.get("emailNotify")));
            repository.save(session, task);
            message = "已新建待办";
        } else if ("reminder:remindNow".equals(actionRef)) {
            int count = ReminderScheduler.remindDueTasks(session);
            message = "已触发 " + count + " 条到期提醒";
        } else if (actionRef.startsWith("reminder:complete:")) {
            ReminderTask task = repository.complete(session, actionRef.substring("reminder:complete:".length()), true);
            if (task == null) {
                response(errorMap("任务不存在"));
                return;
            }
            message = "已完成";
        } else if (actionRef.startsWith("reminder:reopen:")) {
            ReminderTask task = repository.complete(session, actionRef.substring("reminder:reopen:".length()), false);
            if (task == null) {
                response(errorMap("任务不存在"));
                return;
            }
            message = "已恢复待办";
        } else if (actionRef.startsWith("reminder:delete:")) {
            repository.delete(session, actionRef.substring("reminder:delete:".length()));
            message = "已删除";
        } else {
            response(errorMap("不支持的操作"));
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("message", message);
        data.put("surface", surfaceData());
        response(successMap(data));
    }

    public void list() {
        response(successMap(repository.list(session)));
    }

    public void save() {
        Map<String, Object> params = params();
        String title = stringValue(params.get("title"));
        if (!ReminderRepository.notBlank(title)) {
            response(errorMap("标题不能为空"));
            return;
        }
        ReminderTask task = new ReminderTask();
        task.setId(stringValue(params.get("id")));
        task.setTitle(title.trim());
        task.setNote(stringValue(params.get("note")));
        task.setDueAt(stringValue(params.get("dueAt")));
        task.setPriority(stringValue(params.get("priority")));
        task.setStatus(stringValue(params.get("status")));
        task.setEmailNotify(booleanValue(params.get("emailNotify")));
        response(successMap(repository.save(session, task)));
    }

    public void complete() {
        Map<String, Object> params = params();
        ReminderTask task = repository.complete(session, stringValue(params.get("id")), booleanValue(params.get("done")));
        if (task == null) {
            response(errorMap("任务不存在"));
            return;
        }
        response(successMap(task));
    }

    public void remove() {
        response(successMap(repository.delete(session, stringValue(params().get("id")))));
    }

    public void remindNow() {
        int count = ReminderScheduler.remindDueTasks(session);
        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        response(successMap(data));
    }

    private Map<String, Object> pageData() {
        Map<String, Object> data = new HashMap<>();
        data.put("dark", isDarkMode());
        data.put("plugin", session.getPlugin());
        data.put("tasks", repository.list(session));
        return successMap(data);
    }

    private Map<String, Object> surfaceData() {
        List<ReminderTask> tasks = repository.list(session);
        int open = 0;
        int overdue = 0;
        int today = 0;
        for (ReminderTask task : tasks) {
            if (Objects.equals(task.getStatus(), "done")) {
                continue;
            }
            open++;
            long dueAt = ReminderRepository.parseTime(task.getDueAt(), Long.MAX_VALUE);
            if (dueAt < System.currentTimeMillis()) {
                overdue++;
            }
            if (isToday(task.getDueAt())) {
                today++;
            }
        }
        Map<String, Object> surface = new HashMap<>();
        surface.put("version", "1.0");
        surface.put("title", "待办提醒");
        surface.put("description", overdue > 0 ? overdue + " 条待办已逾期" : open + " 条待办待处理");
        surface.put("status", overdue > 0 ? "warning" : "normal");
        surface.put("view", viewMap("进入管理", "page", "index?mode=page"));
        List<Map<String, Object>> metrics = new ArrayList<>();
        metrics.add(metricMap("待处理", open, "normal"));
        metrics.add(metricMap("今天", today, today > 0 ? "processing" : "normal"));
        metrics.add(metricMap("已逾期", overdue, overdue > 0 ? "warning" : "normal"));
        surface.put("metrics", metrics);

        List<Map<String, Object>> items = new ArrayList<>();
        int count = 0;
        for (ReminderTask task : tasks) {
            if (Objects.equals(task.getStatus(), "done")) {
                continue;
            }
            items.add(surfaceItem(task));
            count++;
            if (count >= 5) {
                break;
            }
        }
        surface.put("items", items);

        List<Map<String, Object>> actions = new ArrayList<>();
        actions.add(createAction());
        actions.add(actionMap("立即检查", "reminder:remindNow", "default"));
        surface.put("actions", actions);
        return surface;
    }

    private Map<String, Object> surfaceItem(ReminderTask task) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", task.getId());
        item.put("title", task.getTitle());
        item.put("description", ReminderRepository.notBlank(task.getDueAt()) ? task.getDueAt().substring(0, Math.min(16, task.getDueAt().length())) : "未设置截止时间");
        item.put("status", ReminderRepository.parseTime(task.getDueAt(), Long.MAX_VALUE) < System.currentTimeMillis() ? "warning" : "normal");
        List<Map<String, Object>> actions = new ArrayList<>();
        actions.add(actionMap("完成", "reminder:complete:" + task.getId(), "primary"));
        actions.add(actionMap("删除", "reminder:delete:" + task.getId(), "danger"));
        item.put("actions", actions);
        return item;
    }

    private Map<String, Object> createAction() {
        Map<String, Object> action = actionMap("新建待办", "reminder:create", "primary");
        List<Map<String, Object>> form = new ArrayList<>();
        form.add(fieldMap("title", "标题", "input", true, "例如：整理下周发布计划"));
        form.add(fieldMap("dueAt", "截止时间", "datetime", false, ""));
        form.add(fieldMap("note", "备注", "textarea", false, ""));
        form.add(fieldMap("emailNotify", "邮件提醒", "switch", false, ""));
        action.put("form", form);
        return action;
    }

    private Map<String, Object> metricMap(String label, int value, String status) {
        Map<String, Object> map = new HashMap<>();
        map.put("label", label);
        map.put("value", value);
        map.put("status", status);
        return map;
    }

    private Map<String, Object> actionMap(String label, String actionRef, String style) {
        Map<String, Object> map = new HashMap<>();
        map.put("label", label);
        map.put("actionRef", actionRef);
        map.put("style", style);
        return map;
    }

    private Map<String, Object> fieldMap(String name, String label, String type, boolean required, String placeholder) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("label", label);
        map.put("type", type);
        map.put("required", required);
        map.put("placeholder", placeholder);
        return map;
    }

    private Map<String, Object> viewMap(String label, String view, String url) {
        Map<String, Object> map = new HashMap<>();
        map.put("label", label);
        map.put("view", view);
        map.put("url", url);
        return map;
    }

    private boolean isToday(String value) {
        long time = ReminderRepository.parseTime(value, -1);
        if (time < 0) {
            return false;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return Objects.equals(dateFormat.format(new Date(time)), dateFormat.format(new Date()));
    }

    private Map<String, Object> params() {
        if (requestInfo.getRequestBody() != null && requestInfo.getRequestBody().length > 0) {
            String body = new String(requestInfo.getRequestBody(), StandardCharsets.UTF_8);
            if (body.trim().startsWith("{")) {
                return gson.fromJson(body, Map.class);
            }
        }
        if (requestInfo.getParam() == null) {
            return new HashMap<>();
        }
        return requestInfo.simpleParam();
    }

    private Map parseValues(Object values) {
        if (values == null) {
            return new HashMap();
        }
        if (values instanceof Map) {
            return (Map) values;
        }
        String text = stringValue(values);
        if (!ReminderRepository.notBlank(text)) {
            return new HashMap();
        }
        return gson.fromJson(text, Map.class);
    }

    private Map<String, Object> successMap(Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        map.put("data", data);
        return map;
    }

    private Map<String, Object> errorMap(String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", false);
        map.put("message", message);
        return map;
    }

    private void response(Map<String, Object> map) {
        session.sendMsg(ContentType.JSON, map, requestPacket.getMethodStr(), requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
    }

    private String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof List && !((List) value).isEmpty()) {
            return String.valueOf(((List) value).get(0));
        }
        return String.valueOf(value);
    }

    private boolean booleanValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String text = stringValue(value);
        return "true".equalsIgnoreCase(text) || "on".equalsIgnoreCase(text) || "1".equals(text);
    }

    private boolean isDarkMode() {
        return requestInfo.getHeader() != null && Objects.equals(requestInfo.getHeader().get("Dark-Mode"), "true");
    }
}
