
//path, resourceType, wcmmode,renderAsJson, reactContext, selectors.toArray(new String[selectors.size()]))
var AemGlobal = {
	renderReactComponent : function(path, resourceType, wcmmode, renderAsJson,
			reactContext, javaSelectors) {
		
		if (javaSelectors instanceof Java.type("java.lang.Object[]")) {
			var selectors = Java.from(javaSelectors);
		}else{
			var selectors=javaSelectors;
		}
		
		var state = {
			"selectors" : selectors,
			"path" : path,
			"resourceType" : resourceType,
			"wcmmode" : wcmmode,
			"renderAsJson" : renderAsJson,
			"selectors" : selectors,
			"cqx": Cqx.doit()
		};
		
				var result = {
			html : "my html",
			state : JSON.stringify(state),
			reactContext : reactContext
		}
		return result;

	}
}