package com.zrlog.plugin.reminder;

import com.zrlog.plugin.client.NioClient;
import com.zrlog.plugin.reminder.controller.ReminderController;
import com.zrlog.plugin.reminder.service.ReminderCapabilityService;
import com.zrlog.plugin.render.SimpleTemplateRender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Application {

    public static void main(String[] args) throws IOException {
        List<Class<?>> classList = new ArrayList<>();
        classList.add(ReminderController.class);
        new NioClient(null, new SimpleTemplateRender())
                .connectServer(args, classList, ReminderPluginAction.class, ReminderCapabilityService.class);
    }
}
