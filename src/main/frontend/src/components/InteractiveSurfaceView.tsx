import {Button, Empty, Form, Input, Modal, Select, Space, Switch, Tag, message} from "antd";
import axios from "axios";
import {FunctionComponent, useEffect, useMemo, useState} from "react";
import {
    InteractiveSurface,
    StandardResponse,
    SurfaceAction,
    SurfaceActionResponse,
    SurfaceField,
    SurfaceStatus,
} from "../index";

declare global {
    interface Window {
        ZrLogAdminPluginBridge?: {
            message?: (payload: { type: "success" | "info" | "warning" | "error"; content: string; duration?: number }) => void;
            openView?: (payload: { view: string; params?: Record<string, string> }) => void;
            resize?: (payload: { height: number }) => void;
            refresh?: () => void;
        };
    }
}

const statusColor = (status?: SurfaceStatus) => {
    if (status === "warning") {
        return "gold";
    }
    if (status === "error") {
        return "red";
    }
    if (status === "processing") {
        return "blue";
    }
    return "default";
};

const statusText = (status?: SurfaceStatus) => {
    if (status === "warning") {
        return "注意";
    }
    if (status === "error") {
        return "异常";
    }
    if (status === "processing") {
        return "进行中";
    }
    return "正常";
};

const actionButtonType = (action: SurfaceAction) => action.style === "primary" ? "primary" : "default";

const fieldNode = (field: SurfaceField) => {
    if (field.type === "textarea") {
        return <Input.TextArea rows={3} placeholder={field.placeholder}/>;
    }
    if (field.type === "datetime") {
        return <Input type="datetime-local"/>;
    }
    if (field.type === "switch") {
        return <Switch/>;
    }
    if (field.type === "select") {
        return <Select options={field.options || []}/>;
    }
    return <Input placeholder={field.placeholder}/>;
};

const loadSurface = async () => {
    const {data} = await axios.get<StandardResponse<InteractiveSurface>>("surface");
    if (!data.success) {
        throw new Error(data.message || "加载失败");
    }
    return data.data;
};

const postAction = async (actionRef: string, values: Record<string, unknown>) => {
    const body = new URLSearchParams();
    body.set("actionRef", actionRef);
    body.set("values", JSON.stringify(values));
    const {data} = await axios.post<StandardResponse<SurfaceActionResponse>>("surfaceAction", body, {
        headers: {"Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"},
    });
    if (!data.success) {
        throw new Error(data.message || "操作失败");
    }
    return data.data;
};

const InteractiveSurfaceView: FunctionComponent = () => {
    const [surface, setSurface] = useState<InteractiveSurface | null>(null);
    const [loading, setLoading] = useState(true);
    const [submittingRef, setSubmittingRef] = useState<string>();
    const [activeAction, setActiveAction] = useState<SurfaceAction | null>(null);
    const [form] = Form.useForm();
    const [messageApi, contextHolder] = message.useMessage();

    const bridge = useMemo(() => window.parent?.ZrLogAdminPluginBridge || window.ZrLogAdminPluginBridge, []);

    const notify = (type: "success" | "info" | "warning" | "error", content: string) => {
        if (bridge?.message) {
            bridge.message({type, content});
            return;
        }
        messageApi[type](content);
    };

    const refresh = async () => {
        setLoading(true);
        try {
            const nextSurface = await loadSurface();
            setSurface(nextSurface);
            bridge?.resize?.({height: document.documentElement.scrollHeight});
        } catch (e) {
            notify("error", e instanceof Error ? e.message : "加载失败");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        refresh();
    }, []);

    const runAction = async (action: SurfaceAction, values: Record<string, unknown> = {}) => {
        setSubmittingRef(action.actionRef);
        try {
            const result = await postAction(action.actionRef, values);
            setSurface(result.surface);
            notify("success", result.message || "操作完成");
            bridge?.refresh?.();
            bridge?.resize?.({height: document.documentElement.scrollHeight});
        } catch (e) {
            notify("error", e instanceof Error ? e.message : "操作失败");
        } finally {
            setSubmittingRef(undefined);
        }
    };

    const handleAction = (action: SurfaceAction) => {
        if (action.form && action.form.length > 0) {
            setActiveAction(action);
            form.resetFields();
            return;
        }
        runAction(action);
    };

    const submitModal = async () => {
        if (!activeAction) {
            return;
        }
        const values = await form.validateFields();
        await runAction(activeAction, values);
        setActiveAction(null);
    };

    const openPluginView = () => {
        if (!surface?.view) {
            return;
        }
        if (bridge?.openView) {
            bridge.openView({view: surface.view.view});
            return;
        }
        window.location.href = surface.view.url;
    };

    return (
        <div className="surface-shell">
            {contextHolder}
            <div className="surface-head">
                <div className="surface-title-wrap">
                    <div className="surface-title-row">
                        <h1 className="surface-title">{surface?.title || "插件视图"}</h1>
                        {surface?.status && <Tag color={statusColor(surface.status)}>{statusText(surface.status)}</Tag>}
                    </div>
                    {surface?.description && <div className="surface-description">{surface.description}</div>}
                </div>
                {surface?.view && <Button onClick={openPluginView}>{surface.view.label}</Button>}
            </div>

            {loading ? (
                <div className="surface-empty">加载中...</div>
            ) : surface === null ? (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE}/>
            ) : (
                <>
                    <div className="surface-metrics">
                        {(surface.metrics || []).map(metric => (
                            <div className="surface-metric" key={metric.label}>
                                <div className="surface-metric-label">{metric.label}</div>
                                <div className="surface-metric-value">{metric.value}</div>
                            </div>
                        ))}
                    </div>

                    <div className="surface-list">
                        {(surface.items || []).length === 0 ? (
                            <div className="surface-empty">暂无待办</div>
                        ) : (surface.items || []).map(item => (
                            <div className="surface-item" key={item.id}>
                                <div className="surface-item-main">
                                    <div className="surface-item-title">{item.title}</div>
                                    {item.description && <div className="surface-item-description">{item.description}</div>}
                                </div>
                                <Space wrap>
                                    {(item.actions || []).map(action => (
                                        <Button
                                            key={action.actionRef}
                                            size="small"
                                            danger={action.style === "danger"}
                                            type={actionButtonType(action)}
                                            loading={submittingRef === action.actionRef}
                                            onClick={() => handleAction(action)}
                                        >
                                            {action.label}
                                        </Button>
                                    ))}
                                </Space>
                            </div>
                        ))}
                    </div>

                    <Space wrap className="surface-actions">
                        {(surface.actions || []).map(action => (
                            <Button
                                key={action.actionRef}
                                danger={action.style === "danger"}
                                type={actionButtonType(action)}
                                loading={submittingRef === action.actionRef}
                                onClick={() => handleAction(action)}
                            >
                                {action.label}
                            </Button>
                        ))}
                    </Space>
                </>
            )}

            <Modal
                title={activeAction?.label}
                open={activeAction !== null}
                okText="提交"
                cancelText="取消"
                onOk={submitModal}
                onCancel={() => setActiveAction(null)}
            >
                <Form form={form} layout="vertical">
                    {(activeAction?.form || []).map(field => (
                        <Form.Item
                            key={field.name}
                            name={field.name}
                            label={field.label}
                            valuePropName={field.type === "switch" ? "checked" : "value"}
                            rules={field.required ? [{required: true, message: `请输入${field.label}`}] : undefined}
                        >
                            {fieldNode(field)}
                        </Form.Item>
                    ))}
                </Form>
            </Modal>
        </div>
    );
};

export default InteractiveSurfaceView;
