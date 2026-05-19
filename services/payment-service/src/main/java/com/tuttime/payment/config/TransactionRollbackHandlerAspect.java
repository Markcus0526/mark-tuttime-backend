package com.tuttime.payment.config;

import com.tuttime.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Global aspect to handle transaction rollback-only errors gracefully.
 *
 * <p>This aspect intercepts transactional methods and catches rollback-only errors,
 * converting them into more user-friendly exceptions or logging them appropriately.
 *
 * <p>Order is set to 1 to execute before other aspects (like ReadOnlyTransactionInterceptor).
 */
@Slf4j
@Aspect
@Component
@Order(1) // Execute before other aspects
public class TransactionRollbackHandlerAspect {

    /**
     * Intercepts methods annotated with @Transactional to handle rollback-only errors.
     *
     * @param pjp           ProceedingJoinPoint for the intercepted method
     * @param transactional @Transactional annotation
     * @return Result of the intercepted method
     * @throws Throwable If the method throws an exception
     */
    @Around("@annotation(transactional)")
    public Object handleTransactionRollback(ProceedingJoinPoint pjp, Transactional transactional) throws Throwable {
        try {
            return pjp.proceed();
        } catch (TransactionSystemException e) {
            // Check if this is a rollback-only error
            Throwable cause = e.getCause();
            if (cause instanceof UnexpectedRollbackException) {
                String methodName = pjp.getSignature().getDeclaringTypeName() + "." + pjp.getSignature().getName();
                log.error("Transaction rollback-only error detected in method: {}. " +
                        "This usually indicates an exception was caught and swallowed in a nested transaction. " +
                        "Error: {}", methodName, e.getMessage(), e);

                // Log root cause if available
                Throwable rootCause = e.getRootCause();
                if (rootCause != null && rootCause != cause) {
                    log.error("Root cause: {}", rootCause.getMessage(), rootCause);
                }

                // Re-throw as TransactionSystemException with more context
                // The calling code should handle this appropriately
                throw new BusinessException(
                        "Transaction failed due to rollback-only status. " +
                                "This usually indicates a nested transaction failure was caught and not properly handled. " +
                                "Method: " + methodName, e);
            }
            // Not a rollback-only error, re-throw as-is
            throw e;
        } catch (UnexpectedRollbackException e) {
            // Direct UnexpectedRollbackException (not wrapped in TransactionSystemException)
            String methodName = pjp.getSignature().getDeclaringTypeName() + "." + pjp.getSignature().getName();
            log.error("Unexpected rollback exception in method: {}. Error: {}", methodName, e.getMessage(), e);
            throw e;
        } catch (DataAccessException e) {
            // Database exceptions should rollback transaction - this is expected behavior
            // Log for monitoring but don't convert
            String methodName = pjp.getSignature().getDeclaringTypeName() + "." + pjp.getSignature().getName();
            log.debug("DataAccessException in transactional method: {} (this will rollback transaction as expected). Error: {}",
                    methodName, e.getMessage());
            throw e;
        }
    }

    /**
     * Intercepts classes annotated with @Transactional to handle rollback-only errors.
     *
     * @param pjp ProceedingJoinPoint for the intercepted method
     * @return Result of the intercepted method
     * @throws Throwable If the method throws an exception
     */
    @Around("@within(org.springframework.transaction.annotation.Transactional)")
    public Object handleTransactionRollbackClass(ProceedingJoinPoint pjp) throws Throwable {
        try {
            return pjp.proceed();
        } catch (TransactionSystemException e) {
            // Check if this is a rollback-only error
            Throwable cause = e.getCause();
            if (cause instanceof UnexpectedRollbackException) {
                String methodName = pjp.getSignature().getDeclaringTypeName() + "." + pjp.getSignature().getName();
                log.error("Transaction rollback-only error detected in method: {}. " +
                        "This usually indicates an exception was caught and swallowed in a nested transaction. " +
                        "Error: {}", methodName, e.getMessage(), e);

                // Log root cause if available
                Throwable rootCause = e.getRootCause();
                if (rootCause != null && rootCause != cause) {
                    log.error("Root cause: {}", rootCause.getMessage(), rootCause);
                }

                // Re-throw as TransactionSystemException with more context
                throw new BusinessException(
                        "Transaction failed due to rollback-only status. " +
                                "This usually indicates a nested transaction failure was caught and not properly handled. " +
                                "Method: " + methodName, e);
            }
            throw e;
        } catch (UnexpectedRollbackException e) {
            String methodName = pjp.getSignature().getDeclaringTypeName() + "." + pjp.getSignature().getName();
            log.error("Unexpected rollback exception in method: {}. Error: {}", methodName, e.getMessage(), e);
            throw e;
        } catch (DataAccessException e) {
            String methodName = pjp.getSignature().getDeclaringTypeName() + "." + pjp.getSignature().getName();
            log.debug("DataAccessException in transactional method: {} (this will rollback transaction as expected). Error: {}",
                    methodName, e.getMessage());
            throw e;
        }
    }
}

