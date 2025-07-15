package org.example.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "supabase")
@Data
public class SupabaseConfig {
    
    private String url;
    private String anonKey;
    
    public String getRestUrl() {
        return url + "/rest/v1";
    }
    
    public String getAuthUrl() {
        return url + "/auth/v1";
    }
    
    public String getStorageUrl() {
        return url + "/storage/v1";
    }
} 