# zrlog-plugin-reminder

ZrLog 待办提醒插件。当前版本提供基础待办 CRUD、完成状态切换、到期提醒检查，并通过 ZrLog 既有的邮件服务插件 `emailService` 发送提醒邮件。待办数据通过插件协议保存到 ZrLog 主库的 website 配置项。

## 功能

- 新建、编辑、删除待办
- 按待处理、今天、逾期、已完成筛选
- 完成 / 取消完成
- 到期任务通过邮件服务中转提醒
- 不维护 SMTP 或推送配置，邮件发送能力由 ZrLog 邮件服务插件提供

## 构建

```bash
cd src/main/frontend && yarn type-check
mvn -q -PnodeBuild -DskipTests package
```

前端构建产物会在 `nodeBuild` 流程中生成到 `src/main/resources/templates`，该目录不提交到版本库。打包产物为 `target/reminder.jar`。
