import {
    Button,
    Checkbox,
    Form,
    Input,
    Modal,
    Popconfirm,
    Segmented,
    Select,
    Space,
    Switch,
    Tag,
    message,
} from "antd";
import {SettingOutlined} from "@ant-design/icons";
import axios from "axios";
import {FunctionComponent, useMemo, useState} from "react";
import styled from "styled-components";
import {theme} from "antd";
import {ReminderInfoResponse, ReminderNotificationChannels, ReminderPriority, ReminderTask, StandardResponse} from "../index";

type FilterType = "open" | "today" | "overdue" | "done";

type ReminderFormValues = {
    id?: string;
    title: string;
    note?: string;
    dueAt?: string;
    priority: ReminderPriority;
    emailNotify: boolean;
}

type NotificationChannelFormValues = {
    defaultChannels: string;
    importantChannels: string;
    failedChannels: string;
}

type ReminderIndexProps = {
    data: ReminderInfoResponse;
}

const priorityOptions = [
    {label: "高", value: "high"},
    {label: "普通", value: "normal"},
    {label: "低", value: "low"},
];

const filterOptions = [
    {label: "待处理", value: "open"},
    {label: "今天", value: "today"},
    {label: "已逾期", value: "overdue"},
    {label: "已完成", value: "done"},
];

const request = async <T, >(url: string, params?: Record<string, string>) => {
    const {data} = await axios.post<StandardResponse<T>>(url, new URLSearchParams(params), {
        headers: {"Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"},
    });
    if (!data.success) {
        throw new Error(data.message || "操作失败");
    }
    return data.data;
};

const getList = async () => {
    const {data} = await axios.get<StandardResponse<ReminderTask[]>>("list");
    if (!data.success) {
        throw new Error(data.message || "加载失败");
    }
    return data.data;
}

const defaultNotificationChannels = (): ReminderNotificationChannels => ({
    schema: "plugin.reminder.notification.channels",
    version: 1,
    data: {
        defaultChannels: ["email"],
        importantChannels: ["email"],
        failedChannels: ["email"],
    },
});

const channelText = (values?: string[]) => (values && values.length > 0 ? values : ["email"]).join(",");

const channelLabel = (values?: string[]) => (values && values.length > 0 ? values : ["email"]).join(" / ");

const toInputDate = (value?: string) => {
    if (!value) {
        return "";
    }
    return value.replace(" ", "T").slice(0, 16);
}

const displayDate = (value?: string) => {
    if (!value) {
        return "未设置截止时间";
    }
    return value.slice(0, 16);
}

const isToday = (value?: string) => {
    if (!value) {
        return false;
    }
    const now = new Date();
    const date = new Date(value.replace(" ", "T"));
    return now.getFullYear() === date.getFullYear()
        && now.getMonth() === date.getMonth()
        && now.getDate() === date.getDate();
}

const isOverdue = (task: ReminderTask) => {
    if (!task.dueAt || task.status === "done") {
        return false;
    }
    return new Date(task.dueAt.replace(" ", "T")).getTime() < Date.now();
}

const priorityTag = (priority: ReminderPriority) => {
    if (priority === "high") {
        return <Tag color="red">高</Tag>;
    }
    if (priority === "low") {
        return <Tag>低</Tag>;
    }
    return <Tag color="blue">普通</Tag>;
}

const statusTag = (task: ReminderTask) => {
    if (task.status === "done") {
        return <Tag color="green">已完成</Tag>;
    }
    if (isOverdue(task)) {
        return <Tag color="volcano">已逾期</Tag>;
    }
    if (isToday(task.dueAt)) {
        return <Tag color="gold">今天</Tag>;
    }
    return <Tag>待办</Tag>;
}

const Shell = styled.div`
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
  padding: 20px;
  box-sizing: border-box;
`;

const TopBar = styled.div`
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;

  @media (max-width: 720px) {
    display: block;
    
    .ant-space {
      margin-top: 12px;
    }
  }
`;

const Title = styled.h1`
  margin: 0;
  font-size: 24px;
  line-height: 32px;
  font-weight: 650;
`;

