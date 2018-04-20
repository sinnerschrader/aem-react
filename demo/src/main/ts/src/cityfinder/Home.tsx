import * as React from "react";
import {AemComponent} from "aem-react-js/lib/component/AemComponent";
import {ReactParsys} from "aem-react-js/lib/component/ReactParsys";
import {ServiceProxy} from "aem-react-js/lib/di/ServiceProxy";


export default class Home extends AemComponent<any, any> {

    public renderCityList(): React.ReactElement<any>[] {
        let storeList: React.ReactElement<any>[] = [];

        const baseResourcePath = this.props.baseResourcePath;
        let service: ServiceProxy = this.getRequestModel("com.sinnerschrader.aem.react.demo.CityFinderModel", {});
        let cities: any[] = service.invoke("findCities", baseResourcePath, "par/city_finder/content");

        cities.forEach(function (model: any, childIdx: number): void {
            storeList.push(<li key={model.id}>
                <img src={model.imageSrc} width="100"/>
            </li>);
        }, this);
        return storeList;
    }

    public render(): React.ReactElement<any> {

        let cities: React.ReactElement<any>[] = this.renderCityList();
        return (
            <div className="city-view transition-item">
                <h1>Find a nice town</h1>
                {cities}
                <ReactParsys path={this.props.baseResourcePath+"/_jcr_content/par/city_finder/more"} selectors={[]}></ReactParsys>
            </div>
        );
    }
}
