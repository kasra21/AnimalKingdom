package com.boot.animalkingdom.services;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import com.boot.animalkingdom.SpringBootWebApplication;
import com.boot.animalkingdom.model.AnimalResponse;
import com.boot.animalkingdom.model.BaseResponse;
import com.boot.animalkingdom.model.GetLabelRequest;
import com.boot.animalkingdom.model.GetLabelsResponse;
import com.boot.animalkingdom.model.SimilarityReport;

@Service
public class AnimalService {
	
	private static final Logger logger = LoggerFactory.getLogger(SpringBootWebApplication.class);

	//classifies the image of the animal
	public AnimalResponse classifyImage(MultipartFile file, String animal) {
		//animal can be: All || Dog || Cat
		
		AnimalResponse result = new AnimalResponse();
		ArrayList<String> resultText = new ArrayList<>();
		ArrayList<SimilarityReport> reports = new ArrayList<>();

		if (file.isEmpty()) {
			result.setMsg("redirect:uploadStatus");
			result.setResult(null);
			return result;			
		}

		try {
			// Get the file and save it somewhere
			byte[] imageBytes = file.getBytes();
			String modelDirPath = System.getProperty("user.dir") + "/tensorflowResource/";

			// graph
			byte[] graphDef;
			List<String> labels;
			if("All".equals(animal)) {
				graphDef = readAllBytesOrExit(Paths.get(modelDirPath, "output.pb"));
				labels = readAllLinesOrExit(Paths.get(modelDirPath, "labels.txt"));
			}
			else {
				graphDef = readAllBytesOrExit(Paths.get(modelDirPath, "outputAll"+animal+"s.pb"));
				labels = readAllLinesOrExit(Paths.get(modelDirPath, "labelsAll"+animal+"s.txt"));
			}

			try (Tensor image = Tensor.create(imageBytes)) {
				List<String> tempLabelList = new ArrayList<>();		//can use this for back tracking
				float[] labelProbabilities = executeInceptionGraph(graphDef, image);
				ArrayList<Integer> bestLabelIdxs = top5Index(labelProbabilities);
				for (int j = 0; j < bestLabelIdxs.size(); j++) {
					logger.info(String.format("BEST MATCH: %s (%.2f%% likely) \n", labels.get(bestLabelIdxs.get(j)),
							labelProbabilities[bestLabelIdxs.get(j)] * 100f));
					resultText.add(String.format("BEST MATCH: %s (%.2f%% likely)", labels.get(bestLabelIdxs.get(j)),
							labelProbabilities[bestLabelIdxs.get(j)] * 100f));

					SimilarityReport report = new SimilarityReport();
					String label = labels.get(bestLabelIdxs.get(j)).substring(0, 1).toUpperCase() + labels.get(bestLabelIdxs.get(j)).substring(1);
					//if it ends with digit(s) then grab only the first one
					while(Character.isDigit(label.charAt(label.length()-1))) {
						label = label.substring(0, label.length()-1);
					}
					//The text result and report result are not exactly thew same
					if(!tempLabelList.contains(label)){
						tempLabelList.add(label);
						report.setType(label);
						report.setSimularityPercentage((double) (labelProbabilities[bestLabelIdxs.get(j)] * 100f));
						report.setSimularityPercentageStr(
								String.format("%.2f%%", labelProbabilities[bestLabelIdxs.get(j)] * 100f));
						reports.add(report);
					}
				}
				logger.info("\n");
				result.setMsg("success");
				result.setResultText(resultText);
				result.setResult(reports);
			}
		} catch (IOException e) {
			logger.error(e.getMessage() + "\n");
			//e.printStackTrace();
			result.setMsg(e.getMessage());
			result.setResultText(null);
			result.setResult(null);
			return result; 
		}

		return result;		
	}
	
