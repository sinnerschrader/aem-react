import * as React from "react";
import CityView from "./CityView";
import {CityListView} from "./CityListView";
import {ResourceComponent} from "aem-react-js/lib/component/ResourceComponent";
import {ResourceUtils} from "aem-react-js/lib/ResourceUtils";
import {Sling} from "aem-react-js/lib/store/Sling";
import Home from "./Home";
import {ResourceMapping} from '../router/ResourceMapping';
import {Route, RouteComponentProps, Switch} from 'react-router';
import {ReactNode} from 'react';
import * as TransitionGroup from 'react-transition-group/TransitionGroup';
import * as CSSTransition from 'react-transition-group/CSSTransition';


export interface CityParams {
    name?: string;
}

export default class CityFinder extends ResourceComponent<any, any, any> {


    public renderBody(): React.ReactElement<any> {
        const sling: Sling = this.context.aemContext.container.sling;
        const resourceMapping: ResourceMapping | undefined = this.context.aemContext.container.getService("resourceMapping");
        if (resourceMapping == undefined) {
            return (<span>no resourcemapping</span>);
        }

        const Router: React.SFC<{ children: ReactNode }> | undefined = this.context.aemContext.container.getService("router")
        if (Router == undefined) {
            return (<span>no router</span>);
        }


        // we need to get the containing page path, otherwise requesting just the storelocator component view http would fail.
        let resourcePath: string = resourceMapping.resolve(sling.getContainingPagePath());

        let depth = !!this.getResource() ? this.getResource().depth || 1 : 1;
        let resultPath = ResourceUtils.findAncestor(resourcePath, depth);
        resourcePath = resultPath.path + "";

        let indexPath: string = resourceMapping.map(resourcePath);
        let cityPattern: string = resourceMapping.map(resourcePath + "/:name");


        const renderHome = (options: RouteComponentProps<any>) => {
            return <Home baseResourcePath={resourcePath}/>
        }

        const renderCity = (options: RouteComponentProps<CityParams>) => {

            const pagePath = resourceMapping.resolve(options.match.url);

            let resourcePath: string = this.getPath().substring(this.getPath().indexOf("_jcr_content"));
            let path: string = pagePath + "/" + resourcePath + "/" + "content";
            return <CityView path={path}/>;
        };


        return (
            <div>
                <Router>
                    <Route
                        render={({location}) =>
                            (<CityListView indexPagePath={resourcePath}>
                                <TransitionGroup>
                                    <CSSTransition key={location.key} classNames="fade" timeout={1000}>
                                        <Switch location={location}>
                                            <Route exact path={indexPath} render={renderHome}/>
                                            <Route exact path={cityPattern} render={renderCity}/>
                                        </Switch>
                                    </CSSTransition>
                                </TransitionGroup>
                            </CityListView>)}
                    />

                </Router>
            </div>
        );
    }
}
