import * as React from "react";
import {ResourceComponent, ResourceProps} from "aem-react-js/lib/component/ResourceComponent";
import AccordionElement from "./accordion-element";
import {ResourceInclude} from "aem-react-js/lib/ResourceInclude";
import {ResourceUtils} from "aem-react-js/lib/ResourceUtils";
import {EditMarker} from "aem-react-js/lib/component/EditMarker";

export default class Accordion extends ResourceComponent<any, ResourceProps, any> {

    constructor(props: ResourceProps) {
        super(props);
        this.state = {activeIndex: 0};
    }

    public onChange(childIdx: number): void {
        if (this.state.activeIndex === childIdx) {
            this.setState({activeIndex: -1});
        } else {
            this.setState({activeIndex: childIdx});
        }
    }

    public renderBody(): React.ReactElement<any> {
        let content: any = this.getResource();

        let activeIndex = this.state.activeIndex;

        let toggles: React.ReactElement<any>[] = [];

        let children: any = ResourceUtils.getChildren(content);
        Object.keys(children).forEach((node: string, childIdx: number) => {
            toggles.push(<AccordionElement path={node}
                                           selectors={[]}
                                           groupId={this.props.path}
                                           onChange={()=> {
                                               this.onChange(childIdx);
                                           }}
                                           key={node}
                                           active={this.isWcmEnabled() || activeIndex === childIdx}
            ></AccordionElement>);
        }, this);


        let newZone: React.ReactElement<any> | null = null;
        if (this.isWcmEnabled()) {
            let resourceType = this.getResourceType() + "/new";
            newZone = <ResourceInclude element="div" path="*"
                                       resourceType={resourceType}></ResourceInclude>;
        }
        return (
            <div>
                <EditMarker label="Accordion"/>
                {toggles}
                {newZone}
            </div>
        );
    }

    protected getDepth(): number {
        return 3;
    }

}
