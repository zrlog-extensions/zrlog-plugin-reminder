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
import axios from "axios";
import {FunctionComponent, useMemo, useState} from "react";
import {ReminderInfoResponse, ReminderPriority, ReminderTask, StandardResponse} from "../index";

type FilterType = "open" | "today" | "overdue" | "done";

type ReminderFormValues = {
    id?: string;
    title: string;
    note?: string;
    dueAt?: string;
    priority: ReminderPriority;
    emailNotify: boolean;
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

const SummaryCard: FunctionComponent<{ label: string; value: number }> = ({label, value}) => (
    <div className="summary-card">
        <div className="summary-label">{label}</div>
        <div className="summary-value">{value}</div>
    </div>
);

const ReminderIndex: FunctionComponent<ReminderIndexProps> = ({data}) => {
    const [tasks, setTasks] = useState<ReminderTask[]>(data.tasks || []);
    const [filter, setFilter] = useState<FilterType>("open");
    const [loading, setLoading] = useState(false);
    const [editingTask, setEditingTask] = useState<ReminderTask | null>(null);
    const [modalOpen, setModalOpen] = useState(false);
    const [form] = Form.useForm<ReminderFormValues>();
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
        <div className="reminder-shell">
            {contextHolder}
            <div className="reminder-topbar">
                <div>
                    <h1 className="reminder-title">待办提醒</h1>
                    <div className="reminder-subtitle">记录待办、标记进度，到期后通过 ZrLog 邮件服务发送提醒</div>
                </div>
                <Space wrap>
                    <Button onClick={load} loading={loading}>刷新</Button>
                    <Button onClick={remindNow}>立即检查提醒</Button>
                    <Button type="primary" onClick={() => openModal()}>新建待办</Button>
                </Space>
            </div>

            <div className="summary-grid">
                <SummaryCard label="待处理" value={openTasks.length}/>
                <SummaryCard label="今天" value={todayTasks.length}/>
                <SummaryCard label="已逾期" value={overdueTasks.length}/>
                <SummaryCard label="已完成" value={doneTasks.length}/>
            </div>

            <div className="filter-row">
                <Segmented value={filter} onChange={value => setFilter(value as FilterType)} options={filterOptions}/>
            </div>

            <div className="task-list">
                {visibleTasks.length === 0 ? (
                    <div className="empty-text">暂无待办</div>
                ) : visibleTasks.map(task => (
                    <div className="task-item" key={task.id}>
                        <div className="task-main">
                            <div className="task-head">
                                <Checkbox checked={task.status === "done"} onChange={event => complete(task, event.target.checked)}/>
                                <div className={`task-title ${task.status === "done" ? "done" : ""}`}>{task.title}</div>
                            </div>
                            <div className="task-meta">
                                <span>截止：{displayDate(task.dueAt)}</span>
                                <span>提醒：{task.emailNotify ? (task.remindedAt ? "已发送" : "邮件") : "关闭"}</span>
                                {priorityTag(task.priority)}
                                {statusTag(task)}
                            </div>
                            {task.note && <div className="task-note">{task.note}</div>}
                        </div>
                        <Space className="task-actions">
                            <Button size="small" onClick={() => openModal(task)}>编辑</Button>
                            <Popconfirm title="删除这条待办？" okText="删除" cancelText="取消" onConfirm={() => remove(task)}>
                                <Button size="small" danger>删除</Button>
                            </Popconfirm>
                        </Space>
                    </div>
                ))}
            </div>

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
                        <Switch checkedChildren="邮件提醒" unCheckedChildren="仅记录"/>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
}

export default ReminderIndex;
