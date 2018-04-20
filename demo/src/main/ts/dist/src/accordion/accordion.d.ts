/// <reference types="react" />
import * as React from "react";
import { ResourceComponent, ResourceProps } from "aem-react-js/lib/component/ResourceComponent";
export default class Accordion extends ResourceComponent<any, ResourceProps, any> {
    constructor(props: ResourceProps);
    onChange(childIdx: number): void;
    renderBody(): React.ReactElement<any>;
    protected getDepth(): number;
}
