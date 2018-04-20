import * as React from "react";
import {Resource, ResourceComponent, ResourceProps} from "aem-react-js/lib/component/ResourceComponent";
import {ReactParsys}  from "aem-react-js/lib/component/ReactParsys";


export interface AccordionElementProps extends ResourceProps {
    active: boolean;
    key: string;
    groupId: string;
    onChange(): void;
}

export interface AccordionElementResource extends Resource {
    label: string;
}


export default class AccordionElement extends ResourceComponent<AccordionElementResource, AccordionElementProps, any> {

    public renderBody(): React.ReactElement<any> {
        let onChange = ()=> {
            if (!this.isWcmEnabled()) {
                this.props.onChange();
            }
        };

        let label: string = this.getResource().label || "Set a Label";

        let visible: boolean = this.isWcmEnabled() || this.props.active;

        let type: string = this.isWcmEnabled() ? "checkbox" : "radio";
        return (
            <div className="toggle">
                <input ref="toggleRadio" type={type} className="toggle-input-state" disabled={this.isWcmEnabled()} checked={visible} id={this.getPath()}
                       name={this.props.groupId} onChange={onChange}/>
                <label className="toggle-input-toggle toggle-input-js-toggle"
                       htmlFor={this.getPath()}>{label}
                </label>
                <div className="toggle-input-content">
                    <ReactParsys path="togglepar" selectors={[]}/>
                </div>
            </div>

        );

    }


}


