# zrlog-plugin-reminder

ZrLog 待办提醒插件。用于记录待办事项、标记完成状态，并在任务到期后通过 ZrLog 通知渠道发送提醒。待办数据通过插件协议保存到 ZrLog 主库的 website 配置项。

## 功能

- 新建、编辑、删除待办
- 按待处理、今天、逾期、已完成筛选
- 完成 / 取消完成
- 到期检查任务使用 `reminder.scanDueTasks`，默认每 5 分钟执行一次
- 到期任务通过已配置的通知渠道发送提醒
- 不维护 SMTP 或推送配置，通知通道由插件运行时选择

## 构建

```bash
cd src/main/frontend && yarn type-check
mvn -q -PnodeBuild -DskipTests package
```

前端构建产物会在 `nodeBuild` 流程中生成到 `src/main/resources/templates`，该目录不提交到版本库。打包产物为 `target/reminder.jar`。

Native 构建会先执行 `-PnodeBuild` 生成前端资源，再通过 `exec:exec@java-agent` 注入 GraalVM agent 配置。
