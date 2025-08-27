package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;

import jakarta.servlet.http.HttpSession;
import jakarta.websocket.Session;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

    private final AuthenticationManager authenticationManager;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;

    UserController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }
	
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
		System.out.println("USERNAME: " + userName );
		
		User user = userRepository.getUserByUserName(userName);
		
		System.out.println("USER: " + user);
		
		model.addAttribute("user", user);
			
	}
	
	
	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
		
		model.addAttribute("title", "User Dashboard");
		return "normal/user_dashboard";
	}
	
	
	// open add contact form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}
	
	//Processing add contact form 
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file, Principal principal, HttpSession session) { 
		
		try {
		String name = principal.getName();
		User user = this.userRepository.getUserByUserName(name);
		
		//processing and uploading file
		
		if(file.isEmpty()) {
			
			System.out.println("File is Empty");
			contact.setImage("contact.png");
			
		}
		else {
			contact.setImage(file.getOriginalFilename());
			
			File saveFile = new ClassPathResource("static/img").getFile();
			
			Path path = Paths.get(saveFile.getAbsolutePath() + File.separator+file.getOriginalFilename());
			
			
			Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
		
			System.out.println("Image is Uploaded");
		}
		
		
		contact.setUser(user);
		
		user.getContacts().add(contact);
		
		
		
		this.userRepository.save(user);
	
		System.out.println("DATA "+ contact);
		
		System.out.println(" Contact Added to database");
		
		//message success....
		session.setAttribute("message", new com.smart.helper.Message("Your contact is added !! Add more..", "success"));
		
	   
		
		
		
		}catch (Exception e) {
			System.out.println("ERROR "+ e.getMessage());
			e.printStackTrace();
			//message error
			session.setAttribute("message", new com.smart.helper.Message("Something Want Wrong !! try Again..", "danger"));
		}
		
		return "normal/add_contact_form";
		
	}
	
	
	//show contact handler
	//per page = 5
	//current page = 0 
	
	@GetMapping("/show_contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model model, Principal principal) {
		model.addAttribute("title", "Show User Contacts");
		
		//User ke name se User Nikalo
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		//current page = 0 
		//per page = 5
		Pageable pageable = PageRequest.of(page, 5);
		
		//contact list
		Page<Contact> contacts = this.contactRepository.findContactByUser(user.getId(),pageable);
		
		model.addAttribute("contacts", contacts);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPage",contacts.getTotalPages());
		
		return "normal/show_contacts";
	}
	
	//single user details
	@GetMapping("/{cId}/contact")
	public String showContactDetails(@PathVariable("cId") Integer cId, Model model, Principal principal) {
		
		System.out.println("CID "+cId);
		
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		//User kisi aur ka contact dekh na sakhe isliye sequrity provide karni hai 
		//sebse pahale user nikale aur usi user ka contact print hona chahiye
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		   if (contact.getUser().getId() == user.getId()) {
		        model.addAttribute("contact", contact);
		        model.addAttribute("title", contact.getName());
				
		    } else {
		        // Trying to access someone else's contact → block it
		        return "normal/contact_details"; // or show error page
		    }
		
		
		model.addAttribute("contact", contact);
		
		return "normal/contact_details";
	}
	
	//Delete Handler
	@GetMapping("/delete/{cId}")
	public String deleteContact(@PathVariable("cId") Integer cId, Model model, HttpSession session, Principal principal) {
		
		Contact contact = this.contactRepository.findById(cId).get();
	
		//check...Assignment
		
		//delete krne se pahale user nikalenge aur user ke sare contacts
		User user = this.userRepository.getUserByUserName(principal.getName());
		
		user.getContacts().remove(contact);
		
		this.userRepository.save(user);
		
	
		
		session.setAttribute("message", new com.smart.helper.Message("Contact Deleted Successfully.!!", "success"));
		
		return "redirect:/user/show_contacts/0";
	}
	
	//open Update form hander
	@PostMapping("/update-contact/{cId}")
	public String updateForm(@PathVariable("cId") Integer cId, Model model) {
		model.addAttribute("title", "Update Contact");
		
	Contact contact =	this.contactRepository.findById(cId).get();
	
	model.addAttribute("contact", contact);
		
		return "normal/update_form";
	}
	
	//update contact handler
	@PostMapping("/process-update")
	public String updateHandler(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file, Model model, HttpSession session, Principal principal ) {
		
		//old Contact Details
		Contact oldcontactDetail = this.contactRepository.findById(contact.getcId()).get();
		
		try {
			//image check kya user ne new img select ki hai
			if(!file.isEmpty()) {
//				new data
				// delete old Photo form server when update the contact
				File deleteFile = new ClassPathResource("static/img").getFile();  // classpathResource se img folder mathun sarv images ghete ani deleteFile mathe thevte
				File file1 = new File(deleteFile, oldcontactDetail.getImage());   //mg deleteFile mathun fct oldcontactDetail chi image ghete ani file1 mathe oldContact chi img thevte
				file1.delete();  // mg file1 delete krte
				
				//Update new photo
				File saveFile = new ClassPathResource("static/img").getFile();     // ye line batati hai ki images static mathlya img folder mathe save karni hai
				
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator+file.getOriginalFilename());   //absolute path nikala jaha file save karni hai with original name
				
				
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);     // file.getInputStream() ki help se form me ki image path me save kar diya  aur agar usi name ki file hoto use replace kar diya
			
				contact.setImage(file.getOriginalFilename());
		
			}else {
				contact.setImage(oldcontactDetail.getImage());
			}
			
			User user = this.userRepository.getUserByUserName(principal.getName());    // login user nikala
			
			contact.setUser(user);  // create contact or update contact login suser se link kar diya
			
			this.contactRepository.save(contact);   // contact save kar diaya
			
			session.setAttribute("message", new com.smart.helper.Message("Your contact is update..", "success"));
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		System.out.println("CONTACT NAME: "+contact.getName());
		System.out.println(("CONTACT ID: "+contact.getcId()));
		return "redirect:/user/"+contact.getcId()+"/contact";
	}
	
// your Profile handler
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		
		model.addAttribute("title", "Profile Page");
		return "normal/profile";
	}
	
	// open setting handler
	@GetMapping("/settings")
	public String openSettings() {
		
		return "normal/settings";
	}
	
	//change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword, @RequestParam("newPassword") String newPassword, Principal principal, HttpSession session) {
		
		System.out.println("OLD PASSWORD "+oldPassword);
		System.out.println("NEWPASSWORD "+newPassword);
		
		String userName = principal.getName();
		User currentUser = this.userRepository.getUserByUserName(userName);
		
		System.out.println(currentUser.getPassword());
		
		if(this.bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword())) {
			
			//change Password
			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(currentUser);
			session.setAttribute("message", new com.smart.helper.Message("Your password Successfully Changed..", "success"));
			
			
		}else {
			//error...
			session.setAttribute("message", new com.smart.helper.Message("Please Enter currect old password !!", "danger"));
			return "redirect:/user/settings";
		}
		
		return "redirect:/user/index";
	}
	
	
	
	
	
	

}
