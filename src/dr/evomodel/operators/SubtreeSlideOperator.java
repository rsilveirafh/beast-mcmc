/*
 * SubtreeSlideOperator.java
 *
 * Copyright (c) 2002-2015 Alexei Drummond, Andrew Rambaut and Marc Suchard
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package dr.evomodel.operators;

import dr.evolution.tree.MutableTree;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evomodel.tree.TreeModel;
import dr.evomodelxml.operators.SubtreeSlideOperatorParser;
import dr.inference.operators.*;
import dr.math.MathUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the subtree slide move.
 *
 * @author Alexei Drummond
 * @version $Id: SubtreeSlideOperator.java,v 1.15 2005/06/14 10:40:34 rambaut Exp $
 */
public class SubtreeSlideOperator extends AbstractTreeOperator implements CoercableMCMCOperator {

    private TreeModel tree = null;
    private double size = 1.0;
    private boolean gaussian = false;
    private final boolean swapInRandomRate;
    private final boolean swapInRandomTrait;
    private final boolean scaledDirichletBranches;
    private CoercionMode mode = CoercionMode.DEFAULT;

    public SubtreeSlideOperator(TreeModel tree, double weight, double size, boolean gaussian,
                                boolean swapRates, boolean swapTraits, boolean scaleDirichletBranches, CoercionMode mode) {
        this.tree = tree;
        setWeight(weight);

        if (size == 0.0) {
            double b = 0.0;
            for (int k = 0; k < tree.getNodeCount(); ++k) {
                b += tree.getBranchLength(tree.getNode(k));
            }
            size = b / (2 * tree.getNodeCount());
        }

        this.size = size;
        this.gaussian = gaussian;
        this.swapInRandomRate = swapRates;
        this.swapInRandomTrait = swapTraits;
        this.scaledDirichletBranches = scaleDirichletBranches;

        this.mode = mode;
    }

