Different root react components are executed in separate javascript contexts and thus share no state.
In the browser they are executed in the same context and can share state. This also means that React is bootstrapped
once for each root component on the server and only once in the client. 


The React Context is a special javascript object that is shared among all root components on the server. 
By default it contains the TextPool. The Textpool contains all text elements that should be sent to the client only once 
in the markup and not in the accompanying cache as well ([Markup](ref:/Development guide/Markup)). It generates globaly unique 
identifiers.

The React Context can be used by application code as well and the application code must take care of intiailzing the textpool correctly.

````typescript
AemGlobal.renderReactComponent = (
  path: string,
  resourceType: string,
  wcmmode: string,
  renderAsJson: boolean,
  reactContext: ReactContext,
  javaSelectors: {}
) => {

  if (!reactContext) {
    reactContext = {textPool: container.textPool, rootNo: 0};
  } else {
    reactContext.rootNo++;
    if (reactContext.textPool) {
      container.textPool = reactContext.textPool;
    }
  }
}

````

>[[info]] __Beware__
>
> Be careful with references to objects in the react context. Most objects shouldn't live longer than a single render cycle.
> Best approach is to keep plain JSON strings in the context.