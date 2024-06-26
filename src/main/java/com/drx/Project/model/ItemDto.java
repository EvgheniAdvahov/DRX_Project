package com.drx.Project.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ItemDto {

    @NotEmpty(message = "Name is required")
    @Size(max = 7, message = "Name can't exceed 7 characters")
    private String name;
    private String manufacturer;
    private String category;
    private String department;
    private String model;
    @NotEmpty(message = "Serial number required")
    private String serialNumber;
    private Integer productOrder;
    private Integer inventoryNumber;
    @Size(max = 300, message = "Description can't exceed 300 characters")
    private String description;
    private MultipartFile imageFile;
    private String status;
}

