package uk.gov.hmcts.reform.professionalapi.configuration;

import com.launchdarkly.sdk.server.LDClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.hmcts.reform.professionalapi.util.FeatureConditionEvaluation;

@Configuration
public class LaunchDarklyConfiguration implements WebMvcConfigurer {

    @Bean
    public LDClient ldClient(@Value("${launchdarkly.sdk.key}") String sdkKey) {
        return new LDClient(sdkKey);
    }

    @Autowired
    private FeatureConditionEvaluation featureConditionEvaluation;

    /**
     * add here entry in registry and api endpoint path pattern like below
     * registry.addInterceptor(featureConditionEvaluation)
     * addPathPatterns("/refdata/external/v1/organisations/status/**");
     * @param registry registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
       // add entry
    }
}
