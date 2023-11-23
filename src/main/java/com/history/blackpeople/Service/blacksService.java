package com.history.blackpeople.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.history.blackpeople.Model.Items;
import com.history.blackpeople.Model.Users;
import com.history.blackpeople.Repository.ItemsRepo;
import com.history.blackpeople.Repository.UsersRepo;
import com.history.blackpeople.Request.UpdateAccessRequest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import utils.CommonUtils;
import utils.FileContentReader;

@Service
public class blacksService {

    @Autowired
    private UsersRepo URepo;
    @Autowired
    private ItemsRepo IRepo;
    @Autowired
    private EntityManager entityManager;
    private FileContentReader fileContentReader = new FileContentReader();

    public Items getItemById(Long itemId) {
    	return IRepo.findById(itemId).orElse(null);
    }
    
    public void addUser(Users user) {
        URepo.save(user);
        System.out.println("User saved successfully");
    }

    public Users validateUser(String email, String password){
        Users user = URepo.findByemail(email);
        if(user != null && user.getPassword().equals(password))
            return user;
        else
            return null;        
    }
    
    public List<Users> findByUserType(String userType) {
    	List<Users> users = URepo.findByUserType(userType);
    	return users;
    }
    
	public List<Items> searchByFileName(String searchedWord){	
	    return IRepo.findByItemNameContaining(searchedWord);
	}
	
    public List<Items> findAllItems(){
    	return IRepo.findAll();
    }

    public String saveFileuploaded(Items item){
        System.out.println("inside the save file upload service...");

        if(IRepo.save(item)!=null) {
        	return "File Uploaded Successfully.";
        }
        else
        	return "Something went wrong while uploading the file.";
    }

    public Page<Items> findPaginatedItemsWithSearch(Integer offset, Integer pageSize, String search){
    	Pageable pageable = PageRequest.of(offset, pageSize);
        Page<Items> items;

        if (search != null && !search.isEmpty()) {
            items = IRepo.findByItemNameContaining(search, pageable);
        } else {
            items = IRepo.findAll(pageable);
        }

        return items;
    }
    
    public Page<Items> findPaginatedItemsWithSearchList(Integer offset, Integer pageSize, List<String> searchList) {
        Pageable pageable = PageRequest.of(offset, pageSize);
        Page<Items> itemsList;
        if (searchList != null && !searchList.isEmpty()) {
        	List<Items> items = findItemsContainingSearchList(searchList, pageable);
        	itemsList = new PageImpl<>(items, pageable, items.size());
        } else {
            itemsList = IRepo.findAll(pageable);
        }
        return itemsList;
    }
    
    public List<Items> findItemsContainingSearchList(List<String> searchList, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Items> query = cb.createQuery(Items.class);
        Root<Items> root = query.from(Items.class);

        Predicate[] predicates = searchList.stream()
            .map(search -> cb.like(cb.lower(root.get("itemName")), "%" + search.toLowerCase() + "%"))
            .toArray(Predicate[]::new);

        Predicate finalPredicate = cb.or(predicates);
        query.where(finalPredicate);

        return entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();
    }
    
    public List<Items> findItemsContainingSearchList(List<String> searchList) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Items> query = cb.createQuery(Items.class);
        Root<Items> root = query.from(Items.class);

        Predicate[] predicates = searchList.stream()
            .map(search -> cb.like(cb.lower(root.get("itemName")), "%" + search.toLowerCase() + "%"))
            .toArray(Predicate[]::new);

        Predicate finalPredicate = cb.or(predicates);
        query.where(finalPredicate);

