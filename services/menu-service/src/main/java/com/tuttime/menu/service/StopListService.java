package com.tuttime.menu.service;

import com.tuttime.menu.dto.StopListDto;
import com.tuttime.menu.entity.StopList;
import com.tuttime.menu.exception.ResourceNotFoundException;
import com.tuttime.menu.mapper.MenuMapper;
import com.tuttime.menu.repository.StopListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StopListService {

	private final StopListRepository stopListRepository;

	public List<StopListDto> getByMenuitemId(Long menuItemId) {
		return stopListRepository.findByMenuItemId(menuItemId)
				.stream()
				.map(MenuMapper::toDto)
				.collect(Collectors.toList());
	}

	public StopListDto getById(Long id) {
		StopList stopList = stopListRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Stop list not found with id: " + id));

		return MenuMapper.toDto(stopList);
	}

	public StopListDto create(StopListDto dto) {
		// TODO:
		StopList entity = MenuMapper.toEntity(dto);
		StopList saved = stopListRepository.save(entity);
		return MenuMapper.toDto(saved);
	}

	public StopListDto update(Long id, StopListDto dto) {
		StopList existing = stopListRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Stop list not found with id: " + id));
//    existing.setMenuItem(dto.getMenuItem());
		existing.setBranchId(dto.getBranchId());
		existing.setStartDate(dto.getStartDate());
		existing.setEndDate(dto.getEndDate());
		existing.setReason(dto.getReason());
		StopList updated = stopListRepository.save(existing);
		return MenuMapper.toDto(updated);
	}

	public void delete(Long id) {
		stopListRepository.deleteById(id);
	}
}