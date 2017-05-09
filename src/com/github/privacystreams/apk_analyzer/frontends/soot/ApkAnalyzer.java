package com.github.privacystreams.apk_analyzer.frontends.soot;

import com.github.privacystreams.apk_analyzer.Config;
import com.github.privacystreams.apk_analyzer.Const;
import com.github.privacystreams.apk_analyzer.core.Edge;
import com.github.privacystreams.apk_analyzer.core.Graph;
import com.github.privacystreams.apk_analyzer.core.Node;
import com.github.privacystreams.apk_analyzer.core.PSPipeline;
import com.github.privacystreams.apk_analyzer.frontends.DERGFrontend;
import com.github.privacystreams.apk_analyzer.utils.IgnoreUnknownTokenParser;
import com.github.privacystreams.apk_analyzer.utils.Util;
import org.apache.commons.cli.*;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.AbstractDefinitionStmt;
import soot.jimple.internal.JAssignStmt;
import soot.options.Options;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.*;

import java.io.File;
import java.io.FileFilter;
import java.util.*;

/**
 * Created by liyc on 12/23/15.
 * build the DERG of an Android application
 */
public class ApkAnalyzer extends DERGFrontend {
    public static final String NAME = "apk";
    public static final String DESCRIPTION = "Build PrivacyStreams DFG from .apk file.";

    private ArrayList<SootClass> applicationClasses;

    // File path of android.jar which is forced to use by soot
    private String forceAndroidJarPath = "";
    // Libraries' directory, to be added to soot classpath
    private String librariesDir = "";

