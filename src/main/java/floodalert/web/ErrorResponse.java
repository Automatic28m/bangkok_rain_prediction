package floodalert.web;

/** JSON body of an error response: {@code {"error": "..."}}. */
public record ErrorResponse(String error) {
}
