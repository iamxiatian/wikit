package ruc.irm.wikit.util;

public class ProgressCounter {

    private static final int THOUSAND = 1000;
    private static final int SMALL_STEP = 1 * THOUSAND;
    private static final int BIG_STEP = 50 * THOUSAND;
    private boolean showProgressive = true;

    private long count = 0;
    private long maxCount = Integer.MAX_VALUE;

    public ProgressCounter() {

    }

    public ProgressCounter(boolean showProgressive) {
        this.showProgressive = showProgressive;
    }

    public ProgressCounter(long maxCount) {
        this.maxCount = maxCount;
    }

    public long getCount() {
        return count;
    }

    public void setShowProgress(boolean show) {
        showProgressive = show;
    }

    public void setMaxCount(long maxCount) {
        this.maxCount = maxCount;
    }

    public void increment() {
        count++;
        if(!showProgressive) return;

        if (count % BIG_STEP == 0) {
            System.out.print(". " + count / THOUSAND + "k");
            if (maxCount < Integer.MAX_VALUE) {
                double percent = count/maxCount*100;
                System.out.printf(" %-5.2f%%", percent);
            }
            System.out.println();
        } else if (count % SMALL_STEP == 0) {
            System.out.print(".");
        }
    }

    public void done() {
        System.out.println("DONE!");
    }

}

