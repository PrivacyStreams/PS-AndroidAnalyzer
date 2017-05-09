package com.github.privacystreams.apk_analyzer.backends.desc_gen;

import com.github.privacystreams.apk_analyzer.Config;
import com.github.privacystreams.apk_analyzer.backends.DERGBackend;
import com.github.privacystreams.apk_analyzer.core.Graph;
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
        String export_file_name = String.format("%s/derg.json", Config.outputDir);
        File export_file = new File(export_file_name);
        try {
            FileUtils.writeStringToFile(export_file, g.toJson().toString(2), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}