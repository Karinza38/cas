package org.apereo.cas.configuration.model.support.oidc;

import org.apereo.cas.configuration.support.DurationCapable;
import org.apereo.cas.configuration.support.RequiresModule;

import com.fasterxml.jackson.annotation.JsonFilter;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * This is {@link OidcClientRegistrationProperties}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@RequiresModule(name = "cas-server-support-oidc")
@Getter
@Setter
@Accessors(chain = true)
@JsonFilter("OidcClientRegistrationProperties")
public class OidcClientRegistrationProperties implements Serializable {

    private static final long serialVersionUID = 123128615694269276L;

    /**
     * Whether dynamic registration operates in {@code OPEN} or {@code PROTECTED} mode.
     */
    private String dynamicClientRegistrationMode;

    /**
     * When client secret is issued by CAS, this is the period
     * that gets added to the current time measured in UTC to determine
     * the client secret's expiration date. An example value would be {@code P14D}
     * forcing client applications to expire their client secret in 2 weeks after the
     * registration date. Expired client secrets can be updated using the client configuration
     * endpoint. A value of {@code 0} indicates that client secrets would never expire.
     */
    @DurationCapable
    private String clientSecretExpiration = "0";
}
