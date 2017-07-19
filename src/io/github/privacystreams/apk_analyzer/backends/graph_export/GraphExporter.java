package io.github.privacystreams.apk_analyzer.backends.graph_export;

import io.github.privacystreams.apk_analyzer.Config;
import io.github.privacystreams.apk_analyzer.backends.DERGBackend;
import io.github.privacystreams.apk_analyzer.core.Graph;
import heros.utilities.JsonArray;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

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
        this.dumpPSDFG(g);
        this.dumpPSLibSig(g);
        this.dumpAndroidAPIUsed(g);
    }

    private void dumpPSDFG(Graph g) {
        String export_file_name = String.format("%s/psDFG.json", Config.outputDir);
        File export_file = new File(export_file_name);
        try {
            FileUtils.writeStringToFile(export_file, g.toJson().toString(2), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void dumpPSLibSig(Graph g) {
        String export_file_name = String.format("%s/psLibSig.json", Config.outputDir);
        File export_file = new File(export_file_name);
        try {
            FileUtils.writeStringToFile(export_file, new JSONObject(g.psMethod2Sig).toString(2), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void dumpAndroidAPIUsed(Graph g) {
        String export_file_name = String.format("%s/androidAPIUsed.txt", Config.outputDir);
        File export_file = new File(export_file_name);
        try {
            FileUtils.writeStringToFile(export_file, StringUtils.join(g.androidApiUsed, "\n"), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
