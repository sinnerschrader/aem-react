Each AEM Component is associated with a resource in the content repository. This resource is identified by
`ResourceComponent.getPath()`. When the component is mounted it will load its resource. If it is already available in the cache or can be accessed synchronuously on the server, 
then the `renderBody()` method is called. The result will be wrapped in the AEM wrapper element.

![Component Lifecycle on server and client with cached data](SynchronuousResourceComponentLoad.puml)

If the resource is not available, then the `renderLoading` method is called to display a loading indicator and the resource
is loaded via ajax.

![Component Lifecycle on client without cached data](AsynchronuousResourceComponentLoad.puml)
