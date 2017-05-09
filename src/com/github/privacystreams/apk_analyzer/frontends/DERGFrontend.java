package com.github.privacystreams.apk_analyzer.frontends;

import com.github.privacystreams.apk_analyzer.core.Graph;
import com.github.privacystreams.apk_analyzer.frontends.soot.ApkAnalyzer;
import com.github.privacystreams.apk_analyzer.frontends.json.JSONBuilder;
import com.github.privacystreams.apk_analyzer.utils.Util;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;

/**
 * Created by liyc on 12/23/15.
 * superclass of frontends
 */
public abstract class DERGFrontend {
    public static HashMap<String, DERGFrontend> availableFrontends = new HashMap<>();
    public static String defaultFrontend = "";
    public static void registerFrontends() {
        defaultFrontend = ApkAnalyzer.NAME;
//        availableFrontends.put(SootBuilder.NAME, new SootBuilder());
//        availableFrontends.put(SimpleSootBuilder.NAME, new SimpleSootBuilder());
        availableFrontends.put(ApkAnalyzer.NAME, new ApkAnalyzer());
        availableFrontends.put(JSONBuilder.NAME, new JSONBuilder());
    }
    public static String getAvailableFrontends() {
        return StringUtils.join(availableFrontends.keySet(), "/");
    }

    public abstract Graph build();

    public void parseArgs(String[] args) throws ParseException {}

    public static DERGFrontend getBuilder(String type) {
        if (type == null || type.length() == 0) {
            Util.LOGGER.warning(String.format("no frontend specified, using %s by default.", defaultFrontend));
            type = defaultFrontend;
        }
        if (availableFrontends.containsKey(type)) {
            return availableFrontends.get(type);
        }
        return null;
    }

}
