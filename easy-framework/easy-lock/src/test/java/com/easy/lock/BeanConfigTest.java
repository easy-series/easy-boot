package com.easy.lock;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.easy.lock.core.RedisLock;
import com.easy.lock.core.executor.RedisLockExecutor;
import com.easy.lock.monitor.LockMonitor;
import com.easy.lock.template.LockTemplate;

/**
 * Bean配置测试类
 * 验证所有必要的Bean是否正确注册
 */
public class BeanConfigTest extends BaseLockTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisLockExecutor redisLockExecutor;

    @Autowired
    private RedisLock redisLock;

    @Autowired
    private LockMonitor lockMonitor;

    @Autowired
    private LockTemplate lockTemplate;

    @Test
    public void testBeansExist() {
        assertNotNull("StringRedisTemplate应该存在", stringRedisTemplate);
        assertNotNull("RedisLockExecutor应该存在", redisLockExecutor);
        assertNotNull("RedisLock应该存在", redisLock);
        assertNotNull("LockMonitor应该存在", lockMonitor);
        assertNotNull("LockTemplate应该存在", lockTemplate);
    }
}