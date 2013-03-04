package dr.app.bss;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import dr.app.beagle.tools.parsers.BeagleSequenceSimulatorParser;
import dr.app.beagle.tools.parsers.PartitionParser;
import dr.app.beauti.util.XMLWriter;
import dr.evolution.datatype.DataType;
import dr.evolution.tree.Tree;
import dr.evolution.util.Taxon;
import dr.evolution.util.TaxonList;
import dr.evomodel.sitemodel.SiteModel;
import dr.evomodel.substmodel.NucModelType;
import dr.evomodel.tree.TreeModel;
import dr.evomodelxml.branchratemodel.StrictClockBranchRatesParser;
import dr.evomodelxml.coalescent.CoalescentSimulatorParser;
import dr.evomodelxml.sitemodel.GammaSiteModelParser;
import dr.evomodelxml.substmodel.FrequencyModelParser;
import dr.evomodelxml.substmodel.GTRParser;
import dr.evomodelxml.substmodel.HKYParser;
import dr.evomodelxml.substmodel.TN93Parser;
import dr.evomodelxml.substmodel.YangCodonModelParser;
import dr.evomodelxml.tree.TreeModelParser;
import dr.evoxml.NewickParser;
import dr.evoxml.TaxaParser;
import dr.evoxml.TaxonParser;
import dr.inference.model.ParameterParser;
import dr.util.Attribute;
import dr.xml.Report;
import dr.xml.XMLParser;

/**
 * @author Filip Bielejec
 * @version $Id$
 */
public class XMLGenerator {

	public static final String STARTING_TREE = "startingTree";
	
	private PartitionDataList dataList;

	public XMLGenerator(PartitionDataList dataList) {

		this.dataList = dataList;

	}// END: Constructor

