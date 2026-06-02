import {
    Alert,
    Button,
    Checkbox,
    Form,
    Grid,
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
import {
    NotificationProviderRow,
    ReminderInfoResponse,
    ReminderNotificationChannelInfo,
    ReminderNotificationChannels,
    ReminderPriority,
    ReminderRepeatType,
    ReminderTask,
    StandardResponse,
} from "../index";

type FilterType = "open" | "today" | "overdue" | "done";

type ReminderFormValues = {
    id?: string;
    title: string;
    note?: string;
    dueAt?: string;
    repeatType: ReminderRepeatType;
    priority: ReminderPriority;
    emailNotify: boolean;
}

type NotificationChannelFormValues = {
    defaultChannels: string[];
    importantChannels: string[];
    failedChannels: string[];
}

type ReminderIndexProps = {
    data: ReminderInfoResponse;
}

const priorityOptions = [
    {label: "高", value: "high"},
    {label: "普通", value: "normal"},
    {label: "低", value: "low"},
];

const repeatOptions = [
    {label: "不重复", value: "none"},
    {label: "每天", value: "daily"},
    {label: "每周", value: "weekly"},
    {label: "每月", value: "monthly"},
    {label: "每年", value: "yearly"},
];

const filterOptions = [
    {label: "待处理", value: "open"},
    {label: "今天", value: "today"},
    {label: "已逾期", value: "overdue"},
    {label: "已完成", value: "done"},
];

type ReminderTimeOption = {
    label: string;
    value: string;
}

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

const fetchNotificationChannelInfo = async () => {
    const {data} = await axios.get<StandardResponse<ReminderNotificationChannelInfo>>("notificationChannels");
    if (!data.success) {
        throw new Error(data.message || "加载失败");
    }
    return data.data;
}

const defaultNotificationChannels = (): ReminderNotificationChannels => ({
    defaultChannels: ["email"],
    importantChannels: ["email"],
    failedChannels: ["email"],
});

const padNumber = (value: number) => value < 10 ? `0${value}` : String(value);

const toInputDate = (value?: string) => {
    if (!value) {
        return "";
    }
    return value.replace(" ", "T").slice(0, 16);
}

const toDateInputValue = (date: Date) => {
    return `${date.getFullYear()}-${padNumber(date.getMonth() + 1)}-${padNumber(date.getDate())}`
        + `T${padNumber(date.getHours())}:${padNumber(date.getMinutes())}`;
}

const cloneAt = (date: Date, hour: number, minute: number) => {
    const next = new Date(date);
    next.setHours(hour, minute, 0, 0);
    return next;
}

const addDays = (date: Date, days: number) => {
    const next = new Date(date);
    next.setDate(next.getDate() + days);
    return next;
}

const addMonths = (date: Date, months: number) => {
    const next = new Date(date);
    next.setMonth(next.getMonth() + months);
    return next;
}

const addYears = (date: Date, years: number) => {
    const next = new Date(date);
    next.setFullYear(next.getFullYear() + years);
    return next;
}

const nextWeekdayAt = (weekday: number, hour: number, minute: number, forceNextWeek?: boolean) => {
    const now = new Date();
    let days = (weekday - now.getDay() + 7) % 7;
    if (forceNextWeek && days === 0) {
        days = 7;
    }
    let date = cloneAt(addDays(now, days), hour, minute);
    if (date.getTime() <= now.getTime()) {
        date = cloneAt(addDays(date, 7), hour, minute);
    }
    return date;
}

const buildReminderTimeOptions = (): ReminderTimeOption[] => {
    const now = new Date();
    const inOneHour = new Date(now.getTime() + 60 * 60 * 1000);
    let evening = cloneAt(now, 18, 0);
    if (evening.getTime() <= now.getTime()) {
        evening = cloneAt(addDays(now, 1), 18, 0);
    }
    const options = [
        {label: "1 小时后", value: toDateInputValue(inOneHour)},
        {label: evening.getDate() === now.getDate() ? "今天 18:00" : "明天 18:00", value: toDateInputValue(evening)},
        {label: "明天 09:00", value: toDateInputValue(cloneAt(addDays(now, 1), 9, 0))},
        {label: "下周一 09:00", value: toDateInputValue(nextWeekdayAt(1, 9, 0, true))},
        {label: "1 个月后", value: toDateInputValue(cloneAt(addMonths(now, 1), 9, 0))},
        {label: "1 年后", value: toDateInputValue(cloneAt(addYears(now, 1), 9, 0))},
    ];
    const seen = new Set<string>();
    return options.filter(option => {
        if (seen.has(option.value)) {
            return false;
        }
        seen.add(option.value);
        return true;
    });
}

const parseReminderDate = (value?: string) => {
    if (!value) {
        return null;
    }
    const date = new Date(value.replace(" ", "T"));
    return Number.isNaN(date.getTime()) ? null : date;
}

const sameDate = (left: Date, right: Date) => {
    return left.getFullYear() === right.getFullYear()
        && left.getMonth() === right.getMonth()
        && left.getDate() === right.getDate();
}

const weekdayText = (date: Date) => ["周日", "周一", "周二", "周三", "周四", "周五", "周六"][date.getDay()];

const durationText = (milliseconds: number) => {
    const minutes = Math.max(1, Math.floor(milliseconds / 60000));
    if (minutes < 60) {
        return `${minutes} 分钟`;
    }
    const hours = Math.floor(minutes / 60);
    if (hours < 24) {
        return `${hours} 小时`;
    }
    return `${Math.floor(hours / 24)} 天`;
}

const displayReminderTime = (value?: string, showOverdue = true) => {
    const date = parseReminderDate(value);
    if (!date) {
        return "未设置提醒";
    }
    const now = new Date();
    const time = `${padNumber(date.getHours())}:${padNumber(date.getMinutes())}`;
    if (showOverdue && date.getTime() < now.getTime()) {
        return `已逾期 ${durationText(now.getTime() - date.getTime())}`;
    }
    if (sameDate(now, date)) {
        return `今天 ${time} 提醒`;
    }
    if (sameDate(addDays(now, 1), date)) {
        return `明天 ${time} 提醒`;
    }
    if (date.getTime() - now.getTime() < 7 * 24 * 60 * 60 * 1000) {
        return `${weekdayText(date)} ${time} 提醒`;
    }
    return `${date.getFullYear()}-${padNumber(date.getMonth() + 1)}-${padNumber(date.getDate())} ${time} 提醒`;
}

const reminderMetaText = (task: ReminderTask) => {
    const repeat = repeatLabel(task.repeatType);
    if (!task.emailNotify) {
        return "仅记录";
    }
    if (repeat) {
        return `${repeat} · ${displayReminderTime(task.dueAt)}`;
    }
    if (task.remindedAt) {
        return `已发送 · ${displayReminderTime(task.dueAt, false).replace(" 提醒", "")}`;
    }
    return displayReminderTime(task.dueAt);
}

const repeatLabel = (repeatType?: string) => {
    const matched = repeatOptions.find(option => option.value === repeatType);
    return matched && matched.value !== "none" ? matched.label : "";
}

const isToday = (value?: string) => {
    const date = parseReminderDate(value);
    if (!date) {
        return false;
    }
    const now = new Date();
    return sameDate(now, date);
}

const isOverdue = (task: ReminderTask) => {
    const date = parseReminderDate(task.dueAt);
    if (!date || task.status === "done" || !task.emailNotify) {
        return false;
    }
    return date.getTime() < Date.now();
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

  @media (max-width: 1024px) {
    padding: 16px;
  }

  @media (max-width: 575px) {
    padding: 12px;
  }
`;

const TopBar = styled.div`
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;

  @media (max-width: 960px) {
    display: block;
    
    .ant-space {
      margin-top: 12px;
    }
  }

  @media (max-width: 575px) {
    .ant-space {
      display: flex;
      width: 100%;
    }

    .ant-btn {
      flex: 1;
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

  @media (max-width: 1024px) and (min-width: 721px) {
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

  @media (max-width: 960px) {
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

  @media (max-width: 900px) {
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

const ReminderTimeBox = styled.div`
  display: grid;
  gap: 10px;
`;

const QuickTimeGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;

  @media (max-width: 520px) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
`;

const QuickTimeButton = styled(Button)`
  width: 100%;
`;

const CustomTimeRow = styled.div`
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;

  @media (max-width: 520px) {
    grid-template-columns: 1fr;
  }
`;

const SelectedTimeText = styled.div<{ $token: any }>`
  color: ${props => props.$token.colorTextDescription};
  font-size: 13px;
`;

const AdvancedToggleRow = styled.div`
  display: flex;
  justify-content: flex-start;
`;

const AdvancedSettingsPanel = styled.div<{ $open: boolean; $token: any }>`
  display: ${props => props.$open ? "grid" : "none"};
  gap: 0;
  margin-top: 4px;
  padding-top: 12px;
  border-top: 1px solid ${props => props.$token.colorBorderSecondary};
`;

const ReminderTimePicker: FunctionComponent<{
    value?: string;
    options: ReminderTimeOption[];
    token: any;
    onChange?: (value: string) => void;
    onSelectTime?: () => void;
    onClearTime?: () => void;
}> = ({value, options, token, onChange, onSelectTime, onClearTime}) => {
    const currentValue = value || "";
    const change = (nextValue: string) => {
        onChange?.(nextValue);
        if (nextValue) {
            onSelectTime?.();
        } else {
            onClearTime?.();
        }
    };
    return (
        <ReminderTimeBox>
            <QuickTimeGrid>
                {options.map(option => (
                    <QuickTimeButton
                        key={option.value}
                        type={currentValue === option.value ? "primary" : "default"}
                        onClick={() => change(option.value)}
                    >
                        {option.label}
                    </QuickTimeButton>
                ))}
            </QuickTimeGrid>
            <CustomTimeRow>
                <Input
                    type="datetime-local"
                    value={currentValue}
                    onChange={event => change(event.target.value)}
                />
                <Button onClick={() => change("")}>清除提醒</Button>
            </CustomTimeRow>
            <SelectedTimeText $token={token}>
                已选：{displayReminderTime(currentValue, false)}
            </SelectedTimeText>
        </ReminderTimeBox>
    );
};

const SummaryCard: FunctionComponent<{ label: string; value: number; token: any }> = ({label, value, token}) => (
    <SummaryCardContainer $token={token}>
        <SummaryLabel $token={token}>{label}</SummaryLabel>
        <SummaryValue>{value}</SummaryValue>
    </SummaryCardContainer>
);

const ReminderIndex: FunctionComponent<ReminderIndexProps> = ({data}) => {
    const {token} = theme.useToken();
    const screens = Grid.useBreakpoint();
    const isPhone = Boolean(screens.xs && !screens.sm);
    const [tasks, setTasks] = useState<ReminderTask[]>(data.tasks || []);
    const [filter, setFilter] = useState<FilterType>("open");
    const [loading, setLoading] = useState(false);
    const [channelLoading, setChannelLoading] = useState(false);
    const [editingTask, setEditingTask] = useState<ReminderTask | null>(null);
    const [modalOpen, setModalOpen] = useState(false);
    const [channelModalOpen, setChannelModalOpen] = useState(false);
    const [advancedOpen, setAdvancedOpen] = useState(false);
    const [notificationChannels, setNotificationChannels] = useState<ReminderNotificationChannels>(
        data.notificationChannels || defaultNotificationChannels()
    );
    const [notificationProviders, setNotificationProviders] = useState<NotificationProviderRow[]>([]);
    const [form] = Form.useForm<ReminderFormValues>();
    const [channelForm] = Form.useForm<NotificationChannelFormValues>();
    const [messageApi, contextHolder] = message.useMessage();
    const reminderTimeOptions = useMemo(() => buildReminderTimeOptions(), [modalOpen]);

    const openTasks = useMemo(() => tasks.filter(task => task.status !== "done"), [tasks]);
    const todayTasks = useMemo(() => tasks.filter(task => task.status !== "done" && task.emailNotify && isToday(task.dueAt)), [tasks]);
    const overdueTasks = useMemo(() => tasks.filter(isOverdue), [tasks]);
    const doneTasks = useMemo(() => tasks.filter(task => task.status === "done"), [tasks]);
    const visibleTasks = useMemo(() => tasks.filter(task => {
        if (filter === "done") {
            return task.status === "done";
        }
        if (filter === "today") {
            return task.status !== "done" && task.emailNotify && isToday(task.dueAt);
        }
        if (filter === "overdue") {
            return isOverdue(task);
        }
        return task.status !== "done";
    }), [filter, tasks]);
    const channelOptions = useMemo(() => {
        const rowsByChannel = new Map<string, NotificationProviderRow[]>();
        notificationProviders.forEach(row => {
            if (!row.channel) {
                return;
            }
            rowsByChannel.set(row.channel, [...(rowsByChannel.get(row.channel) || []), row]);
        });
        return Array.from(rowsByChannel.entries()).sort(([left], [right]) => left.localeCompare(right)).map(([channel, rows]) => {
            const provider = rows.find(row => row.selected) || rows.find(row => row.confirmed) || rows[0];
            const providerName = provider?.providerPluginName || provider?.capabilityLabel || "";
            return {
                label: providerName ? `${channel} (${providerName})` : channel,
                value: channel,
            };
        });
    }, [notificationProviders]);
    const availableChannelValues = useMemo(() => new Set(channelOptions.map(option => option.value)), [channelOptions]);

    const filterAvailableChannels = (channels?: string[]) => (channels || []).filter(channel => availableChannelValues.has(channel));

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
        setAdvancedOpen(Boolean(task?.note)
            || (task?.repeatType || "none") !== "none"
            || task?.priority === "high"
            || task?.priority === "low"
            || (Boolean(task?.dueAt) && !task?.emailNotify));
        form.setFieldsValue({
            id: task?.id || "",
            title: task?.title || "",
            note: task?.note || "",
            dueAt: toInputDate(task?.dueAt),
            repeatType: task?.repeatType || "none",
            priority: task?.priority || "normal",
            emailNotify: task?.emailNotify ?? false,
        });
        setModalOpen(true);
    }

    const closeModal = () => {
        setModalOpen(false);
        setEditingTask(null);
        setAdvancedOpen(false);
        form.resetFields();
    }

    const loadNotificationChannels = async () => {
        setChannelLoading(true);
        try {
            const info = await fetchNotificationChannelInfo();
            setNotificationChannels(info.settings || defaultNotificationChannels());
            setNotificationProviders(info.providers || []);
            const values = new Set((info.providers || []).map(row => row.channel).filter(Boolean));
            const defaultChannels = (info.settings?.defaultChannels || []).filter(channel => values.has(channel));
            const importantChannels = (info.settings?.importantChannels || []).filter(channel => values.has(channel));
            const failedChannels = (info.settings?.failedChannels || []).filter(channel => values.has(channel));
            channelForm.setFieldsValue({
                defaultChannels,
                importantChannels: importantChannels.length > 0 ? importantChannels : defaultChannels,
                failedChannels: failedChannels.length > 0 ? failedChannels : defaultChannels,
            });
        } catch (e) {
            messageApi.error(e instanceof Error ? e.message : "通知渠道加载失败");
        } finally {
            setChannelLoading(false);
        }
    }

    const openChannelModal = () => {
        const channels = notificationChannels || defaultNotificationChannels();
        channelForm.setFieldsValue({
            defaultChannels: filterAvailableChannels(channels.defaultChannels),
            importantChannels: filterAvailableChannels(channels.importantChannels),
            failedChannels: filterAvailableChannels(channels.failedChannels),
        });
        setChannelModalOpen(true);
        loadNotificationChannels();
    }

    const saveChannels = async () => {
        const values = await channelForm.validateFields();
        try {
            const defaultChannels = filterAvailableChannels(values.defaultChannels);
            const importantChannels = filterAvailableChannels(values.importantChannels || values.defaultChannels);
            const failedChannels = filterAvailableChannels(values.failedChannels || values.defaultChannels);
            if (defaultChannels.length === 0) {
                throw new Error("请选择 plugin-core 中可用的通知渠道");
            }
            const nextChannels = await request<ReminderNotificationChannelInfo>("saveNotificationChannels", {
                defaultChannels: defaultChannels.join(","),
                importantChannels: (importantChannels.length > 0 ? importantChannels : defaultChannels).join(","),
                failedChannels: (failedChannels.length > 0 ? failedChannels : defaultChannels).join(","),
            });
            setNotificationChannels(nextChannels.settings || defaultNotificationChannels());
            setNotificationProviders(nextChannels.providers || notificationProviders);
            setChannelModalOpen(false);
            messageApi.success("已保存");
        } catch (e) {
            messageApi.error(e instanceof Error ? e.message : "保存失败");
        }
    }

    const save = async () => {
        const values = await form.validateFields();
        try {
            if (values.emailNotify && !values.dueAt) {
                throw new Error("请选择提醒时间，或关闭通知提醒");
            }
            if (values.repeatType !== "none" && (!values.emailNotify || !values.dueAt)) {
                throw new Error("重复提醒需要设置提醒时间");
            }
            await request<ReminderTask>("save", {
                id: values.id || "",
                title: values.title,
                note: values.note || "",
                dueAt: values.dueAt || "",
                repeatType: values.repeatType || "none",
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
                    <SubTitle $token={token}>记录待办、标记进度，到期后通过通知渠道发送提醒</SubTitle>
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
                                <span>{reminderMetaText(task)}</span>
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
                width={isPhone ? "calc(100vw - 24px)" : 560}
            >
                <Form form={form} layout="vertical" preserve={false}>
                    <Form.Item name="id" hidden>
                        <Input/>
                    </Form.Item>
                    <Form.Item label="标题" name="title" rules={[{required: true, message: "请输入标题"}]}>
                        <Input maxLength={80} placeholder="例如：整理下周发布计划"/>
                    </Form.Item>
                    <Form.Item label="提醒时间" name="dueAt">
                        <ReminderTimePicker
                            options={reminderTimeOptions}
                            token={token}
                            onSelectTime={() => form.setFieldValue("emailNotify", true)}
                            onClearTime={() => {
                                form.setFieldValue("emailNotify", false);
                                form.setFieldValue("repeatType", "none");
                            }}
                        />
                    </Form.Item>
                    <AdvancedToggleRow>
                        <Button
                            type="link"
                            size="small"
                            icon={<SettingOutlined/>}
                            onClick={() => setAdvancedOpen(open => !open)}
                        >
                            {advancedOpen ? "收起设置" : "更多设置"}
                        </Button>
                    </AdvancedToggleRow>
                    <AdvancedSettingsPanel $open={advancedOpen} $token={token}>
                        <Form.Item label="重复提醒" name="repeatType">
                            <Select
                                options={repeatOptions}
                                onChange={value => {
                                    if (value !== "none") {
                                        form.setFieldValue("emailNotify", true);
                                    }
                                }}
                            />
                        </Form.Item>
                        <Form.Item label="优先级" name="priority">
                            <Select options={priorityOptions}/>
                        </Form.Item>
                        <Form.Item label="备注" name="note">
                            <Input.TextArea rows={4} maxLength={500}/>
                        </Form.Item>
                        <Form.Item label="通知" name="emailNotify" valuePropName="checked">
                            <Switch
                                checkedChildren="通知提醒"
                                unCheckedChildren="仅记录"
                                onChange={checked => {
                                    if (!checked) {
                                        form.setFieldValue("repeatType", "none");
                                    }
                                }}
                            />
                        </Form.Item>
                    </AdvancedSettingsPanel>
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
                width={isPhone ? "calc(100vw - 24px)" : 520}
            >
                <Form form={channelForm} layout="vertical" preserve={false}>
                    <Form.Item label="默认渠道" name="defaultChannels" rules={[{required: true, message: "请选择通知渠道"}]}>
                        <Select
                            mode="multiple"
                            loading={channelLoading}
                            options={channelOptions}
                            placeholder="选择通知渠道"
                            notFoundContent={channelLoading ? "加载中" : "暂无可用渠道"}
                        />
                    </Form.Item>
                    <Form.Item label="重要渠道" name="importantChannels">
                        <Select
                            mode="multiple"
                            loading={channelLoading}
                            options={channelOptions}
                            placeholder="默认使用默认渠道"
                            notFoundContent={channelLoading ? "加载中" : "暂无可用渠道"}
                        />
                    </Form.Item>
                    <Form.Item label="失败渠道" name="failedChannels">
                        <Select
                            mode="multiple"
                            loading={channelLoading}
                            options={channelOptions}
                            placeholder="默认使用默认渠道"
                            notFoundContent={channelLoading ? "加载中" : "暂无可用渠道"}
                        />
                    </Form.Item>
                </Form>
                {notificationProviders.length === 0 && !channelLoading && (
                    <Alert
                        type="warning"
                        showIcon
                        message="plugin-core 当前没有可用通知渠道"
                    />
                )}
            </Modal>
        </Shell>
    );
}

export default ReminderIndex;
