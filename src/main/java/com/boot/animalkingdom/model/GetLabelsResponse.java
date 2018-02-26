package com.boot.animalkingdom.model;

import java.util.List;

public class GetLabelsResponse extends BaseResponse {
	
	List<String> labels;

	public List<String> getLabels() {
		return labels;
	}

	public void setLabels(List<String> labels) {
		this.labels = labels;
	}
	
	

}
