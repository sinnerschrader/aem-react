/// <reference types="react" />
import * as React from "react";
export interface TextFieldProps {
    label: string;
    required?: boolean;
    pattern?: string;
}
export declare class TextField extends React.Component<TextFieldProps, any> {
    render(): React.ReactElement<any>;
}
