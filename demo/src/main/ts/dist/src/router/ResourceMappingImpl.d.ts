/**
 * Trivial implementation for resource mapping.
 * It adds and removes an extension to convert from url path to resource path.
 *
 *
 */
export declare class ResourceMappingImpl {
    constructor(extension?: string);
    private extension;
    resolve(path: string): string;
    map(path: string): string;
}
