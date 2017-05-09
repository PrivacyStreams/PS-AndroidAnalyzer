package com.github.privacystreams.apk_analyzer.backends.graph_export;

import com.github.privacystreams.apk_analyzer.Config;
import com.github.privacystreams.apk_analyzer.backends.DERGBackend;
import com.github.privacystreams.apk_analyzer.core.Graph;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by liyc on 1/4/16.
 * export graph to file or database
 */
public class GraphExporter extends DERGBackend {
    public static final String NAME = "graph_export";
    public static final String DESCRIPTION = "export DERG to file.";

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
