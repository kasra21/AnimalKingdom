package com.boot.animalkingdom.model;

public class SimilarityReport {
	
	String dogType;
	String simularityPercentageStr;
	Double simularityPercentage;
	
	public String getDogType() {
		return dogType;
	}
	public void setDogType(String dogType) {
		this.dogType = dogType;
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
