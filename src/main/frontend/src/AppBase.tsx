import {FunctionComponent} from "react";
import {ReminderInfoResponse} from "./index";
import ReminderIndex from "./components/ReminderIndex";
import InteractiveSurfaceView from "./components/InteractiveSurfaceView";

export type AppBaseProps = {
    pluginInfo: ReminderInfoResponse;
}

const AppBase: FunctionComponent<AppBaseProps> = ({pluginInfo}) => {
    const params = new URLSearchParams(window.location.search);
    const mode = params.get("mode") || params.get("view");
    if (mode === "surface" || mode === "card") {
        return <InteractiveSurfaceView/>;
    }
    return <ReminderIndex data={pluginInfo}/>;
}

export default AppBase;
