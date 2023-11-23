package com.history.blackpeople.Repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import com.history.blackpeople.Model.Items;

public interface ItemsRepo extends JpaRepository<Items, Long> {
	public List<Items> findByItemNameContaining(String search);
    
    public Page<Items> findByItemNameContaining(String name, Pageable pageable);
}
