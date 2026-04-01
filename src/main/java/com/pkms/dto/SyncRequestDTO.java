package com.pkms.dto;

import lombok.Data;
import java.util.List;

@Data
public class SyncRequestDTO {
    private String username;        // Пока test, потом заменим на токен
    private List<SyncBookDTO> books;
}