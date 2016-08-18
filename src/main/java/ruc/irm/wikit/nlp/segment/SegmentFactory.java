package ruc.irm.wikit.nlp.segment;

import ruc.irm.wikit.common.conf.Conf;

public final class SegmentFactory {
    private static Segment hanSegment = null;
    private static Segment ansjSegment = null;

    /**
     * 分词处理的参数可以通过conf传递
     *
     * @param conf
     * @return
     * @throws SegmentException
     */
    public static final Segment getSegment(Conf conf) {
        if(conf==null || conf.get("segment.name", "ansj").equalsIgnoreCase("ansj")) {
            return getAnsjSegment(conf);
        } else {
            return getHanSegment(conf);
        }
    }


    private static final Segment getHanSegment(Conf conf) {
        if (hanSegment == null) {
            hanSegment = new HanSegment(conf);
        } else {
            hanSegment.setConfiguration(conf);
        }

        return hanSegment;
    }


    private static final Segment getAnsjSegment(Conf conf) {
        if (ansjSegment == null) {
            ansjSegment = new AnsjSegment(conf);
        } else {
            ansjSegment.setConfiguration(conf);
        }

        return ansjSegment;
    }

}
