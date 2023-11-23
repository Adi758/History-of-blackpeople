package utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class CommonUtils {
	
	public static String generateUniqueFileName(String originalFileName) {
        String sanitizedFileName = originalFileName.replaceAll(" ", "_");
        String fileExtension = sanitizedFileName.substring(sanitizedFileName.lastIndexOf("."));
        long currentTimeMillis = System.currentTimeMillis();
        String uniqueFileName = sanitizedFileName.replace(fileExtension, "") + currentTimeMillis + fileExtension;

        return uniqueFileName;
    }

	public static boolean isFileExtensionAllowed(String fileExtension) {
        List<String> allowedExtensions = Arrays.asList("jpg", "jpeg", "png", "doc", "docx", "pdf", "txt");
        return allowedExtensions.contains(fileExtension);
    }
	
	public static String convertDateToReadableFormat(LocalDateTime dateTime) {
    	if(dateTime != null) {
	        DateTimeFormatter formatter = null;
	        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	        return dateTime.format(formatter);
    	}
    	return "Not Found";
    }
	
	public static String convertDateToReadableFormat(LocalDate date) {
	    if (date != null) {
	        DateTimeFormatter formatter = null;
	        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	        return date.format(formatter);
	    }
	    return "Not Found";
	}
}
