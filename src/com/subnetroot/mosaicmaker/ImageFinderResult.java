package com.subnetroot.mosaicmaker;

import java.net.URL;

public class ImageFinderResult
{
	public int x;
	public int y;
	public URL imageUrl;
	
	public ImageFinderResult()
	{
		this.x = 0;
		this.y = 0;
		this.imageUrl = null;
	}
	
	public ImageFinderResult(int x, int y, URL imageUrl)
	{
		this.x = x;
		this.y = y;
		this.imageUrl = imageUrl;
	}
}
