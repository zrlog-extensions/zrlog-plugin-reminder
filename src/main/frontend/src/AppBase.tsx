import {FunctionComponent} from "react";
import {ReminderInfoResponse} from "./index";
import ReminderIndex from "./components/ReminderIndex";

export type AppBaseProps = {
    pluginInfo: ReminderInfoResponse;
}

const AppBase: FunctionComponent<AppBaseProps> = ({pluginInfo}) => {
    return <ReminderIndex data={pluginInfo}/>;
}

export default AppBase;
