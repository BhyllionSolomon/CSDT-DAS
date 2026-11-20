package com.kdu.csdtdas.backend.service;
import org.apache.pdfbox.Loader;import org.apache.pdfbox.pdmodel.*;import org.apache.pdfbox.pdmodel.common.PDRectangle;import org.apache.pdfbox.pdmodel.font.*;import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.poi.xwpf.usermodel.*;import org.apache.poi.wp.usermodel.HeaderFooterType;import java.io.*;import java.time.*;import java.time.ZoneId;
import org.springframework.stereotype.Service;
@Service
public class ExamWatermarkService{
 private static final ZoneId LAGOS=ZoneId.of("Africa/Lagos");
 public byte[] watermark(byte[] input,String contentType,String identity){String stamp="CONFIDENTIAL - "+identity+" - "+LocalDateTime.now(LAGOS);
  try{if(contentType!=null&&contentType.toLowerCase().contains("pdf")){try(PDDocument doc=Loader.loadPDF(input);ByteArrayOutputStream out=new ByteArrayOutputStream()){
    for(PDPage page:doc.getPages()){PDRectangle box=page.getMediaBox();try(PDPageContentStream cs=new PDPageContentStream(doc,page,PDPageContentStream.AppendMode.APPEND,true,true)){
      cs.beginText();cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),9);cs.setNonStrokingColor(130,130,130);cs.newLineAtOffset(30,20);cs.showText(stamp);cs.endText();
      cs.beginText();cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),9);cs.setNonStrokingColor(150,150,150);cs.newLineAtOffset(30,box.getHeight()-20);cs.showText(stamp);cs.endText();
    }}doc.save(out);return out.toByteArray();}}
   if(contentType!=null&&contentType.toLowerCase().contains("wordprocessingml")){try(XWPFDocument doc=new XWPFDocument(new ByteArrayInputStream(input));ByteArrayOutputStream out=new ByteArrayOutputStream()){
    XWPFHeader header=doc.createHeader(HeaderFooterType.DEFAULT);XWPFParagraph p=header.createParagraph();p.setAlignment(ParagraphAlignment.CENTER);XWPFRun run=p.createRun();run.setBold(true);run.setFontSize(9);run.setColor("888888");run.setText(stamp);doc.write(out);return out.toByteArray();}}
   throw new IllegalArgumentException("Unsupported confidential document type.");
  }catch(Exception e){throw new IllegalStateException("Could not watermark confidential examination paper; download blocked.",e);}
 }
}