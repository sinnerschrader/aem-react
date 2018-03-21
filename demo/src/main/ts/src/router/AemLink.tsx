import * as React from "react";
import {Link, NavLink, NavLinkProps} from 'react-router-dom';
import {Route, RouteComponentProps} from 'react-router';
import {AemComponent} from 'aem-react-js/lib/component/AemComponent';

export class AemLink extends AemComponent<NavLinkProps> {

    public render(): React.ReactElement<Link> {
        return (
            <Route exact {...this.props} children={(options: RouteComponentProps<any>) => {
                if (options.match.isExact || this.isWcmEnabled()) {
                    return <NavLink onClick={(e: any) => {
                        e.preventDefault();
                    }} {...this.props}/>
                } else {
                    return <NavLink {...this.props}/>
                }
            }}/>
        )
    }

}