const SubTitle = styled.div<{ $token: any }>`
  margin-top: 6px;
  color: ${props => props.$token.colorTextDescription};
  font-size: 14px;
`;

const SummaryGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;

  @media (max-width: 720px) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
`;

const SummaryCardContainer = styled.div<{ $token: any }>`
  padding: 16px;
  border: 1px solid ${props => props.$token.colorBorderSecondary};
  border-radius: 8px;
  background: ${props => props.$token.colorBgContainer};
`;

const SummaryLabel = styled.div<{ $token: any }>`
  color: ${props => props.$token.colorTextDescription};
  font-size: 13px;
`;

const SummaryValue = styled.div`
  margin-top: 6px;
  font-size: 28px;
  line-height: 32px;
  font-weight: 700;
`;

const FilterRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 8px 0 14px;

  @media (max-width: 720px) {
    display: block;
  }
`;

const TaskListContainer = styled.div<{ $token: any }>`
  border: 1px solid ${props => props.$token.colorBorderSecondary};
  border-radius: 8px;
  background: ${props => props.$token.colorBgContainer};
  overflow: hidden;
`;

const TaskItemContainer = styled.div<{ $token: any }>`
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  padding: 16px;
  border-bottom: 1px solid ${props => props.$token.colorBorderSecondary};

  &:last-child {
    border-bottom: 0;
  }

  @media (max-width: 720px) {
    grid-template-columns: 1fr;
  }
`;

const TaskMain = styled.div`
  min-width: 0;
`;

const TaskHead = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
`;

const TaskTitle = styled.div<{ $done?: boolean; $token: any }>`
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 16px;
  font-weight: 600;
  color: ${props => props.$done ? props.$token.colorTextDisabled : props.$token.colorText};
  text-decoration: ${props => props.$done ? 'line-through' : 'none'};
`;

const TaskMeta = styled.div<{ $token: any }>`
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 8px;
  color: ${props => props.$token.colorTextDescription};
  font-size: 13px;
`;

const TaskNote = styled.div<{ $token: any }>`
  margin-top: 8px;
  color: ${props => props.$token.colorTextDescription};
  white-space: pre-wrap;
