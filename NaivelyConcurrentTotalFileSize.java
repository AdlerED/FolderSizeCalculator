import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class NaivelyConcurrentTotalFileSize implements Callable{
    public String fileName;

    private long getTotalSizeOfFilesInDir(final ExecutorService service,
                                          final File file) throws InterruptedException, ExecutionException,
            TimeoutException {
        if (file.isFile())
            return file.length();

        long total = 0;
        final File[] children = file.listFiles();

        if (children != null) {
            final List<Future<Long>> partialTotalFutures = new ArrayList<Future<Long>>();
            for (final File child : children) {
                ((ArrayList) partialTotalFutures).add(service.submit(new Callable<Long>() {
                    public Long call() throws InterruptedException,
                            ExecutionException, TimeoutException {
                        return getTotalSizeOfFilesInDir(service, child);
                    }
                }));
            }

            for (final Future<Long> partialTotalFuture : partialTotalFutures)
                    total += partialTotalFuture.get(MainWindow.getTimeOut(), TimeUnit.SECONDS);
        }

        return total;

    }

    private long getTotalSizeOfFile(final String fileName)
            throws InterruptedException, ExecutionException, TimeoutException {
        final ExecutorService service = Executors.newFixedThreadPool(MainWindow.getThrs()); //线程池
        try {
            return getTotalSizeOfFilesInDir(service, new File(fileName));
        } finally {
            service.shutdown();
        }
    }

    public void setVar(String set) {
        fileName = set;
    }

    @Override
    public Object call() throws Exception{
        synchronized (this) {
            final long start = System.nanoTime();
            final long total = new NaivelyConcurrentTotalFileSize()
                    .getTotalSizeOfFile(fileName);
            final long end = System.nanoTime();
            //System.out.println("Total Size: " + total);
            System.out.println("Time taken: " + (end - start) / 1.0e9);
            return total / 1024 / 1024;
        }
    }
}
