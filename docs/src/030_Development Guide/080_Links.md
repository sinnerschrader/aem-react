Managing links between pages is an important part of AEM. Links are based on resource paths and
should be rewritten by `ResourceResolver`. While this is automatically handled by AEM by rewriting all 
links in a filter, AEM-React is focused on universal rendering, so that rewriting must be handled elsewhere.

Developers are encouraged to use sling models in all cases and these have an generic
mechanism to rewrite links. The rewriting takes place when AEM-React converts the java
object into a json via Jackson. All text properties that match certain include and exclude patterns
are rewritten. This can be disabled by explicitly adding the annotation `@NoResourceMapping`.

# Example

The configuration for this is done via the ReactScriptEngineFactory OSGI component.
It should include all text that seems to be a link to the context are but exclude links to the dam assets:

````
includePattern=^/content
excludePattern=^/content/dam
````

> __Sling Mapping Configuration__ [[info]]
>
> In some setups the sling mapping might not be configured to support `ResourceResolver.map()` 
> and `ResourceResolver.resolve()` but only the former. Please read more about that in [Configuration](ref:/Configuration/OSGI).
>




 
