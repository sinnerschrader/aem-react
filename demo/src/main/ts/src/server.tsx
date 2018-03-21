import * as React from "react";
import {ServerRenderer, ReactContext} from "aem-react-js/lib/ServerRenderer";
import {RootComponentRegistry} from "aem-react-js/lib/RootComponentRegistry";
import componentRegistry from "./componentRegistry";
import {Container} from "aem-react-js/lib/di/Container";
import {Cache} from "aem-react-js/lib/store/Cache";
import {ServerSling} from "aem-react-js/lib/store/ServerSling";
import {StaticRouter} from "react-router";
import {ResourceMappingImpl} from "./router/ResourceMappingImpl";
import {ReactNode} from 'react';


declare var Cqx: any;
declare var AemGlobal: any;
declare var Java: any;
console.log("initializing AemGlobal");
// AemGlobal = {};

let rootComponentRegistry: RootComponentRegistry = new RootComponentRegistry();
rootComponentRegistry.add(componentRegistry);
rootComponentRegistry.init();
AemGlobal.registry = rootComponentRegistry;

AemGlobal.renderReactComponent = function (path: string, resourceType: string, wcmmode: string, renderAsJson?: boolean,
                                           reactContext?: ReactContext, javaSelectors?: string[]): any {
    const selectors: string[] = Java.from(javaSelectors);


    let cache: Cache = new Cache();
    let serverSling = new ServerSling(cache, Cqx.sling);
    let container: Container = new Container(cache, serverSling, Cqx);
    let url: string = serverSling.getContainingPagePath();
    const routerFn: React.SFC<{ children: ReactNode }> = (props: { children: ReactNode }) => {
        return <StaticRouter location={url} context={{}}>{props.children}</StaticRouter>
    }
    container.setService("router", routerFn);
    console.log("url " + url);
    container.setService("resourceMapping", new ResourceMappingImpl(".html"));

    if (!reactContext) {
        reactContext = {textPool: container.textPool, rootNo: 0};
    } else {
        reactContext.rootNo++;
        if (reactContext.textPool) {
            container.textPool = reactContext.textPool;
        }
    }


    let serverRenderer: ServerRenderer = new ServerRenderer(rootComponentRegistry, container);
    return serverRenderer.renderReactComponent(path, resourceType, wcmmode, renderAsJson, reactContext, selectors);
};
