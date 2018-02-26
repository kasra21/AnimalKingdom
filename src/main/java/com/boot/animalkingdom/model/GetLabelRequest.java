package com.boot.animalkingdom.model;

import org.hibernate.validator.constraints.NotBlank;

public class GetLabelRequest {
	
    @NotBlank(message = "label group can't empty!")
    String labelGroup;

	public String getLabelGroup() {
		return labelGroup;
	}

	public void setLabelGroup(String labelGroup) {
		this.labelGroup = labelGroup;
	}
    
}