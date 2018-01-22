Transformation are responsible for converting java object into react props. A transformation
is a function that is passed the `JavaApi` to access sling models and OSGI services.
It returns a single object which is just a plain object and cannot contain logic because
it is passed to the client as a json object.

# Examples:

## Basic sling model example

In this example a sling model is converted to a json object which serves as react props.

let transform: any = (api: JavaApi) => {
    let model = api.getResourceModel("demop.core.models.MyModel").getObject();
    return model;
};
 
 
## Aggregating sling models


 let transform: any = (api: JavaApi) => {
     let model1 = api.getResourceModel("demop.core.models.MyModel1").getObject();
     let model2 = api.getResourceModel("demop.core.models.MyModel2").getObject();
     return {model1, model2};
 };

## Accessing OSGI service


 let transform: any = (api: JavaApi) => {
     let service: ServiceProxy = api.getOsgiService("demop.core.models.Service1");
     let model = service.invoke('load', '1234', true)
     return model;
 };


# Conversion

The java objects are converted to json via [Jackson](www.fasterxml.com). The conversion
can be customized via the documented annoations.

Also The Conversion can map all properties that are resource paths to the external form via
`ResourceResolver.map`. Read more about this in [Links](ref:/Development Guide/Links).