import ComponentManager from "aem-react-js/ComponentManager";
import {Container} from "aem-react-js/di/Container";
import RootComponentRegistry from "aem-react-js/RootComponentRegistry";
import ClientSling from "aem-react-js/store/ClientSling";
import Cache from "aem-react-js/store/Cache";
import componentRegistry from "./componentRegistry";
import {browserHistory} from "react-router";
import ResourceMappingImpl from "aem-react-js/router/ResourceMappingImpl";
import "function.name";

let rootComponentRegistry: RootComponentRegistry = new RootComponentRegistry();
rootComponentRegistry.add(componentRegistry);
rootComponentRegistry.init();

let container: Container = new Container({});
let cache: Cache = new Cache();
let host: string = location.protocol + "//" + location.host;
let clientSling: ClientSling = new ClientSling(cache, host);
container.register("sling", clientSling);
container.register("cache", cache);
container.register("history", browserHistory);
container.register("resourceMapping", new ResourceMappingImpl(".html"));
let componentManager: ComponentManager = new ComponentManager(rootComponentRegistry, container);

componentManager.initReactComponents();

interface MyWindow {
    AemGlobal: any;
}
declare var window: MyWindow;

if (typeof window === "undefined") {
    throw "this is not the browser";
}

window.AemGlobal = {componentManager: componentManager};
