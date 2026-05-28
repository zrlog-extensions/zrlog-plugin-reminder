package com.zrlog.plugin.reminder;

import com.google.gson.Gson;
import com.zrlog.plugin.common.PluginNativeImageUtils;
import com.zrlog.plugin.reminder.controller.ReminderController;
import com.zrlog.plugin.reminder.model.ReminderStore;
import com.zrlog.plugin.reminder.model.ReminderTask;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class GraalvmAgentApplication {

    public static void main(String[] args) throws IOException {
        PluginNativeImageUtils.usedGsonObject();
        PluginNativeImageUtils.gsonNativeAgentByClazz(Arrays.asList(ReminderStore.class,ReminderTask.class));
        String basePath = System.getProperty("user.dir").replace("\\target", "").replace("/target", "");
        File file = new File(basePath + "/src/main/resources");
        PluginNativeImageUtils.doLoopResourceLoad(file.listFiles(), file.getPath() + "/", "/");
        PluginNativeImageUtils.exposeController(Collections.singletonList(ReminderController.class));
        Application.main(args);
    }
}
