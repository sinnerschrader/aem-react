import {ComponentRegistry} from "aem-react-js/lib/ComponentRegistry";
import {Text} from "./vanilla/Text";
import {requestModelTransform} from "./vanilla/RequestModelTransform";

let registry: ComponentRegistry = new ComponentRegistry("react-demo/components");

registry.registerVanilla({shortName: "text", component: Text});
registry.registerVanilla({
    component: Text,
    selector: "requestmodel",
    shortName: "text",
    transform: requestModelTransform,
});

export default registry;
