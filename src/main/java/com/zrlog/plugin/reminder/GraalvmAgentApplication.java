package com.zrlog.plugin.reminder;

import com.zrlog.plugin.RunConstants;
import com.zrlog.plugin.type.RunType;
import com.zrlog.plugin.common.PluginNativeImageUtils;
import com.zrlog.plugin.reminder.controller.ReminderController;
import com.zrlog.plugin.reminder.model.ReminderStore;
import com.zrlog.plugin.reminder.model.ReminderTask;
import com.zrlog.plugin.reminder.service.ReminderCapabilityService;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class GraalvmAgentApplication {

    public static void main(String[] args) throws IOException {
        RunConstants.runType = RunType.AGENT;
        PluginNativeImageUtils.usedGsonObject();
        PluginNativeImageUtils.gsonNativeAgentByClazz(Arrays.asList(ReminderStore.class, ReminderTask.class));
        warmupServiceReflection();
        String basePath = System.getProperty("user.dir").replace("\\target", "").replace("/target", "");
        File file = new File(basePath + "/src/main/resources");
        PluginNativeImageUtils.doLoopResourceLoad(file.listFiles(), file.getPath() + "/", "/");
        PluginNativeImageUtils.exposeController(Collections.singletonList(ReminderController.class));
        Application.main(args);
    }

    private static void warmupServiceReflection() {
        try {
            ReminderCapabilityService.class.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
