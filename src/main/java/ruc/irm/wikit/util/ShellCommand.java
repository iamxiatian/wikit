package ruc.irm.wikit.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.*;

/**
 * A base class for running a unix command like du or df
 */
abstract public class ShellCommand {
    // logging
    private final static Logger LOG = LoggerFactory.getLogger(ShellCommand.class.getName());

    private long interval; // refresh interval in msec
    private long lastTime; // last time the command was performed

    ShellCommand() {
        this(0L);
    }

    ShellCommand(long interval) {
        this.interval = interval;
        this.lastTime = (interval < 0) ? 0 : -interval;
    }

    /**
     * check to see if a command needs to be execuated
     */
    protected void run() throws IOException {
        if (lastTime + interval > System.currentTimeMillis())
            return;
        runCommand();
    }

    /**
     * Run a command
     */
    private void runCommand() throws IOException {
        Process process;
        process = Runtime.getRuntime().exec(getExecString());

        try {
            if (process.waitFor() != 0) {
                throw new IOException(new BufferedReader(new InputStreamReader(process.getErrorStream())).readLine());
            }
            parseExecResult(new BufferedReader(new InputStreamReader(process.getInputStream())));
        } catch (InterruptedException e) {
            throw new IOException(e.toString());
        } finally {
            process.destroy();
            lastTime = System.currentTimeMillis();
        }
    }

    /**
     * return an array comtaining the command name & its parameters
     */
    protected abstract String[] getExecString();

    /**
     * Parse the execution result
     */
    protected abstract void parseExecResult(BufferedReader lines) throws IOException;

    // / A simple implementation of Command
    public static class SimpleCommandExecutor extends ShellCommand {

        private String[] command;
        private StringBuffer reply;

        SimpleCommandExecutor(String[] execString) {
            super();
            command = execString;
        }

        @Override
        protected String[] getExecString() {
            return command;
        }

        @Override
        protected void parseExecResult(BufferedReader lines) throws IOException {
            reply = new StringBuffer();
            char[] buf = new char[512];
            int nRead;
            while ((nRead = lines.read(buf, 0, buf.length)) > 0) {
                reply.append(buf, 0, nRead);
            }
        }

        String getReply() {
            return (reply == null) ? "" : reply.toString();
        }
    }

    /**
     * 执行命令行程序，如果在1分钟内还没有执行完毕，则超时返回
     */
    public static String execCommand(final String[] cmd) throws IOException {
        return execCommand(cmd, 60);
    }

    public static String execCommand(final String[] cmd, long waitSeconds) throws IOException {
        String reply = "ERROR";

        final FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                SimpleCommandExecutor exec = new SimpleCommandExecutor(cmd);
                exec.run();
                return exec.getReply();
            }
        });

        final Thread thread = new Thread(task);
        try {
            thread.run();
            reply = task.get(waitSeconds, TimeUnit.SECONDS); // timeout in N ms
        } catch (ExecutionException e) {
            e.printStackTrace();
            LOG.error("exec exception", e);
            reply = "ERROR:" + e.getMessage();
        } catch (InterruptedException e) {
            LOG.error("interrupt", e);
            reply = "ERROR:" + e.getMessage();
        } catch (TimeoutException e) {
            LOG.error("timeout", e);
            thread.interrupt();
            task.cancel(true);
            reply = "ERROR: timeout! " + e.getMessage();
        }

        return reply;
    }

    /**
     * 判断命令是否存在
     * 
     * @param command
     * @return
     */
    public static boolean existCommand(String command) {
        try {
            String out = execCommand(new String[] { "which", command });
            return (out != null && out.startsWith("/"));
        } catch (IOException e) {
            return false;
        }
    }
}
