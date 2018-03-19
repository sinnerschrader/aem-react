All data loaded on the server is made available in the client via a cache. This is necessary because client side
rendering needs to be performed synchronuously and must result in the same html as already present form server rendering.

The Cache can hold different kinds of data, each of which is identified by the corresponding resourcePath and 
other parameters.

name | key | description
---|---|---
resources|resourcePath, depth| A certain depth of the resource tree starting at the defined path as a json
wrappers/scripts | resourcePath | The wrapper element as rendered by AEM
components | application defined | This can be any data and application code must specify the key.
serviceCalls | resourcePath, java class name, method parameters | Service calls can be calls to java objects like Sling models or OSGI services.
transforms | resourcePath, selectors| Transforms aggregate one or more serviceCalls and are executes on behalf of one AEM component. They are most important for the vanilla 
react approach.
included | resourcePath, selectors | Including a non-react AEM component means including its rendered html. The html is stored in the cache.


