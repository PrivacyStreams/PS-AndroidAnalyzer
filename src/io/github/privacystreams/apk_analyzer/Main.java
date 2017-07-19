package io.github.privacystreams.apk_analyzer;

import io.github.privacystreams.apk_analyzer.core.Graph;

public class Main {

    public static void main(String[] args) {
	// write your code here
        Config.init();
        if (!Config.parseArgs(args)) {
            return;
        }

        Graph g = Config.dergFrontend.build();
        Config.dergBackend.run(g);
    }
}
