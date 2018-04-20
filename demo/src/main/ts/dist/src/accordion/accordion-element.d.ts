/// <reference types="react" />
import * as React from "react";
import { Resource, ResourceComponent, ResourceProps } from "aem-react-js/lib/component/ResourceComponent";
export interface AccordionElementProps extends ResourceProps {
    active: boolean;
    key: string;
    groupId: string;
    onChange(): void;
}
export interface AccordionElementResource extends Resource {
    label: string;
}
export default class AccordionElement extends ResourceComponent<AccordionElementResource, AccordionElementProps, any> {
    renderBody(): React.ReactElement<any>;
}