        return entityManager.createQuery(query).getResultList();
    }


	public void removeFile(Long itemID) {
		Items item = getItemById(itemID);
		File deleteFile = new File(System.getProperty("user.dir")+"/src/main/resources/static/uploads/" + item.getItemName());
		deleteFile.delete();
		
		IRepo.deleteById(itemID);
	}
	
	public String loadFilesByItems(List<Items> items, String userType) {
    	String fileDisplayCode = "<ul id='files' class='grid-container'>\n";
    	if(items.size() > 0) {
	    	for(Items item: items) {
	    		fileDisplayCode = fileDisplayCode.concat("<li class='grid-item'><a href='/uploads/" + item.getItemName() + "' target='_blank'>" + item.getItemName() + "</a>");
	    		if(!fileContentReader.isImageFile(fileContentReader.getFileExtension(item.getItemName()))) {
	    			fileDisplayCode = fileDisplayCode.concat("<p>" + item.getItemCaption() + "</p><div> <a href='/download/" + item.getItemID() + "' target='_blank'>Download as text file</a> <span class='date'>" + item.getCreatedAt() + "</span> </div>");
	    		} else {
	    			fileDisplayCode = fileDisplayCode.concat("<p>" + item.getItemCaption() + "<span class='date'>" + CommonUtils.convertDateToReadableFormat(item.getCreatedAt()) + "</span></p>");
	    		}
	    		fileDisplayCode = fileDisplayCode.concat("<div><p>Author Name: " + (item.getAuthorName() != null && !item.getAuthorName().isBlank() ? item.getAuthorName() : "Not Found") + "<span class='date'>Publish Date: " + CommonUtils.convertDateToReadableFormat(item.getPublishDate()) + "</span></p></div>");
	    		if(userType.equals("admin")) {
	    			fileDisplayCode = fileDisplayCode.concat("<form class='deletebtn' action='/delete' method='post'><input type='hidden' name='itemID' value='" + item.getItemID() + "'><input type='submit' value='X'></form></li>");
	    		}
	    	}
    	} else {
    		fileDisplayCode = fileDisplayCode.concat("<li class='grid-item'><p>No files of selected type found.</p></li>");
    	}
    	fileDisplayCode = fileDisplayCode.concat("</ul>");
    	
    	return fileDisplayCode;
    }
	
	// pagination work
	public String getPageNumbersCode(int size, int pageSize, String type) {
		int noOfPages = (size/pageSize);		
		if((size%pageSize)==0)
			return buttonCode(noOfPages, type);
		else
			return buttonCode(noOfPages+1, type);
		
	}
	
	public String buttonCode(int noOfPages, String type) {
		String pageNoCode = "";
		if(type == "all") {
			for(int i=0;i<noOfPages;i++) {
				pageNoCode = pageNoCode.concat("<a href='/dashboard?page="+i+"'>"+(i+1)+"</a>\n");
			}
		} else {
			for(int i=0;i<noOfPages;i++) {
				pageNoCode = pageNoCode.concat("<a href='/files/"+ type +"?page="+i+"'>"+(i+1)+"</a>\n");
			}
		}
		return pageNoCode;
	}
	
	public String generateFileDisplayCode(List<Items> items, HashMap<Long, String> allFileContent, String userType) {
		String fileDisplayCode = "<ul id='files' class='grid-container'>\n";
		TreeMap<Long, String> sorted = new TreeMap<>();
        sorted.putAll(allFileContent);
		
		for(Map.Entry matchedContent : sorted.entrySet()){
			Items item = getItemById((Long) matchedContent.getKey());
			
			fileDisplayCode = fileDisplayCode.concat("<li class='grid-item'>\n"
					+ "<div class='name_expand'>"
	        		+ "<a href='/uploads/"+item.getItemName()+"' target='_blank'>"+item.getItemName()+"</a>\n"
	        		+ "<a href='#' onclick='toggleFindData("+item.getItemID()+")' class='toggle-data'><span id='"+item.getItemID()+"rotate'>&#10140;</span></a>\n"
					+ "</div>"
					+ "<p>"+item.getItemCaption()+"</p>\n"
					+ "<div><p>Author Name: " + (item.getAuthorName() != null && !item.getAuthorName().isBlank() ? item.getAuthorName() : "Not Found") + "<span class='date'>Publish Date: " + CommonUtils.convertDateToReadableFormat(item.getPublishDate()) + "</span></p></div>"
					+ "<div class='paragraphs' id='"+item.getItemID()+"_para'>\n"
					+ "<p>"+matchedContent.getValue()+"</p>\n"
					+ "</div>\n");
					if(userType.equals("admin")) {
						fileDisplayCode = fileDisplayCode.concat("<form class='deletebtn' action='/delete' method='post'>\n"
							+ "<input type='hidden' name='itemID' value='" + item.getItemID() + "'>\n"
							+ "<input type='submit' value='X'>\n"
							+ "</form>\n");
					}
					
					fileDisplayCode = fileDisplayCode.concat("</li>\n");
		}  
	    fileDisplayCode = fileDisplayCode.concat("</ul>\n");
		
		return fileDisplayCode;
	}
	
	public Page<Items> getItemsSortedByYear(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("publishDate")));
        return IRepo.findAll(pageable);
    }
	
    public Page<Items> getItemsSortedByDecade(int page, int size) {
        List<Items> sortedItems = IRepo.findAll(); // Fetch items

        // Sort items into decades
        List<Items> itemsSortedByDecade = sortItemsByDecade(sortedItems);

        Pageable pageable = PageRequest.of(page, size);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), itemsSortedByDecade.size());

        List<Items> pageItems = itemsSortedByDecade.subList(start, end);
        return new PageImpl<>(pageItems, pageable, itemsSortedByDecade.size());
    }

    public static List<Items> sortItemsByDecade(List<Items> items) {
        Collections.sort(items, new Comparator<Items>() {
            @Override
            public int compare(Items item1, Items item2) {
                int decade1 = item1.getPublishDate().getYear() / 10;
                int decade2 = item2.getPublishDate().getYear() / 10;
                
                return Integer.compare(decade2, decade1);
            }
        });
        return items;
    }
    
    public String getFileContentById(Long id) throws Exception {
    	String content = null;
    	Items item = getItemById(id);
    	if(item != null) {
    		content = fileContentReader.getContent(item.getItemName());
    	}
    	return content;
    }
	
	// search work
    public String highlightAndCountOccurrences(String content, String searchKeyword) {
        if (content == null || content.isEmpty() || searchKeyword == null || searchKeyword.isEmpty()) {
            return content;
        }

        content = escapeHtml(content);

        // Split the searchKeyword into individual terms
        String[] keywords = searchKeyword.split("\\s+");
        List<String> andKeywords = new ArrayList<>();
        List<String> orKeywords = new ArrayList<>();
        int occurrenceCount = 0;
        
        boolean useAnd = true;
        for (String keyword : keywords) {
            if ("and".equalsIgnoreCase(keyword)) {
                useAnd = true;
            } else if ("or".equalsIgnoreCase(keyword)) {
                useAnd = false;
            } else if (useAnd) {
                andKeywords.add(keyword);
            } else {
                orKeywords.add(keyword);
            }
        }

        for (String andKeyword : andKeywords) {
            Pattern pattern = Pattern.compile(Pattern.quote(andKeyword), Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(content);
            if (!matcher.find()) {
                // If any 'and' keyword is not found, return null
                return null;
            }
            while (matcher.find()) {
                occurrenceCount++;
            }
        }
        
        for (String orKeyword : orKeywords) {
            Pattern pattern = Pattern.compile(Pattern.quote(orKeyword), Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                while (matcher.find()) {
                    occurrenceCount++;
                }
            }
        }
        
        if (occurrenceCount <= 0) {
            return null;
        }

        for (String andKeyword : andKeywords) {
            content = content.replaceAll("(?i)" + Pattern.quote(andKeyword), "<span>$0</span>");
        }
        
        for (String orKeyword : orKeywords) {
            content = content.replaceAll("(?i)" + Pattern.quote(orKeyword), "<span>$0</span>");
        }

        String htmlContent = "<p>Occurrences of '" + searchKeyword + "': " + occurrenceCount + "</p>";
        htmlContent += "<p>Content: " + content + "</p";

        return htmlContent;
    }

    private String escapeHtml(String content) {
        return content
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
    
	public HashMap<Long, String> getContentAllFiles(List<Items> items, String searchedWord, Boolean allowNull) throws Exception {
		HashMap<Long, String> allFileContent = new HashMap<Long, String>();
		for(Items item: items) {
			String matchedContent = highlightAndCountOccurrences(fileContentReader.getContent(item.getItemName()), searchedWord);
			if((matchedContent != null && !matchedContent.isBlank()) || allowNull) {
				allFileContent.put(item.getItemID(), matchedContent);
			}
		}
		
		return allFileContent;
	}
	
	public void updateUserAccess(UpdateAccessRequest request) {
		Users user = URepo.findByemail(request.getEmail());
        if(user != null) {
        	user.setUserType(request.getUserType());
        	URepo.save(user);
        } 
	}
}
