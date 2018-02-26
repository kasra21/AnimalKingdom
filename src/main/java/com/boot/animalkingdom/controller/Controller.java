package com.boot.animalkingdom.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlow;

import com.boot.animalkingdom.SpringBootWebApplication;
import com.boot.animalkingdom.model.AjaxResponseBody;
import com.boot.animalkingdom.model.BaseResponse;
import com.boot.animalkingdom.model.GetLabelRequest;
import com.boot.animalkingdom.model.GetLabelsResponse;
import com.boot.animalkingdom.model.SimilarityReport;
import com.boot.animalkingdom.services.AnimalService;

import javax.validation.Valid;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class Controller {

	AnimalService animalService;
	private static final Logger logger = LoggerFactory.getLogger(SpringBootWebApplication.class);

	@Autowired
	public void setUserService(AnimalService animalService) {
		this.animalService = animalService;
	}

	@CrossOrigin
	@PostMapping("/api/classifyImage")
	public ResponseEntity<?> classifyImageResultViaAjax(@RequestParam("file") MultipartFile file) {

		AjaxResponseBody result = new AjaxResponseBody();
		ArrayList<String> resultText = new ArrayList<>();
		ArrayList<SimilarityReport> reports = new ArrayList<>();

		if (file.isEmpty()) {
			result.setMsg("redirect:uploadStatus");
			return ResponseEntity.badRequest().body(result);
		}

		try {
			// Get the file and save it somewhere
			byte[] imageBytes = file.getBytes();
			String modelDirPath = System.getProperty("user.dir") + "/tensorflowResource/";

			// graph
			byte[] graphDef = readAllBytesOrExit(Paths.get(modelDirPath, "output.pb"));
			List<String> labels = readAllLinesOrExit(Paths.get(modelDirPath, "labels.txt"));

			try (Tensor image = Tensor.create(imageBytes)) {
				float[] labelProbabilities = executeInceptionGraph(graphDef, image);
				ArrayList<Integer> bestLabelIdxs = top5Index(labelProbabilities);
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
			}
		} catch (IOException e) {
			logger.error(e.getMessage() + "\n");
			//e.printStackTrace();
			result.setMsg(e.getMessage());
			result.setResultText(null);
			result.setResult(null);
			return ResponseEntity.badRequest().body(result);
		}

		return ResponseEntity.ok(result);
	}

	@CrossOrigin
	@PostMapping("/api/classifyDogImage")
	public ResponseEntity<?> classifyDogImageResultViaAjax(@RequestParam("file") MultipartFile file) {

		AjaxResponseBody result = new AjaxResponseBody();
		ArrayList<String> resultText = new ArrayList<>();
		ArrayList<SimilarityReport> reports = new ArrayList<>();

		if (file.isEmpty()) {
			result.setMsg("redirect:uploadStatus");
			return ResponseEntity.badRequest().body(result);
		}

		try {
			// Get the file and save it somewhere
			byte[] imageBytes = file.getBytes();
			String modelDirPath = System.getProperty("user.dir") + "/tensorflowResource/";

			// graph
			byte[] graphDef = readAllBytesOrExit(Paths.get(modelDirPath, "outputAllDogs.pb"));
			List<String> labels = readAllLinesOrExit(Paths.get(modelDirPath, "labelsAllDogs.txt"));

			try (Tensor image = Tensor.create(imageBytes)) {
				float[] labelProbabilities = executeInceptionGraph(graphDef, image);
				ArrayList<Integer> bestLabelIdxs = top5Index(labelProbabilities);
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
			}
		} catch (IOException e) {
			//e.printStackTrace();
			logger.error(e.getMessage() + "\n");
			result.setMsg(e.getMessage());
			result.setResultText(null);
			result.setResult(null);
			return ResponseEntity.badRequest().body(result);
		}

		return ResponseEntity.ok(result);
	}
	
    @CrossOrigin
    @PostMapping("/api/getLabels")
    public ResponseEntity<?> getLabels(@Valid @RequestBody GetLabelRequest request, Errors errors) {

    	GetLabelsResponse result = new GetLabelsResponse();
        //If error, just return a 400 bad request, along with the error message
        if (errors.hasErrors()) {
            String errorMsg = "";
            for(int i =0; i<errors.getAllErrors().size(); i++) {
                errorMsg += errors.getAllErrors().get(i).getDefaultMessage();
                if(i > 0) {
                    errorMsg += "and/or";
                }
            }
            result.setMsg(errorMsg);
            return ResponseEntity.badRequest().body(result);
        }
        
        try {
			// Get the file and save it somewhere
			String modelDirPath = System.getProperty("user.dir") + "/tensorflowResource/";
			List<String> labels = new ArrayList<> ();
			if("All".equals(request.getLabelGroup())) {
				labels = readAllLinesOrExit(Paths.get(modelDirPath, "labels.txt"));
				List<String> tempLabels = readAllLinesOrExit(Paths.get(modelDirPath, "labelsAllDogs.txt"));
				labels.addAll(tempLabels);
			}
			if("Animals".equals(request.getLabelGroup())) {
				labels = readAllLinesOrExit(Paths.get(modelDirPath, "labels.txt"));
			}
			else if("Dogs".equals(request.getLabelGroup())) {
				labels = readAllLinesOrExit(Paths.get(modelDirPath, "labelsAllDogs.txt"));
			}
			List<String> labelsToReturn = new ArrayList<>();
			for(String l : labels) {
				if(!l.matches(".+\\d+")) {
					labelsToReturn.add(l);
				}
			}
									
			if(labelsToReturn.isEmpty()) {
				result.setMsg("failed to return labels");
				logger.info("failed to return labels \n");
				return ResponseEntity.badRequest().body(result);
			}
			
			result.setLabels(labelsToReturn);
			result.setMsg("success");
			logger.info("success \n");


		} catch (Exception e) {
			//e.printStackTrace();
			logger.error(e.getMessage() + "\n");
			result.setMsg(e.getMessage());
			return ResponseEntity.badRequest().body(result);
		}
        
        
        return ResponseEntity.ok(result);
    }
	
	//uploads the file into the system
	@CrossOrigin
	@PostMapping("/api/uploadTmpImage")
	public ResponseEntity<?> uploadTmpImage(@RequestParam("file") MultipartFile file, @RequestParam("label") String label) {

		BaseResponse result = new BaseResponse();

		if (file.isEmpty()) {
			result.setMsg("redirect:uploadStatus");
			return ResponseEntity.badRequest().body(result);
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
				return ResponseEntity.ok(result);
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

		return ResponseEntity.ok(result);
	}

	@GetMapping("/api/tensorFlowTest")
	public ResponseEntity<?> tensorFlowTest() throws UnsupportedEncodingException {

		AjaxResponseBody result = new AjaxResponseBody();
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
			ResponseEntity.badRequest().body(result);
		}

		return ResponseEntity.ok(result);
	}

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
