import {Transform} from "aem-react-js/lib/component/WrapperFactory";
import {JavaApi} from "aem-react-js/lib/component/JavaApi";
import {TextProps} from "./Text";

export const requestModelTransform: Transform<TextProps> = (api: JavaApi) =>
    api.getRequestModel("com.sinnerschrader.aem.react.integration.TextProps", {}).getObject();


