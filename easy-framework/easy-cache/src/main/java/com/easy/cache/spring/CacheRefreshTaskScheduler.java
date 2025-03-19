package com.easy.cache.spring;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;

import com.easy.cache.annotation.CacheRefresh;

/**
 * 缓存刷新任务调度器
 */
@Component
public class CacheRefreshTaskScheduler {

    @Autowired
    private TaskScheduler taskScheduler;

    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public void scheduleRefreshTask(String cacheName, Method method) {
        CacheRefresh refresh = method.getAnnotation(CacheRefresh.class);
        if (refresh == null) {
            return;
        }

        // 取消已存在的任务
        cancelTask(cacheName);

        // 创建新的调度任务
        Runnable task = () -> {
            try {
                Object result = method.invoke(null);
                if (result != null) {
                    // TODO: 更新缓存
                }
            } catch (Exception e) {
                // 记录错误日志
                e.printStackTrace();
            }
        };

        // 创建周期性触发器
        PeriodicTrigger trigger = new PeriodicTrigger(refresh.interval() * 1000);
        trigger.setInitialDelay(0);

        // 调度任务
        ScheduledFuture<?> future = taskScheduler.schedule(task, trigger);
        scheduledTasks.put(cacheName, future);
    }

    public void cancelTask(String cacheName) {
        ScheduledFuture<?> future = scheduledTasks.remove(cacheName);
        if (future != null) {
            future.cancel(true);
        }
    }
}