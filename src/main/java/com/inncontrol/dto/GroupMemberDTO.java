package com.inncontrol.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberDTO {
    private Long id;
    private String name;
    private String role;
    private String photo;
    @com.fasterxml.jackson.annotation.JsonProperty("isAdmin")
    private boolean isAdmin;
}
