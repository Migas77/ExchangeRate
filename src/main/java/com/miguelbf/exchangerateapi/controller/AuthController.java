package com.miguelbf.exchangerateapi.controller;

import com.miguelbf.exchangerateapi.model.dto.*;
import com.miguelbf.exchangerateapi.service.impl.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = """
    Issues and renews the JWT bearer tokens required by every other endpoint of this API. \
    These endpoints are the only ones that do not require an `Authorization` header. Apart from user auth, it also \
    supports user creation.""")
@SecurityRequirements({})
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
        summary = "Create an user account with email & password and obtain access and refresh tokens",
        description = """
            Registers a new user with the default `FREE_TIER` role and returns an access token, \
            refresh token and expiry. Use the returned `accessToken` as `Authorization: Bearer \
            <accessToken>` on the protected endpoints."""
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Account created. Contains the access token, the refresh token and the access token lifetime in seconds."
        ),
        @ApiResponse(
            responseCode = "400",
            description = """
                The request body is malformed or missing a required field (`Unreadable Request Body`), \
                or it was read but the payload did not pass validation (`Validation Failure`).""",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class),
                examples = {
                    @ExampleObject(
                        name = "Unreadable Request Body",
                        value = """
                            {
                              "type": "about:blank",
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "Missing or invalid value for field 'email'.",
                              "instance": "/api/auth/signup"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Validation Failure",
                        value = """
                            {
                              "type": "about:blank",
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "email: must be a well-formed email address, password: Password must be between 6 and 128 characters long",
                              "instance": "/api/auth/signup"
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "The account could not be created with the supplied credentials. Empty body.",
            content = @Content
        )
    })
    @PostMapping("/signup")
    public ResponseEntity<AuthResponseDTO> signUp(
        @Valid @RequestBody SignUpRequestDTO signUpRequestDTO
    ) {
        AuthResponseDTO authResponseDTO = authService.signup(signUpRequestDTO);
        if (authResponseDTO == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponseDTO);
    }

    @Operation(
        summary = "Authenticate an existing user with email & password and obtain access and refresh tokens",
        description = """
            Authenticates an existing user with email and password and returns access token, \
            refresh token and expiry. Use the returned `accessToken` as `Authorization: Bearer \
            <accessToken>` on the protected endpoints."""
    )
    @ApiResponse(
        responseCode = "200",
        description = "Authenticated. Contains the access token, the refresh token and the access token lifetime in seconds."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "400",
            description = """
                The request body is malformed or missing a required field (`Unreadable Request Body`), \
                or it was read but the payload did not pass validation (`Validation Failure`).""",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class),
                examples = {
                    @ExampleObject(
                        name = "Unreadable Request Body",
                        value = """
                            {
                              "type": "about:blank",
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "Missing or invalid value for field 'email'.",
                              "instance": "/api/auth/login"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Validation Failure",
                        value = """
                            {
                              "type": "about:blank",
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "email: must be a well-formed email address, password: Password must be between 6 and 128 characters long",
                              "instance": "/api/auth/login"
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid email or password.",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(
                    name = "Invalid Credentials",
                    value = """
                        {
                          "type": "about:blank",
                          "title": "Unauthorized",
                          "status": 401,
                          "detail": "Invalid email or password.",
                          "instance": "/api/auth/login"
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
        @Valid @RequestBody LoginRequestDTO loginRequestDTO
    ) {
        AuthResponseDTO authResponseDTO = authService.login(loginRequestDTO);
        if (authResponseDTO == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(authResponseDTO);
    }

    @Operation(
        summary = "Renew an access token",
        description = """
            Exchanges a still valid refresh token for a new access token, without re-sending the user \
            credentials. The refresh token travels in the request body and is not accepted as a bearer \
            token on any endpoint. Use the returned `accessToken` as `Authorization: Bearer \
            <accessToken>` on the protected endpoints."""
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "A new access token and its lifetime in seconds."
        ),
        @ApiResponse(
            responseCode = "400",
            description = """
                The request body is malformed or missing a required field (`Unreadable Request Body`), \
                or the refresh token is not a well-formed JWT (`Validation Failure`).""",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class),
                examples = {
                    @ExampleObject(
                        name = "Unreadable Request Body",
                        value = """
                            {
                              "type": "about:blank",
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "Missing or invalid value for field 'refreshToken'.",
                              "instance": "/api/auth/refresh"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Validation Failure",
                        value = """
                            {
                              "type": "about:blank",
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "refreshToken: Malformed refresh token",
                              "instance": "/api/auth/refresh"
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = """
                The refresh token is invalid, expired, or is an access token rather than a refresh token. \
                The body is empty; the reason is carried by the `WWW-Authenticate` header \
                ([RFC 6750 Section 3 WWW-Authenticate response headers]\
                (https://datatracker.ietf.org/doc/html/rfc6750#section-3)).""",
            content = @Content,
            headers = @Header(
                name = "WWW-Authenticate",
                description = """
                    Bearer challenge as defined by [RFC 6750 Sec. 3]\
                    (https://datatracker.ietf.org/doc/html/rfc6750#section-3).""",
                schema = @Schema(
                    type = "string",
                    example = "Bearer error=\"invalid_token\", "
                        + "error_description=\"An error occurred while attempting to decode the Jwt: "
                        + "Signed JWT rejected: Invalid signature\", "
                        + "error_uri=\"https://tools.ietf.org/html/rfc6750#section-3.1\", "
                        + "resource_metadata=\"http://localhost:8080/.well-known/oauth-protected-resource\""
                )
            )
        )
    })
    @PostMapping("/refresh")
    public ResponseEntity<JwtRefreshResponseDTO> refreshToken(
        @Valid @RequestBody JwtRefreshRequestDTO jwtRefreshRequestDTO
    ) {
        JwtRefreshResponseDTO jwtRefreshResponseDTO = authService.refresh(jwtRefreshRequestDTO);
        if (jwtRefreshResponseDTO == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(jwtRefreshResponseDTO);
    }


}