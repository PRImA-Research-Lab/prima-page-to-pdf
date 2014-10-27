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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.primaresearch.dla.page.Page;
import org.primaresearch.dla.page.io.xml.PageXmlInputOutput;
import org.primaresearch.dla.page.layout.physical.shared.LowLevelTextType;

public class PageToPdfConverterTest {

	@Test
	public void test() {

		try {
			Page page = PageXmlInputOutput.readPage("e:\\temp\\debug\\00000259.xml");
			//Page page = new Page();
			
			PageToPdfConverter converter = new PageToPdfConverter(LowLevelTextType.TextLine,
																	true, false, true, false);
			converter.setFontFilePath("e:\\temp\\debug\\AletheiaSans.ttf");
			
			converter.convert(page, "e:\\temp\\debug\\00000259.png", "e:\\temp\\debug\\PdfFromPage.pdf");
		} catch(Exception exc) {
			exc.printStackTrace();
			fail();
		}
	}

	@Test
	public void testMultiplePages() {

		try {
			//Page page = new Page();
			
			PageToPdfConverter converter = new PageToPdfConverter(LowLevelTextType.TextLine,
																	true, false, true, false);
			converter.setFontFilePath("e:\\temp\\debug\\AletheiaSans.ttf");
			
			List<Page> pages = new ArrayList<Page>();  
			pages.add(PageXmlInputOutput.readPage("e:\\temp\\debug\\00000259.xml"));
			pages.add(PageXmlInputOutput.readPage("e:\\temp\\debug\\00000086.xml"));
			
			List<String> images = new ArrayList<String>();
			images.add("e:\\temp\\debug\\00000259.png");
			images.add("e:\\temp\\debug\\00000086.png");
			
			converter.convert(pages, images, "e:\\temp\\debug\\PdfFromPages.pdf");
		} catch(Exception exc) {
			exc.printStackTrace();
			fail();
		}
	}
}
