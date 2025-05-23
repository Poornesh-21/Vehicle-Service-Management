package com.albany.restapi.dto;

import lombok.Data;
import java.util.List;

@Data
public class MaterialRequestDTO {
    private List<MaterialItemDTO> items;
    private boolean replaceExisting;
}