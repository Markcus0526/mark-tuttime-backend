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

import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Zone;
import com.tuttime.restaurant.exception.ResourceNotFoundException;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.ZoneRepository;
import com.tuttime.restaurant.service.ZoneService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ZoneServiceTest {

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private BranchRepository branchRepository;

    @InjectMocks
    private ZoneService service;

    private Zone sampleZone;
    private Branch sampleBranch;
    private UUID zoneId;
    private UUID branchId;

    @BeforeEach
    void setUp() {
        branchId = UUID.randomUUID();
        zoneId = UUID.randomUUID();
        sampleBranch = Branch.builder().branchId(branchId).name("Test Branch").build();
        sampleZone = Zone.builder()
                .zoneId(zoneId)
                .branch(sampleBranch)
                .name("Test Zone")
                .description("Test Description")
                .capacity(50)
                .build();
    }

    @Test
    void getAll() {
        when(zoneRepository.findAll()).thenReturn(List.of(sampleZone));
        List<Zone> result = service.getAll();
        assertEquals(1, result.size());
        verify(zoneRepository).findAll();
    }

    @Test
    void getById_Existing() {
        when(zoneRepository.findById(zoneId)).thenReturn(Optional.of(sampleZone));
        Zone result = service.getById(zoneId);
        assertEquals(sampleZone, result);
    }

    @Test
    void getById_NonExisting() {
        UUID invalid = UUID.randomUUID();
        when(zoneRepository.findById(invalid)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getById(invalid));
    }

    @Test
    void create_ValidBranch() {
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(sampleBranch));
        when(zoneRepository.save(sampleZone)).thenReturn(sampleZone);
        Zone result = service.create(sampleZone);
        assertEquals(sampleZone, result);
        verify(zoneRepository).save(sampleZone);
    }

    @Test
    void create_InvalidBranch() {
        UUID invalidBranchId = UUID.randomUUID();
        sampleZone.getBranch().setBranchId(invalidBranchId);
        when(branchRepository.findById(invalidBranchId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.create(sampleZone));
        verify(zoneRepository, never()).save(any());
    }

    @Test
    void update_Valid() {
        when(zoneRepository.findById(zoneId)).thenReturn(Optional.of(sampleZone));
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(sampleBranch));
        when(zoneRepository.save(any())).thenReturn(sampleZone);

        sampleZone.setName("Updated Zone");
        Zone result = service.update(zoneId, sampleZone);
        assertEquals("Updated Zone", result.getName());
    }

    @Test
    void delete_Existing() {
        when(zoneRepository.findById(zoneId)).thenReturn(Optional.of(sampleZone));
        service.delete(zoneId);
        verify(zoneRepository).delete(sampleZone);
    }
}
