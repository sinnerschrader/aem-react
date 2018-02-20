The main OSGI service is the ReactScriptEngineFactory which has the following
properties:

name | default |description
---|---|--
scripts.paths | |resource paths to the javascript files for the server
pool.total.size | pool size for nashorn engines. Correlates with the number of concurrent requests
scripts.reload | whether changes to javascript file should be observed
subServiceName | subService name for accessing the crx. If left blank then the deprecated admin is used.
root.element.name | div | the root element name
root.element.class.name||the root element class name
mapping.reverse.enable|false|if the incoming sling mapping is not supported, then this option will make sure aem react works as expected
mapping.disable|false|check this option to disable sling mapping in aem react
mapping.mangle.namespaces|true|set according to resource resolver setting if mapping.reverse.enable=true
json.resourcemapping.include.pattern|^/content|pattern for text properties in sling models that must be mapped by resource resolver
json.resourcemapping.exclude.pattern||pattern for text properties in sling models that must _NOT_ be mapped by resource resolver

The __root element__ refers to the element that wraps the root aem react component.

The sling mapping or resource mapping ensures that all paths used inside the react components are mapped paths.
For this to work properly the sling mapping must provides incoming and outgoing mappings. In some setups
the incoming mapping is handled by the dispatcher and deactivated in the publish instances. Then
the option __mapping.reverse.enable__ should be selected. Alternatively the mapping can be deactivated in aem react by setting
__mapping.disable__=true. In the latter case content loaded via ajax will use
the fully expanded resource path for http requests, which is generally not desirable.
