import * as React from "react";
import {ComponentManager} from "aem-react-js/lib/ComponentManager";
import {Container} from "aem-react-js/lib/di/Container";
import {RootComponentRegistry} from "aem-react-js/lib/RootComponentRegistry";
import {ClientSling} from "aem-react-js/lib/store/ClientSling";
import {Cache} from "aem-react-js/lib/store/Cache";
import componentRegistry from "./componentRegistry";
import {BrowserRouter} from "react-router-dom";
import {ResourceMappingImpl} from "./router/ResourceMappingImpl";
import {ReactNode} from 'react';

let rootComponentRegistry: RootComponentRegistry = new RootComponentRegistry();
rootComponentRegistry.add(componentRegistry);
rootComponentRegistry.init();

let cache: Cache = new Cache();
let host: string = location.protocol + "//" + location.host;
let clientSling: ClientSling = new ClientSling(cache, host);
let container: Container = new Container(cache, clientSling);
const routerFn: React.SFC<{children: ReactNode}> = (props:{children: ReactNode})=> {
    return <BrowserRouter>{props.children}</BrowserRouter>
}
container.setService("router", routerFn);
container.setService("resourceMapping", new ResourceMappingImpl(".html"));
let componentManager: ComponentManager = new ComponentManager(rootComponentRegistry, container);
window.AemGlobal = {componentManager: componentManager};


componentManager.initReactComponents();

interface MyWindow {
    AemGlobal: any;
}
declare var window: MyWindow;

if (typeof window === "undefined") {
    throw "this is not the browser";
}

