import * as React from "react";
import * as resource from "aem-react-js/lib/component/ResourceComponent";
import Text from "../text/text";


export interface EmbeddedResource extends resource.Resource {
    description: string;
}

export default class Embedded extends resource.ResourceComponent<EmbeddedResource, resource.ResourceProps, any> {

     public renderBody(): React.ReactElement<any> {
        return (
            <div>
                <span>{this.getResource().description}</span>
                <div>
                    <span>Here comes the embedded text:</span><br/>
                    <Text path="text" selectors={[]}></Text>
                </div>
            </div>
        );
    }
}