	public void generateXML(File file) throws IOException {

		XMLWriter writer = new XMLWriter(new BufferedWriter(
				new FileWriter(file)));

		// //////////////
		// ---header---//
		// //////////////

		writer.writeText("<?xml version=\"1.0\" standalone=\"yes\"?>");
		writer.writeComment("Generated by "
				+ BeagleSequenceSimulatorApp.BEAGLE_SEQUENCE_SIMULATOR + " "
				+ BeagleSequenceSimulatorApp.VERSION);

		writer.writeOpenTag("beast");
		writer.writeBlankLine();

		// ////////////////////
		// ---taxa element---//
		// ////////////////////

		try {

			writeTaxa(dataList.taxonList, writer);
			writer.writeBlankLine();

		} catch (Exception e) {

			throw new RuntimeException("Taxon list generation has failed:\n"
					+ e.getMessage());

		}// END: try-catch block

		// /////////////////////////////
		// ---starting tree element---//
		// /////////////////////////////

		try {

			int suffix = 1;
			ArrayList<TreeModel> treeModelList = new ArrayList<TreeModel>();
			for (PartitionData data : dataList) {

				if (data.treeModel == null) {

					throw new RuntimeException(
							"Set Tree Model in Partitions tab for " + suffix
									+ " partition.");

				} else {

					TreeModel treeModel = data.treeModel;

					if (treeModelList.size() == 0 | !Utils.isTreeModelInList(treeModel, treeModelList)) {

						data.treeModelIdref += suffix;

						writeStartingTree(treeModel, writer,
								String.valueOf(suffix));
						writer.writeBlankLine();

						treeModelList.add(treeModel);

					} else {

						int index = Utils.treeModelIsIdenticalWith(treeModel,
								treeModelList) + 1;
						data.treeModelIdref += index;

					}

				}// END: exception

				suffix++;

			}// END: partition loop

		} catch (Exception e) {

			throw new RuntimeException("Starting tree generation has failed:\n"
					+ e.getMessage());

		}// END: try-catch block

		// //////////////////////////
		// ---tree model element---//
		// //////////////////////////

		try {

			int suffix = 1;
			ArrayList<TreeModel> treeModelList = new ArrayList<TreeModel>();
			for (PartitionData data : dataList) {

				TreeModel treeModel = data.treeModel;

				if (treeModelList.size() == 0 | !Utils.isTreeModelInList(treeModel, treeModelList)) {

					writeTreeModel(treeModel, writer, String.valueOf(suffix));
					writer.writeBlankLine();

					treeModelList.add(treeModel);

				}

				suffix++;

			}// END: partition loop

		} catch (Exception e) {

			throw new RuntimeException("Tree model generation has failed:\n"
					+ e.getMessage());

		}// END: try-catch block

		// //////////////////////////////////
		// ---branch rates model element---//
		// //////////////////////////////////

		try {

			int suffix = 1;
			ArrayList<PartitionData> partitionList = new ArrayList<PartitionData>();
			for (PartitionData data : dataList) {

				if (partitionList.size() == 0 | !Utils.isElementInList(data, partitionList, Utils.BRANCH_RATE_MODEL_ELEMENT)) {

					data.clockModelIdref += suffix;

					writeBranchRatesModel(data, writer, String.valueOf(suffix));
					writer.writeBlankLine();
					partitionList.add(data);

				} else {

					int index = Utils.isIdenticalWith(data, partitionList, Utils.BRANCH_RATE_MODEL_ELEMENT) + 1;
					data.clockModelIdref += index;

				}

				suffix++;

			}// END: partition loop

		} catch (Exception e) {

			throw new RuntimeException("Clock model generation has failed:\n"
					+ e.getMessage());

		}// END: try-catch block

		// ///////////////////////////////
		// ---frequency model element---//
		// ///////////////////////////////

		try {

			int suffix = 1;
			ArrayList<PartitionData> partitionList = new ArrayList<PartitionData>();
			for (PartitionData data : dataList) {

				if (partitionList.size() == 0 | !Utils.isElementInList(data, partitionList, Utils.FREQUENCY_MODEL_ELEMENT)) {

					data.frequencyModelIdref += suffix;

					writeFrequencyModel(data, writer);
					writer.writeBlankLine();
					partitionList.add(data);

				} else {

					int index = Utils.isIdenticalWith(data, partitionList, Utils.FREQUENCY_MODEL_ELEMENT) + 1;
					data.frequencyModelIdref += index;

				}

				suffix++;

			}// END: partition loop

		} catch (Exception e) {

			throw new RuntimeException(
					"Frequency model generation has failed:\n" + e.getMessage());

		}// END: try-catch block

		// ////////////////////////////
		// ---branch model element---//
		// ////////////////////////////

		try {

			int suffix = 1;
			ArrayList<PartitionData> partitionList = new ArrayList<PartitionData>();
			for (PartitionData data : dataList) {

				if (partitionList.size() == 0 | !Utils.isElementInList(data, partitionList, Utils.BRANCH_MODEL_ELEMENT)) {

					data.substitutionModelIdref += suffix;

					writeBranchModel(data, writer, String.valueOf(suffix));
					writer.writeBlankLine();
					partitionList.add(data);

				} else {

					int index = Utils.isIdenticalWith(data, partitionList, Utils.BRANCH_MODEL_ELEMENT) + 1;
					data.substitutionModelIdref += index;

				}

				suffix++;

			}// END: partition loop

		} catch (Exception e) {

			throw new RuntimeException("Branch model generation has failed:\n"
					+ e.getMessage());

		}// END: try-catch block

		// ///////////////////////////////
		// ---site rate model element---//
		// ///////////////////////////////

		try {

			int suffix = 1;
			ArrayList<PartitionData> partitionList = new ArrayList<PartitionData>();
			for (PartitionData data : dataList) {

				if (partitionList.size() == 0 | !Utils.isElementInList(data, partitionList, Utils.SITE_RATE_MODEL_ELEMENT)) {

					data.siteRateModelIdref += suffix;

					writeSiteRateModel(data, writer);
					writer.writeBlankLine();
					partitionList.add(data);

				} else {

					int index = Utils.isIdenticalWith(data, partitionList, Utils.SITE_RATE_MODEL_ELEMENT) + 1;
					data.siteRateModelIdref += index;

				}

				suffix++;

			}// END: partition loop

		} catch (Exception e) {

			System.err.println(e);
			throw new RuntimeException(
					"Site rate model generation has failed:\n" + e.getMessage());

		}// END: try-catch block

		// /////////////////////////////////////////
		// ---beagle sequence simulator element---//
		// /////////////////////////////////////////

		try {

			writeBeagleSequenceSimulator(writer);
			writer.writeBlankLine();

		} catch (Exception e) {

			throw new RuntimeException(
					"Beagle Sequence Simulator element generation has failed:\n"
							+ e.getMessage());

		}// END: try-catch block

		// //////////////////////
		// ---report element---//
		// //////////////////////

		try {

			writeReport(writer);
			writer.writeBlankLine();

		} catch (Exception e) {

			System.err.println(e);
			throw new RuntimeException(
					"Report element generation has failed:\n" + e.getMessage());

		}// END: try-catch block

		writer.writeCloseTag("beast");
		writer.flush();
		writer.close();
	}// END: generateXML

