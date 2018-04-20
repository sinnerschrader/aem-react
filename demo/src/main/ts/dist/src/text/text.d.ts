/// <reference types="react" />
import * as React from "react";
import * as resource from "aem-react-js/lib/component/ResourceComponent";
export interface ReactTextResource extends resource.Resource {
    propText: string;
}
export default class Text extends resource.ResourceComponent<ReactTextResource, resource.ResourceProps, any> {
    renderBody(): React.ReactElement<any>;
}
