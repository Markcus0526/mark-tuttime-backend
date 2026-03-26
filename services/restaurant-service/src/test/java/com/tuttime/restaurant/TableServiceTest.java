package com.tuttime.restaurant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.tuttime.restaurant.entity.Table;
import com.tuttime.restaurant.entity.Zone;
import com.tuttime.restaurant.exception.ResourceNotFoundException;
import com.tuttime.restaurant.repository.TableRepository;
import com.tuttime.restaurant.repository.ZoneRepository;
import com.tuttime.restaurant.service.TableService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TableServiceTest {

    @Mock
    private TableRepository tableRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @InjectMocks
    private TableService service;

    private Table sampleTable;
    private Zone sampleZone;
    private UUID tableId;
    private UUID zoneId;

    @BeforeEach
    void setUp() {
        zoneId = UUID.randomUUID();
        tableId = UUID.randomUUID();
        sampleZone = Zone.builder().zoneId(zoneId).name("Test Zone").build();
        sampleTable = Table.builder()
                .tableId(tableId)
                .zone(sampleZone)
                .number("T001")
                .seats(4)
                .status("active")
                .build();
    }

    @Test
    void getAll_ShouldReturnAll() {
        when(tableRepository.findAll()).thenReturn(List.of(sampleTable));
        List<Table> result = service.getAll();
        assertEquals(1, result.size());
        verify(tableRepository).findAll();
    }

    @Test
    void getById_Existing_ShouldReturnTable() {
        when(tableRepository.findById(tableId)).thenReturn(Optional.of(sampleTable));
        Table result = service.getById(tableId);
        assertEquals(sampleTable, result);
    }

    @Test
    void getById_NonExisting_ShouldThrow() {
        UUID invalidId = UUID.randomUUID();
        when(tableRepository.findById(invalidId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getById(invalidId));
    }

    @Test
    void create_ValidZone_ShouldSave() {
        when(zoneRepository.findById(zoneId)).thenReturn(Optional.of(sampleZone));
        when(tableRepository.save(sampleTable)).thenReturn(sampleTable);
        Table result = service.create(sampleTable);
        assertEquals(sampleTable, result);
        verify(tableRepository).save(sampleTable);
    }

    @Test
    void create_InvalidZone_ShouldThrow() {
        UUID invalidZoneId = UUID.randomUUID();
        sampleTable.getZone().setZoneId(invalidZoneId);
        when(zoneRepository.findById(invalidZoneId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.create(sampleTable));
        verify(tableRepository, never()).save(any());
    }

    @Test
    void update_Valid_ShouldUpdate() {
        when(tableRepository.findById(tableId)).thenReturn(Optional.of(sampleTable));
        when(zoneRepository.findById(zoneId)).thenReturn(Optional.of(sampleZone));
        when(tableRepository.save(any())).thenReturn(sampleTable);

        sampleTable.setNumber("T002");
        Table result = service.update(tableId, sampleTable);
        assertEquals("T002", result.getNumber());
    }

    @Test
    void delete_Existing_ShouldDelete() {
        when(tableRepository.findById(tableId)).thenReturn(Optional.of(sampleTable));
        service.delete(tableId);
        verify(tableRepository).delete(sampleTable);
    }
}
