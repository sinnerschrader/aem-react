/// <reference types="react" />
import * as React from "react";
import { ResourceComponent } from "aem-react-js/lib/component/ResourceComponent";
export interface CityParams {
    name?: string;
}
export default class CityFinder extends ResourceComponent<any, any, any> {
    renderBody(): React.ReactElement<any>;
}
