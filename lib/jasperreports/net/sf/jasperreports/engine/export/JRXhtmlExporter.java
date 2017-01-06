/*
 * JasperReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2016 TIBCO Software Inc. All rights reserved.
 * http://www.jaspersoft.com
 *
 * Unless you have purchased a commercial license agreement from Jaspersoft,
 * the following license terms apply:
 *
 * This program is part of JasperReports.
 *
 * JasperReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JasperReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JasperReports. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Contributors:
 * Alex Parfenov - aparfeno@users.sourceforge.net
 * Adrian Jackson - iapetus@users.sourceforge.net
 * David Taylor - exodussystems@users.sourceforge.net
 * Lars Kristensen - llk@users.sourceforge.net
 */

package net.sf.jasperreports.engine.export;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.font.TextAttribute;
import java.awt.geom.Dimension2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.tools.codec.Base64Encoder;

import net.sf.jasperreports.components.headertoolbar.HeaderToolbarElement;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.ImageMapRenderable;
import net.sf.jasperreports.engine.JRBoxContainer;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRGenericPrintElement;
import net.sf.jasperreports.engine.JRLineBox;
import net.sf.jasperreports.engine.JRPen;
import net.sf.jasperreports.engine.JRPrintElement;
import net.sf.jasperreports.engine.JRPrintElementIndex;
import net.sf.jasperreports.engine.JRPrintEllipse;
import net.sf.jasperreports.engine.JRPrintFrame;
import net.sf.jasperreports.engine.JRPrintGraphicElement;
import net.sf.jasperreports.engine.JRPrintHyperlink;
import net.sf.jasperreports.engine.JRPrintHyperlinkParameter;
import net.sf.jasperreports.engine.JRPrintImage;
import net.sf.jasperreports.engine.JRPrintImageArea;
import net.sf.jasperreports.engine.JRPrintImageAreaHyperlink;
import net.sf.jasperreports.engine.JRPrintLine;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JRPrintRectangle;
import net.sf.jasperreports.engine.JRPrintText;
import net.sf.jasperreports.engine.JRPropertiesUtil;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.PrintPageFormat;
import net.sf.jasperreports.engine.base.JRBasePrintText;
import net.sf.jasperreports.engine.type.HyperlinkTypeEnum;
import net.sf.jasperreports.engine.type.LineDirectionEnum;
import net.sf.jasperreports.engine.type.LineSpacingEnum;
import net.sf.jasperreports.engine.type.LineStyleEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.RotationEnum;
import net.sf.jasperreports.engine.type.RunDirectionEnum;
import net.sf.jasperreports.engine.type.ScaleImageEnum;
import net.sf.jasperreports.engine.util.JRBoxUtil;
import net.sf.jasperreports.engine.util.JRColorUtil;
import net.sf.jasperreports.engine.util.JRStringUtil;
import net.sf.jasperreports.engine.util.JRStyledText;
import net.sf.jasperreports.engine.util.JRTextAttribute;
import net.sf.jasperreports.engine.util.JRTypeSniffer;
import net.sf.jasperreports.engine.util.Pair;
import net.sf.jasperreports.export.ExportInterruptedException;
import net.sf.jasperreports.export.ExporterInputItem;
import net.sf.jasperreports.export.HtmlExporterConfiguration;
import net.sf.jasperreports.export.HtmlReportConfiguration;
import net.sf.jasperreports.export.WriterExporterOutput;
import net.sf.jasperreports.renderers.AreaHyperlinksRenderable;
import net.sf.jasperreports.renderers.DataRenderable;
import net.sf.jasperreports.renderers.DimensionRenderable;
import net.sf.jasperreports.renderers.Renderable;
import net.sf.jasperreports.renderers.RenderersCache;
import net.sf.jasperreports.renderers.ResourceRenderer;
import net.sf.jasperreports.renderers.util.RendererUtil;


