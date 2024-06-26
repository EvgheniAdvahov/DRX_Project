package com.drx.Project.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "item")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private String manufacturer;
    private String category;
    private String department;
    private String model;
    private String status;
    private String imageFileName;
    private String serialNumber;
    private Integer productOrder;
    private Integer inventoryNumber;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String createdAt;
    private String modifiedAt;
    @OneToMany(mappedBy = "item", cascade = CascadeType.REMOVE)
    private List<MyLog> myLogs;
}

