package net.i2p.util;

/*
 * free (adj.): unencumbered; not under the control of others
 * Written by jrandom in 2003 and released into the public domain 
 * with no warranty of any kind, either expressed or implied.  
 * It probably won't make your computer catch on fire, or eat 
 * your children, but it might.  Use at your own risk.
 *
 */


import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * In case its useful later...
 * (e.g. w/ native programatic thread dumping, etc)
 *
 * As of 0.9.21, I2PThreads are initialized to NORM_PRIORITY
 * (not the priority of the creating thread).
 */
public class I2PThread extends Thread {
    /**
     *  Non-static to avoid refs to old context in Android.
     *  Probably should just remove all the logging though.
     *  Logging removed, too much trouble with extra contexts
     */
    //private volatile Log _log;
    private static final Set<OOMEventListener> _listeners = new CopyOnWriteArraySet<OOMEventListener>();
    //private String _name;
    //private Exception _createdBy;

    public I2PThread() {
        super();
        setPriority(NORM_PRIORITY);
        //if ( (_log == null) || (_log.shouldLog(Log.DEBUG)) )
        //    _createdBy = new Exception("Created by");
    }

    public I2PThread(String name) {
        super(name);
        setPriority(NORM_PRIORITY);
        //if ( (_log == null) || (_log.shouldLog(Log.DEBUG)) )
        //    _createdBy = new Exception("Created by");
    }

    public I2PThread(Runnable r) {
        super(r);
        setPriority(NORM_PRIORITY);
        //if ( (_log == null) || (_log.shouldLog(Log.DEBUG)) )
        //    _createdBy = new Exception("Created by");
    }

    public I2PThread(Runnable r, String name) {
        super(r, name);
        setPriority(NORM_PRIORITY);
        //if ( (_log == null) || (_log.shouldLog(Log.DEBUG)) )
        //    _createdBy = new Exception("Created by");
    }
    public I2PThread(Runnable r, String name, boolean isDaemon) {
        super(r, name);
	setDaemon(isDaemon);
        setPriority(NORM_PRIORITY);
        //if ( (_log == null) || (_log.shouldLog(Log.DEBUG)) )
        //    _createdBy = new Exception("Created by");
    }
    
    public I2PThread(ThreadGroup g, Runnable r) {
        super(g, r);
        setPriority(NORM_PRIORITY);
        //if ( (_log == null) || (_log.shouldLog(Log.DEBUG)) )
        //    _createdBy = new Exception("Created by");
    }

/****
    private void log(int level, String msg) { log(level, msg, null); }

    private void log(int level, String msg, Throwable t) {
        // we cant assume log is created
        if (_log == null) _log = new Log(I2PThread.class);
        if (_log.shouldLog(level))
            _log.log(level, msg, t);
    }
****/
    
    /**
     *  Overridden to provide useful info to users on OOM, and to prevent
     *  shutting down the whole JVM for what is most likely not a heap issue.
     *  If the calling thread is an I2PThread an OOM would shut down the JVM.
     *  Telling the user to increase the heap size may make the problem worse.
     *  We may be able to continue without this thread, particularly in app context.
     *
     *  @since 0.9.20
     */
    @Override
    public void start() {
        try {
            super.start();
        } catch (OutOfMemoryError oom) {
            System.out.println("ERROR: Thread could not be started: " + getName());
            if (!(SystemVersion.isWindows() || SystemVersion.isAndroid())) {
                System.out.println("Check ulimit -u, /etc/security/limits.conf, or /proc/sys/kernel/threads-max");
            }
            oom.printStackTrace();
            throw new RuntimeException("Thread could not be started", oom);
        }
    }
    
    @Override
    public void run() {
        //_name = Thread.currentThread().getName();
        //log(Log.INFO, "New thread started" + (isDaemon() ? " (daemon): " : ": ") + _name, _createdBy);
        try {
            super.run();
        } catch (Throwable t) {
          /****
            try {
                log(Log.CRIT, "Thread terminated unexpectedly: " + getName(), t);
            } catch (Throwable woof) {
                System.err.println("Died within the OOM itself");
                t.printStackTrace();
            }
          ****/
            if (t instanceof OutOfMemoryError) {
                fireOOM((OutOfMemoryError)t);
            } else {
                System.out.println ("Thread terminated unexpectedly: " + getName());
                t.printStackTrace();
            }
        }
        // This creates a new I2PAppContext after it was deleted
        // in Router.finalShutdown() via RouterContext.killGlobalContext()
        //log(Log.INFO, "Thread finished normally: " + _name);
    }
    
/****
    protected void finalize() throws Throwable {
        //log(Log.DEBUG, "Thread finalized: " + _name);
        super.finalize();
    }
****/
    
    protected void fireOOM(OutOfMemoryError oom) {
        for (OOMEventListener listener : _listeners)
            listener.outOfMemory(oom);
    }

    /** register a new component that wants notification of OOM events */
    public static void addOOMEventListener(OOMEventListener lsnr) {
        _listeners.add(lsnr);
    }

    /** unregister a component that wants notification of OOM events */    
    public static void removeOOMEventListener(OOMEventListener lsnr) {
        _listeners.remove(lsnr);
    }

    public interface OOMEventListener {
        public void outOfMemory(OutOfMemoryError err);
    }

/****
    public static void main(String args[]) {
        I2PThread t = new I2PThread(new Runnable() {
            public void run() {
                throw new NullPointerException("blah");
            }
        });
        t.start();
        try {
            Thread.sleep(10000);
        } catch (Throwable tt) { // nop
        }
    }
****/
}
