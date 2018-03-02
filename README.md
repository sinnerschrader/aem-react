# Adobe Experience Manager ("[AEM]") components written in [React]
-----

This project makes it possible to render Adobe Experience Manager ("AEM") components via React.

# STATUS

[![Build Status](https://travis-ci.org/sinnerschrader/aem-react.svg?branch=master)](https://travis-ci.org/sinnerschrader/aem-react)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.sinnerschrader.aem.react/aem-react/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.sinnerschrader.aem.react/aem-react)
[![codecov](https://codecov.io/gh/sinnerschrader/aem-react/branch/next/graph/badge.svg)](https://codecov.io/gh/sinnerschrader/aem-react)

- Version: 0.4
- API is unstable

# Documentation

An comprehensive **[documentation](https://sinnerschrader.github.io/aem-react/)** is being developed. 


# Deploy / install the demo

You can download the demo content package from [here]() and deploy it to your local AEM instance. 
View the [demo](localhost:4502/content/react-demo/index.html)

To install the demo locally:

```bash

git clone https://github.com/sinnerschrader/aem-react.git

cd aem-react

mvn clean install -PautoInstallPackage

```

# Deploy core bundle

```bash
cd aem-react/core
mvn clean install -PautoInstallBundle
``

##Trouble shooting

### Nashorn engine erro: "No such function renderReactComponent"
Check configuration of ReactScriptEngine in webconsole:
- go to /system/console/configMgr
- search for "react" in the browser
- open the configuration of "ReactJs Script Engine Factory"
- important: jcr path must be "/etc/designs/react-demo/js/react-demo/server.js/jcr:content"
- if it is not then update it.


# Background

Nowadays every web application contains at least some highly interactive ui components
that are implemented via javascript in the client. 

React is one of the most popular javascript web frameworks today. React makes client-side
web application development as easy as good old server side web development because of its "render
everthing" ansatz. The developer doesn't need to think about the minimal set of dom manipulations
 to get to the next state of the
ui but instead defines the new state and the framework handles the dom manipulation.

React can be directly used in conjunction with AEM for client-side tasks. Unfortunately
that means that the dom that Reactjs handles will not be rendered on the server and thus 
will not be available on first page load and requires extra work to be indexable by search engines.
This project makes it possible to render AEM components with React on the server.

# License

MIT

# Contribution

Contributions are welcome! 

The repository contains the OSGI bundle in the folder core.
In the folder demo is a demo project that can be used as a starting point 
for your own project.

## OSGI core bundle

The maven module in `./core` contains the implementation of the AEM component type and the
pool of nashorn engines.

## Demo content package

The maven module in `./demo` defines a demo content package that includes some example components and demo pages.

## Javascript library

The [javascript library](//www.github.com/sinnerschrader/aem-react-js) is required
and integrated by the build tools in the demo project.


# Dependencies

- >= Java 8 (Oracle JDK with nashorn engine)
- >= AEM 6.0


[React]: http://facebook.github.io/react/
[AEM]: http://www.adobe.com/de/marketing-cloud/enterprise-content-management.html

