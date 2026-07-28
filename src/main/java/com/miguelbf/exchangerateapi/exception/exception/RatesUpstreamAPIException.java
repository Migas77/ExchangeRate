package com.miguelbf.exchangerateapi.exception.exception;

import com.miguelbf.exchangerateapi.config.logging.CustomLogStructureFields;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpMethod;

import java.net.URI;
import java.util.Map;

@Getter
public abstract sealed class RatesUpstreamAPIException extends RuntimeException implements CustomLogStructureFields {

    private final URI url;
    private final HttpMethod method;

    protected RatesUpstreamAPIException(String message, Throwable cause, URI url, HttpMethod method) {
        super(message, cause);
        this.url = url;
        this.method = method;
    }

    @Override
    public Map<String, @Nullable Object> getLogStructureFields() {
        return Map.of(
            "url", this.url,
            "method", this.method
        );
    }

    @Getter
    public static final class DocumentedHttpError extends RatesUpstreamAPIException {
        // upstream API returns expected / documented error status code (probably a client-side issue)
        public DocumentedHttpError(String message, Throwable cause, URI url, HttpMethod method) {
            super(message, cause, url, method);
        }
    }

    @Getter
    public static final class UnexpectedHttpError extends RatesUpstreamAPIException {
        // upstream API returns unexpected / not documented error status code (probably an upstream issue)
        public UnexpectedHttpError(String message, Throwable cause, URI url, HttpMethod method) {
            super(message, cause, url, method);
        }
    }

    @Getter
    public static final class UnexpectedEmptyResponse extends RatesUpstreamAPIException {
        // upstream API returns unexpected empty response (probably an upstream issue)
        public UnexpectedEmptyResponse(String message, Throwable cause, URI url, HttpMethod method) {
            super(message, cause, url, method);
        }
    }

}