    /**
     * Do a probablistic subtree slide move.
     *
     * @return the log-transformed hastings ratio
     */
    public double doOperation() throws OperatorFailedException {

        double logq;

        final NodeRef root = tree.getRoot();
        final double oldTreeHeight = tree.getNodeHeight(root);

        NodeRef i;

        // 1. choose a random node avoiding root
        do {
            i = tree.getNode(MathUtils.nextInt(tree.getNodeCount()));
        } while (root == i);

        final NodeRef iP = tree.getParent(i);
        final NodeRef CiP = getOtherChild(tree, iP, i);
        final NodeRef PiP = tree.getParent(iP);

        // 2. choose a delta to move
        final double delta = getDelta();
        final double oldHeight = tree.getNodeHeight(iP);
        final double newHeight = oldHeight + delta;

        // 3. if the move is up
        if (delta > 0) {

            // 3.1 if the topology will change
            if (PiP != null && tree.getNodeHeight(PiP) < newHeight) {
                // find new parent
                NodeRef newParent = PiP;
                NodeRef newChild = iP;
                while (tree.getNodeHeight(newParent) < newHeight) {
                    newChild = newParent;
                    newParent = tree.getParent(newParent);
                    if (newParent == null) break;
                }

                tree.beginTreeEdit();

                // 3.1.1 if creating a new root
                if (tree.isRoot(newChild)) {
                    tree.removeChild(iP, CiP);
                    tree.removeChild(PiP, iP);
                    tree.addChild(iP, newChild);
                    tree.addChild(PiP, CiP);
                    tree.setRoot(iP);
                    //System.err.println("Creating new root!");

                    if (tree.hasNodeTraits()) {
                        // **********************************************
                        // swap traits and rates so that root keeps it trait and rate values
                        // **********************************************

                        tree.swapAllTraits(newChild, iP);

                    }

                    if (tree.hasRates()) {
                        final double rootNodeRate = tree.getNodeRate(newChild);
                        tree.setNodeRate(newChild, tree.getNodeRate(iP));
                        tree.setNodeRate(iP, rootNodeRate);
                    }

                    // **********************************************

                }
                // 3.1.2 no new root
                else {
                    tree.removeChild(iP, CiP);
                    tree.removeChild(PiP, iP);
                    tree.removeChild(newParent, newChild);
                    tree.addChild(iP, newChild);
                    tree.addChild(PiP, CiP);
                    tree.addChild(newParent, iP);
                    //System.err.println("No new root!");
                }

                tree.setNodeHeight(iP, newHeight);

                tree.endTreeEdit();

                // 3.1.3 count the hypothetical sources of this destination.
                final int possibleSources = intersectingEdges(tree, newChild, oldHeight, null);
                //System.out.println("possible sources = " + possibleSources);

                logq = -Math.log(possibleSources);

            } else {
                // just change the node height
                tree.setNodeHeight(iP, newHeight);
                logq = 0.0;
            }
        }
        // 4 if we are sliding the subtree down.
        else {

            // 4.0 is it a valid move?
            if (tree.getNodeHeight(i) > newHeight) {
                return Double.NEGATIVE_INFINITY;
            }

            // 4.1 will the move change the topology
            if (tree.getNodeHeight(CiP) > newHeight) {

                List<NodeRef> newChildren = new ArrayList<NodeRef>();
                final int possibleDestinations = intersectingEdges(tree, CiP, newHeight, newChildren);

                // if no valid destinations then return a failure
                if (newChildren.size() == 0) {
                    return Double.NEGATIVE_INFINITY;
                }

                // pick a random parent/child destination edge uniformly from options
                final int childIndex = MathUtils.nextInt(newChildren.size());
                NodeRef newChild = newChildren.get(childIndex);
                NodeRef newParent = tree.getParent(newChild);

                tree.beginTreeEdit();

                // 4.1.1 if iP was root
                if (tree.isRoot(iP)) {
                    // new root is CiP
                    tree.removeChild(iP, CiP);
                    tree.removeChild(newParent, newChild);
                    tree.addChild(iP, newChild);
                    tree.addChild(newParent, iP);
                    tree.setRoot(CiP);

                    if (tree.hasNodeTraits()) {
                        // **********************************************
                        // swap traits and rates, so that root keeps it trait and rate values
                        // **********************************************

                        tree.swapAllTraits(iP, CiP);

                    }

                    if (tree.hasRates()) {
                        final double rootNodeRate = tree.getNodeRate(iP);
                        tree.setNodeRate(iP, tree.getNodeRate(CiP));
                        tree.setNodeRate(CiP, rootNodeRate);
                    }

                    // **********************************************

                    //System.err.println("DOWN: Creating new root!");
                } else {
                    tree.removeChild(iP, CiP);
                    tree.removeChild(PiP, iP);
                    tree.removeChild(newParent, newChild);
                    tree.addChild(iP, newChild);
                    tree.addChild(PiP, CiP);
                    tree.addChild(newParent, iP);
                    //System.err.println("DOWN: no new root!");
                }

                tree.setNodeHeight(iP, newHeight);

                tree.endTreeEdit();

                logq = Math.log(possibleDestinations);
            } else {
                tree.setNodeHeight(iP, newHeight);
                logq = 0.0;
            }
        }

        if (swapInRandomRate) {
            final NodeRef j = tree.getNode(MathUtils.nextInt(tree.getNodeCount()));
            if (j != i) {
                final double tmp = tree.getNodeRate(i);
                tree.setNodeRate(i, tree.getNodeRate(j));
                tree.setNodeRate(j, tmp);
            }

        }

        if (swapInRandomTrait) {
            final NodeRef j = tree.getNode(MathUtils.nextInt(tree.getNodeCount()));
            if (j != i) {

                tree.swapAllTraits(i, j);

//                final double tmp = tree.getNodeTrait(i, TRAIT);
//                tree.setNodeTrait(i, TRAIT, tree.getNodeTrait(j, TRAIT));
//                tree.setNodeTrait(j, TRAIT, tmp);
            }

        }

        if (logq == Double.NEGATIVE_INFINITY) throw new OperatorFailedException("invalid slide");

        if (scaledDirichletBranches) {
            if (oldTreeHeight != tree.getNodeHeight(tree.getRoot()))
                throw new OperatorFailedException("Temporarily disabled."); // TODO calculate Hastings ratio
        }

        return logq;
    }

    private double getDelta() {
        if (!gaussian) {
            return (MathUtils.nextDouble() * size) - (size / 2.0);
        } else {
            return MathUtils.nextGaussian() * size;
        }
    }

    private int intersectingEdges(Tree tree, NodeRef node, double height, List<NodeRef> directChildren) {

        final NodeRef parent = tree.getParent(node);

        if (tree.getNodeHeight(parent) < height) return 0;

        if (tree.getNodeHeight(node) < height) {
            if (directChildren != null) directChildren.add(node);
            return 1;
        }

        int count = 0;
        for (int i = 0; i < tree.getChildCount(node); i++) {
            count += intersectingEdges(tree, tree.getChild(node, i), height, directChildren);
        }
        return count;
    }

    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        this.size = size;
    }

    public double getCoercableParameter() {
        return Math.log(getSize());
    }

    public void setCoercableParameter(double value) {
        setSize(Math.exp(value));
    }

    public double getRawParameter() {
        return getSize();
    }

    public CoercionMode getMode() {
        return mode;
    }

    public double getTargetAcceptanceProbability() {
        return 0.234;
    }


    public String getPerformanceSuggestion() {
        double prob = MCMCOperator.Utils.getAcceptanceProbability(this);
        double targetProb = getTargetAcceptanceProbability();

        double ws = OperatorUtils.optimizeWindowSize(getSize(), Double.MAX_VALUE, prob, targetProb);

        if (prob < getMinimumGoodAcceptanceLevel()) {
            return "Try decreasing size to about " + ws;
        } else if (prob > getMaximumGoodAcceptanceLevel()) {
            return "Try increasing size to about " + ws;
        } else return "";
    }

    public String getOperatorName() {
        return SubtreeSlideOperatorParser.SUBTREE_SLIDE + "(" + tree.getId() + ")";
    }

}
