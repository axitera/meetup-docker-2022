package de.axitera.sb_devtools.data;

import de.axitera.sb_devtools.model.TestData;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HelloRepository extends JpaRepository<TestData, String> {
}
