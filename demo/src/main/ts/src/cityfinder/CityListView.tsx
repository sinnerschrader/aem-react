import * as React from "react";
import {AemComponent} from "aem-react-js/lib/component/AemComponent";
import {ServiceProxy} from "aem-react-js/lib/di/ServiceProxy";
//import PageTransition from "react-router-page-transition";
import {AemLink} from '../router/AemLink';
import {ResourceMapping} from '../router/ResourceMapping';

export interface CityListProps {
    indexPagePath: string;
}

export class CityListView extends AemComponent<CityListProps, any> {


    public render(): React.ReactElement<any> {
        const resourceMapping: ResourceMapping | undefined = this.getContainer().getService("resourceMapping");
        if (resourceMapping === undefined) {
            return <span>no resource mapping</span>;
        }

        const baseResourcePath = this.props.indexPagePath;

        let storeList: React.ReactElement<any>[] = this.renderStoreList(baseResourcePath);

        let index: string = resourceMapping.map(baseResourcePath);

        return (
            <div>
                <h3><AemLink to={index}>Cities</AemLink></h3>
                <div style={{display: "flex", flexDirection: "row"}}>
                    <ul style={{flexBasis: "20%"}}>
                        {storeList}
                    </ul>
                    <div style={{flexGrow: 1}} className="detail">
                        {this.props.children}
                    </div>
                </div>
            </div>
        );
    }

    public renderStoreList(baseResourcePath: string): React.ReactElement<any>[] {
        let storeList: React.ReactElement<any>[] = [];

        const resourceMapping: ResourceMapping | undefined = this.getContainer().getService("resourceMapping");
        if (resourceMapping == undefined) {
            return [<span>no resource mapping</span>];
        }

        let service: ServiceProxy = this.getRequestModel("com.sinnerschrader.aem.react.demo.CityFinderModel", {});
        let stores: any[] = service.invoke("findCities", baseResourcePath, "par/city_finder/content");

        stores.forEach(function (model: any, childIdx: number): void {
            let link: string = resourceMapping.map(baseResourcePath + "/" + model.id);
            storeList.push(<li key={model.id}>
                <AemLink activeClassName="active" to={link}>{model.name}</AemLink>
            </li>);
        }, this);
        return storeList;
    }

}


