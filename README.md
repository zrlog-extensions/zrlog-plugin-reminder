# zrlog-plugin-reminder

ZrLog 待办提醒插件。当前版本提供基础待办 CRUD、完成状态切换、到期提醒检查，并通过 ZrLog v4 插件运行时发布标准通知。待办数据通过插件协议保存到 ZrLog 主库的 website 配置项。

## 功能

- 新建、编辑、删除待办
- 按待处理、今天、逾期、已完成筛选
- 完成 / 取消完成
- 到期任务声明为 `reminder.scanDueTasks` 调度能力，并默认每 5 分钟注册到调度中心
- 到期任务通过标准通知发布到 `email` 通道
- 不维护 SMTP 或推送配置，通知通道由 plugin-core 选择具体 provider

## 构建

```bash
cd src/main/frontend && yarn type-check
mvn -q -PnodeBuild -DskipTests package
```

前端构建产物会在 `nodeBuild` 流程中生成到 `src/main/resources/templates`，该目录不提交到版本库。打包产物为 `target/reminder.jar`。

Native 构建会先执行 `-PnodeBuild` 生成前端资源，再通过 `exec:exec@java-agent` 注入 GraalVM agent 配置。
