package com.englishvocab.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security.remember-me")
public class RememberMeProperties {

    /**
     * Secret key used to generate remember-me tokens.
     */
    private String key = "englishvocab-remember-me";

    /**
     * Name of the remember-me cookie.
     */
    private String cookieName = "englishvocab-remember-me";

    /**
     * Request parameter name for remember-me toggle.
     */
    private String parameter = "remember-me";

    /**
     * Token validity period in days.
     */
    private int validityDays = 7;
}