/**
 * Exports a JasperReports document to XHTML format.

 * @deprecated Replaced by {@link HtmlExporter}.
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class JRXhtmlExporter extends AbstractHtmlExporter<HtmlReportConfiguration, HtmlExporterConfiguration>
{
	private static final Log log = LogFactory.getLog(JRXhtmlExporter.class);
	
	private static final String XHTML_EXPORTER_PROPERTIES_PREFIX = JRPropertiesUtil.PROPERTY_PREFIX + "export.xhtml.";

	public static final String PROPERTY_IGNORE_HYPERLINK = XHTML_EXPORTER_PROPERTIES_PREFIX + JRPrintHyperlink.PROPERTY_IGNORE_HYPERLINK_SUFFIX;//FIXMEEXPORT can we do something about it?

	/**
	 * The exporter key, as used in
	 * {@link GenericElementHandlerEnviroment#getElementHandler(net.sf.jasperreports.engine.JRGenericElementType, String)}.
	 */
	public static final String XHTML_EXPORTER_KEY = JRPropertiesUtil.PROPERTY_PREFIX + "xhtml";

	protected class ExporterContext extends BaseExporterContext implements JRHtmlExporterContext
	{
		@Override
		public String getExportPropertiesPrefix()
		{
			return XHTML_EXPORTER_PROPERTIES_PREFIX;
		}

		@Override
		public String getHyperlinkURL(JRPrintHyperlink link)
		{
			return JRXhtmlExporter.this.getHyperlinkURL(link);
		}
	}

	/**
	 *
	 */
	protected Writer writer;
	protected Map<String,String> rendererToImagePathMap;
	protected Map<Pair<String,Rectangle>,String> imageMaps;
	protected RenderersCache renderersCache;
	
	protected int reportIndex;
	protected int pageIndex;
	protected List<FrameInfo> frameInfoStack;
	protected int elementIndex;
	protected int topLimit;
	protected int leftLimit;
	protected int rightLimit;
	protected int bottomLimit;

	protected JRHyperlinkTargetProducerFactory targetProducerFactory;		

	protected boolean hyperlinkStarted;	

	/**
	 * @see #JRXhtmlExporter(JasperReportsContext)
	 */
	public JRXhtmlExporter()
	{
		this(DefaultJasperReportsContext.getInstance());
	}

	
	/**
	 *
	 */
	public JRXhtmlExporter(JasperReportsContext jasperReportsContext)
	{
		super(jasperReportsContext);

		exporterContext = new ExporterContext();
		targetProducerFactory = new DefaultHyperlinkTargetProducerFactory(jasperReportsContext);		
	}


	@Override
	protected Class<HtmlExporterConfiguration> getConfigurationInterface()
	{
		return HtmlExporterConfiguration.class;
	}


	@Override
	protected Class<HtmlReportConfiguration> getItemConfigurationInterface()
	{
		return HtmlReportConfiguration.class;
	}
	

	@Override
	protected void ensureOutput()
	{
		if (exporterOutput == null)
		{
			exporterOutput = 
				new net.sf.jasperreports.export.parameters.ParametersHtmlExporterOutput(
					getJasperReportsContext(),
					getParameters(),
					getCurrentJasperPrint()
					);
		}
	}
	

	@Override
	public void exportReport() throws JRException
	{
		/*   */
		ensureJasperReportsContext();
		ensureInput();

		rendererToImagePathMap = new HashMap<String,String>();
		imageMaps = new HashMap<Pair<String,Rectangle>,String>();
		renderersCache = new RenderersCache(getJasperReportsContext());

		fontsToProcess = new HashMap<String, HtmlFontFamily>();
		
		initExport();
		
		ensureOutput();
		
		writer = getExporterOutput().getWriter();

		try
		{
			exportReportToWriter();
		}
		catch (IOException e)
		{
			throw 
				new JRException(
					EXCEPTION_MESSAGE_KEY_OUTPUT_WRITER_ERROR,
					new Object[]{jasperPrint.getName()}, 
					e);
		}
		finally
		{
			getExporterOutput().close();
			resetExportContext();
		}
	}


	@Override
	protected void initExport()
	{
		super.initExport();
	}


	@Override
	protected void initReport()
	{
		super.initReport();
	}
	

	/**
	 *
	 */
	protected void exportReportToWriter() throws JRException, IOException
	{
		HtmlExporterConfiguration configuration = getCurrentConfiguration();
		String htmlHeader = configuration.getHtmlHeader();
		String betweenPagesHtml = configuration.getBetweenPagesHtml();
		String htmlFooter = configuration.getHtmlFooter();

		if (htmlHeader == null)
		{
			String encoding = ((WriterExporterOutput)getExporterOutput()).getEncoding();

			writer.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n");
			writer.write("<html>\n");
			writer.write("<head>\n");
			writer.write("  <title></title>\n");
			writer.write("  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=" + encoding + "\"/>\n");
			writer.write("  <style type=\"text/css\">\n");
			writer.write("    a {text-decoration: none}\n");
			writer.write("  </style>\n");
			writer.write("</head>\n");
			writer.write("<body text=\"#000000\" link=\"#000000\" alink=\"#000000\" vlink=\"#000000\">\n");
			writer.write("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n");
			writer.write("<tr><td width=\"50%\">&nbsp;</td><td align=\"center\">\n");
			writer.write("\n");
		}
		else
		{
			writer.write(htmlHeader);
		}

		List<ExporterInputItem> items = exporterInput.getItems();

		for(reportIndex = 0; reportIndex < items.size(); reportIndex++)
		{
			ExporterInputItem item = items.get(reportIndex);

			setCurrentExporterInputItem(item);

			List<JRPrintPage> pages = jasperPrint.getPages();
			if (pages != null && pages.size() > 0)
			{
				PageRange pageRange = getPageRange();
				int startPageIndex = (pageRange == null || pageRange.getStartPageIndex() == null) ? 0 : pageRange.getStartPageIndex();
				int endPageIndex = (pageRange == null || pageRange.getEndPageIndex() == null) ? (pages.size() - 1) : pageRange.getEndPageIndex();

				JRPrintPage page = null;
				for(pageIndex = startPageIndex; pageIndex <= endPageIndex; pageIndex++)
				{
					if (Thread.interrupted())
					{
						throw new ExportInterruptedException();
					}

					page = pages.get(pageIndex);

					writer.write("<a name=\"" + JR_PAGE_ANCHOR_PREFIX + reportIndex + "_" + (pageIndex + 1) + "\"></a>\n");

					/*   */
					exportPage(page);

					if (reportIndex < items.size() - 1 || pageIndex < endPageIndex)
					{
						if (betweenPagesHtml == null)
						{
							writer.write("<br/>\n<br/>\n");
						}
						else
						{
							writer.write(betweenPagesHtml);
						}
					}

					writer.write("\n");
				}
			}
		}

		if (fontsToProcess != null && fontsToProcess.size() > 0)// when no fontHandler and/or resourceHandler, fonts are not processed 
		{
			HtmlResourceHandler fontHandler = 
				getExporterOutput().getFontHandler() == null
				? getFontHandler()
				: getExporterOutput().getFontHandler();
			for (HtmlFontFamily htmlFontFamily : fontsToProcess.values())
			{
				writer.write("<link class=\"jrWebFont\" rel=\"stylesheet\" href=\"" + fontHandler.getResourcePath(htmlFontFamily.getId()) + "\">\n");
			}
		}
		
//		if (!isOutputResourcesToDir)
		{
			writer.write("<!--[if IE]>\n");
			writer.write("<script>\n");
			writer.write("var links = document.querySelectorAll('link.jrWebFont');\n");
			writer.write("setTimeout(function(){ if (links) { for (var i = 0; i < links.length; i++) { links.item(i).href = links.item(i).href; } } }, 0);\n");
			writer.write("</script>\n");
			writer.write("<![endif]-->\n");
		}

		if (htmlFooter == null)
		{
			writer.write("</td><td width=\"50%\">&nbsp;</td></tr>\n");
			writer.write("</table>\n");
			writer.write("</body>\n");
			writer.write("</html>\n");
		}
		else
		{
			writer.write(htmlFooter);
		}

		writer.flush();//FIXMEEXPORT other exporters always perform flush
	}


	/**
	 *
	 */
	protected void exportPage(JRPrintPage page) throws JRException, IOException
	{
		PrintPageFormat pageFormat = jasperPrint.getPageFormat(pageIndex);
		topLimit = pageFormat.getPageHeight();
		leftLimit = pageFormat.getPageWidth();
		rightLimit = 0;
		bottomLimit = 0;
		
		setPageLimits(page.getElements());

		HtmlReportConfiguration configuration = getCurrentItemConfiguration();

		boolean isIgnorePageMargins = configuration.isIgnorePageMargins();
		if (!isIgnorePageMargins)
		{
			topLimit = 0;
		}
		if (!isIgnorePageMargins)
		{
			leftLimit = 0;
		}
		if (pageFormat.getPageWidth() > rightLimit && !isIgnorePageMargins)
		{
			rightLimit = pageFormat.getPageWidth();
		}
		if (pageFormat.getPageHeight() > bottomLimit && !isIgnorePageMargins)
		{
			bottomLimit = pageFormat.getPageHeight();
		}
		
		if (topLimit > bottomLimit)//these can occur only when empty page and page margins are ignored
		{
			topLimit = bottomLimit;
		}
		if (leftLimit > rightLimit)
		{
			leftLimit = rightLimit;
		}
		
		boolean isWhitePageBackground = configuration.isWhitePageBackground();
		writer.write(
			"<div class=\"jrPage\" style=\"" + (isWhitePageBackground ? "background-color: #FFFFFF;" : "") 
			+ "position:relative;width:" + toSizeUnit(rightLimit - leftLimit) 
			+ ";height:" + toSizeUnit(bottomLimit - topLimit) + ";\">\n"
			);

		frameInfoStack = new ArrayList<FrameInfo>();
		
		exportElements(page.getElements());

		writer.write("</div>");
		
		JRExportProgressMonitor progressMonitor = configuration.getProgressMonitor();
		if (progressMonitor != null)
		{
			progressMonitor.afterPageExport();
		}
	}
	

	/**
	 *
	 */
	protected void exportElements(List<JRPrintElement> elements) throws IOException, JRException
	{
		if (elements != null && elements.size() > 0)
		{
			JRPrintElement element;
			for(int i = 0; i < elements.size(); i++)
			{
				elementIndex = i;
				
				element = elements.get(i);
				
				if (filter == null || filter.isToExport(element))
				{
					if (element instanceof JRPrintLine)
					{
						exportLine((JRPrintLine)element);
					}
					else if (element instanceof JRPrintRectangle)
					{
						exportRectangle((JRPrintRectangle)element);
					}
					else if (element instanceof JRPrintEllipse)
					{
						//exportEllipse((JRPrintEllipse)element);
						exportRectangle((JRPrintEllipse)element);
					}
					else if (element instanceof JRPrintImage)
					{
						exportImage((JRPrintImage)element);
					}
					else if (element instanceof JRPrintText)
					{
						exportText((JRPrintText)element);
					}
					else if (element instanceof JRPrintFrame)
					{
						exportFrame((JRPrintFrame) element);
					}
					else if (element instanceof JRGenericPrintElement)
					{
						exportGenericElement((JRGenericPrintElement) element);
					}
				}
			}
		}
	}

	
	/**
	 *
	 */
	protected void exportLine(JRPrintLine line) throws IOException
	{
		writer.write("<span");

		appendId(line);

		StringBuilder styleBuilder = new StringBuilder();

		appendPositionStyle(line, line.getLinePen(), styleBuilder);
		appendSizeStyle(line, line.getLinePen(), styleBuilder);
		appendBackcolorStyle(line, styleBuilder);
		
		String side = null;
		float ratio = line.getWidth() / line.getHeight();
		if (ratio > 1)
		{
			if (line.getDirectionValue() == LineDirectionEnum.TOP_DOWN)
			{
				side = "top";
			}
			else
			{
				side = "bottom";
			}
		}
		else
		{
			if (line.getDirectionValue() == LineDirectionEnum.TOP_DOWN)
			{
				side = "left";
			}
			else
			{
				side = "right";
			}
		}

		appendPen(
			styleBuilder,
			line.getLinePen(),
			side
			);

		if (styleBuilder.length() > 0)
		{
			writer.write(" style=\"");
			writer.write(styleBuilder.toString());
			writer.write("\"");
		}

		writer.write("></span>\n");
	}


	/**
	 *
	 */
	protected void exportRectangle(JRPrintGraphicElement element) throws IOException
	{
		writer.write("<span");

		appendId(element);

		StringBuilder styleBuilder = new StringBuilder();

		appendPositionStyle(element, element.getLinePen(), styleBuilder);
		appendSizeStyle(element, element.getLinePen(), styleBuilder);
		appendBackcolorStyle(element, styleBuilder);
		
		appendPen(
			styleBuilder,
			element.getLinePen(),
			null
			);

		if (styleBuilder.length() > 0)
		{
			writer.write(" style=\"");
			writer.write(styleBuilder.toString());
			writer.write("\"");
		}

		writer.write("></span>\n");
	}


	/**
	 *
	 */
	protected void exportStyledText(JRPrintText printText, JRStyledText styledText, String tooltip) throws IOException
	{
		Locale locale = getTextLocale(printText);
		LineSpacingEnum lineSpacing = printText.getParagraph().getLineSpacing();
		Float lineSpacingSize = printText.getParagraph().getLineSpacingSize();
		float lineSpacingFactor = printText.getLineSpacingFactor();
		Color backcolor = printText.getBackcolor();
		
		String text = styledText.getText();

		int runLimit = 0;

		AttributedCharacterIterator iterator = styledText.getAttributedString().getIterator();

		boolean first = true;
		boolean startedSpan = false;
		while(runLimit < styledText.length() && (runLimit = iterator.getRunLimit()) <= styledText.length())
		{
			//if there are several text runs, write the tooltip into a parent <span>
			if (first && runLimit < styledText.length() && tooltip != null)
			{
				startedSpan = true;
				writer.write("<span title=\"");
				writer.write(JRStringUtil.xmlEncode(tooltip));
				writer.write("\">");
				//reset the tooltip so that inner <span>s to not use it
				tooltip = null;
			}
			first = false;
			
			exportStyledTextRun(
				iterator.getAttributes(), 
				text.substring(iterator.getIndex(), runLimit),
				tooltip,
				locale,
				lineSpacing,
				lineSpacingSize,
				lineSpacingFactor,
				backcolor
				);

			iterator.setIndex(runLimit);
		}
		
		if (startedSpan)
		{
			writer.write("</span>");
		}
	}


	/**
	 *
	 */
	protected void exportStyledTextRun(
		Map<Attribute,Object> attributes, 
		String text,
		String tooltip,
		Locale locale,
		LineSpacingEnum lineSpacing,
		Float lineSpacingSize,
		float lineSpacingFactor,
		Color backcolor
		) throws IOException
	{
		boolean localHyperlink = false;
		JRPrintHyperlink hyperlink = (JRPrintHyperlink)attributes.get(JRTextAttribute.HYPERLINK);
		if (!hyperlinkStarted && hyperlink != null)
		{
			startHyperlink(hyperlink);
			localHyperlink = true;
		}
		
		boolean isBold = TextAttribute.WEIGHT_BOLD.equals(attributes.get(TextAttribute.WEIGHT));
		boolean isItalic = TextAttribute.POSTURE_OBLIQUE.equals(attributes.get(TextAttribute.POSTURE));

		String fontFamily = resolveFontFamily(attributes, locale);

		// do not put single quotes around family name here because the value might already contain quotes, 
		// especially if it is coming from font extension export configuration
		writer.write("<span style=\"font-family: ");
		writer.write(fontFamily);
		writer.write("; ");

		Color forecolor = (Color)attributes.get(TextAttribute.FOREGROUND);
		if (!hyperlinkStarted || !Color.black.equals(forecolor))
		{
			writer.write("color: ");
			writer.write(JRColorUtil.getCssColor(forecolor));
			writer.write("; ");
		}

		Color runBackcolor = (Color)attributes.get(TextAttribute.BACKGROUND);
		if (runBackcolor != null && !runBackcolor.equals(backcolor))
		{
			writer.write("background-color: ");
			writer.write(JRColorUtil.getCssColor(runBackcolor));
			writer.write("; ");
		}

		writer.write("font-size: ");
		writer.write(toSizeUnit((Float)attributes.get(TextAttribute.SIZE)));
		writer.write(";");
		
		switch (lineSpacing)
		{
			case SINGLE:
			default:
			{
				if (lineSpacingFactor == 0)
				{
					writer.write(" line-height: 1; *line-height: normal;");
				}
				else
				{
					writer.write(" line-height: " + lineSpacingFactor + ";");
				}
				break;
			}
			case ONE_AND_HALF:
			{
				if (lineSpacingFactor == 0)
				{
					writer.write(" line-height: 1.5;");
				}
				else
				{
					writer.write(" line-height: " + lineSpacingFactor + ";");
				}
				break;
			}
			case DOUBLE:
			{
				if (lineSpacingFactor == 0)
				{
					writer.write(" line-height: 2.0;");
				}
				else
				{
					writer.write(" line-height: " + lineSpacingFactor + ";");
				}
				break;
			}
			case PROPORTIONAL:
			{
				if (lineSpacingSize != null) {
					writer.write(" line-height: " + lineSpacingSize.floatValue() + ";");
				}
				break;
			}
			case AT_LEAST:
			case FIXED:
			{
				if (lineSpacingSize != null) {
					writer.write(" line-height: " + lineSpacingSize.floatValue() + "px;");
				}
				break;
			}
		}

		/*
		if (!horizontalAlignment.equals(CSS_TEXT_ALIGN_LEFT))
		{
			writer.write(" text-align: ");
			writer.write(horizontalAlignment);
			writer.write(";");
		}
		*/

		if (isBold)
		{
			writer.write(" font-weight: bold;");
		}
		if (isItalic)
		{
			writer.write(" font-style: italic;");
		}
		if (TextAttribute.UNDERLINE_ON.equals(attributes.get(TextAttribute.UNDERLINE)))
		{
			writer.write(" text-decoration: underline;");
		}
		if (TextAttribute.STRIKETHROUGH_ON.equals(attributes.get(TextAttribute.STRIKETHROUGH)))
		{
			writer.write(" text-decoration: line-through;");
		}

		if (TextAttribute.SUPERSCRIPT_SUPER.equals(attributes.get(TextAttribute.SUPERSCRIPT)))
		{
			writer.write(" vertical-align: super;");
		}
		else if (TextAttribute.SUPERSCRIPT_SUB.equals(attributes.get(TextAttribute.SUPERSCRIPT)))
		{
			writer.write(" vertical-align: sub;");
		}
		
		writer.write("\"");

		if (tooltip != null)
		{
			writer.write(" title=\"");
			writer.write(JRStringUtil.xmlEncode(tooltip));
			writer.write("\"");
		}
		
		writer.write(">");

		writer.write(
			JRStringUtil.htmlEncode(text)
			);

		writer.write("</span>");

		if (localHyperlink)
		{
			endHyperlink();
		}
	}


	/**
	 *
	 */
	protected void exportText(JRPrintText text) throws IOException
	{
		JRStyledText styledText = getStyledText(text);

		int textLength = 0;

		if (styledText != null)
		{
			textLength = styledText.length();
		}

		if (text.getAnchorName() != null)
		{
			writer.write("<a name=\"");
			writer.write(text.getAnchorName());
			writer.write("\"></a>");
		}

		writer.write("<div");//FIXME why dealing with cell style if no text to print (textLength == 0)?

		appendId(text);

		if (text.getRunDirectionValue() == RunDirectionEnum.RTL)
		{
			writer.write(" dir=\"rtl\"");
		}

		StringBuilder styleBuilder = new StringBuilder();
		StringBuilder divStyleBuilder = new StringBuilder();
		String rotationValue = null;

		if (text.getRotationValue() == RotationEnum.NONE)
		{
			appendPositionStyle(text, text, divStyleBuilder);
			appendSizeStyle(text, text, divStyleBuilder);
			appendBorderStyle(text.getLineBox(), divStyleBuilder);

			appendPositionStyle(
				text.getLineBox().getLeftPadding() - getInsideBorderOffset(text.getLineBox().getLeftPen().getLineWidth(), false),
				text.getLineBox().getTopPadding() - getInsideBorderOffset(text.getLineBox().getTopPen().getLineWidth(), false), 
				styleBuilder
				);
			appendSizeStyle(
				text.getWidth() - text.getLineBox().getLeftPadding() - text.getLineBox().getRightPadding(), 
				text.getHeight() - text.getLineBox().getTopPadding() - text.getLineBox().getBottomPadding(), 
				styleBuilder
				);
//			styleBuilder.append("width: 100%; height: 100%;");
		}
		else
		{
			JRBasePrintText rotatedText = new JRBasePrintText(text.getDefaultStyleProvider());
			rotatedText.setUUID(text.getUUID());
			rotatedText.setX(text.getX());
			rotatedText.setY(text.getY());
			rotatedText.setWidth(text.getWidth());
			rotatedText.setHeight(text.getHeight());
			rotatedText.copyBox(text.getLineBox());
			
			JRBoxUtil.rotate(rotatedText.getLineBox(), text.getRotationValue());
			
			int rotationIE = 0;
			int rotationAngle = 0;
			int translateX = 0;
			int translateY = 0;
			switch (text.getRotationValue())
			{
				case LEFT : 
				{
					translateX = 
						- (text.getHeight()
							- text.getLineBox().getTopPadding()
							- text.getLineBox().getBottomPadding()
						- (text.getWidth() 
							- text.getLineBox().getLeftPadding()
							- text.getLineBox().getRightPadding()
							)) / 2;
					translateY = 
						(text.getHeight() 
							- text.getLineBox().getTopPadding()
							- text.getLineBox().getBottomPadding()
						- (text.getWidth() 
							- text.getLineBox().getLeftPadding()
							- text.getLineBox().getRightPadding()
							)) / 2;
					rotatedText.setWidth(text.getHeight());
					rotatedText.setHeight(text.getWidth());
					rotationIE = 3;
					rotationAngle = -90;
					rotationValue = "left";
					break;
				}
				case RIGHT : 
				{
					translateX = 
						- (text.getHeight() 
							- text.getLineBox().getTopPadding()
							- text.getLineBox().getBottomPadding()
						- (text.getWidth() 
							- text.getLineBox().getLeftPadding()
							- text.getLineBox().getRightPadding()
							)) / 2;
					translateY = 
						(text.getHeight() 
							- text.getLineBox().getTopPadding()
							- text.getLineBox().getBottomPadding()
						- (text.getWidth() 
							- text.getLineBox().getLeftPadding()
							- text.getLineBox().getRightPadding()
							)) / 2;
					rotatedText.setWidth(text.getHeight());
					rotatedText.setHeight(text.getWidth());
					rotationIE = 1;
					rotationAngle = 90;
					rotationValue = "right";
					break;
				}
				case UPSIDE_DOWN : 
				{
					rotationIE = 2;
					rotationAngle = 180;
					rotationValue = "upsideDown";
					break;
				}
				case NONE :
				default :
				{
				}
			}
			
			appendPositionStyle(text, text, divStyleBuilder);
			appendSizeStyle(text, text, divStyleBuilder);
			appendBorderStyle(text.getLineBox(), divStyleBuilder);

			appendPositionStyle(
				text.getLineBox().getLeftPadding() - getInsideBorderOffset(text.getLineBox().getLeftPen().getLineWidth(), false), 
				text.getLineBox().getTopPadding() - getInsideBorderOffset(text.getLineBox().getTopPen().getLineWidth(), false), 
				styleBuilder
				);
			appendSizeStyle(
				rotatedText.getWidth() - rotatedText.getLineBox().getLeftPadding() - rotatedText.getLineBox().getRightPadding(), 
				rotatedText.getHeight() - rotatedText.getLineBox().getTopPadding() - rotatedText.getLineBox().getBottomPadding(), 
				styleBuilder
				);
//			appendSizeStyle(rotatedText, rotatedText, styleBuilder);
			
			styleBuilder.append("-webkit-transform: translate(" + translateX + "px," + translateY + "px) ");
			styleBuilder.append("rotate(" + rotationAngle + "deg); ");
			styleBuilder.append("-moz-transform: translate(" + translateX + "px," + translateY + "px) ");
			styleBuilder.append("rotate(" + rotationAngle + "deg); ");
			styleBuilder.append("-ms-transform: translate(" + translateX + "px," + translateY + "px) ");
			styleBuilder.append("rotate(" + rotationAngle + "deg); ");
			styleBuilder.append("-o-transform: translate(" + translateX + "px," + translateY + "px) ");
			styleBuilder.append("rotate(" + rotationAngle + "deg); ");
			styleBuilder.append("filter: progid:DXImageTransform.Microsoft.BasicImage(rotation=" + rotationIE + "); ");
		}

		appendBackcolorStyle(text, divStyleBuilder);

		String verticalAlignment = HTML_VERTICAL_ALIGN_TOP;

		switch (text.getVerticalTextAlign())
		{
			case BOTTOM :
			{
				verticalAlignment = HTML_VERTICAL_ALIGN_BOTTOM;
				break;
			}
			case MIDDLE :
			{
				verticalAlignment = HTML_VERTICAL_ALIGN_MIDDLE;
				break;
			}
			case TOP :
			case JUSTIFIED :
			default :
			{
				verticalAlignment = HTML_VERTICAL_ALIGN_TOP;
			}
		}

		String horizontalAlignment = CSS_TEXT_ALIGN_LEFT;

		if (textLength > 0)
		{
			switch (text.getHorizontalTextAlign())
			{
				case RIGHT :
				{
					horizontalAlignment = CSS_TEXT_ALIGN_RIGHT;
					break;
				}
				case CENTER :
				{
					horizontalAlignment = CSS_TEXT_ALIGN_CENTER;
					break;
				}
				case JUSTIFIED :
				{
					horizontalAlignment = CSS_TEXT_ALIGN_JUSTIFY;
					break;
				}
				case LEFT :
				default :
				{
					horizontalAlignment = CSS_TEXT_ALIGN_LEFT;
				}
			}

//			if (
//				(text.getRunDirection() == JRPrintText.RUN_DIRECTION_LTR
//				 && !horizontalAlignment.equals(CSS_TEXT_ALIGN_LEFT))
//				|| (text.getRunDirection() == JRPrintText.RUN_DIRECTION_RTL
//					&& !horizontalAlignment.equals(CSS_TEXT_ALIGN_RIGHT))
//				)
//			{
				styleBuilder.append("text-align: ");
				styleBuilder.append(horizontalAlignment);
				styleBuilder.append(";");
//			}
//
//			if (!verticalAlignment.equals(HTML_VERTICAL_ALIGN_TOP))
//			{
				styleBuilder.append(" display:table;");
//			}
		}

		if (getCurrentItemConfiguration().isWrapBreakWord())
		{
			//styleBuilder.append("width: " + toSizeUnit(text.getWidth()) + "; ");
			styleBuilder.append("word-wrap: break-word; ");
		}
		
		styleBuilder.append("text-indent: " + text.getParagraph().getFirstLineIndent().intValue() + "px; ");
//		styleBuilder.append("margin-left: " + text.getParagraph().getLeftIndent().intValue() + "px; ");
//		styleBuilder.append("margin-right: " + text.getParagraph().getRightIndent().intValue() + "px; ");
//		styleBuilder.append("margin-top: " + text.getParagraph().getSpacingBefore().intValue() + "px; ");
//		styleBuilder.append("margin-bottom: " + text.getParagraph().getSpacingAfter().intValue() + "px; ");

		if (text.getLineBreakOffsets() != null)
		{
			//if we have line breaks saved in the text, set nowrap so that
			//the text only wraps at the explicit positions
			styleBuilder.append("white-space: nowrap; ");
		}
		
		divStyleBuilder.append("overflow: hidden;");
		
		if (divStyleBuilder.length() > 0) 
		{
			writer.write(" style=\"");
			writer.write(divStyleBuilder.toString());
			writer.write("\"");
		}
		writer.write("><span");
		
		if (rotationValue != null) 
		{
			writer.write(" class=\"rotated\" data-rotation=\"" + rotationValue + "\"");
		}
		
		if (styleBuilder.length() > 0)
		{
			writer.write(" style=\"");
			writer.write(styleBuilder.toString());
			writer.write("\"");
		}
		
		writer.write(">");
		
//		if (!verticalAlignment.equals(HTML_VERTICAL_ALIGN_TOP))
//		{
			writer.write("<span style=\"display:table-cell;vertical-align:"); //display:table-cell conflicts with overflow: hidden;
			writer.write(verticalAlignment);
			writer.write(";\">");
//		}

		startHyperlink(text);

		if (textLength > 0)
		{
			//only use text tooltip when no hyperlink present
//			String textTooltip = hyperlinkStarted ? null : text.getHyperlinkTooltip();
			exportStyledText(text, styledText, text.getHyperlinkTooltip());
		}
//		else
//		{
//			//writer.write(emptyCellStringProvider.getStringForEmptyTD(imagesURI));
//		}

		endHyperlink();

//		if (!verticalAlignment.equals(HTML_VERTICAL_ALIGN_TOP))
//		{
			writer.write("</span>");//FIXMENOW move tooltip span here
//		}

		writer.write("</span></div>\n");
	}


	protected boolean startHyperlink(JRPrintHyperlink link) throws IOException
	{
		String href = getHyperlinkURL(link);

		if (href != null)
		{
			writer.write("<a href=\"");
			writer.write(href);
			writer.write("\"");

			String target = getHyperlinkTarget(link);
			if (target != null)
			{
				writer.write(" target=\"");
				writer.write(target);
				writer.write("\"");
			}

			if (link.getHyperlinkTooltip() != null)
			{
				writer.write(" title=\"");
				writer.write(JRStringUtil.xmlEncode(link.getHyperlinkTooltip()));
				writer.write("\"");
			}
			
			writer.write(">");
		}
		
		hyperlinkStarted = href != null;
		
		return hyperlinkStarted;
	}


	protected String getHyperlinkTarget(JRPrintHyperlink link)
	{
		String target = null;
		JRHyperlinkTargetProducer producer = targetProducerFactory.getHyperlinkTargetProducer(link.getLinkTarget());		
		if (producer == null)
		{
			switch(link.getHyperlinkTargetValue())
			{
				case BLANK :
				{
					target = "_blank";
					break;
				}
				case PARENT :
				{
					target = "_parent";
					break;
				}
				case TOP :
				{
					target = "_top";
					break;
				}
				case CUSTOM :
				{
					boolean paramFound = false;
					List<JRPrintHyperlinkParameter> parameters = link.getHyperlinkParameters() == null ? null : link.getHyperlinkParameters().getParameters();
					if (parameters != null)
					{
						for(Iterator<JRPrintHyperlinkParameter> it = parameters.iterator(); it.hasNext();)
						{
							JRPrintHyperlinkParameter parameter = it.next();
							if (link.getLinkTarget().equals(parameter.getName()))
							{
								target = parameter.getValue() == null ? null : parameter.getValue().toString();
								paramFound = true;
								break;
							}
						}
					}
					if (!paramFound)
					{
						target = link.getLinkTarget();
					}
					break;
				}
				case SELF :
				default :
				{
				}
			}
		}
		else
		{
			target = producer.getHyperlinkTarget(link);
		}

		return target;
	}


	protected String getHyperlinkURL(JRPrintHyperlink link)
	{
		String href = null;
		
		Boolean ignoreHyperlink = HyperlinkUtil.getIgnoreHyperlink(PROPERTY_IGNORE_HYPERLINK, link);
		if (ignoreHyperlink == null)
		{
			ignoreHyperlink = getPropertiesUtil().getBooleanProperty(jasperPrint, PROPERTY_IGNORE_HYPERLINK, false);
		}

		if (!ignoreHyperlink)
		{
			JRHyperlinkProducer customHandler = getHyperlinkProducer(link);		
			if (customHandler == null)
			{
				switch(link.getHyperlinkTypeValue())
				{
					case REFERENCE :
					{
						if (link.getHyperlinkReference() != null)
						{
							href = link.getHyperlinkReference();
						}
						break;
					}
					case LOCAL_ANCHOR :
					{
						if (link.getHyperlinkAnchor() != null)
						{
							href = "#" + link.getHyperlinkAnchor();
						}
						break;
					}
					case LOCAL_PAGE :
					{
						if (link.getHyperlinkPage() != null)
						{
							href = "#" + JR_PAGE_ANCHOR_PREFIX + reportIndex + "_" + link.getHyperlinkPage().toString();
						}
						break;
					}
					case REMOTE_ANCHOR :
					{
						if (
							link.getHyperlinkReference() != null &&
							link.getHyperlinkAnchor() != null
							)
						{
							href = link.getHyperlinkReference() + "#" + link.getHyperlinkAnchor();
						}
						break;
					}
					case REMOTE_PAGE :
					{
						if (
							link.getHyperlinkReference() != null &&
							link.getHyperlinkPage() != null
							)
						{
							href = link.getHyperlinkReference() + "#" + JR_PAGE_ANCHOR_PREFIX + "0_" + link.getHyperlinkPage().toString();
						}
						break;
					}
					case NONE :
					default :
					{
						break;
					}
				}
			}
			else
			{
				href = customHandler.getHyperlink(link);
			}
		}
		
		return href;
	}


	protected void endHyperlink() throws IOException
	{
		if (hyperlinkStarted)
		{
			writer.write("</a>");
		}
		hyperlinkStarted = false;
	}


	protected boolean appendBorderStyle(JRLineBox box, StringBuilder styleBuilder)
	{
		boolean addedToStyle = false;
		
		if (box != null)
		{
			LineStyleEnum tps = box.getTopPen().getLineStyleValue();
			LineStyleEnum lps = box.getLeftPen().getLineStyleValue();
			LineStyleEnum bps = box.getBottomPen().getLineStyleValue();
			LineStyleEnum rps = box.getRightPen().getLineStyleValue();
			
			float tpw = box.getTopPen().getLineWidth().floatValue();
			float lpw = box.getLeftPen().getLineWidth().floatValue();
			float bpw = box.getBottomPen().getLineWidth().floatValue();
			float rpw = box.getRightPen().getLineWidth().floatValue();
			
			if (0f < tpw && tpw < 1f) {
				tpw = 1f;
			}
			if (0f < lpw && lpw < 1f) {
				lpw = 1f;
			}
			if (0f < bpw && bpw < 1f) {
				bpw = 1f;
			}
			if (0f < rpw && rpw < 1f) {
				rpw = 1f;
			}
			
			Color tpc = box.getTopPen().getLineColor();
			
			// try to compact all borders into one css property
			if (tps == lps &&												// same line style
					tps == bps &&
					tps == rps &&
					tpw == lpw &&											// same line width
					tpw == bpw &&
					tpw == rpw &&
					tpc.equals(box.getLeftPen().getLineColor()) &&			// same line color
					tpc.equals(box.getBottomPen().getLineColor()) &&
					tpc.equals(box.getRightPen().getLineColor())) 
			{
				addedToStyle |= appendPen(
						styleBuilder,
						box.getTopPen(),
						null
						);
			} else {
				addedToStyle |= appendPen(
					styleBuilder,
					box.getTopPen(),
					"top"
					);
				addedToStyle |= appendPen(
					styleBuilder,
					box.getLeftPen(),
					"left"
					);
				addedToStyle |= appendPen(
					styleBuilder,
					box.getBottomPen(),
					"bottom"
					);
				addedToStyle |= appendPen(
					styleBuilder,
					box.getRightPen(),
					"right"
					);
			}
			
			Integer tp = box.getTopPadding();
			Integer lp = box.getLeftPadding();
			Integer bp = box.getBottomPadding();
			Integer rp = box.getRightPadding();
			
			// try to compact all paddings into one css property
			if (tp == lp && tp == bp && tp == rp)
			{
				addedToStyle |= appendPadding(
						styleBuilder,
						tp,
						null
						);
			} else 
			{
				addedToStyle |= appendPadding(
						styleBuilder,
						box.getTopPadding(),
						"top"
						);
				addedToStyle |= appendPadding(
						styleBuilder,
						box.getLeftPadding(),
						"left"
						);
				addedToStyle |= appendPadding(
						styleBuilder,
						box.getBottomPadding(),
						"bottom"
						);
				addedToStyle |= appendPadding(
						styleBuilder,
						box.getRightPadding(),
						"right"
						);
			}
		}
		
		return addedToStyle;
	}


	protected int getInsideBorderOffset(float borderWidth, boolean small)
	{
		int intBorderWidth = (int)borderWidth;
		if (0f < borderWidth && borderWidth < 1f)
		{
			intBorderWidth = 1;
		}
		return intBorderWidth / 2 + (small ? 0 : intBorderWidth % 2);
	}
		
	
	protected void appendSizeStyle(JRPrintElement element, JRBoxContainer boxContainer, StringBuilder styleBuilder)
	{
		int widthDiff = 0;
		int heightDiff = 0;

		JRLineBox box = boxContainer == null ? null :  boxContainer.getLineBox();
		if (box != null)
		{
			widthDiff = 
				box.getLeftPadding().intValue() + box.getRightPadding().intValue()
				+ getInsideBorderOffset(box.getLeftPen().getLineWidth().floatValue(), false)
				+ getInsideBorderOffset(box.getRightPen().getLineWidth().floatValue(), true);
			heightDiff =
				box.getTopPadding().intValue() + box.getBottomPadding().intValue()
				+ getInsideBorderOffset(box.getTopPen().getLineWidth().floatValue(), false)
				+ getInsideBorderOffset(box.getBottomPen().getLineWidth().floatValue(), true);
		}
		
		int width = element.getWidth() - widthDiff; 
		int height = element.getHeight() - heightDiff; 
		
		appendSizeStyle(
			width < 0 ? 0 : width,
			height < 0 ? 0 : height,
			styleBuilder
			);
	}


	protected void appendSizeStyle(JRPrintElement element, JRPen pen, StringBuilder styleBuilder)
	{
		int diff = 0;

		if (pen != null)
		{
			diff = 
				getInsideBorderOffset(pen.getLineWidth().floatValue(), false) 
				+ getInsideBorderOffset(pen.getLineWidth().floatValue(), true);
		}
		
		appendSizeStyle(
			element.getWidth() - diff,
			element.getHeight() - diff,
			styleBuilder
			);
	}


	protected void appendSizeStyle(int width, int height, StringBuilder styleBuilder)
	{
		styleBuilder.append("width:");
		styleBuilder.append(toSizeUnit(width));
		styleBuilder.append(";");

		styleBuilder.append("height:");
		styleBuilder.append(toSizeUnit(height));
		styleBuilder.append(";");
	}


	protected void appendPositionStyle(JRPrintElement element, JRBoxContainer boxContainer, StringBuilder styleBuilder)
	{
		int leftOffset = 0;
		int topOffset = 0;

		JRLineBox box = boxContainer == null ? null :  boxContainer.getLineBox();
		if (box != null)
		{
			leftOffset = 
				getInsideBorderOffset(box.getLeftPen().getLineWidth().floatValue(), true);
			topOffset = 
				getInsideBorderOffset(box.getTopPen().getLineWidth().floatValue(), true);
		}
		
		FrameInfo frameInfo = frameInfoStack.size() == 0 ? null : frameInfoStack.get(frameInfoStack.size() - 1); 
		
		appendPositionStyle(
			element.getX() - leftOffset - (frameInfo == null ? leftLimit : frameInfo.leftInsideBorderOffset),
			element.getY() - topOffset - (frameInfo == null ? topLimit : frameInfo.topInsideBorderOffset),
			styleBuilder
			);
	}


	protected void appendPositionStyle(JRPrintElement element, JRPen pen, StringBuilder styleBuilder)
	{
		int offset = 0;

		if (pen != null)
		{
			offset = 
				getInsideBorderOffset(pen.getLineWidth().floatValue(), true);
		}
		
		FrameInfo frameInfo = frameInfoStack.size() == 0 ? null : frameInfoStack.get(frameInfoStack.size() - 1); 
		
		appendPositionStyle(
			element.getX() - offset - (frameInfo == null ? leftLimit : frameInfo.leftInsideBorderOffset),
			element.getY() - offset - (frameInfo == null ? topLimit : frameInfo.topInsideBorderOffset),
			styleBuilder
			);
	}


	protected void appendPositionStyle(int x, int y, StringBuilder styleBuilder)
	{
		styleBuilder.append("position:absolute;");
		styleBuilder.append("left:");
		styleBuilder.append(toSizeUnit(x));
		styleBuilder.append(";");
		styleBuilder.append("top:");
		styleBuilder.append(toSizeUnit(y));
		styleBuilder.append(";");
	}


	protected void appendBackcolorStyle(JRPrintElement element, StringBuilder styleBuilder)
	{
		if (element.getModeValue() == ModeEnum.OPAQUE)
		{
			styleBuilder.append("background-color: ");
			styleBuilder.append(JRColorUtil.getCssColor(element.getBackcolor()));
			styleBuilder.append("; ");
		}
	}


	/**
	 *
	 */
	protected void exportImage(JRPrintImage image) throws JRException, IOException
	{
		writer.write("<span");

		appendId(image);

		float xAlignFactor = 0f;

		switch (image.getHorizontalImageAlign())
		{
			case RIGHT :
			{
				xAlignFactor = 1f;
				break;
			}
			case CENTER :
			{
				xAlignFactor = 0.5f;
				break;
			}
			case LEFT :
			default :
			{
				xAlignFactor = 0f;
			}
		}

		float yAlignFactor = 0f;

		switch (image.getVerticalImageAlign())
		{
			case BOTTOM :
			{
				yAlignFactor = 1f;
				break;
			}
			case MIDDLE :
			{
				yAlignFactor = 0.5f;
				break;
			}
			case TOP :
			default :
			{
				yAlignFactor = 0f;
			}
		}

		StringBuilder styleBuilder = new StringBuilder();
		appendPositionStyle(image, image, styleBuilder);
		appendSizeStyle(image, image, styleBuilder);
		appendBackcolorStyle(image, styleBuilder);
		
		boolean addedToStyle = appendBorderStyle(image.getLineBox(), styleBuilder);
		if (!addedToStyle)
		{
			appendPen(
				styleBuilder,
				image.getLinePen(),
				null
				);
		}

		if (styleBuilder.length() > 0)
		{
			writer.write(" style=\"");
			writer.write(styleBuilder.toString());
			writer.write("\"");
		}

		writer.write(">");

		if (image.getAnchorName() != null)
		{
			writer.write("<a name=\"");
			writer.write(image.getAnchorName());
			writer.write("\"></a>");
		}
		
		Renderable renderer = image.getRenderer();

		if (renderer != null)
		{
			boolean hasAreaHyperlinks =
				renderer instanceof ImageMapRenderable
				&& ((ImageMapRenderable) renderer).hasImageAreaHyperlinks();

			boolean hasHyperlinks = false;

			if (hasAreaHyperlinks)
			{
				hasHyperlinks = true;
				hyperlinkStarted = false;
			}
			else
			{
				hasHyperlinks = startHyperlink(image);
			}
			
			int availableImageWidth = image.getWidth() - image.getLineBox().getLeftPadding().intValue() - image.getLineBox().getRightPadding().intValue();
			if (availableImageWidth < 0)
			{
				availableImageWidth = 0;
			}
		
			int availableImageHeight = image.getHeight() - image.getLineBox().getTopPadding().intValue() - image.getLineBox().getBottomPadding().intValue();
			if (availableImageHeight < 0)
			{
				availableImageHeight = 0;
			}

			writer.write("<img");
			String imagePath = null;
			
			boolean isLazy = RendererUtil.isLazy(renderer);
			
			if (isLazy)
			{
				// we do not cache imagePath for lazy images because the short location string is already cached inside the render itself
				imagePath = RendererUtil.getResourceLocation(renderer);
			}
			else
			{
				if (renderer instanceof ResourceRenderer)
				{
					renderer = renderersCache.getLoadedRenderer((ResourceRenderer)renderer);
				}

				boolean isEmbedImage = isEmbedImage(image);
				
				if (
					!isEmbedImage //we do not cache imagePath for embedded images because it is too big
					&& renderer instanceof DataRenderable //we do not cache imagePath for non-data renderers because they render width different width/height each time
					&& rendererToImagePathMap.containsKey(renderer.getId())
					)
				{
					imagePath = rendererToImagePathMap.get(renderer.getId());
				}
				else
				{
					if (isEmbedImage)
					{
						DataRenderable dataRenderer = 
							getRendererUtil().getDataRenderable(
								renderer,
								new Dimension(availableImageWidth, availableImageHeight),
								ModeEnum.OPAQUE == image.getModeValue() ? image.getBackcolor() : null
								);
						
						byte[] imageData = dataRenderer.getData(jasperReportsContext);
						
						String imageMimeType = 
							getRendererUtil().isSvgData(imageData)
							? RendererUtil.SVG_MIME_TYPE
							: JRTypeSniffer.getImageTypeValue(imageData).getMimeType();
						
						ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						
						Base64Encoder encoder = new Base64Encoder(bais, baos);
						encoder.process();
						
						imagePath = "data:" + imageMimeType + ";base64," + new String(baos.toByteArray(), "UTF-8"); // UTF-8 is fine as we just need an ASCII compatible encoding for the Base64 array
						//don't cache the base64 encoded image as imagePath because they are too big
					}
					else
					{
						HtmlResourceHandler imageHandler = 
							getImageHandler() == null 
							? getExporterOutput().getImageHandler() 
							: getImageHandler();
						if (imageHandler != null)
						{
							DataRenderable dataRenderer = 
								getRendererUtil().getDataRenderable(
									renderer,
									new Dimension(availableImageWidth, availableImageHeight),
									ModeEnum.OPAQUE == image.getModeValue() ? image.getBackcolor() : null
									);

							byte[] imageData = dataRenderer.getData(jasperReportsContext);
							
							String fileExtension = 
								getRendererUtil().isSvgData(imageData)
								? RendererUtil.SVG_FILE_EXTENSION
								: JRTypeSniffer.getImageTypeValue(imageData).getFileExtension();

							String imageName = getImageName(getElementIndex(), fileExtension);

							imageHandler.handleResource(imageName, imageData);

							imagePath = imageHandler.getResourcePath(imageName);

							if (dataRenderer == renderer)
							{
								//cache imagePath only for true ImageRenderable instances because the wrapping ones render with different width/height each time
								rendererToImagePathMap.put(renderer.getId(), imagePath);
							}
							//does not make sense to cache null imagePath, in the absence of an image handler
						}
						//does not make sense to cache null imagePath, in the absence of an image handler
					}
				}
			}
			
			String imageMapName = null;
			List<JRPrintImageAreaHyperlink> imageMapAreas = null;

			if (hasAreaHyperlinks)
			{
				Rectangle renderingArea = new Rectangle(image.getWidth(), image.getHeight());
				
				if (renderer instanceof DataRenderable)
				{
					imageMapName = imageMaps.get(new Pair<String,Rectangle>(renderer.getId(), renderingArea));
				}

				if (imageMapName == null)
				{
					Renderable originalRenderer = image.getRenderer();
					imageMapName = "map_" + getElementIndex().toString();
					imageMapAreas = ((AreaHyperlinksRenderable) originalRenderer).getImageAreaHyperlinks(renderingArea);//FIXMECHART
					
					if (renderer instanceof DataRenderable)
					{
						imageMaps.put(new Pair<String,Rectangle>(renderer.getId(), renderingArea), imageMapName);
					}
				}
			}
	
			writer.write(" src=\"");
			if (imagePath != null)
			{
				writer.write(imagePath);
			}
			writer.write("\"");
		
			ScaleImageEnum scaleImage = image.getScaleImageValue();
			
			switch (scaleImage)
			{
				case FILL_FRAME :
				{
					int leftDiff = 0;
					int topDiff = 0;
					int widthDiff = 0;
					int heightDiff = 0;

					JRLineBox box = image.getLineBox();
					if (box != null)
					{
						leftDiff = box.getLeftPadding().intValue();
						topDiff = box.getTopPadding().intValue();
						widthDiff = 
							getInsideBorderOffset(box.getLeftPen().getLineWidth().floatValue(), false)
							+ getInsideBorderOffset(box.getRightPen().getLineWidth().floatValue(), true);
						heightDiff =
							getInsideBorderOffset(box.getTopPen().getLineWidth().floatValue(), false)
							+ getInsideBorderOffset(box.getBottomPen().getLineWidth().floatValue(), true);
					}
					
					writer.write(" style=\"position:absolute;left:");
					writer.write(toSizeUnit(leftDiff));
					writer.write(";top:");
					writer.write(toSizeUnit(topDiff));
					writer.write(";width:");
					writer.write(toSizeUnit(availableImageWidth - widthDiff));
					writer.write(";height:");
					writer.write(toSizeUnit(availableImageHeight - heightDiff));
					writer.write("\"");
		
					break;
				}
				case CLIP :
				{
					double normalWidth = availableImageWidth;
					double normalHeight = availableImageHeight;
		
					if (!isLazy)
					{
						// Image load might fail. 
						Renderable tmpRenderer = 
							net.sf.jasperreports.engine.RenderableUtil.getInstance(jasperReportsContext).getOnErrorRendererForDimension(
								renderersCache,
								renderer, 
								image.getOnErrorTypeValue()
								);
						if (tmpRenderer instanceof DimensionRenderable)
						{
							Dimension2D dimension = ((DimensionRenderable)tmpRenderer).getDimension(jasperReportsContext);
							if (dimension != null)
							{
								normalWidth = dimension.getWidth();
								normalHeight = dimension.getHeight();
							}
						}
					}

					int leftDiff = 0;
					int topDiff = 0;
					int widthDiff = 0;
					int heightDiff = 0;

					JRLineBox box = image.getLineBox();
					if (box != null)
					{
						leftDiff = box.getLeftPadding().intValue();
						topDiff = box.getTopPadding().intValue();
						widthDiff = 
							getInsideBorderOffset(box.getLeftPen().getLineWidth().floatValue(), false)
							+ getInsideBorderOffset(box.getRightPen().getLineWidth().floatValue(), true);
						heightDiff =
							getInsideBorderOffset(box.getTopPen().getLineWidth().floatValue(), false)
							+ getInsideBorderOffset(box.getBottomPen().getLineWidth().floatValue(), true);
					}
					
					writer.write(" style=\"position:absolute;left:");
					writer.write(toSizeUnit((int)(leftDiff + xAlignFactor * (availableImageWidth - widthDiff - normalWidth))));
					writer.write(";top:");
					writer.write(toSizeUnit((int)(topDiff + yAlignFactor * (availableImageHeight - heightDiff - normalHeight))));
					writer.write(";width:");
					writer.write(toSizeUnit((int)normalWidth));
					writer.write(";height:");
					writer.write(toSizeUnit((int)normalHeight));
					writer.write(";clip:rect(");
					writer.write(toSizeUnit((int)(yAlignFactor * (normalHeight - availableImageHeight + heightDiff))));
					writer.write(",");
					writer.write(toSizeUnit((int)(xAlignFactor * normalWidth + (1 - xAlignFactor) * (availableImageWidth - widthDiff))));
					writer.write(",");
					writer.write(toSizeUnit((int)(yAlignFactor * normalHeight + (1 - yAlignFactor) * (availableImageHeight - heightDiff))));
					writer.write(",");
					writer.write(toSizeUnit((int)(xAlignFactor * (normalWidth - availableImageWidth + widthDiff))));
					writer.write(")\"");

					break;
				}
				case RETAIN_SHAPE :
				default :
				{
					double normalWidth = availableImageWidth;
					double normalHeight = availableImageHeight;
		
					if (!isLazy)
					{
						// Image load might fail. 
						Renderable tmpRenderer = 
							net.sf.jasperreports.engine.RenderableUtil.getInstance(jasperReportsContext).getOnErrorRendererForDimension(
								renderersCache,
								renderer, 
								image.getOnErrorTypeValue()
								);
						if (tmpRenderer instanceof DimensionRenderable)
						{
							Dimension2D dimension = ((DimensionRenderable)tmpRenderer).getDimension(jasperReportsContext);
							if (dimension != null)
							{
								normalWidth = dimension.getWidth();
								normalHeight = dimension.getHeight();
							}
						}
					}
		
					int leftDiff = 0;
					int topDiff = 0;
					int widthDiff = 0;
					int heightDiff = 0;

					JRLineBox box = image.getLineBox();
					if (box != null)
					{
						leftDiff = box.getLeftPadding().intValue();
						topDiff = box.getTopPadding().intValue();
						widthDiff = 
							getInsideBorderOffset(box.getLeftPen().getLineWidth().floatValue(), false)
							+ getInsideBorderOffset(box.getRightPen().getLineWidth().floatValue(), true);
						heightDiff =
							getInsideBorderOffset(box.getTopPen().getLineWidth().floatValue(), false)
							+ getInsideBorderOffset(box.getBottomPen().getLineWidth().floatValue(), true);
					}
					
					if (availableImageHeight > 0)
					{
						double ratio = normalWidth / normalHeight;
		
						if( ratio > (double)availableImageWidth / (double)availableImageHeight )
						{
							writer.write(" style=\"position:absolute;left:");
							writer.write(toSizeUnit(leftDiff));
							writer.write(";top:");
							writer.write(toSizeUnit((int)(topDiff + yAlignFactor * (availableImageHeight - heightDiff - (availableImageWidth - widthDiff) / ratio))));
							writer.write(";width:");
							writer.write(toSizeUnit(availableImageWidth - widthDiff));
							writer.write("\"");
						}
						else
						{
							writer.write(" style=\"position:absolute;left:");
							//writer.write(String.valueOf(leftDiff));
							writer.write(toSizeUnit((int)(leftDiff + xAlignFactor * (availableImageWidth - widthDiff - ratio * (availableImageHeight - heightDiff)))));
							writer.write(";top:");
							writer.write(toSizeUnit(topDiff));
							writer.write(";height:");
							writer.write(toSizeUnit(availableImageHeight - heightDiff));
							writer.write("\"");
						}
					}
				}
			}
			
			if (imageMapName != null)
			{
				writer.write(" usemap=\"#" + imageMapName + "\"");
			}
			
			writer.write(" alt=\"\"");
			
			if (hasHyperlinks)
			{
				writer.write(" border=\"0\"");
			}
			
			if (image.getHyperlinkTooltip() != null)
			{
				writer.write(" title=\"");
				writer.write(JRStringUtil.xmlEncode(image.getHyperlinkTooltip()));
				writer.write("\"");
			}
			
			writer.write("/>");

			endHyperlink();
			
			if (imageMapAreas != null)
			{
				writer.write("\n");
				writeImageMap(imageMapName, image, imageMapAreas);
			}
		}
		writer.write("</span>\n");
	}


	protected JRPrintElementIndex getElementIndex()
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < frameInfoStack.size(); i++)
		{
			FrameInfo frameInfo = frameInfoStack.get(i);
			Integer frameIndex = frameInfo.elementIndex;

			sb.append(frameIndex).append("_");
		}
		
		JRPrintElementIndex imageIndex =
			new JRPrintElementIndex(
					reportIndex,
					pageIndex,
					sb.append(elementIndex).toString()
					);
		return imageIndex;
	}


	protected void writeImageMap(String imageMapName, JRPrintImage image, List<JRPrintImageAreaHyperlink> imageMapAreas) throws IOException
	{
		writer.write("<map name=\"" + imageMapName + "\">\n");

		for (ListIterator<JRPrintImageAreaHyperlink> it = imageMapAreas.listIterator(imageMapAreas.size()); it.hasPrevious();)
		{
			JRPrintImageAreaHyperlink areaHyperlink = it.previous();
			JRPrintImageArea area = areaHyperlink.getArea();

			writer.write("  <area shape=\"" + JRPrintImageArea.getHtmlShape(area.getShape()) + "\"");
			writeImageAreaCoordinates(area.getCoordinates());			
			writeImageAreaHyperlink(areaHyperlink.getHyperlink());
			writer.write("/>\n");
		}
		
		if (image.getHyperlinkTypeValue() != HyperlinkTypeEnum.NONE)
		{
			writer.write("  <area shape=\"default\"");
			writeImageAreaCoordinates(new int[]{0, 0, image.getWidth(), image.getHeight()});//for IE
			writeImageAreaHyperlink(image);
			writer.write("/>\n");
		}
		
		writer.write("</map>\n");
	}

	
	protected void writeImageAreaCoordinates(int[] coords) throws IOException
	{
		if (coords != null && coords.length > 0)
		{
			StringBuilder coordsEnum = new StringBuilder(coords.length * 4);
			coordsEnum.append(toZoom(coords[0]));
			for (int i = 1; i < coords.length; i++)
			{
				coordsEnum.append(',');
				coordsEnum.append(toZoom(coords[i]));
			}
			writer.write(" coords=\"" + coordsEnum + "\"");
		}		
	}


	protected void writeImageAreaHyperlink(JRPrintHyperlink hyperlink) throws IOException
	{
		String href = getHyperlinkURL(hyperlink);
		if (href == null)
		{
			writer.write(" nohref=\"nohref\"");
		}
		else
		{
			writer.write(" href=\"" + href + "\"");
			
			String target = getHyperlinkTarget(hyperlink);
			if (target != null)
			{
				writer.write(" target=\"");
				writer.write(target);
				writer.write("\"");
			}
		}

		if (hyperlink.getHyperlinkTooltip() != null)
		{
			writer.write(" title=\"");
			writer.write(JRStringUtil.xmlEncode(hyperlink.getHyperlinkTooltip()));
			writer.write("\"");
		}
	}


	/**
	 *
	 */
	private boolean appendPadding(StringBuilder sb, Integer padding, String side)
	{
		boolean addedToStyle = false;
		
		if (padding.intValue() > 0)
		{
			sb.append("padding");
			if (side != null)
			{
				sb.append("-");
				sb.append(side);
			}
			sb.append(": ");
			sb.append(toSizeUnit(padding.intValue()));
			sb.append("; ");

			addedToStyle = true;
		}
		
		return addedToStyle;
	}


	/**
	 *
	 */
	private boolean appendPen(StringBuilder sb, JRPen pen, String side)
	{
		boolean addedToStyle = false;
		
		float borderWidth = pen.getLineWidth().floatValue();
		if (0f < borderWidth && borderWidth < 1f)
		{
			borderWidth = 1f;
		}

		String borderStyle = null;
		switch (pen.getLineStyleValue())
		{
			case DOUBLE :
			{
				borderStyle = "double";
				break;
			}
			case DOTTED :
			{
				borderStyle = "dotted";
				break;
			}
			case DASHED :
			{
				borderStyle = "dashed";
				break;
			}
			case SOLID :
			default :
			{
				borderStyle = "solid";
				break;
			}
		}

		if (borderWidth > 0f)
		{
			sb.append("border");
			if (side != null)
			{
				sb.append("-");
				sb.append(side);
			}
			
			sb.append(": ");
			sb.append(toSizeUnit((int)borderWidth));
			
			sb.append(" ");
			sb.append(borderStyle);

			sb.append(" ");
			sb.append(JRColorUtil.getCssColor(pen.getLineColor()));
			sb.append("; ");

			addedToStyle = true;
		}

		return addedToStyle;
	}


	/**
	 *
	 */
	private void appendId(JRPrintElement element) throws IOException
	{
		String dataAttr = getDataAttributes(element);
		if (dataAttr != null)
		{
			writer.write(dataAttr);
		}
	}
	
		
	/**
	 *
	 */
	public String getDataAttributes(JRPrintElement element)
	{
		StringBuilder sb = new StringBuilder();
		String id = getPropertiesUtil().getProperty(element, JRHtmlExporter.PROPERTY_HTML_ID);
		if (id != null)
		{
			sb.append(" id=\"" + id + "\"");
		}
		String clazz = getPropertiesUtil().getProperty(element, JRHtmlExporter.PROPERTY_HTML_CLASS);
		if (clazz != null)
		{
			sb.append(" class=\"" + clazz + "\"");
		}
		String columnUuid = getPropertiesUtil().getProperty(element, HeaderToolbarElement.PROPERTY_COLUMN_UUID);//FIXMEJIVE register properties like this in a pluggable way; extensions?
		if (columnUuid != null)
		{
			sb.append(" data-coluuid=\"" + columnUuid + "\"");
		}
		String cellId = getPropertiesUtil().getProperty(element, HeaderToolbarElement.PROPERTY_CELL_ID);
		if (cellId != null)
		{
			sb.append(" data-cellid=\"" + cellId + "\"");
		}
		
		return sb.length() > 0 ? sb.toString() : null;
	}
	
		
	/**
	 *
	 */
	private void setPageLimits(List<JRPrintElement> elements) throws IOException, JRException
	{
		if (elements != null && elements.size() > 0)
		{
			JRPrintElement element;
			for(int i = 0; i < elements.size(); i++)
			{
				elementIndex = i;
				
				element = elements.get(i);
				
				if (filter == null || filter.isToExport(element))
				{
					topLimit = element.getY() < topLimit ? element.getY() : topLimit;
					leftLimit = element.getX() < leftLimit ? element.getX() : leftLimit;
					rightLimit = (element.getX() + element.getWidth()) > rightLimit ? element.getX() + element.getWidth() : rightLimit;
					bottomLimit = (element.getY() + element.getHeight()) > bottomLimit ? element.getY() + element.getHeight() : bottomLimit;
				}
			}
		}
	}


	public void exportFrame(JRPrintFrame frame) throws IOException, JRException
	{
		writer.write("<div");
		
		appendId(frame);

		StringBuilder styleBuilder = new StringBuilder();
		appendPositionStyle(frame, frame, styleBuilder);
		appendSizeStyle(frame, frame, styleBuilder);
		appendBackcolorStyle(frame, styleBuilder);
		appendBorderStyle(frame.getLineBox(), styleBuilder);

		if (styleBuilder.length() > 0)
		{
			writer.write(" style=\"");//overflow:hidden;");
			writer.write(styleBuilder.toString());
			writer.write("\"");
		}
		
		if (frame.getPropertiesMap() != null && frame.getPropertiesMap().containsProperty(HeaderToolbarElement.PROPERTY_TABLE_UUID)) {
			writer.write(" data-uuid=\"");
			writer.write(frame.getPropertiesMap().getProperty(HeaderToolbarElement.PROPERTY_TABLE_UUID));
			writer.write("\"");
			writer.write(" class=\"jrtableframe\"");
		}


		writer.write(">\n");
		
		FrameInfo frameInfo = new FrameInfo();
		frameInfo.elementIndex = elementIndex;
		JRLineBox box = frame.getLineBox();
		if (box != null)
		{
			frameInfo.leftInsideBorderOffset =
				- box.getLeftPadding() + getInsideBorderOffset(box.getLeftPen().getLineWidth().floatValue(), false);
			frameInfo.topInsideBorderOffset =
				- box.getTopPadding() + getInsideBorderOffset(box.getTopPen().getLineWidth().floatValue(), false);
		}
		frameInfoStack.add(frameInfo);

		exportElements(frame.getElements());

		frameInfoStack.remove(frameInfoStack.size() - 1);

		writer.write("</div>\n");
	}


	protected void exportGenericElement(JRGenericPrintElement element) throws IOException
	{
		GenericElementHtmlHandler handler = (GenericElementHtmlHandler) 
				GenericElementHandlerEnviroment.getInstance(getJasperReportsContext()).getElementHandler(
						element.getGenericType(), XHTML_EXPORTER_KEY);
		
		if (handler == null)
		{
			if (log.isDebugEnabled())
			{
				log.debug("No XHTML generic element handler for " 
						+ element.getGenericType());
			}
		}
		else if (handler.toExport(element))
		{
//			writer.write("<div");
//
//			StringBuilder styleBuilder = new StringBuilder();
//
//			appendPositionStyle(element, styleBuilder);
//			appendSizeStyle(element, (JRBoxContainer)null, styleBuilder);
//			appendBackcolorStyle(element, styleBuilder);
//			
//			if (styleBuilder.length() > 0)
//			{
//				writer.write(" style=\"");
//				writer.write(styleBuilder.toString());
//				writer.write("\"");
//			}
//
//			writer.write(">");

			String htmlFragment = handler.getHtmlFragment(exporterContext, element);
			if (htmlFragment != null)
			{
				writer.write(htmlFragment);
			}

//			writer.write("</div>\n");
		}
	}

	public Map<net.sf.jasperreports.engine.JRExporterParameter,Object> getExportParameters()
	{
		return parameters;
	}

	@Override
	public String getExporterPropertiesPrefix()
	{
		return XHTML_EXPORTER_PROPERTIES_PREFIX;
	}

	public JasperPrint getExportedReport()
	{
		return jasperPrint;
	}


	public String toSizeUnit(float size)
	{
		Number number = toZoom(size);
		if (number.intValue() == number.floatValue())
		{
			number = number.intValue();
		}

		return String.valueOf(number) + getCurrentItemConfiguration().getSizeUnit().getName();
	}

	/**
	 * @deprecated Replaced by {@link #toSizeUnit(float)}.
	 */
	public String toSizeUnit(int size)
	{
		return toSizeUnit((float)size);
	}

	public float toZoom(float size)
	{
		float zoom = DEFAULT_ZOOM;
		
		Float zoomRatio = getCurrentItemConfiguration().getZoomRatio();
		if (zoomRatio != null)
		{
			zoom = zoomRatio.floatValue();
			if (zoom <= 0)
			{
				throw 
					new JRRuntimeException(
						EXCEPTION_MESSAGE_KEY_INVALID_ZOOM_RATIO,  
						new Object[]{zoom} 
						);
			}
		}

		return (zoom * size);
	}

	/**
	 * @deprecated Replaced by {@link #toZoom(float)}.
	 */
	public int toZoom(int size)
	{
		return (int)toZoom((float)size);
	}
	
	
	@Override
	public String getExporterKey()
	{
		return XHTML_EXPORTER_KEY;
	}
	
	private static class FrameInfo
	{
		protected int elementIndex;
		protected int leftInsideBorderOffset;
		protected int topInsideBorderOffset;
	}
}

