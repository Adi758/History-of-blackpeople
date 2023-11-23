package com.history.blackpeople.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.history.blackpeople.Model.Users;

public interface UsersRepo extends JpaRepository<Users, String> {
    
    @Query(value = "SELECT user FROM Users user WHERE user.email = ?1")
	public Users findByemail(String email);
    
	public List<Users> findByUserType(String userType);
}