    public void parseArgs(String[] args) throws ParseException {
        org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();
        Option library = Option.builder("l").argName("directory")
                .longOpt("library").hasArg().desc("path to library dir").build();
        Option sdk = Option.builder("sdk").argName("android.jar")
                .longOpt("android-sdk").hasArg().desc("path to android.jar").build();
        Option help_opt = Option.builder("h").desc("print this help message")
                .longOpt("help").build();

        options.addOption(library);
        options.addOption(sdk);
        options.addOption(help_opt);

        CommandLineParser parser = new IgnoreUnknownTokenParser();

        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption('l')) {
                librariesDir = cmd.getOptionValue('l');
                File lib = new File(librariesDir);
                if (!lib.exists()) {
                    throw new ParseException("Library does not exist.");
                }
                if (lib.isFile() && !lib.getName().endsWith(".jar")) {
                    throw new ParseException("Library format error, should be directory or jar.");
                }
            }
            if (cmd.hasOption("sdk")) {
                forceAndroidJarPath = cmd.getOptionValue("sdk");
                File sdkFile = new File(forceAndroidJarPath);
                if (!sdkFile.exists()) {
                    throw new ParseException("Android jar does not exist.");
                }
            }
            if (cmd.hasOption("h")) {
                throw new ParseException("print help message.");
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.setOptionComparator(new Comparator<Option>() {
                @Override
                public int compare(Option o1, Option o2) {
                    return o1.getOpt().length() - o2.getOpt().length();
                }
            });
            formatter.printHelp(ApkAnalyzer.NAME, options, true);
            throw new ParseException("Parsing arguments failed in " + ApkAnalyzer.NAME);
        }
    }

    private boolean init() {
        Util.LOGGER.info("Start Initializing " + ApkAnalyzer.NAME);
        Options.v().set_debug(false);
        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_output_dir(Config.outputDir);

        List<String> process_dirs = new ArrayList<>();
        process_dirs.add(Config.inputDirOrFile);
        Options.v().set_process_dir(process_dirs);

        if (Config.inputDirOrFile.endsWith(".apk")) {
            Options.v().set_src_prec(Options.src_prec_apk);
            Options.v().set_output_format(Options.output_format_dex);
        }
        else if (Config.inputDirOrFile.endsWith(".jar")) {
            Options.v().set_src_prec(Options.src_prec_class);
            Options.v().set_output_jar(true);
        }
        else {
            Options.v().set_src_prec(Options.src_prec_java);
            Options.v().set_output_format(Options.output_format_jimple);
        }

        String classpath = "";
        if (this.librariesDir != null && this.librariesDir.length() != 0) {
            File lib = new File(this.librariesDir);
            if (lib.isFile() && lib.getName().endsWith(".jar"))
                classpath = lib.getAbsolutePath();
            else if (lib.isDirectory()) {
                FileFilter fileFilter = new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.getName().endsWith(".jar");
                    }
                };
                for (File file : lib.listFiles(fileFilter)) {
                    classpath += file.getAbsolutePath() + ";";
                }
            }
            Options.v().set_soot_classpath(classpath);
        }

        Options.v().set_force_android_jar(this.forceAndroidJarPath);

        Scene.v().loadNecessaryClasses();

        applicationClasses = new ArrayList<>();
        for (SootClass cls : Scene.v().getApplicationClasses()) {
            applicationClasses.add(cls);
        }
        Collections.sort(applicationClasses, new Comparator<SootClass>() {
            @Override
            public int compare(SootClass o1, SootClass o2) {
                return String.CASE_INSENSITIVE_ORDER.compare(
                        o1.getName(), o2.getName());
            }
        });
        Util.LOGGER.info("Finish Initializing " + ApkAnalyzer.NAME);
        return true;
    }

    public void addAPICallRelations(Graph g, SootMethod method) {
        // consider the scope inside a method
        if (method.getSource() == null) return;
        Node v_method = getMethodNode(g, method);

        Node v_root = g.genDefaultRoot();
        g.createEdge(v_root, v_method, Edge.TYPE_CONTAINS);

        try {
            Body body = method.retrieveActiveBody();

            // add reference relation
            for (ValueBox valueBox : body.getUseAndDefBoxes()) {
                Value value = valueBox.getValue();
                if (value instanceof InvokeExpr) {
                    SootMethod invokedMethod = ((InvokeExpr) value).getMethod();
                    SootClass invokedClass = invokedMethod.getDeclaringClass();
                    if (invokedClass == null || invokedClass.isApplicationClass()) continue;

                    List<Value> arguments = ((InvokeExpr) value).getArgs();
                    Node v_api = getAPINode(g, invokedMethod, arguments);

                    g.createEdge(v_method, v_api, Edge.TYPE_REFER);

                    System.out.println(v_api.name);
                }
            }
        } catch (Exception e) {
            Util.logException(e);
        }
    }

    public static Node getAPINode(Graph g, SootMethod apiMethod, List<Value> parameters) {
        String methodStr = apiMethod.getSignature();
        for (Value parameter : parameters) {
            methodStr += "-----" + parameter.toString();
        }

        Node result = g.getNodeOrCreate(apiMethod, methodStr, Node.TYPE_API);
        result.sig = apiMethod.getSignature();
        return result;
    }

    public static Node getPackageNode(Graph g, PackageNode pkgNode) {
        return g.getNodeOrCreate(pkgNode, pkgNode.getSegName(), Node.TYPE_PACKAGE);
    }

    public static Node getClassNode(Graph g, SootClass cls) {
        Node result = g.getNodeOrCreate(cls, cls.getShortName(), Node.TYPE_CLASS);
        result.sig = cls.getName();
        return result;
    }

    public static Node getMethodNode(Graph g, SootMethod method) {
        Node result = g.getNodeOrCreate(method, method.getName(), Node.TYPE_METHOD);
        result.sig = method.getSignature();
        return result;
    }

    public static Node getFieldNode(Graph g, SootField field) {
        Node result = g.getNodeOrCreate(field, field.getName(), Node.TYPE_FIELD);
        result.sig = field.getSignature();
        return result;
    }

    public static Node getTypeNode(Graph g, Type type) {
        if (type instanceof RefType) {
            RefType refType = (RefType) type;
            return getClassNode(g, refType.getSootClass());
        }
        return g.getNodeOrCreate(type, type.toString(), Node.TYPE_TYPE);
    }

    public static Node getConstNode(Graph g, Constant con) {
        return g.getNodeOrCreate(con, con.toString(), Node.TYPE_CONST);
    }

    SootMethod getMStreamAPI;
    SootMethod getSStreamAPI;

    private Set<SootMethod> findPendingMethods() {
        Set<SootMethod> pendingMethods = new HashSet<>();

        for (SootClass cls : this.applicationClasses) {
            // Skip library package
            if (cls.getPackageName().startsWith(Const.psPackage)) continue;
            List<SootMethod> methods = new ArrayList<>();
            for (SootMethod method : cls.getMethods()) {
                methods.add(method);
            }
            for (SootMethod method : methods) {
                if (method.getSource() == null) continue;
                try {
                    Body body = method.retrieveActiveBody();
                    for (Unit u : body.getUnits()) {
                        // add reference relation
                        if (!(u instanceof AbstractDefinitionStmt)) continue;
                        AbstractDefinitionStmt s = (AbstractDefinitionStmt) u;
                        Value s_rOp = s.getRightOp();
                        Value s_lOp = s.getLeftOp();
                        if (s_rOp instanceof VirtualInvokeExpr) {
                            SootMethod invokedMethod = ((VirtualInvokeExpr) s_rOp).getMethod();
                            if (invokedMethod == getMStreamAPI || invokedMethod == getSStreamAPI) {
                                pendingMethods.add(method);
                                break;
                            }
                        }
                    }
                }
                catch (Exception e) {
                    Util.logException(e);
                }
            }
        }
        return pendingMethods;
    }

    private List<PSPipeline> findDFGInMethod(Graph g, SootMethod method) {
        List<PSPipeline> pipelines = new ArrayList<>();

        Node v_root = g.genDefaultRoot();
        Node v_method = getMethodNode(g, method);
        g.createEdge(v_root, v_method, Edge.TYPE_CONTAINS);

        Body body = method.retrieveActiveBody();
        BriefUnitGraph ug = new BriefUnitGraph(body);
        SimpleLocalDefs localDefs = new SimpleLocalDefs(ug);
        SimpleLocalUses localUses = new SimpleLocalUses(body, localDefs);

        for (Unit u : body.getUnits()) {
            // add reference relation
            if (!(u instanceof AbstractDefinitionStmt)) continue;
            AbstractDefinitionStmt s = (AbstractDefinitionStmt) u;
            Value s_rOp = s.getRightOp();
            if (s_rOp instanceof VirtualInvokeExpr) {
                InvokeExpr invokeExpr = (InvokeExpr) s_rOp;
                SootMethod invokedMethod = invokeExpr.getMethod();
                if (invokedMethod == getMStreamAPI || invokedMethod == getSStreamAPI) {
                    PSPipeline pipeline = new PSPipeline(invokeExpr, u, method, body, localDefs, localUses);
                    pipelines.add(pipeline);
                }
            }
        }

        return pipelines;

    }

    public Graph build() {
        this.init();
//        PackManager.v().runPacks();
        Util.LOGGER.info("generating PrivacyStreams DFG.");

        Graph g = new Graph();
        g.genDefaultRoot();

        SootClass uqiClass = Scene.v().tryLoadClass(Const.uqiClass, SootClass.SIGNATURES);
        if (uqiClass == null) {
            Util.LOGGER.info("This is not a PrivacyStreams app.");
            return g;
        }

        getMStreamAPI = Scene.v().getMethod(Const.uqiGetMStreamAPI);
        getSStreamAPI = Scene.v().getMethod(Const.uqiGetSStreamAPI);

        Set<SootMethod> pendingMethods = this.findPendingMethods();

        List<PSPipeline> pipelines = new ArrayList<>();

        for (SootMethod method : pendingMethods) {
            pipelines.addAll(this.findDFGInMethod(g, method));
        }

        for (PSPipeline psPipeline : pipelines) {
            System.out.println(psPipeline);
        }

        Util.LOGGER.info("finished building PrivacyStreams DFG");
        return g;
    }
}
