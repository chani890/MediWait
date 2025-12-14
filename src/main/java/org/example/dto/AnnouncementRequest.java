package org.example.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnnouncementRequest {
    private String title;
    private String content;
    private Integer displayOrder;
    private Boolean isActive;
}


