/*
 * Copyright 2019 PRImA Research Lab, University of Salford, United Kingdom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.primaresearch.pdf;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.primaresearch.dla.page.Page;
import org.primaresearch.dla.page.io.xml.PageXmlInputOutput;
import org.primaresearch.dla.page.layout.physical.shared.ContentType;
import org.primaresearch.dla.page.layout.physical.shared.LowLevelTextType;
import org.primaresearch.dla.page.layout.physical.shared.RegionType;

/**
 * Command line interface for PAGE to PDF converter
 * 
 * @author Christian Clausner
 *
 */
public class CommandLineTool {

	/**
	 * Main function
	 * @param args Arguments (see function <code>showUsage()</code> for help)
	 */
	public static void main(String[] args) {
		if (args.length == 0) {
			showUsage();
			return;
		}

		try {
			//Parse arguments
			String xmlSource = null;
			String imageSource = null;
			String pdfFilename = null;
			String textSource = null;
			String outlines = null;
			String fontFile = null;
			for (int i=0; i<args.length; i++) {
				if ("-xml".equals(args[i])) {
					i++;
					xmlSource = args[i];
				}
				else if ("-image".equals(args[i])) {
					i++;
					imageSource = args[i];
				}
				else if ("-pdf".equals(args[i])) {
					i++;
					pdfFilename = args[i];
				}
				else if ("-text-source".equals(args[i])) {
					i++;
					textSource = args[i];
				}
				else if ("-outlines".equals(args[i])) {
					i++;
					outlines = args[i];
				}
				else if ("-font".equals(args[i])) {
					i++;
					fontFile = args[i];
				}
				else {
					System.err.println("Unknown argument: "+args[i]);
				}
			}
			
			//Text source
			ContentType textSourceType = null;
			if (textSource != null) {
				textSource = textSource.toLowerCase();
				if (textSource.equals("r"))
					textSourceType = RegionType.TextRegion;
				else if (textSource.equals("l"))
					textSourceType = LowLevelTextType.TextLine;
				else if (textSource.equals("w"))
					textSourceType = LowLevelTextType.Word;
				else if (textSource.equals("g"))
					textSourceType = LowLevelTextType.Glyph;
			}
			
			//Outlines to render
			boolean addRegionOutlines = false;
			boolean addTextLineOutlines = false;
			boolean addWordOutlines = false;
			boolean addGlyphOutlines = false;
			if (outlines != null) {
				outlines = outlines.toLowerCase();
				addRegionOutlines = outlines.contains("r");
				addTextLineOutlines = outlines.contains("l");
				addWordOutlines = outlines.contains("w");
				addGlyphOutlines = outlines.contains("g");
			}
			
			//Load page file
			if (xmlSource == null) {
				System.err.println("PAGE XML file / folder not specified!");
				return;
			}
			File xmlSourceFile = new File(xmlSource);
			if (!xmlSourceFile.exists()) {
				System.err.println("PAGE XML file / folder does not exist!");
				return;
			}
			
			//Check image
			if (imageSource == null) {
				System.err.println("Image file / folder not specified!");
				return;
			}
			
			//Check target file path
			if (pdfFilename == null) {
				System.err.println("Output file not specified!");
				return;
			}
			
			PageToPdfConverter converter = new PageToPdfConverter(	textSourceType, 
																	addRegionOutlines, 
																	addTextLineOutlines, 
																	addWordOutlines,
																	addGlyphOutlines);
			//Font
			if (fontFile != null)
				converter.setFontFilePath(fontFile);
			
			//Convert
			// Multiple files
			if (xmlSourceFile.isDirectory()) {
				//Page XMLs
				File[] xmlFiles = xmlSourceFile.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.toLowerCase().endsWith(".xml");
					}
				});
				List<Page> pages = new ArrayList<Page>();
				List<String> images = new ArrayList<String>();
				for (File f : xmlFiles) {
					pages.add(PageXmlInputOutput.readPage(f.getAbsolutePath()));
					//Image
					String path = imageSource + File.separator + f.getName();
					if (new File(path.toLowerCase().replace(".xml", ".tif")).exists())
						images.add(path.substring(0, path.length()-4) + ".tif");
					else if (new File(path.toLowerCase().replace(".xml", ".png")).exists())
						images.add(path.substring(0, path.length()-4) + ".png");
					else if (new File(path.toLowerCase().replace(".xml", ".jpg")).exists())
						images.add(path.substring(0, path.length()-4) + ".jpg");
				}
				
				converter.convert(pages, images, pdfFilename);
			} 
			// Single file
			else 
			{ 
				Page page = PageXmlInputOutput.readPage(xmlSource);
				converter.convert(page, imageSource, pdfFilename);
			}
			
		} catch(Exception exc) {
			exc.printStackTrace();
		}
	}

	/**
	 * Prints usage help to stdout
	 */
	private static void showUsage() {
		System.out.println("PAGE to PDF Converter");
		System.out.println("");
		System.out.println("PRImA Research Lab, University of Salford, UK");
		System.out.println("");
		System.out.println("Arguments:");
		System.out.println("");
		System.out.println("  -xml <XML file|folder>     Single PAGE XML file to convert or");
		System.out.println("                             a folder with multiple XML files.");
		System.out.println("");
		System.out.println("  -image <image file|folder> Single document image (.tif, .png, .jpg) or");
		System.out.println("                             a folder with multiple images (the filenames");
		System.out.println("                             have to match the filenames of the XMLs).");
		System.out.println("");
		System.out.println("  -pdf <PDF file>         Output PDF file.");
		System.out.println("");
		System.out.println("  -text-source <R|L|W|G>  Optional. Add hidden text layer, using text from:");
		System.out.println("                            Text region objects  R");
		System.out.println("                            Text line objects    T");
		System.out.println("                            Word objects         W");
		System.out.println("                            Glyph objects        G");
		System.out.println("");
		System.out.println("  -outlines <R|L|W|G>     Optional. Add layer with object outlines.");
		System.out.println("                          One or a combination of (no spaces)):");
		System.out.println("                              Regions     R");
		System.out.println("                              Text lines  T");
		System.out.println("                              Words       W");
		System.out.println("                              Glyphs      G");
		System.out.println("");
		System.out.println("  -font <TTF file>        Optional. TrueType font to be used.");
		System.out.println("                          See included font 'data/AletheiaSans.ttf' ");
		System.out.println("");
	}

}
