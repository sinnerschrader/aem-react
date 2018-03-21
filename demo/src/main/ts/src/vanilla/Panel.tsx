import * as React from "react";

export interface PanelProps {
    label: string;
}

export class Panel extends React.Component<PanelProps, any> {

    public render(): React.ReactElement<any> {
        let label: string = this.props.label || "please select a label";
        return (
            <div><span>{label}</span>{this.props.children}</div>
        );
    }
}
