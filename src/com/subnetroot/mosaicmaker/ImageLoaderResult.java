package com.subnetroot.mosaicmaker;

import java.awt.image.BufferedImage;

public class ImageLoaderResult
{
	public int x;
	public int y;
	public BufferedImage image;
	public Exception loaderError;
	
	public ImageLoaderResult()
	{
		this.x = 0;
		this.y = 0;
		this.image = null;
		this.loaderError = null;
	}
	
	public ImageLoaderResult(int x, int y)
	{
		this();
		this.x = x;
		this.y = y;
	}
}
