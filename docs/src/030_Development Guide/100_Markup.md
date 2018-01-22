In AEM a lot of the content is long text fragments. These must be passed to React 
as props to be able to render it. Since the props are passed to the browser separate from
the markup the text will be in the html page twice. This will increase the markup size.

To prevent this the `<Text>`-component makes sure that the `value` passed to it will be stored
in the props as an id referring to the element wrapping the text in the page when rendered on the server.

````
render() {
  return (<Text value={this.props.longText}/>);
}
````

More about the `<Text>` component in [XSS](ref:/Development guide/XSS)