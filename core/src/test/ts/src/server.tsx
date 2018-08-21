import {ReactContext, ServerRenderer} from "aem-react-js/lib/ServerRenderer";
import {RootComponentRegistry} from "aem-react-js/lib/RootComponentRegistry";
import componentRegistry from "./componentRegistry";
import {Container} from "aem-react-js/lib/di/Container";
import {Cache} from "aem-react-js/lib/store/Cache";
import {ServerSling} from "aem-react-js/lib/store/ServerSling";

//declare var Cqx: any;
declare var AemGlobal: any;
console.log("initializing server.tsx");

let rootComponentRegistry: RootComponentRegistry = new RootComponentRegistry();
rootComponentRegistry.add(componentRegistry);
rootComponentRegistry.init();
AemGlobal.registry = rootComponentRegistry;

/*tslint:disable-next-line*/
declare var Java: any;

export function renderReactComponent(path: string, resourceType: string, wcmmode: string, renderRootDialog: boolean,
                                           reactContext: ReactContext, javaSelectors: object, cqx: any): any {
    const selectors: string[] = Java.from(javaSelectors);

    const cache: Cache = new Cache();
    const javaSling = cqx.sling;
    const serverSling = new ServerSling(cache, javaSling);
    const container: Container = new Container(cache, serverSling, cqx);
    container.setService("javaSling", javaSling);
    container.setService("sling", serverSling);
    container.setService("cache", cache);

    let serverRenderer: ServerRenderer = new ServerRenderer(rootComponentRegistry, container);
    return serverRenderer.renderReactComponent(path, resourceType, wcmmode, renderRootDialog, undefined, selectors);
};
