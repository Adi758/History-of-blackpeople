package com.history.blackpeople.Controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.history.blackpeople.Model.Items;
import com.history.blackpeople.Model.Users;
import com.history.blackpeople.Request.UpdateAccessRequest;
import com.history.blackpeople.Service.blacksService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import utils.CommonUtils;

@Controller
public class blacksController {
    
    @Autowired
    private blacksService service;
    private String uploadDirectory = System.getProperty("user.dir")+"/src/main/resources/static/uploads";
    int pageSize = 5;

    @GetMapping(value={"/login", "/"})
    public String login(){
        return "login";
    }

    @GetMapping("/signup")
    public String signup(){
        return "signup";
    }
    
    @GetMapping("/contact")
    public String contant(){
        return "contact";
    }
    
    @GetMapping("/about")
    public String about(){
        return "about";
    }
    
    @PostMapping("/register")
    public String register(@ModelAttribute Users user){
        service.addUser(user);
        return "login";
    }

    @PostMapping("/login-validation")
    public String loginValidation(HttpServletRequest request, Model model, String email, String password){
        Users user = service.validateUser(email,password);
        if(user==null){
            model.addAttribute("error", "Invalid Credentials, Try Again...!!!");
            return "login";
        }
        else{
        	HttpSession session = request.getSession();
            session.setAttribute("username", user.getFirstName() + " " + user.getLastName());
            session.setAttribute("email", user.getEmail());
            session.setAttribute("userType", user.getUserType());
            return "redirect:/dashboard";
        }
    }
    
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "login";
    }
    
    @PostMapping("/file-searching")
    public String fileSearching(HttpServletRequest request, Model model, String searchedWord, String year, String decade) throws Exception{
    	HttpSession session = request.getSession(false);
    	if(session != null) {
	        List<Items> items = service.findAllItems();
	        if(!year.isBlank()) {
	        	items = items.stream()
	                    .filter(item -> item.getPublishDate().getYear() == Integer.parseInt(year))
	                    .collect(Collectors.toList());
	        }
	        if(!decade.isBlank()) {
	        	String[] rangeBounds = decade.split("-");
	        	int startYear = Integer.parseInt(rangeBounds[0]);
	            int endYear = Integer.parseInt(rangeBounds[1]);

	            // Assuming the start of the year (January 1)
	            LocalDate startDate = LocalDate.of(startYear, 1, 1);
	            LocalDate endDate = LocalDate.of(endYear, 1, 1);
	
	            items = items.stream()
	                    .filter(item -> {
	                        LocalDate publishDate = item.getPublishDate();
	                        return !publishDate.isBefore(startDate) && !publishDate.isAfter(endDate);
	                    })
	                    .collect(Collectors.toList());
	        }
	        HashMap<Long, String> allFileContent = service.getContentAllFiles(items, searchedWord.toLowerCase(), searchedWord.isBlank() ? true: false);
	        String fileDisplayCode = service.generateFileDisplayCode(items, allFileContent, (String) session.getAttribute("userType"));
	        model.addAttribute("fileDisplayCode", fileDisplayCode);
	        return "dashboard";
    	}
    	return "login";
    }

    @SuppressWarnings("unchecked")
	@GetMapping("/dashboard")
	public String dashboard(HttpServletRequest request, Model model, @RequestParam(required = false) Integer page){
    	HttpSession session = request.getSession(false);
    	if(session != null) {
	        String searchKey = (String) model.getAttribute("searchKey");
	        
    		if(model.getAttribute("items") == null) {
    			model.addAttribute("fileType", "all");
        		model.addAttribute("totalItems", service.findAllItems().size());
        		model.addAttribute("items", service.findPaginatedItemsWithSearch(page != null ? page : 0, pageSize, searchKey).toList());
    		}
	    		
    		model.addAttribute("fileDisplayCode", service.loadFilesByItems((List<Items>) model.getAttribute("items"), (String) session.getAttribute("userType")));
	    	model.addAttribute("buttonCode", service.getPageNumbersCode((Integer) model.getAttribute("totalItems"), pageSize, (String) model.getAttribute("fileType")));
	    	model.addAttribute("usersList", service.findByUserType("user"));
	        return "dashboard";
    	}
    	return "login";
    }
    
    @PostMapping("/upload")
    public String uploaded(HttpServletRequest request, Model model, @RequestParam("itemName") MultipartFile itemName, @RequestParam("itemCaption") String itemCaption,
    		@RequestParam("authorName") String authorName, @RequestParam("publishDate") String publishDate) throws Exception {
    	Items item = new Items();
        String originalFilename = itemName.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        
        // Check if the file extension is allowed
        if (CommonUtils.isFileExtensionAllowed(fileExtension)) {
            item.setItemName(CommonUtils.generateUniqueFileName(originalFilename));
            item.setItemCaption(itemCaption);
            item.setAuthorName(authorName);
            item.setPublishDate(LocalDate.parse(publishDate, DateTimeFormatter.ISO_LOCAL_DATE));
            Path fileNameAndPath = Paths.get(uploadDirectory, item.getItemName());
            try {
                Files.write(fileNameAndPath, itemName.getBytes());
                model.addAttribute("message", service.saveFileuploaded(item));
            } catch (IOException e) {
                e.printStackTrace();
                model.addAttribute("message", "Something went wrong while uploading the file.");
            }
        } else {
            model.addAttribute("message", "File type not allowed. Please upload a .jpg, .jpeg, .png, .doc, .docx, .txt, or .pdf file.");
        }
        
        return dashboard(request, model, null);
    }
    
    @GetMapping("/files/{type}")
    public String files(HttpServletRequest request, Model model, @PathVariable String type, @RequestParam(required = false) Integer page) {
    	List<Items> items = null;
    	Integer totalItems = null;
    	if(type.equalsIgnoreCase("documents")) {
    		items = service.findPaginatedItemsWithSearch(page != null ? page : 0, pageSize, ".doc").toList();
    		totalItems = service.searchByFileName(".doc").size();
    	} else if (type.equalsIgnoreCase("pdfs")) {
    		items = service.findPaginatedItemsWithSearch(page != null ? page : 0, pageSize, ".pdf").toList();
    		totalItems = service.searchByFileName(".pdf").size();
    	} else if (type.equalsIgnoreCase("txt")) {
    		items = service.findPaginatedItemsWithSearch(page != null ? page : 0, pageSize, ".txt").toList();
    		totalItems = service.searchByFileName(".txt").size();
    	} else if(type.equalsIgnoreCase("images")) {
    		List<String> searchWords = Arrays.asList(".png", ".jpg", ".jpeg");
    		items = service.findPaginatedItemsWithSearchList(page != null ? page : 0, pageSize, searchWords).toList();
    		totalItems = service.findItemsContainingSearchList(searchWords).size();
    	} else if(type.equalsIgnoreCase("sort-by-years")) {
    		items = service.getItemsSortedByYear(page != null ? page : 0, pageSize).toList();
    		totalItems = service.findAllItems().size();
    	} else if (type.equalsIgnoreCase("sort-by-decades")) {
    		items = service.getItemsSortedByDecade(page != null ? page : 0, pageSize).toList();
    		totalItems = service.findAllItems().size();
    	}
    	model.addAttribute("fileType", type);
    	model.addAttribute("items", items);
    	model.addAttribute("totalItems", totalItems != null ? totalItems : 0);
    	
    	return dashboard(request, model, page);
    }
    
    @GetMapping("/download/{itemID}")
    public ResponseEntity<byte[]> downloadAsTXT(HttpServletRequest request, @PathVariable Long itemID) throws Exception {
    	HttpSession session = request.getSession(false);
    	if(session != null) {
	        String content = service.getFileContentById(itemID);
	
	        // Convert the content string to bytes
	        byte[] contentBytes = content.getBytes();
	
	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.TEXT_PLAIN);
	        headers.setContentLength(contentBytes.length);
	        headers.setContentDispositionFormData("attachment", CommonUtils.generateUniqueFileName("download.txt")); // Set the file name
	
	        return new ResponseEntity<>(contentBytes, headers, HttpStatus.OK);
    	}
    	return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    @RequestMapping("/delete")
    public String deleted(HttpServletRequest request, Model model,@RequestParam("itemID") Long itemID) {
    	service.removeFile(itemID);
    	return dashboard(request, model, null);
    }
    
    @PostMapping("/users/update-access")
    @ResponseBody
    public ResponseEntity<String> createPerson(@RequestBody UpdateAccessRequest request) {
    	try {
    		service.updateUserAccess(request);
        } catch(Exception e) {
        	return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
