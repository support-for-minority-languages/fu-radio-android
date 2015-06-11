package com.udmspell.furadio;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

/**
 * Komodo Lab: Tagin! Project: 3D Tag Cloud
 * Google Summer of Code 2011
 * @authors Reza Shiftehfar, Sara Khosravinasr and Jorge Silva
 */

/*
 * Tag class:
 * For now tags are just cubes. Later they will be replaced by real texts!
 */
public class Tag extends SugarRecord<Tag> implements Comparable<Tag>{
	public Tag() {
	}

	public Tag(int stationId, String text, int popularity, String url) {
        this.stationId = stationId;
		this.text = text;
    	this.locX = 0f;
    	this.locY = 0f;
    	this.locZ = 0f;

    	this.loc2DX = 0;
    	this.loc2DY=0;
    	
    	this.colorR= 0.5f;
    	this.colorG= 0.5f;
    	this.colorB= 0.5f;
    	this.alpha = 1.0f;
    	
    	this.scale = 1.0f;
    	this.popularity= popularity;
    	this.url = url;
    }	
	
	@Override
	public int compareTo(Tag another) {
		return (int)(another.locZ - locZ);
	}
	
    public float getLocX() {
		return locX;
	}
	public void setLocX(float locX) {
		this.locX = locX;
	}
	public float getLocY() {
		return locY;
	}
	public void setLocY(float locY) {
		this.locY = locY;
	}
	public float getLocZ() {
		return locZ;
	}
	public void setLocZ(float locZ) {
		this.locZ = locZ;
	}
	public float getScale() {
		return scale;
	}
	public void setScale(float scale) {
		this.scale = scale;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public float getColorR() {
		return colorR;
	}
	public void setColorR(float colorR) {
		this.colorR = colorR;
	}
	public float getColorG() {
		return colorG;
	}
	public void setColorG(float colorG) {
		this.colorG = colorG;
	}
	public float getColorB() {
		return colorB;
	}
	public void setColorB(float colorB) {
		this.colorB = colorB;
	}
	public float getAlpha() {
		return alpha;
	}
	public void setAlpha(float alpha) {
		this.alpha = alpha;
	}
	public int getPopularity() {
		return popularity;
	}
	public void setPopularity(int popularity) {
		this.popularity = popularity;
	}
	
	public int getTextSize() {
		return textSize;
	}
	public void setTextSize(int textSize) {
		this.textSize = textSize;
	}
	public float getLoc2DX() {
		return loc2DX;
	}
	public void setLoc2DX(float loc2dx) {
		loc2DX = loc2dx;
	}
	public float getLoc2DY() {
		return loc2DY;
	}
	public void setLoc2DY(float loc2dy) {
		loc2DY = loc2dy;
	}
	public int getParamNo() {
		return paramNo;
	}
	public void setParamNo(int paramNo) {
		this.paramNo = paramNo;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}

    public int getStationId() {
        return stationId;
    }

    public void setStationId(int stationId) {
        this.stationId = stationId;
    }

	String text, url;
    int stationId;
	int popularity;  //this is the importance/popularity of the Tag
	@Ignore
	private int textSize;
	@Ignore
	private float locX, locY, locZ; //the center of the 3D Tag
	@Ignore
	private float loc2DX, loc2DY;
	@Ignore
	private float scale;
	@Ignore
	private float colorR, colorG, colorB, alpha;
	@Ignore
    private static final int DEFAULT_POPULARITY = 1;
	@Ignore
    private int paramNo; //parameter that holds the setting for this Tag
}
