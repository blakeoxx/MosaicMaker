package com.subnetroot.mosaicmaker;

import java.awt.image.BufferedImage;

public class ImageLoaderResult
{
	public int x;
	public int y;
	public BufferedImage image;
	
	public ImageLoaderResult()
	{
		this.x = 0;
		this.y = 0;
		this.image = null;
	}
	
	public ImageLoaderResult(int x, int y, BufferedImage image)
	{
		this.x = x;
		this.y = y;
		this.image = image;
	}
}
