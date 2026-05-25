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
