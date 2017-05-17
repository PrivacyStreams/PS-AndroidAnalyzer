package com.github.privacystreams.apk_analyzer.backends.desc_gen;

import com.github.privacystreams.apk_analyzer.Config;
import com.github.privacystreams.apk_analyzer.backends.DERGBackend;
import com.github.privacystreams.apk_analyzer.core.Graph;
import com.github.privacystreams.apk_analyzer.core.PSPipeline;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by yuanchun on 5/9/17.
 */
public class PrivDescGenerator extends DERGBackend {
    public static final String NAME = "desc_gen";
    public static final String DESCRIPTION = "Generate a privacy description.";

    @Override
    public void run(Graph g) {
        if (g.pipelines == null) return;
        for (PSPipeline pipeline : g.pipelines) {
            pipeline.toString();
        }
    }
}
