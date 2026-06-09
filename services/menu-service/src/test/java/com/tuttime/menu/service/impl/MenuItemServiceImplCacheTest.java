package com.tuttime.menu.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the cache-eviction fix: evictCacheByBranch evicts the branch_menus cache
 * PROGRAMMATICALLY (via CacheManager), so it works even when called from sibling methods
 * within the same bean (Spring @CacheEvict would be bypassed on self-invocation).
 */
class MenuItemServiceImplCacheTest {

    @Test
    void evictCacheByBranchEvictsBranchMenusCache() {
        CacheManager cacheManager = mock(CacheManager.class);
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache("branch_menus")).thenReturn(cache);

        // Constructor args follow field declaration order; only cacheManager is exercised here.
        MenuItemServiceImpl service = new MenuItemServiceImpl(
                null, null, null, null, null, null, null, null, null, null, cacheManager);

        UUID branchId = UUID.randomUUID();
        service.evictCacheByBranch(branchId);

        verify(cache).evict(eq(branchId));
    }
}
