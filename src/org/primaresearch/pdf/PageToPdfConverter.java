/*
 * Copyright 2015 PRImA Research Lab, University of Salford, United Kingdom
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.primaresearch.dla.page.Page;
import org.primaresearch.dla.page.layout.physical.ContentIterator;
import org.primaresearch.dla.page.layout.physical.ContentObject;
import org.primaresearch.dla.page.layout.physical.shared.ContentType;
import org.primaresearch.dla.page.layout.physical.shared.LowLevelTextType;
import org.primaresearch.dla.page.layout.physical.shared.RegionType;
import org.primaresearch.dla.page.layout.physical.text.LowLevelTextObject;
import org.primaresearch.dla.page.layout.physical.text.TextObject;
import org.primaresearch.dla.page.layout.physical.text.impl.TextRegion;
import org.primaresearch.maths.geometry.Point;
import org.primaresearch.maths.geometry.Polygon;
import org.primaresearch.maths.geometry.Rect;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * PAGE to PDF Converter using iText library
 * http://itextpdf.com/
 * 
 * @author Christian Clausner
 *
 */
public class PageToPdfConverter {
	//private boolean DEBUG = true;
	ContentType textLevel;
	private String ttfFontFilePath = null;
	private BaseFont font;
	private boolean addRegionOutlines;
	private boolean addTextLineOutlines;
	private boolean addWordOutlines;
	private boolean addGlyphOutlines;

	/**
	 * Constructor
	 * @param textLevel Page content level from which to get the text (regions, text lines, words, or glyphs).
	 * Use <code>RegionType.TextRegion</code> for regions (blocks/zones), <code>LowLevelTextType....</code> otherwise.
	 * @param addRegionOutlines Add graphical overlay with polygonal region (zone) outlines
	 * @param addTextLineOutlines Add graphical overlay with polygonal text line outlines
	 * @param addWordOutlines Add graphical overlay with polygonal word outlines
	 * @param addGlyphOutlines Add graphical overlay with polygonal glyph (character) outlines
	 */
	public PageToPdfConverter(	ContentType textLevel, 
								boolean addRegionOutlines,
								boolean addTextLineOutlines,
								boolean addWordOutlines,
								boolean addGlyphOutlines) {
		this.textLevel = textLevel;
		this.addRegionOutlines = addRegionOutlines;
		this.addTextLineOutlines = addTextLineOutlines;
		this.addWordOutlines = addWordOutlines;
		this.addGlyphOutlines = addGlyphOutlines;
	}
	
	/**
	 * Converts a list of pages to PDF
	 * @param pages
	 * @param imageFiles
	 * @param targetPdf
	 */
	public void convert(List<Page> pages, List<String> imageFiles, String targetPdf) {

		Document document = null;
		try {
			PdfWriter writer = null;
		    
			//Add pages
		    createFont();
		    boolean addPageBreak = false;
			for (int i=0; i<pages.size(); i++) {
				if (document == null) {
					document = new Document(new Rectangle(pages.get(i).getLayout().getWidth(), pages.get(i).getLayout().getHeight()));
					writer = PdfWriter.getInstance(document, new FileOutputStream(targetPdf));
					document.open();
				}
				addPage(writer, document, pages.get(i), imageFiles.get(i), addPageBreak);
				addPageBreak = true;
			}
		} catch (Exception exc) {
			exc.printStackTrace();
		} finally {
		    document.close();
		}
	}

