To bootstrap the application a couple of tasks need to be performed:

- register components as described in [Registering react component](ref:/Development guide/Registering react component))
- instantiate the sling instance
- render components

# client

The client bootstrap code must be run once after content ready event. All root react components
are discovered and instantiated.

`````typescript
import {ComponentManager} from 'aem-react-js/lib/ComponentManager';
import {Container} from 'aem-react-js/lib/di/Container';
import {Cache} from 'aem-react-js/lib/store/Cache';
import {ClientSling} from 'aem-react-js/lib/store/ClientSling';
import {rootRegistry} from './registries/rootRegistry';


// setup Container
const cache = new Cache();
const host = location.protocol + '//' + location.host;
const container = new Container(cache, new ClientSling(cache, host));

// setup componentManager
const componentManager = new ComponentManager(rootRegistry, container);

// optional: make API available globally
(window as any).AemGlobal = {componentManager};

// instantiate components
componentManager.initReactComponents();

`````

# server

The server boostrap code is run once for each root component. The code to run must be made available as 
`AemGlobal.renderReactComponent()`. Parameters are as follows:

name|required/default|description
---|---|---
path|__required__|the absolute resource path
resourceType| __required__| resourceType
wcmmode|__required__|
renderAsJson|false|If true, then don't replace text element in cache by ids.
reactContext |`undefined`| The first root component must set up the reactContext.
selectors:|`undefined`| the selectors


The method must return the following object:

property|description
---|---
html | the html of the rendered component.
state | the cache as json
reactContext | the reactContext object which will be passed to the next invocation of this method for a root component in the same page.

Example in typescript:

`````typescript


import {
  ReactContext,
  ServerRenderer,
  ServerResponse
} from 'aem-react-js/lib/ServerRenderer';
import {replaceFactory} from 'aem-react-js/lib/component/text/TextUtils';
import {Container} from 'aem-react-js/lib/di/Container';
import {Cache} from 'aem-react-js/lib/store/Cache';
import {ServerSling} from 'aem-react-js/lib/store/ServerSling';
import {rootRegistry} from './registries/rootRegistry';

AemGlobal.renderReactComponent = (
  path: string,
  resourceType: string,
  wcmmode: string,
  renderAsJson: boolean,
  reactContext: ReactContext,
  javaSelectors: {}
) => {
    // parse Java array of selectors into js array
  const selectors = Java.from(javaSelectors);

  // setup cache, sling and container
  const cache = new Cache();
  const sling = new ServerSling(cache, Cqx.sling);
  const container = new Container(cache, sling, Cqx); // Cqx is the server side Java Api object
  container.setService('registry', registry);


  // setup react context
  if (!reactContext) {
    reactContext = {textPool: container.textPool, rootNo: 0};
  } else {
    reactContext.rootNo++;
    if (reactContext.textPool) {
      container.textPool = reactContext.textPool;
    }
  }

  // setup server renderer
  const serverRenderer = new ServerRenderer(rootRegistry, container);

  // render
  const {html} = serverRenderer.renderReactComponent(
    path,
    resourceType,
    wcmmode,
    false,
    reactContext,
    selectors
  );

  // compile result object
  return renderAsJson
    ? {
        html,
        reactContext,
        state: JSON.stringify(cache.getFullState())
      }
    : {
        html,
        reactContext,
        state: JSON.stringify(
          cache.getFullState(),
          replaceFactory(container.textPool)
        )
      };
};
`````