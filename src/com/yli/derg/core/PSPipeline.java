package com.yli.derg.core;

import org.apache.commons.lang.StringUtils;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.internal.AbstractDefinitionStmt;
import soot.toolkits.scalar.LocalDefs;
import soot.toolkits.scalar.LocalUses;
import soot.toolkits.scalar.UnitValueBoxPair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by yuanchun on 04/03/17.
 * Package: DERG
 */
public class PSPipeline extends PSFunction {

    protected List<PSPipeline> nextPipelines;

    public PSPipeline(InvokeExpr invokeExpr, Unit hostUnit, SootMethod hostMethod, Body hostBody, LocalDefs localDefs, LocalUses localUses) {
        super(invokeExpr, hostUnit, hostMethod, hostBody, localDefs, localUses);
        this.nextPipelines = new ArrayList<>();
        this.findNextPipelines();
    }

    private void findNextPipelines() {
        List<UnitValueBoxPair> uses = this.localUses.getUsesOf(this.hostUnit);
        for (UnitValueBoxPair unitValueBoxPair : uses) {
            Unit useUnit = unitValueBoxPair.getUnit();

            // Find next InvokeExpr
            InvokeExpr invokeExpr = null;
            if (useUnit instanceof AbstractDefinitionStmt) {
                Value rightOp = ((AbstractDefinitionStmt) useUnit).getRightOp();
                if (rightOp instanceof InvokeExpr) {
                    invokeExpr = (InvokeExpr) rightOp;
                }
            }
            else if (useUnit instanceof InvokeStmt) {
                invokeExpr = ((InvokeStmt) useUnit).getInvokeExpr();
            }

            if (invokeExpr != null && invokeExpr.getMethod().getDeclaringClass().getShortName().contains("ItemStream")) {
                this.nextPipelines.add(new PSPipeline(invokeExpr, useUnit, hostMethod, hostBody, localDefs, localUses));
                continue;
            }
            this.nextPipelines.add(null);

        }
    }

    public String toString(int indent) {
        String thisIndent = StringUtils.repeat(" ", indent);
        String result = thisIndent + super.toString();
        for (PSPipeline psPipeline : this.nextPipelines) {
            result += "\n";
            if (psPipeline == null) result += StringUtils.repeat(" ", indent + 2) + "<unknown>";
            else result += psPipeline.toString(indent + 2);
        }
        return result;
    }

    public String toString() {
        return String.format("PrivacyStreams DFG in method %s\n%s", this.hostMethod, this.toString(2));
    }
}
