package com.subnetroot.mosaicmaker;

import java.net.URL;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;

public class ImageLoader implements Callable<ImageLoaderResult>
{
	int x;
	int y;
	URL imageUrl;
	
	public ImageLoader(int x, int y, URL imageUrl)
	{
		super();
		this.x = x;
		this.y = y;
		this.imageUrl = imageUrl;
	}
	
	public ImageLoaderResult call() throws Exception
	{
		ImageLoaderResult result = new ImageLoaderResult(x, y, null);
		if (imageUrl != null) result.image = ImageIO.read(imageUrl);
		return result;
	}
}
