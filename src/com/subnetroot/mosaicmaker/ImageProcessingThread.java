package com.subnetroot.mosaicmaker;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class ImageProcessingThread extends Thread
{
	private MosaicMaker parent;
	private int cellsWide;
	private int cellsHigh;
	private BufferedImage imgBaseImage;
	private int threadcount;
	private int mosaicWidth;
	private int mosaicHeight;
	private int tileFillMode;
	
	// Constants for tileFillMode
	public static final int FILLMODE_COLOR = 0;
	public static final int FILLMODE_IMAGE_NOCOLOR = 1;
	public static final int FILLMODE_IMAGE_COLOR = 2;
	
	public void run()
	{
		parent.setStatus("Profiling image colors...");
		Color[][] colorProfile = profileImageColors();
		
		parent.setStatus("Finding tile images...");
		URL[][] subImageURLs;
		if (tileFillMode == FILLMODE_IMAGE_NOCOLOR || tileFillMode == FILLMODE_IMAGE_COLOR) subImageURLs = findSubImages(colorProfile);
		else subImageURLs = new URL[cellsWide][cellsHigh];
		
		parent.setStatus("Combining tile images...");
		BufferedImage mosaic = combineSubImages(colorProfile, subImageURLs);
		
		parent.endProcessing(mosaic);
	}
	
	public ImageProcessingThread(MosaicMaker parent, int cellsWide, int cellsHigh, BufferedImage imgBaseImage, int threadcount, int mosaicWidth, int mosaicHeight, int tileFillMode)
	{
		super();
		this.parent = parent;
		this.cellsWide = cellsWide;
		this.cellsHigh = cellsHigh;
		this.imgBaseImage = imgBaseImage;
		this.threadcount = Math.max(threadcount, 1);
		this.mosaicWidth = mosaicWidth;
		this.mosaicHeight = mosaicHeight;
		this.tileFillMode = tileFillMode;
	}
	
	private Color[][] profileImageColors()
	{
		Color[][] colorProfile = new Color[cellsWide][cellsHigh];
		float cellWidth = (float)imgBaseImage.getWidth()/cellsWide;
		float cellHeight = (float)imgBaseImage.getHeight()/cellsHigh;
		
		int thisX = 0;
		for (int x = 0; x < cellsWide; x++)
		{
			int nextX = (int)Math.round(cellWidth*(x+1));
			int thisY = 0;
			for (int y = 0; y < cellsHigh; y++)
			{
				int nextY = (int)Math.round(cellHeight*(y+1));
				int[] thisRegion = imgBaseImage.getRGB(thisX, thisY, nextX-thisX, nextY-thisY, null, 0, nextX-thisX);
				colorProfile[x][y] = getAverageColor(thisRegion);
				parent.setStatus("Profiling image colors "+((x*cellsHigh)+y+1)+"/"+(cellsWide*cellsHigh));
				parent.setPreviewColorProfile(colorProfile);
				thisY = nextY;
			}
			thisX = nextX;
		}
		
		return colorProfile;
	}
	
	// There are many different interpolation techniques. We use linear interpolation because it's fast and good enough for low-resolution averages
	private Color getAverageColor(int[] pixels)
	{
		long colorAccumulatorR = 0;
		long colorAccumulatorG = 0;
		long colorAccumulatorB = 0;
		for (int a = 0; a < pixels.length; a++)
		{
			colorAccumulatorR += pixels[a]>>16&0xff;
			colorAccumulatorG += pixels[a]>>8&0xff;
			colorAccumulatorB += pixels[a]&0xff;
		}
		return new Color((int)Math.round((double)colorAccumulatorR/pixels.length),
						(int)Math.round((double)colorAccumulatorG/pixels.length),
						(int)Math.round((double)colorAccumulatorB/pixels.length));
	}
	
	private URL[][] findSubImages(Color[][] colorProfile)
	{
		ThreadPoolExecutor threadpool = (ThreadPoolExecutor)Executors.newFixedThreadPool(threadcount);
		CompletionService<ImageFinderResult> completionService = new ExecutorCompletionService<ImageFinderResult>(threadpool);
		URL[][] subimages = new URL[cellsWide][cellsHigh];
		
		for (int x = 0; x < cellsWide; x++)
		{
			for (int y = 0; y < cellsHigh; y++)
			{
				CloseableHttpClient httpclient = HttpClients.createDefault();
				URIBuilder partialuri = new URIBuilder().setScheme("http").setHost("labs.tineye.com").setPath("/multicolr/rest/color_search/");
				completionService.submit(new ImageFinder(x, y, httpclient, partialuri, colorProfile[x][y]));
			}
		}
		
		threadpool.shutdown();
		
		int totalComplete = 0;
		while (!threadpool.isTerminated() || totalComplete < threadpool.getTaskCount())
		{
			try
			{
				Future<ImageFinderResult> thisFuture = completionService.poll(1, TimeUnit.SECONDS);
				if (thisFuture == null) continue;
				ImageFinderResult finderResult = thisFuture.get();
				subimages[finderResult.x][finderResult.y] = finderResult.imageUrl;
			}
			catch (Exception e)
			{
				Throwable cause = e.getCause();
				if (cause != null) System.out.println("ImageFinder error: "+cause.toString());
				else System.out.println("findSubImages thread pool error: "+e.toString());
			}
			parent.setStatus("Finding tile images "+(++totalComplete)+"/"+(cellsWide*cellsHigh));
		}
		
		return subimages;
	}
	
	private BufferedImage combineSubImages(Color[][] colorProfile, URL[][] subImageURLs)
	{
		BufferedImage combined = new BufferedImage(mosaicWidth, mosaicHeight, BufferedImage.TYPE_INT_RGB);
		Graphics g = combined.getGraphics();
		
		float cellWidth = (float)combined.getWidth()/cellsWide;
		float cellHeight = (float)combined.getHeight()/cellsHigh;
		
		ThreadPoolExecutor threadpool = (ThreadPoolExecutor)Executors.newFixedThreadPool(threadcount);
		CompletionService<ImageLoaderResult> completionService = new ExecutorCompletionService<ImageLoaderResult>(threadpool);
		
		for (int x = 0; x < cellsWide; x++)
		{
			for (int y = 0; y < cellsHigh; y++)
			{
				completionService.submit(new ImageLoader(x, y, subImageURLs[x][y]));
			}
		}
		
		threadpool.shutdown();
		
		int totalComplete = 0;
		while (!threadpool.isTerminated() || totalComplete < threadpool.getTaskCount())
		{
			try
			{
				Future<ImageLoaderResult> thisFuture = completionService.poll(1, TimeUnit.SECONDS);
				if (thisFuture == null) continue;
				ImageLoaderResult loaderResult = thisFuture.get();
				int thisX = (int)Math.round(cellWidth*(loaderResult.x));
				int thisY = (int)Math.round(cellHeight*(loaderResult.y));
				int nextX = (int)Math.round(cellWidth*(loaderResult.x+1));
				int nextY = (int)Math.round(cellHeight*(loaderResult.y+1));
				if (loaderResult.image == null)
				{
					if (tileFillMode == FILLMODE_COLOR || tileFillMode == FILLMODE_IMAGE_COLOR) g.setColor(colorProfile[loaderResult.x][loaderResult.y]);
					else g.setColor(Color.black);
					g.fillRect(thisX, thisY, nextX-thisX, nextY-thisY);
				}
				else
				{
					g.drawImage(loaderResult.image, thisX, thisY, nextX-thisX, nextY-thisY, null);
					parent.setPreviewSubImage(loaderResult.x, loaderResult.y, loaderResult.image);
					loaderResult.image = null;
				}
			}
			catch (Exception e)
			{
				Throwable cause = e.getCause();
				if (cause != null) System.out.println("ImageLoader error: "+cause.toString());
				else System.out.println("combineSubImages thread pool error: "+e.toString());
			}
			parent.setStatus("Combining tile images "+(++totalComplete)+"/"+(cellsWide*cellsHigh));
		}
		
		return combined;
	}
}
