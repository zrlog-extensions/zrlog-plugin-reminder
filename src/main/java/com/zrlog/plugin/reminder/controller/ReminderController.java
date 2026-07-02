package com.zrlog.plugin.reminder.controller;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.common.SessionNotificationChannelRepository;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.HttpRequestInfo;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.message.NotificationChannelProvider;
import com.zrlog.plugin.message.NotificationChannelQueryResult;
import com.zrlog.plugin.reminder.model.ReminderActionResponse;
import com.zrlog.plugin.reminder.model.ReminderApiResponse;
import com.zrlog.plugin.reminder.model.ReminderCountResponse;
import com.zrlog.plugin.reminder.model.ReminderNotificationChannelInfo;
import com.zrlog.plugin.reminder.model.ReminderNotificationChannels;
import com.zrlog.plugin.reminder.model.ReminderPageData;
import com.zrlog.plugin.reminder.model.ReminderRequestParams;
import com.zrlog.plugin.reminder.model.ReminderTask;
import com.zrlog.plugin.reminder.model.ReminderTaskInput;
import com.zrlog.plugin.reminder.service.ReminderNotificationSettingRepository;
import com.zrlog.plugin.reminder.service.ReminderRepository;
import com.zrlog.plugin.reminder.service.ReminderScheduler;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ReminderController {

    private final IOSession session;
    private final MsgPacket requestPacket;
    private final HttpRequestInfo requestInfo;
    private final ReminderRepository repository = ReminderRepository.getInstance();
    private final ReminderNotificationSettingRepository notificationSettingRepository = ReminderNotificationSettingRepository.getInstance();
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
        response(ReminderApiResponse.success(surfaceData()));
    }

    public void surfaceAction() {
        ReminderRequestParams params = params();
        String actionRef = stringValue(params.getActionRef());
        ReminderTaskInput values = params.effectiveTask();
        String message = "操作完成";
        if ("reminder:create".equals(actionRef)) {
            ReminderTask task = values.toTask("todo");
            task.setTitle(stringValue(task.getTitle()).trim());
            if (!ReminderRepository.notBlank(task.getTitle())) {
                response(ReminderApiResponse.error("标题不能为空"));
                return;
            }
            task.setEmailNotify(ReminderRepository.notBlank(task.getDueAt()) && !values.recordOnly());
            if (!validReminderTime(task)) {
                response(ReminderApiResponse.error(reminderTimeError(task)));
                return;
            }
            repository.save(session, task);
            message = "已新建待办";
        } else if ("reminder:remindNow".equals(actionRef)) {
            int count = ReminderScheduler.remindDueTasks(session);
            message = "已触发 " + count + " 条到期提醒";
        } else if (actionRef.startsWith("reminder:complete:")) {
            ReminderTask task = repository.complete(session, actionRef.substring("reminder:complete:".length()), true);
            if (task == null) {
                response(ReminderApiResponse.error("任务不存在"));
                return;
            }
            message = "已完成";
        } else if (actionRef.startsWith("reminder:reopen:")) {
            ReminderTask task = repository.complete(session, actionRef.substring("reminder:reopen:".length()), false);
            if (task == null) {
                response(ReminderApiResponse.error("任务不存在"));
                return;
            }
            message = "已恢复待办";
        } else if (actionRef.startsWith("reminder:delete:")) {
            repository.delete(session, actionRef.substring("reminder:delete:".length()));
            message = "已删除";
        } else {
            response(ReminderApiResponse.error("不支持的操作"));
            return;
        }
        response(ReminderApiResponse.success(new ReminderActionResponse(message, surfaceData())));
    }

    public void list() {
        response(ReminderApiResponse.success(repository.list(session)));
    }

    public void notificationChannels() {
        try {
            response(ReminderApiResponse.success(notificationChannelInfo()));
        } catch (Exception e) {
            response(ReminderApiResponse.error(e.getMessage()));
        }
    }

    public void saveNotificationChannels() {
        ReminderRequestParams params = params();
        List<NotificationChannelProvider> providers;
        try {
            providers = queryNotificationProviders();
        } catch (Exception e) {
            response(ReminderApiResponse.error(e.getMessage()));
            return;
        }
        Set<String> availableChannels = availableChannels(providers);
        List<String> defaultChannels = configuredChannels(params.getDefaultChannels(), availableChannels);
        if (defaultChannels.isEmpty()) {
            response(ReminderApiResponse.error("请选择 plugin-core 中可用的通知渠道"));
            return;
        }
        List<String> importantChannels = configuredChannels(params.getImportantChannels(), availableChannels);
        if (importantChannels.isEmpty()) {
            importantChannels = defaultChannels;
        }
        List<String> failedChannels = configuredChannels(params.getFailedChannels(), availableChannels);
        if (failedChannels.isEmpty()) {
            failedChannels = defaultChannels;
        }
        ReminderNotificationChannels channels = new ReminderNotificationChannels();
        channels.setDefaultChannels(defaultChannels);
        channels.setImportantChannels(importantChannels);
        channels.setFailedChannels(failedChannels);
        notificationSettingRepository.save(session, channels);
        ReminderNotificationChannelInfo result = new ReminderNotificationChannelInfo();
        result.setSettings(notificationSettingRepository.get(session));
        result.setProviders(providers);
        response(ReminderApiResponse.success(result));
    }

    public void save() {
        ReminderTaskInput input = params().effectiveTask();
        String title = stringValue(input.getTitle());
        if (!ReminderRepository.notBlank(title)) {
            response(ReminderApiResponse.error("标题不能为空"));
            return;
        }
        ReminderTask task = input.toTask("todo");
        task.setTitle(title.trim());
        if (!validReminderTime(task)) {
            response(ReminderApiResponse.error(reminderTimeError(task)));
            return;
        }
        response(ReminderApiResponse.success(repository.save(session, task)));
    }

    public void complete() {
        ReminderTaskInput input = params().effectiveTask();
        ReminderTask task = repository.complete(session, stringValue(input.getId()), input.done());
        if (task == null) {
            response(ReminderApiResponse.error("任务不存在"));
            return;
        }
        response(ReminderApiResponse.success(task));
    }

    public void remove() {
        response(ReminderApiResponse.success(repository.delete(session, stringValue(params().effectiveTask().getId()))));
    }

    public void remindNow() {
        int count = ReminderScheduler.remindDueTasks(session);
        response(ReminderApiResponse.success(new ReminderCountResponse(count)));
    }

    private ReminderApiResponse<ReminderPageData> pageData() {
        ReminderPageData data = new ReminderPageData();
        data.setDark(isDarkMode());
        data.setAdminColorPrimary(getAdminColorPrimary());
        data.setPlugin(session.getPlugin());
        data.setTasks(repository.list(session));
        data.setNotificationChannels(notificationSettingRepository.get(session));
        return ReminderApiResponse.success(data);
    }

    private ReminderNotificationChannelInfo notificationChannelInfo() {
        ReminderNotificationChannelInfo data = new ReminderNotificationChannelInfo();
        data.setSettings(notificationSettingRepository.get(session));
        data.setProviders(queryNotificationProviders());
        return data;
    }

    private List<NotificationChannelProvider> queryNotificationProviders() {
        NotificationChannelQueryResult result = SessionNotificationChannelRepository.of(session).query(Duration.ofSeconds(15));
        if (!result.isOk()) {
            throw new IllegalStateException(stringValue(result.getMessage()));
        }
        return result.getItems();
    }

    private Set<String> availableChannels(List<NotificationChannelProvider> providers) {
        Set<String> channels = new LinkedHashSet<>();
        for (NotificationChannelProvider item : providers) {
            String channel = item == null ? "" : item.getChannel();
            if (ReminderRepository.notBlank(channel)) {
                channels.add(channel);
            }
        }
        return channels;
    }

    private List<String> configuredChannels(Object value, Set<String> availableChannels) {
        List<String> result = new ArrayList<>();
        for (String channel : channelList(value)) {
            if (availableChannels.contains(channel) && !result.contains(channel)) {
                result.add(channel);
            }
        }
        return result;
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
            if (task.isEmailNotify()) {
                long dueAt = ReminderRepository.parseTime(task.getDueAt(), Long.MAX_VALUE);
                if (dueAt < System.currentTimeMillis()) {
                    overdue++;
                }
                if (isToday(task.getDueAt())) {
                    today++;
                }
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
        item.put("description", reminderTimeText(task));
        item.put("status", task.isEmailNotify()
                && ReminderRepository.parseTime(task.getDueAt(), Long.MAX_VALUE) < System.currentTimeMillis() ? "warning" : "normal");
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
        form.add(fieldMap("dueAt", "提醒时间", "datetime", false, ""));
        form.add(fieldMap("repeatType", "重复提醒", "select", false, "", repeatOptions()));
        form.add(fieldMap("priority", "优先级", "select", false, "", priorityOptions()));
        form.add(fieldMap("note", "备注", "textarea", false, ""));
        form.add(fieldMap("recordOnly", "仅记录", "switch", false, ""));
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

    private Map<String, Object> fieldMap(String name,
                                         String label,
                                         String type,
                                         boolean required,
                                         String placeholder,
                                         List<Map<String, String>> options) {
        Map<String, Object> map = fieldMap(name, label, type, required, placeholder);
        map.put("options", options);
        return map;
    }

    private List<Map<String, String>> repeatOptions() {
        List<Map<String, String>> options = new ArrayList<>();
        options.add(optionMap("不重复", "none"));
        options.add(optionMap("每天", "daily"));
        options.add(optionMap("每周", "weekly"));
        options.add(optionMap("每月", "monthly"));
        options.add(optionMap("每年", "yearly"));
        return options;
    }

    private List<Map<String, String>> priorityOptions() {
        List<Map<String, String>> options = new ArrayList<>();
        options.add(optionMap("高", "high"));
        options.add(optionMap("普通", "normal"));
        options.add(optionMap("低", "low"));
        return options;
    }

    private Map<String, String> optionMap(String label, String value) {
        Map<String, String> map = new HashMap<>();
        map.put("label", label);
        map.put("value", value);
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

    private boolean isTomorrow(long time) {
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DATE, 1);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return Objects.equals(dateFormat.format(new Date(time)), dateFormat.format(tomorrow.getTime()));
    }

    private String reminderTimeText(ReminderTask task) {
        if (!task.isEmailNotify()) {
            return "仅记录";
        }
        long time = ReminderRepository.parseTime(task.getDueAt(), -1);
        if (time < 0) {
            return "未设置提醒";
        }
        long now = System.currentTimeMillis();
        if (time < now) {
            return repeatPrefix(task) + "已逾期 " + durationText(now - time);
        }
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        if (isToday(task.getDueAt())) {
            return repeatPrefix(task) + "今天 " + timeFormat.format(new Date(time)) + " 提醒";
        }
        if (isTomorrow(time)) {
            return repeatPrefix(task) + "明天 " + timeFormat.format(new Date(time)) + " 提醒";
        }
        return repeatPrefix(task) + task.getDueAt().substring(0, Math.min(16, task.getDueAt().length())) + " 提醒";
    }

    private String durationText(long milliseconds) {
        long minutes = Math.max(1, milliseconds / 60000);
        if (minutes < 60) {
            return minutes + " 分钟";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + " 小时";
        }
        return (hours / 24) + " 天";
    }

    private boolean validReminderTime(ReminderTask task) {
        boolean recurring = ReminderRepository.isRecurring(task.getRepeatType());
        if (recurring && !task.isEmailNotify()) {
            return false;
        }
        return (!task.isEmailNotify() && !recurring) || ReminderRepository.notBlank(task.getDueAt());
    }

    private String reminderTimeError(ReminderTask task) {
        if (ReminderRepository.isRecurring(task.getRepeatType())) {
            return "重复提醒需要设置提醒时间";
        }
        return "请选择提醒时间，或关闭通知提醒";
    }

    private String repeatPrefix(ReminderTask task) {
        String label = repeatLabel(task.getRepeatType());
        return ReminderRepository.notBlank(label) ? label + " · " : "";
    }

    private String repeatLabel(String repeatType) {
        String normalized = ReminderRepository.normalizeRepeatType(repeatType);
        if (Objects.equals("daily", normalized)) {
            return "每天";
        }
        if (Objects.equals("weekly", normalized)) {
            return "每周";
        }
        if (Objects.equals("monthly", normalized)) {
            return "每月";
        }
        if (Objects.equals("yearly", normalized)) {
            return "每年";
        }
        return "";
    }

    private ReminderRequestParams params() {
        if (requestInfo.getRequestBody() != null && requestInfo.getRequestBody().length > 0) {
            String body = new String(requestInfo.getRequestBody(), StandardCharsets.UTF_8);
            if (body.trim().startsWith("{")) {
                ReminderRequestParams params = gson.fromJson(body, ReminderRequestParams.class);
                return params == null ? new ReminderRequestParams() : params;
            }
        }
        return ReminderRequestParams.fromParams(this::paramObject, gson);
    }

    private Object paramObject(String key) {
        if (requestInfo.getParam() == null || requestInfo.getParam().get(key) == null || requestInfo.getParam().get(key).length == 0) {
            return null;
        }
        String[] values = requestInfo.getParam().get(key);
        return values.length == 1 ? values[0] : values;
    }

    private void response(ReminderApiResponse<?> response) {
        session.sendMsg(ContentType.JSON, response, requestPacket.getMethodStr(), requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
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

    private List<String> channelList(Object value) {
        if (value instanceof String[]) {
            return Arrays.asList((String[]) value);
        }
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List) value) {
                addChannels(result, stringValue(item));
            }
            return result;
        }
        return Arrays.asList(stringValue(value).split(","));
    }

    private void addChannels(List<String> result, String text) {
        if (!ReminderRepository.notBlank(text)) {
            return;
        }
        String[] values = text.split(",");
        for (String value : values) {
            if (ReminderRepository.notBlank(value)) {
                result.add(value.trim());
            }
        }
    }

    private boolean isDarkMode() {
        return requestInfo.isDarkMode();
    }

    private String getAdminColorPrimary() {
        return requestInfo.getAdminColorPrimary();
    }
}
