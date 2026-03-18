package com.tuttime.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * IP Whitelist Configuration (Optional)
 * 
 * <p>Configures IP whitelisting for admin endpoints.
 * If enabled, only requests from whitelisted IPs will be allowed.
 * 
 * <p>To enable IP whitelisting, set:
 * <pre>
 * app.security.ip-whitelist.enabled=true
 * app.security.ip-whitelist.allowed-ips=192.168.1.1,10.0.0.1
 * </pre>
 */
@Configuration
public class IpWhitelistConfig {

    @Value("${app.security.ip-whitelist.enabled:false}")
    private boolean enabled;

    @Value("${app.security.ip-whitelist.allowed-ips:}")
    private String allowedIps;

    /**
     * Get whitelisted IP addresses
     */
    public Set<String> getAllowedIps() {
        if (!enabled || allowedIps == null || allowedIps.trim().isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(allowedIps.split(",")));
    }

    /**
     * Check if IP whitelisting is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Check if an IP address is whitelisted
     */
    public boolean isIpAllowed(String ipAddress) {
        if (!enabled) {
            return true; // If disabled, allow all IPs
        }
        Set<String> allowed = getAllowedIps();
        if (allowed.isEmpty()) {
            return true; // If no IPs configured, allow all (should be configured in production)
        }
        return allowed.contains(ipAddress);
    }
}

