package server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ThreadPoolManager - Quản lý tập trung Thread Pool và cấu hình tối ưu
 * 
 * Sử dụng:
 *  - ThreadPoolExecutor với custom queue
 *  - RejectedExecutionHandler để xử lý quá tải
 *  - Monitoring metrics (tasks, queue size, active threads)
 */
public class ThreadPoolManager {

    private static ThreadPoolManager instance;
    private final ThreadPoolExecutor executor;
    private final AtomicInteger tasksSubmitted = new AtomicInteger(0);
    private final AtomicInteger tasksCompleted = new AtomicInteger(0);
    private final AtomicLong totalTimeMs = new AtomicLong(0);

    // Cấu hình
    private static final int CORE_THREADS = 20;          // Threads luôn chạy
    private static final int MAX_THREADS = 50;           // Max threads khi quá tải
    private static final long KEEP_ALIVE_TIME = 60;      // Giây - thời gian chờ
    private static final int QUEUE_CAPACITY = 200;       // Queue dự phòng cho tasks

    private ThreadPoolManager() {
        // Tạo BlockingQueue với capacity hạn chế
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

        // Tạo ThreadPoolExecutor với custom RejectedExecutionHandler
        this.executor = new ThreadPoolExecutor(
            CORE_THREADS,                              // corePoolSize
            MAX_THREADS,                               // maximumPoolSize
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            queue,
            new CustomThreadFactory("ClientHandler"),
            new RejectionHandler()                     // Custom rejection handler
        );

        // Cho phép thread core timeout nếu không có task
        executor.allowCoreThreadTimeOut(true);

        System.out.println("[ThreadPool] ✓ Initialized: " +
            "core=" + CORE_THREADS + ", max=" + MAX_THREADS +
            ", queueCapacity=" + QUEUE_CAPACITY);
    }

    /**
     * Singleton instance
     */
    public static synchronized ThreadPoolManager getInstance() {
        if (instance == null) {
            instance = new ThreadPoolManager();
        }
        return instance;
    }

    /**
     * Submit một ClientHandler task
     */
    public void submitTask(ClientHandler handler) {
        long startTime = System.currentTimeMillis();
        
        executor.execute(() -> {
            try {
                handler.run();
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                tasksCompleted.incrementAndGet();
                totalTimeMs.addAndGet(duration);
            }
        });
        
        tasksSubmitted.incrementAndGet();
    }

    /**
     * Submit async task với CompletableFuture
     */
    public <T> CompletableFuture<T> submitAsync(Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                T result = task.call();
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Shutdown graceful
     */
    public void shutdown() {
        System.out.println("[ThreadPool] Shutting down...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                System.out.println("[ThreadPool] Force shutdown remaining tasks...");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        printMetrics();
    }

    /**
     * In ra metrics hiệu năng
     */
    public void printMetrics() {
        int queueSize = executor.getQueue().size();
        int activeThreads = executor.getActiveCount();
        int poolSize = executor.getPoolSize();
        
        System.out.println("\n[ThreadPool Metrics]");
        System.out.println("  Active Threads: " + activeThreads + " / " + poolSize);
        System.out.println("  Queue Size: " + queueSize);
        System.out.println("  Tasks Submitted: " + tasksSubmitted.get());
        System.out.println("  Tasks Completed: " + tasksCompleted.get());
        if (tasksCompleted.get() > 0) {
            long avgTime = totalTimeMs.get() / tasksCompleted.get();
            System.out.println("  Avg Task Time: " + avgTime + "ms");
        }
        System.out.println();
    }

    /**
     * Custom ThreadFactory - Tạo threads với tên dễ debug
     */
    private static class CustomThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(0);

        public CustomThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName(prefix + "-" + counter.incrementAndGet());
            thread.setDaemon(false);
            return thread;
        }
    }

    /**
     * Custom RejectedExecutionHandler - Xử lý khi queue đầy
     */
    private static class RejectionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            System.err.println("[ThreadPool] ⚠ REJECTED - Queue full! " +
                "Active=" + executor.getActiveCount() + 
                ", Queue=" + executor.getQueue().size());
            
            // Có thể log client IP để phân tích
            // hoặc block client này nếu muốn
        }
    }

    // Getters for monitoring
    public int getActiveThreadCount() { return executor.getActiveCount(); }
    public int getPoolSize() { return executor.getPoolSize(); }
    public int getQueueSize() { return executor.getQueue().size(); }
    public long getCompletedTaskCount() { return executor.getCompletedTaskCount(); }
}