	private void writeBeagleSequenceSimulator(XMLWriter writer) {

		writer.writeOpenTag(
				BeagleSequenceSimulatorParser.BEAGLE_SEQUENCE_SIMULATOR,
				new Attribute[] {
						new Attribute.Default<String>(XMLParser.ID, "simulator"),
						new Attribute.Default<String>(
								BeagleSequenceSimulatorParser.SITE_COUNT,
								String.valueOf(dataList.siteCount)) });

		for (PartitionData data : dataList) {

			// TODO: not always all three are needed
			writer.writeOpenTag(
					PartitionParser.PARTITION,
					new Attribute[] {
							new Attribute.Default<String>(PartitionParser.FROM,
									String.valueOf(data.from)),
							new Attribute.Default<String>(PartitionParser.TO,
									String.valueOf(data.to)),
							new Attribute.Default<String>(
									PartitionParser.EVERY, String
											.valueOf(data.every)) });

			writer.writeIDref(TreeModel.TREE_MODEL, data.treeModelIdref);

			int substitutionModelIndex = data.substitutionModelIndex;
			switch (substitutionModelIndex) {

			case 0: // HKY

				writer.writeIDref(NucModelType.HKY.getXMLName(),
						data.substitutionModelIdref);
				break;

			case 1: // GTR

				writer.writeIDref(GTRParser.GTR_MODEL,
						data.substitutionModelIdref);
				break;

			case 2: // TN93

				writer.writeIDref(NucModelType.TN93.getXMLName(),
						data.substitutionModelIdref);
				break;

			case 3: // Yang Codon Model

				writer.writeIDref(YangCodonModelParser.YANG_CODON_MODEL,
						data.substitutionModelIdref);
				break;

			}// END: switch

			writer.writeIDref(SiteModel.SITE_MODEL, data.siteRateModelIdref);

			int clockModel = data.clockModelIndex;
			switch (clockModel) {

			case 0: // StrictClock

				writer.writeIDref(
						StrictClockBranchRatesParser.STRICT_CLOCK_BRANCH_RATES,
						data.clockModelIdref);
				break;

			}// END: switch

			writer.writeIDref(FrequencyModelParser.FREQUENCY_MODEL,
					data.frequencyModelIdref);

			// TODO: ancestral sequence

			writer.writeCloseTag(PartitionParser.PARTITION);

		}// END: partitions loop

		writer.writeCloseTag(BeagleSequenceSimulatorParser.BEAGLE_SEQUENCE_SIMULATOR);

	}// END: writeBeagleSequenceSimulator

	private void writeBranchRatesModel(PartitionData data, XMLWriter writer,
			String suffix) {

		int clockModel = data.clockModelIndex;
		switch (clockModel) {

		case 0: // StrictClock

			writer.writeOpenTag(
					StrictClockBranchRatesParser.STRICT_CLOCK_BRANCH_RATES,
					new Attribute[] { new Attribute.Default<String>(
							XMLParser.ID, data.clockModelIdref) });

			writeParameter("rate", "clock.rate" + suffix, 1,
					String.valueOf(data.clockParameterValues[0]), null, null,
					writer);

			writer.writeCloseTag(StrictClockBranchRatesParser.STRICT_CLOCK_BRANCH_RATES);

			break;

		}// END: switch

	}// END: writeBranchRatesModel

