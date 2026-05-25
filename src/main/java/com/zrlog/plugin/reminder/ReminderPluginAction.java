package com.zrlog.plugin.reminder;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.api.IPluginAction;
import com.zrlog.plugin.data.codec.HttpRequestInfo;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.reminder.controller.ReminderController;
import com.zrlog.plugin.reminder.service.ReminderScheduler;

public class ReminderPluginAction implements IPluginAction {

    @Override
    public void start(IOSession ioSession, MsgPacket msgPacket) {
        ReminderScheduler.start(ioSession);
    }

    @Override
    public void stop(IOSession ioSession, MsgPacket msgPacket) {
        ReminderScheduler.stop();
    }

    @Override
    public void install(IOSession ioSession, MsgPacket msgPacket, HttpRequestInfo httpRequestInfo) {
        ReminderScheduler.start(ioSession);
        new ReminderController(ioSession, msgPacket, httpRequestInfo).index();
    }

    @Override
    public void uninstall(IOSession ioSession, MsgPacket msgPacket) {
        ReminderScheduler.stop();
    }
}
