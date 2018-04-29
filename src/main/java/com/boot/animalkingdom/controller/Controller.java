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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlow;

import com.boot.animalkingdom.SpringBootWebApplication;
import com.boot.animalkingdom.model.AnimalResponse;
import com.boot.animalkingdom.model.BaseResponse;
import com.boot.animalkingdom.model.GetLabelRequest;
import com.boot.animalkingdom.model.GetLabelsResponse;
import com.boot.animalkingdom.model.SimilarityReport;
import com.boot.animalkingdom.services.AnimalService;
import com.boot.animalkingdom.services.DbService;

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
@RequestMapping("/api")
public class Controller {

	AnimalService animalService;
	
	@Autowired
	public void setUserService(AnimalService animalService) {
		this.animalService = animalService;
	}

	@CrossOrigin
	@PostMapping("/classifyImage")
	public ResponseEntity<?> classifyImage(@RequestParam("file") MultipartFile file, @RequestParam("animal") String animal) {
		//animal can be: All || Dog || Cat
		AnimalResponse result = animalService.classifyImage(file, animal);
		return result.getResult() == null ? ResponseEntity.badRequest().body(result) : ResponseEntity.ok(result);
	}
	
    @CrossOrigin
    @PostMapping("/getLabels")
    public ResponseEntity<?> getLabels(@Valid @RequestBody GetLabelRequest request, Errors errors) {
    	//request.getLabelGroup can be All | Animal | Dog | Cat 
    	
    	//If error, just return a 400 bad request, along with the error message
    	//can use lamda expressions
        if (errors.hasErrors()) {
            String errorMsg = "";
            for(int i =0; i<errors.getAllErrors().size(); i++) {
                errorMsg += errors.getAllErrors().get(i).getDefaultMessage();
                if(i > 0) {
                    errorMsg += "and/or";
                }
            }
            GetLabelsResponse result = new GetLabelsResponse();
            result.setMsg(errorMsg);
            return ResponseEntity.badRequest().body(result);
        }
    	
    	GetLabelsResponse result = animalService.getLabels(request);
		return result.getLabels() == null ? ResponseEntity.badRequest().body(result) : ResponseEntity.ok(result);
    }
	
	//uploads the file into the system
	@CrossOrigin
	@PostMapping("/uploadTmpImage")
	public ResponseEntity<?> uploadTmpImage(@RequestParam("file") MultipartFile file, @RequestParam("label") String label) {
		BaseResponse result = animalService.uploadTmpImage(file, label);
		return (result.getMsg().contains("redirect") || result.getMsg().contains("Failed")) ? ResponseEntity.badRequest().body(result) : ResponseEntity.ok(result);
	}

	@GetMapping("/tensorFlowTest")
	public ResponseEntity<?> tensorFlowTest() throws UnsupportedEncodingException {
		AnimalResponse result = animalService.tensorFlowTest();
		return result.getMsg().contains("success") ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
	}

}