	private void writeTaxa(TaxonList taxonList, XMLWriter writer) {

		writer.writeOpenTag(TaxaParser.TAXA, // tagname
				new Attribute[] { // attributes[]
				new Attribute.Default<String>(XMLParser.ID, TaxaParser.TAXA) });

		for (int i = 0; i < taxonList.getTaxonCount(); i++) {

			Taxon taxon = taxonList.getTaxon(i);

			writer.writeTag(
					TaxonParser.TAXON, // tagname
					new Attribute[] { // attributes[]
					new Attribute.Default<String>(XMLParser.ID, taxon.getId()) },
					true // close
			);

		}// END: i loop

		writer.writeCloseTag(TaxaParser.TAXA);
	}// END: writeTaxa

	private void writeTreeModel(TreeModel tree, XMLWriter writer, String suffix) {

		final String treeModelName = TreeModel.TREE_MODEL + suffix;

		writer.writeTag(TreeModel.TREE_MODEL, new Attribute.Default<String>(
				XMLParser.ID, treeModelName), false);

		writer.writeIDref("tree", STARTING_TREE
				+ suffix);

		writeParameter(TreeModelParser.ROOT_HEIGHT, treeModelName + "."
				+ CoalescentSimulatorParser.ROOT_HEIGHT, 1, null, null, null,
				writer);

		writer.writeOpenTag(TreeModelParser.NODE_HEIGHTS,
				new Attribute.Default<String>(TreeModelParser.INTERNAL_NODES,
						"true"));

		writeParameter(null, treeModelName + "." + "internalNodeHeights", 1,
				null, null, null, writer);

		writer.writeCloseTag(TreeModelParser.NODE_HEIGHTS);

		writer.writeOpenTag(TreeModelParser.NODE_HEIGHTS,
				new Attribute[] {
						new Attribute.Default<String>(
								TreeModelParser.INTERNAL_NODES, "true"),
						new Attribute.Default<String>(
								TreeModelParser.ROOT_NODE, "true") });

		writeParameter(null, treeModelName + "." + "allInternalNodeHeights", 1,
				null, null, null, writer);

		writer.writeCloseTag(TreeModelParser.NODE_HEIGHTS);

		writer.writeCloseTag(TreeModel.TREE_MODEL);

	}// END: writeTreeModel

	private void writeStartingTree(TreeModel tree, XMLWriter writer,
			String suffix) {

		writer.writeOpenTag(NewickParser.NEWICK,
				new Attribute[] { new Attribute.Default<String>(XMLParser.ID,
						STARTING_TREE + suffix) });

		writer.writeText(Tree.Utils.newick(tree));

		writer.writeCloseTag(NewickParser.NEWICK);

	}// END: writeStartingTree

	private void writeReport(XMLWriter writer) {

		writer.writeOpenTag(Report.REPORT,
				new Attribute[] { new Attribute.Default<String>(
						Report.FILENAME, "sequences.fasta") });

		writer.writeIDref(
				BeagleSequenceSimulatorParser.BEAGLE_SEQUENCE_SIMULATOR,
				"simulator");

		writer.writeCloseTag(Report.REPORT);

	}// END: writeReport

	private void writeSiteRateModel(PartitionData data, XMLWriter writer) {

		writer.writeOpenTag(SiteModel.SITE_MODEL,
				new Attribute[] { new Attribute.Default<String>(XMLParser.ID,
						data.siteRateModelIdref) });

		writer.writeOpenTag(GammaSiteModelParser.SUBSTITUTION_MODEL);

		int substitutionModelIndex = data.substitutionModelIndex;
		switch (substitutionModelIndex) {

		case 0: // HKY

			writer.writeIDref(NucModelType.HKY.getXMLName(),
					data.substitutionModelIdref);
			break;

		case 1: // GTR

			writer.writeIDref(GTRParser.GTR_MODEL, data.substitutionModelIdref);
			break;

		case 2: // TN93

			writer.writeIDref(NucModelType.TN93.getXMLName(),
					data.substitutionModelIdref);
			break;

		case 3: // Yang Codon Model

			writer.writeIDref(YangCodonModelParser.YANG_CODON_MODEL,
					data.substitutionModelIdref);
			break;

		}// END: switch

		writer.writeCloseTag(GammaSiteModelParser.SUBSTITUTION_MODEL);

		int siteRateModelIndex = data.siteRateModelIndex;
		switch (siteRateModelIndex) {

		case 0: // no model

			// do nothing

			break;

		case 1: // GammaSiteRateModel
			writer.writeOpenTag(
					GammaSiteModelParser.GAMMA_SHAPE,
					new Attribute.Default<String>(
							GammaSiteModelParser.GAMMA_CATEGORIES,
							String.valueOf(data.siteRateModelParameterValues[0])));

			writeParameter(null, "alpha", 1,
					String.valueOf(data.siteRateModelParameterValues[1]), null,
					null, writer);

			writer.writeCloseTag(GammaSiteModelParser.GAMMA_SHAPE);
			break;
		}// END: switch

		writer.writeCloseTag(SiteModel.SITE_MODEL);

	}// END: writeSiteModel

