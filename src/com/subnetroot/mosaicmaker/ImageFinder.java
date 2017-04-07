package com.subnetroot.mosaicmaker;

import java.awt.Color;
import java.net.URI;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.Callable;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

public class ImageFinder implements Callable<ImageFinderResult>
{
	int x;
	int y;
	CloseableHttpClient httpclient;
	URIBuilder partialuri;
	Color color;
	
	public ImageFinder(int x, int y, CloseableHttpClient httpclient, URIBuilder partialuri, Color color)
	{
		this.x = x;
		this.y = y;
		this.httpclient = httpclient;
		this.partialuri = partialuri;
		this.color = color;
	}
	
	public ImageFinderResult call() throws Exception
	{
		ImageFinderResult result = new ImageFinderResult(x, y, null);
		try
		{
			result.imageUrl = findImageByColor();
		}
		finally
		{
			httpclient.close();
			httpclient = null;
		}
		return result;
	}
	
	private URL findImageByColor() throws Exception
	{
		URI uri;
		HttpGet request = null;
		CloseableHttpResponse response = null;
		HttpEntity entity = null;
		
		partialuri.setParameter("limit", "1")
			.setParameter("offset", ""+(new Random()).nextInt(20))
			.setParameter("return_metadata", "<photoID/>")
			.setParameter("colors[0]", 
					(color.getRed()<0x10?"0":"")+Integer.toHexString(color.getRed())+
					(color.getGreen()<0x10?"0":"")+Integer.toHexString(color.getGreen())+
					(color.getBlue()<0x10?"0":"")+Integer.toHexString(color.getBlue()) )
			.setParameter("weights[0]", "100");
		
		try
		{
			try
			{
				uri = partialuri.build();
			}
			catch (Exception e)
			{
				throw new Exception("Error building URI: "+e.toString());
			}
			
			request = new HttpGet(uri);
			try
			{
				response = httpclient.execute(request);
			}
			catch (Exception e)
			{
				throw new Exception("Error making HTTP request: "+e.toString());
			}
			
			entity = response.getEntity();
			
			if (response.getStatusLine().getStatusCode() != 200) throw new Exception("Status code not 200 ("+response.getStatusLine().getStatusCode()+")");
			else if (entity == null) throw new Exception("Response entity null");
			
			URL imageurl = null;
			try
			{
				JSONObject json = new JSONObject(EntityUtils.toString(entity));
				String fpath = json.getJSONArray("result")
						.getJSONObject(0)
						.getString("filepath");
				imageurl = new URL("http://img.tineye.com/flickr-images/?filepath=labs-flickr-public/images/"+fpath);
			}
			catch (Exception e)
			{
				throw new Exception("Malformed JSON");
			}
			
			return imageurl;
		}
		finally
		{
			if (entity != null){ entity.getContent().close(); entity = null; }
			if (response != null){ response.close(); response = null; }
			if (request != null){ request.releaseConnection(); request = null; }
		}
	}
}
