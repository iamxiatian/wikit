package ruc.irm.wikit.web;

import ruc.irm.wikit.common.conf.Conf;

/**
 * @author Tian Xia
 * @date Feb 13, 2016 9:52 AM
 */
public class WebContex {

    private Conf conf = null;

    private static WebContex instance = null;

    public static WebContex getInstance() {
        if (instance == null) {
            instance = new WebContex();
        }
        return instance;
    }

    private WebContex(){}

    public Conf getConf() {
        return conf;
    }

    public void setConf(Conf conf) {
        this.conf = conf;
    }

}
