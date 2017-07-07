package alien4cloud.plugin.Janus;

import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;

/**
 *  Manage a pool of threads to execute some AlienTask objects
 */
@Slf4j
public class TaskManager {

    protected List<AlienTask> workList = new LinkedList<>();
    protected boolean valid = true; // set to false when WorkManager is removed.

    protected static int threadnumber = 0;  // number of created threads
    protected int maxpoolsz;
    protected int minpoolsz;
    protected int poolsz; // current size of thread pool
    protected int freeThreads;  // free threads ready to work
    protected long waitingTime; // in millisec

    /**
     * Constructor. Start the minimum number of threads to be run.
     * @param minsz
     * @param maxsz
     * @param threadwait max time in seconds a thread will wait
     */
    public TaskManager(final int minsz, final int maxsz, final long threadwait) {
        minpoolsz = minsz;
        maxpoolsz = maxsz;
        waitingTime = threadwait * 1000L;
        for (poolsz = 0; poolsz < minsz; poolsz++) {
            WorkThread st = new WorkThread(this, threadnumber++);
            st.start();
        }
    }
    
    /**
     * Get the next JWork object to be run.
     * @return next JWork object to be run, or null if thread must end.
     */
    public void nextWork() throws Exception {
        AlienTask task;
        boolean haswait = false;
        synchronized (workList) {
            while (workList.isEmpty()) {
                if ((haswait && freeThreads > minpoolsz) || !valid) {
                    // Finish this thread if too many threads or pool removed
                    poolsz--;
                    throw new InterruptedException("Thread ending");
                }
                try {
                    // Wait for a new task to run
                    freeThreads++;
                    log.debug("waiting");
                    workList.wait(waitingTime);
                    log.debug("notified");
                    freeThreads--;
                    haswait = true;
                } catch (InterruptedException e) {
                    freeThreads--;
                    poolsz--;
                    throw e;
                }
            }
            task = workList.remove(0);
        }
        // run the task
        task.run();
    }

    /**
     * Add a task to run
     * If not enough threads, create a new one.
     * @param task
     */
    public void addTask(final AlienTask task) {
         synchronized (workList) {
            workList.add(task);
            if (poolsz < maxpoolsz && workList.size() > freeThreads) {
                // We need one more thread.
                poolsz++;
                WorkThread st = new WorkThread(this, threadnumber++);
                st.start();
            } else {
                // Just wake up a thread waiting for work.
                workList.notify();
            }
        }
    }

    /**
     * Thread executing works for the work manager.
     */
    class WorkThread extends Thread {

        private TaskManager mgr;
        private int number;

        /**
         * Constructor
         * @param m The WorkManager
         * @param num thread number
         */
        WorkThread(final TaskManager m, final int num) {
            super("WorkThread-" + num);
            mgr = m;
            number = num;
        }

        @Override
        public void run() {
            log.debug("running " + number);
            while (true) {
                try {
                    mgr.nextWork();
                } catch (InterruptedException e) {
                    log.debug("Exiting: ", e);
                    return;
                } catch (Exception e) {
                    log.error("Exception during work run: ", e);
                }
            }
        }

    }
}
