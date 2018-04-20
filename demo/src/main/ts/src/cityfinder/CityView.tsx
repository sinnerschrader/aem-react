import * as React from "react";
import {ResourceComponent, Resource} from "aem-react-js/lib/component/ResourceComponent";
import {ReactParsys} from "aem-react-js/lib/component/ReactParsys";
import {ResourceInclude} from "aem-react-js/lib/ResourceInclude";

export interface CityDetail extends Resource {
    name: string;
    description: string;

}

export default class CityView extends ResourceComponent<CityDetail, any, any> {

    public renderBody(): React.ReactElement<any> {
        let cityDetail: CityDetail = this.getResource();
        let name: string = this.getRequestModel("com.sinnerschrader.aem.react.demo.CityViewModel").invoke("getName");
        return (
            <div>
                <h1>{name}</h1>
                <p>{cityDetail.description}</p>
                <ResourceInclude path="image" resourceType="wcm/foundation/components/image"/>
                <ReactParsys path="more" selectors={[]}></ReactParsys>
            </div>
        );
    }

    public renderLoading():React.ReactElement<any> {
        return <div className="city-view transition-item">Loading...</div>
    }
}