	/**
	 * Converts a single page to PDF
	 * @param page
	 * @param imageFile
	 * @param targetPdf
	 */
	public void convert(Page page, String imageFile, String targetPdf) {
		Document document = new Document(new Rectangle(page.getLayout().getWidth(), page.getLayout().getHeight()));
		try {
		    PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(targetPdf));
		    document.open();

		    createFont();
		    addPage(writer, document, page, imageFile, false);
		} catch (Exception exc) {
			exc.printStackTrace();
		} finally {
		    document.close();
		}
	}
	
	/**
	 * Creates the font that is to be used for the hidden text layer in the PDF.
	 */
	private void createFont() throws DocumentException, IOException  {
		
		//TODO Even with the 'NOT_EMBEDDED' settings it seems to embed the font!
		
		if (ttfFontFilePath == null)
			font = BaseFont.createFont(BaseFont.HELVETICA,
					BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
		else {
			font = FontFactory.getFont(ttfFontFilePath, BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED).getBaseFont();
				//PDTrueTypeFont.loadTTF(document, ttfFontFilePath);
			//Encoding enc = font.getFontEncoding();
			//Map<Integer, String> map = enc.getCodeToNameMap();
			//System.out.println("Font encoding map size: " + map.size());
		}
	}
	
	//public void setDebug(boolean debug) {
	//	DEBUG = debug;
	//}

	/**
	 * Use TTF font 
	 * @param ttfFontFilePath True Type font file
	 */
	public void setFontFilePath(String ttfFontFilePath) {
		this.ttfFontFilePath = ttfFontFilePath;
	}

	/**
	 * Adds a page to the PDF
	 * @param writer
	 * @param doc
	 * @param page
	 * @param imageFile
	 * @param addPageBreak
	 */
	private void addPage(PdfWriter writer, Document doc, Page page, String imageFile, boolean addPageBreak) {
		try {

			if (addPageBreak) {
				doc.setPageSize(new Rectangle(page.getLayout().getWidth(), page.getLayout().getHeight()));
				doc.newPage();
			}
			
			//TODO Use image DPI and size
			//The measurement unit of the PDF is point (1 Point = 0.0352777778 cm)
			//For now: Set the PDF size to the PAGE size (1px = 1pt)
			//PDPage pdfPage = new PDPage(PDPage.PAGE_SIZE_A4); 
			/*PDPage pdfPage = new PDPage(new PDRectangle(page.getLayout().getWidth(), page.getLayout().getHeight()));
			doc.addPage( pdfPage );
			
			if (DEBUG) {
				System.out.println("Mediabox width: "+pdfPage.getMediaBox().getWidth());
				System.out.println("Mediabox height: "+pdfPage.getMediaBox().getHeight());
			}
			
	
			// Start a new content stream which will "hold" the to be created content
			PDPageContentStream contentStream = new PDPageContentStream(doc, pdfPage);
			*/
			try{
				addText(writer, page);
				addImage(imageFile, writer, doc, page); //The images hides the text
				if (addRegionOutlines)
					addOutlines(writer, page, null);
				if (addTextLineOutlines)
					addOutlines(writer, page, LowLevelTextType.TextLine);
				if (addWordOutlines)
					addOutlines(writer, page, LowLevelTextType.Word);
				if (addGlyphOutlines)
					addOutlines(writer, page, LowLevelTextType.Glyph);
			}
			finally {
				// Make sure that the content stream is closed:
				//contentStream.close();
			}
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		
	}
	
	/**
	 * Adds the text of the given page to the current PDF page
	 * @param writer
	 * @param page
	 */
	private void addText(PdfWriter writer, Page page) {
		
		if (textLevel == null)
			return;
		
		int pageHeight = page.getLayout().getHeight();
		
		try {
			PdfContentByte cb = writer.getDirectContentUnder();
			cb.saveState();
			for (ContentIterator it = page.getLayout().iterator(textLevel); it.hasNext(); ) {
				ContentObject obj = it.next();
				if (obj == null || !(obj instanceof TextObject))
					continue;
				TextObject textObj = (TextObject)obj;

				if (textObj.getText() != null && !textObj.getText().isEmpty()) {
					
					List<String> strings = new ArrayList<String>();
					List<Rect> boxes = new ArrayList<Rect>();
					
					float fontSize = 1.0f;
					
					//Collect
					if (textObj instanceof LowLevelTextObject) {
						strings.add(textObj.getText());
						Rect boundingBox = obj.getCoords().getBoundingBox();
						boxes.add(boundingBox);
						fontSize = calculateFontSize(textObj.getText(), boundingBox.getWidth(), boundingBox.getHeight());
					} else {
						fontSize = splitTextRegion((TextRegion)obj, strings, boxes);
					}
					
					//Render
					for (int i=0; i<strings.size(); i++) {
						String text = strings.get(i);
						Rect boundingBox = boxes.get(i);

						//Calculate vertical transition (text is rendered at baseline -> descending bits are below the chosen position)
						int descent = (int)font.getDescentPoint(text, fontSize);
						int ascent = (int)font.getAscentPoint(text, fontSize);
						int textHeight = Math.abs(descent) + ascent;
						int transY = descent;
						
						if (textHeight < boundingBox.getHeight()) {
							transY = descent - (boundingBox.getHeight() - textHeight) / 2; 
						}
						
						cb.beginText();
						//cb.moveText(boundingBox.left, pageHeight - boundingBox.bottom);
						cb.setTextMatrix(boundingBox.left, pageHeight - boundingBox.bottom - transY);
						cb.setFontAndSize(font, fontSize);
						cb.showText(text);
						cb.endText();
						
						//Debug
						//cb.moveTo(boundingBox.left, pageHeight - boundingBox.bottom - transY);
						//cb.lineTo(boundingBox.right, pageHeight - boundingBox.bottom - transY);
						//cb.moveTo(boundingBox.left, pageHeight - boundingBox.bottom);
						//cb.lineTo(boundingBox.right, pageHeight - boundingBox.bottom);
					}
				}
			}
			cb.restoreState();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*private int calculateDescent(String text, float fontSize) {
		int maxDescent = 0;
		
		char[] chars = text.toCharArray();
		for (char c : chars) {
			int[] bb = font.getCharBBox(c);
			if (bb != null) {
				
				int descent = (int)(fontSize * Math.abs(bb[1]) / 1000f);
				if (descent > maxDescent)
					maxDescent = descent;
			}
		}
		
		return maxDescent;
	}*/
	
	/**
	 * Splits the given text region into lines
	 * @param reg region to be split
	 * @param strings (out) Target list for text line strings 
	 * @param boxes (out) Target list for text line bounding boxes
	 * @return The font size to be used for the whole region
	 * @throws IOException 
	 */
	private float splitTextRegion(TextRegion reg, List<String> strings, List<Rect> boxes) throws IOException {
		
		Rect regionBoundingBox = reg.getCoords().getBoundingBox();
		
		//Split text content into lines
		String regionText = reg.getText().replace("\r", "");
		String[] splitText = regionText.split("\n");
		
		int lineCount = splitText.length;
		if (lineCount == 1) {
			strings.add(regionText);
			boxes.add(regionBoundingBox);
			return calculateFontSize(regionText, regionBoundingBox.getWidth(), regionBoundingBox.getHeight());
		} 
		
		if(lineCount >= 2) {
			float minFontSize = 1000.0f;
			double lineHeight = (double)regionBoundingBox.getHeight() / (double)lineCount;
			for (int i=0; i<lineCount; i++) {
				strings.add(splitText[i]);
				
				Rect bb = new Rect(	regionBoundingBox.left,
									regionBoundingBox.top + (int)((double)i * lineHeight),
									regionBoundingBox.right,
									regionBoundingBox.top + (int)((double)(i+1) * lineHeight));
				
				float fontSize = calculateFontSize(splitText[i], bb.getWidth(), bb.getHeight());
				
				if (fontSize < minFontSize)
					minFontSize = fontSize;
				
				boxes.add(bb);
			}
			return minFontSize;
		}
		
		return 1.0f;
	}
	
	/**
	 * Adds the specified outlines of the given page to the current PDF page. 
	 * @param writer
	 * @param page
	 * @param type
	 */
	private void addOutlines(PdfWriter writer, Page page, ContentType type) {
		int pageHeight = page.getLayout().getHeight();
		
		try {
			PdfContentByte cb = writer.getDirectContent();
			cb.saveState();
			for (ContentIterator it = page.getLayout().iterator(type); it.hasNext(); ) {
				ContentObject contentObj = it.next();
				drawLayoutObject(contentObj, cb, pageHeight);
			}
			cb.restoreState();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Draws the outline of the given layout object on the current PDF page. 
	 * @param obj
	 * @param canvas
	 * @param pageHeight
	 */
	private void drawLayoutObject(ContentObject obj, PdfContentByte canvas, int pageHeight) {
		if (obj == null)
			return;
		
		Polygon polygon = obj.getCoords();
		
		if (polygon == null || polygon.getSize() < 3)
			return;
		
		canvas.setColorStroke(getOutlineColor(obj.getType()));
		canvas.setLineWidth(1.0f);
		
		//Move to last point
		Point p = polygon.getPoint(polygon.getSize()-1);
		canvas.moveTo(p.x, pageHeight-p.y);
		//Now draw all line segments
		for (int i=0; i<polygon.getSize(); i++) {
			p = polygon.getPoint(i);
			canvas.lineTo(p.x, pageHeight-p.y);
		}
		canvas.stroke();
	}
	
	/**
	 * Returns the correct stroke colour for the given layout object type (e.g. blue for text region).  
	 */
	private BaseColor getOutlineColor(ContentType type) {
		if (type == LowLevelTextType.TextLine)
			return new BaseColor(50, 205, 50);
		else if (type == LowLevelTextType.Word)
			return new BaseColor(178, 34, 34);
		else if (type == LowLevelTextType.Glyph)
			return new BaseColor(46, 139, 8);
		else if (type == RegionType.TextRegion)
			return new BaseColor(0, 0, 255);
		else if (type == RegionType.ChartRegion)
			return new BaseColor(128, 0, 128);
		else if (type == RegionType.GraphicRegion)
			return new BaseColor(0,128,0);
		else if (type == RegionType.ImageRegion)
			return new BaseColor(0,206,209);
		else if (type == RegionType.LineDrawingRegion)
			return new BaseColor(184, 134, 11);
		else if (type == RegionType.MathsRegion)
			return new BaseColor(0, 191, 255);
		else if (type == RegionType.NoiseRegion)
			return new BaseColor(255, 0, 0);
		else if (type == RegionType.SeparatorRegion)
			return new BaseColor(255, 0, 255);
		else if (type == RegionType.TableRegion)
			return new BaseColor(139, 69, 19);
		else if (type == RegionType.AdvertRegion)
			return new BaseColor(70, 130, 180);
		else if (type == RegionType.ChemRegion)
			return new BaseColor(255, 140,   0);
		else if (type == RegionType.MusicRegion)
			return new BaseColor(148,   0, 211);
		return BaseColor.BLUE;
	}

	/**
	 * Adds the document page image to the current PDF page (spanning the whole page). 
	 */
	private void addImage(String filepath, PdfWriter writer, Document doc, Page page) throws MalformedURLException, IOException, DocumentException {
		
		PdfContentByte cb = writer.getDirectContentUnder();
		cb.saveState();
		
		Image img = Image.getInstance(filepath);
		img.setAbsolutePosition(0f, 0f);
		//if (img.getScaledWidth() > 300 || img.getScaledHeight() > 300) {
			//img.scaleToFit(300, 300);
		//}
		img.scaleToFit(page.getLayout().getWidth(), page.getLayout().getHeight());

		cb.addImage(img);
		
		cb.restoreState();
		
	}
	
	/**
	 * Calculates the font size to fit the given text into the specified dimensions.
	 * @param text
	 * @param width
	 * @param height
	 * @return
	 * @throws IOException
	 */
	private float calculateFontSize(String text, int width, int height) throws IOException {
		
		float sw = font.getWidth(text);
		
		//float textHeight = font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * fontSize * 0.865;
		
		//float fontSizeY = height * 1000.0f / (font.get().getFontBoundingBox().getHeight() * 0.865f);
		float fontSizeX = width * 1000.0f / (sw * 0.865f);
		
		//Validate and reduce font size until it fits
		Chunk chunk = new Chunk(text, new Font(font, fontSizeX));;
		while (chunk.getWidthPoint() > width) {
			fontSizeX -= 0.5f;
			chunk = new Chunk(text, new Font(font, fontSizeX));
		}
		
		//if (fontSizeX <= 0.0f && fontSizeY <= 0.0f)
		//	return 12f;
		
		//return Math.min(fontSizeX, fontSizeY);
		return fontSizeX;
	}
	
	//private void addMetadata(PDDocument doc, Page page) {
	//	MetaData pageMetadata = page.getMetaData();

		
	//}
}
