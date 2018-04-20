import * as React from "react";

export interface    TextFieldProps {
    label: string;
    required?: boolean;
    pattern?: string;
}

export class TextField extends React.Component<TextFieldProps, any> {

    public render(): React.ReactElement<any> {
        let label: string = this.props.label || "please select a label";
        return (
            <label>
                <span>{label}</span>
                <input pattern={this.props.pattern} required={this.props.required}></input>
            </label>
        );
    }
}