	public GetLabelsResponse getLabels(GetLabelRequest request) {
		
		GetLabelsResponse result = new GetLabelsResponse();
        
		try {
			// Get the file and save it somewhere
			String modelDirPath = System.getProperty("user.dir") + "/tensorflowResource/";
			List<String> labels = new ArrayList<> ();
			if("All".equals(request.getLabelGroup())) {
				labels = readAllLinesOrExit(Paths.get(modelDirPath, "labels.txt"));
				List<String> tempLabels = readAllLinesOrExit(Paths.get(modelDirPath, "labelsAllDogs.txt"));
				labels.addAll(tempLabels);
				tempLabels = readAllLinesOrExit(Paths.get(modelDirPath, "labelsAllCats.txt"));
				labels.addAll(tempLabels);
			}
			else if("Animal".equals(request.getLabelGroup())) {
				labels = readAllLinesOrExit(Paths.get(modelDirPath, "labels.txt"));
			}
			else {
				labels = readAllLinesOrExit(Paths.get(modelDirPath, "labelsAll"+request.getLabelGroup()+"s.txt"));
			}
			
			if(labels.isEmpty()) {
				result.setMsg("failed to return labels");
				result.setLabels(null);
				logger.info("failed to return labels \n");
				return result;
			}
			for(int i=0; i<labels.size(); i++){
				labels.set(i, labels.get(i).substring(0, 1).toUpperCase() + labels.get(i).substring(1));
			}
			Collections.sort(labels);
			result.setLabels(labels);
			result.setMsg("success");
			logger.info("success \n");


		} catch (Exception e) {
			//e.printStackTrace();
			logger.error(e.getMessage() + "\n");
			result.setMsg(e.getMessage());
			return result;
		}
        
        
        return result;
	}
	
	public BaseResponse uploadTmpImage(MultipartFile file, String label) {
		BaseResponse result = new BaseResponse();

		if (file.isEmpty()) {
			result.setMsg("redirect:uploadStatus");
			return result;
		}
		try {
			// Get the file and save it somewhere
			byte[] imageBytes = file.getBytes();
			String modelDirPath = System.getProperty("user.dir") + "/tensorflowResource/";
			Boolean containsLabel = false;
			List<String> labels = readAllLinesOrExit(Paths.get(modelDirPath, "labelsAllDogs.txt"));
			for( String l : labels ) {
				if(l.toLowerCase().contains(label.toLowerCase())) {
					containsLabel = true;
					break;
				}
			}
			if( !containsLabel ) {
				logger.info("Failed to upload Help Image: label is not in the list of labels");
				result.setMsg("Failed to upload Help Image: label is not in the list of labels");
				return result;
			}
			File tmpDir = new File(System.getProperty("user.dir")+"/tmp");
			
			if(!tmpDir.exists()) {
				tmpDir.mkdir();
			}
			File convFile = new File(System.getProperty("user.dir")+"/tmp/"+label+"_"+file.getOriginalFilename());
			if(!convFile.exists()) {
				file.transferTo(convFile);
			}
			else {
				convFile = new File(System.getProperty("user.dir")+"/tmp/_"+file.getOriginalFilename());
				file.transferTo(convFile);	
			}
			result.setMsg("successfully uploaded the file");
			logger.info("successfully uploaded the file \n");


		} catch (IOException e) {
			//e.printStackTrace();
			logger.error(e.getMessage() + "\n");
			result.setMsg(e.getMessage());
			ResponseEntity.badRequest().body(result);
		}

		return result;
	}
	
