/*
 * Copyright 2014 PRImA Research Lab, University of Salford, United Kingdom
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

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.pdfbox.encoding.Encoding;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDTrueTypeFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.primaresearch.dla.page.MetaData;
import org.primaresearch.dla.page.Page;
import org.primaresearch.dla.page.layout.physical.ContentIterator;
import org.primaresearch.dla.page.layout.physical.shared.LowLevelTextType;
import org.primaresearch.dla.page.layout.physical.text.LowLevelTextObject;
import org.primaresearch.maths.geometry.Rect;

/**
 * 
 * @author Christian Clausner
 * @deprecated
 */
public class PageToPdfConverterUsingPdfBox {
	private boolean DEBUG = true;
	LowLevelTextType textLevel;
	private String ttfFontFilePath = null;
	private PDFont font = null;
	
	//Show stopper:
	// PDFBox does not support all Unicode characters. The result is a jumbled up text.
	
	/**
	 * Constructor
	 * @param textLevel Page content level from which to get the text (text lines, words, or glyphs) 
	 */
	public PageToPdfConverterUsingPdfBox(LowLevelTextType textLevel) {
		this.textLevel = textLevel;
	}

	public void convert(Collection<Page> pages, String targetPdf) {
		try{
			// Create a new empty document
			PDDocument document = new PDDocument();
			
			//Metadata (use first page)
			if (pages.size() > 0)
			addMetadata(document, pages.iterator().next());

			//Font
			createFont(document);
			
			//Add pages
			for (Iterator<Page> it = pages.iterator(); it.hasNext(); )
				addPage(document, it.next());

			// Save the newly created document
			document.save(targetPdf);

			// finally make sure that the document is properly
			// closed.
			document.close();
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}
	
	public void convert(Page page, String targetPdf) {
		try {
			// Create a new empty document
			PDDocument document = new PDDocument();
			
			//Metadata
			addMetadata(document, page);

			//Font
			createFont(document);

			//Add page
			addPage(document, page);
			
			// Save the newly created document
			document.save(targetPdf);
	
			// finally make sure that the document is properly
			// closed.
			document.close();
			
		} catch(Exception exc) {
			exc.printStackTrace();
		}
	}
	
	private void createFont(PDDocument document) throws IOException {
		
		if (ttfFontFilePath == null)
			font = PDType1Font.HELVETICA;
		else {
			font =  PDTrueTypeFont.loadTTF(document, ttfFontFilePath);
			Encoding enc = font.getFontEncoding();
			Map<Integer, String> map = enc.getCodeToNameMap();
			System.out.println("Font encoding map size: " + map.size());
		}
	}
	
	public void setDebug(boolean debug) {
		DEBUG = debug;
	}

	/**
	 * Use TTF font 
	 * @param ttfFontFilePath True Type font file
	 */
	public void setFontFilePath(String ttfFontFilePath) {
		this.ttfFontFilePath = ttfFontFilePath;
	}

	private void addPage(PDDocument doc, Page page) {
		try {
			// Create a new blank page and add it to the document

			//TODO Use image DPI and size
			//The measurement unit of the PDF is point (1 Point = 0.0352777778 cm)
			//For now: Set the PDF size to the PAGE size (1px = 1pt)
			//PDPage pdfPage = new PDPage(PDPage.PAGE_SIZE_A4); 
			PDPage pdfPage = new PDPage(new PDRectangle(page.getLayout().getWidth(), page.getLayout().getHeight()));
			doc.addPage( pdfPage );
			
			if (DEBUG) {
				System.out.println("Mediabox width: "+pdfPage.getMediaBox().getWidth());
				System.out.println("Mediabox height: "+pdfPage.getMediaBox().getHeight());
			}
			
	
			// Start a new content stream which will "hold" the to be created content
			PDPageContentStream contentStream = new PDPageContentStream(doc, pdfPage);
	
			try{
				addText(contentStream, page);
				// Define a text content stream using the selected font, moving the cursor and drawing the text "Hello World"
				//contentStream.beginText();
				//contentStream.setFont( font, 12 );
				//contentStream.moveTextPositionByAmount( 100, 700 );
				//contentStream.drawString( "Hello World" );
				//contentStream.endText();
			}
			finally {
				// Make sure that the content stream is closed:
				contentStream.close();
			}
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		
	}
	
	private void addText(PDPageContentStream contentStream, Page page) throws IOException {
		// Create a new font object selecting one of the PDF base fonts
		//PDFont font = PDType1Font.HELVETICA;
		
		int pageHeight = page.getLayout().getHeight();

		contentStream.beginText();

		for (ContentIterator it = page.getLayout().iterator(textLevel); it.hasNext(); ) {
			LowLevelTextObject textObj = (LowLevelTextObject)it.next();
			if (textObj != null && textObj.getText() != null && !textObj.getText().isEmpty()) {
				String text = textObj.getText();
				Rect boundingBox = textObj.getCoords().getBoundingBox();
				
				contentStream.setFont(font, calculateFontSize(text, boundingBox.getWidth(), boundingBox.getHeight()));
				contentStream.setTextTranslation(boundingBox.left, pageHeight - boundingBox.bottom);
				//contentStream.moveTextPositionByAmount( 100, 700 );
				contentStream.drawString(text);
			}
		}
		contentStream.endText();
	}
	
	private float calculateFontSize(String text, int width, int height) throws IOException {
		
		//float sw = font.getStringWidth(text);
		
		//float textHeight = font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * fontSize * 0.865;
		
		float fontSizeY = height * 1000.0f / (font.getFontDescriptor().getFontBoundingBox().getHeight() * 0.865f);
		float fontSizeX = width * 1000.0f / (font.getStringWidth(text) * 0.865f);
		
		if (fontSizeX <= 0.0f && fontSizeY <= 0.0f)
			return 12f;
		
		return Math.min(fontSizeX, fontSizeY);
	}
	
	private void addMetadata(PDDocument doc, Page page) {
		MetaData pageMetadata = page.getMetaData();

		PDDocumentInformation info = new PDDocumentInformation();
		
		//Creator
		if (pageMetadata.getCreator() != null && !pageMetadata.getCreator().isEmpty())
			info.setCreator(pageMetadata.getCreator());
		
		//Comments
		if (pageMetadata.getComments() != null && !pageMetadata.getComments().isEmpty())
			info.setCustomMetadataValue("Comments", pageMetadata.getComments());
		
		//TODO
		
		doc.setDocumentInformation(info);
	}

}
