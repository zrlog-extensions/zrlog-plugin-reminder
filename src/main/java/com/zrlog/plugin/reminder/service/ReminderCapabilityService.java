package com.zrlog.plugin.reminder.service;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.api.IPluginService;
import com.zrlog.plugin.api.ScheduledCapability;
import com.zrlog.plugin.api.Service;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.message.CapabilityInvokeResult;

import java.util.HashMap;
import java.util.Map;

@Service("reminder.scanDueTasks")
@ScheduledCapability(
        key = "reminder.scanDueTasks",
        label = "检查到期待办",
        description = "扫描未完成的到期待办，并通过邮件服务发送提醒。",
        defaultCron = "*/5 * * * *",
        timeoutSeconds = 60
)
public class ReminderCapabilityService implements IPluginService {

    @Override
    public void handle(IOSession session, MsgPacket msgPacket) {
        ReminderScheduler.ReminderScanResult scanResult = ReminderScheduler.scanDueTasks(session);
        CapabilityInvokeResult result = new CapabilityInvokeResult();
        result.setSuccess(scanResult.getFailedCount() == 0);
        Map<String, Object> data = new HashMap<>();
        data.put("dueCount", scanResult.getDueCount());
        data.put("successCount", scanResult.getSuccessCount());
        data.put("failedCount", scanResult.getFailedCount());
        result.setData(data);
        if (!result.isSuccess()) {
            result.setErrorMessage("部分到期提醒发布失败");
        }
        session.sendJsonMsg(result, msgPacket.getMethodStr(), msgPacket.getMsgId(),
                result.isSuccess() ? MsgPacketStatus.RESPONSE_SUCCESS : MsgPacketStatus.RESPONSE_ERROR);
    }
}
