package com.drx.Project.model;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostDto {

    private int id;
    @NotEmpty(message = "Title is required")
    private String title;
    @NotEmpty(message = "Content is required")
    private String content;
    private String createdAt;
    private String modifiedAt;
    private String createdBy;
    private String modifiedBy;
}

