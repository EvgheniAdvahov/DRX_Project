package com.drx.Project.repositories;

import com.drx.Project.model.MyLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogRepository extends JpaRepository<MyLog, Integer> {

    List<MyLog> findAllByItemId(int itemId);
}