	public AnimalResponse tensorFlowTest() {
		
		AnimalResponse result = new AnimalResponse();
		ArrayList<String> resultText = new ArrayList<>();
		ArrayList<SimilarityReport> reports = new ArrayList<>();

		String modelDirPath = System.getProperty("user.dir") + "/tensorflowResource/";
		String imagePath = System.getProperty("user.dir") + "/testResource/imageSingle/peachy.jpg";

		// graph
		byte[] graphDef = readAllBytesOrExit(Paths.get(modelDirPath, "output.pb"));
		List<String> labels = readAllLinesOrExit(Paths.get(modelDirPath, "labels.txt"));
		byte[] imageBytes = readAllBytesOrExit(Paths.get(imagePath));

		try (Tensor image = Tensor.create(imageBytes)) {
			float[] labelProbabilities = executeInceptionGraph(graphDef, image);
			ArrayList<Integer> bestLabelIdxs = top5Index(labelProbabilities);
			// result.setText("");
			for (int j = 0; j < bestLabelIdxs.size(); j++) {
				logger.info(String.format("BEST MATCH: %s (%.2f%% likely) \n", labels.get(bestLabelIdxs.get(j)),
						labelProbabilities[bestLabelIdxs.get(j)] * 100f));
				resultText.add(String.format("BEST MATCH: %s (%.2f%% likely)", labels.get(bestLabelIdxs.get(j)),
						labelProbabilities[bestLabelIdxs.get(j)] * 100f));

				SimilarityReport report = new SimilarityReport();
				report.setType(labels.get(bestLabelIdxs.get(j)));
				report.setSimularityPercentage((double) (labelProbabilities[bestLabelIdxs.get(j)] * 100f));
				report.setSimularityPercentageStr(
						String.format("%.2f%%", labelProbabilities[bestLabelIdxs.get(j)] * 100f));

				reports.add(report);
			}
			logger.info("\n");
			result.setMsg("success");
			result.setResultText(resultText);
			result.setResult(reports);
		} catch (Exception e) {
			//e.printStackTrace();
			logger.error(e.getMessage() + "\n");
			result.setMsg(e.getMessage());
			result.setResultText(null);
			result.setResult(null);
			return result;
		}

		return result;
	}
	
	
	//--------------------------------------------Private methods
	
	private static List<String> readAllLinesOrExit(Path path) {
		try {
			return Files.readAllLines(path, Charset.forName("UTF-8"));
		} catch (IOException e) {
			logger.error("Failed to read [" + path + "]: " + e.getMessage() + "\n");
			System.exit(0);
		}
		return null;
	}

	private static byte[] readAllBytesOrExit(Path path) {
		try {
			return Files.readAllBytes(path);
		} catch (IOException e) {
			logger.error("Failed to read [" + path + "]: " + e.getMessage() + "\n");
			System.exit(1);
		}
		return null;
	}

	private static float[] executeInceptionGraph(byte[] graphDef, Tensor image) {
		try (Graph g = new Graph()) {
			g.importGraphDef(graphDef);
			try (Session s = new Session(g);
					Tensor<?> result = s.runner().feed("DecodeJpeg/contents", image).fetch("final_result").run()
							.get(0)) {
				final long[] rshape = result.shape();
				if (result.numDimensions() != 2 || rshape[0] != 1) {
					throw new RuntimeException(String.format(
							"Expected model to produce a [1 N] shaped tensor where N is the number of labels, instead it produced one with shape %s",
							Arrays.toString(rshape)));
				}
				int nlabels = (int) rshape[1];
				return result.copyTo(new float[1][nlabels])[0];
			}
		}
	}

	private static ArrayList<Integer> top5Index(float[] probabilities) {
		ArrayList<Integer> Top5Index = new ArrayList<>();

		Map<Integer, Float> probabilitiesMap = new HashMap<>();
		for (int i = 0; i < probabilities.length; i++) {
			probabilitiesMap.put(i, probabilities[i]);
		}
		Set<Entry<Integer, Float>> set = probabilitiesMap.entrySet();
		List<Entry<Integer, Float>> list = new ArrayList<Entry<Integer, Float>>(set);

		Collections.sort(list, new Comparator<Map.Entry<Integer, Float>>() {
			@Override
			public int compare(Entry<Integer, Float> o1, Entry<Integer, Float> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});

		for (int i = 0; i < 5; i++) {
			Top5Index.add(list.get(i).getKey());
		}

		return Top5Index;
	}


}
