/**
 * interface for mapping a resourcepath to a url's path and vice versa. Js equivalent to
 * ResourceResolver mapping functionality.
 */
export interface ResourceMapping {
    /**
     *
     * @param requestPath
     * @return the resource path
     */
    resolve(requestPath: string): string;
    /**
     *
     * @param resourcePath
     * @return the url's path
     */
    map(resourcePath: string): string;
}