A vanilla react component can be registered
as a AEM component as well. It is the recommended way to write AEM components.

````typescript
registry.registerVanilla({component: TextField});
registry.registerVanilla({component: Panel, parsys: {path: "content"}, depth: 2});
````

All resource properties are passed as props to the component.

For a simple component that only needs a single level of the resource tree
 and doesn't display children it is sufficient to define the React component class
 that should be registered. The following additional parameters are available

 parameter | type | description
 ---|---|---
 depth | number | the number of levels of the resource available
 props? | any | extra props that are passed
 parsys? | object | define this property to define a parsys as the only child of this component
 parsys.path | string | the relative content path to store children 
 parsys.className?| string | class name added to the parsys element
 parsys.elementName?| string | name of the parsys element (default is "div")
 parsys.childElementName?| string| If provided each child is wrapped in an extra element
 parsys.childClassName?| string| class name added to children elements.
 transform? | function | a function to tranform the resource into react props
 
 # Container
 
 If the `parsys` property is set then the vanilla component will be turned into
 an AEM container. The `.content.xml` must also set the corresponding attribute:
 
 ````xml
 <?xml version="1.0" encoding="UTF-8"?>
 <jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:cq="http://www.day.com/jcr/cq/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0"
     jcr:primaryType="cq:Component"
     jcr:title="My Title"
     cq:isContainer="true"
     />
 ````
 
# Resource transformation

If the resource's structure does not match the props of the vanilla component then a transformation can be used.
A transformation is a function that is passed the resource and the resourceComponent and returns the props that will be passed to
the react component. The props will be cached as json, so they cannot not have methods.

In this example a sling model is converted to json and returned as the props of the component:
````typescript
let transform: any = (api: JavaApi) => {
    let model: ServiceProxy = api.getResourceModel("demop.core.models.MyModel").getObject();
    return model;
};


registry.registerVanilla({
    component: myComponent, transform: transform
});
````

The transformation in the previous example is very simple. It is recommended because of its simplicity.
More information on transformations can be found in the chapter [Transformation](ref:/Development Guide/Transformation).


# Include vanilla wrapper

When including a vanilla component registered as an AEM component directly in a jsx  you need to use `<VanillaInclude/>`.
 Otherwise it will not be editable on the page.
 
 
````typescript
  <div>
     <VanillaInclude path="test" component={MyVanillaComponent}/>
  </div>   
````
alternatively you can also use the standard include:

````typescript
  <div>
     <ResourceInclude path="test" resourceType="/components/my-vanilla"/>
  </div>   
````


