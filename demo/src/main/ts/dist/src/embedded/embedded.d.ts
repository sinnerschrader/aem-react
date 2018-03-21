/// <reference types="react" />
import * as React from "react";
import * as resource from "aem-react-js/lib/component/ResourceComponent";
export interface EmbeddedResource extends resource.Resource {
    description: string;
}
export default class Embedded extends resource.ResourceComponent<EmbeddedResource, resource.ResourceProps, any> {
    renderBody(): React.ReactElement<any>;
}