	private void writeBranchModel(PartitionData data, XMLWriter writer,
			String suffix) {

		int substitutionModelIndex = data.substitutionModelIndex;

		switch (substitutionModelIndex) {

		case 0: // HKY

			writer.writeOpenTag(NucModelType.HKY.getXMLName(),
					new Attribute[] { new Attribute.Default<String>(
							XMLParser.ID, data.substitutionModelIdref) });

			writer.writeOpenTag(FrequencyModelParser.FREQUENCIES);

			writer.writeIDref(FrequencyModelParser.FREQUENCY_MODEL,
					data.frequencyModelIdref);

			writer.writeCloseTag(FrequencyModelParser.FREQUENCIES);

			writeParameter(HKYParser.KAPPA, HKYParser.KAPPA + suffix, 1,
					String.valueOf(data.substitutionParameterValues[0]), null,
					null, writer);

			writer.writeCloseTag(NucModelType.HKY.getXMLName());

			break;

		case 1: // GTR

			writer.writeOpenTag(GTRParser.GTR_MODEL,
					new Attribute[] { new Attribute.Default<String>(
							XMLParser.ID, data.substitutionModelIdref) });

			writer.writeOpenTag(FrequencyModelParser.FREQUENCIES);

			writer.writeIDref(FrequencyModelParser.FREQUENCY_MODEL,
					data.frequencyModelIdref);

			writer.writeCloseTag(FrequencyModelParser.FREQUENCIES);

			writeParameter(GTRParser.A_TO_C, "ac" + suffix, 1,
					String.valueOf(data.substitutionParameterValues[1]), null,
					null, writer);
			writeParameter(GTRParser.A_TO_G, "ag" + suffix, 1,
					String.valueOf(data.substitutionParameterValues[2]), null,
					null, writer);
			writeParameter(GTRParser.A_TO_T, "at" + suffix, 1,
					String.valueOf(data.substitutionParameterValues[3]), null,
					null, writer);
			writeParameter(GTRParser.C_TO_G, "cg" + suffix, 1,
					String.valueOf(data.substitutionParameterValues[4]), null,
					null, writer);
			writeParameter(GTRParser.C_TO_T, "ct" + suffix, 1,
					String.valueOf(data.substitutionParameterValues[5]), null,
					null, writer);
			writeParameter(GTRParser.G_TO_T, "gt" + suffix, 1,
					String.valueOf(data.substitutionParameterValues[6]), null,
					null, writer);

			writer.writeCloseTag(GTRParser.GTR_MODEL);

			break;

		case 2: // TN93

			writer.writeOpenTag(NucModelType.TN93.getXMLName(),
					new Attribute[] { new Attribute.Default<String>(
							XMLParser.ID, data.substitutionModelIdref) });

			writer.writeOpenTag(FrequencyModelParser.FREQUENCIES);

			writer.writeIDref(FrequencyModelParser.FREQUENCY_MODEL,
					data.frequencyModelIdref);

			writer.writeCloseTag(FrequencyModelParser.FREQUENCIES);

			writeParameter(TN93Parser.KAPPA1, "kappa1" + suffix, 1,
					String.valueOf(data.substitutionParameterValues[7]), null,
					null, writer);

			writeParameter(TN93Parser.KAPPA2, "kappa2" + suffix, 1,
					String.valueOf(data.substitutionParameterValues[8]), null,
					null, writer);

			writer.writeCloseTag(NucModelType.TN93.getXMLName());

			break;

		case 3: // Yang Codon Model

			writer.writeOpenTag(YangCodonModelParser.YANG_CODON_MODEL,
					new Attribute[] { new Attribute.Default<String>(
							XMLParser.ID, data.substitutionModelIdref) });

			writer.writeOpenTag(FrequencyModelParser.FREQUENCIES);

			writer.writeIDref(FrequencyModelParser.FREQUENCY_MODEL,
					data.frequencyModelIdref);

			writer.writeCloseTag(FrequencyModelParser.FREQUENCIES);

			writeParameter(YangCodonModelParser.OMEGA, "omega" + suffix, 1,
					String.valueOf(data.substitutionParameterValues[9]), null,
					null, writer);

			writeParameter(YangCodonModelParser.KAPPA, "kappa" + suffix, 1,
					String.valueOf(data.substitutionParameterValues[10]), null,
					null, writer);

			writer.writeCloseTag(YangCodonModelParser.YANG_CODON_MODEL);

			break;

		}// END: switch

	}// END: writeBranchModel

