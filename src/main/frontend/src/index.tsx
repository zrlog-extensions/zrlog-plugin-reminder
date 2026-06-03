import {legacyLogicalPropertiesTransformer, StyleProvider} from "@ant-design/cssinjs";
import {App, ConfigProvider, Layout, theme} from "antd";
import zhCN from "antd/es/locale/zh_CN";
import {useEffect, useState} from "react";
import {createRoot} from "react-dom/client";
import axios from "axios";
import AppBase from "./AppBase";

const {darkAlgorithm, defaultAlgorithm} = theme;
const {Content} = Layout;

export type ReminderPriority = "high" | "normal" | "low";
export type ReminderStatus = "todo" | "done";
export type ReminderRepeatType = "none" | "daily" | "weekly" | "monthly" | "yearly";

export interface ReminderTask {
    id: string;
    title: string;
    note?: string;
    dueAt?: string;
    repeatType?: ReminderRepeatType;
    priority: ReminderPriority;
    status: ReminderStatus;
    emailNotify: boolean;
    remindedAt?: string;
    createdAt?: string;
    updatedAt?: string;
    completedAt?: string;
}

export interface ReminderNotificationChannels {
    defaultChannels: string[];
    importantChannels: string[];
    failedChannels: string[];
}

export interface NotificationProviderRow {
    channel: string;
    providerPluginId: string;
    providerPluginName?: string;
    providerPluginPreviewImageBase64?: string;
    capabilityKey: string;
    capabilityLabel?: string;
    providerStatus: string;
    selected: boolean;
    confirmed: boolean;
    reviewRequired: boolean;
}

export interface ReminderNotificationChannelInfo {
    settings: ReminderNotificationChannels;
    providers: NotificationProviderRow[];
}

export interface Plugin {
    id: string;
    version: string;
    name: string;
    paths: string[];
    actions: string[];
    desc: string;
    author: string;
    shortName: string;
    indexPage: string;
    previewImageBase64: string;
    services: string[];
    dependentService: string[];
}

export interface ReminderInfoResponse {
    dark: boolean;
    adminColorPrimary?: string;
    plugin: Plugin;
    tasks: ReminderTask[];
    notificationChannels: ReminderNotificationChannels;
}

export interface StandardResponse<T> {
    success: boolean;
    message?: string;
    data: T;
}

const loadFromDocument = () => {
    try {
        const node = document.getElementById("pluginInfo");
        if (node === null || node.innerText.length === 0) {
            return null;
        }
        return JSON.parse(node.innerText) as StandardResponse<ReminderInfoResponse>;
    } catch (e) {
        return null;
    }
}

const Index = () => {
    const [response, setResponse] = useState<StandardResponse<ReminderInfoResponse> | null>(loadFromDocument);

    useEffect(() => {
        if (response === null) {
            axios.get<StandardResponse<ReminderInfoResponse>>("json").then(({data}) => {
                setResponse(data);
            });
        }
    }, [response]);

    if (response === null || !response.success) {
        return <></>;
    }

    return (
        <ConfigProvider
            locale={zhCN}
            theme={{
                algorithm: response.data.dark ? darkAlgorithm : defaultAlgorithm,
                token: response.data.adminColorPrimary ? {
                    colorPrimary: response.data.adminColorPrimary,
                } : undefined,
            }}
        >
            <StyleProvider transformers={[legacyLogicalPropertiesTransformer]}>
                <Content>
                    <App>
                        <AppBase pluginInfo={response.data}/>
                    </App>
                </Content>
            </StyleProvider>
        </ConfigProvider>
    );
};

const container = document.getElementById("app");
const root = createRoot(container!);
root.render(<Index/>);
