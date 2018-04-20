/// <reference types="react" />
import * as React from "react";
import { Link, NavLinkProps } from 'react-router-dom';
import { AemComponent } from 'aem-react-js/lib/component/AemComponent';
export declare class AemLink extends AemComponent<NavLinkProps> {
    render(): React.ReactElement<Link>;
}
