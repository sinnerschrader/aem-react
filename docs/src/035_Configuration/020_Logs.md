To log server side rendering in javascript you can simple use `console.*`. As parameters you can only pass a message string.
The name of the logger is `com.sinnerschrader.aem.react.JavascriptEngine`. So an appropriate OSGI log configuration would be:

Level | file | Logger
---|---|---
Information|logs/react.log|com.sinnerschrader.aem.react
