package com.example.antiaddictionapp;

import java.util.HashSet;
import java.util.Set;

public class FilteringEngine {

    private Set<String> blockedDomains;

    public FilteringEngine() {
        blockedDomains = new HashSet<>();
        // In a real application, this list would be populated from a local database,
        // a downloaded blocklist, or queried through an AI model (like TFLite) 
        // to classify the domain name.
        
        // Example domains
        blockedDomains.add("pornhub.com");
        blockedDomains.add("xhamster.com");
        blockedDomains.add("xvideos.com");
        // Add variations and common keywords here
    }

    /**
     * Checks if a domain should be blocked.
     * @param domain The domain name to check.
     * @return true if the domain should be blocked, false otherwise.
     */
    public boolean shouldBlock(String domain) {
        if (domain == null || domain.isEmpty()) {
            return false;
        }
        
        String lowerDomain = domain.toLowerCase();
        
        // Block by exact match or if it contains specific substrings
        if (blockedDomains.contains(lowerDomain) || lowerDomain.contains("xxx") || lowerDomain.contains("porn") || lowerDomain.contains("sex")) {
            return true;
        }
        
        return false;
    }
}
