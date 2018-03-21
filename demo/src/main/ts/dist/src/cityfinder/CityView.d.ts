/// <reference types="react" />
import * as React from "react";
import { ResourceComponent, Resource } from "aem-react-js/lib/component/ResourceComponent";
export interface CityDetail extends Resource {
    name: string;
    description: string;
}
export default class CityView extends ResourceComponent<CityDetail, any, any> {
    renderBody(): React.ReactElement<any>;
    renderLoading(): React.ReactElement<any>;
}
