package com.smart.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smart.entities.Contact;
import com.smart.entities.User;


public interface ContactRepository extends JpaRepository<Contact, Integer> {
	//Pegination ...
	//Pageable contain:=> Current page,  per page quentity-5
	
	@Query("from Contact as c where c.user.id =:userID")
	public Page<Contact> findContactByUser(@Param("userID") int userId, Pageable pageable);
	
	//Jb search kro contact tob usi user me ka contact search ho
	public List<Contact> findByNameContainingAndUser(String name, User user);
	
}
