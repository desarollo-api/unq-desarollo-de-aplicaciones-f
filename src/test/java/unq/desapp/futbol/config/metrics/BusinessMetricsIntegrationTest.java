package unq.desapp.futbol.config.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

@SpringBootTest(classes = { BusinessMetricsIntegrationTest.TestConfig.class, BusinessMetricsAspect.class })
@Tag("e2e")
class BusinessMetricsIntegrationTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private TestService testService;

    @Test
    void shouldRegisterSuccessMetric() {
        testService.performSuccessOperation();

        assertThat(meterRegistry.find("app_business_operation_success_total")
                .tag("operation", "integration_success")
                .tag("class", TestService.class.getName())
                .counter())
                .isNotNull()
                .satisfies(counter -> assertThat(counter.count()).isEqualTo(1.0));
    }

    @Test
    void shouldRegisterFailureMetric() {
        assertThrows(RuntimeException.class, () -> testService.performFailureOperation());

        assertThat(meterRegistry.find("app_business_operation_failed_total")
                .tag("operation", "integration_failure")
                .tag("class", TestService.class.getName())
                .tag("exception", "RuntimeException")
                .counter())
                .isNotNull()
                .satisfies(counter -> assertThat(counter.count()).isEqualTo(1.0));
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class TestConfig {
        @Bean
        public MeterRegistry meterRegistry() {
            return new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        }

        @Bean
        public TestService testService() {
            return new TestService();
        }
    }

    @Component
    static class TestService {

        @BusinessMetric(name = "integration_success", help = "Integration success help")
        public void performSuccessOperation() {
            // Do nothing
        }

        @BusinessMetric(name = "integration_failure", help = "Integration failure help")
        public void performFailureOperation() {
            throw new RuntimeException("Integration error");
        }
    }
}
