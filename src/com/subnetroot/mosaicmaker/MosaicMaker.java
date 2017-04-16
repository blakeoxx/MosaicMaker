package com.subnetroot.mosaicmaker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.NumberFormatter;

public class MosaicMaker
{
	private JFrame frame;
	private JTextField txtBaseImage;
	private JButton btnChooseBaseImage;
	private JSpinner spinTileWidth;
	private SpinnerNumberModel spinmodelTileWidth;
	private JSpinner spinTileHeight;
	private SpinnerNumberModel spinmodelTileHeight;
	private JButton btnStartProcessing;
	private JFormattedTextField numMosaicWidth;
	private JFormattedTextField numMosaicHeight;
	private JCheckBox chkboxLockMosaicSizeRatio;
	private JSpinner spinThreadCount;
	private SpinnerNumberModel spinmodelThreadCount;
	private JCheckBox chkboxUseTileColorFill;
	private JButton btnSaveMosaic;
	private PreviewPanel panelPreview;
	private JLabel labelStatus;
	
	private File fBaseImage;
	private BufferedImage imgBaseImage;
	private BufferedImage imgCompletedMosaic;
	
	public MosaicMaker()
	{
		fBaseImage = null;
		imgBaseImage = null;
		imgCompletedMosaic = null;
		
		setupWindow();
	}
	
	public static void main(String[] args)
	{
		new MosaicMaker();
	}
	
