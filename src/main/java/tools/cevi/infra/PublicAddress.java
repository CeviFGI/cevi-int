package tools.cevi.infra;

import jakarta.ws.rs.core.UriInfo;

import java.net.URI;

/**
 * The Public Address of a page with a Short Name, as the share control passes it on and the page
 * shows it without script (UC-009 A4, UC-011 A4). The script replaces the origin with the one the
 * browser actually loaded the page from, so the host taken from the request here only reaches a
 * visitor whose browser runs no script.
 *
 * @param address the complete address, scheme, host and port included
 * @param path    the path with the Short Name as its one parameter, for the share script
 */
public record PublicAddress(String address, String path) {

    // UC-009 BR-003, UC-011 BR-003: origin of the request, path from the Short Name, no other parameter.
    public static PublicAddress of(UriInfo uriInfo, String pagePath, String slug) {
        URI uri = uriInfo.getBaseUriBuilder()
                .replacePath(pagePath)
                .replaceQuery(null)
                .fragment(null)
                .queryParam("slug", slug)
                .build();
        return new PublicAddress(uri.toString(), uri.getRawPath() + "?" + uri.getRawQuery());
    }
}
