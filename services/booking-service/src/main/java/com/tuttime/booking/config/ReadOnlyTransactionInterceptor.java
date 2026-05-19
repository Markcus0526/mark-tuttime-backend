package com.tuttime.booking.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Interceptor to route read-only transactions to read replica
 * Only active when read replica is configured
 */
@Aspect
@Component
@Order(0)
@ConditionalOnProperty(name = "spring.datasource.replica.url")
public class ReadOnlyTransactionInterceptor {

    @Around("@annotation(transactional)")
    public Object routeToReadReplica(ProceedingJoinPoint pjp, Transactional transactional) throws Throwable {
        try {
            if (transactional != null && transactional.readOnly()) {
                ReadWriteDataSourceConfig.ReadWriteRoutingDataSourceContext.setDataSourceType("read");
            } else {
                ReadWriteDataSourceConfig.ReadWriteRoutingDataSourceContext.setDataSourceType("write");
            }
            return pjp.proceed();
        } finally {
            ReadWriteDataSourceConfig.ReadWriteRoutingDataSourceContext.clearDataSourceType();
        }
    }

    @Around("@within(transactional)")
    public Object routeToReadReplicaClass(ProceedingJoinPoint pjp, Transactional transactional) throws Throwable {
        try {
            if (transactional != null && transactional.readOnly()) {
                ReadWriteDataSourceConfig.ReadWriteRoutingDataSourceContext.setDataSourceType("read");
            } else {
                ReadWriteDataSourceConfig.ReadWriteRoutingDataSourceContext.setDataSourceType("write");
            }
            return pjp.proceed();
        } finally {
            ReadWriteDataSourceConfig.ReadWriteRoutingDataSourceContext.clearDataSourceType();
        }
    }
}

