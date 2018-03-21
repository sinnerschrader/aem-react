/// <reference types="react" />
import * as React from "react";
import { Route } from "react-router";
import { RouteProps } from "react-router";
import { ResourceComponent } from 'aem-react-js/lib/component/ResourceComponent';
export interface AemRouteProps extends RouteProps {
    resourceComponent: ResourceComponent<any, any, any>;
}
export declare class AemRoute extends React.Component<AemRouteProps, void> {
    render(): React.ReactElement<Route>;
}
