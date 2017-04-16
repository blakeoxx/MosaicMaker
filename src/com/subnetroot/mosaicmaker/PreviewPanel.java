package com.subnetroot.mosaicmaker;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

class PreviewPanel extends JPanel
{
	private static final long serialVersionUID = -4960702664844676567L;	// Make Eclipse happy. We don't actually care about serialization
	
	private BufferedImage imgBaseImage;
	private int mosaicWidth;
	private int mosaicHeight;
	private boolean showGrid;
	private int gridCellsWide;
	private int gridCellsHigh;
	private Color[][] colorProfile;
	private BufferedImage[][] subImages;
	
	public PreviewPanel()
	{
		setBorder(BorderFactory.createLineBorder(Color.black));
		imgBaseImage = null;
		mosaicWidth = 1;
		mosaicHeight = 1;
		showGrid = true;
		gridCellsWide = 1;
		gridCellsHigh = 1;
		colorProfile = null;
		subImages = null;
	}
	
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		
		// Draw the background
		g.setColor(Color.gray);
		g.fillRect(0, 0, getWidth(), getHeight());
		
		if (imgBaseImage != null)
		{
			// Rescale the base image to fit in the panel with a preserved aspect ratio
			int imgWidth = mosaicWidth;
			int imgHeight = mosaicHeight;
			float widthRatio = (float)getWidth()/imgWidth;
			float heightRatio = (float)getHeight()/imgHeight;
			float aspectRatio;
			
			if (widthRatio < heightRatio) aspectRatio = widthRatio;
			else aspectRatio = heightRatio;
			int adjustedWidth = (int)Math.floor(imgWidth*aspectRatio);
			int adjustedHeight = (int)Math.floor(imgHeight*aspectRatio);
			
			// Draw the base image
			g.drawImage(imgBaseImage, 0, 0, adjustedWidth, adjustedHeight, null);
			
			if ((showGrid || colorProfile != null || subImages != null) && gridCellsWide > 0 && gridCellsHigh > 0)
			{
				// Draw the grid cells
				float cellWidth = (float)adjustedWidth/gridCellsWide;
				float cellHeight = (float)adjustedHeight/gridCellsHigh;
				int thisX = 0;
				for (int x = 0; x < gridCellsWide; x++)
				{
					int nextX = (int)Math.round(cellWidth*(x+1));
					int thisY = 0;
					for (int y = 0; y < gridCellsHigh; y++)
					{
						int nextY = (int)Math.round(cellHeight*(y+1));
						
						if (subImages != null && subImages[x][y] != null)
						{
							// Draw the subimage for this tile
							g.drawImage(subImages[x][y], thisX, thisY, nextX-thisX, nextY-thisY, null);
						}
						else if (colorProfile != null && colorProfile.length >= x && colorProfile[x].length >= y && colorProfile[x][y] != null)
						{
							// Draw the color profile for this tile
							g.setColor(colorProfile[x][y]);
							g.fillRect(thisX, thisY, nextX-thisX, nextY-thisY);
						}
						
						if (showGrid)
						{
							// Draw the cell border for this tile
							g.setColor(Color.red);
							g.drawRect(thisX, thisY, nextX-thisX, nextY-thisY);
						}
						
						thisY = nextY;
					}
					thisX = nextX;
				}
			}
		}
	}
	
	public void setBaseImage(BufferedImage newImg)
	{
		imgBaseImage = newImg;
		colorProfile = null;
		subImages = new BufferedImage[gridCellsWide][gridCellsHigh];
		repaint();
	}
	
	public void setMosaicSize(int mosaicWidth, int mosaicHeight)
	{
		this.mosaicWidth = mosaicWidth;
		this.mosaicHeight = mosaicHeight;
		repaint();
	}

	public void setShowGrid(boolean showGrid)
	{
		this.showGrid = showGrid;
		repaint();
	}

	public void setGridWidth(int gridCellsWide)
	{
		this.gridCellsWide = gridCellsWide;
		colorProfile = null;
		subImages = new BufferedImage[gridCellsWide][gridCellsHigh];
		repaint();
	}

	public void setGridHeight(int gridCellsHigh)
	{
		this.gridCellsHigh = gridCellsHigh;
		colorProfile = null;
		subImages = new BufferedImage[gridCellsWide][gridCellsHigh];
		repaint();
	}

	public void setColorProfile(Color[][] colorProfile)
	{
		this.colorProfile = colorProfile;
		repaint();
	}
	
	public void setSubImage(int x, int y, BufferedImage newImg)
	{
		if (subImages == null || x < 0 || x >= subImages.length || y < 0 || y >= subImages[x].length) return;
		subImages[x][y] = newImg;
		repaint();
	}
}
