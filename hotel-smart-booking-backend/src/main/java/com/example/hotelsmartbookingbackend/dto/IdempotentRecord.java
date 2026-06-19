package com.example.hotelsmartbookingbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdempotentRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private String status; // PROCESSING, SUCCESS
    private int responseStatus;
    private Object responseBody;
}
