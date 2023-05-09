package org.apereo.cas.aup;

import org.apereo.cas.configuration.model.support.aup.AcceptableUsagePolicyProperties;
import org.apereo.cas.ticket.registry.TicketRegistrySupport;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.HttpUtils;
import org.apereo.cas.util.LoggingUtils;
import org.apereo.cas.util.serialization.JacksonObjectMapperFactory;
import org.apereo.cas.web.support.WebUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.HttpEntityContainer;
import org.apache.hc.core5.http.HttpResponse;
import org.hjson.JsonValue;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.webflow.execution.RequestContext;

import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

/**
 * This is {@link RestAcceptableUsagePolicyRepository}.
 * Examines the principal attribute collection to determine if
 * the policy has been accepted, and if not, allows for a configurable
 * way so that user's choice can later be remembered and saved back via REST.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@Slf4j
public class RestAcceptableUsagePolicyRepository extends BaseAcceptableUsagePolicyRepository {
    private static final ObjectMapper MAPPER = JacksonObjectMapperFactory.builder()
        .defaultTypingEnabled(false).build().toObjectMapper();

    @Serial
    private static final long serialVersionUID = 1600024683199961892L;

    public RestAcceptableUsagePolicyRepository(final TicketRegistrySupport ticketRegistrySupport,
                                               final AcceptableUsagePolicyProperties aupProperties) {
        super(ticketRegistrySupport, aupProperties);
    }

    @Override
    public boolean submit(final RequestContext requestContext) {
        HttpResponse response = null;
        try {
            val rest = aupProperties.getRest();
            val request = WebUtils.getHttpServletRequestFromExternalWebflowContext(requestContext);
            val principal = WebUtils.getAuthentication(requestContext).getPrincipal();
            val service = WebUtils.getService(requestContext);
            val parameters = CollectionUtils.<String, String>wrap(
                "username", principal.getId(),
                "locale", request.getLocale().toString());
            if (service != null) {
                parameters.put("service", service.getId());
            }
            val exec = HttpUtils.HttpExecutionRequest.builder()
                .basicAuthPassword(rest.getBasicAuthPassword())
                .basicAuthUsername(rest.getBasicAuthUsername())
                .method(HttpMethod.valueOf(rest.getMethod().toUpperCase(Locale.ENGLISH)))
                .url(rest.getUrl())
                .parameters(parameters)
                .build();
            response = HttpUtils.execute(exec);
            val statusCode = Optional.ofNullable(response).map(HttpResponse::getCode).orElseGet(HttpStatus.SERVICE_UNAVAILABLE::value);
            LOGGER.debug("AUP submit policy request returned with response code [{}]", statusCode);
            return HttpStatus.valueOf(statusCode).is2xxSuccessful();
        } finally {
            HttpUtils.close(response);
        }
    }

    @Override
    public Optional<AcceptableUsagePolicyTerms> fetchPolicy(final RequestContext requestContext) {
        HttpResponse response = null;
        try {
            val rest = aupProperties.getRest();
            val url = StringUtils.appendIfMissing(rest.getUrl(), "/").concat("policy");
            val principal = WebUtils.getAuthentication(requestContext).getPrincipal();
            val request = WebUtils.getHttpServletRequestFromExternalWebflowContext(requestContext);

            val exec = HttpUtils.HttpExecutionRequest.builder()
                .basicAuthPassword(rest.getBasicAuthPassword())
                .basicAuthUsername(rest.getBasicAuthUsername())
                .method(HttpMethod.valueOf(rest.getMethod().toUpperCase(Locale.ENGLISH)))
                .url(url)
                .parameters(CollectionUtils.wrap("username", principal.getId(),
                    "locale", request.getLocale().toString()))
                .build();
            response = HttpUtils.execute(exec);
            val statusCode = response.getCode();
            if (HttpStatus.valueOf(statusCode).is2xxSuccessful()) {
                val result = IOUtils.toString(((HttpEntityContainer) response).getEntity().getContent(), StandardCharsets.UTF_8);
                val terms = MAPPER.readValue(JsonValue.readHjson(result).toString(), AcceptableUsagePolicyTerms.class);
                return Optional.ofNullable(terms);
            }
            LOGGER.warn("AUP fetch policy request returned with response code [{}] check your API for problems", statusCode);
        } catch (final Exception e) {
            LoggingUtils.error(LOGGER, e);
        } finally {
            HttpUtils.close(response);
        }
        return Optional.empty();
    }
}