`;

const TaskActions = styled(Space)`
  align-items: center;

  @media (max-width: 720px) {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
`;

const EmptyText = styled.div<{ $token: any }>`
  padding: 42px 16px;
  text-align: center;
  color: ${props => props.$token.colorTextDisabled};
`;

const SummaryCard: FunctionComponent<{ label: string; value: number; token: any }> = ({label, value, token}) => (
    <SummaryCardContainer $token={token}>
        <SummaryLabel $token={token}>{label}</SummaryLabel>
        <SummaryValue>{value}</SummaryValue>
    </SummaryCardContainer>
);

const ReminderIndex: FunctionComponent<ReminderIndexProps> = ({data}) => {
    const {token} = theme.useToken();
    const [tasks, setTasks] = useState<ReminderTask[]>(data.tasks || []);
    const [filter, setFilter] = useState<FilterType>("open");
    const [loading, setLoading] = useState(false);
    const [editingTask, setEditingTask] = useState<ReminderTask | null>(null);
    const [modalOpen, setModalOpen] = useState(false);
    const [channelModalOpen, setChannelModalOpen] = useState(false);
    const [notificationChannels, setNotificationChannels] = useState<ReminderNotificationChannels>(
        data.notificationChannels || defaultNotificationChannels()
    );
    const [form] = Form.useForm<ReminderFormValues>();
    const [channelForm] = Form.useForm<NotificationChannelFormValues>();
    const [messageApi, contextHolder] = message.useMessage();

    const openTasks = useMemo(() => tasks.filter(task => task.status !== "done"), [tasks]);
    const todayTasks = useMemo(() => tasks.filter(task => task.status !== "done" && isToday(task.dueAt)), [tasks]);
    const overdueTasks = useMemo(() => tasks.filter(isOverdue), [tasks]);
    const doneTasks = useMemo(() => tasks.filter(task => task.status === "done"), [tasks]);
    const visibleTasks = useMemo(() => tasks.filter(task => {
        if (filter === "done") {
            return task.status === "done";
        }
        if (filter === "today") {
            return task.status !== "done" && isToday(task.dueAt);
        }
        if (filter === "overdue") {
            return isOverdue(task);
        }
        return task.status !== "done";
    }), [filter, tasks]);

    const load = async () => {
        setLoading(true);
        try {
            setTasks(await getList());
        } catch (e) {
            messageApi.error(e instanceof Error ? e.message : "加载失败");
        } finally {
            setLoading(false);
        }
    }

    const openModal = (task?: ReminderTask) => {
        setEditingTask(task || null);
        form.setFieldsValue({
            id: task?.id || "",
            title: task?.title || "",
            note: task?.note || "",
            dueAt: toInputDate(task?.dueAt),
            priority: task?.priority || "normal",
            emailNotify: task?.emailNotify ?? true,
        });
        setModalOpen(true);
    }

    const closeModal = () => {
        setModalOpen(false);
        setEditingTask(null);
        form.resetFields();
    }

    const openChannelModal = () => {
        const channels = notificationChannels || defaultNotificationChannels();
        channelForm.setFieldsValue({
            defaultChannels: channelText(channels.data.defaultChannels),
            importantChannels: channelText(channels.data.importantChannels),
            failedChannels: channelText(channels.data.failedChannels),
        });
        setChannelModalOpen(true);
    }

    const saveChannels = async () => {
        const values = await channelForm.validateFields();
        try {
            const nextChannels = await request<ReminderNotificationChannels>("saveNotificationChannels", {
                defaultChannels: values.defaultChannels || "email",
                importantChannels: values.importantChannels || values.defaultChannels || "email",
                failedChannels: values.failedChannels || values.defaultChannels || "email",
            });
            setNotificationChannels(nextChannels);
            setChannelModalOpen(false);
            messageApi.success("已保存");
        } catch (e) {
            messageApi.error(e instanceof Error ? e.message : "保存失败");
        }
    }

    const save = async () => {
        const values = await form.validateFields();
        try {
            await request<ReminderTask>("save", {
                id: values.id || "",
                title: values.title,
                note: values.note || "",
                dueAt: values.dueAt || "",
                priority: values.priority || "normal",
                status: editingTask?.status || "todo",
                emailNotify: values.emailNotify ? "true" : "false",
            });
            messageApi.success("已保存");
            closeModal();
            await load();
        } catch (e) {
            messageApi.error(e instanceof Error ? e.message : "保存失败");
        }
    }

    const complete = async (task: ReminderTask, done: boolean) => {
        try {
            await request<ReminderTask>("complete", {id: task.id, done: done ? "true" : "false"});
            await load();
        } catch (e) {
            messageApi.error(e instanceof Error ? e.message : "操作失败");
        }
    }

    const remove = async (task: ReminderTask) => {
        try {
            await request<boolean>("remove", {id: task.id});
            messageApi.success("已删除");
            await load();
        } catch (e) {
            messageApi.error(e instanceof Error ? e.message : "删除失败");
        }
    }

    const remindNow = async () => {
        try {
            const result = await request<{ count: number }>("remindNow", {});
            messageApi.success(`已触发 ${result.count || 0} 条到期提醒`);
            await load();
        } catch (e) {
            messageApi.error(e instanceof Error ? e.message : "提醒失败");
        }
    }

    return (
        <Shell>
            {contextHolder}
            <TopBar>
                <div>
                    <Title>待办提醒</Title>
                    <SubTitle $token={token}>记录待办、标记进度，到期后通过 ZrLog 通知渠道发送提醒</SubTitle>
                </div>
                <Space wrap>
                    <Button onClick={load} loading={loading}>刷新</Button>
                    <Button icon={<SettingOutlined/>} onClick={openChannelModal}>通知设置</Button>
                    <Button onClick={remindNow}>立即检查提醒</Button>
                    <Button type="primary" onClick={() => openModal()}>新建待办</Button>
                </Space>
            </TopBar>

            <SummaryGrid>
                <SummaryCard label="待处理" value={openTasks.length} token={token}/>
                <SummaryCard label="今天" value={todayTasks.length} token={token}/>
                <SummaryCard label="已逾期" value={overdueTasks.length} token={token}/>
                <SummaryCard label="已完成" value={doneTasks.length} token={token}/>
            </SummaryGrid>

            <FilterRow>
                <Segmented value={filter} onChange={value => setFilter(value as FilterType)} options={filterOptions}/>
            </FilterRow>

            <TaskListContainer $token={token}>
                {visibleTasks.length === 0 ? (
                    <EmptyText $token={token}>暂无待办</EmptyText>
                ) : visibleTasks.map(task => (
                    <TaskItemContainer $token={token} key={task.id}>
                        <TaskMain>
                            <TaskHead>
                                <Checkbox checked={task.status === "done"} onChange={event => complete(task, event.target.checked)}/>
                                <TaskTitle $done={task.status === "done"} $token={token}>{task.title}</TaskTitle>
                            </TaskHead>
                            <TaskMeta $token={token}>
                                <span>截止：{displayDate(task.dueAt)}</span>
                                <span>通知：{task.emailNotify ? (task.remindedAt ? "已发送" : "待发送") : "关闭"}</span>
                                {priorityTag(task.priority)}
                                {statusTag(task)}
                            </TaskMeta>
                            {task.note && <TaskNote $token={token}>{task.note}</TaskNote>}
                        </TaskMain>
                        <TaskActions>
                            <Button size="small" onClick={() => openModal(task)}>编辑</Button>
                            <Popconfirm title="删除这条待办？" okText="删除" cancelText="取消" onConfirm={() => remove(task)}>
                                <Button size="small" danger>删除</Button>
                            </Popconfirm>
                        </TaskActions>
                    </TaskItemContainer>
                ))}
            </TaskListContainer>

            <Modal
                title={editingTask ? "编辑待办" : "新建待办"}
                open={modalOpen}
                destroyOnClose
                okText="保存"
                cancelText="取消"
                onCancel={closeModal}
                onOk={save}
            >
                <Form form={form} layout="vertical" preserve={false}>
                    <Form.Item name="id" hidden>
                        <Input/>
                    </Form.Item>
                    <Form.Item label="标题" name="title" rules={[{required: true, message: "请输入标题"}]}>
                        <Input maxLength={80} placeholder="例如：整理下周发布计划"/>
                    </Form.Item>
                    <Form.Item label="截止时间" name="dueAt">
                        <Input type="datetime-local"/>
                    </Form.Item>
                    <Form.Item label="优先级" name="priority">
                        <Select options={priorityOptions}/>
                    </Form.Item>
                    <Form.Item label="备注" name="note">
                        <Input.TextArea rows={4} maxLength={500}/>
                    </Form.Item>
                    <Form.Item name="emailNotify" valuePropName="checked">
                        <Switch checkedChildren="通知提醒" unCheckedChildren="仅记录"/>
                    </Form.Item>
                </Form>
            </Modal>

            <Modal
                title="通知设置"
                open={channelModalOpen}
                destroyOnClose
                okText="保存"
                cancelText="取消"
                onCancel={() => setChannelModalOpen(false)}
                onOk={saveChannels}
            >
                <Form form={channelForm} layout="vertical" preserve={false}>
                    <Form.Item label="默认渠道" name="defaultChannels" rules={[{required: true, message: "请输入通知渠道"}]}>
                        <Input placeholder="email"/>
                    </Form.Item>
                    <Form.Item label="重要渠道" name="importantChannels">
                        <Input placeholder={channelLabel(notificationChannels.data.defaultChannels)}/>
                    </Form.Item>
                    <Form.Item label="失败渠道" name="failedChannels">
                        <Input placeholder={channelLabel(notificationChannels.data.defaultChannels)}/>
                    </Form.Item>
                </Form>
            </Modal>
        </Shell>
    );
}

export default ReminderIndex;
