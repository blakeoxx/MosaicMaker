package com.subnetroot.mosaicmaker;

import java.net.URL;
import java.util.Iterator;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

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
	
	public ImageLoaderResult call()
	{
		ImageLoaderResult result = new ImageLoaderResult(x, y);
		if (imageUrl != null)
		{
			try
			{
				ImageInputStream iis = ImageIO.createImageInputStream(imageUrl.openStream());
				Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
				
				String format = null;
				if (readers.hasNext())
				{
					ImageReader reader = readers.next();
					format = reader.getFormatName();
					reader.setInput(iis);
					result.image = reader.read(0);
				}
				
				if (format == null)
				{
					throw new Exception("Could not load image from URL '" + imageUrl.toString() + "': No reader registered for this format");
				}
				else if (result.image == null)
				{
					throw new Exception("Could not load image from URL '" + imageUrl.toString() + "': Reader for format '" + format + "' returned null");
				}
			}
			catch (Exception e)
			{
				// Put any errors in the ImageLoaderResult instead of bubbling the exception, so the ImageProcessingThread always gets a good result
				result.loaderError = e;
			}
		}
		return result;
	}
}
