/**
 * Copyright (c) 2022 BlinkPay
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package nz.co.blink.debit.config;

import jakarta.validation.Validator;
import nz.co.blink.debit.client.v1.BlinkDebitClient;
import nz.co.blink.debit.client.v1.EnduringConsentsApiClient;
import nz.co.blink.debit.client.v1.MetaApiClient;
import nz.co.blink.debit.client.v1.OAuthApiClient;
import nz.co.blink.debit.client.v1.PaymentsApiClient;
import nz.co.blink.debit.client.v1.QuickPaymentsApiClient;
import nz.co.blink.debit.client.v1.RefundsApiClient;
import nz.co.blink.debit.client.v1.SingleConsentsApiClient;
import nz.co.blink.debit.helpers.AccessTokenHandler;
import nz.co.blink.debit.service.ValidationService;
import nz.co.blink.debit.service.impl.JakartaValidationServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

/**
 * The {@link AutoConfiguration} for Spring-based consumers.
 */
@AutoConfiguration
@PropertySource(value = "classpath:blinkdebit.properties", ignoreResourceNotFound = true)
@PropertySource(value = "classpath:blinkdebit.yaml", ignoreResourceNotFound = true,
        factory = YamlPropertySourceFactory.class)
@PropertySource(value = "classpath:blinkdebit.yml", ignoreResourceNotFound = true,
        factory = YamlPropertySourceFactory.class)
@Import(BlinkDebitConfiguration.class)
@EnableConfigurationProperties(BlinkPayProperties.class)
@ConditionalOnClass(BlinkDebitClient.class)
public class BlinkDebitAutoConfiguration {

    private final ReactorClientHttpConnector connector;

    private final Validator validator;


    private final BlinkPayProperties properties;

    /**
     * Default constructor.
     *
     * @param connector  the {@link ReactorClientHttpConnector}
     * @param validator  the {@link Validator}
     * @param properties the {@link BlinkPayProperties}
     */
    @Autowired
    public BlinkDebitAutoConfiguration(@Qualifier("blinkDebitClientHttpConnector") ReactorClientHttpConnector connector,
                                       Validator validator, BlinkPayProperties properties) {
        this.connector = connector;
        this.validator = validator;
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    ValidationService validationService() {
        return new JakartaValidationServiceImpl(validator);
    }

    @Bean
    @ConditionalOnMissingBean
    OAuthApiClient oauthApiClient() {
        return new OAuthApiClient(connector, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    AccessTokenHandler accessTokenHandler() {
        return new AccessTokenHandler(oauthApiClient());
    }

    @Bean
    @ConditionalOnMissingBean
    SingleConsentsApiClient singleConsentsApiClient() {
        return new SingleConsentsApiClient(connector, properties, accessTokenHandler(), validationService());
    }

    @Bean
    @ConditionalOnMissingBean
    EnduringConsentsApiClient enduringConsentsApiClient() {
        return new EnduringConsentsApiClient(connector, properties, accessTokenHandler(), validationService());
    }

    @Bean
    @ConditionalOnMissingBean
    QuickPaymentsApiClient quickPaymentsApiClient() {
        return new QuickPaymentsApiClient(connector, properties, accessTokenHandler(), validationService());
    }

    @Bean
    @ConditionalOnMissingBean
    PaymentsApiClient paymentsApiClient() {
        return new PaymentsApiClient(connector, properties, accessTokenHandler(), validationService());
    }

    @Bean
    @ConditionalOnMissingBean
    RefundsApiClient refundsApiClient() {
        return new RefundsApiClient(connector, properties, accessTokenHandler(), validationService());
    }

    @Bean
    @ConditionalOnMissingBean
    MetaApiClient metaApiClient() {
        return new MetaApiClient(connector, properties, accessTokenHandler());
    }

    @Bean
    @ConditionalOnMissingBean
    public BlinkDebitClient blinkDebitClient() {
        return new BlinkDebitClient(singleConsentsApiClient(), enduringConsentsApiClient(), quickPaymentsApiClient(),
                paymentsApiClient(), refundsApiClient(), metaApiClient(), validationService());
    }
}
