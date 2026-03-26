package com.tuttime.restaurant.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.restaurant.dto.TableDto;
import com.tuttime.restaurant.entity.Table;
import com.tuttime.restaurant.entity.Zone;
import com.tuttime.restaurant.mapper.TableMapper;
import com.tuttime.restaurant.service.TableService;
import com.tuttime.restaurant.service.ZoneService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(TableController.class)
@ActiveProfiles("test")
class TableControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TableService tableService;

    @MockBean
    private ZoneService zoneService;

    @MockBean
    private TableMapper tableMapper;

    private TableDto sampleDto;
    private Table sampleTable;
    private Zone sampleZone;
    private UUID tableId;
    private UUID zoneId;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        zoneId = UUID.randomUUID();
        tableId = UUID.randomUUID();
        sampleZone = Zone.builder().zoneId(zoneId).build();
        sampleDto = TableDto.builder()
                .tableId(tableId)
                .zoneId(zoneId)
                .number("T001")
                .seats(4)
                .status("active")
                .build();
        sampleTable = Table.builder()
                .tableId(tableId)
                .zone(sampleZone)
                .number("T001")
                .seats(4)
                .status("active")
                .build();
    }

    @Test
    void getAll() throws Exception {
        when(tableService.getAll()).thenReturn(List.of(sampleTable));
        when(tableMapper.toDto(any())).thenReturn(sampleDto);
        mockMvc.perform(get("/api/v1/tables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getById() throws Exception {
        when(tableService.getById(tableId)).thenReturn(sampleTable);
        when(tableMapper.toDto(sampleTable)).thenReturn(sampleDto);
        mockMvc.perform(get("/api/v1/tables/{id}", tableId))
                .andExpect(status().isOk());
    }

    @Test
    void create() throws Exception {
        when(zoneService.getById(zoneId)).thenReturn(sampleZone);
        when(tableMapper.toEntity(sampleDto, sampleZone)).thenReturn(sampleTable);
        when(tableService.create(sampleTable)).thenReturn(sampleTable);
        when(tableMapper.toDto(sampleTable)).thenReturn(sampleDto);

        mockMvc.perform(post("/api/v1/tables")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDto)))
                .andExpect(status().isOk());
    }

    @Test
    void update() throws Exception {
        when(zoneService.getById(zoneId)).thenReturn(sampleZone);
        when(tableMapper.toEntity(sampleDto, sampleZone)).thenReturn(sampleTable);
        when(tableService.update(tableId, sampleTable)).thenReturn(sampleTable);
        when(tableMapper.toDto(sampleTable)).thenReturn(sampleDto);

        mockMvc.perform(put("/api/v1/tables/{id}", tableId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDto)))
                .andExpect(status().isOk());
    }

    @Test
    void delete() throws Exception {
        doNothing().when(tableService).delete(tableId);
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/tables/{id}", tableId))
                .andExpect(status().isNoContent());
    }
}
