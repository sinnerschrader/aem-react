/// <reference types="react" />
import * as React from "react";
import { AemComponent } from "aem-react-js/lib/component/AemComponent";
export interface CityListProps {
    indexPagePath: string;
}
export declare class CityListView extends AemComponent<CityListProps, any> {
    render(): React.ReactElement<any>;
    renderStoreList(baseResourcePath: string): React.ReactElement<any>[];
}
