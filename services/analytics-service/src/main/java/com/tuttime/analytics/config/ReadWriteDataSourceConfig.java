package com.tuttime.analytics.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Read/Write DataSource Routing: routes writes to primary, reads to replica. Set spring.datasource.replica.url to enable.
 */
@Configuration
@ConditionalOnExpression("!'${spring.datasource.replica.url:}'.isEmpty()")
public class ReadWriteDataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.replica")
    public DataSourceProperties replicaDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * Creates primary (write) DataSource with HikariCP configuration.
     * Uses Spring Boot's DataSourceBuilder which automatically applies HikariCP properties.
     */
    @Bean
    public DataSource primaryDataSource() {
        return primaryDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    /**
     * Creates replica (read) DataSource with HikariCP configuration.
     * Uses Spring Boot's DataSourceBuilder which automatically applies HikariCP properties.
     */
    @Bean
    public DataSource replicaDataSource() {
        DataSource dataSource = replicaDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();

        // Set pool name for monitoring (if HikariDataSource)
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).setPoolName("HikariCP-Replica");
        }

        return dataSource;
    }

    /**
     * Creates routing DataSource that automatically routes transactions.
     */
    @Bean
    @Primary
    public DataSource routingDataSource() {
        AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
            @Override
            protected Object determineCurrentLookupKey() {
                return ReadWriteRoutingDataSourceContext.getDataSourceType();
            }
        };

        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put("write", primaryDataSource());
        dataSourceMap.put("read", replicaDataSource());

        routingDataSource.setTargetDataSources(dataSourceMap);
        routingDataSource.setDefaultTargetDataSource(dataSourceMap.get("write"));

        return routingDataSource;
    }

    /**
     * Thread-local context to determine which datasource to use.
     * Uses a stack to handle nested transactions properly.
     */
    public static class ReadWriteRoutingDataSourceContext {
        private static final ThreadLocal<Deque<String>> contextHolder = new ThreadLocal<>();

        /**
         * Push datasource type onto the stack for nested transactions.
         * This ensures outer transaction context is preserved when inner transactions complete.
         */
        public static void setDataSourceType(String dataSourceType) {
            Deque<String> stack = contextHolder.get();
            if (stack == null) {
                stack = new ArrayDeque<>();
                contextHolder.set(stack);
            }
            stack.push(dataSourceType);
        }

        /**
         * Get the current datasource type from the top of the stack.
         * Returns "write" as default if no context is set.
         */
        public static String getDataSourceType() {
            Deque<String> stack = contextHolder.get();
            if (stack == null || stack.isEmpty()) {
                return "write";
            }
            return stack.peek();
        }

        /**
         * Pop datasource type from the stack.
         * This restores the previous context for nested transactions.
         */
        public static void clearDataSourceType() {
            Deque<String> stack = contextHolder.get();
            if (stack != null && !stack.isEmpty()) {
                stack.pop();
                // Clean up if stack is empty
                if (stack.isEmpty()) {
                    contextHolder.remove();
                }
            }
        }
    }
}

