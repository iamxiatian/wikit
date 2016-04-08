package ruc.irm.wikit.nlp.segment;

import ruc.irm.wikit.common.conf.Conf;

public final class SegmentFactory {
    private static Segment segment = null;

    /**
     * 分词处理的参数可以通过conf传递
     *
     * @param conf
     * @return
     * @throws SegmentException
     */
    public static final Segment getSegment(Conf conf) {
        return getHanSegment(conf);
    }


    private static final Segment getHanSegment(Conf conf) {
        if (segment == null) {
            segment = new HanSegment(conf);
        } else {
            segment.setConfiguration(conf);
        }

        return segment;
    }

}
