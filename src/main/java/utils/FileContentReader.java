package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;

public class FileContentReader {
	private String uploadFolderPath = System.getProperty("user.dir")+"/src/main/resources/static/uploads";
	
    public String getContent(String fileName) throws IOException {
        String fileContent = "";
        File file = new File(uploadFolderPath + "/" + fileName);

        // Determine the file extension
        String fileExtension = getFileExtension(fileName);

        if (!isImageFile(fileExtension)) {
            if (fileExtension.equalsIgnoreCase("pdf")) {
                // Read PDF file
                fileContent = readPdfFile(file);
            } else if (fileExtension.equalsIgnoreCase("doc") || fileExtension.equalsIgnoreCase("docx")) {
                // Read Word document
                fileContent = readWordFile(file, fileExtension);
            } else {
                // Read text file
                fileContent = readTextFile(file);
            }
        }

        return fileContent;
    }

    public String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex != -1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }

    public boolean isImageFile(String fileExtension) {
        String[] imageExtensions = { "jpg", "jpeg", "png" };
        for (String imageExt : imageExtensions) {
            if (fileExtension.equalsIgnoreCase(imageExt)) {
                return true;
            }
        }
        return false;
    }

    private String readTextFile(File file) throws IOException {
        StringBuilder fileContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                fileContent.append(line).append("\n");
            }
        }
        return fileContent.toString();
    }

    private String readPdfFile(File file) throws IOException {
		PDDocument pdfFile = Loader.loadPDF(file);
		PDFTextStripper pdfTextStripper = new PDFTextStripper();
		String fileContent = pdfTextStripper.getText(pdfFile);
		pdfFile.close();

		return fileContent;
    }
    
    private String readWordFile(File file, String fileExtension) throws IOException {
        String fileContent = "";
        FileInputStream fis = new FileInputStream(file);
        if (fileExtension.equalsIgnoreCase("doc")) {
            HWPFDocument doc = new HWPFDocument(fis);
            try (WordExtractor wordExtractor = new WordExtractor(doc)) {
				fileContent = wordExtractor.getText();
			}
        } else if (fileExtension.equalsIgnoreCase("docx")) {
            XWPFDocument docx = new XWPFDocument(fis);
            try (XWPFWordExtractor wordExtractor = new XWPFWordExtractor(docx)) {
				fileContent = wordExtractor.getText();
			}
        }
        fis.close();
        return fileContent;
    }
}