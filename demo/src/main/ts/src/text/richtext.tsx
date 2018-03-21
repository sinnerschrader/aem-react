import * as React from "react";
import * as resource from "aem-react-js/lib/component/ResourceComponent";

export interface ReactTextResource extends resource.Resource {
    text: string;
}


export default class RichText extends resource.ResourceComponent<ReactTextResource, resource.ResourceProps,  any> {

    public renderBody(): React.ReactElement<any> {
        let text: string = this.getResource().text;
        if (this.isWcmEnabled() && !text) {
            text = "please enter a text";
        }
        return (
            <span dangerouslySetInnerHTML={{__html: text}}></span>
        );
    }
}
