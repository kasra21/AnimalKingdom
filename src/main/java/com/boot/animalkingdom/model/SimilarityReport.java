package com.boot.animalkingdom.model;

public class SimilarityReport {
	
	String type;
	String simularityPercentageStr;
	Double simularityPercentage;
	
	public String getType() {
		return type;
	}
	public void setType(String dogType) {
		this.type = dogType;
	}
	public String getSimularityPercentageStr() {
		return simularityPercentageStr;
	}
	public void setSimularityPercentageStr(String simularityPercentageStr) {
		this.simularityPercentageStr = simularityPercentageStr;
	}
	public Double getSimularityPercentage() {
		return simularityPercentage;
	}
	public void setSimularityPercentage(Double simularityPercentage) {
		this.simularityPercentage = simularityPercentage;
	}

}
