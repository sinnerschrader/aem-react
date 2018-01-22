ResourceComponent is the base class for AEM components. It provides access to the
resource (content). It also adds the necessary wrapper element, so that the component
can be edited in the author mode.


> [[info]] __Recommendation__
> 
>  It is not recommended to use ResourceComponent directly. Rather use a plain react component
> and a transformation as described in [Vanilla component](ref:/Development Guide/Vanilla component)

The main method to implement in a ResourceComponent is `renderBody()`. It is called
by the `render` method once the resource is successfully fetched. The resource is available via `this.getResource()`.

````typescript

import {ResourceComponent} from "aem-react-js/component/ResourceComponent";
import * as React from "react";
import Text from "../text/text";

export default class MyComponent extends ResourceComponent<any, any, any> {
    public renderBody(): React.ReactElement<any> {

        let label: string = this.getResource().label;
        return (
            <div>
                <span>Hello {label}</span>
            </div>
        );
    }
}

 ````

# Container

If the component is a container then it must to render its children by itself. One option is to call `renderChildren` which
turns the component into a parsys like container for any child components. If the child components are restricted to a certain type
  then a custom rendering might be a better solution.
  
  
````typescript

public renderBody(): React.ReactElement<any> {

    let label: string = this.getResource().label;
    let children: React.ReactElement<any>[] =this.renderChildren(this.getResource(), "children");
    return (
        <div>
            <span>Hello {label}</span>
            {children}
        </div>
    );
}
````

# Embed AEM components

To embed another AEm component you use `<ResourceInclude/>` and pass path and resourceType.

````typescript

  return (<div><ResourceInclude resourceType="/components/text" path="test"/></div>)

````