	private void setupWindow()
	{
		try
		{
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		}
		catch (Exception e){ }
		
		frame = new JFrame("Mosaic Maker by SNR");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(5, 5, 5, 5);
		
		c.weightx = 0;
		c.weighty = 0;
		
		// A label we can set up and add as needed
		JLabel label;
		
		// Row 1
		c.gridx = 0;
		c.gridy = 0;
		
		label = new JLabel("Base Image:");
		frame.add(label, c);
		c.gridx += c.gridwidth;
		
		txtBaseImage = new JTextField();
		txtBaseImage.setEditable(false);
		c.weightx = 1;
		c.gridwidth = 4;
		frame.add(txtBaseImage, c);
		c.gridx += c.gridwidth;
		c.weightx = 0;
		c.gridwidth = 1;
		
		btnChooseBaseImage = new JButton("Browse...");
		btnChooseBaseImage.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent ev){ selectBaseImage(); }});
		frame.add(btnChooseBaseImage, c);
		c.gridx += c.gridwidth;
		
		// Row 2
		c.gridx = 0;
		c.gridy = 1;
		
		label = new JLabel("Tile Width:");
		frame.add(label, c);
		c.gridx += c.gridwidth;
		
		spinmodelTileWidth = new SpinnerNumberModel(1, 1, 100, 1);
		spinmodelTileWidth.addChangeListener(new ChangeListener(){ public void stateChanged(ChangeEvent e){ panelPreview.setGridWidth(spinmodelTileWidth.getNumber().intValue()); }});
		spinTileWidth = new JSpinner(spinmodelTileWidth);
		spinTileWidth.setToolTipText("How many subimages the base image should be cut into width-wise");
		c.fill = GridBagConstraints.NONE;
		frame.add(spinTileWidth, c);
		c.gridx += c.gridwidth;
		c.fill = GridBagConstraints.HORIZONTAL;
		
		label = new JLabel("Tile Height:");
		frame.add(label, c);
		c.gridx += c.gridwidth;
		
		spinmodelTileHeight = new SpinnerNumberModel(1, 1, 100, 1);
		spinmodelTileHeight.addChangeListener(new ChangeListener(){ public void stateChanged(ChangeEvent e){ panelPreview.setGridHeight(spinmodelTileHeight.getNumber().intValue()); }});
		spinTileHeight = new JSpinner(spinmodelTileHeight);
		spinTileHeight.setToolTipText("How many subimages the base image should be cut into length-wise");
		c.fill = GridBagConstraints.NONE;
		frame.add(spinTileHeight, c);
		c.gridx += c.gridwidth;
		c.fill = GridBagConstraints.HORIZONTAL;
		
		JCheckBox chkboxShowGrid = new JCheckBox("Show tile grid", true);
		chkboxShowGrid.setToolTipText("Draws grid lines in the image preview representing where the subimages will be placed. These will not appear on the actual generated mosaic");
		chkboxShowGrid.addItemListener(new ItemListener(){ public void itemStateChanged(ItemEvent e){ panelPreview.setShowGrid(((JCheckBox)e.getItemSelectable()).isSelected()); }});
		frame.add(chkboxShowGrid, c);
		c.gridx += c.gridwidth;
		
		btnStartProcessing = new JButton("Make Mosaic");
		btnStartProcessing.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent ev){ startProcessing(); }});
		btnStartProcessing.setEnabled(false);
		frame.add(btnStartProcessing, c);
		c.gridx += c.gridwidth;
		
		// Row 3
		c.gridx = 0;
		c.gridy = 2;
		
		label = new JLabel("Mosaic Size:");
		frame.add(label, c);
		c.gridx += c.gridwidth;
		
		// Set up the formatter for the mosaic dimensions inputs
		NumberFormatter mosaicSizeFormatter = new NumberFormatter();
		mosaicSizeFormatter.setValueClass(Integer.class);
		mosaicSizeFormatter.setMinimum(1);
		mosaicSizeFormatter.setMaximum(5000);
		mosaicSizeFormatter.setAllowsInvalid(false);
		
		numMosaicWidth = new JFormattedTextField(mosaicSizeFormatter);
		numMosaicWidth.setToolTipText("The width of the generated mosaic image (in pixels)");
		numMosaicWidth.setValue(1);
		numMosaicWidth.setColumns(4);
		numMosaicWidth.addPropertyChangeListener("value", new PropertyChangeListener(){ public void propertyChange(PropertyChangeEvent e){ checkMosaicSize((int)numMosaicWidth.getValue(), (int)numMosaicHeight.getValue(), false); panelPreview.setMosaicSize((int)numMosaicWidth.getValue(), (int)numMosaicHeight.getValue()); }});
		
		numMosaicHeight = new JFormattedTextField(mosaicSizeFormatter);
		numMosaicHeight.setToolTipText("The height of the generated mosaic image (in pixels)");
		numMosaicHeight.setValue(1);
		numMosaicHeight.setColumns(4);
		numMosaicHeight.addPropertyChangeListener("value", new PropertyChangeListener(){ public void propertyChange(PropertyChangeEvent e){ checkMosaicSize((int)numMosaicWidth.getValue(), (int)numMosaicHeight.getValue(), true); panelPreview.setMosaicSize((int)numMosaicWidth.getValue(), (int)numMosaicHeight.getValue()); }});
		
		chkboxLockMosaicSizeRatio = new JCheckBox("Lock aspect ratio", true);
		chkboxLockMosaicSizeRatio.addItemListener(new ItemListener(){ public void itemStateChanged(ItemEvent e){ checkMosaicSize((int)numMosaicWidth.getValue(), (int)numMosaicHeight.getValue(), false); panelPreview.setMosaicSize((int)numMosaicWidth.getValue(), (int)numMosaicHeight.getValue()); }});
		
		JPanel mosaicSizePanel = new JPanel(new BorderLayout(5, 0));
		mosaicSizePanel.add(numMosaicWidth, BorderLayout.WEST);
		mosaicSizePanel.add(new JLabel("x"), BorderLayout.CENTER);
		mosaicSizePanel.add(numMosaicHeight, BorderLayout.EAST);
		mosaicSizePanel.add(chkboxLockMosaicSizeRatio, BorderLayout.SOUTH);
		c.gridwidth = 2;
		c.fill = GridBagConstraints.NONE;
		frame.add(mosaicSizePanel, c);
		c.gridx += c.gridwidth;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		
		label = new JLabel("Threads:");
		frame.add(label, c);
		c.gridx += c.gridwidth;
		
		spinmodelThreadCount = new SpinnerNumberModel(15, 1, 100, 1);
		spinThreadCount = new JSpinner(spinmodelThreadCount);
		spinThreadCount.setToolTipText("How many processing threads to use for subimage downloading. The more threads, the less time it takes to generate a mosaic. May slow down other programs");
		c.fill = GridBagConstraints.NONE;
		frame.add(spinThreadCount, c);
		c.gridx += c.gridwidth;
		c.fill = GridBagConstraints.HORIZONTAL;
		
		btnSaveMosaic = new JButton("Save Mosaic...");
		btnSaveMosaic.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent ev){ saveMosaic(imgCompletedMosaic); }});
		btnSaveMosaic.setEnabled(false);
		frame.add(btnSaveMosaic, c);
		c.gridx += c.gridwidth;
		
		// Row 4
		c.gridx = 0;
		c.gridy = 3;
		
		chkboxUseTileColorFill = new JCheckBox("Fill missing subimages with tile color", true);
		c.gridwidth = 3;
		frame.add(chkboxUseTileColorFill, c);
		c.gridx += c.gridwidth;
		c.gridwidth = 1;
		
		// Row 5
		c.gridx = 0;
		c.gridy = 4;
		
		panelPreview = new PreviewPanel();
		panelPreview.setMinimumSize(new Dimension(400, 300));
		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = 6;
		c.fill = GridBagConstraints.BOTH;
		frame.add(panelPreview, c);
		c.gridx += c.gridwidth;
		c.weightx = 0;
		c.weighty = 0;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		
		// Row 6
		c.gridx = 0;
		c.gridy = 5;
		
		labelStatus = new JLabel("Ready");
		c.gridwidth = 6;
		frame.add(labelStatus, c);
		c.gridx += c.gridwidth;
		c.gridwidth = 1;
		
		frame.pack();
		Dimension minsize = frame.getMinimumSize();
		frame.setMinimumSize(minsize);
		frame.setSize(minsize);
		frame.setVisible(true);
	}
	
	private void selectBaseImage()
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogType(JFileChooser.OPEN_DIALOG);
		
		chooser.addChoosableFileFilter(new FileNameExtensionFilter("Bitmap (*.bmp)", "bmp"));
		chooser.addChoosableFileFilter(new FileNameExtensionFilter("GIF Image (*.gif)", "gif"));
		chooser.addChoosableFileFilter(new FileNameExtensionFilter("JPEG Image (*.jpg, *.jpeg)", "jpg", "jpeg"));
		chooser.addChoosableFileFilter(new FileNameExtensionFilter("PNG Image (*.png)", "png"));
		
		if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
		{
			fBaseImage = chooser.getSelectedFile();
			txtBaseImage.setText(fBaseImage.getAbsolutePath());
			try
			{
				imgBaseImage = ImageIO.read(fBaseImage);
				if (imgBaseImage == null) throw new Exception("Image format unsupported");
				
				panelPreview.setBaseImage(imgBaseImage);
				numMosaicWidth.setValue(imgBaseImage.getWidth());
				numMosaicHeight.setValue(imgBaseImage.getHeight());
				btnStartProcessing.setEnabled(true);
			}
			catch (Exception e)
			{
				if (e instanceof IOException) setStatus("Could not open file "+fBaseImage.getName());
				else setStatus("File is not a supported image format");
				
				fBaseImage = null;
				txtBaseImage.setText("");
				imgBaseImage = null;
				panelPreview.setBaseImage(null);
				btnStartProcessing.setEnabled(false);
			}
		}
	}
	
	private void startProcessing()
	{
		if (fBaseImage == null || imgBaseImage == null) return;
		
		// Lock GUI components
		btnChooseBaseImage.setEnabled(false);
		spinTileWidth.setEnabled(false);
		spinTileHeight.setEnabled(false);
		btnStartProcessing.setEnabled(false);
		numMosaicWidth.setEnabled(false);
		numMosaicHeight.setEnabled(false);
		spinThreadCount.setEnabled(false);
		chkboxUseTileColorFill.setEnabled(false);
		btnSaveMosaic.setEnabled(false);
		
		setStatus("Processing...");
		imgCompletedMosaic = null;
		ImageProcessingThread t = new ImageProcessingThread(this, spinmodelTileWidth.getNumber().intValue(), spinmodelTileHeight.getNumber().intValue(), imgBaseImage, spinmodelThreadCount.getNumber().intValue(), (int)numMosaicWidth.getValue(), (int)numMosaicHeight.getValue(), chkboxUseTileColorFill.isSelected());
		t.start();
	}
	
	protected void endProcessing(BufferedImage mosaic)
	{
		setStatus("Done");
		imgCompletedMosaic = mosaic;
		saveMosaic(mosaic);
		
		// Unlock GUI components
		btnChooseBaseImage.setEnabled(true);
		spinTileWidth.setEnabled(true);
		spinTileHeight.setEnabled(true);
		btnStartProcessing.setEnabled(true);
		numMosaicWidth.setEnabled(true);
		numMosaicHeight.setEnabled(true);
		spinThreadCount.setEnabled(true);
		chkboxUseTileColorFill.setEnabled(true);
		btnSaveMosaic.setEnabled(true);
	}
	
	private void saveMosaic(BufferedImage mosaic)
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogType(JFileChooser.SAVE_DIALOG);
		chooser.setAcceptAllFileFilterUsed(false);
		
		// Accepted file formats for saving. Key should be a file format name recognized by ImageIO. Value should be the FileFilter for the chooser to use
		Map<String, FileNameExtensionFilter> acceptedFormats = new LinkedHashMap<String, FileNameExtensionFilter>();
		acceptedFormats.put("BMP", new FileNameExtensionFilter("Bitmap (*.bmp)", "bmp"));
		acceptedFormats.put("GIF", new FileNameExtensionFilter("GIF Image (*.gif)", "gif"));
		acceptedFormats.put("JPG", new FileNameExtensionFilter("JPEG Image (*.jpg, *.jpeg)", "jpg", "jpeg"));
		acceptedFormats.put("PNG", new FileNameExtensionFilter("PNG Image (*.png)", "png"));
		
		// Add the file formats to the chooser
		for (FileNameExtensionFilter thisFilter : acceptedFormats.values())
		{
			chooser.addChoosableFileFilter(thisFilter);
		}
		
		if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION)
		{
			File target = chooser.getSelectedFile();
			try
			{
				// Find the image writer format based on the file filter (default to PNG format)
				FileNameExtensionFilter chosenFilter = (FileNameExtensionFilter)chooser.getFileFilter();
				String outputFormatName = "PNG";
				for (Map.Entry<String, FileNameExtensionFilter> thisFilter : acceptedFormats.entrySet())
				{
					if (thisFilter.getValue() == chosenFilter){ outputFormatName = thisFilter.getKey(); break; }
				}
				
				// Save using the image writer format
				if (!ImageIO.write(mosaic, outputFormatName, target)) throw new Exception("No image writer for the selected format. Please choose a different format");
				setStatus("Saved mosaic to "+target.getPath());
			}
			catch (Exception e)
			{
				System.out.println("Error saving mosaic: "+e.toString());
				setStatus("Error saving mosaic: "+e.toString());
			}
		}
	}
	
	protected void checkMosaicSize(int width, int height, boolean whichChanged)
	{
		if (chkboxLockMosaicSizeRatio.isSelected())
		{
			// Enforce aspect ratio
			float aspectRatio = (float)imgBaseImage.getWidth()/imgBaseImage.getHeight();
			
			if (whichChanged == false)
			{
				// Width changed, so adjust height
				int newHeight;
				if (width == imgBaseImage.getWidth()) newHeight = imgBaseImage.getHeight();
				else newHeight = Math.max(Math.round(width/aspectRatio), 1);
				
				if (newHeight != height) numMosaicHeight.setValue(newHeight);
			}
			else
			{
				// Height changed, so adjust width
				int newWidth;
				if (height == imgBaseImage.getHeight()) newWidth = imgBaseImage.getWidth();
				else newWidth = Math.max(Math.round(height*aspectRatio), 1);
				
				if (newWidth != width) numMosaicWidth.setValue(newWidth);
			}
		}
	}
	
	private void callFromSwingThread(Runnable r)
	{
		if (SwingUtilities.isEventDispatchThread()) r.run();
		else SwingUtilities.invokeLater(r);
	}
	
	protected synchronized void setStatus(final String status)
	{
		callFromSwingThread(new Runnable() {
			public void run() {
				labelStatus.setText(status);
			}
		});
	}
	
	protected synchronized void setPreviewColorProfile(final Color[][] colorProfile)
	{
		callFromSwingThread(new Runnable() {
			public void run() {
				panelPreview.setColorProfile(colorProfile);
			}
		});
	}
	
	protected synchronized void setPreviewSubImage(final int x, final int y, final BufferedImage newImg)
	{
		callFromSwingThread(new Runnable() {
			public void run() {
				panelPreview.setSubImage(x, y, newImg);
			}
		});
	}
}
