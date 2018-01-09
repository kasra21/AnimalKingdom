package com.boot.animalkingdom.model;

import java.util.ArrayList;

public class AjaxResponseBody {

    String msg;
    ArrayList<String> resultText;		//result of the similarity result

	public ArrayList<String> getResultText() {
		return resultText;
	}

	public void setResultText(ArrayList<String> resultText) {
		this.resultText = resultText;
	}

	public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

}
