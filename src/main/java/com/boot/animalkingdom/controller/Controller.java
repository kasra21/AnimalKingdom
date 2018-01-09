package com.boot.animalkingdom.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
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

import com.boot.animalkingdom.model.AjaxResponseBody;
import com.boot.animalkingdom.services.AnimalService;

import javax.validation.Valid;

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

	@Autowired
	public void setUserService(AnimalService animalService) {
		this.animalService = animalService;
	}
	
	@PostMapping("/api/classifyImage")
	public ResponseEntity<?> classifyImageResultViaAjax(@RequestParam("file") MultipartFile file) {

		AjaxResponseBody result = new AjaxResponseBody();
		
		if (file.isEmpty()) {
            result.setMsg("redirect:uploadStatus");
            return ResponseEntity.ok(result);
        }
		
		try {
            // Get the file and save it somewhere
			byte[] imageBytes = file.getBytes();
            String modelDirPath = System.getProperty("user.dir") + "/tensorflowResource/";
    		
    		//graph
    		byte[] graphDef = readAllBytesOrExit(Paths.get(modelDirPath, "output.pb"));
    		List<String> labels = readAllLinesOrExit(Paths.get(modelDirPath, "labels.txt"));
    		
    		try (Tensor image = Tensor.create(imageBytes)) {
                float[] labelProbabilities = executeInceptionGraph(graphDef, image);
                ArrayList<Integer> bestLabelIdxs = top5Index(labelProbabilities);
                //result.setText("");
                for(int j=0; j<bestLabelIdxs.size(); j++) {
                	result.setMsg(String.format(
                            "BEST MATCH: %s (%.2f%% likely)",
                            labels.get(bestLabelIdxs.get(j)), labelProbabilities[bestLabelIdxs.get(j)] * 100f));
                    System.out.println(
                            String.format(
                                    "BEST MATCH: %s (%.2f%% likely)",
                                    labels.get(bestLabelIdxs.get(j)), labelProbabilities[bestLabelIdxs.get(j)] * 100f));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

		return ResponseEntity.ok(result);
	}
	

//	@PostMapping("/api/deleteUser")
//	public ResponseEntity<?> deleteUsersResultViaAjax(@Valid @RequestBody SearchOrDeleteCriteria delete,
//			Errors errors) {
//
//		AjaxResponseBody result = new AjaxResponseBody();
//
//		// If error, just return a 400 bad request, along with the error message
//		if (errors.hasErrors()) {
//			result.setMsg(
//					errors.getAllErrors().stream().map(x -> x.getDefaultMessage()).collect(Collectors.joining(",")));
//			return ResponseEntity.badRequest().body(result);
//		}
//
//		List<User> users = animalService.deleteUser(delete.getUsername());
//		if (users.isEmpty()) {
//			result.setMsg("could not delete user");
//		} else {
//			result.setMsg("success");
//		}
//		result.setResult(users);
//
//		return ResponseEntity.ok(result);
//	}
	
	//--------------------------------------------------------------------------------------DarkZone

	@GetMapping("/api/tensorFlowTest")
	public ResponseEntity<?> tensorFlowTest() throws UnsupportedEncodingException {

		AjaxResponseBody result = new AjaxResponseBody();
		String modelDirPath = System.getProperty("user.dir") + "/tensorflowResource/";
		String imagePath = System.getProperty("user.dir") + "/testResource/imageSingle/peachy.jpg";
		
		//graph
		byte[] graphDef = readAllBytesOrExit(Paths.get(modelDirPath, "output.pb"));
		List<String> labels = readAllLinesOrExit(Paths.get(modelDirPath, "labels.txt"));
		byte[] imageBytes = readAllBytesOrExit(Paths.get(imagePath));
		
		try (Tensor image = Tensor.create(imageBytes)) {
            float[] labelProbabilities = executeInceptionGraph(graphDef, image);
            ArrayList<Integer> bestLabelIdxs = top5Index(labelProbabilities);
            //result.setText("");
            for(int j=0; j<bestLabelIdxs.size(); j++) {
            	result.setMsg(String.format(
                        "BEST MATCH: %s (%.2f%% likely)",
                        labels.get(bestLabelIdxs.get(j)), labelProbabilities[bestLabelIdxs.get(j)] * 100f));
                System.out.println(
                        String.format(
                                "BEST MATCH: %s (%.2f%% likely)",
                                labels.get(bestLabelIdxs.get(j)), labelProbabilities[bestLabelIdxs.get(j)] * 100f));
            }
        }
		
		return ResponseEntity.ok(result);
	}

	private static List<String> readAllLinesOrExit(Path path) {
		try {
			return Files.readAllLines(path, Charset.forName("UTF-8"));
		} catch (IOException e) {
			System.err.println("Failed to read [" + path + "]: " + e.getMessage());
			System.exit(0);
		}
		return null;
	}

	private static byte[] readAllBytesOrExit(Path path) {
		try {
			return Files.readAllBytes(path);
		} catch (IOException e) {
			System.err.println("Failed to read [" + path + "]: " + e.getMessage());
			System.exit(1);
		}
		return null;
	}
	
	private static float[] executeInceptionGraph(byte[] graphDef, Tensor image) {
        try (Graph g = new Graph()) {
            g.importGraphDef(graphDef);
            try (Session s = new Session(g);
                    Tensor<?> result = s.runner().feed("DecodeJpeg/contents", image).fetch("final_result").run().get(0)) {
                final long[] rshape = result.shape();
                if (result.numDimensions() != 2 || rshape[0] != 1) {
                    throw new RuntimeException(
                            String.format(
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
        for(int i=0; i<probabilities.length; i++) {
        	probabilitiesMap.put(i, probabilities[i]);
        }
        Set<Entry<Integer, Float>> set = probabilitiesMap.entrySet();
        List<Entry<Integer, Float>> list = new ArrayList<Entry<Integer, Float>>(set);

        Collections.sort(list, new Comparator<Map.Entry<Integer, Float>>()
        {
			@Override
			public int compare(Entry<Integer, Float> o1, Entry<Integer, Float> o2) {
				return (o2.getValue()).compareTo( o1.getValue() );
			}
        } );
                        
        for (int i = 0; i < 5; i++) {
        	Top5Index.add(list.get(i).getKey());
        }
        
        return Top5Index;
    }

	
	//--------------------------------------------------------------------------------------DarkZone

}