	private void writeFrequencyModel(PartitionData data, XMLWriter writer) {

		DataType dataType = null;
		String frequencies = null;
		int dataTypeIndex = data.dataTypeIndex;

		switch (dataTypeIndex) {

		case 0: // Nucleotide

			dataType = data.createDataType();

			frequencies = data.frequencyParameterValues[0] + "";
			for (int i = 1; i < 4; i++) {
				frequencies += " " + data.frequencyParameterValues[i];
			}

			writer.writeOpenTag(FrequencyModelParser.FREQUENCY_MODEL, // tagname
					new Attribute[] { // attributes[]
							new Attribute.Default<String>(XMLParser.ID,
									data.frequencyModelIdref), // id
							new Attribute.Default<String>("dataType", dataType
									.getDescription()) // dataType
					});

			writeParameter(FrequencyModelParser.FREQUENCIES, null,
					dataType.getStateCount(), frequencies, 0.0, 1.0, writer);

			writer.writeCloseTag(FrequencyModelParser.FREQUENCY_MODEL);

			break;

		case 1: // Codon

			dataType = data.createDataType();

			frequencies = data.frequencyParameterValues[4] + "";
			for (int i = 5; i < 64; i++) {
				frequencies += " " + data.frequencyParameterValues[i];
			}

			writer.writeOpenTag(FrequencyModelParser.FREQUENCY_MODEL, // tagname
					new Attribute[] { // attributes[]
							new Attribute.Default<String>(XMLParser.ID,
									"freqModel"), // id
							new Attribute.Default<String>("dataType", dataType
									.getDescription()) // dataType
					});

			writeParameter(FrequencyModelParser.FREQUENCIES, null,
					dataType.getStateCount(), frequencies, 0.0, 1.0, writer);

			writer.writeCloseTag(FrequencyModelParser.FREQUENCY_MODEL);

		}// END: switch

	}// END: writeFrequencyModel

	@SuppressWarnings("rawtypes")
	private void writeParameter(String wrapper, String id, int dimension,
			String value, Double lower, Double upper, XMLWriter writer) {

		if (wrapper != null) {
			writer.writeOpenTag(wrapper);
		}

		ArrayList<Attribute.Default> attributes = new ArrayList<Attribute.Default>();

		if (id != null) {
			attributes.add(new Attribute.Default<String>(XMLParser.ID, id));
		}

		if (dimension > 1) {
			attributes.add(new Attribute.Default<String>(
					ParameterParser.DIMENSION, String.valueOf(dimension)));
		}

		if (value != null) {
			attributes.add(new Attribute.Default<String>(ParameterParser.VALUE,
					value));
		}

		if (lower != null) {
			attributes.add(new Attribute.Default<String>(ParameterParser.LOWER,
					String.valueOf(lower)));
		}

		if (upper != null) {
			attributes.add(new Attribute.Default<String>(ParameterParser.UPPER,
					String.valueOf(upper)));
		}

		Attribute[] attrArray = new Attribute[attributes.size()];
		for (int i = 0; i < attrArray.length; i++) {
			attrArray[i] = attributes.get(i);
		}

		writer.writeTag(ParameterParser.PARAMETER, attrArray, true);

		if (wrapper != null) {
			writer.writeCloseTag(wrapper);
		}

	}// END: writeParameter

}// END: class