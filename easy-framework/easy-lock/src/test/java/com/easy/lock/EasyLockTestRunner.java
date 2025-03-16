package com.easy.lock;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import com.easy.lock.annotation.EasyLockAnnotationTest;
import com.easy.lock.core.RedisLockExecutorTest;
import com.easy.lock.core.RedisLockTest;
import com.easy.lock.monitor.LockMonitorTest;
import com.easy.lock.template.LockTemplateTest;

/**
 * 分布式锁测试运行器
 */
public class EasyLockTestRunner {

    public static void main(String[] args) {
        System.out.println("开始运行分布式锁测试...");

        Result result = JUnitCore.runClasses(
                RedisLockExecutorTest.class,
                RedisLockTest.class,
                LockMonitorTest.class,
                EasyLockAnnotationTest.class,
                LockTemplateTest.class);

        System.out.println("\n测试完成！");
        System.out.println("运行测试用例总数: " + result.getRunCount());
        System.out.println("通过测试用例数: " + (result.getRunCount() - result.getFailureCount()));
        System.out.println("失败测试用例数: " + result.getFailureCount());
        System.out.println("测试运行时间: " + result.getRunTime() + "ms");

        if (!result.wasSuccessful()) {
            System.out.println("\n失败的测试:");
            for (Failure failure : result.getFailures()) {
                System.out.println(failure.toString());
            }
            System.exit(1);
        }

        System.out.println("\n所有测试通过！");
    }
}