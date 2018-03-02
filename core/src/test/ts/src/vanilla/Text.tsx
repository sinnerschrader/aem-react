import * as React from "react";

export interface TextProps {
    content: string;
}

export class Text extends React.Component<TextProps, any> {

    public render(): React.ReactElement<any> {
        let content: string = this.props.content || "no text";
        return (
            <span>{content}</span>
        );
    }
}
